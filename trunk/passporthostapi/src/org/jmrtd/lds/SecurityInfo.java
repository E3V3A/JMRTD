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

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;

/**
 * Abstract base class for security info structure.
 * See the EAC 1.11 specification.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * 
 * FIXME: dependency on BC in interface?
 */
public abstract class SecurityInfo extends AbstractLDSInfo {

	private static final long serialVersionUID = -7919854443619069808L;

	public static final String
	ID_PK_DH_OID = EACObjectIdentifiers.id_PK_DH.getId(),
	ID_PK_ECDH_OID = EACObjectIdentifiers.id_PK_ECDH.getId(),
	ID_TA_OID = EACObjectIdentifiers.id_TA.getId(),
	ID_CA_DH_3DES_CBC_CBC_OID = EACObjectIdentifiers.id_CA_DH_3DES_CBC_CBC.getId(),
	ID_CA_ECDH_3DES_CBC_CBC_OID = EACObjectIdentifiers.id_CA_ECDH_3DES_CBC_CBC.getId();
	
	public static final String
	ID_EC_PUBLIC_KEY_TYPE = X9ObjectIdentifiers.id_publicKeyType.getId(),
	ID_EC_PUBLIC_KEY = X9ObjectIdentifiers.id_ecPublicKey.getId();
	
	/**
	 * Returns a DER object with this SecurityInfo data (DER sequence)
	 * 
	 * @return a DER object with this SecurityInfo data
	 * 
	 * @deprecated Remove this method from visible interface (because of dependency on BC API)
	 */
	abstract ASN1Primitive getDERObject();
	
	/**
	 * Writes this SecurityInfo to output stream.
	 * 
	 * @param outputStream an ouput stream
	 * 
	 * @throws IOException if writing fails
	 */
	public void writeObject(OutputStream outputStream) throws IOException {
		ASN1Primitive derEncoded = getDERObject();
		if (derEncoded == null) { throw new IOException("Could not decode from DER."); }
		byte[] derEncodedBytes = derEncoded.getEncoded(ASN1Encoding.DER);
		if (derEncodedBytes == null) { throw new IOException("Could not decode from DER."); }
		outputStream.write(derEncodedBytes);
	}
	
	/**
	 * Returns the object identifier of this SecurityInfo.
	 * 
	 * @return this SecurityInfo object identifier
	 */
	public abstract String getObjectIdentifier();
	
	/**
	 * Factory method for creating security info objects given an input.
	 * 
	 * @param obj the input
	 * 
	 * @return a concrete security info object
	 */
	static SecurityInfo getInstance(ASN1Primitive obj) {
		try {
			ASN1Sequence sequence = (ASN1Sequence)obj;

			String oid = ((ASN1ObjectIdentifier)sequence.getObjectAt(0)).getId();

			System.out.println("DEBUG: oid = " + oid);
			
			ASN1Primitive requiredData = sequence.getObjectAt(1).toASN1Primitive();
			ASN1Primitive optionalData = null;
			if (sequence.size() == 3) {
				optionalData = sequence.getObjectAt(2).toASN1Primitive();
			}

			if (ChipAuthenticationPublicKeyInfo.checkRequiredIdentifier(oid)) {
				System.out.println("DEBUG: " + oid + " is ChipAuthenticationPublicKeyInfo");
				SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo((ASN1Sequence)requiredData);
				if (optionalData == null) {
					return new ChipAuthenticationPublicKeyInfo(oid, subjectPublicKeyInfo);
				} else {
					ASN1Integer optionalDataAsASN1Integer = (ASN1Integer)optionalData;
					System.out.println("DEBUG: optionalData as bytes: " + Hex.bytesToHexString(optionalDataAsASN1Integer.getEncoded()));
					System.out.println("DEBUG: optionalData as BigInteger: " + optionalDataAsASN1Integer.getValue());
					System.out.println("DEBUG: optionalData as BigInteger converted to bytes: " + Hex.bytesToHexString(optionalDataAsASN1Integer.getValue().toByteArray()));					
					System.out.println("DEBUG: optionalData as int: " + optionalDataAsASN1Integer.getValue().intValue());
					BigInteger keyId = optionalDataAsASN1Integer.getValue();
					return new ChipAuthenticationPublicKeyInfo(oid, subjectPublicKeyInfo, keyId);
				}
			} else if (ChipAuthenticationInfo.checkRequiredIdentifier(oid)) {
				System.out.println("DEBUG: " + oid + " is ChipAuthenticationInfo");
				int version = ((ASN1Integer)requiredData).getValue().intValue();
				if (optionalData == null) {
					return new ChipAuthenticationInfo(oid, version);
				} else {
					ASN1Integer optionalDataAsASN1Integer = (ASN1Integer)optionalData;
					System.out.println("DEBUG: optionalData as bytes: " + Hex.bytesToHexString(optionalDataAsASN1Integer.getEncoded()));
					System.out.println("DEBUG: optionalData as BigInteger: " + optionalDataAsASN1Integer.getValue());
					System.out.println("DEBUG: optionalData as BigInteger converted to bytes: " + Hex.bytesToHexString(optionalDataAsASN1Integer.getValue().toByteArray()));					
					System.out.println("DEBUG: optionalData as int: " + optionalDataAsASN1Integer.getValue().intValue());
					BigInteger keyId = optionalDataAsASN1Integer.getValue();
					return new ChipAuthenticationInfo(oid, version, keyId);
				}
			} else if (TerminalAuthenticationInfo.checkRequiredIdentifier(oid)) {
				System.out.println("DEBUG: " + oid + " is TerminalAuthenticationInfo");
				int version = ((ASN1Integer)requiredData).getValue().intValue();
				if (optionalData == null) {
					return new TerminalAuthenticationInfo(oid, version);
				} else {
					ASN1Sequence efCVCA = (ASN1Sequence)optionalData;
					return new TerminalAuthenticationInfo(oid, version, efCVCA);
				}
			}
			throw new IllegalArgumentException("Malformed input stream.");
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed input stream.");
		}
	}
	
	protected static String lookupMnemonicByOID(String oid) throws NoSuchAlgorithmException {
		if (ID_PK_DH_OID.equals(oid)) { return "id_PK_DH"; }
		if (ID_PK_ECDH_OID.equals(oid)) { return "id_PK_ECDH"; }
		if (ID_TA_OID.equals(oid)) { return "id_TA"; }
		throw new NoSuchAlgorithmException("Unknown OID " + oid);
	}
}
