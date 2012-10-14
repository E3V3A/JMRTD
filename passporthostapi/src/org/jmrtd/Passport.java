/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
import org.jmrtd.lds.LDS;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.LDSFileUtil;
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
 * FIXME: probably should split this up in a class in org.jmrtd.lds for aggregating LDS infos, a class for accessing an MRTD on ICC (performing all necessary access and verification protocols)
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: $
 */
public class Passport {

	private static final int BUFFER_SIZE = 243;

	private static final int DEFAULT_MAX_TRIES_PER_BAC_ENTRY = 5;

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

	private LDS lds;

	private boolean isPKIXRevocationCheckingEnabled = false;
	private boolean hasEACSupport = false;

	private PrivateKey docSigningPrivateKey;

	private CardVerifiableCertificate cvcaCertificate;

	private PrivateKey eacPrivateKey;

	private PrivateKey aaPrivateKey;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private BACKeySpec bacKeySpec;
	private MRTDTrustStore trustManager;

	private PassportService service;

	private Passport() {
		this.progressListeners = new HashSet<ProgressListener>();
	}

	/**
	 * Creates a document from LDS data structures.
	 * 
	 * @param comFile the EF_COM
	 * @param dataGroups the data groups
	 * @param sodFile the EF_SOd
	 * @param docSigningPrivateKey the document signing private key
	 * @param trustManager the trust manager (CSCA, CVCA)
	 * 
	 * @throws GeneralSecurityException if error
	 */
	public Passport(LDS lds, PrivateKey docSigningPrivateKey, MRTDTrustStore trustManager) throws GeneralSecurityException {
		this();
		this.trustManager = trustManager;
		this.verificationStatus = new VerificationStatus();
		this.docSigningPrivateKey = docSigningPrivateKey;

		rawStreams = new HashMap<Short, InputStream>();
		bufferedStreams = new HashMap<Short, InputStream>();
		filesBytes = new HashMap<Short, byte[]>();
		fileLengths = new HashMap<Short, Integer>();
		couldNotRead = new ArrayList<Short>();

		this.lds = lds;
		byte[] comBytes = lds.getCOMFile().getEncoded();
		int fileLength = comBytes.length;
		totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
		fileLengths.put(PassportService.EF_COM, fileLength);
		rawStreams.put(PassportService.EF_COM, new ByteArrayInputStream(comBytes));

		for (DataGroup dg: lds.getDataGroups()) {
			byte[] dgBytes = dg.getEncoded();
			fileLength = dgBytes.length;
			totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
			short fid = LDSFileUtil.lookupFIDByTag(dg.getTag());
			fileLengths.put(fid, fileLength);
			rawStreams.put(fid, new ByteArrayInputStream(dgBytes));
		}

		byte[] sodBytes = lds.getSODFile().getEncoded();
		fileLength = sodBytes.length;
		totalLength += fileLength; notifyProgressListeners(bytesRead, totalLength);
		fileLengths.put(PassportService.EF_SOD, fileLength);
		rawStreams.put(PassportService.EF_SOD, new ByteArrayInputStream(sodBytes));
	}

	/**
	 * Creates a document by reading it from a service.
	 * 
	 * @param service the service to read from
	 * @param trustManager the trust manager (CSCA, CVCA)
	 * @param bacStore the BAC entries
	 * 
	 * @throws CardServiceException on error
	 */
	public Passport(PassportService service, MRTDTrustStore trustManager, BACStore bacStore) throws CardServiceException {
		this(service, trustManager, bacStore, DEFAULT_MAX_TRIES_PER_BAC_ENTRY);
	}

