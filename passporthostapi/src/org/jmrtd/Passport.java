/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2009  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id:  $
 */

package org.jmrtd;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.Gender;
import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.ejbca.cvc.CVCertificate;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.DG14File;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.PassportFile;
import org.jmrtd.lds.SODFile;

/**
 * A passport object is basically a collection of buffered input streams for the data groups,
 * combined with some status information (progress).
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class Passport
{
	private static final int BUFFER_SIZE = 243;

	private Map<Short, InputStream> rawStreams = new HashMap<Short, InputStream>();
	private Map<Short, InputStream> bufferedStreams = new HashMap<Short, InputStream>();
	private Map<Short, byte[]> filesBytes = new HashMap<Short, byte[]>();
	private Map<Short, Integer> fileLengths = new HashMap<Short, Integer>();
	private int bytesRead = 0;
	private int totalLength = 0;

	public short CVCA_FID = PassportService.EF_CVCA;

	// Our local copies of the COM and SOD files:
	private COMFile comFile = null;
	private SODFile sodFile = null;

	private boolean hasEACSupport = false;
	private boolean isEACSuccess = false;

	private PrivateKey docSigningPrivateKey = null;
	private CVCertificate cvcaCertificate = null;
	private PrivateKey eacPrivateKey = null;

	private PrivateKey aaPrivateKey = null;

	/**
	 * Constructs a passport object by reading from an actual passport chip
	 * through a PassportService.
	 * 
	 * @param service the service to read from
	 * @param documentNumber the document number is used in EAC
	 * 
	 * @throws IOException on error
	 * @throws CardServiceException on error
	 */
	public Passport(PassportService service, CVCAStore d, String documentNumber) throws IOException, CardServiceException {
		BufferedInputStream bufferedIn = null;

		bufferedIn = preReadFile(service, PassportService.EF_COM);
		comFile = new COMFile(bufferedIn);
		bufferedIn.reset();

		// For now save the EAC fids (DG3/DG4) and deal with them later
		// Also, deal with DG14 in a special way, like with COM/SOD
		List<Short> eacFids = new ArrayList<Short>();
		DG14File dg14file = null;
		CVCAFile cvcaFile = null;
		for (int tag: comFile.getTagList()) {
			short fid = PassportFile.lookupFIDByTag(tag);
			if(fid == PassportService.EF_DG14) {
				bufferedIn = preReadFile(service, PassportService.EF_DG14);
				dg14file = new DG14File(bufferedIn);
				bufferedIn.reset();
				// Now try to deal with EF.CVCA
				List<Integer> cvcafids = dg14file.getCVCAFileIds();
				if(cvcafids != null && cvcafids.size() != 0) {
					if(cvcafids.size() > 1) {
						System.err.println("Warning: more than one CVCA file id present in DG14.");
					}
					CVCA_FID = cvcafids.get(0).shortValue();
				}
				bufferedIn = preReadFile(service, CVCA_FID);
				cvcaFile = new CVCAFile(bufferedIn);
				bufferedIn.reset();
			}else{
				try{
					setupFile(service, fid);
				}catch(CardServiceException ex) {
					// Most likely EAC protected file: 
					eacFids.add(fid);                  
				}
			}
		}
		bufferedIn = preReadFile(service, PassportService.EF_SOD);
		sodFile = new SODFile(bufferedIn);
		bufferedIn.reset();
		// Try to do EAC, if DG14File present 
		if(dg14file != null) {
			hasEACSupport = true;
			List<List<CVCertificate>> termCerts = new ArrayList<List<CVCertificate>>();
			List<PrivateKey> termKeys = new ArrayList<PrivateKey>();
			List<String> caRefs = new ArrayList<String>();
			for(String caRef : new String[]{ cvcaFile.getCAReference(), cvcaFile.getAltCAReference() }) {
				if(caRef != null) {
					try {
						List<CVCertificate> t = d.getCertificates(caRef);
						if(t != null) {
							termCerts.add(t);
							termKeys.add(d.getPrivateKey(caRef));
							caRefs.add(caRef);
						}
					} catch(NoSuchElementException nsee) {}
				}
			}
			if(termCerts.size() == 0) {
				// no luck, passport has EAC, but we don't have the certificates
				return;
			}
			// Try EAC
			if(documentNumber == null) {
				// Try DG1 if document number was not supplied
				bufferedIn = preReadFile(service, PassportService.EF_DG1);
				documentNumber = new DG1File(bufferedIn).getMRZInfo().getDocumentNumber();
				bufferedIn.reset();
			}

			Map<Integer, PublicKey> cardKeys = dg14file.getPublicKeys();
			Set<Integer> keyIds = cardKeys.keySet();
			for(int i : keyIds) {
				if(isEACSuccess) { break; }
				for(int termIndex=0; termIndex<termCerts.size(); termIndex++) {
					try {
						service.doEAC(i, cardKeys.get(i), caRefs.get(termIndex), termCerts.get(termIndex), termKeys.get(termIndex), documentNumber);
						isEACSuccess = true;
						break;
					}catch(CardServiceException cse) {
						cse.printStackTrace();
					}
				}
			}
			if(isEACSuccess) {
				// setup DG3 and/or DG4 for reading
				for (Short fid : eacFids) {
					setupFile(service, fid);
				}
			}
		}
	}

	public Passport(File file) throws IOException {
		ZipFile zipIn = new ZipFile(file);
		Enumeration<? extends ZipEntry> entries = zipIn.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (entry == null) { break; }
			String fileName = entry.getName();
			long sizeAsLong = entry.getSize();
			if (sizeAsLong < 0) {
				throw new IOException("ZipEntry has negative size.");
			}
			int size = (int)(sizeAsLong & 0x00000000FFFFFFFFL);
			try {
				int fid = -1;
				int delimIndex = fileName.indexOf('.');
				if (!fileName.endsWith(".bin") && !fileName.endsWith(".BIN")) {
					System.out.println("DEBUG: skipping file " + fileName + "(delimIndex == " + delimIndex + ")");
					continue;					
				}
				if (delimIndex == 4) {
					try {
						/* Filename <FID>.bin? */
						fid = Hex.hexStringToShort(fileName.substring(0, fileName.indexOf('.')));
					} catch (NumberFormatException nfe) {
						/* ...guess not */ 
					}
				}
				byte[] bytes = new byte[size];
				int fileLength = bytes.length;
				DataInputStream dataIn = new DataInputStream(zipIn.getInputStream(entry));
				dataIn.readFully(bytes);
				if (fid < 0) {
					/* FIXME: untested! */
					fid = PassportFile.lookupFIDByTag(bytes[0] & 0xFF);
				}
				totalLength += fileLength;
				fileLengths.put((short)fid, fileLength);
				rawStreams.put((short)fid, new ByteArrayInputStream(bytes));
				if(fid == PassportService.EF_COM) {
					comFile = new COMFile(new ByteArrayInputStream(bytes));
				}else if(fid == PassportService.EF_SOD) {
					sodFile = new SODFile(new ByteArrayInputStream(bytes));                  
				}
			} catch (NumberFormatException nfe) {
				/* NOTE: ignore this file */
			}
		}
	}

	public Passport() throws GeneralSecurityException {

		/* EF.COM */
		List<Integer> tagList = new ArrayList<Integer>();
		tagList.add(PassportFile.EF_DG1_TAG);
		tagList.add(PassportFile.EF_DG2_TAG);
		comFile = new COMFile("01", "07", "04", "00", "00", tagList);
		byte[] comBytes = comFile.getEncoded();
		int fileLength = comBytes.length;
		totalLength += fileLength;
		fileLengths.put(PassportService.EF_COM, fileLength);
		rawStreams.put(PassportService.EF_COM, new ByteArrayInputStream(comBytes));

		/* EF.DG1 */
		Date today = Calendar.getInstance().getTime();
		String primaryIdentifier = "";
		String[] secondaryIdentifiers = { "" };
		MRZInfo mrzInfo = new MRZInfo(MRZInfo.DOC_TYPE_ID1, Country.NL, primaryIdentifier, secondaryIdentifiers, "", Country.NL, today, Gender.MALE, today, "");
		DG1File dg1 = new DG1File(mrzInfo);
		byte[] dg1Bytes = dg1.getEncoded();
		fileLength = dg1Bytes.length;
		totalLength += fileLength;
		fileLengths.put(PassportService.EF_DG1, fileLength);
		rawStreams.put(PassportService.EF_DG1, new ByteArrayInputStream(dg1Bytes));

		/* EF.DG2 */
		DG2File dg2 = new DG2File(); 
		byte[] dg2Bytes = dg2.getEncoded();
		fileLength = dg2Bytes.length;
		totalLength += fileLength;
		fileLengths.put(PassportService.EF_DG2, fileLength);
		rawStreams.put(PassportService.EF_DG2, new ByteArrayInputStream(dg2Bytes));

		/* EF.SOD */
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		Date dateOfIssuing = today;
		Date dateOfExpiry = today;
		String digestAlgorithm = "SHA256";
		String signatureAlgorithm = "SHA256withRSA";
		X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
		certGenerator.setSerialNumber(new BigInteger("1"));
		certGenerator.setIssuerDN(new X509Name("C=NL, O=JMRTD, OU=CSCA, CN=jmrtd.org/emailAddress=info@jmrtd.org"));
		certGenerator.setSubjectDN(new X509Name("C=NL, O=JMRTD, OU=DSCA, CN=jmrtd.org/emailAddress=info@jmrtd.org"));
		certGenerator.setNotBefore(dateOfIssuing);
		certGenerator.setNotAfter(dateOfExpiry);
		certGenerator.setPublicKey(publicKey);
		certGenerator.setSignatureAlgorithm(signatureAlgorithm);
		X509Certificate docSigningCert = (X509Certificate)certGenerator.generate(privateKey, "BC");
		docSigningPrivateKey = privateKey;
		Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
		MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
		hashes.put(1, digest.digest(dg1Bytes));
		hashes.put(2, digest.digest(dg2Bytes));
		sodFile = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, privateKey, docSigningCert);
		byte[] sodBytes = sodFile.getEncoded();
		fileLength = sodBytes.length;
		totalLength += fileLength;
		fileLengths.put(PassportService.EF_SOD, fileLength);
		rawStreams.put(PassportService.EF_SOD, new ByteArrayInputStream(sodBytes));
	}

	/**
	 * Gets an inputstream that is ready for reading.
	 * 
	 * @param fid
	 * @return an inputstream for <code>fid</code>
	 */
	public synchronized InputStream getInputStream(final short fid) {
		try {
			InputStream in = null;
			byte[] file = filesBytes.get(fid);
			if (file != null) {
				/* Already completely read this file. */
				in = new ByteArrayInputStream(file);
				in.mark(file.length + 1);
			} else {
				/* Maybe partially read? Use the buffered stream. */
				in = bufferedStreams.get(fid); // FIXME: some thread may already be reading this one?
				if (in != null && in.markSupported()) { in.reset(); }
			}
			if (in == null) {
				/* Not read yet. Start reading it. */
				startCopyingRawInputStream(fid);
				in = bufferedStreams.get(fid);              
			}
			return in;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalStateException("ERROR: " + ioe.toString());
		}
	}

	public void putFile(short fid, byte[] bytes) {
		if (bytes == null) { return; }
		filesBytes.put(fid, bytes);
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		int fileLength = bytes.length;
		in.mark(fileLength + 1);
		bufferedStreams.put(fid, in);
		fileLengths.put(fid, fileLength);
		// FIXME: is this necessary?
		totalLength += fileLength;
		if(fid != PassportService.EF_COM && fid != PassportService.EF_SOD && fid != CVCA_FID) {
			updateCOMSODFile(null);
		}
	}

	public void updateCOMSODFile(X509Certificate newCertificate) {
		try {
			String digestAlg = sodFile.getDigestAlgorithm();
			String signatureAlg = sodFile.getDigestEncryptionAlgorithm();
			X509Certificate cert = newCertificate != null ? newCertificate : sodFile.getDocSigningCertificate();
			byte[] signature = sodFile.getEncryptedDigest();
			Map<Integer, byte[]> dgHashes = new TreeMap<Integer, byte[]>();
			List<Short> dgFids = new ArrayList<Short>();
			dgFids.addAll(fileLengths.keySet());
			Collections.sort(dgFids);
			MessageDigest digest = MessageDigest.getInstance(digestAlg);
			for (Short fid : dgFids) {
				if (fid != PassportService.EF_COM && fid != PassportService.EF_SOD && fid != CVCA_FID) {
					byte[] data = getFileBytes(fid);
					byte tag = data[0];
					dgHashes.put(PassportFile.lookupDataGroupNumberByTag(tag), digest.digest(data));
					comFile.insertTag(new Integer(tag));
				}
			}
			if(docSigningPrivateKey != null) {
				sodFile = new SODFile(digestAlg, signatureAlg, dgHashes, docSigningPrivateKey, cert);
			} else {
				sodFile = new SODFile(digestAlg, signatureAlg, dgHashes, signature, cert);            
			}
			putFile(PassportService.EF_SOD, sodFile.getEncoded());
			putFile(PassportService.EF_COM, comFile.getEncoded());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public byte[] getFileBytes(short fid) {
		byte[] result = filesBytes.get(fid);
		if (result != null) { return result; }
		InputStream in = getInputStream(fid);
		if (in == null) { return null; }
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[256];
		while (true) {
			try {
				int bytesRead = in.read(buf);
				if (bytesRead < 0) { break; }
				out.write(buf, 0, bytesRead);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return out.toByteArray();
	}

	public void setDocSigningPrivateKey(PrivateKey key) {
		docSigningPrivateKey = key;
		updateCOMSODFile(null);
	}

	public void setDocSigningCertificate(X509Certificate newCertificate) {
		updateCOMSODFile(newCertificate);
	}

	public void setCVCertificate(CVCertificate cert) {
		this.cvcaCertificate = cert;
		try {
			CVCAFile cvcaFile = new CVCAFile(cvcaCertificate.getCertificateBody().getHolderReference().getConcatenated());
			putFile(CVCA_FID, cvcaFile.getEncoded());
		} catch (NoSuchFieldException ex) {
			/* FIXME: Woj, why is this silent? -- MO */
		}
	}

	public CVCertificate getCVCertificate() {
		return cvcaCertificate;
	}

	public PrivateKey getDocSigningPrivateKey() {
		return docSigningPrivateKey;
	}

	public void setEACKeys(KeyPair keyPair) {
		this.eacPrivateKey = keyPair.getPrivate();
		Map<Integer,PublicKey> key = new TreeMap<Integer, PublicKey>();
		key.put(-1, keyPair.getPublic());
		DG14File dg14file = new DG14File(key, null, null, null);
		putFile(PassportService.EF_DG14, dg14file.getEncoded());
	}

	public void setAAKeys(KeyPair keyPair) {
		this.aaPrivateKey = keyPair.getPrivate();
		DG15File dg15file = new DG15File(keyPair.getPublic());
		putFile(PassportService.EF_DG15, dg15file.getEncoded());
	}

	public PrivateKey getAAPrivateKey() {
		return aaPrivateKey;
	}

	public void setAAPrivateKey(PrivateKey key) {
		aaPrivateKey = key;
	}

	public void setAAPublicKey(PublicKey key) {
		DG15File dg15file = new DG15File(key);
		putFile(PassportService.EF_DG15, dg15file.getEncoded());
	}

	public PrivateKey getEACPrivateKey() {
		return eacPrivateKey;
	}

	public boolean hasEAC() {
		return hasEACSupport;
	}

	public boolean wasEACPerformed() {
		return isEACSuccess;
	}

	public int getTotalLength() {
		return totalLength;
	}

	public int getBytesRead() {
		return bytesRead;
	}

	public List<Short> getFileList() {
		List<Short> result = new ArrayList<Short>();
		result.addAll(fileLengths.keySet());
		return result;
	}

	/* Only private methods below. */

	private BufferedInputStream preReadFile(PassportService service, short fid) throws CardServiceException {
		BufferedInputStream bufferedIn = null;
		if(rawStreams.containsKey(fid)) {
			int length = fileLengths.get(fid); 
			bufferedIn = new BufferedInputStream(rawStreams.get(fid), length+1);
			bufferedIn.mark(length + 1);
			rawStreams.put(fid, bufferedIn);
			return bufferedIn;
		}else{
			CardFileInputStream cardIn = service.readFile(fid);
			int length = cardIn.getFileLength();
			bufferedIn = new BufferedInputStream(cardIn, length + 1);
			totalLength += length;
			fileLengths.put(fid, length);
			bufferedIn.mark(length + 1);
			rawStreams.put(fid, bufferedIn);
			return bufferedIn;
		}
	}

	private void setupFile(PassportService service, short fid) throws CardServiceException {
		CardFileInputStream in = service.readFile(fid);
		int fileLength = in.getFileLength();
		in.mark(fileLength + 1);
		rawStreams.put(fid, in);
		totalLength += fileLength;
		fileLengths.put(fid, fileLength);        
	}

	/**
	 * Starts a thread to read the raw inputstream.
	 *
	 * @param fid
	 * @throws IOException
	 */
	private synchronized void startCopyingRawInputStream(final short fid) throws IOException {
		final Passport passport = this;
		final InputStream unBufferedIn = rawStreams.get(fid);
		if (unBufferedIn == null) { throw new IOException("No raw inputstream to copy " + Integer.toHexString(fid)); }
		final int fileLength = fileLengths.get(fid);
		unBufferedIn.reset();
		final PipedInputStream pipedIn = new PipedInputStream(fileLength + 1);
		final PipedOutputStream out = new PipedOutputStream(pipedIn);
		final ByteArrayOutputStream copyOut = new ByteArrayOutputStream();
		InputStream in = new BufferedInputStream(pipedIn, fileLength + 1);
		in.mark(fileLength + 1);
		bufferedStreams.put(fid, in);
		(new Thread(new Runnable() {
			public void run() {
				byte[] buf = new byte[BUFFER_SIZE];
				try {
					while (true) {
						int bytesRead = unBufferedIn.read(buf);
						if (bytesRead < 0) { break; }
						out.write(buf, 0, bytesRead);
						copyOut.write(buf, 0, bytesRead);
						passport.bytesRead += bytesRead;
					}
					out.flush(); out.close();
					copyOut.flush();
					byte[] copyOutBytes = copyOut.toByteArray();
					filesBytes.put(fid, copyOutBytes);
					copyOut.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					/* FIXME: what if something goes wrong inside this thread? */
				}
			}
		})).start();
	}
}
