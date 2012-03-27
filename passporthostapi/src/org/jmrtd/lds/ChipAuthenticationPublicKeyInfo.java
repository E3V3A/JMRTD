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
 * $Id: $
 */

package org.jmrtd.lds;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.DHParameter;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962NamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.jmrtd.JMRTDSecurityProvider;

/**
 * A concrete SecurityInfo structure that stores chip authentication public
 * key info, see EAC 1.11 specification.
 * 
 * This data structure provides a Chip Authentication Public Key of the MRTD chip.
 * <ul>
 * <li>The object identifier <code>protocol</code> SHALL identify the type of the public key
 *     (i.e. DH or ECDH).</li>
 * <li>The sequence <code>chipAuthenticationPublicKey</code> SHALL contain the public key
 *     in encoded form.</li>
 * <li>The integer <code>keyId</code> MAY be used to indicate the local key identifier.
 *     It MUST be used if the MRTD chip provides multiple public keys for Chip
 *     Authentication.</li>
 * </ul>
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * 
 * FIXME: interface dependency on BC classes?
 * FIXME: maybe clean up some of these constructors...
 */
public class ChipAuthenticationPublicKeyInfo extends SecurityInfo {

	private static final long serialVersionUID = 5687291829854501771L;

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();
	
	private String oid;
	private SubjectPublicKeyInfo subjectPublicKeyInfo;
	private int keyId;

	/**
	 * Constructs a new object.
	 * 
	 * @param oid
	 *            a proper EAC identifier
	 * @param publicKeyInfo
	 *            appropriate SubjectPublicKeyInfo structure
	 * @param keyId
	 *            the key identifier or -1 if not present
	 */
	ChipAuthenticationPublicKeyInfo(String oid, SubjectPublicKeyInfo publicKeyInfo, int keyId) {
		this.oid = oid;
		this.subjectPublicKeyInfo = publicKeyInfo;
		this.keyId = keyId;
		checkFields();
	}

	ChipAuthenticationPublicKeyInfo(String oid, SubjectPublicKeyInfo publicKeyInfo) {
		this(oid, publicKeyInfo, -1);
	}

	/**
	 * Creates a public key info structure.
	 * 
	 * @param publicKey Either a DH public key or an EC public key
	 * @param keyId
	 */
	public ChipAuthenticationPublicKeyInfo(PublicKey publicKey, int keyId) {
		this(inferProtocolIdentifier(publicKey), getSubjectPublicKeyInfo(publicKey), keyId);
	}

	/**
	 * Creates a public key info structure.
	 * 
	 * @param publicKey Either a DH public key or an EC public key
	 */
	public ChipAuthenticationPublicKeyInfo(PublicKey publicKey) {
		this(inferProtocolIdentifier(publicKey), getSubjectPublicKeyInfo(publicKey), -1);
	}

	ASN1Primitive getDERObject() {
		ASN1EncodableVector vector = new ASN1EncodableVector();
		vector.add(new ASN1ObjectIdentifier(oid));
		vector.add((ASN1Sequence)subjectPublicKeyInfo.toASN1Primitive());
		if (keyId >= 0) {
			vector.add(new ASN1Integer(keyId));
		}
		return new DLSequence(vector);
	}

	public String getObjectIdentifier() {
		return oid;
	}

	/**
	 * Returns a key identifier stored in this ChipAuthenticationPublicKeyInfo
	 * structure, null if not present
	 * 
	 * @return key identifier stored in this ChipAuthenticationPublicKeyInfo
	 *         structure
	 */
	public int getKeyId() {
		return keyId;
	}

	/**
	 * Returns a SubjectPublicKeyInfo contained in this
	 * ChipAuthenticationPublicKeyInfo structure.
	 * 
	 * @return SubjectPublicKeyInfo contained in this
	 *         ChipAuthenticationPublicKeyInfo structure
	 */
	public PublicKey getSubjectPublicKey() {
		return getPublicKey(subjectPublicKeyInfo);
	}

	/**
	 * Checks the correctness of the data for this instance of SecurityInfo
	 */
	// FIXME: also check type of public key
	protected void checkFields() {
		try {
			if (!checkRequiredIdentifier(oid)) {
				throw new IllegalArgumentException("Wrong identifier: " + oid);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed ChipAuthenticationInfo.");
		}
	}

	/**
	 * Checks whether the given object identifier identifies a
	 * ChipAuthenticationPublicKeyInfo structure.
	 * 
	 * @param oid object identifier
	 * 
	 * @return true if the match is positive
	 */
	public static boolean checkRequiredIdentifier(String oid) {
		return ID_PK_DH_OID.equals(oid) || ID_PK_ECDH_OID.equals(oid);
	}

	public String toString() {
		String protocol = oid;
		try {
			protocol = lookupMnemonicByOID(oid);
		} catch (NoSuchAlgorithmException nsae) {
			/* NOTE: we'll stick with oid */
		}

		return "ChipAuthenticationPublicKeyInfo ["
		+ "protocol = " + protocol + ", "
		+ "chipAuthenticationPublicKey = " + getSubjectPublicKey().toString() + ", "
		+ "keyId = " + Integer.toString(getKeyId()) +
		"]";
	}

	public int hashCode() {
		return 	123 + 1337 * (oid.hashCode() + keyId + subjectPublicKeyInfo.hashCode());
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!ChipAuthenticationPublicKeyInfo.class.equals(other.getClass())) { return false; }
		ChipAuthenticationPublicKeyInfo otherInfo = (ChipAuthenticationPublicKeyInfo)other;
		return oid.equals(otherInfo.oid)
		&& keyId == otherInfo.keyId
		&& subjectPublicKeyInfo.equals(otherInfo.subjectPublicKeyInfo)
		;
	}