	/**
	 * Creates a document by reading it from a service.
	 * 
	 * @param service the service to read from
	 * @param trustManager the trust manager (CSCA, CVCA)
	 * @param bacStore the BAC entries
	 * @param maxTriesPerBACEntry the number of times each BAC entry will be tried before giving up
	 * 
	 * @throws CardServiceException on error
	 */
	public Passport(PassportService service, MRTDTrustStore trustManager, BACStore bacStore, int maxTriesPerBACEntry) throws CardServiceException {
		this();
		this.service = service;
		this.trustManager = trustManager;
		this.verificationStatus = new VerificationStatus();
		try {
			service.open();
		} catch (CardServiceException cse) {
			throw cse;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CardServiceException("Cannot open document. " + e.getMessage());
		}
		this.bacKeySpec = null;

		/* Find out whether this MRTD supports BAC. */
		boolean isBACPassport = false;
		try {
			/* Attempt to read EF.COM before BAC. */
			CardFileInputStream comIn = service.getInputStream(PassportService.EF_COM);
			new COMFile(comIn);
			isBACPassport = false;
		} catch (CardServiceException cse) {
			isBACPassport = true;
		} catch (IOException e) {
			e.printStackTrace();
			/* NOTE: Now what? */
		}

		/* Try entries from BACStore. */
		List<BACKey> triedBACEntries = new ArrayList<BACKey>();
		if (isBACPassport) {
			int tries = maxTriesPerBACEntry;
			List<BACKeySpec> bacEntries = bacStore.getEntries();
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
			throw new BACDeniedException("Basic Access denied!", triedBACEntries);
		}
		try {
			readFromService(service, bacKeySpec, trustManager);
		} catch (IOException ioe) {
			//			ioe.printStackTrace();
			throw new CardServiceException(ioe.getMessage());
		}
	}

	/**
	 * Creates document by reading from service.
	 * 
	 * @param service the service to read from
	 * @param bacKeySpec BAC entry
	 * @param trustManager the trust manager
	 * 
	 * @throws CardServiceException on error
	 */
	public Passport(PassportService service, BACKey bacKeySpec, MRTDTrustStore trustManager) throws CardServiceException {
		this();
		this.trustManager = trustManager;
		try {
			readFromService(service, bacKeySpec, trustManager);
		} catch (IOException ioe) {
			//			ioe.printStackTrace();
			throw new CardServiceException(ioe.getMessage());
		}
	}

	/**
	 * Creates a document by reading it from a ZIP file.
	 * 
	 * @param file the ZIP file to read from
	 * @param trustManager the trust manager (CSCA, CVCA)
	 * 
	 * @throws IOException on error
	 */
	public Passport(File file, MRTDTrustStore trustManager) throws IOException {
		this();
		this.trustManager = trustManager;
		this.verificationStatus = new VerificationStatus();
		rawStreams = new HashMap<Short, InputStream>();
		bufferedStreams = new HashMap<Short, InputStream>();
		filesBytes = new HashMap<Short, byte[]>();
		fileLengths = new HashMap<Short, Integer>();
		couldNotRead = new ArrayList<Short>();

		COMFile comFile = null;
		SODFile sodFile = null;
		ZipFile zipFile = new ZipFile(file);
		try {
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
					if (delimIndex >= 0
							&& !fileName.endsWith(".bin")
							&& !fileName.endsWith(".BIN")
							&& !fileName.endsWith(".dat")
							&& !fileName.endsWith(".DAT")) {
						LOGGER.warning("Skipping file \"" + fileName + "\" in \"" + file.getName() + "\"");
						continue;					
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
					int tagBasedFID = LDSFileUtil.lookupFIDByTag(bytes[0] & 0xFF);
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
					LOGGER.warning("Ignoring entry \"" + fileName + "\" in \"" + file.getName() + "\"");
				}
				this.lds = new LDS(comFile, Collections.<DataGroup>emptySet(), sodFile);
			}
		} finally {
			zipFile.close();
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
			String fileName = LDSFileUtil.lookupFileNameByFID(fid);
			LOGGER.warning("Could not read  " + fileName);
			throw new CardServiceException("Could not read " + fileName);
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

	/**
	 * Inserts a file into this document, and updates EF_COM and EF_SOd accordingly.
	 * 
	 * @param fid the FID of the new file
	 * @param bytes the contents of the new file
	 */
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
		try {
			lds.add(inputStream);
		} catch (IOException ioe) {
			System.out.println("DEBUG: failed to add to LDS");
		}
		verificationStatus.setAll(Verdict.UNKNOWN);
	}

