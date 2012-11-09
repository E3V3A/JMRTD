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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DLSet;

/**
 * Data Group 14 stores a set of SecurityInfos for Extended Access Control, see
 * EAC 1.11 spec. To us the interesting bits are: the map of public (EC or DH)
 * keys, the map of protocol identifiers which should match the key's map (not
 * checked here!), and the file identifier of the efCVCA file.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 */
public class DG14File extends DataGroup {

	private static final long serialVersionUID = -3536507558193769953L;

	/** The security infos that make up this file */
	private Set<SecurityInfo> securityInfos;

	/**
	 * Constructs a new DG14 file from the provided data.
	 *  
	 * @param securityInfos a list of security infos
	 */
	public DG14File(Collection<SecurityInfo> securityInfos) {
		super(EF_DG14_TAG);
		if (securityInfos == null) { throw new IllegalArgumentException("Null securityInfos"); }
		this.securityInfos = new HashSet<SecurityInfo>(securityInfos);
	}

	/**
	 * Constructs a new DG14 file from the data in <code>in</code>.
	 * 
	 * @param in
	 *            the input stream to parse the data from
	 */
	public DG14File(InputStream in) throws IOException {
		super(EF_DG14_TAG, in);
	}

	protected void readContent(InputStream inputStream) throws IOException {
		securityInfos = new HashSet<SecurityInfo>();
		ASN1InputStream asn1In = new ASN1InputStream(inputStream);
		ASN1Set set = (ASN1Set)asn1In.readObject();
		for (int i = 0; i < set.size(); i++) {
			ASN1Primitive object = set.getObjectAt(i).toASN1Primitive();
			SecurityInfo securityInfo = SecurityInfo.getInstance(object);
			securityInfos.add(securityInfo);
		}
	}

	/* FIXME: rewrite (using writeObject instead of getDERObject) to remove interface dependency on BC. */
	protected void writeContent(OutputStream outputStream) throws IOException {
		ASN1EncodableVector vector = new ASN1EncodableVector();
		for (SecurityInfo si : securityInfos) {
			vector.add(si.getDERObject());
		}
		ASN1Set derSet = new DLSet(vector);
		outputStream.write(derSet.getEncoded(ASN1Encoding.DER));
	}

	/**
	 * Gets the list of file identifier references to efCVCA files, possibly
	 * empty.
	 * 
	 * @return the list of file identifier
	 */
	public List<Integer> getCVCAFileIds() {
		List<Integer> cvcaFiles = new ArrayList<Integer>();
		for (SecurityInfo si : securityInfos) {
			if (si instanceof TerminalAuthenticationInfo) {
				int i = ((TerminalAuthenticationInfo) si).getFileId();
				if (i != -1) {
					cvcaFiles.add(i);
				}
			}
		}
		return cvcaFiles;
	}

	/**
	 * Gets a corresponding short file ID.
	 * 
	 * @param fileId
	 *            the file ID
	 * @return an SFI for the given file ID, -1 if not present
	 */
	public byte getCVCAShortFileId(int fileId) {
		for (SecurityInfo si : securityInfos) {
			if (si instanceof TerminalAuthenticationInfo) {
				if (((TerminalAuthenticationInfo) si).getFileId() == fileId) {
					return ((TerminalAuthenticationInfo) si).getShortFileId();
				}
			}
		}
		return -1;
	}

	/**
	 * Gets the mapping of key identifiers to EAC protocol identifiers
	 * contained in this file. The key identifier may be -1 if there is only one
	 * protocol identifier.
	 * 
	 * @return the mapping of key identifiers to EAC protocol identifiers
	 */
	public Map<Integer, String> getChipAuthenticationInfos() {
		Map<Integer, String> map = new TreeMap<Integer, String>();
		for (SecurityInfo securityInfo : securityInfos) {
			if (securityInfo instanceof ChipAuthenticationInfo) {
				ChipAuthenticationInfo chipAuthNInfo = (ChipAuthenticationInfo)securityInfo;
				map.put(chipAuthNInfo.getKeyId(), chipAuthNInfo.getObjectIdentifier());
				if (chipAuthNInfo.getKeyId() == -1) {
					return map;
				}
			}
		}
		return map;
	}

	/**
	 * Gets the mapping of key identifiers to public keys. The key identifier
	 * may be -1 if there is only one key.
	 * 
	 * @return the mapping of key identifiers to public keys
	 */
	public Map<Integer, PublicKey> getChipAuthenticationPublicKeyInfos() {
		Map<Integer, PublicKey> publicKeys = new TreeMap<Integer, PublicKey>();
		boolean foundOne = false;
		for (SecurityInfo securityInfo: securityInfos) {
			if (securityInfo instanceof ChipAuthenticationPublicKeyInfo) {
				ChipAuthenticationPublicKeyInfo info = (ChipAuthenticationPublicKeyInfo)securityInfo;
				publicKeys.put(info.getKeyId(), info.getSubjectPublicKey());
				foundOne = true;
			}
		}
		if (!foundOne) { throw new IllegalStateException("No keys?"); }
		return publicKeys;
	}

	/**
	 * Gets the security infos as an unordered collection.
	 * 
	 * @return security infos
	 */
	public Collection<SecurityInfo> getSecurityInfos() {
		return securityInfos;
	}

	public String toString() {
		return "DG14File [" + securityInfos.toString() + "]";
	}

	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (!(obj.getClass().equals(this.getClass()))) { return false; }
		DG14File other = (DG14File)obj;
		if (securityInfos == null) { return  other.securityInfos == null; }
		if (other.securityInfos == null) { return securityInfos == null; }
		return securityInfos.equals(other.securityInfos);
	}

	public int hashCode() {
		return 5 * securityInfos.hashCode() + 41;
	}
}
