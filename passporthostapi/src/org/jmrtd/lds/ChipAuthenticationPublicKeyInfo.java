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
 */

package org.jmrtd.lds;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * A specialised SecurityInfo structure that stores chip authentication public
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
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 * FIXME: interface dependency on BC classes?
 */
public class ChipAuthenticationPublicKeyInfo extends SecurityInfo
{
	public ChipAuthenticationPublicKeyInfo(SecurityInfo securityInfo) {
		super(securityInfo);
	}

	public ChipAuthenticationPublicKeyInfo(InputStream in) throws IOException {
		super(in);
	}

	/**
	 * Constructs a new object.
	 * 
	 * @param identifier
	 *            a proper EAC identifier
	 * @param publicKeyInfo
	 *            appropriate SubjectPublicKeyInfo structure
	 * @param keyId
	 *            the key identifier
	 */
	public ChipAuthenticationPublicKeyInfo(DERObjectIdentifier identifier,
			SubjectPublicKeyInfo publicKeyInfo, Integer keyId) {
		super(identifier, publicKeyInfo.getDERObject(),
				keyId != null ? new DERInteger(keyId) : null);
	}

	/**
	 * Constructs a new object.
	 * 
	 * @param identifier
	 *            a proper EAC identifier
	 * @param publicKeyInfo
	 *            appropriate SubjectPublicKeyInfo structure
	 */
	public ChipAuthenticationPublicKeyInfo(DERObjectIdentifier identifier,
			SubjectPublicKeyInfo publicKeyInfo) {
		this(identifier, publicKeyInfo, null);
	}

	/**
	 * Returns a key identifier stored in this ChipAuthenticationPublicKeyInfo
	 * structure, null if not present
	 * 
	 * @return key identifier stored in this ChipAuthenticationPublicKeyInfo
	 *         structure
	 */
	public Integer getKeyId() {
		if (optionalData == null) {
			return -1;
		}
		return ((DERInteger) optionalData).getValue().intValue();
	}

	/**
	 * Returns a SubjectPublicKeyInfo contained in this
	 * ChipAuthenticationPublicKeyInfo structure
	 * 
	 * @return SubjectPublicKeyInfo contained in this
	 *         ChipAuthenticationPublicKeyInfo structure
	 */
	public SubjectPublicKeyInfo getSubjectPublicKeyInfo() {
		return new SubjectPublicKeyInfo((DERSequence) requiredData);
	}

	/**
	 * Checks the correctness of the data for this instance of SecurityInfo
	 */
	protected void checkFields() {
		try {
			if (!checkRequiredIdentifier(identifier)) {
				throw new IllegalArgumentException("Wrong identifier: "
						+ identifier.getId());
			}
			getSubjectPublicKeyInfo();
			if (optionalData != null && !(optionalData instanceof DERInteger)) {
				throw new IllegalArgumentException("Key Id not an integer: "
						+ optionalData.getClass().getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
			"Malformed ChipAuthenticationInfo.");
		}
	}

	/**
	 * Checks whether the given object identifier identfies a
	 * ChipAuthenticationPublicKeyInfo structure.
	 * 
	 * @param id
	 *            object identifier
	 * @return true if the match is positive
	 */
	public static boolean checkRequiredIdentifier(DERObjectIdentifier id) {
		return id.equals(EACObjectIdentifiers.id_PK_DH)
		|| id.equals(EACObjectIdentifiers.id_PK_ECDH);
	}

	public String toString() {
		DERObjectIdentifier protocolOID = getObjectIdentifier();
		String protocol = null;
		try {
			protocol = lookupMnemonicByOID(getObjectIdentifier());
		} catch (NoSuchAlgorithmException nsae) {
			protocol = protocolOID.getId();
		}

		return "ChipAuthenticationPublicKeyInfo ["
		+ "protocol = " + protocol + ", "
		+ "chipAuthenticationPublicKey = " + subjectPublicKeyInfoToString(getSubjectPublicKeyInfo()) + ", "
		+ "keyId = " + Integer.toString(getKeyId()) +
		"]";
	}

	private String subjectPublicKeyInfoToString(SubjectPublicKeyInfo spki) {
		try {
			StringBuffer result = new StringBuffer();
			byte[] encodedPublicKeyInfoBytes = spki.getDEREncoded();
			KeySpec keySpec = new X509EncodedKeySpec(encodedPublicKeyInfoBytes);
			KeyFactory factory = KeyFactory.getInstance("DH");
			PublicKey publicKey = factory.generatePublic(keySpec);
			result.append(publicKey.toString());
			return result.toString();
		} catch (Exception ee) {
			ee.printStackTrace();
			return ee.getMessage();
		}
	}
}
