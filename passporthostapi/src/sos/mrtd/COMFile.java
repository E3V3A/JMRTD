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
 * $Id$
 */

package sos.mrtd;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import sos.smartcards.CardServiceException;
import sos.tlv.BERTLVInputStream;
import sos.tlv.BERTLVObject;

/**
 * File structure for the EF_COM file.
 * This file contains the common data (version and
 * data group presence table) information.
 * 
 * FIXME: get rid of all those BERTLVObjects in favor of BERTLVInputStreams. -- MO
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class COMFile extends PassportFile
{
	private static final int TAG_LIST_TAG = 0x5C;
	private static final int VERSION_UNICODE_TAG = 0x5F36;
	private static final int VERSION_LDS_TAG = 0x5F01;

	private String versionLDS;
	private String updateLevelLDS;
	private String majorVersionUnicode;
	private String minorVersionUnicode;
	private String releaseLevelUnicode;
	private List<Integer> tagList;

	/**
	 * Constructs a new file.
	 * 
	 * @param versionLDS a numerical string of length 2
	 * @param updateLevelLDS a numerical string of length 2
	 * @param majorVersionUnicode a numerical string of length 2
	 * @param minorVersionUnicode a numerical string of length 2
	 * @param releaseLevelUnicode a numerical string of length 2
	 * @param tagList a list of ICAO data group tags
	 * 
	 * @throws IllegalArgumentException if the input is not well-formed
	 */
	public COMFile(String versionLDS, String updateLevelLDS,
			String majorVersionUnicode, String minorVersionUnicode,
			String releaseLevelUnicode, List<Integer> tagList) {
		if (versionLDS == null || versionLDS.length() != 2
				|| updateLevelLDS == null || updateLevelLDS.length() != 2
				|| majorVersionUnicode == null || majorVersionUnicode.length() != 2
				|| minorVersionUnicode == null || minorVersionUnicode.length() != 2
				|| releaseLevelUnicode == null || releaseLevelUnicode.length() != 2
				|| tagList == null) {
			throw new IllegalArgumentException();
		}
		this.versionLDS = versionLDS;
		this.updateLevelLDS = updateLevelLDS;
		this.majorVersionUnicode = majorVersionUnicode;
		this.minorVersionUnicode = minorVersionUnicode;
		this.releaseLevelUnicode = releaseLevelUnicode;
		this.tagList = new ArrayList<Integer>();
		this.tagList.addAll(tagList);
		// System.arraycopy(tagList, 0, this.tagList, 0, tagList.length);
	}

	/**
	 * Constructs a new EF_COM file based on the encoded
	 * value in <code>in</code>.
	 * 
	 * @param in should contain a TLV object with appropriate
	 *           tag and contents
	 * 
	 * @throws IOException if the input could not be decoded
	 */
	public COMFile(InputStream in) throws CardServiceException {
		try {
			BERTLVInputStream tlvIn = new BERTLVInputStream(in);
			int tag = tlvIn.readTag();
			if (tag != EF_COM_TAG) {
				throw new IOException("Wrong tag!");
			}
			int length = tlvIn.readLength();
			byte[] valueBytes = tlvIn.readValue();

			BERTLVObject object = new BERTLVObject(tag, valueBytes);

			BERTLVObject versionLDSObject = object.getSubObject(VERSION_LDS_TAG);
			BERTLVObject versionUnicodeObject = object.getSubObject(VERSION_UNICODE_TAG);
			BERTLVObject tagListObject = object.getSubObject(TAG_LIST_TAG);
			byte[] versionLDSBytes = (byte[])versionLDSObject.getValue();
			if (versionLDSBytes.length != 4) {
				throw new IllegalArgumentException("Wrong length of LDS version object");
			}
			versionLDS = new String(versionLDSBytes, 0, 2);
			updateLevelLDS = new String(versionLDSBytes, 2, 2);
			byte[] versionUnicodeBytes = (byte[])versionUnicodeObject.getValue();
			if (versionUnicodeBytes.length != 6) {
				throw new IllegalArgumentException("Wrong length of unicode version object");
			}
			majorVersionUnicode = new String(versionUnicodeBytes, 0, 2);
			minorVersionUnicode = new String(versionUnicodeBytes, 2, 2);
			releaseLevelUnicode = new String(versionUnicodeBytes, 4, 2);
			byte[] tagBytes = (byte[])tagListObject.getValue();
			tagList = new ArrayList<Integer>();
			for (int i = 0; i < tagBytes.length; i++) { int dgTag = (tagBytes[i] & 0xFF); tagList.add(dgTag); }
		} catch (IOException ioe) {
			throw new CardServiceException(ioe.toString());
		}
	}

	/**
	 * The tag byte of this file.
	 * 
	 * @return the tag
	 */
	public int getTag() {
		return EF_COM_TAG;
	}

	/**
	 * Gets the LDS version as a dot seperated string
	 * containing version and update level.
	 * 
	 * @return a string of the form "aa.bb"
	 */
	public String getLDSVersion() {
		return versionLDS + "." + updateLevelLDS;
	}

	/**
	 * Gets the unicode version as a dot seperated string
	 * containing major version, minor version, and release level.
	 * 
	 * @return a string of the form "aa.bb.cc"
	 */
	public String getUnicodeVersion() {
		return majorVersionUnicode
		+ "." + minorVersionUnicode
		+ "." + releaseLevelUnicode;
	}

	/**
	 * Gets the ICAO datagroup tags as a list of bytes.
	 * 
	 * @return a list of bytes
	 */
	public List<Integer> getTagList() {
		return tagList;
	}

	public byte[] getEncoded() {
		try {
			byte[] versionLDSBytes = (versionLDS + updateLevelLDS).getBytes();
			BERTLVObject versionLDSObject = new BERTLVObject(VERSION_LDS_TAG, versionLDSBytes);
			byte[] versionUnicodeBytes =
				(majorVersionUnicode + minorVersionUnicode + releaseLevelUnicode).getBytes();
			BERTLVObject versionUnicodeObject = new BERTLVObject(VERSION_UNICODE_TAG, versionUnicodeBytes);
			byte[] tagListAsBytes = new byte[tagList.size()];
			for (int i = 0; i < tagList.size(); i++) {
				int dgTag = tagList.get(i);
				tagListAsBytes[i] = (byte)dgTag;
			}
			BERTLVObject tagListObject = new BERTLVObject(TAG_LIST_TAG, tagListAsBytes);
			BERTLVObject[] value = { versionLDSObject, versionUnicodeObject, tagListObject };
			BERTLVObject ef011E =
				new BERTLVObject(EF_COM_TAG, value);
			ef011E.reconstructLength();
			return ef011E.getEncoded();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("COMFile ");
		result.append("LDS " + versionLDS + "." + updateLevelLDS);
		result.append(", ");
		result.append("Unicode " + majorVersionUnicode + "." + minorVersionUnicode + "." + releaseLevelUnicode);
		result.append(", ");
		int i = 0;
		result.append("[");
		int dgCount = tagList.size();
		for (int tag: tagList) {
			result.append("DG" + PassportFile.lookupDataGroupNumberByTag(tag));
			if (i < dgCount - 1) { result.append(", "); }
			i++;
		}
		result.append("]");
		return result.toString();
	}
	
	/**
	 * Whether other is equal to this file.
	 * 
	 * @return a boolean
	 */
	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!other.getClass().equals(getClass())) { return false; }
		COMFile otherCOMFile = (COMFile)other;
		return versionLDS.equals(otherCOMFile.versionLDS) &&
			updateLevelLDS.equals(otherCOMFile.updateLevelLDS) &&
			majorVersionUnicode.equals(otherCOMFile.majorVersionUnicode) &&
			minorVersionUnicode.equals(otherCOMFile.minorVersionUnicode) &&
			releaseLevelUnicode.equals(otherCOMFile.releaseLevelUnicode) &&
			tagList.equals(otherCOMFile.tagList);
	}
	
	public int hashCode() {
		return 3 * versionLDS.hashCode() +
			5 * updateLevelLDS.hashCode() +
			7 * majorVersionUnicode.hashCode() +
			11 * minorVersionUnicode.hashCode() +
			13 * releaseLevelUnicode.hashCode() +
			17 * tagList.hashCode();
	}
}