	private static PublicKey getPublicKey(SubjectPublicKeyInfo spki) {
		try {
			byte[] encodedPublicKeyInfoBytes = spki.getEncoded(ASN1Encoding.DER);
			KeySpec keySpec = new X509EncodedKeySpec(encodedPublicKeyInfoBytes);
			try {
				KeyFactory factory = KeyFactory.getInstance("DH");
				return factory.generatePublic(keySpec);
			} catch (GeneralSecurityException gse) {
				KeyFactory factory = KeyFactory.getInstance("EC", BC_PROVIDER);
				return factory.generatePublic(keySpec);
			}
		} catch (GeneralSecurityException gse2) {
			gse2.printStackTrace();
			return null;
		} catch (Exception ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	/*
	 * Woj, I moved this here from DG14File, seemed more appropriate here. -- MO
	 */
	private static SubjectPublicKeyInfo getSubjectPublicKeyInfo(PublicKey publicKey) {
		// Here we need to some hocus-pokus, the EAC specification require for
		// all the key information to include the domain parameters explicitly. This is
		// not what Bouncy Castle does by default. But we first have to check if this is
		// the case.
		try {
			String algorithm = publicKey.getAlgorithm();
			if ("EC".equals(algorithm) || "ECDH".equals(algorithm)) {
				ASN1InputStream asn1In = new ASN1InputStream(publicKey.getEncoded());
				SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo((ASN1Sequence)asn1In.readObject());
				asn1In.close();
				AlgorithmIdentifier algorithmIdentifier = subjectPublicKeyInfo.getAlgorithmId();
				String algOID = algorithmIdentifier.getAlgorithm().getId();
				if (!ID_EC_PUBLIC_KEY.equals(algOID)) {
					throw new IllegalStateException("Was expecting id-ecPublicKey (" + ID_EC_PUBLIC_KEY_TYPE + "), found " + algOID);
				}
				ASN1Primitive derEncodedParams = algorithmIdentifier.getParameters().toASN1Primitive();
				X9ECParameters params = null;
				if (derEncodedParams instanceof ASN1ObjectIdentifier) {
					ASN1ObjectIdentifier paramsOID = (ASN1ObjectIdentifier)derEncodedParams;

					/* It's a named curve from X9.62. */
					params = X962NamedCurves.getByOID(paramsOID);
					if (params == null) { throw new IllegalStateException("Could not find X9.62 named curve for OID " + paramsOID.getId()); }

					/* Reconstruct the parameters. */
					org.bouncycastle.math.ec.ECPoint generator = params.getG();
					org.bouncycastle.math.ec.ECCurve curve = generator.getCurve();
					generator = curve.createPoint(generator.getX().toBigInteger(), generator.getY().toBigInteger(), false);
					params = new X9ECParameters(params.getCurve(), generator, params.getN(), params.getH(), params.getSeed());
				} else {
					/* It's not a named curve, we can just return the decoded public key info. */
					return subjectPublicKeyInfo;
				}

				if (publicKey instanceof org.bouncycastle.jce.interfaces.ECPublicKey) {
					org.bouncycastle.jce.interfaces.ECPublicKey ecPublicKey = (org.bouncycastle.jce.interfaces.ECPublicKey)publicKey;
					AlgorithmIdentifier id = new AlgorithmIdentifier(subjectPublicKeyInfo.getAlgorithmId().getAlgorithm(), params.toASN1Primitive());
					org.bouncycastle.math.ec.ECPoint q = ecPublicKey.getQ();
					// In case we would like to compress the point:
					// p = p.getCurve().createPoint(p.getX().toBigInteger(), p.getY().toBigInteger(), true);
					subjectPublicKeyInfo = new SubjectPublicKeyInfo(id, q.getEncoded());
					return subjectPublicKeyInfo;
				} else {
					return subjectPublicKeyInfo;
				}
			} else if ("DH".equals(algorithm)) {
				DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
				DHParameterSpec dhSpec = dhPublicKey.getParams();
				return new SubjectPublicKeyInfo(
						new AlgorithmIdentifier(EACObjectIdentifiers.id_PK_DH,
								new DHParameter(dhSpec.getP(), dhSpec.getG(), dhSpec.getL()).toASN1Primitive()),
								new ASN1Integer(dhPublicKey.getY()));
			} else {
				throw new IllegalArgumentException("Unrecognized key type, found " + publicKey.getAlgorithm() + ", should be DH or ECDH");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String inferProtocolIdentifier(PublicKey publicKey) {
		String algorithm = publicKey.getAlgorithm();
		if ("EC".equals(algorithm) || "ECDH".equals(algorithm)) {
			return ID_PK_ECDH_OID;
		} else if ("DH".equals(algorithm)) {
			return ID_PK_DH_OID;
		} else {
			throw new IllegalArgumentException("Wrong key type. Was expecting ECDH or DH public key.");
		}
	}
}
