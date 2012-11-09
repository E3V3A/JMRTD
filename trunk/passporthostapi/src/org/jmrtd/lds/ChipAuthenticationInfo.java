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

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DLSequence;

/**
 * A concrete SecurityInfo structure that stores chip authentication info,
 * see EAC 1.11 specification.
 * 
 * This data structure provides detailed information on an implementation of
 * Chip Authentication.
 * <ul>
 * <li>The object identifier <code>protocol</code> SHALL identify the
 *     algorithms to be used (i.e. key agreement, symmetric cipher and MAC).</li>
 * <li>The integer <code>version</code> SHALL identify the version of the protocol.
 *     Currently, versions 1 and 2 are supported.</li>
 * <li>The integer <code>keyId</code> MAY be used to indicate the local key identifier.
 *     It MUST be used if the MRTD chip provides multiple public keys for Chip
 *     Authentication.</li>
 * </ul>
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * 
 * FIXME: dependency on BC?
 */
public class ChipAuthenticationInfo extends SecurityInfo {

	private static final long serialVersionUID = 5591988305059068535L;

	public static final int VERSION_NUM = 1;

	private String oid;
	private int version;
	private BigInteger keyId;

	/**
	 * Constructs a new object.
	 * 
	 * @param oid
	 *            a proper EAC identifier
	 * @param version
	 *            has to be 1
	 * @param keyId
	 *            the key identifier
	 */
	public ChipAuthenticationInfo(String oid, int version, BigInteger keyId) {
		this.oid = oid;
		this.version = version;
		this.keyId = keyId;
		checkFields();
	}

	/**
	 * Constructs a new object.
	 * 
	 * @param oid
	 *            a proper EAC identifier
	 * @param version
	 *            has to be 1
	 */
	public ChipAuthenticationInfo(String oid, int version) {
		this(oid, version, BigInteger.valueOf(-1));
	}

	ASN1Primitive getDERObject() {
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1ObjectIdentifier(oid));
		v.add(new ASN1Integer(version));
		if (keyId.compareTo(BigInteger.ZERO) >= 0) {
			v.add(new ASN1Integer(keyId));
		}
		return new DLSequence(v);
	}
	
	public String getObjectIdentifier() {
		return oid;
	}

	/**
	 * Returns a key identifier stored in this ChipAuthenticationInfo structure, null
	 * if not present
	 * 
	 * @return key identifier stored in this ChipAuthenticationInfo structure
	 */
	public BigInteger getKeyId() {
		return keyId;
	}

	/**
	 * Checks the correctness of the data for this instance of SecurityInfo
	 */
	protected void checkFields() {
		try {
			if (!checkRequiredIdentifier(oid)) {
				throw new IllegalArgumentException("Wrong identifier: "	+ oid);
			}
			if (version != VERSION_NUM) {
				throw new IllegalArgumentException("Wrong version");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed ChipAuthenticationInfo.");
		}
	}

	/**
	 * Checks whether the given object identifier identfies a
	 * ChipAuthenticationInfo structure.
	 * 
	 * @param oid
	 *            object identifier
	 * @return true if the match is positive
	 */
	static boolean checkRequiredIdentifier(String oid) {
		return ID_CA_DH_3DES_CBC_CBC_OID.equals(oid) || ID_CA_ECDH_3DES_CBC_CBC_OID.equals(oid);
	}
	
	public String toString() {
		return  "ChipAuthenticationInfo [oid = " + oid + ", version = " + version + ", keyId = " + keyId + "]";
	}
	
	public int hashCode() {
		return 3 + 11 * (oid == null ? 0 : oid.hashCode()) + 61 * version + 1991 * keyId.hashCode();
	}
	
	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!ChipAuthenticationInfo.class.equals(other.getClass())) { return false; }
		ChipAuthenticationInfo otherChipAuthenticationInfo = (ChipAuthenticationInfo)other;
		return oid.equals(otherChipAuthenticationInfo.oid)
			&& version == otherChipAuthenticationInfo.version
			&& keyId.equals(otherChipAuthenticationInfo.keyId);
	}
}
