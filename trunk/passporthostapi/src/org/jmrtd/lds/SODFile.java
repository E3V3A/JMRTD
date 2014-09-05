/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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
 * $Id: SODFile.java 1491 2013-02-19 22:03:36Z martijno $
 */

package org.jmrtd.lds;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLSet;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.icao.DataGroupHash;
import org.bouncycastle.asn1.icao.LDSSecurityObject;
import org.bouncycastle.asn1.icao.LDSVersionInfo;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.jmrtd.JMRTDSecurityProvider;

/**
 * File structure for the EF_SOD file (the Document Security Object).
 * Based on Appendix 3 of Doc 9303 Part 1 Vol 2.
 * 
 * Basically the Document Security Object is a SignedData type as specified in
 * <a href="http://www.ietf.org/rfc/rfc3369.txt">RFC 3369</a>.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 1491 $
 */
public class SODFile extends DataGroup { /* FIXME: strictly speaking this is not a DataGroup, consider changing the name of the DataGroup class. */

	private static final long serialVersionUID = -1081347374739311111L;

	//	private static final String SHA1_HASH_ALG_OID = "1.3.14.3.2.26";
	//	private static final String SHA1_WITH_RSA_ENC_OID = "1.2.840.113549.1.1.5";
	//	private static final String SHA256_HASH_ALG_OID = "2.16.840.1.101.3.4.2.1";
	//	private static final String E_CONTENT_TYPE_OID = "1.2.528.1.1006.1.20.1";

	/**
	 * OID to indicate content-type in encapContentInfo.
	 * 
	 * <pre>
	 * id-icao-ldsSecurityObject OBJECT IDENTIFIER ::= 
	 *    {joint-iso-itu-t(2) international-organizations(23) icao(136) mrtd(1) security(1) ldsSecurityObject(1)}
	 * </pre>
	 */
	private static final String ICAO_LDS_SOD_OID = "2.23.136.1.1.1";

	/**
	 * This TC_SOD_IOD is apparently used in
	 * "PKI for Machine Readable Travel Documents Offering ICC Read-Only Access Version - 1.1, Annex C".
	 * Seen in live French and Belgian MRTDs.
	 * 
	 * <pre>
	 * id-icao-ldsSecurityObjectid OBJECT IDENTIFIER ::=
	 *    {iso(1) identified-organization(3) icao(27) atn-end-system-air(1) security(1) ldsSecurityObject(1)}
	 * </pre>
	 */
	private static final String ICAO_LDS_SOD_ALT_OID = "1.3.27.1.1.1";

	/**
	 * This is used in some test MRTDs.
	 * Appears to have been included in a "worked example" somewhere and perhaps used in live documents.
	 * 
	 * <pre>
	 * id-sdu-ldsSecurityObjectid OBJECT IDENTIFIER :=
	 *    {iso(1) member-body(2) nl(528) nederlandse-organisatie(1) enschede-sdu(1006) 1 20 1}
	 * </pre>
	 */
	private static final String SDU_LDS_SOD_OID = "1.2.528.1.1006.1.20.1";

	private static final String
	RFC_3369_SIGNED_DATA_OID = "1.2.840.113549.1.7.2",		/* id-signedData OBJECT IDENTIFIER ::= { iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs7(7) 2 } */
	RFC_3369_CONTENT_TYPE_OID = "1.2.840.113549.1.9.3",
	RFC_3369_MESSAGE_DIGEST_OID = "1.2.840.113549.1.9.4",
	PKCS1_RSA_OID = "1.2.840.113549.1.1.1",
	PKCS1_MD2_WITH_RSA_OID = "1.2.840.113549.1.1.2",
	PKCS1_MD4_WITH_RSA_OID = "1.2.840.113549.1.1.3",
	PKCS1_MD5_WITH_RSA_OID = "1.2.840.113549.1.1.4",
	PKCS1_SHA1_WITH_RSA_OID = "1.2.840.113549.1.1.5",
//	PKCS1_RSAOAEP_ENC_SET = "1.2.840.113549.1.1.6", // other identifier: ripemd160WithRSAEncryption
//	PKCS1_RSAES_OAEP = "1.2.840.113549.1.1.7",
	PKCS1_SHA256_WITH_RSA_AND_MGF1 = "1.2.840.113549.1.1.8",
	PKCS1_RSASSA_PSS_OID = "1.2.840.113549.1.1.10",
	PKCS1_SHA256_WITH_RSA_OID = "1.2.840.113549.1.1.11",
	PKCS1_SHA384_WITH_RSA_OID = "1.2.840.113549.1.1.12",
	PKCS1_SHA512_WITH_RSA_OID = "1.2.840.113549.1.1.13",
	PKCS1_SHA224_WITH_RSA_OID = "1.2.840.113549.1.1.14",
	X9_SHA1_WITH_ECDSA_OID = "1.2.840.10045.4.1",
	X9_SHA224_WITH_ECDSA_OID = "1.2.840.10045.4.3.1",
	X9_SHA256_WITH_ECDSA_OID = "1.2.840.10045.4.3.2",
	IEEE_P1363_SHA1_OID = "1.3.14.3.2.26";

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd.lds");

	private SignedData signedData;

