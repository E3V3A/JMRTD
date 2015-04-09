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

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
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
	
	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	
	/**
	 * Used in ECDSA based Active Authentication.
	 * <code>{joint-iso-itu-t(2) international-organizations(23) 136 mrtd(1) security(1) aaProtocolObject(5)}</code>.
	 */
	public static final String ID_AA_OID = "2.23.136.1.1.5";
	
	public static final String
	ID_PK_DH_OID = EACObjectIdentifiers.id_PK_DH.getId(),
	ID_PK_ECDH_OID = EACObjectIdentifiers.id_PK_ECDH.getId(),
	ID_TA_OID = EACObjectIdentifiers.id_TA.getId(),
	ID_CA_DH_3DES_CBC_CBC_OID = EACObjectIdentifiers.id_CA_DH_3DES_CBC_CBC.getId(),
	ID_CA_ECDH_3DES_CBC_CBC_OID = EACObjectIdentifiers.id_CA_ECDH_3DES_CBC_CBC.getId();
	
	public static final String
	ID_CA_DH_AES_CBC_CMAC_128_OID = "0.4.0.127.0.7.2.2.3.1.2",
	ID_CA_DH_AES_CBC_CMAC_192_OID = "0.4.0.127.0.7.2.2.3.1.3",
	ID_CA_DH_AES_CBC_CMAC_256_OID = "0.4.0.127.0.7.2.2.3.1.4",
	ID_CA_ECDH_AES_CBC_CMAC_128_OID = "0.4.0.127.0.7.2.2.3.2.2",
	ID_CA_ECDH_AES_CBC_CMAC_192_OID = "0.4.0.127.0.7.2.2.3.2.3",
	ID_CA_ECDH_AES_CBC_CMAC_256_OID = "0.4.0.127.0.7.2.2.3.2.4";
	
	public static final String
	ID_EC_PUBLIC_KEY_TYPE = X9ObjectIdentifiers.id_publicKeyType.getId(),
	ID_EC_PUBLIC_KEY = X9ObjectIdentifiers.id_ecPublicKey.getId();
	
	private static final String ID_BSI = "0.4.0.127.0.7";

	/* protocols (2), smartcard (2), PACE (4) */
	public static final String ID_PACE = ID_BSI + ".2.2.4";

	public static final String
	ID_PACE_DH_GM = ID_PACE + ".1";

	public static final String
	ID_PACE_ECDH_GM = ID_PACE + ".2";
	
	public static final String
	ID_PACE_DH_IM = ID_PACE + ".3";

	public static final String
	ID_PACE_ECDH_IM = ID_PACE + ".4";
	
	public static final String
	ID_PACE_DH_GM_3DES_CBC_CBC = ID_PACE_DH_GM + ".1", /* 0.4.0.127.0.7.2.2.4.1.1, id-PACE-DH-GM-3DES-CBC-CBC */
	ID_PACE_DH_GM_AES_CBC_CMAC_128 = ID_PACE_DH_GM + ".2", /* 0.4.0.127.0.7.2.2.4.1.2, id-PACE-DH-GM-AES-CBC-CMAC-128 */
	ID_PACE_DH_GM_AES_CBC_CMAC_192 = ID_PACE_DH_GM + ".3", /* 0.4.0.127.0.7.2.2.4.1.3, id-PACE-DH-GM-AES-CBC-CMAC-192 */
	ID_PACE_DH_GM_AES_CBC_CMAC_256 = ID_PACE_DH_GM + ".4"; /* 0.4.0.127.0.7.2.2.4.1.4, id-PACE-DH-GM-AES-CBC-CMAC-256 */

	public static final String
	ID_PACE_ECDH_GM_3DES_CBC_CBC = ID_PACE_ECDH_GM + ".1", /* 0.4.0.127.0.7.2.2.4.2.1, id-PACE-ECDH-GM-3DES-CBC-CBC */
	ID_PACE_ECDH_GM_AES_CBC_CMAC_128 = ID_PACE_ECDH_GM + ".2", /* 0.4.0.127.0.7.2.2.4.2.2, id-PACE-ECDH-GM-AES-CBC-CMAC-128 */
	ID_PACE_ECDH_GM_AES_CBC_CMAC_192 = ID_PACE_ECDH_GM + ".3", /* 0.4.0.127.0.7.2.2.4.2.3, id-PACE-ECDH-GM-AES-CBC-CMAC-192 */
	ID_PACE_ECDH_GM_AES_CBC_CMAC_256 = ID_PACE_ECDH_GM + ".4"; /* 0.4.0.127.0.7.2.2.4.2.4, id-PACE-ECDH-GM-AES-CBC-CMAC-256 */

	public static final String
	ID_PACE_DH_IM_3DES_CBC_CBC = ID_PACE_DH_IM + ".1", /* 0.4.0.127.0.7.2.2.4.3.1, id-PACE-DH-IM-3DES-CBC-CBC */
	ID_PACE_DH_IM_AES_CBC_CMAC_128 = ID_PACE_DH_IM + ".2", /* 0.4.0.127.0.7.2.2.4.3.2, id-PACE-DH-IM-AES-CBC-CMAC-128 */
	ID_PACE_DH_IM_AES_CBC_CMAC_192 = ID_PACE_DH_IM + ".3", /* 0.4.0.127.0.7.2.2.4.3.3, id-PACE-DH-IM-AES-CBC-CMAC-192 */
	ID_PACE_DH_IM_AES_CBC_CMAC_256 = ID_PACE_DH_IM + ".4"; /* 0.4.0.127.0.7.2.2.4.3.4, id-PACE-DH-IM-AES-CBC-CMAC-256 */

	public static final String
	ID_PACE_ECDH_IM_3DES_CBC_CBC = ID_PACE_ECDH_IM + ".1", /* 0.4.0.127.0.7.2.2.4.4.1, id-PACE-ECDH-IM-3DES-CBC-CBC */
	ID_PACE_ECDH_IM_AES_CBC_CMAC_128 = ID_PACE_ECDH_IM + ".2", /* 0.4.0.127.0.7.2.2.4.4.2, id-PACE-ECDH-IM-AES-CBC-CMAC-128 */
	ID_PACE_ECDH_IM_AES_CBC_CMAC_192 = ID_PACE_ECDH_IM + ".3", /* 0.4.0.127.0.7.2.2.4.4.3, id-PACE-ECDH-IM-AES-CBC-CMAC-192 */
	ID_PACE_ECDH_IM_AES_CBC_CMAC_256 = ID_PACE_ECDH_IM + ".4"; /* 0.4.0.127.0.7.2.2.4.4.4, id-PACE-ECDH-IM-AES-CBC-CMAC-256 */
	
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
			ASN1Primitive requiredData = sequence.getObjectAt(1).toASN1Primitive();
			ASN1Primitive optionalData = null;
			if (sequence.size() == 3) {
				optionalData = sequence.getObjectAt(2).toASN1Primitive();
			}

			if (ActiveAuthenticationInfo.checkRequiredIdentifier(oid)) {
				int version = ((ASN1Integer)requiredData).getValue().intValue();
				if (optionalData == null) {
					return new ActiveAuthenticationInfo(oid, version, null);
				} else {
					String signatureAlgorithmOID = ((ASN1ObjectIdentifier)optionalData).getId();
					return new ActiveAuthenticationInfo(oid, version, signatureAlgorithmOID);
				}
			} else if (ChipAuthenticationPublicKeyInfo.checkRequiredIdentifier(oid)) {
				SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo((ASN1Sequence)requiredData);
				if (optionalData == null) {
					return new ChipAuthenticationPublicKeyInfo(oid, subjectPublicKeyInfo);
				} else {
					ASN1Integer optionalDataAsASN1Integer = (ASN1Integer)optionalData;
					BigInteger keyId = optionalDataAsASN1Integer.getValue();
					return new ChipAuthenticationPublicKeyInfo(oid, subjectPublicKeyInfo, keyId);
				}
			} else if (ChipAuthenticationInfo.checkRequiredIdentifier(oid)) {
				int version = ((ASN1Integer)requiredData).getValue().intValue();
				if (optionalData == null) {
					return new ChipAuthenticationInfo(oid, version);
				} else {
					ASN1Integer optionalDataAsASN1Integer = (ASN1Integer)optionalData;
					BigInteger keyId = optionalDataAsASN1Integer.getValue();
					return new ChipAuthenticationInfo(oid, version, keyId);
				}
			} else if (TerminalAuthenticationInfo.checkRequiredIdentifier(oid)) {
				int version = ((ASN1Integer)requiredData).getValue().intValue();
				if (optionalData == null) {
					return new TerminalAuthenticationInfo(oid, version);
				} else {
					ASN1Sequence efCVCA = (ASN1Sequence)optionalData;
					return new TerminalAuthenticationInfo(oid, version, efCVCA);
				}
			} else if (PACEInfo.checkRequiredIdentifier(oid)) {
				int version = ((ASN1Integer)requiredData).getValue().intValue();
				int parameterId = -1;
				if (optionalData != null) {
					parameterId = ((ASN1Integer)optionalData).getValue().intValue();
				}
				return new PACEInfo(oid, version, parameterId);
			} else if (PACEDomainParameterInfo.checkRequiredIdentifier(oid)) {
				AlgorithmIdentifier domainParameters = AlgorithmIdentifier.getInstance(requiredData);
				if (optionalData != null) {
					int parameterId = ((ASN1Integer)optionalData).getValue().intValue();
					return new PACEDomainParameterInfo(oid, domainParameters, parameterId);
				}
				return new PACEDomainParameterInfo(oid, domainParameters);
			}
//			throw new IllegalArgumentException("Malformed input stream.");
			LOGGER.warning("Unsupported SecurityInfo, oid = " + oid);
			return null;
		} catch (Exception e) {
			LOGGER.severe("Exception: " + e.getMessage());
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
