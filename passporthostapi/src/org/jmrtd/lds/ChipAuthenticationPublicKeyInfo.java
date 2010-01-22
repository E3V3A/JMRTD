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
 * $Id: $
 */

package org.jmrtd.lds;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.interfaces.DHPublicKey;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962NamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;

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
public class ChipAuthenticationPublicKeyInfo extends SecurityInfo
{
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
	public ChipAuthenticationPublicKeyInfo(String oid, SubjectPublicKeyInfo publicKeyInfo, int keyId) {
		this.oid = oid;
		this.subjectPublicKeyInfo = publicKeyInfo;
		this.keyId = keyId;
		checkFields();
	}
	
	public ChipAuthenticationPublicKeyInfo(String oid, SubjectPublicKeyInfo publicKeyInfo) {
		this(oid, publicKeyInfo, -1);
	}
	
	public ChipAuthenticationPublicKeyInfo(String oid, PublicKey publicKey, int keyId) {
		this(oid, getSubjectPublicKeyInfo(publicKey), keyId);
	}
	
	public ChipAuthenticationPublicKeyInfo(String oid, PublicKey publicKey) {
		this(oid, getSubjectPublicKeyInfo(publicKey), -1);
	}
	
	public ChipAuthenticationPublicKeyInfo(PublicKey publicKey, int keyId) {
		this(inferProtocolIdentifier(publicKey), getSubjectPublicKeyInfo(publicKey), keyId);
	}

	public ChipAuthenticationPublicKeyInfo(PublicKey publicKey) {
		this(inferProtocolIdentifier(publicKey), getSubjectPublicKeyInfo(publicKey), -1);
	}

	public DERObject getDERObject() {
		ASN1EncodableVector vector = new ASN1EncodableVector();
		vector.add(new DERObjectIdentifier(oid));
		vector.add((DERSequence)subjectPublicKeyInfo.getDERObject());
		if (keyId >= 0) {
			vector.add(new DERInteger(keyId));
		}
		return new DERSequence(vector);
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
			throw new IllegalArgumentException(
			"Malformed ChipAuthenticationInfo.");
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
		DERObjectIdentifier derOID = new DERObjectIdentifier(oid);
		return derOID.equals(EACObjectIdentifiers.id_PK_DH)
		|| derOID.equals(EACObjectIdentifiers.id_PK_ECDH);
	}

	public String toString() {
		DERObjectIdentifier derOID = new DERObjectIdentifier(oid);
		String protocol = oid;
		try {
			protocol = lookupMnemonicByOID(derOID);
		} catch (NoSuchAlgorithmException nsae) {
			/* NOTE: we'll stick with oid */
		}

		return "ChipAuthenticationPublicKeyInfo ["
		+ "protocol = " + protocol + ", "
		+ "chipAuthenticationPublicKey = " + getSubjectPublicKey().toString() + ", "
		+ "keyId = " + Integer.toString(getKeyId()) +
		"]";
	}

	private static PublicKey getPublicKey(SubjectPublicKeyInfo spki) {
		try {
			byte[] encodedPublicKeyInfoBytes = spki.getDEREncoded();
			KeySpec keySpec = new X509EncodedKeySpec(encodedPublicKeyInfoBytes);
			KeyFactory factory = KeyFactory.getInstance("DH");
			return factory.generatePublic(keySpec);
		} catch (GeneralSecurityException gse) {
			gse.printStackTrace();
			return null;
		}
	}
	
	/*
	 * Woj, I moved this here from DG14File, seemed more appropriate here. -- MO
	 */
	private static SubjectPublicKeyInfo getSubjectPublicKeyInfo(PublicKey publicKey) {
		// Here we need to some hocus-pokus, the EAC specification require for
		// all the
		// key information to include the domain parameters explicitly. This is
		// not what
		// Bouncy Castle does by default. But we first have to check if this is
		// the case.
		try {
			if (publicKey instanceof ECPublicKey) {
				ASN1InputStream asn1In = new ASN1InputStream(publicKey.getEncoded());
				SubjectPublicKeyInfo vInfo = new SubjectPublicKeyInfo((DERSequence)asn1In.readObject());
				asn1In.close();
				DERObject parameters = vInfo.getAlgorithmId().getParameters().getDERObject();
				X9ECParameters params = null;
				if (parameters instanceof DERObjectIdentifier) {
					params = X962NamedCurves.getByOID((DERObjectIdentifier)parameters);
					org.bouncycastle.math.ec.ECPoint p = params.getG();
					p = p.getCurve().createPoint(p.getX().toBigInteger(), p.getY().toBigInteger(), false);
					params = new X9ECParameters(params.getCurve(), p, params.getN(), params.getH(), params.getSeed());
				} else {
					return vInfo;
				}

				org.bouncycastle.jce.interfaces.ECPublicKey pub = (org.bouncycastle.jce.interfaces.ECPublicKey)publicKey;
				AlgorithmIdentifier id = new AlgorithmIdentifier(vInfo.getAlgorithmId().getObjectId(), params.getDERObject());
				org.bouncycastle.math.ec.ECPoint p = pub.getQ();
				// In case we would like to compress the point:
				// p = p.getCurve().createPoint(p.getX().toBigInteger(),
				// p.getY().toBigInteger(), true);
				vInfo = new SubjectPublicKeyInfo(id, p.getEncoded());
				return vInfo;
			} else if (publicKey instanceof DHPublicKey) {
				ASN1InputStream asn1In = new ASN1InputStream(publicKey.getEncoded());
				try {
					return new SubjectPublicKeyInfo((DERSequence)asn1In.readObject());
				} finally {
					asn1In.close();
				}
			} else {
				throw new IllegalArgumentException("Unrecognized key type, should be DH or EC");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static String inferProtocolIdentifier(PublicKey publicKey) {
		// FIXME: Couldn't we use PublicKey.getAlgorithm() here?
		if (publicKey instanceof ECPublicKey) {
			return EACObjectIdentifiers.id_PK_ECDH.getId();
		} else if (publicKey instanceof DHPublicKey) {
			return EACObjectIdentifiers.id_PK_DH.getId();
		} else {
			throw new IllegalArgumentException("Wrong key type.");
		}
	}
}
