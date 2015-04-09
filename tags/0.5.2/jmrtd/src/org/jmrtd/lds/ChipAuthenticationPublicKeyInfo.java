/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
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
 * $Id$
 */

package org.jmrtd.lds;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.util.logging.Logger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.Util;

/**
 * A concrete SecurityInfo structure that stores chip authentication public
 * key info, see EAC TR 03110 1.11 specification.
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

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	
	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	private String oid;
	private SubjectPublicKeyInfo subjectPublicKeyInfo;
	private BigInteger keyId;

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
	ChipAuthenticationPublicKeyInfo(String oid, SubjectPublicKeyInfo publicKeyInfo, BigInteger keyId) {
		this.oid = oid;
		this.subjectPublicKeyInfo = publicKeyInfo;
		this.keyId = keyId;
		checkFields();
	}

	ChipAuthenticationPublicKeyInfo(String oid, SubjectPublicKeyInfo publicKeyInfo) {
		this(oid, publicKeyInfo, BigInteger.valueOf(-1));
	}

	/**
	 * Creates a public key info structure.
	 * 
	 * @param publicKey Either a DH public key or an EC public key
	 * @param keyId key identifier
	 */
	public ChipAuthenticationPublicKeyInfo(PublicKey publicKey, BigInteger keyId) {
		this(Util.inferProtocolIdentifier(publicKey), Util.toSubjectPublicKeyInfo(Util.reconstructPublicKey(publicKey)), keyId);
	}

	/**
	 * Creates a public key info structure.
	 * 
	 * @param publicKey Either a DH public key or an EC public key
	 */
	public ChipAuthenticationPublicKeyInfo(PublicKey publicKey) {
		this(publicKey, BigInteger.valueOf(-1));
	}

	ASN1Primitive getDERObject() {
		ASN1EncodableVector vector = new ASN1EncodableVector();
		vector.add(new ASN1ObjectIdentifier(oid));
		vector.add((ASN1Sequence)subjectPublicKeyInfo.toASN1Primitive());
		if (keyId.compareTo(BigInteger.ZERO) >= 0) {
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
	public BigInteger getKeyId() {
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
		return Util.toPublicKey(subjectPublicKeyInfo);
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
			LOGGER.severe("Exception: " + e.getMessage());
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
		+ "keyId = " + getKeyId().toString() +
		"]";
	}

	public int hashCode() {
		return 	123 + 1337 * (oid.hashCode() + keyId.hashCode() + subjectPublicKeyInfo.hashCode());
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!ChipAuthenticationPublicKeyInfo.class.equals(other.getClass())) { return false; }
		ChipAuthenticationPublicKeyInfo otherInfo = (ChipAuthenticationPublicKeyInfo)other;
		return oid.equals(otherInfo.oid)
				&& keyId.equals(otherInfo.keyId)
				&& subjectPublicKeyInfo.equals(otherInfo.subjectPublicKeyInfo);
	}
}
