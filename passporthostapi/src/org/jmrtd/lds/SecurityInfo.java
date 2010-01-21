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

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;


/**
 * The general implementation of the SecurityInfo structure. See eg. the EAC
 * 1.11 specification.
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 * FIXME: dependency on BC?
 */
public class SecurityInfo /* FIXME: Shouldn't this class be abstract? */
{
	protected DERObjectIdentifier identifier;
	protected DERObject requiredData;
	protected DERObject optionalData;

	/**
	 * Constructs a new object based on another object of the same type. This is
	 * used to "remake" specialised instances of this class in subclasses.
	 * 
	 * @param securityInfo
	 *            a SecurityInfo object to construct this object from
	 */
	public SecurityInfo(SecurityInfo securityInfo) {
		this(securityInfo.identifier, securityInfo.requiredData,
				securityInfo.optionalData);
	}

	/**
	 * Constructs a new object
	 * 
	 * @param identifier
	 *            DER object identifier
	 * @param requiredData
	 *            DER object with the required SecurityInfo data
	 */
	public SecurityInfo(DERObjectIdentifier identifier, DERObject requiredData) {
		this(identifier, requiredData, null);
	}

	/**
	 * Constructs a new object
	 * 
	 * @param identifier
	 *            DER object identifier
	 * @param requiredData
	 *            DER object with the required SecurityInfo data
	 * @param optionalData
	 *            DER object with the optional SecurityInfo data
	 */
	public SecurityInfo(DERObjectIdentifier identifier, DERObject requiredData,
			DERObject optionalData) {
		this.identifier = identifier;
		this.requiredData = requiredData;
		this.optionalData = optionalData;
		checkFields();
	}

	/**
	 * Constructs a new object
	 * 
	 * @param obj
	 *            a DER object containing SecurityInfo object (DER sequence)
	 */
	public SecurityInfo(DERObject obj) {
		try {
			DERSequence s = (DERSequence) obj;
			identifier = (DERObjectIdentifier) s.getObjectAt(0);
			requiredData = s.getObjectAt(1).getDERObject();
			if (s.size() == 3) {
				optionalData = s.getObjectAt(2).getDERObject();
			}
			checkFields();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed input stream.");
		}
	}

	/**
	 * Constructs a new object from the data in <code>in</code>
	 * 
	 * @param in
	 *            the input stream containing the encoded data
	 * @throws IOException
	 *             on error
	 */
	public SecurityInfo(InputStream in) throws IOException {
		this(new ASN1InputStream(in).readObject());
	}

	/**
	 * Returns a DER object with this SecurityInfo data (DER sequence)
	 * 
	 * @return a DER object with this SecurityInfo data
	 */
	public DERObject getDERObject() {
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(identifier);
		v.add(requiredData);
		if (optionalData != null) {
			v.add(optionalData);
		}
		return new DERSequence(v);
	}

	/**
	 * REturns the object identifier of this SecurityInfo
	 * 
	 * @return this SecurityInfo object identifier
	 */
	public DERObjectIdentifier getObjectIdentifier() {
		return identifier;
	}

	/**
	 * Checks the correctness of the data encapsulated in SecurityInfo. Used in
	 * subclasses.
	 * 
	 */
	protected void checkFields() {
	}

	protected static String lookupMnemonicByOID(DERObjectIdentifier oid) throws NoSuchAlgorithmException {
		if (oid.equals(EACObjectIdentifiers.id_PK_DH)) { return "id_PK_DH"; }
		if (oid.equals(EACObjectIdentifiers.id_PK_ECDH)) { return "id_PK_ECDH"; }
		throw new NoSuchAlgorithmException("Unknown OID " + oid.getId() + "(is not " + EACObjectIdentifiers.id_PK_DH.getId() + ", and not " + EACObjectIdentifiers.id_PK_ECDH.getId());
	}

	public static SecurityInfo createSecurityInfo(DERObject obj) {
		try {
			DERSequence s = (DERSequence) obj;
			DERObjectIdentifier identifier = (DERObjectIdentifier) s.getObjectAt(0);
			DERObject requiredData = s.getObjectAt(1).getDERObject();
			DERObject optionalData = null;
			if (s.size() == 3) {
				optionalData = s.getObjectAt(2).getDERObject();
			}

			SecurityInfo si = new SecurityInfo(obj);
// FIXME: get rid of this constructor, make this class abstract...
			if (ChipAuthenticationPublicKeyInfo.checkRequiredIdentifier(identifier)) {
				return new ChipAuthenticationPublicKeyInfo(si);
			} else if (ChipAuthenticationInfo.checkRequiredIdentifier(identifier)) {
				return new ChipAuthenticationInfo(si);
			} else if (TerminalAuthenticationInfo.checkRequiredIdentifier(identifier)) {
				return new TerminalAuthenticationInfo(si);
			}
			return si;
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed input stream.");
		}

	}
}
