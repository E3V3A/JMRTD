/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.security.auth.x500.X500Principal;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.VerificationStatus.Verdict;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.DG14File;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DataGroup;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.SecurityInfo;

/**
 * A passport object is basically a collection of buffered input streams for the
 * data groups, combined with some status information (progress).
 * 
 * Contains methods for creating instances from scratch, from file, and from
 * card service.
 * 
 * Also contains the document verification logic.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class Passport<C, R>
{
	private static final int BUFFER_SIZE = 243;

	private static final int MAX_TRIES_PER_BAC_ENTRY = 10;

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	/** Maps FID to stream. */
	private Map<Short, InputStream> rawStreams;

	/** Maps FID to stream. */
	private Map<Short, InputStream> bufferedStreams;

	/** Maps FID to bytes. */
	private Map<Short, byte[]> filesBytes;

	/** Maps FID to file length. */
	private Map<Short, Integer> fileLengths;

	/** FIDs that could not be read (because of EAC, e.g.). */
	private Collection<Short> couldNotRead;

	/** Bytes read so far. */
	private int bytesRead;

	/** Total bytes to read */
	private int totalLength;

	private Collection<ProgressListener> progressListeners;

	private VerificationStatus verificationStatus;

	private short cvcaFID = PassportService.EF_CVCA;

	/* Our local copies of the COM and SOD files: */
	private COMFile comFile;
	private SODFile sodFile;

	private boolean isPKIXRevocationCheckingEnabled = false;
	private boolean hasEACSupport = false;

	private PrivateKey docSigningPrivateKey;

	private CardVerifiableCertificate cvcaCertificate;

	private PrivateKey eacPrivateKey;

	private PrivateKey aaPrivateKey;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private BACKeySpec bacKeySpec;
	private MRTDTrustStore trustManager;

	private PassportService<C, R> service;

	private Passport() {
		this.progressListeners = new HashSet<ProgressListener>();
	}

	public Passport(COMFile comFile, Collection<DataGroup> dataGroups, SODFile sodFile,
			PrivateKey docSigningPrivateKey, MRTDTrustStore trustManager) throws GeneralSecurityException {
		this();
		this.trustManager = trustManager;
		this.verificationStatus = new VerificationStatus();
		this.docSigningPrivateKey = docSigningPrivateKey;

		rawStreams = new HashMap<Short, InputStream>();
		bufferedStreams = new HashMap<Short, InputStream>();
		filesBytes = new HashMap<Short, byte[]>();
		fileLengths = new HashMap<Short, Integer>();
		couldNotRead = new ArrayList<Short>();

		this.comFile = comFile;
		byte[] comBytes = comFile.getEncoded();
		int fileLength = comBytes.length;
		totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
		fileLengths.put(PassportService.EF_COM, fileLength);
		rawStreams.put(PassportService.EF_COM, new ByteArrayInputStream(comBytes));

		for (DataGroup dg: dataGroups) {
			byte[] dgBytes = dg.getEncoded();
			fileLength = dgBytes.length;
			totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
			short fid = LDSFile.lookupFIDByTag(dg.getTag());
			fileLengths.put(fid, fileLength);
			rawStreams.put(fid, new ByteArrayInputStream(dgBytes));
		}

		this.sodFile = sodFile;
		byte[] sodBytes = sodFile.getEncoded();
		fileLength = sodBytes.length;
		totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
		fileLengths.put(PassportService.EF_SOD, fileLength);
		rawStreams.put(PassportService.EF_SOD, new ByteArrayInputStream(sodBytes));
	}

	public Passport(PassportService<C, R> service, MRTDTrustStore trustManager, BACStore bacStore) throws CardServiceException {
		this();
		this.service = service;
		this.trustManager = trustManager;
		this.verificationStatus = new VerificationStatus();
		try {
			service.open();
		} catch (Exception e) {
			e.printStackTrace();
			throw new CardServiceException("Cannot open passport. " + e.getMessage());
		}
		this.bacKeySpec = null;

		/* Find out whether this passport supports BAC. */
		boolean isBACPassport = false;
		try {
			/* Attempt to read EF.COM before BAC. */
			CardFileInputStream comIn = service.readFile(PassportService.EF_COM);
			new COMFile(comIn);
			isBACPassport = false;
		} catch (CardServiceException cse) {
			isBACPassport = true;
		} catch (IOException e) {
			e.printStackTrace();
			/* NOTE: Now what? */
		}

		/* Try entries from BACStore. */
		if (isBACPassport) {
			int tries = MAX_TRIES_PER_BAC_ENTRY;
			List<BACKeySpec> bacEntries = bacStore.getEntries();
			List<BACKeySpec> triedBACEntries = new ArrayList<BACKeySpec>();
			try {
				/* NOTE: outer loop, try N times all entries (user may be entering new entries meanwhile). */
				while (bacKeySpec == null && tries-- > 0) {
					/* NOTE: inner loop, loops through stored BAC entries. */
					synchronized (bacStore) {
						for (BACKeySpec otherBACKeySpec: bacEntries) {
							try {
								if (!triedBACEntries.contains(otherBACKeySpec)) {
									LOGGER.info("BAC: " + otherBACKeySpec);
									service.doBAC(otherBACKeySpec);
									/* NOTE: if successful, doBAC terminates normally, otherwise exception. */
									bacKeySpec = otherBACKeySpec;
									break; /* out of inner for loop */
								}
								Thread.sleep(500);
							} catch (CardServiceException cse) {
								/* NOTE: BAC failed? Try next BACEntry */
							}
						}
					}
				}
			} catch (InterruptedException ie) {
				/* NOTE: Interrupted? leave loop. */
			}
		}
		if (isBACPassport && bacKeySpec == null) {
			/* Passport requires BAC, but we failed to authenticate. */
			throw new CardServiceException("Basic Access denied!");
		}
		try {
			readFromService(service, bacKeySpec, trustManager);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new CardServiceException(ioe.getMessage());
		}
	}

	public Passport(PassportService<C, R> service, BACKeySpec bacKeySpec, MRTDTrustStore trustManager) throws IOException, CardServiceException {
		this();
		this.trustManager = trustManager;
		readFromService(service, bacKeySpec, trustManager);
	}

	public Passport(File file, MRTDTrustStore trustManager) throws IOException {
		this();
		this.trustManager = trustManager;
		this.verificationStatus = new VerificationStatus();
		rawStreams = new HashMap<Short, InputStream>();
		bufferedStreams = new HashMap<Short, InputStream>();
		filesBytes = new HashMap<Short, byte[]>();
		fileLengths = new HashMap<Short, Integer>();
		couldNotRead = new ArrayList<Short>();

		ZipFile zipFile = new ZipFile(file);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
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
				int delimIndex = fileName.lastIndexOf('.');
				String baseName = delimIndex < 0 ? fileName : fileName.substring(0, fileName.indexOf('.'));
				if (delimIndex >= 0) {
					if (!fileName.endsWith(".bin")
							&& !fileName.endsWith(".BIN")
							&& !fileName.endsWith(".dat")
							&& !fileName.endsWith(".DAT")) {
						LOGGER.warning("Skipping file " + fileName + "(delimIndex == " + delimIndex + ")");
						continue;					
					}
				}

				if (baseName.length() == 4) {
					try {
						/* Filename <FID>.bin? */
						fid = Hex.hexStringToShort(baseName);
					} catch (NumberFormatException nfe) {
						/* ...guess not */ 
					}
				}

				byte[] bytes = new byte[size];
				int fileLength = bytes.length;
				InputStream zipEntryIn = zipFile.getInputStream(entry);
				DataInputStream dataIn = new DataInputStream(zipEntryIn);
				dataIn.readFully(bytes);
				dataIn.close();
				int tagBasedFID = LDSFile.lookupFIDByTag(bytes[0] & 0xFF);
				if (fid < 0) {
					/* FIXME: untested! */
					fid = tagBasedFID;
				}
				if (fid != tagBasedFID) {
					LOGGER.warning("File name based FID = " + Integer.toHexString(fid) + ", while tag based FID = " + tagBasedFID);
				}
				totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
				fileLengths.put((short)fid, fileLength);
				rawStreams.put((short)fid, new ByteArrayInputStream(bytes));
				if(fid == PassportService.EF_COM) {
					comFile = new COMFile(new ByteArrayInputStream(bytes));
				} else if (fid == PassportService.EF_SOD) {
					sodFile = new SODFile(new ByteArrayInputStream(bytes));                  
				}
			} catch (NumberFormatException nfe) {
				/* NOTE: ignore this file */
			}
		}
	}

	/**
	 * Constructs a passport object by reading from an actual MRTD chip
	 * through a PassportService.
	 * 
	 * @param service the service to read from
	 * @param cvcaStore contains EAC relevant credentials
	 * @param bacKeySpec contains the the document number that is used in EAC
	 * 
	 * @throws IOException on error
	 * @throws CardServiceException on error
	 */
	private void readFromService(PassportService<C, R> service, BACKeySpec bacKeySpec, MRTDTrustStore trustManager) throws IOException, CardServiceException {	
		if (service == null) { throw new IllegalArgumentException("service parameter cannot be null"); }
		rawStreams = new HashMap<Short, InputStream>();
		bufferedStreams = new HashMap<Short, InputStream>();
		filesBytes = new HashMap<Short, byte[]>();
		fileLengths = new HashMap<Short, Integer>();
		couldNotRead = new ArrayList<Short>();
		InputStream comIn = preReadFile(service, PassportService.EF_COM);
		comFile = new COMFile(comIn);
		int[] comTagList = comFile.getTagList();
		InputStream sodIn = preReadFile(service, PassportService.EF_SOD);
		sodFile = new SODFile(sodIn);
		InputStream dg1In = preReadFile(service, PassportService.EF_DG1);
		DG1File dg1File = new DG1File(dg1In);
		String documentNumber = bacKeySpec != null ? bacKeySpec.getDocumentNumber() : dg1File.getMRZInfo().getDocumentNumber();

		DG14File dg14File = null;
		CVCAFile cvcaFile = null;

		/* Find out if we need to do EAC. */
		if (Arrays.asList(comTagList).contains(LDSFile.EF_DG14_TAG)) {
			InputStream dg14In = preReadFile(service, PassportService.EF_DG14);
			dg14File = new DG14File(dg14In);

			/* Now try to deal with EF.CVCA */
			List<Integer> cvcafids = dg14File.getCVCAFileIds();
			if (cvcafids != null && cvcafids.size() != 0) {
				if (cvcafids.size() > 1) { LOGGER.warning("More than one CVCA file id present in DG14."); }
				cvcaFID = cvcafids.get(0).shortValue();
			}
			InputStream cvcaIn = preReadFile(service, cvcaFID);
			cvcaFile = new CVCAFile(cvcaIn);

			/* Try to do EAC. */
			for (KeyStore cvcaStore: trustManager.getCVCAStores()) {
				// FIXME: Try with all cvcaStores?
				doEAC(documentNumber, dg14File, cvcaFile, cvcaStore);
			}
		}

		/* Start reading each of the files. */
		for (int tag: comTagList) {
			short fid = LDSFile.lookupFIDByTag(tag);
			try {
				setupFile(service, fid);
			} catch(CardServiceException ex) {
				/* NOTE: Most likely EAC protected file. */
				LOGGER.info("Could not read file with FID " + Integer.toHexString(fid)
						+ ": " + ex.getMessage());
			}
		}
	}

	private void doEAC(String documentNumber, DG14File dg14File, CVCAFile cvcaFile, KeyStore cvcaStore) throws CardServiceException {
		hasEACSupport = true;
		Map<Integer, PublicKey> cardKeys = dg14File.getChipAuthenticationPublicKeyInfos();
		for (CVCPrincipal caRef: new CVCPrincipal[]{ cvcaFile.getCAReference(), cvcaFile.getAltCAReference() }) {
			if (caRef != null) {
				try {
					List<String> aliases = Collections.list(cvcaStore.aliases());
					for (String alias: aliases) {
						if (cvcaStore.isCertificateEntry(alias)) {
							CardVerifiableCertificate certificate = (CardVerifiableCertificate)cvcaStore.getCertificate(alias);
							CVCPrincipal authRef = certificate.getAuthorityReference();
							CVCPrincipal holderRef = certificate.getHolderReference();
							if (caRef.equals(authRef)) {
								/* See if we have a private key for that certificate. */
								PrivateKey privateKey = (PrivateKey)cvcaStore.getKey(holderRef.getName(), "".toCharArray());
								Certificate[] certPath = cvcaStore.getCertificateChain(holderRef.getName());
								if (privateKey != null) {
									List<CardVerifiableCertificate> terminalCerts = new ArrayList<CardVerifiableCertificate>(certPath.length);
									for (Certificate c: certPath) { terminalCerts.add((CardVerifiableCertificate)c); }

									for (Map.Entry<Integer, PublicKey> entry: cardKeys.entrySet()) {
										int i = entry.getKey();
										PublicKey publicKey = entry.getValue();
										try {
											service.doEAC(i, publicKey, caRef, terminalCerts, privateKey, documentNumber);
											verificationStatus.setEAC(Verdict.SUCCEEDED);
											break;
										} catch(CardServiceException cse) {
											cse.printStackTrace();
											/* NOTE: Failed, too bad, try next public key. */
										}
									}
								}
							}				
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Gets an inputstream that is ready for reading.
	 * 
	 * @param fid
	 * @return an inputstream for <code>fid</code>
	 */
	public synchronized InputStream getInputStream(final short fid) throws CardServiceException {
		if (couldNotRead.contains(fid)) {
			throw new CardServiceException("Could not read " + Integer.toHexString(fid));
		}
		try {
			InputStream inputStream = null;
			byte[] file = filesBytes.get(fid);
			if (file != null) {
				/* Already completely read this file. */
				inputStream = new ByteArrayInputStream(file);
				inputStream.mark(file.length + 1);
			} else {
				/* FIXME: why not simply return new BufferedInputStream(rawInputStreams.get(fid) ? */

				/* Maybe partially read? Use the buffered stream. */
				inputStream = bufferedStreams.get(fid); // FIXME: some thread may already be reading this one?
				if (inputStream != null && inputStream.markSupported()) { inputStream.reset(); }
			}
			if (inputStream == null) {
				/* Not read yet. Start reading it. */
				startCopyingRawInputStream(fid);
				inputStream = bufferedStreams.get(fid);              
			}
			return inputStream;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalStateException("ERROR: " + ioe.toString());
		}
	}

	public void putFile(short fid, byte[] bytes) {
		if (bytes == null) { return; }
		filesBytes.put(fid, bytes);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
		int fileLength = bytes.length;
		inputStream.mark(fileLength + 1);
		bufferedStreams.put(fid, inputStream);
		fileLengths.put(fid, fileLength);
		// FIXME: is this necessary?
		totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
		if(fid != PassportService.EF_COM && fid != PassportService.EF_SOD && fid != cvcaFID) {
			updateCOMSODFile(null);
		}
		verificationStatus.setAll(Verdict.UNKNOWN);
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
			MessageDigest digest = null;
			digest = MessageDigest.getInstance(digestAlg);
			for (Short fid : dgFids) {
				if (fid != PassportService.EF_COM && fid != PassportService.EF_SOD && fid != cvcaFID) {
					byte[] data = getFileBytes(fid);
					byte tag = data[0];
					dgHashes.put(LDSFile.lookupDataGroupNumberByTag(tag), digest.digest(data));
					comFile.insertTag(Integer.valueOf(tag));
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
		try {
			InputStream inputStream = getInputStream(fid);
			if (inputStream == null) { return null; }

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[256];
			while (true) {
				try {
					int bytesRead = inputStream.read(buf);
					if (bytesRead < 0) { break; }
					out.write(buf, 0, bytesRead);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			return out.toByteArray();
		} catch (CardServiceException cse) {
			return null;
		}
	}

	public BACKeySpec getBACKeySpec() {
		return bacKeySpec;
	}

	public void setDocSigningPrivateKey(PrivateKey key) {
		docSigningPrivateKey = key;
		updateCOMSODFile(null);
	}

	public void setDocSigningCertificate(X509Certificate newCertificate) {
		updateCOMSODFile(newCertificate);
	}

	public void setCVCertificate(CardVerifiableCertificate cert) {
		this.cvcaCertificate = cert;
		try {
			CVCAFile cvcaFile = new CVCAFile(cvcaCertificate.getHolderReference().getName());
			putFile(cvcaFID, cvcaFile.getEncoded());
		} catch (CertificateException ce) {
			ce.printStackTrace();
		}
	}

	public CardVerifiableCertificate getCVCertificate() {
		return cvcaCertificate;
	}

	public PrivateKey getDocSigningPrivateKey() {
		return docSigningPrivateKey;
	}

	public MRTDTrustStore getTrustManager() {
		return trustManager;
	}

	public void setEACPrivateKey(PrivateKey privateKey) {
		this.eacPrivateKey = privateKey;
	}

	public void setEACPublicKey(PublicKey publicKey) {
		ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = new ChipAuthenticationPublicKeyInfo(publicKey);
		DG14File dg14File = new DG14File(Arrays.asList(new SecurityInfo[] { chipAuthenticationPublicKeyInfo }));		
		putFile(PassportService.EF_DG14, dg14File.getEncoded());
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

	public int getTotalLength() {
		return totalLength;
	}

	public int getBytesRead() {
		return bytesRead;
	}

	public List<Short> getFileList() {
		List<Short> result = new ArrayList<Short>();
		result.addAll(fileLengths.keySet());
		result.addAll(couldNotRead);
		Collections.sort(result);
		return result;
	}

	/**
	 * Verifies the passport using the security related mechanisms.
	 * Adjusts the verificationIndicator to show the user the verification status.
	 * 
	 * Assumes passport object is non-null and read from the service.
	 */
	public void verifySecurity() {
		verifyBAC();
		verifyEAC();
		verifyDS();
		verifyCS();
		verifyAA(service);
		/*
		 * FIXME: The verifyAA call used to be right after verifyEAC
		 * but that seems to generate a security status not satisfied
		 * if in SAFE_MODE on my EAC NIK. It seems to work fine in
		 * PROGRESSIVE_MODE though. Some kind of synchronization error?
		 * -- MO
		 */
	}

	/**
	 * Builds certificate chain from SOd to CSCA anchor.
	 * Uses PKIX algorithm.
	 * 
	 * @return a list of certificates
	 * 
	 * @throws GeneralSecurityException if could not be checked
	 */
	public List<Certificate> getCertificateChain() throws GeneralSecurityException {
		List<CertStore> cscaStores = trustManager.getCSCAStores();
		if (cscaStores == null) {
			LOGGER.warning("No certificate stores found.");
			return null;
		}
		SODFile sod = null;
		try {
			InputStream sodIn = getInputStream(PassportService.EF_SOD);
			sod = new SODFile(sodIn);
		} catch (IOException ioe) {
			LOGGER.warning("Error opening SOD file");
			return null;
		} catch (CardServiceException cse) {
			LOGGER.warning("Error opening SOD file");
			return null;
		}
		X500Principal sodIssuer = sod.getIssuerX500Principal();
		BigInteger sodSerialNumber = sod.getSerialNumber();
		X509Certificate docSigningCertificate = null;
		try {
			docSigningCertificate = sod.getDocSigningCertificate();
		} catch (Exception e) {
			LOGGER.warning("Error getting document signing certificate: " + e.getMessage());
			// FIXME: search for it in cert stores?
		}
		if (docSigningCertificate != null) {
			X500Principal docIssuer = docSigningCertificate.getIssuerX500Principal();
			if (!sodIssuer.equals(docIssuer)) {
				LOGGER.severe("Security object issuer principal is different from embedded DS certificate issuer!");
				return null;
			}
			BigInteger docSerialNumber = docSigningCertificate.getSerialNumber();
			if (!sodSerialNumber.equals(docSerialNumber)) {
				LOGGER.warning("Security object serial number is different from embedded DS certificate serial number!");
			}
		}
		return getCertificateChain(docSigningCertificate, sodIssuer, sodSerialNumber);
	}

	/**
	 * Builds a certificate chain to an anchor using the PKIX algorithm.
	 * 
	 * @param docSigningCertificate the start certificate
	 * @param sodIssuer the issuer of the start certificate (ignored unless <code>docSigningCertificate</code> is <code>null</code>)
	 * @param sodSerialNumber the serial number of the start certificate (ignored unless <code>docSigningCertificate</code> is <code>null</code>)
	 * 
	 * @return the certificate chain
	 */
	private List<Certificate> getCertificateChain(X509Certificate docSigningCertificate, final X500Principal sodIssuer, final BigInteger sodSerialNumber) {
		X509CertSelector selector = new X509CertSelector();
		try {
			if (docSigningCertificate != null) {
				selector.setCertificate(docSigningCertificate);
			} else {
				selector.setIssuer(sodIssuer);
				selector.setSerialNumber(sodSerialNumber);
			}

			CertStoreParameters docStoreParams = new CollectionCertStoreParameters(Collections.singleton((Certificate)docSigningCertificate));
			CertStore docStore = CertStore.getInstance("Collection", docStoreParams);

			CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", BC_PROVIDER);
			PKIXBuilderParameters  buildParams = new PKIXBuilderParameters(trustManager.getCSCAAnchors(), selector);
			buildParams.addCertStore(docStore);
			for (CertStore trustStore: trustManager.getCSCAStores()) {
				buildParams.addCertStore(trustStore);
			}
			buildParams.setRevocationEnabled(isPKIXRevocationCheckingEnabled); /* NOTE: set to false for checking disabled. */
			Security.addProvider(BC_PROVIDER); /* DEBUG: needed, or builder will throw a runtime exception. FIXME! */
			PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult)builder.build(buildParams);
			if (result == null) { return null; }
			CertPath chain = result.getCertPath();
			List<Certificate> chainCertificates = new ArrayList<Certificate>(chain.getCertificates());
			if (chainCertificates.size() > 0 && docSigningCertificate != null && !chainCertificates.contains(docSigningCertificate)) {
				/* NOTE: if target certificate not in list, we add it ourselves. */
				LOGGER.warning("Adding start certificate after PKIXBuilder finished");
				chainCertificates.add(0, docSigningCertificate);
			}
			Certificate anchorCert = result.getTrustAnchor().getTrustedCert();
			if (chainCertificates.size() > 0 && anchorCert != null && !chainCertificates.contains(anchorCert)) {
				chainCertificates.add(anchorCert);
			}			
			return chainCertificates;
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.info("Building a chain failed (" + e.getMessage() + ").");
		}
		return null;
	}

	/**
	 * Adds an authentication listener to this MRTD.
	 * 
	 * @param l an authentication listener
	 */
	public void addAuthenticationListener(AuthListener l) {
		if (service != null) {
			service.addAuthenticationListener(l);
		}
	}

	/**
	 * Gets the verification status of this MRTD.
	 * 
	 * @return a {@link VerificationStatus}
	 */
	public VerificationStatus getVerificationStatus() {
		return verificationStatus;
	}

	/* Only private methods below. */

	private BufferedInputStream preReadFile(PassportService<C, R> service, short fid) throws CardServiceException {
		if (rawStreams.containsKey(fid)) {
			int length = fileLengths.get(fid); 
			BufferedInputStream bufferedIn = new BufferedInputStream(rawStreams.get(fid), length + 1);
			bufferedIn.mark(length + 1);
			rawStreams.put(fid, bufferedIn);
			return bufferedIn;
		} else {
			CardFileInputStream cardIn = service.readFile(fid);
			int length = cardIn.getFileLength();
			BufferedInputStream bufferedIn = new BufferedInputStream(cardIn, length + 1);
			totalLength += length; notifyProgressListeners(bytesRead, totalLength);
			fileLengths.put(fid, length);
			bufferedIn.mark(length + 1);
			rawStreams.put(fid, bufferedIn);
			return bufferedIn;
		}
	}

	private void setupFile(PassportService<C, R> service, short fid) throws CardServiceException {
		if (rawStreams.containsKey(fid)) {
			LOGGER.info("Raw input stream for " + Integer.toHexString(fid) + " already set up.");
			return;
		}
		CardFileInputStream cardIn = service.readFile(fid);
		int fileLength = cardIn.getFileLength();
		cardIn.mark(fileLength + 1);
		rawStreams.put(fid, cardIn);
		totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
		fileLengths.put(fid, fileLength);        
	}

	/**
	 * Starts a thread to read the raw (unbuffered) inputstream and copy its bytes into a buffer
	 * so that clients can read from those.
	 *
	 * @param fid indicates the file to copy
	 * @throws IOException
	 */
	private synchronized void startCopyingRawInputStream(final short fid) throws IOException {
		final Passport<C, R> passport = this;
		if (couldNotRead.contains(fid)) { return; }
		final InputStream unBufferedIn = rawStreams.get(fid);
		if (unBufferedIn == null) {
			String message = "Cannot read " + LDSFile.toString(LDSFile.lookupTagByFID(fid));
			LOGGER.warning(message + " (not starting thread)");
			couldNotRead.add(fid);
			return;
		}
		final int fileLength = fileLengths.get(fid);
		unBufferedIn.reset();
		final PipedInputStream pipedIn = new PipedInputStream(fileLength + 1);
		final PipedOutputStream out = new PipedOutputStream(pipedIn);
		final ByteArrayOutputStream copyOut = new ByteArrayOutputStream();
		InputStream inputStream = new BufferedInputStream(pipedIn, fileLength + 1);
		inputStream.mark(fileLength + 1);
		bufferedStreams.put(fid, inputStream);
		(new Thread(new Runnable() {
			public void run() {
				byte[] buf = new byte[BUFFER_SIZE];
				try {
					while (true) {
						int bytesRead = unBufferedIn.read(buf);
						if (bytesRead < 0) { break; }
						out.write(buf, 0, bytesRead);
						copyOut.write(buf, 0, bytesRead);
						passport.bytesRead += bytesRead; notifyProgressListeners(passport.bytesRead, totalLength);
					}
					out.flush(); out.close();
					copyOut.flush();
					byte[] copyOutBytes = copyOut.toByteArray();
					filesBytes.put(fid, copyOutBytes);
					copyOut.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					/* FIXME: what if something goes wrong inside this thread? */
					couldNotRead.add(fid);
					return;
				}
			}
		})).start();
	}

	/** Checks whether BAC was used. */
	private void verifyBAC() {
		if (bacKeySpec != null) {
			verificationStatus.setBAC(Verdict.SUCCEEDED);
		} else {
			verificationStatus.setBAC(Verdict.NOT_PRESENT);
		}
	}

	/** Checks whether EAC was used. */
	private void verifyEAC() {
		if (!hasEACSupport) {
			verificationStatus.setEAC(Verdict.NOT_PRESENT);
		}
		/* NOTE: If EAC was performed, verification status already updated! */
	}

	/** Check active authentication. */
	private void verifyAA(PassportService<C, R> service) {
		try {
			InputStream sodIn = getInputStream(PassportService.EF_SOD);
			SODFile	sod = new SODFile(sodIn);
			if (sod.getDataGroupHashes().get(15) == null) {
				verificationStatus.setAA(Verdict.NOT_PRESENT);
				return;
			}
			InputStream dg15In = getInputStream(PassportService.EF_DG15);
			if (dg15In == null) { LOGGER.severe("dg15In == null in Passport.verifyAA"); }
			if (dg15In != null && service != null) {
				DG15File dg15 = new DG15File(dg15In);
				PublicKey pubKey = dg15.getPublicKey();
				if (service.doAA(pubKey)) {
					verificationStatus.setAA(Verdict.SUCCEEDED);
				} else {
					verificationStatus.setAA(Verdict.FAILED);
				}
			}
		} catch (CardServiceException cse) {
			cse.printStackTrace();
			verificationStatus.setAA(Verdict.FAILED);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			verificationStatus.setAA(Verdict.FAILED);           
		}
	}

	/**
	 * Checks hashes in the SOd correspond to hashes we compute, 
	 * checks the security object's signature.
	 * 
	 * TODO: Check the cert stores (notably PKD) to fetch document signer certificate (if not embedded in SOd) and check its validity before checking the signature.
	 */
	private void verifyDS() {
		try {
			InputStream comIn = getInputStream(PassportService.EF_COM);
			COMFile com = new COMFile(comIn);
			List<Integer> comDGList = new ArrayList<Integer>();
			for(Integer tag : com.getTagList()) {
				comDGList.add(LDSFile.lookupDataGroupNumberByTag(tag));
			}
			Collections.sort(comDGList);

			InputStream sodIn = getInputStream(PassportService.EF_SOD);
			SODFile sod = new SODFile(sodIn);
			Map<Integer, byte[]> hashes = sod.getDataGroupHashes();

			verificationStatus.setDS(Verdict.UNKNOWN);

			/* Jeroen van Beek sanity check */
			List<Integer> tagsOfHashes = new ArrayList<Integer>(hashes.keySet());
			Collections.sort(tagsOfHashes);
			if (!tagsOfHashes.equals(comDGList)) {
				LOGGER.warning("Found mismatch between EF.COM and EF.SOd:\n"
						+ "tagsOfHashes = " + tagsOfHashes + "\n"
						+ "comDGList = " + comDGList);
				verificationStatus.setDS(Verdict.FAILED);
				return; /* NOTE: Serious enough to not perform other checks, leave method. */
			}

			String digestAlgorithm = sod.getDigestAlgorithm();
			MessageDigest digest = null;
			if (Security.getAlgorithms("MessageDigest").contains(digestAlgorithm)) {
				digest = MessageDigest.getInstance(digestAlgorithm);
			} else {
				digest = MessageDigest.getInstance(digestAlgorithm, BC_PROVIDER);
			}
			for (int dgNumber: hashes.keySet()) {
				short fid = LDSFile.lookupFIDByTag(LDSFile.lookupTagByDataGroupNumber(dgNumber));
				byte[] storedHash = hashes.get(dgNumber);

				digest.reset();

				InputStream dgIn = null;
				Exception ex = null;
				try {
					dgIn = getInputStream(fid);
				} catch(Exception e) {
					dgIn = null;
					ex = e;
				}
				if (dgIn == null) {
					LOGGER.warning("Skipping DG" + dgNumber + " during DS verification because file could not be read.");
					continue;
				}
				if (hasEACSupport && (verificationStatus.getEAC() != Verdict.SUCCEEDED) && (fid == PassportService.EF_DG3 || fid == PassportService.EF_DG4)) {
					LOGGER.warning("Skipping DG" + dgNumber + " during DS verification because EAC failed.");
					continue;
				} else if (ex != null) {
					throw ex;
				}

				byte[] buf = new byte[4096];
				while (true) {
					int bytesRead = dgIn.read(buf);
					if (bytesRead < 0) { break; }
					digest.update(buf, 0, bytesRead);
				}
				byte[] computedHash = digest.digest();
				if (!Arrays.equals(storedHash, computedHash)) {
					LOGGER.warning("Authentication of DG" + dgNumber + " failed");
					verificationStatus.setDS(Verdict.FAILED);
					return; /* NOTE: Serious enough to not perform other checks, leave method. */
				}
			}

			X509Certificate docSigningCert = sod.getDocSigningCertificate();
			if (docSigningCert == null) {
				LOGGER.warning("Could not get document signer certificate from EF.SOd.");
				// FIXME: We search for it in cert stores. See note at verifyCS.
				// X500Principal issuer = sod.getIssuerX500Principal();
				// BigInteger serialNumber = sod.getSerialNumber();
			}
			if (sod.checkDocSignature(docSigningCert)) {
				verificationStatus.setDS(Verdict.SUCCEEDED);
			} else {
				LOGGER.warning("DS Signature incorrect");
				verificationStatus.setDS(Verdict.FAILED);
			}
		} catch (NoSuchAlgorithmException nsae) {
			verificationStatus.setDS(Verdict.FAILED);
			return; /* NOTE: Serious enough to not perform other checks, leave method. */
		} catch (Exception e) {
			e.printStackTrace();
			verificationStatus.setDS(Verdict.FAILED);
			return; /* NOTE: Serious enough to not perform other checks, leave method. */
		}
	}

	/**
	 * Checks the certificate chain.
	 * 
	 * FIXME: Rename this method (it does more than just CS -> DS checking).
	 */
	private void verifyCS() {
		try {
			List<Certificate> chainCertificates = getCertificateChain();
			if (chainCertificates == null) {
				verificationStatus.setCS(Verdict.FAILED);
				return;
			}

			int chainDepth = chainCertificates.size();
			if (chainDepth < 1) {
				LOGGER.warning("Could not find certificate in stores to check target certificate. Chain depth = " + chainDepth + ".");
				verificationStatus.setCS(Verdict.FAILED);
				return;				
			}

			/* FIXME: This is no longer necessary after PKIX has done its job? */
			if (chainDepth == 1) {
				// X509Certificate docSigningCertificate = (X509Certificate)chainCertificates.get(0);
				LOGGER.info("Document signer certificate found in store. Chain depth = " + chainDepth + ".");
				verificationStatus.setCS(Verdict.SUCCEEDED);
			} else if (chainDepth == 2) {
				X509Certificate docSigningCertificate = (X509Certificate)chainCertificates.get(0);
				X509Certificate countrySigningCertificate = (X509Certificate)chainCertificates.get(1);
				LOGGER.info("Country signer certificate found in store. Chain depth = " + chainDepth + ". Checking signature.");
				docSigningCertificate.verify(countrySigningCertificate.getPublicKey());
				verificationStatus.setCS(Verdict.SUCCEEDED); /* NOTE: No exception... verification succeeded! */
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.warning("CSCA certificate check failed!" + e.getMessage());
			verificationStatus.setCS(Verdict.FAILED);
		}
	}

	public void addProgressListener(ProgressListener l) {
		progressListeners.add(l);
	}

	public void removeProgressListener(ProgressListener l) {
		progressListeners.remove(l);
	}

	private void notifyProgressListeners(int progress, int max) {
		if (progressListeners == null) { return; }
		for (ProgressListener l: progressListeners) {
			l.changed(progress, max);
		}
	}

	public interface ProgressListener {
		void changed(int progress, int max);
	}
}
