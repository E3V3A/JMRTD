/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2011  The JMRTD team
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
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.scuba.tlv.TLVInputStream;
import net.sourceforge.scuba.tlv.TLVOutputStream;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;

/**
 * Data Group 14 stores a set of SecurityInfos for Extended Access Control, see
 * EAC 1.11 spec. To us the interesting bits are: the map of public (EC or DH)
 * keys, the map of protocol identifiers which should match the key's map (not
 * checked here!), and the file identifier of the efCVCA file.
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 */
public class DG14File extends DataGroup
{
	/** The different security infos that make up this file */
	private List<SecurityInfo> securityInfos;

	/**
	 * Constructs a new DG14 file from the provided data.
	 *  
	 * @param securityInfos a list of security infos
	 */
	public DG14File(List<SecurityInfo> securityInfos) {
		super(EF_DG14_TAG);
		this.securityInfos = new ArrayList<SecurityInfo>(securityInfos);
	}

	/**
	 * Constructs a new DG14 file from the data in <code>in</code>.
	 * 
	 * @param in
	 *            the input stream to parse the data from
	 */
	public DG14File(InputStream in) {
		super(EF_DG14_TAG, in);
	}

	protected void readContent(TLVInputStream tlvIn) throws IOException {
		securityInfos = new ArrayList<SecurityInfo>();
		byte[] value = tlvIn.readValue();
		ASN1InputStream asn1In = new ASN1InputStream(value);
		DERSet set = (DERSet)asn1In.readObject();
		/* TODO: check if it contains additional objects? */
		asn1In.close();
		for (int i = 0; i < set.size(); i++) {
			DERObject o = set.getObjectAt(i).getDERObject();
			SecurityInfo si = SecurityInfo.createSecurityInfo(o);
			securityInfos.add(si);
		}
	}

	/**
	 * Constructs a new DG14 file from the provided data.
	 * 
	 * @param publicKeys
	 *            the map of (EC or DH) public keys indexed by key identifiers.
	 *            If only one key, the index can be -1.
	 * @param chipInfoMap
	 *            the map of protocol identifiers for EAC indexed by key
	 *            identifiers. If only one protocol identifier, the index can be
	 *            -1.
	 * @param cvcaFileIdList
	 *            the list of file identifiers of the efCVCA file(s)
	 * @param cvcaShortFileIdMap
	 *            a mapping of file identifiers (see above) to short file
	 *            identifiers (can be empty)
	 */
	// FIXME: why not simply use the List<SecurityInfo>() constructor?
	public DG14File(Map<Integer, PublicKey> publicKeys,
			Map<Integer, DERObjectIdentifier> chipInfoMap,
			List<Integer> cvcaFileIdList,
			Map<Integer, Integer> cvcaShortFileIdMap) {
		super(EF_DG14_TAG);
		if (publicKeys.size() == 0) {
			throw new IllegalArgumentException("Need at least one key.");
		}
		securityInfos = new ArrayList<SecurityInfo>();
		if (publicKeys.size() == 1 && publicKeys.containsKey(-1)) {
			PublicKey publicKey = publicKeys.get(-1);
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey));
		} else {
			for (Map.Entry<Integer, PublicKey> entry: publicKeys.entrySet()) {
				int i = entry.getKey();
				PublicKey publicKey = entry.getValue();
				if (i < 0) {
					throw new IllegalArgumentException("Wrong key Id: " + i);
				}
				securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey, i));
			}
		}
		if (chipInfoMap != null && chipInfoMap.size() > 0) {
			if (chipInfoMap.size() == 1 && chipInfoMap.containsKey(-1)) {
				securityInfos.add(new ChipAuthenticationInfo(chipInfoMap.get(-1).getId(),
						ChipAuthenticationInfo.VERSION_NUM));
			} else {
				for (int i : chipInfoMap.keySet()) {
					securityInfos.add(new ChipAuthenticationInfo(chipInfoMap.get(i).getId(),
							ChipAuthenticationInfo.VERSION_NUM, i));
				}
			}
		}
		if (cvcaFileIdList == null || cvcaFileIdList.size() == 0) {
			securityInfos.add(new TerminalAuthenticationInfo(
					EACObjectIdentifiers.id_TA.getId(),
					TerminalAuthenticationInfo.VERSION_NUM));
		} else {
			for (Integer i : cvcaFileIdList) {
				securityInfos.add(new TerminalAuthenticationInfo(i,
						cvcaShortFileIdMap.containsKey(i) ? cvcaShortFileIdMap.get(i) : -1));
			}
		}
	}
	
	protected void writeContent(TLVOutputStream out) throws IOException {
		ASN1EncodableVector vector = new ASN1EncodableVector();
		for (SecurityInfo si : securityInfos) {
			vector.add(si.getDERObject());
		}
		DERSet derSet = new DERSet(vector);
		out.writeValue(derSet.getDEREncoded());
	}

	/**
	 * Returns the list of file identifier references to efCVCA files, possibly
	 * empty.
	 * 
	 * @return the list of file identifier
	 */
	public List<Integer> getCVCAFileIds() {
		List<Integer> cvcaFiles = new ArrayList<Integer>();
		for (SecurityInfo si : securityInfos) {
			if (si instanceof TerminalAuthenticationInfo) {
				int i = ((TerminalAuthenticationInfo) si).getFileID();
				if (i != -1) {
					cvcaFiles.add(i);
				}
			}
		}
		return cvcaFiles;
	}

	/**
	 * Returns a corresponding short file ID.
	 * 
	 * @param fileId
	 *            the file ID
	 * @return an SFI for the given file ID, -1 if not present
	 */
	public byte getCVCAShortFileId(int fileId) {
		for (SecurityInfo si : securityInfos) {
			if (si instanceof TerminalAuthenticationInfo) {
				if (((TerminalAuthenticationInfo) si).getFileID() == fileId) {
					return ((TerminalAuthenticationInfo) si).getShortFileID();
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the mapping of key identifiers to EAC protocol identifiers
	 * contained in this file. The key identifier may be -1 if there is only one
	 * protocol identifier.
	 * 
	 * @return the mapping of key identifiers to EAC protocol identifiers
	 */
	public Map<Integer, DERObjectIdentifier> getChipAuthenticationInfos() {
		Map<Integer, DERObjectIdentifier> map = new TreeMap<Integer, DERObjectIdentifier>();
		for (SecurityInfo securityInfo : securityInfos) {
			if (securityInfo instanceof ChipAuthenticationInfo) {
				ChipAuthenticationInfo chipAuthNInfo = (ChipAuthenticationInfo)securityInfo;
				map.put(chipAuthNInfo.getKeyId(), new DERObjectIdentifier(chipAuthNInfo.getObjectIdentifier()));
				if (chipAuthNInfo.getKeyId() == -1) {
					return map;
				}
			}
		}
		return map;
	}

	/**
	 * Returns the mapping of key identifiers to public keys. The key identifier
	 * may be -1 if there is only one key.
	 * 
	 * @return the mapping of key identifiers to public keys
	 */
	public Map<Integer, PublicKey> getPublicKeys() {
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

	public List<SecurityInfo> getSecurityInfos() {
		return securityInfos;
	}

	public String toString() {
		return "DG14File [" + securityInfos.toString() + "]";
	}

	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (!(obj.getClass().equals(this.getClass()))) { return false; }
		DG14File other = (DG14File)obj;
		return (securityInfos == null && other.securityInfos == null)
		|| (securityInfos != null && securityInfos.equals(other.securityInfos));
	}

	public int hashCode() {
		return 5 * securityInfos.hashCode() + 41;
	}
}
