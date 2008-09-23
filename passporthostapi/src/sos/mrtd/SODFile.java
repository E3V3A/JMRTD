/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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
 * $Id: $
 */

package sos.mrtd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.icao.DataGroupHash;
import org.bouncycastle.asn1.icao.LDSSecurityObject;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.jce.provider.X509CertificateObject;

import sos.smartcards.CardServiceException;
import sos.tlv.BERTLVInputStream;
import sos.util.Hex;

/**
 * File structure for the EF_SOD file.
 * This file contains the security object.
 * 
 * TODO: implement this without coupling the public interface of
 * this class to the Bouncy Castle classes.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class SODFile extends PassportFile
{
//	private static final String SIGNED_DATA_OID = "1.2.840.113549.1.7.2";
//	private static final String SHA1_HASH_ALG_OID = "1.3.14.3.2.26";
//	private static final String SHA1_WITH_RSA_ENC_OID = "1.2.840.113549.1.1.5";
//	private static final String SHA256_HASH_ALG_OID = "2.16.840.1.101.3.4.2.1";
//	private static final String ICAO_SOD_OID = "2.23.136.1.1.1";
//	private static final String E_CONTENT_TYPE_OID = "1.2.528.1.1006.1.20.1";
	private static final DERObjectIdentifier RFC_3369_CONTENT_TYPE_OID = new DERObjectIdentifier("1.2.840.113549.1.9.3");
	private static final DERObjectIdentifier RFC_3369_MESSAGE_DIGEST_OID = new DERObjectIdentifier("1.2.840.113549.1.9.4");
	private static final DERObjectIdentifier RSA_SA_PSS_OID = new DERObjectIdentifier("1.2.840.113549.1.1.10");
	private static final DERObjectIdentifier PKCS1_SHA256_WITH_RSA_OID = new DERObjectIdentifier("1.2.840.113549.1.1.11");
	private static final DERObjectIdentifier PKCS1_SHA384_WITH_RSA_OID = new DERObjectIdentifier("1.2.840.113549.1.1.12");
	private static final DERObjectIdentifier PKCS1_SHA512_WITH_RSA_OID = new DERObjectIdentifier("1.2.840.113549.1.1.13");
	private static final DERObjectIdentifier PKCS1_SHA224_WITH_RSA_OID = new DERObjectIdentifier("1.2.840.113549.1.1.14");

	
	
	private SignedData signedData;

	/**
	 * Constructs a Security Object file.
	 *
	 * @param digestAlgorithm a digest algorithm, such as "SHA1" or "SHA256"
	 * @param dataGroupHashes maps datagroupnumbers (1 to 16) to hashes of the data groups
	 * @param signature ???
	 * @param docSigningCertificate the document signing certificate
	 */
	public SODFile(String digestAlgorithm,
			Map<Integer, byte[]> dataGroupHashes,
			Certificate docSigningCertificate) {
		ASN1Set digestAlgorithmsSet = null;
		ContentInfo contentInfo = null;
		ASN1Set certificates =  null;
		ASN1Set crls = null;
		ASN1Set signerInfos = null;
		signedData = new SignedData(digestAlgorithmsSet, contentInfo, certificates, crls, signerInfos);
	}

	public SODFile(SignedData signedData) {
		this.signedData = signedData;
	}

	public SODFile(InputStream in) throws CardServiceException {
		try {
			BERTLVInputStream tlvIn = new BERTLVInputStream(in);
			tlvIn.readTag();
			tlvIn.readLength();
			ASN1InputStream asn1in =
				new ASN1InputStream(in);
			DERSequence seq = (DERSequence)asn1in.readObject();
			//To test DER spitting:
			//System.out.println("Test\n" + Util.printDERObject(seq));
			DERObjectIdentifier objectIdentifier = (DERObjectIdentifier)seq.getObjectAt(0);
			DERSequence s2 = (DERSequence)((DERTaggedObject)seq.getObjectAt(1)).getObject();
			this.signedData = new SignedData(s2);




			// DEBUG code below -- MO

			try {
				System.out.println("DEBUG: SOD contents ");

				DERInteger versionObject = signedData.getVersion(); // Should be 3
				int version = versionObject.getValue().intValue();
				System.out.println("DEBUG:    versionInt = " + version);

				ASN1Set digestAlgorithmsObject = signedData.getDigestAlgorithms();
				Enumeration<?> digestAlgorithmsEnum = digestAlgorithmsObject.getObjects();
				List<String> digestAlgorithms = new ArrayList<String>();
				while (digestAlgorithmsEnum.hasMoreElements()) {
					DERSequence digestAlgorithmSequence = (DERSequence)digestAlgorithmsEnum.nextElement();
					DERObjectIdentifier digestAlgorithmOID = (DERObjectIdentifier)digestAlgorithmSequence.getObjectAt(0);
					String digestAlgorithm = getAlgorithm(digestAlgorithmOID);
					digestAlgorithms.add(digestAlgorithm);
				}
				System.out.println("DEBUG:   digestAlgorithms = " + digestAlgorithms);

				LDSSecurityObject sObject = getSecurityObject();
				sObject.getDigestAlgorithmIdentifier();
				DataGroupHash[] dghs = sObject.getDatagroupHash();
				for (int i = 0; i < dghs.length; i++) {
					DataGroupHash dgh = dghs[i];
					int dataGroupNumber = dgh.getDataGroupNumber();
					byte[] dataGroupHash = dgh.getDataGroupHashValue().getOctets();
					System.out.println("DEBUG:      dg " + dataGroupNumber + " -> " + Hex.bytesToHexString(dataGroupHash));
				}

				Certificate docSigningCertificate = getDocSigningCertificate();
				System.out.println("DEBUG:   DSC");

				SignerInfo signerInfo = getSignerInfo();
				System.out.println("DEBUG:    signerInfo = " + signerInfo);
				SignerIdentifier signerIdentifier = signerInfo.getSID();
				System.out.println("DEBUG:        signerInfo.getSID().getId() = " + signerIdentifier.getId()); // In Dutch NIK v009/001: [[[[2.5.4.3, CSCA NL]], [[2.5.4.11, Ministry of the Interior and Kingdom Relations]], [[2.5.4.10, State of the Netherlands]], [[2.5.4.6, NL]]], 2]
				signerInfo.getEncryptedDigest();
				signerInfo.getUnauthenticatedAttributes();
				System.out.println("DEBUG:        signerInfo.getVersion() = " + signerInfo.getVersion()); // In Dutch NIK v009/001: 1
				System.out.println("DEBUG:        signerInfo.getDigestAlgorithm() = " + getAlgorithm(signerInfo.getDigestAlgorithm().getObjectId())); // In Dutch NIK v009/001: SHA256
				System.out.println("DEBUG:        signerInfo.getDigestEncryptionAlgorithm = " + getAlgorithm(signerInfo.getDigestEncryptionAlgorithm().getObjectId()));  // In Dutch NIK v009/001: SHA1
				System.out.println("DEBUG:        signerInfo.getDigestEncryptionAlgorithm params = " + signerInfo.getDigestEncryptionAlgorithm().getParameters()); // In Dutch NIK v009/001: null

				ASN1Set signedAttributes = signerInfo.getAuthenticatedAttributes();
				Enumeration<?> signedAttributesObjects = signedAttributes.getObjects();
				while (signedAttributesObjects.hasMoreElements()) {
					Attribute signedAttributeObject = new Attribute((DERSequence)signedAttributesObjects.nextElement());
					System.out.println("DEBUG: signedAttributeObject.getAttrType() = " + signedAttributeObject.getAttrType());
					System.out.println("DEBUG: signedAttributeObject.getAttrValues() = " + signedAttributeObject.getAttrValues());
				}
				
				ContentInfo contentInfo = signedData.getEncapContentInfo();
				byte[] content = ((DEROctetString)contentInfo.getContent()).getOctets();
				System.out.println("DEBUG:     content = " + Hex.bytesToHexString(content));
				
				MessageDigest dig = MessageDigest.getInstance("SHA256");
				byte[] mydig = dig.digest(content);
				System.out.println("DEBUG:     mydig = " + Hex.bytesToHexString(mydig));
			} catch (Exception exx) {
				exx.printStackTrace();
			}

		} catch (IOException e) {
			throw new CardServiceException(e.toString());
		}
	}

	/**
	 * The tag of this file.
	 * 
	 * @return the tag
	 */
	public int getTag() {
		return EF_SOD_TAG;
	}

	/**
	 * FIXME: needed for output.
	 */
	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject.getEncoded();
		}
		return null;
	}

	private SignerInfo getSignerInfo()  {
		ASN1Set signerInfos = signedData.getSignerInfos();
		if (signerInfos.size() > 1) {
			System.out.println("DEBUG: WARNING: found " + signerInfos.size() + " signerInfos");
		}
		for (int i = 0; i < signerInfos.size(); i++) {
			SignerInfo info = new SignerInfo((DERSequence)signerInfos.getObjectAt(i));
			return info;
		}
		return null;
	}

	/**
	 * Reads the security object (containing the hashes
	 * of the data groups) found in the SOd on the passport.
	 * 
	 * @return the security object
	 * 
	 * @throws IOException
	 */
	private LDSSecurityObject getSecurityObject() {
		try {
			ContentInfo contentInfo = signedData.getEncapContentInfo();
			byte[] content = ((DEROctetString)contentInfo.getContent()).getOctets();
			ASN1InputStream in =
				new ASN1InputStream(new ByteArrayInputStream(content)); 

			LDSSecurityObject sod =
				new LDSSecurityObject((DERSequence)in.readObject());
			Object nextObject = in.readObject();

			if (nextObject != null) {
				System.out.println("DEBUG: WARNING: extra object found after LDSSecurityObject...");
			}
			return sod;
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not read security object in signedData");
		}
	}

	/**
	 * Gets the stored data group hashes.
	 *
	 * @return data group hashes indexed by data group numbers (1 to 16).
	 */
	public Map<Integer, byte[]> getDataGroupHashes() {
		DataGroupHash[] hashObjects = getSecurityObject().getDatagroupHash();
		Map<Integer, byte[]> hashMap = new HashMap<Integer, byte[]>(); /* HashMap... get it? :D */
		for (int i = 0; i < hashObjects.length; i++) {
			DataGroupHash hashObject = hashObjects[i];
			int number = hashObject.getDataGroupNumber();
			byte[] hashValue = hashObject.getDataGroupHashValue().getOctets();
			hashMap.put(number, hashValue);
		}
		return hashMap;
	}

	/**
	 * Gets the algorithm used in the data group hashes.
	 * 
	 * @return an algorithm string such as "SHA1" or "SHA256"
	 */
	public String getDigestAlgorithm() throws Exception {
		return getAlgorithm(getSecurityObject().getDigestAlgorithmIdentifier().getObjectId());      
	}

	/**
	 * Gets the document signing certificate.
	 * Use this certificate to verify that
	 * <i>eSignature</i> is a valid signature for
	 * <i>eContent</i>. This certificate itself is
	 * signed using the country signing certificate.
	 *
	 * @return the document signing certificate
	 */
	public Certificate getDocSigningCertificate()
	throws IOException, CertificateException {
		byte[] certSpec = null;
		ASN1Set certs = signedData.getCertificates();
		if (certs.size() != 1) {
			System.out.println("DEBUG: WARNING: found "
					+ certs.size() + " certificates");
		}
		for (int i = 0; i < certs.size(); i++) {
			X509CertificateStructure e =
				new X509CertificateStructure((DERSequence)certs.getObjectAt(i));
			certSpec = new X509CertificateObject(e).getEncoded();
		}

		/* NOTE: we could have just returned that X509CertificateObject here,
		 * but by reconstructing it we hide the fact that we're using BC.
		 */
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		Certificate cert = factory.generateCertificate(new ByteArrayInputStream(certSpec));
		return cert;
	}

	/**
	 * Gets the contents of the security object over which the
	 * signature is to be computed. 
	 * 
	 * See RFC 3369, Cryptographic Message Syntax, August 2002,
	 * Section 5.4 for details.
	 *
	 * @see #getDocSigningCertificate()
	 * @see #getSignature()
	 * 
	 * @return the contents of the security object over which the
	 *         signature is to be computed
	 */
	private byte[] getEContent() throws IOException {
		SignerInfo signerInfo = getSignerInfo();
		ASN1Set signedAttributes = signerInfo.getAuthenticatedAttributes();

		ContentInfo contentInfo = signedData.getEncapContentInfo();
		byte[] content = ((DEROctetString)contentInfo.getContent()).getOctets();

		if (signedAttributes.size() == 0) {
			/* Signed attributes absent, return content to be digested... */
			return content;
		} else {
			/* Signed attributes present, return the attributes to be digested... */
			/* This option is taken by ICAO passports. */
			/* FIXME: we should probably check that the contents actually digest to this value! */
			return signedAttributes.getDEREncoded();
		}
	}

	/**
	 * Gets the contents of the security object over which the
	 * signature is to be computed. 
	 * 
	 * See RFC 3369, Cryptographic Message Syntax, August 2002,
	 * Section 5.4 for details.
	 *
	 * @see #getDocSigningCertificate()
	 * @see #getSignature()
	 * 
	 * @return the contents of the security object over which the
	 *         signature is to be computed
	 */
	private byte[] getSignedAttributes() {
		SignerInfo signerInfo = getSignerInfo();
		ASN1Set signedAttributes = signerInfo.getAuthenticatedAttributes();
		return signedAttributes.getDEREncoded();
	}

	/**
	 * Gets the stored signature of the security object.
	 * 
	 * @see #getDocSigningCertificate()
	 * 
	 * @return the signature
	 * @throws IOException when something goes wrong
	 */
	public byte[] getSignature() throws IOException {
		SignerInfo signerInfo = getSignerInfo();
		return signerInfo.getEncryptedDigest().getOctets();
	}

	/**
	 * Verifies the signature over the contents of the security object.
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
	 * @throws IOException if something goes wrong
	 * 
	 * @deprecated Leave this responsibility to client?
	 */
	public boolean checkDocSignature(Certificate docSigningCert)
	throws GeneralSecurityException, IOException {

		byte[] eContent = getEContent();      
		byte[] signature = getSignature();

		String encAlg = getSignerInfo().getDigestEncryptionAlgorithm().getObjectId().getId();

		// For the cases where the signature is simply a digest (haven't seen a passport like this, 
		// thus this is guessing)

		if(encAlg == null) {
			String digestAlg = getSignerInfo().getDigestAlgorithm().getObjectId().getId();
			MessageDigest digest = MessageDigest.getInstance(digestAlg);
			digest.update(eContent);
			byte[] digestBytes = digest.digest();
			return Arrays.equals(digestBytes, signature);
		}

		// For the RSA_SA_PSS 1. the default hash is SHA1, 2. The hash id is not encoded in OID
		// So it has to be specified "manually"
		if(encAlg.equals(RSA_SA_PSS_OID.toString())) {
			encAlg = getAlgorithm(getSignerInfo().getDigestAlgorithm().getObjectId()) +
			"withRSA/PSS"; 
		}

		Signature sig = Signature.getInstance(encAlg);
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

	/*
    private static String getHashId(byte[] derBytes) throws IOException {
		ASN1InputStream asn1in = new ASN1InputStream(derBytes);
		DERSequence seq = (DERSequence)asn1in.readObject();
		return ((DERObjectIdentifier)((DERSequence)seq.getObjectAt(0)).getObjectAt(0)).getId();       
	}
	 */
	/*
	private static byte[] getHashBytes(byte[] derBytes) throws IOException {
		ASN1InputStream asn1in = new ASN1InputStream(derBytes);
		DERSequence seq = (DERSequence)asn1in.readObject();
		return ((DEROctetString)seq.getObjectAt(1)).getOctets();       
	}
	 */

	private static String getAlgorithm(DERObjectIdentifier oid) throws NoSuchAlgorithmException {
		if(oid.equals(X509ObjectIdentifiers.id_SHA1)) { return "SHA1"; }
		if(oid.equals(NISTObjectIdentifiers.id_sha224)) { return "SHA224"; }
		if(oid.equals(NISTObjectIdentifiers.id_sha256)) { return "SHA256"; }
		if(oid.equals(NISTObjectIdentifiers.id_sha384)) { return "SHA384"; }
		if(oid.equals(NISTObjectIdentifiers.id_sha512)) { return "SHA512"; }
		if (oid.equals(PKCS1_SHA256_WITH_RSA_OID)) { return "SHA256withRSA"; }
		if (oid.equals(PKCS1_SHA384_WITH_RSA_OID)) { return "SHA384withRSA"; }
		if (oid.equals(PKCS1_SHA512_WITH_RSA_OID)) { return "SHA512withRSA"; }
		if (oid.equals(PKCS1_SHA224_WITH_RSA_OID)) { return "SHA224withRSA"; }
		throw new NoSuchAlgorithmException("Unknown OID " + oid);
	}
}