	/**
	 * Constructs a Security Object data structure.
	 *
	 * @param digestAlgorithm a digest algorithm, such as "SHA-1" or "SHA-256"
	 * @param digestEncryptionAlgorithm a digest encryption algorithm, such as "SHA256withRSA"
	 * @param dataGroupHashes maps datagroup numbers (1 to 16) to hashes of the data groups
	 * @param encryptedDigest ???
	 * @param docSigningCertificate the document signing certificate
	 * 
	 * @throws NoSuchAlgorithmException if either of the algorithm parameters is not recognized
	 * @throws CertificateException if the document signing certificate cannot be used
	 */
	public SODFile(String digestAlgorithm, String digestEncryptionAlgorithm,
			Map<Integer, byte[]> dataGroupHashes,
			byte[] encryptedDigest,
			X509Certificate docSigningCertificate)
					throws NoSuchAlgorithmException, CertificateException {
		super(EF_SOD_TAG);
		try {
			signedData = createSignedData(digestAlgorithm,
					digestEncryptionAlgorithm,
					dataGroupHashes,
					encryptedDigest,
					docSigningCertificate);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			LOGGER.severe("Error creating signedData: " + ioe.getMessage());
			throw new IllegalArgumentException(ioe.getMessage());
		}
	}

	/**
	 * Constructs a Security Object data structure using a specified signature provider.
	 *
	 * @param digestAlgorithm a digest algorithm, such as "SHA-1" or "SHA-256"
	 * @param digestEncryptionAlgorithm a digest encryption algorithm, such as "SHA256withRSA"
	 * @param dataGroupHashes maps datagroup numbers (1 to 16) to hashes of the data groups
	 * @param privateKey private key to sign the data
	 * @param docSigningCertificate the document signing certificate
	 * @param provider specific signature provider that should be used to create the signature 
	 * 
	 * @throws NoSuchAlgorithmException if either of the algorithm parameters is not recognized
	 * @throws CertificateException if the document signing certificate cannot be used
	 */
	public SODFile(String digestAlgorithm, String digestEncryptionAlgorithm,
			Map<Integer, byte[]> dataGroupHashes,
			PrivateKey privateKey,
			X509Certificate docSigningCertificate, String provider)
					throws NoSuchAlgorithmException, CertificateException {
		super(EF_SOD_TAG);
		try {
			signedData = createSignedData(digestAlgorithm,
					digestEncryptionAlgorithm,
					dataGroupHashes,
					privateKey,
					docSigningCertificate, provider);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			LOGGER.severe("Error creating signedData: " + ioe.getMessage());
			throw new IllegalArgumentException(ioe.getMessage());
		}
	}

	/**
	 * Constructs a Security Object data structure using a specified signature provider.
	 *
	 * @param digestAlgorithm a digest algorithm, such as "SHA-1" or "SHA-256"
	 * @param digestEncryptionAlgorithm a digest encryption algorithm, such as "SHA256withRSA"
	 * @param dataGroupHashes maps datagroup numbers (1 to 16) to hashes of the data groups
	 * @param privateKey private key to sign the data
	 * @param docSigningCertificate the document signing certificate
	 * @param provider specific signature provider that should be used to create the signature
	 *
	 * @throws NoSuchAlgorithmException if either of the algorithm parameters is not recognized
	 * @throws CertificateException if the document signing certificate cannot be used
	 */
	public SODFile(String digestAlgorithm, String digestEncryptionAlgorithm,
			Map<Integer, byte[]> dataGroupHashes,
			PrivateKey privateKey,
			X509Certificate docSigningCertificate, String provider,
			String ldsVersion, String unicodeVersion)
					throws NoSuchAlgorithmException, CertificateException {
		super(EF_SOD_TAG);
		try {
			signedData = createSignedData(digestAlgorithm,
					digestEncryptionAlgorithm,
					dataGroupHashes,
					privateKey,
					docSigningCertificate, provider, ldsVersion, unicodeVersion);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			LOGGER.severe("Error creating signedData: " + ioe.getMessage());
			throw new IllegalArgumentException(ioe.getMessage());
		}
	}

	/**
	 * Constructs a Security Object data structure.
	 *
	 * @param digestAlgorithm a digest algorithm, such as "SHA1" or "SHA256"
	 * @param digestEncryptionAlgorithm a digest encryption algorithm, such as "SHA256withRSA"
	 * @param dataGroupHashes maps datagroup numbers (1 to 16) to hashes of the data groups
	 * @param privateKey private key to sign the data
	 * @param docSigningCertificate the document signing certificate
	 * 
	 * @throws NoSuchAlgorithmException if either of the algorithm parameters is not recognized
	 * @throws CertificateException if the document signing certificate cannot be used
	 */
	public SODFile(String digestAlgorithm, String digestEncryptionAlgorithm,
			Map<Integer, byte[]> dataGroupHashes,
			PrivateKey privateKey,
			X509Certificate docSigningCertificate)
					throws NoSuchAlgorithmException, CertificateException {
		super(EF_SOD_TAG);
		try {
			signedData = createSignedData(digestAlgorithm,
					digestEncryptionAlgorithm,
					dataGroupHashes,
					privateKey,
					docSigningCertificate, null);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			LOGGER.severe("Error creating signedData: " + ioe.getMessage());
			throw new IllegalArgumentException(ioe.getMessage());
		}
	}

	/**
	 * Constructs a Security Object data structure.
	 *
	 * @param inputStream some inputstream
	 * 
	 * @throws IOException if something goes wrong
	 */
	public SODFile(InputStream inputStream) throws IOException {
		super(EF_SOD_TAG, inputStream);
	}

