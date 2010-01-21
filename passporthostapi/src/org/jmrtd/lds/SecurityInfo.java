/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

import java.security.NoSuchAlgorithmException;

import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * The abstract SecurityInfo structure.
 * See the EAC 1.11 specification.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * 
 * FIXME: dependency on BC in interface?
 */
public abstract class SecurityInfo
{
	/**
	 * Returns a DER object with this SecurityInfo data (DER sequence)
	 * 
	 * @return a DER object with this SecurityInfo data
	 */
	public abstract DERObject getDERObject();

	/**
	 * Returns the object identifier of this SecurityInfo
	 * 
	 * @return this SecurityInfo object identifier
	 */
	public abstract String getObjectIdentifier();
	
	/**
	 * Factory method for creating security info objects given an input stream.
	 * 
	 * @param in the input stream
	 * 
	 * @return a concrete security info object
	 */
	public static SecurityInfo createSecurityInfo(DERObject obj) {
		try {
//			DERObject obj = new ASN1InputStream(in).readObject();
			DERSequence sequence = (DERSequence)obj;
			String oid = ((DERObjectIdentifier)sequence.getObjectAt(0)).getId();
			DERObject requiredData = sequence.getObjectAt(1).getDERObject();
			DERObject optionalData = null;
			if (sequence.size() == 3) {
				optionalData = sequence.getObjectAt(2).getDERObject();
			}
			
			if (ChipAuthenticationPublicKeyInfo.checkRequiredIdentifier(oid)) {
				SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo((DERSequence)requiredData);
				if (optionalData == null) {
					return new ChipAuthenticationPublicKeyInfo(oid, subjectPublicKeyInfo);
				} else {
					int keyId = ((DERInteger)optionalData).getValue().intValue();
					return new ChipAuthenticationPublicKeyInfo(oid, subjectPublicKeyInfo, keyId);
				}
			} else if (ChipAuthenticationInfo.checkRequiredIdentifier(oid)) {
				int version = ((DERInteger)requiredData).getValue().intValue();
				if (optionalData == null) {
					return new ChipAuthenticationInfo(oid, version);
				} else {
					int keyId = ((DERInteger)optionalData).getValue().intValue();
					return new ChipAuthenticationInfo(oid, version, keyId);
				}
			} else if (TerminalAuthenticationInfo.checkRequiredIdentifier(oid)) {
				int version = ((DERInteger)requiredData).getValue().intValue();
				if (optionalData == null) {
					return new TerminalAuthenticationInfo(oid, version);
				} else {
					DERSequence efCVCA = (DERSequence)optionalData;
					return new TerminalAuthenticationInfo(oid, version, efCVCA);
				}
			}
			throw new IllegalArgumentException("Malformed input stream.");
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed input stream.");
		}
	}

	protected static String lookupMnemonicByOID(DERObjectIdentifier oid) throws NoSuchAlgorithmException {
		if (oid.equals(EACObjectIdentifiers.id_PK_DH)) { return "id_PK_DH"; }
		if (oid.equals(EACObjectIdentifiers.id_PK_ECDH)) { return "id_PK_ECDH"; }
		if (oid.equals(EACObjectIdentifiers.id_TA)) { return "id_TA"; }
		throw new NoSuchAlgorithmException("Unknown OID " + oid.getId() + "(is not " + EACObjectIdentifiers.id_PK_DH.getId() + ", and not " + EACObjectIdentifiers.id_PK_ECDH.getId());
	}
}
