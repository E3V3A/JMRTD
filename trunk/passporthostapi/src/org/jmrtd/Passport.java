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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
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
import java.util.Collections;
import java.util.Enumeration;
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

import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.DG14File;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.LDS;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.SecurityInfo;

/**
 * A passport object is basically a collection of input streams for the
 * data groups, combined with some status information (progress).
 * 
 * Contains methods for creating instances from scratch, from file, and from
 * card service.
 * 
 * Also contains the document verification logic.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: $
 */
public class Passport {

	private static final int DEFAULT_MAX_TRIES_PER_BAC_ENTRY = 5;

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	private FeatureStatus featureStatus;
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
		this.featureStatus = new FeatureStatus();
		this.verificationStatus = new VerificationStatus();
	}

	/**
	 * Creates a document from LDS data structures.
	 * 
	 * @param lds the logical data structure
	 * @param docSigningPrivateKey the document signing private key
	 * @param trustManager the trust manager (CSCA, CVCA)
	 * 
	 * @throws GeneralSecurityException if error
	 */
	public Passport(LDS lds, PrivateKey docSigningPrivateKey, MRTDTrustStore trustManager) throws GeneralSecurityException {
		this();
		this.trustManager = trustManager;
		this.docSigningPrivateKey = docSigningPrivateKey;
		this.lds = lds;
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
	public Passport(PassportService service, MRTDTrustStore trustManager, List<BACKeySpec> bacStore) throws CardServiceException {
		this(service, trustManager, bacStore, DEFAULT_MAX_TRIES_PER_BAC_ENTRY);
	}

	/**
	 * Creates a document by reading it from a card service.
	 * 
	 * @param service the service to read from
	 * @param trustManager the trust manager (CSCA, CVCA)
	 * @param bacStore the BAC entries
	 * @param maxTriesPerBACEntry the number of times each BAC entry will be tried before giving up
	 * 
	 * @throws CardServiceException on error
	 */
	public Passport(PassportService service, MRTDTrustStore trustManager, List<BACKeySpec> bacStore, int maxTriesPerBACEntry) throws CardServiceException {
		this();
		if (service == null) { throw new IllegalArgumentException("Service cannot be null"); }
		int lastKnownSW = -1;
		this.service = service;
		this.trustManager = trustManager;
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
		try {
			/* Attempt to read EF.COM before BAC. */
			new COMFile(service.getInputStream(PassportService.EF_COM));
			featureStatus.setBAC(FeatureStatus.Verdict.NOT_PRESENT);
		} catch (CardServiceException cse) {
			featureStatus.setBAC(FeatureStatus.Verdict.PRESENT);
		} catch (IOException e) {
			e.printStackTrace();
			/* NOTE: Now what? */
		}

		/* Try entries from BACStore. */
		boolean hasBAC = featureStatus.hasBAC().equals(FeatureStatus.Verdict.PRESENT);
		List<BACKey> triedBACEntries = new ArrayList<BACKey>();
		if (hasBAC) {
			int triesLeft = maxTriesPerBACEntry;
			List<BACKeySpec> bacEntries = bacStore; // FIXME

			/* NOTE: outer loop, try N times all entries (user may be entering new entries meanwhile). */
			while (bacKeySpec == null && triesLeft-- > 0) {
				/* NOTE: inner loop, loops through stored BAC entries. */
				synchronized (bacStore) {
					for (BACKeySpec otherBACKeySpec: bacEntries) {
						try {
							if (!triedBACEntries.contains(otherBACKeySpec)) {
								LOGGER.info("Trying BAC: " + otherBACKeySpec);
								service.doBAC(otherBACKeySpec);
								/* NOTE: if successful, doBAC terminates normally, otherwise exception. */
								bacKeySpec = otherBACKeySpec;
								break; /* out of inner for loop */
							}
						} catch (CardServiceException cse) {
							lastKnownSW = cse.getSW();
							/* NOTE: BAC failed? Try next BACEntry */
						}
					}
				}
			}
		}
		if (hasBAC && bacKeySpec == null) {
			/* Passport requires BAC, but we failed to authenticate. */
			verificationStatus.setBAC(VerificationStatus.Verdict.FAILED, "BAC failed");
			throw new BACDeniedException("Basic Access denied!", triedBACEntries, lastKnownSW);
		}

		this.lds = new LDS();

		/* Pre-read these files that are always present. */
		COMFile comFile = null;
		DG1File dg1File = null;
		SODFile sodFile = null;

		try {
			CardFileInputStream comIn = service.getInputStream(PassportService.EF_COM);
			lds.add(PassportService.EF_COM, comIn, comIn.getLength());
			comFile = lds.getCOMFile();

			CardFileInputStream sodIn = service.getInputStream(PassportService.EF_SOD);
			lds.add(PassportService.EF_SOD, sodIn, sodIn.getLength());
			sodFile = lds.getSODFile();

			CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
			lds.add(PassportService.EF_DG1, dg1In, dg1In.getLength());
			dg1File = lds.getDG1File();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			LOGGER.warning("Could not read file");
		}

		/* We get the list of DGs from EF.SOd, not from EF.COM. */
		List<Integer> dgNumbers = new ArrayList<Integer>(sodFile.getDataGroupHashes().keySet());
		Collections.sort(dgNumbers);

		String documentNumber = bacKeySpec != null ? bacKeySpec.getDocumentNumber() : dg1File.getMRZInfo().getDocumentNumber();

		/* Pre-read DG14 in case we have to do EAC. */
		if (dgNumbers.contains(14)) {
			featureStatus.setEAC(FeatureStatus.Verdict.PRESENT);
		} else {
			featureStatus.setEAC(FeatureStatus.Verdict.NOT_PRESENT);
		}
		boolean hasEAC = featureStatus.hasEAC().equals(FeatureStatus.Verdict.PRESENT);
		DG14File dg14File = null;
		CVCAFile cvcaFile = null;
		if (hasEAC) {
			try {
				CardFileInputStream dg14In = service.getInputStream(PassportService.EF_DG14);
				lds.add(PassportService.EF_DG14, dg14In, dg14In.getLength());
				dg14File = lds.getDG14File();

				/* Now try to deal with EF.CVCA */
				cvcaFID = PassportService.EF_CVCA;
				List<Short> cvcaFIDs = dg14File.getCVCAFileIds();
				if (cvcaFIDs != null && cvcaFIDs.size() != 0) {
					if (cvcaFIDs.size() > 1) { LOGGER.warning("More than one CVCA file id present in DG14."); }
					cvcaFID = cvcaFIDs.get(0).shortValue();
				}

				CardFileInputStream cvcaIn = service.getInputStream(cvcaFID);
				lds.add(cvcaFID, cvcaIn, cvcaIn.getLength());
				cvcaFile = lds.getCVCAFile();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				LOGGER.warning("Could not read EF.DG14 or EF.CVCA");
			}
			List<KeyStore> cvcaKeyStores = trustManager.getCVCAStores();

			/* Try to do EAC. */
			for (KeyStore cvcaStore: cvcaKeyStores) {
				try {
					doEAC(documentNumber, dg14File, cvcaFile, cvcaStore);
					break;
				} catch (Exception e) {
					LOGGER.warning("EAC failed using CVCA store " + cvcaStore + ": " + e.getMessage());
					e.printStackTrace();
				}
			}
		}

		/* Check to see if we can do AA. */
		if (dgNumbers.contains(15)) {
			featureStatus.setAA(FeatureStatus.Verdict.PRESENT);
		} else {
			featureStatus.setAA(FeatureStatus.Verdict.NOT_PRESENT);
		}
		boolean hasAA = featureStatus.hasAA().equals(FeatureStatus.Verdict.PRESENT);
		if (hasAA) {
			try {
				CardFileInputStream dg15In = service.getInputStream(PassportService.EF_DG15);
				lds.add(PassportService.EF_DG15, dg15In, dg15In.getLength());
				DG15File dg15File = lds.getDG15File();
				verifyAA(service, dg15File);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				LOGGER.warning("Could not read EF.DG15");
				verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "Could not read EF.DG15");
			}
		}

		/* Add remaining data groups to LDS. */
		for (int dgNumber: dgNumbers) {
			if (dgNumber == 1 || dgNumber == 14) { continue; }
			if ((dgNumber == 3 || dgNumber == 4) && !verificationStatus.getEAC().equals(VerificationStatus.Verdict.SUCCEEDED)) { continue; }
			try {
				short fid = LDSFileUtil.lookupFIDByDataGroupNumber(dgNumber);
				CardFileInputStream cardFileInputStream = service.getInputStream(fid);
				lds.add(fid, cardFileInputStream, cardFileInputStream.getLength());
			} catch (IOException ioe) {
				LOGGER.warning("Error reading DG" + dgNumber + ": " + ioe.getMessage());
				break; /* out of for loop */
			} catch(CardServiceException ex) {
				/* NOTE: Most likely EAC protected file. So log, ignore, continue with next file. */
				LOGGER.info("Could not read DG" + dgNumber + ": " + ex.getMessage());
			} catch (NumberFormatException nfe) {
				LOGGER.warning("NumberFormatException trying to get FID for DG" + dgNumber);
				nfe.printStackTrace();
			}
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
		ZipFile zipFile = new ZipFile(file);
		this.lds = new LDS();
		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry == null) { break; }
				String fileName = entry.getName();
				long sizeAsLong = entry.getSize();
				if (sizeAsLong < 0) { throw new IOException("ZipEntry has negative size."); }
				int size = (int)(sizeAsLong & 0xFFFFFFFFL);
				try {
					byte[] bytes = new byte[size];
					InputStream zipEntryIn = zipFile.getInputStream(entry);
					DataInputStream dataIn = new DataInputStream(zipEntryIn);
					dataIn.readFully(bytes);
					dataIn.close();
					int fid = guessFID(fileName, bytes);
					if (fid > 0) {
						this.lds.add((short)fid, new ByteArrayInputStream(bytes), bytes.length);
					}
				} catch (NumberFormatException nfe) {
					/* NOTE: ignore this file */
					LOGGER.warning("Ignoring entry \"" + fileName + "\" in \"" + file.getName() + "\"");
				}
			}
		} finally {
			zipFile.close();
		}
	}

	private int guessFID(String fileName, byte[] bytes) {
		int fid = -1;
		int delimIndex = fileName.lastIndexOf('.');
		String baseName = delimIndex < 0 ? fileName : fileName.substring(0, fileName.indexOf('.'));
		if (delimIndex >= 0
				&& !fileName.endsWith(".bin")
				&& !fileName.endsWith(".BIN")
				&& !fileName.endsWith(".dat")
				&& !fileName.endsWith(".DAT")) {
			LOGGER.warning("Not considering file name \"" + fileName + "\" in determining FID (while reading ZIP file)");
		} else if (baseName.length() == 4) {
			try {
				/* Filename <FID>.bin? */
				fid = Hex.hexStringToShort(baseName);
			} catch (NumberFormatException nfe) {
				/* ...guess not */ 
			}
		}

		int tagBasedFID = LDSFileUtil.lookupFIDByTag(bytes[0] & 0xFF);
		if (fid < 0) {
			/* FIXME: untested! */
			fid = tagBasedFID;
		}
		if (fid != tagBasedFID) {
			LOGGER.warning("File name based FID = " + Integer.toHexString(fid) + ", while tag based FID = " + tagBasedFID);
		}
		return fid;
	}

	/**
	 * Inserts a file into this document, and updates EF_COM and EF_SOd accordingly.
	 * 
	 * @param fid the FID of the new file
	 * @param bytes the contents of the new file
	 */
	public void putFile(short fid, byte[] bytes) {
		if (bytes == null) { return; }
		try {
			lds.add(fid, new ByteArrayInputStream(bytes), bytes.length);
			// FIXME: is this necessary?
			if(fid != PassportService.EF_COM && fid != PassportService.EF_SOD && fid != cvcaFID) {
				updateCOMSODFile(null);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		verificationStatus.setAll(VerificationStatus.Verdict.UNKNOWN, "Unknown");
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
			List<Short> dgFids = lds.getDataGroupList();
			MessageDigest digest = null;
			digest = MessageDigest.getInstance(digestAlg);
			for (Short fid : dgFids) {
				if (fid != PassportService.EF_COM && fid != PassportService.EF_SOD && fid != cvcaFID) {
					int length = lds.getLength(fid);
					InputStream inputStream = lds.getInputStream(fid);
					if (inputStream ==  null) { LOGGER.warning("Could not get input stream for " + Integer.toHexString(fid)); continue; }
					DataInputStream dataInputStream = new DataInputStream(inputStream);
					byte[] data = new byte[length];
					dataInputStream.readFully(data);
					byte tag = data[0];
					dgHashes.put(LDSFileUtil.lookupDataGroupNumberByTag(tag), digest.digest(data));
					comFile.insertTag((int)(tag & 0xFF));
				}
			}
			if(docSigningPrivateKey != null) {
				sodFile = new SODFile(digestAlg, signatureAlg, dgHashes, docSigningPrivateKey, cert);
			} else {
				sodFile = new SODFile(digestAlg, signatureAlg, dgHashes, signature, cert);            
			}
			lds.add(comFile);
			lds.add(sodFile);
		} catch (Exception e) {
			e.printStackTrace();
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

	public LDS getLDS() {
		return lds;
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
			CVCAFile cvcaFile = new CVCAFile(cvcaFID, cvcaCertificate.getHolderReference().getName());
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

	public FeatureStatus getFeatures() {
		return featureStatus;
	}
	
	/**
	 * Verifies the document using the security related mechanisms.
	 * Adjusts the verificationIndicator to show the user the verification status.
	 * 
	 * Assumes passport object is non-null and read from the service.
	 */
	public VerificationStatus verifySecurity() {
		verifyBAC();
		verifyEAC();

		/* COM DG list versus SOd DG list. */
		if (!verifyCOMSOd(lds, verificationStatus)) { return verificationStatus; }

		/* Verify hashes. */
		verifyHashes(lds, verificationStatus);
		//		if (!verifyHashes(lds)) { return verificationStatus; }

		verifyDS();
		verifyCS();

		/*
		 * FIXME: The verifyAA call used to be right after verifyEAC
		 * but that seems to generate a security status not satisfied
		 * if in SAFE_MODE on my EAC NIK. It seems to work fine in
		 * PROGRESSIVE_MODE though. Some kind of synchronization error?
		 * -- MO
		 */

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
		/* Get doc signing certificate. */
		SODFile sod = lds.getSODFile();
		X500Principal sodIssuer = sod.getIssuerX500Principal();
		BigInteger sodSerialNumber = sod.getSerialNumber();
		X509Certificate docSigningCertificate = null;
		try {
			docSigningCertificate = sod.getDocSigningCertificate();
		} catch (Exception e) {
			LOGGER.warning("Error getting document signing certificate: " + e.getMessage());
			// FIXME: search for it in cert stores?
		}

		if (docSigningCertificate == null) {
			LOGGER.warning("Error getting document signing certificate from EF.SOd");
			return Collections.emptyList();
		}

		/* Get trust anchors. */
		List<CertStore> cscaStores = trustManager.getCSCAStores();
		if (cscaStores == null) {
			LOGGER.warning("No certificate stores found.");
			return Collections.singletonList((Certificate)docSigningCertificate);
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

		/* Use PKIX to construct chain. */
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

	private void doEAC(String documentNumber, DG14File dg14File, CVCAFile cvcaFile, KeyStore cvcaStore) throws CardServiceException {
		hasEACSupport = true;
		Map<BigInteger, PublicKey> cardKeys = dg14File.getChipAuthenticationPublicKeyInfos();
		boolean isKeyFound = false;
		boolean isSucceeded = false;
		for (CVCPrincipal caReference: new CVCPrincipal[]{ cvcaFile.getCAReference(), cvcaFile.getAltCAReference() }) {
			if (caReference == null) { continue; }
			try {
				List<String> aliases = Collections.list(cvcaStore.aliases());
				PrivateKey privateKey = null;
				Certificate[] chain = null;
				for (String alias: aliases) {
					if (cvcaStore.isKeyEntry(alias)) {
						Security.insertProviderAt(JMRTDSecurityProvider.getBouncyCastleProvider(), 0);
						Key key = cvcaStore.getKey(alias, "".toCharArray());
						if (key instanceof PrivateKey) {
							privateKey = (PrivateKey)key;
						} else {
							LOGGER.warning("skipping non-private key " + alias);
							continue;
						}
						chain = cvcaStore.getCertificateChain(alias);
					} else if (cvcaStore.isCertificateEntry(alias)) { 
						CardVerifiableCertificate certificate = (CardVerifiableCertificate)cvcaStore.getCertificate(alias);
						CVCPrincipal authRef = certificate.getAuthorityReference();
						CVCPrincipal holderRef = certificate.getHolderReference();
						if (!caReference.equals(authRef)) { continue; }
						/* See if we have a private key for that certificate. */
						privateKey = (PrivateKey)cvcaStore.getKey(holderRef.getName(), "".toCharArray());
						chain = cvcaStore.getCertificateChain(holderRef.getName());
						if (privateKey == null) { continue; }
						LOGGER.fine("found a key, privateKey = " + privateKey);
					}
					if (privateKey == null || chain == null) {
						LOGGER.severe("null chain or key for entry " + alias + ": chain = " + chain + ", privateKey = " + privateKey);
						continue;
					}
					List<CardVerifiableCertificate> terminalCerts = new ArrayList<CardVerifiableCertificate>(chain.length);
					for (Certificate c: chain) { terminalCerts.add((CardVerifiableCertificate)c); }
					for (Map.Entry<BigInteger, PublicKey> entry: cardKeys.entrySet()) {
						BigInteger keyId = entry.getKey();
						PublicKey publicKey = entry.getValue();
						try {
							isKeyFound = true;
							service.doEAC(keyId, publicKey, caReference, terminalCerts, privateKey, documentNumber);
							isSucceeded = true;
							verificationStatus.setEAC(VerificationStatus.Verdict.SUCCEEDED, "EAC succeeded, CA reference is: " + caReference);
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
		if (!isKeyFound) {
			throw new CardServiceException("EAC not performed. No key found.");
		}
		if (!isSucceeded) {
			throw new CardServiceException("EAC failed");
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

	/** Checks whether BAC was used. */
	private void verifyBAC() {
		if (bacKeySpec != null) {
			verificationStatus.setBAC(VerificationStatus.Verdict.SUCCEEDED, "BAC succeeded with key " + bacKeySpec);
		} else {
			verificationStatus.setBAC(VerificationStatus.Verdict.NOT_PRESENT, "BAC was not used");
		}
	}

	/** Checks whether EAC was used. */
	private void verifyEAC() {
		if (!hasEACSupport) {
			verificationStatus.setEAC(VerificationStatus.Verdict.NOT_PRESENT, "EAC not present");
		}
		/* NOTE: If EAC was performed, verification status already updated! */
	}

	/** Check active authentication. */
	private void verifyAA(PassportService service, DG15File dg15) {
		if (dg15 == null || service == null) {
			verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "AA failed");
		}
		try {
			PublicKey pubKey = dg15.getPublicKey();
			if (service.doAA(pubKey)) {
				verificationStatus.setAA(VerificationStatus.Verdict.SUCCEEDED, "AA succeeded");
			} else {
				verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "AA failed due to signature failure");
			}
		} catch (CardServiceException cse) {
			cse.printStackTrace();
			verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "AA failed due to exception");
		} catch (Exception e) {
			LOGGER.severe("DEBUG: this exception wasn't caught in verification logic (< 0.4.8) -- MO 3. Type is " + e.getClass().getCanonicalName());
			e.printStackTrace();
			verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "AA failed due to exception");
		}
	}

	/**
	 *  Jeroen van Beek sanity check.
	 */
	private boolean verifyCOMSOd(LDS lds, VerificationStatus verificationStatus) {
		COMFile com = lds.getCOMFile();
		SODFile sod = lds.getSODFile();

		List<Integer> comDGList = getCOMDGList(com);
		List<Integer> sodDGList = getSODDGList(sod);
		if (!sodDGList.equals(comDGList)) {
			LOGGER.warning("Found mismatch between EF.COM and EF.SOd:\n"
					+ "datagroups reported in SOd = " + sodDGList + "\n"
					+ "datagroups reported in COM = " + comDGList);
			verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "Mismatch between DG lists in EF.COM and EF.SOd");
			return false; /* NOTE: Serious enough to not perform other checks, leave method. */
		}

		return true;
	}

	/**
	 * Checks the security object's signature.
	 * 
	 * TODO: Check the cert stores (notably PKD) to fetch document signer certificate (if not embedded in SOd) and check its validity before checking the signature.
	 */
	private void verifyDS() {
		try {
			verificationStatus.setDS(VerificationStatus.Verdict.UNKNOWN, "Unknown");

			SODFile sod = lds.getSODFile();

			/* Check document signing signature. */
			X509Certificate docSigningCert = sod.getDocSigningCertificate();
			if (docSigningCert == null) {
				LOGGER.warning("Could not get document signer certificate from EF.SOd");
				// FIXME: We search for it in cert stores. See note at verifyCS.
				// X500Principal issuer = sod.getIssuerX500Principal();
				// BigInteger serialNumber = sod.getSerialNumber();
			}
			if (sod.checkDocSignature(docSigningCert)) {
				verificationStatus.setDS(VerificationStatus.Verdict.SUCCEEDED, "Signature checked");
			} else {
				verificationStatus.setDS(VerificationStatus.Verdict.FAILED, "Signature incorrect");
			}
		} catch (NoSuchAlgorithmException nsae) {
			verificationStatus.setDS(VerificationStatus.Verdict.FAILED, "Unsupported signature algorithm");
			return; /* NOTE: Serious enough to not perform other checks, leave method. */
		} catch (Exception e) {
			e.printStackTrace();
			verificationStatus.setDS(VerificationStatus.Verdict.FAILED, "Unexpected exception");
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
				verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "Unable to build certificate chain");
				return;
			}

			int chainDepth = chainCertificates.size();
			if (chainDepth < 1) {
				verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "Could not find certificate in stores to check target certificate. Chain depth = " + chainDepth);
				return;
			}

			/* FIXME: This is no longer necessary after PKIX has done its job? */
			if (chainDepth == 1) {
				// X509Certificate docSigningCertificate = (X509Certificate)chainCertificates.get(0);
				verificationStatus.setCS(VerificationStatus.Verdict.SUCCEEDED, "Document signer from store");
			} else if (chainDepth == 2) {
				X509Certificate docSigningCertificate = (X509Certificate)chainCertificates.get(0);
				X509Certificate countrySigningCertificate = (X509Certificate)chainCertificates.get(1);
				docSigningCertificate.verify(countrySigningCertificate.getPublicKey());
				verificationStatus.setCS(VerificationStatus.Verdict.SUCCEEDED, "Signature checked"); /* NOTE: No exception... verification succeeded! */
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.warning("CSCA certificate check failed!" + e.getMessage());
			verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "Signature failed");
		}
	}

	/**
	 * Checks hashes in the SOd correspond to hashes we compute.
	 * 
	 * TODO: return more status: which DGs have mismatches, which DGs could not be read, etc. Currently reason is abused for this (and checking stops after first mismatch).
	 * 
	 * @param lds
	 * @param verificationStatus
	 * 
	 * @return whether an error was found (could be reason to abort the verification as a whole)
	 */
	private boolean verifyHashes(LDS lds, VerificationStatus verificationStatus) {
		SODFile sod = lds.getSODFile();

		/* Initialize hash. */
		MessageDigest digest = null;
		String digestAlgorithm = sod.getDigestAlgorithm();
		LOGGER.info("Using hash algorithm " + digestAlgorithm);
		try {
			if (Security.getAlgorithms("MessageDigest").contains(digestAlgorithm)) {
				digest = MessageDigest.getInstance(digestAlgorithm);
			} else {
				digest = MessageDigest.getInstance(digestAlgorithm, BC_PROVIDER);
			}
		} catch (NoSuchAlgorithmException nsae) {
			verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "Unsupported algorithm \"" + digestAlgorithm + "\"");
		}

		/* Compare stored hashes to computed hashes. */
		Map<Integer, byte[]> hashes = sod.getDataGroupHashes();
		for (int dgNumber: hashes.keySet()) {
			short fid = LDSFileUtil.lookupFIDByTag(LDSFileUtil.lookupTagByDataGroupNumber(dgNumber));
			byte[] storedHash = hashes.get(dgNumber);

			digest.reset();

			byte[] dgBytes = null;
			InputStream dgIn = null;
			Exception ex = null;
			try {
				dgBytes = new byte[lds.getLength(fid)];
				dgIn = lds.getInputStream(fid);
				DataInputStream dgDataIn = new DataInputStream(dgIn);
				dgDataIn.readFully(dgBytes);
			} catch(Exception e) {
				dgIn = null;
				ex = e;
			}

			if (dgIn == null && hasEACSupport && (verificationStatus.getEAC() != VerificationStatus.Verdict.SUCCEEDED) && (fid == PassportService.EF_DG3 || fid == PassportService.EF_DG4)) {
				LOGGER.warning("Skipping DG" + dgNumber + " during DS verification because EAC failed.");
				continue;
			}
			if (dgIn == null) {
				LOGGER.warning("Skipping DG" + dgNumber + " during DS verification because file could not be read.");
				continue;
			}
			if (ex != null) {
				verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "DG" + dgNumber + " failed due to exception");
				return false;
			}

			try {
				byte[] computedHash = digest.digest(dgBytes);

				if (!Arrays.equals(storedHash, computedHash)) {
					verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "DG" + dgNumber + " hash mismatch");
				}
			} catch (Exception ioe) {
				verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "DG" + dgNumber + " hash failed due to exception");
				return false;
			}
		}

		if (verificationStatus.getHT().equals(VerificationStatus.Verdict.UNKNOWN)) {
			verificationStatus.setHT(VerificationStatus.Verdict.SUCCEEDED, "Hashes are identical");
		}
		return true;
	}

	private List<Integer> getCOMDGList(COMFile com) {
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
		return comDGList;
	}

	private List<Integer> getSODDGList(SODFile sod) {
		Map<Integer, byte[]> hashes = sod.getDataGroupHashes();
		List<Integer> sodDGList = new ArrayList<Integer>(hashes.keySet());
		Collections.sort(sodDGList);
		return sodDGList;
	}
}