	protected void readContent(InputStream inputStream) throws IOException {
		ASN1InputStream asn1in = new ASN1InputStream(inputStream);
		ASN1Sequence sequence = (ASN1Sequence)asn1in.readObject();
		if (sequence.size() != 2) {
			throw new IOException("Was expecting a DER sequence of length 2, found a DER sequence of length " + sequence.size());
		}
		String contentTypeOID = ((ASN1ObjectIdentifier)sequence.getObjectAt(0)).getId();
		if (!RFC_3369_SIGNED_DATA_OID.equals(contentTypeOID)) {
			throw new IOException("Was expecting signed-data content type OID (" + RFC_3369_SIGNED_DATA_OID + "), found " + contentTypeOID);
		}
		ASN1Primitive content = null;
		int tagNo = -1;
		ASN1Encodable asn1Encodable = sequence.getObjectAt(1);
		
		/*
		 * Most EU passports have DERTaggedObject,
		 * New Zealand has BERTaggedObject,
		 * cast problems between BER and DER since (at least) BC 1.47...
		 * Thanks to Nick von Dadelszen for helping out with debugging.
		 */
		if (asn1Encodable instanceof DERTaggedObject) {
			DERTaggedObject derTaggedObject = (DERTaggedObject)asn1Encodable;
			tagNo = derTaggedObject.getTagNo();
			content = derTaggedObject.getObject();
		} else if (asn1Encodable instanceof BERTaggedObject) {
			BERTaggedObject berTaggedObject = (BERTaggedObject)asn1Encodable;
			tagNo = berTaggedObject.getTagNo();
			content = berTaggedObject.getObject();
		} else if (asn1Encodable instanceof ASN1TaggedObject) {
			DLTaggedObject dlTaggedObject = (DLTaggedObject)asn1Encodable;
			tagNo = dlTaggedObject.getTagNo();
			content = dlTaggedObject.getObject();			
		} else if (asn1Encodable instanceof ASN1TaggedObject) {
			ASN1TaggedObject asn1TaggedObject = (ASN1TaggedObject)asn1Encodable;
			tagNo = asn1TaggedObject.getTagNo();
			content = asn1TaggedObject.getObject();			
		} else {
			throw new IOException("Was expecting an ASN1TaggedObject, found " + asn1Encodable.getClass().getCanonicalName());
		}
		if (tagNo != 0) {
			throw new IOException("Was expecting tag 0, found " + Integer.toHexString(tagNo));
		}		
		if (!(content instanceof ASN1Sequence)) {
			throw new IOException("Was expecting an ASN.1 sequence as content");
		}
		this.signedData = SignedData.getInstance((ASN1Sequence)content);
	}

