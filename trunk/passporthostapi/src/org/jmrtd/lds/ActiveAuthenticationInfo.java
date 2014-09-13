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
 * $Id$
 */

package org.jmrtd.lds;

import java.security.NoSuchAlgorithmException;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DLSequence;

/**
 * A concrete SecurityInfo structure that stores active authentication
 * info, see TR-LDS-PKI Maintenance V1.0.
 * 
 * <pre>
 * ActiveAuthenticationInfo ::= SEQUENCE { 
 *    protocol id-icao-mrtd-security-aaProtocolObject, 
 *    version INTEGER -- MUST be 1 
 *    signatureAlgorithm OBJECT IDENTIFIER 
 * } 
 *
 * -- Object Identifiers 
 * id-icao OBJECT IDENTIFIER ::= {2 23 136} 
 * id-icao-mrtd OBJECT IDENTIFIER ::= {id-icao 1} 
 * id-icao-mrtd-security OBJECT IDENTIFIER ::= {id-icao-mrtd 1} 
 *
 * id-icao-mrtd-security-aaProtocolObject OBJECT IDENTIFIER ::= 
 *    {id-icao-mrtd-security 5} 
 * </pre>
 * 
 * @author JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 */
public class ActiveAuthenticationInfo extends SecurityInfo {

	private static final long serialVersionUID = 6830847342039845308L;

	public static final int VERSION_NUM = 1;

	/** Specified in BSI TR 03111 Section 5.2.1. */

	/** Specified in BSI TR 03111 Section 5.2.1. */
	public static final String
	ECDSA_PLAIN_SIGNATURES = "0.4.0.127.0.7.1.1.4.1",
	ECDSA_PLAIN_SHA1_OID = ECDSA_PLAIN_SIGNATURES + ".1", /* 0.4.0.127.0.7.1.1.4.1.1, ecdsa-plain-SHA1 */
	ECDSA_PLAIN_SHA224_OID = ECDSA_PLAIN_SIGNATURES + ".2", /* 0.4.0.127.0.7.1.1.4.1.2, ecdsa-plain-SHA224 */
	ECDSA_PLAIN_SHA256_OID = ECDSA_PLAIN_SIGNATURES + ".3", /* 0.4.0.127.0.7.1.1.4.1.3, ecdsa-plain-SHA256 */
	ECDSA_PLAIN_SHA384_OID = ECDSA_PLAIN_SIGNATURES + ".4", /* 0.4.0.127.0.7.1.1.4.1.4, ecdsa-plain-SHA384 */
	ECDSA_PLAIN_SHA512_OID = ECDSA_PLAIN_SIGNATURES + ".5", /* 0.4.0.127.0.7.1.1.4.1.5, ecdsa-plain-SHA512 */
	ECDSA_PLAIN_RIPEMD160_OID = ECDSA_PLAIN_SIGNATURES + ".6"; /* 0.4.0.127.0.7.1.1.4.1.6, ecdsa-plain-RIPEMD160 */

	private String oid;
	private int version;
	private String signatureAlgorithmOID;

	/**
	 * Constructs a new object.
	 * 
	 * @param oid the id_AA identifier
	 * @param version has to be 1
	 * @param the signature algorithm OID
	 */
	ActiveAuthenticationInfo(String oid, int version, String signatureAlgorithmOID) {
		this.oid = oid;
		this.version = version;
		this.signatureAlgorithmOID = signatureAlgorithmOID;
		checkFields();
	}

	/**
	 * Constructs a new object.
	 *
	 * @param the signature algorithm OID
	 */
	public ActiveAuthenticationInfo(String signatureAlgorithmOID) {
		this(ID_AA_OID, VERSION_NUM, signatureAlgorithmOID);
	}

	ASN1Primitive getDERObject() {
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1ObjectIdentifier(oid));
		v.add(new ASN1Integer(version));
		if (signatureAlgorithmOID != null) {
			v.add(new ASN1ObjectIdentifier(signatureAlgorithmOID));
		}
		return new DLSequence(v);
	}

	/**
	 * Gets the object identifier of this AA security info.
	 * 
	 * @return an object identifier
	 */
	public String getObjectIdentifier() {
		return oid;
	}

	public String getSignatureAlgorithmOID() {
		return signatureAlgorithmOID;
	}


	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("ActiveAuthenticationInfo");
		result.append("[");
		result.append("signatureAlgorithmOID = " + getSignatureAlgorithmOID());
		result.append("]");
		return result.toString();
	}

	public int hashCode() {
		return 12345
				+ 3 * (oid == null ? 0 : oid.hashCode())
				+ 5 * version
				+ 11 * (signatureAlgorithmOID == null ? 1 : signatureAlgorithmOID.hashCode());
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!ActiveAuthenticationInfo.class.equals(other.getClass())) { return false; }
		ActiveAuthenticationInfo otherActiveAuthenticationInfo = (ActiveAuthenticationInfo)other;
		return getDERObject().equals(otherActiveAuthenticationInfo.getDERObject());
	}

	/* ONLY NON-PUBLIC METHODS BELOW */

	/**
	 * Checks whether the given object identifier identifies a
	 * ActiveAuthenticationInfo structure.
	 * 
	 * @param id
	 *            object identifier
	 * @return true if the match is positive
	 */
	static boolean checkRequiredIdentifier(String id) {
		return ID_AA_OID.equals(id);
	}

	/**
	 * Checks the correctness of the data for this instance of SecurityInfo
	 */
	private void checkFields() {
		try {
			if (!checkRequiredIdentifier(oid)) { throw new IllegalArgumentException("Wrong identifier: " + oid); }
			if (version != VERSION_NUM) { throw new IllegalArgumentException("Wrong version: " + version); }
			/* FIXME check to see if signatureAlgorithmOID is valid. */
			
			if (!ECDSA_PLAIN_SHA1_OID.equals(signatureAlgorithmOID)
					&& !ECDSA_PLAIN_SHA224_OID.equals(signatureAlgorithmOID)
					&& !ECDSA_PLAIN_SHA256_OID.equals(signatureAlgorithmOID)
					&& !ECDSA_PLAIN_SHA384_OID.equals(signatureAlgorithmOID)
					&& !ECDSA_PLAIN_SHA512_OID.equals(signatureAlgorithmOID)
					&& !ECDSA_PLAIN_RIPEMD160_OID.equals(signatureAlgorithmOID)) {
				throw new IllegalArgumentException("Wrong signature algorithm OID: " + signatureAlgorithmOID);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed ActiveAuthenticationInfo.");
		}
	}
	
	public static String lookupMnemonicByOID(String oid) throws NoSuchAlgorithmException {		
		if (ECDSA_PLAIN_SHA1_OID.equals(oid)) { return "SHA1withECDSA"; }
		if (ECDSA_PLAIN_SHA224_OID.equals(oid)) { return "SHA224withECDSA"; }
		if (ECDSA_PLAIN_SHA256_OID.equals(oid)) { return "SHA256withECDSA"; }
		if (ECDSA_PLAIN_SHA384_OID.equals(oid)) { return "SHA384withECDSA"; }
		if (ECDSA_PLAIN_SHA512_OID.equals(oid)) { return "SHA512withECDSA"; }
		if (ECDSA_PLAIN_RIPEMD160_OID.equals(oid)) { return "RIPEMD160withECDSA"; }
		throw new NoSuchAlgorithmException("Unknown OID " + oid);
	}
}