	/**
	 * Updates EF_COM and EF_SOd using a new document signing certificate.
	 * 
	 * @param newCertificate a certificate
	 */
	public void updateCOMSODFile(X509Certificate newCertificate) {
		try {
			COMFile comFile = lds.getCOMFile();
			SODFile sodFile = lds.getSODFile();
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
					dgHashes.put(LDSFileUtil.lookupDataGroupNumberByTag(tag), digest.digest(data));
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
			this.lds = new LDS(comFile, lds.getDataGroups(), sodFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the contents of a file.
	 * 
	 * @param fid the FID of the file
	 * 
	 * @return a byte array
	 * 
	 * @deprecated use {@link #getInputStream(short)} instead
	 */
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

	/**
	 * Gets the BAC entry that was used (only relevant when reading from service).
	 * 
	 * @return a BAC entry
	 */
	public BACKeySpec getBACKeySpec() {
		return bacKeySpec;
	}

	/**
	 * Sets the document signing private key.
	 * 
	 * @param docSigningPrivateKey a private key
	 */
	public void setDocSigningPrivateKey(PrivateKey docSigningPrivateKey) {
		this.docSigningPrivateKey = docSigningPrivateKey;
		updateCOMSODFile(null);
	}

	/**
	 * Gets the CVCA certificate.
	 * 
	 * @return a CV certificate or <code>null</code>
	 */
	public CardVerifiableCertificate getCVCertificate() {
		return cvcaCertificate;
	}

	/**
	 * Sets the CVCA certificate.
	 * 
	 * @param cert the CV certificate
	 */
	public void setCVCertificate(CardVerifiableCertificate cert) {
		this.cvcaCertificate = cert;
		try {
			CVCAFile cvcaFile = new CVCAFile(cvcaCertificate.getHolderReference().getName());
			putFile(cvcaFID, cvcaFile.getEncoded());
		} catch (CertificateException ce) {
			ce.printStackTrace();
		}
	}

	/**
	 * Gets the document signing private key, or <code>null</code> if not present.
	 * 
	 * @return a private key or <code>null</code>
	 */
	public PrivateKey getDocSigningPrivateKey() {
		return docSigningPrivateKey;
	}

	/**
	 * Sets the document signing certificate.
	 * 
	 * @param docSigningCertificate a certificate
	 */
	public void setDocSigningCertificate(X509Certificate docSigningCertificate) {
		updateCOMSODFile(docSigningCertificate);
	}

	/**
	 * Gets the CSCA, CVCA trust store.
	 * 
	 * @return the trust store in use
	 */
	public MRTDTrustStore getTrustManager() {
		return trustManager;
	}

	/**
	 * Gets the private key for EAC, or <code>null</code> if not present.
	 * 
	 * @return a private key or <code>null</code>
	 */
	public PrivateKey getEACPrivateKey() {
		return eacPrivateKey;
	}

	/**
	 * Sets the private key for EAC.
	 * 
	 * @param eacPrivateKey a private key
	 */
	public void setEACPrivateKey(PrivateKey eacPrivateKey) {
		this.eacPrivateKey = eacPrivateKey;
	}

	/**
	 * Sets the public key for EAC.
	 * 
	 * @param eacPublicKey a public key
	 */
	public void setEACPublicKey(PublicKey eacPublicKey) {
		ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = new ChipAuthenticationPublicKeyInfo(eacPublicKey);
		DG14File dg14File = new DG14File(Arrays.asList(new SecurityInfo[] { chipAuthenticationPublicKeyInfo }));		
		putFile(PassportService.EF_DG14, dg14File.getEncoded());
	}

	/**
	 * Gets the private key for AA, or <code>null</code> if not present.
	 * 
	 * @return a private key or <code>null</code>
	 */
	public PrivateKey getAAPrivateKey() {
		return aaPrivateKey;
	}

	/**
	 * Sets the private key for AA.
	 * 
	 * @param aaPrivateKey a private key
	 */
	public void setAAPrivateKey(PrivateKey aaPrivateKey) {
		this.aaPrivateKey = aaPrivateKey;
	}

	/**
	 * Sets the public key for AA.
	 * 
	 * @param aaPublicKey a public key
	 */
	public void setAAPublicKey(PublicKey aaPublicKey) {
		DG15File dg15file = new DG15File(aaPublicKey);
		putFile(PassportService.EF_DG15, dg15file.getEncoded());
	}

	/**
	 * Gets the total number of  bytes to read (only relevant when reading from service).
	 * 
	 * @return the total number of bytes in the document
	 */
	public int getTotalLength() {
		return totalLength;
	}

	/**
	 * Gets the number of bytes read (only relevant when reading from service).
	 * 
	 * @return the number of bytes read
	 */
	public int getBytesRead() {
		return bytesRead;
	}

	/**
	 * Gets a list of FIDs.
	 * 
	 * @return a list of FIDs
	 */
	public List<Short> getFileList() {
		List<Short> result = new ArrayList<Short>();
		result.addAll(rawStreams.keySet());
		result.addAll(couldNotRead);
		Collections.sort(result);
		return result;
	}

	/**
	 * Verifies the document using the security related mechanisms.
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
	 * Gets the verification status of this document (and/or the service it was read from).
	 * 
	 * @return a {@link VerificationStatus}
	 */
	public VerificationStatus getVerificationStatus() {
		return verificationStatus;
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
	 * Adds an authentication listener to this document.
	 * 
	 * @param l an authentication listener
	 */
	public void addAuthenticationListener(AuthListener l) {
		if (service != null) {
			service.addAuthenticationListener(l);
		}
	}

	/* ONLY PRIVATE METHODS BELOW. */

	/**
	 * Constructs a document by reading from an actual MRTD chip
	 * through a service.
	 * 
	 * @param service the service to read from
	 * @param cvcaStore contains EAC relevant credentials
	 * @param bacKeySpec contains the the document number that is used in EAC
	 * 
	 * @throws IOException on error
	 * @throws CardServiceException on error
	 */
	private void readFromService(PassportService service, BACKeySpec bacKeySpec, MRTDTrustStore trustManager) throws IOException, CardServiceException {	
		if (service == null) { throw new IllegalArgumentException("Service cannot be null"); }
		rawStreams = new HashMap<Short, InputStream>();
		bufferedStreams = new HashMap<Short, InputStream>();
		filesBytes = new HashMap<Short, byte[]>();
		fileLengths = new HashMap<Short, Integer>();
		couldNotRead = new ArrayList<Short>();
		InputStream comIn = preReadFile(service, PassportService.EF_COM);
		COMFile comFile = new COMFile(comIn);
		int[] comTagList = comFile.getTagList();
		InputStream sodIn = preReadFile(service, PassportService.EF_SOD);
		SODFile sodFile = new SODFile(sodIn);
		InputStream dg1In = preReadFile(service, PassportService.EF_DG1);
		DG1File dg1File = new DG1File(dg1In);
		String documentNumber = bacKeySpec != null ? bacKeySpec.getDocumentNumber() : dg1File.getMRZInfo().getDocumentNumber();

		DG14File dg14File = null;
		CVCAFile cvcaFile = null;

		/* Find out if we need to do EAC. */
		boolean isDG14Present = false;
		for (int tag: comTagList) { if (LDSFile.EF_DG14_TAG == tag) { isDG14Present = true; break; } }
		if (isDG14Present) {
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

		if (isDG14Present) {
			this.lds = new LDS(comFile, Arrays.asList(new DataGroup[] { dg1File, dg14File }), cvcaFile, sodFile);
		} else {
			this.lds = new LDS(comFile, Arrays.asList(new DataGroup[] { dg1File }), sodFile);
		}

		/* Start reading each of the files. */
		for (int tag: comTagList) {
			try {
				short fid = LDSFileUtil.lookupFIDByTag(tag);
				try {
					setupFile(service, fid);
				} catch(CardServiceException ex) {
					/* NOTE: Most likely EAC protected file. */
					LOGGER.info("Could not read file with FID " + Integer.toHexString(fid)
							+ ": " + ex.getMessage());
				}
			} catch (NumberFormatException nfe) {
				LOGGER.warning("DEBUG: ----------> NFE, tag = " + Integer.toHexString(tag));
			}
		}
	}

	private void doEAC(String documentNumber, DG14File dg14File, CVCAFile cvcaFile, KeyStore cvcaStore) throws CardServiceException {
		hasEACSupport = true;
		Map<Integer, PublicKey> cardKeys = dg14File.getChipAuthenticationPublicKeyInfos();
		for (CVCPrincipal caRef: new CVCPrincipal[]{ cvcaFile.getCAReference(), cvcaFile.getAltCAReference() }) {
			if (caRef == null) { continue; }
			try {
				List<String> aliases = Collections.list(cvcaStore.aliases());
				for (String alias: aliases) {
					if (!cvcaStore.isCertificateEntry(alias)) { continue; }
					CardVerifiableCertificate certificate = (CardVerifiableCertificate)cvcaStore.getCertificate(alias);
					CVCPrincipal authRef = certificate.getAuthorityReference();
					CVCPrincipal holderRef = certificate.getHolderReference();
					if (!caRef.equals(authRef)) { continue; }
					/* See if we have a private key for that certificate. */
					PrivateKey privateKey = (PrivateKey)cvcaStore.getKey(holderRef.getName(), "".toCharArray());
					Certificate[] certPath = cvcaStore.getCertificateChain(holderRef.getName());
					if (privateKey == null) { continue; }
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
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
			// e.printStackTrace();
			LOGGER.info("Building a chain failed (" + e.getMessage() + ").");
		}
		return null;
	}

	private BufferedInputStream preReadFile(PassportService service, short fid) throws CardServiceException {
		if (rawStreams.containsKey(fid)) {
			int length = fileLengths.get(fid); 
			BufferedInputStream bufferedIn = new BufferedInputStream(rawStreams.get(fid), length + 1);
			bufferedIn.mark(length + 1);
			rawStreams.put(fid, bufferedIn);
			return bufferedIn;
		} else {
			CardFileInputStream cardIn = service.getInputStream(fid);
			int length = cardIn.getFileLength();
			BufferedInputStream bufferedIn = new BufferedInputStream(cardIn, length + 1);
			totalLength += length; notifyProgressListeners(bytesRead, totalLength);
			fileLengths.put(fid, length);
			bufferedIn.mark(length + 1);
			rawStreams.put(fid, bufferedIn);
			return bufferedIn;
		}
	}

	private void setupFile(PassportService service, short fid) throws CardServiceException {
		if (rawStreams.containsKey(fid)) {
			LOGGER.info("Raw input stream for " + Integer.toHexString(fid) + " already set up.");
			return;
		}
		CardFileInputStream cardInputStream = service.getInputStream(fid);
		int fileLength = cardInputStream.getFileLength();
		cardInputStream.mark(fileLength + 1);
		rawStreams.put(fid, cardInputStream);
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
		final Passport passport = this;
		if (couldNotRead.contains(fid)) { return; }
		final InputStream unBufferedIn = rawStreams.get(fid);
		if (unBufferedIn == null) {
			String message = "Cannot read " + LDSFileUtil.lookupFileNameByTag(LDSFileUtil.lookupTagByFID(fid));
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
	private void verifyAA(PassportService service) {
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
		} catch (Exception e) {
			System.out.println("DEBUG: this exception wasn't caught in verification logic (< 0.4.8) -- MO 3. Type is " + e.getClass().getCanonicalName());
			e.printStackTrace();
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
				try {
					int dgNumber = LDSFileUtil.lookupDataGroupNumberByTag(tag);
					comDGList.add(dgNumber);
				} catch (NumberFormatException nfe) {
					LOGGER.warning("Found non-datagroup tag 0x" + Integer.toHexString(tag) + " in COM.");
				}
			}
			Collections.sort(comDGList);

			InputStream sodIn = getInputStream(PassportService.EF_SOD);
			SODFile sod = new SODFile(sodIn);
			Map<Integer, byte[]> hashes = sod.getDataGroupHashes();

			verificationStatus.setDS(Verdict.UNKNOWN);

			/* Jeroen van Beek sanity check */
			List<Integer> sodDGList = new ArrayList<Integer>(hashes.keySet());
			Collections.sort(sodDGList);
			if (!sodDGList.equals(comDGList)) {
				LOGGER.warning("Found mismatch between EF.COM and EF.SOd:\n"
						+ "datagroups reported in SOd = " + sodDGList + "\n"
						+ "datagroups reported in COM = " + comDGList);
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
				short fid = LDSFileUtil.lookupFIDByTag(LDSFileUtil.lookupTagByDataGroupNumber(dgNumber));
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

	/**
	 * Document processing progress listener interface.
	 * 
	 * @author The JMRTD team (info@jmrtd.org)
	 *
	 * @version $Revision: $
	 */
	public interface ProgressListener {
		void changed(int progress, int max);
	}
}