	protected void writeContent(OutputStream out) throws IOException {
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1ObjectIdentifier(RFC_3369_SIGNED_DATA_OID));
		v.add(new DERTaggedObject(0, signedData));
		ASN1Sequence fileContentsObject = new DLSequence(v);
		byte[] fileContentsBytes = fileContentsObject.getEncoded(ASN1Encoding.DER);
		out.write(fileContentsBytes);
	}

	/**
	 * Gets the stored data group hashes.
	 *
	 * @return data group hashes indexed by data group numbers (1 to 16)
	 */
	public Map<Integer, byte[]> getDataGroupHashes() {
		DataGroupHash[] hashObjects = getLDSSecurityObject(signedData).getDatagroupHash();
		Map<Integer, byte[]> hashMap = new TreeMap<Integer, byte[]>(); /* HashMap... get it? :D (not funny anymore, now that it's a TreeMap.) */
		for (int i = 0; i < hashObjects.length; i++) {
			DataGroupHash hashObject = hashObjects[i];
			int number = hashObject.getDataGroupNumber();
			byte[] hashValue = hashObject.getDataGroupHashValue().getOctets();
			hashMap.put(number, hashValue);
		}
		return hashMap;
	}

	/**
	 * Gets the signature (the encrypted digest) over the hashes.
	 *
	 * @return the encrypted digest
	 */
	public byte[] getEncryptedDigest() {
		return getEncryptedDigest(signedData);
	}

	/**
	 * Gets the e-content inside the signed data strucure.
	 * 
	 * @return the e-content
	 */
	public byte[] getEContent() {
		return getEContent(signedData);
	}

	/**
	 * Gets the name of the algorithm used in the data group hashes.
	 * 
	 * @return an algorithm string such as "SHA-1" or "SHA-256"
	 */
	public String getDigestAlgorithm() {
		try {
			return lookupMnemonicByOID(getLDSSecurityObject(signedData).getDigestAlgorithmIdentifier().getAlgorithm().getId());      
		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
			throw new IllegalStateException(nsae.toString());
		}
	}

	/**
	 * Gets the name of the algorithm used in the signature.
	 * 
	 * @return an algorithm string such as "SHA256withRSA"
	 */
	public String getDigestEncryptionAlgorithm() {
		try {
			return lookupMnemonicByOID(getSignerInfo(signedData).getDigestEncryptionAlgorithm().getAlgorithm().getId());      
		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
			throw new IllegalStateException(nsae.toString());
		}
	}

	/**
	 * Gets the version of the LDS if stored in the Security Object (SOd).
	 *
	 * @return the version of the LDS in "aabb" format or null if LDS &lt; V1.8
	 *
	 * @since LDS V1.8
	 */
	public String getLDSVersion() {
		LDSVersionInfo ldsVersionInfo = getLDSSecurityObject(signedData).getVersionInfo();
		if (ldsVersionInfo == null) {
			return null;
		} else {
			return ldsVersionInfo.getLdsVersion();
		}
	}

	/**
	 * Gets the version of unicode if stored in the Security Object (SOd).
	 *
	 * @return the unicode version in "aabbcc" format or null if LDS &lt; V1.8
	 * 
	 * @since LDS V1.8
	 */
	public String getUnicodeVersion() {
		LDSVersionInfo ldsVersionInfo = getLDSSecurityObject(signedData).getVersionInfo();
		if (ldsVersionInfo == null) {
			return null;
		} else {
			return ldsVersionInfo.getUnicodeVersion();
		}
	}

	/**
	 * Gets the embedded document signing certificate (if present).
	 * Use this certificate to verify that <i>eSignature</i> is a valid
	 * signature for <i>eContent</i>. This certificate itself is signed
	 * using the country signing certificate.
	 *
	 * @return the document signing certificate
	 */
	public X509Certificate getDocSigningCertificate() throws CertificateException {
		byte[] certSpec = null;
		ASN1Set certs = signedData.getCertificates();
		if (certs == null || certs.size() <= 0) { return null; }
		if (certs.size() != 1) {
			LOGGER.warning("Found " + certs.size() + " certificates");
		}
		X509CertificateObject certObject = null;
		for (int i = 0; i < certs.size(); i++) {
			org.bouncycastle.asn1.x509.Certificate certAsASN1Object = org.bouncycastle.asn1.x509.Certificate.getInstance((ASN1Sequence)certs.getObjectAt(i));
			certObject = new X509CertificateObject(certAsASN1Object); // NOTE: >= BC 1.48
//			certObject = new X509CertificateObject(X509CertificateStructure.getInstance(certAsASN1Object)); // NOTE: <= BC 1.47
			certSpec = certObject.getEncoded();
		}

		/*
		 * NOTE: we could have just returned that X509CertificateObject here,
		 * but by reconstructing it using the client's default provider we hide
		 * the fact that we're using BC.
		 */
		try {
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(certSpec));
			return cert;
		} catch (Exception e) {
			/* NOTE: Reconstructing using preferred provider didn't work?!?! */
			return certObject;
		}
	}

	/**
	 * Verifies the signature over the contents of the security object.
	 * Clients can also use the accessors of this class and check the
	 * validity of the signature for themselves.
	 * 
	 * See RFC 3369, Cryptographic Message Syntax, August 2002,
	 * Section 5.4 for details.
	 * 
	 * @param docSigningCert the certificate to use
	 *        (should be X509 certificate)
	 * 
	 * @return status of the verification
	 * 
	 * @throws GeneralSecurityException if something goes wrong
	 */
	public boolean checkDocSignature(Certificate docSigningCert)
			throws GeneralSecurityException {

		byte[] eContent = getEContent(signedData);
		byte[] signature = getEncryptedDigest(signedData);

		String encAlgId = getSignerInfo(signedData).getDigestEncryptionAlgorithm().getAlgorithm().getId();
		String encAlgJavaString = lookupMnemonicByOID(encAlgId);

		// For the cases where the signature is simply a digest (haven't seen a passport like this, 
		// thus this is guessing)

		if (encAlgId == null) {
			String digestAlg = getSignerInfo(signedData).getDigestAlgorithm().getAlgorithm().getId();
			MessageDigest digest = null;
			try {
				digest = MessageDigest.getInstance(digestAlg);
			} catch (Exception e) {
				digest = MessageDigest.getInstance(digestAlg, BC_PROVIDER);
			}
			digest.update(eContent);
			byte[] digestBytes = digest.digest();
			return Arrays.equals(digestBytes, signature);
		}

		/* For the RSA_SA_PSS
		 *    1. the default hash is SHA1,
		 *    2. The hash id is not encoded in OID
		 * So it has to be specified "manually".
		 */
		if (PKCS1_RSASSA_PSS_OID.equals(encAlgId)) {
			String digestJavaString = lookupMnemonicByOID(getSignerInfo(signedData).getDigestAlgorithm().getAlgorithm().getId());
			encAlgJavaString = digestJavaString.replace("-", "") + "withRSA/PSS";
		}

		if (PKCS1_RSA_OID.equals(encAlgId)) {
			String digestJavaString = lookupMnemonicByOID(getSignerInfo(signedData).getDigestAlgorithm().getAlgorithm().getId());
			encAlgJavaString = digestJavaString.replace("-", "") + "withRSA";
		}

		LOGGER.info("OID = " + encAlgId);
		LOGGER.info("encAlgJavaString = " + encAlgJavaString);

		Signature sig = null;
		try {
			sig = Signature.getInstance(encAlgJavaString);
		} catch (Exception e) {
			/* FIXME: Warn client that they should perhaps add BC as provider? */
			sig = Signature.getInstance(encAlgJavaString, BC_PROVIDER);
		}
		sig.initVerify(docSigningCert);
		sig.update(eContent);
		return sig.verify(signature);

		// 2. Do it manually, decrypt the signature and extract the hashing algorithm
		/*
		try {
			Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			c.init(Cipher.DECRYPT_MODE, docSigningCert);
			c.update(signature);
			byte[] decryptedBytes = c.doFinal();
			String id = getHashId(decryptedBytes);
			byte[] expectedHash = getHashBytes(decryptedBytes);
			MessageDigest digest = MessageDigest.getInstance(id);
			digest.update(eContent);
			byte[] digestBytes = digest.digest();
			result = Arrays.equals(digestBytes, expectedHash);
		}catch(Exception e) {

		}
        String[] sigAlgs = new String[] {"SHA1withRSA", "SHA1withRSA/PSS", "SHA256withRSA", "SHA256withRSA/PSS"};
		 */
	}

	/**
	 * Gets the issuer of the document signing certificate.
	 * 
	 * @return a certificate issuer
	 */
	public X500Principal getIssuerX500Principal() {
		try {
			IssuerAndSerialNumber issuerAndSerialNumber = getIssuerAndSerialNumber();
			X500Name name = issuerAndSerialNumber.getName();
			X500Principal x500Principal = new X500Principal(name.getEncoded(ASN1Encoding.DER));		
			return x500Principal;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			LOGGER.severe("Could not get issuer: " + ioe.getMessage());
			return null;
		}
	}

	/**
	 * Gets the serial number of the document signing certificate.
	 * 
	 * @return a certificate serial number
	 */
	public BigInteger getSerialNumber() {
		IssuerAndSerialNumber issuerAndSerialNumber = getIssuerAndSerialNumber();
		BigInteger serialNumber = issuerAndSerialNumber.getSerialNumber().getValue();
		return serialNumber;
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		try {
			X509Certificate cert = getDocSigningCertificate();
			return "SODFile " + cert.getIssuerX500Principal();
		} catch (Exception e) {
			return "SODFile";
		}
	}

	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (!obj.getClass().equals(this.getClass())) { return false; }
		SODFile other = (SODFile)obj;
		return Arrays.equals(getEncoded(), other.getEncoded());
	}

	public int hashCode() {
		return 11 * Arrays.hashCode(getEncoded()) + 111;
	}

	/* ONLY PRIVATE METHODS BELOW */

	private static SignerInfo getSignerInfo(SignedData signedData)  {
		ASN1Set signerInfos = signedData.getSignerInfos();
		if (signerInfos.size() > 1) {
			LOGGER.warning("Found " + signerInfos.size() + " signerInfos");
		}
		for (int i = 0; i < signerInfos.size(); i++) {
			SignerInfo info = new SignerInfo((ASN1Sequence)signerInfos.getObjectAt(i));
			return info;
		}
		return null;
	}

	/**
	 * Reads the security object (containing the hashes
	 * of the data groups) found in the SignedData field.
	 * 
	 * @return the security object
	 * 
	 * @throws IOException
	 */
	private static LDSSecurityObject getLDSSecurityObject(SignedData signedData) {
		try {
			ContentInfo encapContentInfo = signedData.getEncapContentInfo();
			String contentType = encapContentInfo.getContentType().getId();
			DEROctetString eContent = (DEROctetString)encapContentInfo.getContent();
			if (!(ICAO_LDS_SOD_OID.equals(contentType)
				|| SDU_LDS_SOD_OID.equals(contentType)
				|| ICAO_LDS_SOD_ALT_OID.equals(contentType))) {
				LOGGER.warning("SignedData does not appear to contain an LDS SOd. (content type is " + contentType + ", was expecting " + ICAO_LDS_SOD_OID + ")");
			}
			ASN1InputStream inputStream = new ASN1InputStream(new ByteArrayInputStream(eContent.getOctets()));

			Object firstObject = inputStream.readObject();
			if (!(firstObject instanceof ASN1Sequence)) {
				throw new IllegalStateException("Expected ASN1Sequence, found " + firstObject.getClass().getSimpleName());
			}
			LDSSecurityObject sod = LDSSecurityObject.getInstance(firstObject);
			Object nextObject = inputStream.readObject();
			if (nextObject != null) {
				LOGGER.warning("Ignoring extra object found after LDSSecurityObject...");
			}
			return sod;
			} catch (IOException ioe) {
				throw new IllegalStateException("Could not read security object in signedData");
			}
		}

		/**
		 * Gets the contents of the security object over which the
		 * signature is to be computed. 
		 * 
		 * See RFC 3369, Cryptographic Message Syntax, August 2002,
		 * Section 5.4 for details.
		 * 
		 * FIXME: Maybe throw an exception instead of issuing warnings
		 * on stderr if signed attributes don't check out.
		 *
		 * @see #getDocSigningCertificate()
		 * @see #getSignature()
		 * 
		 * @return the contents of the security object over which the
		 *         signature is to be computed
		 */
		private static byte[] getEContent(SignedData signedData) {
			SignerInfo signerInfo = getSignerInfo(signedData);
			ASN1Set signedAttributesSet = signerInfo.getAuthenticatedAttributes();

			ContentInfo contentInfo = signedData.getEncapContentInfo();
			byte[] contentBytes = ((DEROctetString)contentInfo.getContent()).getOctets();

			if (signedAttributesSet.size() == 0) {
				/* Signed attributes absent, return content to be signed... */
				return contentBytes;
			} else {
				/* Signed attributes present (i.e. a structure containing a hash of the content), return that structure to be signed... */
				/* This option is taken by ICAO passports. */
				byte[] attributesBytes = null;
				String digAlg = signerInfo.getDigestAlgorithm().getAlgorithm().getId();
				try {
					attributesBytes = signedAttributesSet.getEncoded(ASN1Encoding.DER);

					/* We'd better check that the content actually digests to the hash value contained! ;) */
					Enumeration<?> attributes = signedAttributesSet.getObjects();
					byte[] storedDigestedContent = null;
					while (attributes.hasMoreElements()) {
						Attribute attribute = Attribute.getInstance((ASN1Sequence)attributes.nextElement());
						ASN1ObjectIdentifier attrType = attribute.getAttrType();
						if (RFC_3369_MESSAGE_DIGEST_OID.equals(attrType.getId())) {
							ASN1Set attrValuesSet = attribute.getAttrValues();
							if (attrValuesSet.size() != 1) {
								LOGGER.warning("Expected only one attribute value in signedAttribute message digest in eContent!");
							}
							storedDigestedContent = ((DEROctetString)attrValuesSet.getObjectAt(0)).getOctets();
						}
					}
					if (storedDigestedContent == null) {
						LOGGER.warning("Error extracting signedAttribute message digest in eContent!");
					}	
					MessageDigest dig = MessageDigest.getInstance(digAlg);
					byte[] computedDigestedContent = dig.digest(contentBytes);
					if (!Arrays.equals(storedDigestedContent, computedDigestedContent)) {
						LOGGER.warning("Error checking signedAttribute message digest in eContent!");
					}
				} catch (NoSuchAlgorithmException nsae) {
					nsae.printStackTrace();
					LOGGER.warning("Error checking signedAttributes in eContent! No such algorithm: \"" + digAlg + "\"");
				} catch (IOException ioe) {
					ioe.printStackTrace();
					LOGGER.severe("Error getting signedAttributes: " + ioe.getMessage());
				}
				return attributesBytes;
			}
		}

		private IssuerAndSerialNumber getIssuerAndSerialNumber() {
			SignerInfo signerInfo = getSignerInfo(signedData);
			SignerIdentifier signerIdentifier = signerInfo.getSID();
			IssuerAndSerialNumber issuerAndSerialNumber = IssuerAndSerialNumber.getInstance(signerIdentifier.getId());
			X500Name issuer = issuerAndSerialNumber.getName();
			BigInteger serialNumber = issuerAndSerialNumber.getSerialNumber().getValue();
			return new IssuerAndSerialNumber(issuer, serialNumber);
		}

		/**
		 * Gets the stored signature of the security object.
		 * 
		 * @see #getDocSigningCertificate()
		 * 
		 * @return the signature
		 */
		private static byte[] getEncryptedDigest(SignedData signedData) {
			SignerInfo signerInfo = getSignerInfo(signedData);
			return signerInfo.getEncryptedDigest().getOctets();
		}

		/* METHODS BELOW ARE FOR CONSTRUCTING SOD STRUCTS */

		private static SignedData createSignedData(
				String digestAlgorithm,
				String digestEncryptionAlgorithm,
				Map<Integer, byte[]> dataGroupHashes,
				byte[] encryptedDigest,
				X509Certificate docSigningCertificate)
						throws NoSuchAlgorithmException, CertificateException, IOException {
			ASN1Set digestAlgorithmsSet = createSingletonSet(createDigestAlgorithms(digestAlgorithm));
			ContentInfo contentInfo = createContentInfo(digestAlgorithm, dataGroupHashes);
			byte[] content = ((DEROctetString)contentInfo.getContent()).getOctets();
			ASN1Set certificates =  createSingletonSet(createCertificate(docSigningCertificate));
			ASN1Set crls = null;
			ASN1Set signerInfos = createSingletonSet(createSignerInfo(digestAlgorithm, digestEncryptionAlgorithm, content, encryptedDigest, docSigningCertificate).toASN1Object());
			return new SignedData(digestAlgorithmsSet, contentInfo, certificates, crls, signerInfos);
		}

		private static SignedData createSignedData(String digestAlgorithm,
				String digestEncryptionAlgorithm,
				Map<Integer, byte[]> dataGroupHashes, PrivateKey privateKey,
				X509Certificate docSigningCertificate, String provider)
						throws NoSuchAlgorithmException, CertificateException, IOException {
			return createSignedData(digestAlgorithm, digestEncryptionAlgorithm,
					dataGroupHashes, privateKey, docSigningCertificate, provider,
					null, null);
		}

		private static SignedData createSignedData(String digestAlgorithm,
				String digestEncryptionAlgorithm,
				Map<Integer, byte[]> dataGroupHashes, PrivateKey privateKey,
				X509Certificate docSigningCertificate, String provider,
				String ldsVersion, String unicodeVersion)
						throws NoSuchAlgorithmException, CertificateException, IOException {
			ASN1Set digestAlgorithmsSet = createSingletonSet(createDigestAlgorithms(digestAlgorithm));
			ContentInfo contentInfo = createContentInfo(digestAlgorithm,
					dataGroupHashes, ldsVersion, unicodeVersion);
			byte[] content = ((DEROctetString) contentInfo.getContent())
					.getOctets();

			byte[] encryptedDigest = null;
			try {
				byte[] dataToBeSigned = createAuthenticatedAttributes(digestAlgorithm, content).getEncoded(ASN1Encoding.DER);
				Signature s = null;
				if (provider != null) {
					s = Signature.getInstance(digestEncryptionAlgorithm, provider);            	
				} else {
					s = Signature.getInstance(digestEncryptionAlgorithm);            	
				}
				s.initSign(privateKey);
				s.update(dataToBeSigned);
				encryptedDigest = s.sign();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			ASN1Set certificates = createSingletonSet(createCertificate(docSigningCertificate));
			ASN1Set crls = null;
			ASN1Set signerInfos = createSingletonSet(createSignerInfo(
					digestAlgorithm, digestEncryptionAlgorithm, content,
					encryptedDigest, docSigningCertificate).toASN1Object());
			return new SignedData(digestAlgorithmsSet, contentInfo, certificates,
					crls, signerInfos);
		}

		private static ASN1Sequence createDigestAlgorithms(String digestAlgorithm) throws NoSuchAlgorithmException {
			ASN1ObjectIdentifier algorithmIdentifier = new ASN1ObjectIdentifier(lookupOIDByMnemonic(digestAlgorithm));
			//		ASN1Primitive[] result = { algorithmIdentifier };
			ASN1EncodableVector v = new ASN1EncodableVector();
			v.add(algorithmIdentifier);
			return new DLSequence(v);
		}

		private static ASN1Sequence createCertificate(X509Certificate cert) throws CertificateException {
			try {
				byte[] certSpec = cert.getEncoded();
				ASN1InputStream asn1In = new ASN1InputStream(certSpec);
				try {
					ASN1Sequence certSeq = (ASN1Sequence)(asn1In).readObject();
					return certSeq;
				} finally {
					asn1In.close();
				}
			} catch (IOException ioe) {
				throw new CertificateException("Could not construct certificate byte stream");
			}
		}

		private static ContentInfo createContentInfo(
				String digestAlgorithm,
				Map<Integer, byte[]> dataGroupHashes)
						throws NoSuchAlgorithmException, IOException {
			return createContentInfo(digestAlgorithm, dataGroupHashes, null,
					null);
		}

		private static ContentInfo createContentInfo(
				String digestAlgorithm,
				Map<Integer, byte[]> dataGroupHashes,
				String ldsVersion, String unicodeVersion) throws NoSuchAlgorithmException, IOException {
			DataGroupHash[] dataGroupHashesArray = new DataGroupHash[dataGroupHashes.size()];
			int i = 0;
			for (int dataGroupNumber: dataGroupHashes.keySet()) {
				byte[] hashBytes = dataGroupHashes.get(dataGroupNumber);
				DataGroupHash hash = new DataGroupHash(dataGroupNumber, new DEROctetString(hashBytes));
				dataGroupHashesArray[i++] = hash;
			}
			AlgorithmIdentifier digestAlgorithmIdentifier = AlgorithmIdentifier.getInstance(lookupOIDByMnemonic(digestAlgorithm));
			LDSVersionInfo ldsVersionInfo;
			LDSSecurityObject securityObject = null;
			if (ldsVersion == null) {
				ldsVersionInfo = null;
				securityObject = new LDSSecurityObject(digestAlgorithmIdentifier, dataGroupHashesArray);
			} else {
				ldsVersionInfo = new LDSVersionInfo(ldsVersion, unicodeVersion);
				securityObject = new LDSSecurityObject(digestAlgorithmIdentifier, dataGroupHashesArray, ldsVersionInfo);
			}
			return new ContentInfo(new ASN1ObjectIdentifier(ICAO_LDS_SOD_OID), new DEROctetString(securityObject));
		}

		private static SignerInfo createSignerInfo(
				String digestAlgorithm,
				String digestEncryptionAlgorithm,
				byte[] content,
				byte[] encryptedDigest,
				X509Certificate docSigningCertificate)
						throws NoSuchAlgorithmException {
			/* Get the issuer name (CN, O, OU, C) from the cert and put it in a SignerIdentifier struct. */
			X500Principal docSignerPrincipal = ((X509Certificate)docSigningCertificate).getIssuerX500Principal();
			X500Name docSignerName = new X500Name(docSignerPrincipal.getName(X500Principal.RFC2253));
			BigInteger serial = ((X509Certificate)docSigningCertificate).getSerialNumber();
			SignerIdentifier sid = new SignerIdentifier(new IssuerAndSerialNumber(docSignerName, serial));

			AlgorithmIdentifier digestAlgorithmObject = new AlgorithmIdentifier(lookupOIDByMnemonic(digestAlgorithm)); 
			AlgorithmIdentifier digestEncryptionAlgorithmObject = new AlgorithmIdentifier(lookupOIDByMnemonic(digestEncryptionAlgorithm));

			ASN1Set authenticatedAttributes = createAuthenticatedAttributes(digestAlgorithm, content); // struct containing the hash of content
			ASN1OctetString encryptedDigestObject = new DEROctetString(encryptedDigest); // this is the signature
			ASN1Set unAuthenticatedAttributes = null; // should be empty set?
			return new SignerInfo(sid, digestAlgorithmObject, authenticatedAttributes, digestEncryptionAlgorithmObject, encryptedDigestObject, unAuthenticatedAttributes);
		}

		private static ASN1Set createAuthenticatedAttributes(String digestAlgorithm, byte[] contentBytes)
				throws NoSuchAlgorithmException {
			/* Check bug found by Paulo Assumpco. */
			if ("SHA256".equals(digestAlgorithm)) { digestAlgorithm = "SHA-256"; LOGGER.warning("Replaced \"SHA256\" with \"SHA-256\"."); }
			MessageDigest dig = MessageDigest.getInstance(digestAlgorithm);
			byte[] digestedContentBytes = dig.digest(contentBytes);
			ASN1OctetString digestedContent = new DEROctetString(digestedContentBytes);
			Attribute contentTypeAttribute = new Attribute(new ASN1ObjectIdentifier(RFC_3369_CONTENT_TYPE_OID), createSingletonSet(new ASN1ObjectIdentifier(ICAO_LDS_SOD_OID)));
			Attribute messageDigestAttribute = new Attribute(new ASN1ObjectIdentifier(RFC_3369_MESSAGE_DIGEST_OID), createSingletonSet(digestedContent));
			ASN1Object[] result = { contentTypeAttribute.toASN1Primitive(), messageDigestAttribute.toASN1Primitive() };
			return new DLSet(result);
		}

		private static ASN1Set createSingletonSet(ASN1Object e) {
			return new DLSet(new ASN1Encodable[] { e });
		}

		/**
		 * Gets the common mnemonic string (such as "SHA1", "SHA256withRSA") given an OID.
		 *
		 * @param oid an OID
		 *
		 * @throws NoSuchAlgorithmException if the provided OID is not yet supported
		 */
		private static String lookupMnemonicByOID(String oid) throws NoSuchAlgorithmException {
			if (oid.equals(X509ObjectIdentifiers.organization.getId())) { return "O"; }
			if (oid.equals(X509ObjectIdentifiers.organizationalUnitName.getId())) { return "OU"; }
			if (oid.equals(X509ObjectIdentifiers.commonName.getId())) { return "CN"; }
			if (oid.equals(X509ObjectIdentifiers.countryName.getId())) { return "C"; }
			if (oid.equals(X509ObjectIdentifiers.stateOrProvinceName.getId())) { return "ST"; }
			if (oid.equals(X509ObjectIdentifiers.localityName.getId())) { return "L"; }
			if(oid.equals(X509ObjectIdentifiers.id_SHA1.getId())) { return "SHA-1"; }
			if(oid.equals(NISTObjectIdentifiers.id_sha224.getId())) { return "SHA-224"; }
			if(oid.equals(NISTObjectIdentifiers.id_sha256.getId())) { return "SHA-256"; }
			if(oid.equals(NISTObjectIdentifiers.id_sha384.getId())) { return "SHA-384"; }
			if(oid.equals(NISTObjectIdentifiers.id_sha512.getId())) { return "SHA-512"; }
			if (oid.equals(X9_SHA1_WITH_ECDSA_OID)) { return "SHA1withECDSA"; }
			if (oid.equals(X9_SHA224_WITH_ECDSA_OID)) { return "SHA224withECDSA"; }
			if (oid.equals(X9_SHA256_WITH_ECDSA_OID)) { return "SHA256withECDSA"; }		
			if (oid.equals(PKCS1_RSA_OID)) { return "RSA"; }
			if (oid.equals(PKCS1_MD2_WITH_RSA_OID)) { return "MD2withRSA"; }
			if (oid.equals(PKCS1_MD4_WITH_RSA_OID)) { return "MD4withRSA"; }
			if (oid.equals(PKCS1_MD5_WITH_RSA_OID)) { return "MD5withRSA"; }
			if (oid.equals(PKCS1_SHA1_WITH_RSA_OID)) { return "SHA1withRSA"; }
			if (oid.equals(PKCS1_SHA256_WITH_RSA_OID)) { return "SHA256withRSA"; }
			if (oid.equals(PKCS1_SHA384_WITH_RSA_OID)) { return "SHA384withRSA"; }
			if (oid.equals(PKCS1_SHA512_WITH_RSA_OID)) { return "SHA512withRSA"; }
			if (oid.equals(PKCS1_SHA224_WITH_RSA_OID)) { return "SHA224withRSA"; }
			if (oid.equals(IEEE_P1363_SHA1_OID)) { return "SHA-1"; }
			if (oid.equals(PKCS1_RSASSA_PSS_OID)) { return "SSAwithRSA/PSS"; }
			if (oid.equals(PKCS1_SHA256_WITH_RSA_AND_MGF1)) { return "SHA256withRSAandMGF1"; }
			throw new NoSuchAlgorithmException("Unknown OID " + oid);
		}

		private static String lookupOIDByMnemonic(String name) throws NoSuchAlgorithmException {
			if (name.equals("O")) { return X509ObjectIdentifiers.organization.getId(); }
			if (name.equals("OU")) { return X509ObjectIdentifiers.organizationalUnitName.getId(); }
			if (name.equals("CN")) { return X509ObjectIdentifiers.commonName.getId(); }
			if (name.equals("C")) { return X509ObjectIdentifiers.countryName.getId(); }
			if (name.equals("ST")) { return X509ObjectIdentifiers.stateOrProvinceName.getId(); }
			if (name.equals("L")) { return X509ObjectIdentifiers.localityName.getId(); }
			if(name.equalsIgnoreCase("SHA-1") || name.equalsIgnoreCase("SHA1")) { return X509ObjectIdentifiers.id_SHA1.getId(); }
			if(name.equalsIgnoreCase("SHA-224") || name.equalsIgnoreCase("SHA224")) { return NISTObjectIdentifiers.id_sha224.getId(); }
			if(name.equalsIgnoreCase("SHA-256") || name.equalsIgnoreCase("SHA256")) { return NISTObjectIdentifiers.id_sha256.getId(); }
			if(name.equalsIgnoreCase("SHA-384") || name.equalsIgnoreCase("SHA384")) { return NISTObjectIdentifiers.id_sha384.getId(); }
			if(name.equalsIgnoreCase("SHA-512") || name.equalsIgnoreCase("SHA512")) { return NISTObjectIdentifiers.id_sha512.getId(); }
			if (name.equalsIgnoreCase("RSA")) { return PKCS1_RSA_OID; }
			if (name.equalsIgnoreCase("MD2withRSA")) { return PKCS1_MD2_WITH_RSA_OID; } 
			if (name.equalsIgnoreCase("MD4withRSA")) { return PKCS1_MD4_WITH_RSA_OID; } 
			if (name.equalsIgnoreCase("MD5withRSA")) { return  PKCS1_MD5_WITH_RSA_OID; }
			if (name.equalsIgnoreCase("SHA1withRSA")) { return  PKCS1_SHA1_WITH_RSA_OID; }
			if (name.equalsIgnoreCase("SHA256withRSA")) { return PKCS1_SHA256_WITH_RSA_OID; }
			if (name.equalsIgnoreCase("SHA384withRSA")) { return PKCS1_SHA384_WITH_RSA_OID; }
			if (name.equalsIgnoreCase("SHA512withRSA")) { return PKCS1_SHA512_WITH_RSA_OID; }
			if (name.equalsIgnoreCase("SHA224withRSA")) { return PKCS1_SHA224_WITH_RSA_OID; }
			if (name.equalsIgnoreCase("SHA1withECDSA")) { return X9_SHA1_WITH_ECDSA_OID; }
			if (name.equalsIgnoreCase("SHA224withECDSA")) { return X9_SHA224_WITH_ECDSA_OID; }
			if (name.equalsIgnoreCase("SHA256withECDSA")) { return X9_SHA256_WITH_ECDSA_OID; }
			if (name.equalsIgnoreCase("SAwithRSA/PSS")) { return PKCS1_RSASSA_PSS_OID; }
			if (name.equalsIgnoreCase("SSAwithRSA/PSS")) { return PKCS1_RSASSA_PSS_OID; }
			if (name.equalsIgnoreCase("RSASSA-PSS")) { return PKCS1_RSASSA_PSS_OID; }
			if (name.equalsIgnoreCase("SHA256withRSAandMGF1")) { return PKCS1_SHA256_WITH_RSA_AND_MGF1; }
			throw new NoSuchAlgorithmException("Unknown name " + name);
		}
	}
