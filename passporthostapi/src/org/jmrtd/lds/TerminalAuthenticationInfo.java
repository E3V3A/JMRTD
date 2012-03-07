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

import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

/**
 * A concrete SecurityInfo structure that stores terminal authentication
 * info, see EAC 1.11 specification.
 * 
 * This data structure provides detailed information on an implementation of Terminal Authentication.
 * <ul>
 * <li>The object identifier <code>protocol</code> SHALL identify the Terminal
 *     Authentication Protocol as the specific protocol may change over time.</li>
 * <li>The integer <code>version</code> SHALL identify the version of the protocol.
 *     Currently, versions 1 and 2 are supported.</li>
 * <li>The sequence <code>efCVCA</code> MAY be used to indicate a (short) file
 *     identifier of the file EF.CVCA. It MUST be used, if the default (short) file
 *     identifier is not used.</li>
 * </ul>
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class TerminalAuthenticationInfo extends SecurityInfo
{
	public static final int VERSION_NUM = 1;

	private String oid;
	private int version;
	private DERSequence efCVCA; /* FIXME: this contains just a file identifier? Why not byte (or short?) instead of DERSequence? -- MO */

	/**
	 * Constructs a new object.
	 * 
	 * @param oid
	 *            the id_TA identifier
	 * @param version
	 *            has to be 1
	 * @param efCVCA
	 *            the file ID information of the efCVCA file
	 */
	TerminalAuthenticationInfo(String oid, int version, DERSequence efCVCA) {
		this.oid = oid;
		this.version = version;
		this.efCVCA = efCVCA;
		checkFields();
	}

	/**
	 * Constructs a new object.
	 * 
	 * @param identifier
	 *            the id_TA identifier
	 * @param version
	 *            has to be 1
	 */
	TerminalAuthenticationInfo(String identifier, int version) {
		this(identifier, version, null);
	}

	/**
	 * Constructs a terminal authentication info using id_TA identifier {@link #ID_TA_OID}
	 * and version {@value #VERSION_NUM}.
	 */
	public TerminalAuthenticationInfo() {
		this(ID_TA_OID, VERSION_NUM);
	}

	/**
	 * Constructs a new object with the required object identifier and version
	 * number and:
	 * 
	 * @param fileId
	 *            a file identifier reference to the efCVCA file
	 * @param shortFileId
	 *            short file id for the above file, -1 if none
	 */
	public TerminalAuthenticationInfo(Integer fileId, Integer shortFileId) {
		this(ID_TA_OID, VERSION_NUM, shortFileId
				.byteValue() != -1 ? new DERSequence(new ASN1Encodable[] {
						new DEROctetString(Hex.hexStringToBytes(Hex
								.shortToHexString(fileId.shortValue()))),
								new DEROctetString(Hex.hexStringToBytes(Hex
										.byteToHexString(shortFileId.byteValue()))) })
		: new DERSequence(new ASN1Encodable[] { new DEROctetString(Hex
				.hexStringToBytes(Hex.shortToHexString(fileId
						.shortValue()))) }));
	}

	DERObject getDERObject() {
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new DERObjectIdentifier(oid));
		v.add(new DERInteger(version));
		if (efCVCA != null) {
			v.add(efCVCA);
		}
		return new DERSequence(v);
	}

	/**
	 * Gets the object identifier of this TA security info.
	 * 
	 * @return an object identifier
	 */
	public String getObjectIdentifier() {
		return oid;
	}

	/**
	 * Returns the efCVCA file identifier stored in this file, -1 if none
	 * 
	 * @return the efCVCA file identifier stored in this file
	 */
	public int getFileID() {
		if (efCVCA == null) { return -1; }
		DERSequence s = (DERSequence) efCVCA;
		DEROctetString fid = (DEROctetString) s.getObjectAt(0);
		byte[] fidBytes = fid.getOctets();
		return Hex.hexStringToInt(Hex.bytesToHexString(fidBytes));
	}

	/**
	 * Returns the efCVCA short file identifier stored in this file, -1 if none
	 * or not present
	 * 
	 * @return the efCVCA short file identifier stored in this file
	 */
	public byte getShortFileID() {
		if (efCVCA == null) { return -1; }
		DERSequence s = (DERSequence) efCVCA;
		if (s.size() != 2) { return -1; }
		return ((DEROctetString) s.getObjectAt(1)).getOctets()[0];
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("TerminalAuthenticationInfo");
		result.append("[");
		result.append("fileID = " + getFileID());
		result.append("]");
		return result.toString();
	}

	public int hashCode() {
		return 123
		+ 7 * (oid == null ? 0 : oid.hashCode())
		+ 5 * version
		+ 3 * (efCVCA == null ? 1 : efCVCA.hashCode());
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!TerminalAuthenticationInfo.class.equals(other.getClass())) { return false; }
		TerminalAuthenticationInfo otherTerminalAuthenticationInfo = (TerminalAuthenticationInfo)other;
		if (efCVCA == null && otherTerminalAuthenticationInfo.efCVCA != null) { return false; }
		if (efCVCA != null && otherTerminalAuthenticationInfo.efCVCA == null) { return false; }
		return getDERObject().equals(otherTerminalAuthenticationInfo.getDERObject());
	}
	
	/* ONLY NON-PUBLIC METHODS BELOW */

	/**
	 * Checks whether the given object identifier identifies a
	 * TerminalAuthenticationInfo structure.
	 * 
	 * @param id
	 *            object identifier
	 * @return true if the match is positive
	 */
	static boolean checkRequiredIdentifier(String id) {
		return ID_TA_OID.equals(id);
	}
	
	/**
	 * Checks the correctness of the data for this instance of SecurityInfo
	 */
	private void checkFields() {
		try {
			if (!checkRequiredIdentifier(oid)) { throw new IllegalArgumentException("Wrong identifier: " + oid); }
			if (version != VERSION_NUM) { throw new IllegalArgumentException("Wrong version"); }
			if (efCVCA != null) {
				DERSequence sequence = (DERSequence) efCVCA;
				DEROctetString fid = (DEROctetString) sequence.getObjectAt(0);
				if (fid.getOctets().length != 2) { throw new IllegalArgumentException("Malformed FID."); }
				if (sequence.size() == 2) {
					DEROctetString sfi = (DEROctetString) sequence.getObjectAt(1);
					if (sfi.getOctets().length != 1) {
						throw new IllegalArgumentException("Malformed SFI.");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed TerminalAuthenticationInfo.");
		}
	}
}
