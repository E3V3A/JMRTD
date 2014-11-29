/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2014  The JMRTD team
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.scuba.tlv.TLVInputStream;
import net.sf.scuba.tlv.TLVOutputStream;

/**
 * File structure for the EF_COM file.
 * This file contains the common data (version and
 * data group presence table) information.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class COMFile extends DataGroup { /* FIXME: strictly speaking this is not a DataGroup, consider changing the name of the DataGroup class. */

	private static final long serialVersionUID = 2002455279067170063L;

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
	 * Constructs a new COM file.
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
			String releaseLevelUnicode, int[] tagList) {
		super(EF_COM_TAG);
		initialize(versionLDS, updateLevelLDS, majorVersionUnicode, minorVersionUnicode, releaseLevelUnicode, tagList);
	}

	/**
	 * Constructs a new COM file.
	 * 
	 * @param ldsVer a "x.y" version number
	 * @param unicodeVer a "x.y.z" version number
	 * @param tagList list of tags
	 */
	public COMFile(String ldsVer, String unicodeVer, int [] tagList) {
		super(EF_COM_TAG);
		try {
			if (ldsVer == null) { throw new IllegalArgumentException("Null versionLDS"); }
			if (unicodeVer == null) { throw new IllegalArgumentException("Null versionUnicode"); }
			StringTokenizer st = new StringTokenizer(ldsVer, ".");
			if (st.countTokens() != 2) { throw new IllegalArgumentException("Could not parse LDS version. Expecting 2 level version number x.y."); }
			Integer versionLDS = Integer.parseInt(st.nextToken().trim());
			Integer updateLevelLDS = Integer.parseInt(st.nextToken().trim());
			st = new StringTokenizer(unicodeVer, ".");
			if (st.countTokens() != 3) { throw new IllegalArgumentException("Could not parse unicode version. Expecting 3 level version number x.y.z."); }
			Integer majorVersionUnicode = Integer.parseInt(st.nextToken().trim());
			Integer minorVersionUnicode = Integer.parseInt(st.nextToken().trim());
			Integer releaseLevelUnicode = Integer.parseInt(st.nextToken().trim());
			initialize(
				String.format("%02d", versionLDS),
				String.format("%02d", updateLevelLDS),
				String.format("%02d", majorVersionUnicode),
				String.format("%02d", minorVersionUnicode),
				String.format("%02d", releaseLevelUnicode),
				tagList);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Could not parse version number. " + nfe.getMessage());
		} catch (IllegalFormatConversionException ifce) {
			throw new IllegalArgumentException("Could not parse version number. " + ifce.getMessage());
		}
	}

	private void initialize(String versionLDS, String updateLevelLDS,
			String majorVersionUnicode, String minorVersionUnicode,
			String releaseLevelUnicode, int[] tagList) {
		if (tagList == null) { throw new IllegalArgumentException("Null tag list"); }
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
		this.tagList = new ArrayList<Integer>(tagList.length);
		for (int tag: tagList) { this.tagList.add(tag); }
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
	public COMFile(InputStream in) throws IOException {
		super(EF_COM_TAG, in);
	}

	protected void readContent(InputStream in) throws IOException {
		TLVInputStream tlvIn = in instanceof TLVInputStream ? (TLVInputStream)in : new TLVInputStream(in);
		int versionLDSTag = tlvIn.readTag();
		if (versionLDSTag != VERSION_LDS_TAG) {
			throw new IllegalArgumentException("Excepected VERSION_LDS_TAG (" + Integer.toHexString(VERSION_LDS_TAG) + "), found " + Integer.toHexString(versionLDSTag));
		}
		int versionLDSLength = tlvIn.readLength();
		if (versionLDSLength != 4) {
			throw new IllegalArgumentException("Wrong length of LDS version object");
		}
		byte[] versionLDSBytes = tlvIn.readValue();
		versionLDS = new String(versionLDSBytes, 0, 2);
		updateLevelLDS = new String(versionLDSBytes, 2, 2);

		int versionUnicodeTag = tlvIn.readTag();
		if (versionUnicodeTag != VERSION_UNICODE_TAG) {
			throw new IllegalArgumentException("Expected VERSION_UNICODE_TAG (" + Integer.toHexString(VERSION_UNICODE_TAG) + "), found " + Integer.toHexString(versionUnicodeTag));
		}
		int versionUnicodeLength = tlvIn.readLength();
		if (versionUnicodeLength != 6) {
			throw new IllegalArgumentException("Wrong length of LDS version object");
		}
		byte[] versionUnicodeBytes = tlvIn.readValue();
		majorVersionUnicode = new String(versionUnicodeBytes, 0, 2);
		minorVersionUnicode = new String(versionUnicodeBytes, 2, 2);
		releaseLevelUnicode = new String(versionUnicodeBytes, 4, 2);

		int tagListTag = tlvIn.readTag();
		if (tagListTag != TAG_LIST_TAG) {
			throw new IllegalArgumentException("Expected TAG_LIST_TAG (" + Integer.toHexString(TAG_LIST_TAG) + "), found " + Integer.toHexString(tagListTag));
		}
		/* int tagListLength = */ tlvIn.readLength();
		byte[] tagBytes = tlvIn.readValue();
		tagList = new ArrayList<Integer>();
		for (int i = 0; i < tagBytes.length; i++) { int dgTag = (tagBytes[i] & 0xFF); tagList.add(dgTag); }
	}

	/**
	 * Gets the LDS version as a dot seperated string
	 * containing version and update level.
	 * 
	 * @return a string of the form "a.b"
	 */
	public String getLDSVersion() {
		String ldsVersion = versionLDS + "." + updateLevelLDS;
		try {
			int major = Integer.parseInt(versionLDS);
			int minor = Integer.parseInt(updateLevelLDS);
			ldsVersion = major + "." + minor;
		} catch (NumberFormatException nfe) {
			/* NOTE: leave ldsVersion as is. */
		}
		return ldsVersion;
	}

	/**
	 * Gets the unicode version as a dot seperated string
	 * containing major version, minor version, and release level.
	 * 
	 * @return a string of the form "aa.bb.cc"
	 */
	public String getUnicodeVersion() {
		String unicodeVersion = majorVersionUnicode
				+ "." + minorVersionUnicode
				+ "." + releaseLevelUnicode;

		try {
			int major = Integer.parseInt(majorVersionUnicode);
			int minor = Integer.parseInt(minorVersionUnicode);
			int releaseLevel = Integer.parseInt(releaseLevelUnicode);
			unicodeVersion = major + "." + minor + "." + releaseLevel;
		} catch (NumberFormatException nfe) {
			/* NOTE: leave unicodeVersion as is. */
		}

		return unicodeVersion;
	}

	/**
	 * Gets the ICAO datagroup tags as a list of bytes.
	 * 
	 * @return a list of bytes
	 */
	public int[] getTagList() {
		int[] result = new int[tagList.size()];
		int i = 0;
		for (Integer tag: tagList) {
			result[i++] = tag;
		}
		return result;
	}

	/**
	 * Inserts a tag in a proper place if not already present
	 * 
	 * @param tag tag to insert
	 */
	public void insertTag(Integer tag) {
		if(tagList.contains(tag)) { return; }
		tagList.add(tag);
		Collections.sort(tagList);

	}

	protected void writeContent(OutputStream out) throws IOException {
		TLVOutputStream tlvOut = out instanceof TLVOutputStream ? (TLVOutputStream)out : new TLVOutputStream(out);
		tlvOut.writeTag(VERSION_LDS_TAG);
		tlvOut.writeValue((versionLDS + updateLevelLDS).getBytes());
		tlvOut.writeTag(VERSION_UNICODE_TAG);
		tlvOut.writeValue((majorVersionUnicode + minorVersionUnicode + releaseLevelUnicode).getBytes());
		tlvOut.writeTag(TAG_LIST_TAG);

		tlvOut.writeLength(tagList.size());
		for (int tag: tagList) {
			tlvOut.write((byte)tag);
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
			result.append("DG" + LDSFileUtil.lookupDataGroupNumberByTag(tag));
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
