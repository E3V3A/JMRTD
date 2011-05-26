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
 * $Id$
 */

package org.jmrtd.lds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * File structure for the EF_DG2 file.
 * Datagroup 2 contains the facial features of
 * the document holder.
 * See A 13.3 in MRTD's LDS document.
 * 
 * NOTE: multiple FaceInfos may be embedded in two ways:
 * 1) as multiple images in the same record (see Fig. 3 in ISO/IEC
 * 19794-5)
 * 2) as multiple records (see A 13.3 in LDS technical report).
 * For writing we choose option 2, because otherwise we have to
 * precalc the total length of all FaceInfos, which sucks. -- CB
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG2File extends CBEFFDataGroup
{
	// Facial Record Header, Sect. 5.4, ISO SC37
	private static final byte[] FORMAT_IDENTIFIER = { 'F', 'A', 'C', 0x00 };
	private static final byte[] VERSION_NUMBER = { '0', '1', '0', 0x00 };

	private List<FaceInfo> faceInfos;

	/**
	 * Creates a new file with zero images.
	 */
	public DG2File() {
		super(EF_DG2_TAG, BIOMETRIC_DATA_BLOCK_TAG);
		if (faceInfos == null) { faceInfos = new ArrayList<FaceInfo>(); }
		isSourceConsistent = false;
	}

	/**
	 * Creates a new file based on an input stream.
	 *
	 * @param in an input stream
	 */
	public DG2File(InputStream in) {
		super(EF_DG2_TAG, in);
	}

	protected void readBiometricData(InputStream in, int valueLength) throws IOException {
		DataInputStream dataIn = in instanceof DataInputStream ? (DataInputStream)in : new DataInputStream(in);
		/* Facial Record Header (14) */
		/* int fac0 = */ dataIn.readInt(); // header (e.g. "FAC", 0x00)
		/* int version = */ dataIn.readInt(); // version in ASCII (e.g. "010" 0x00)
		/* long length = */ dataIn.readInt() /* & 0x000000FFFFFFFFL */;
		int faceCount = dataIn.readUnsignedShort();
		for (int i = 0; i < faceCount; i++) {
			addFaceInfo(new FaceInfo(dataIn));
		}
	}

	/**
	 * Adds an image to this file.
	 *
	 * @param fi the image to add
	 */
	public void addFaceInfo(FaceInfo fi) {
		if (faceInfos == null) { faceInfos = new ArrayList<FaceInfo>(); }
		faceInfos.add(fi);
		isSourceConsistent = false;
	}

	/**
	 * Removes an image from this file.
	 *
	 * @param index the index of the image to remove
	 */
	public void removeFaceInfo(int index) {
		faceInfos.remove(index);
		isSourceConsistent = false;
	}

	protected void writeBiometricData(OutputStream out, BiometricTemplate info) throws IOException {
		byte[] infoBytes = info.getEncoded();
		
		DataOutputStream dataOut = out instanceof DataOutputStream ? (DataOutputStream)out : new DataOutputStream(out);

		/** NOTE: Facial Record Header (5.4 of annex D). Should be 14... */
		int lengthOfRecord = FORMAT_IDENTIFIER.length + VERSION_NUMBER.length + 4 + 2 + infoBytes.length;
		dataOut.write(FORMAT_IDENTIFIER);
		dataOut.write(VERSION_NUMBER);
		dataOut.writeInt(lengthOfRecord);	/* The (4 byte) Length of Record field shall
											 * be the combined length in bytes for the record.
											 * This is the entire length of the record including
											 * the Facial Record Header and Facial Record Data.
											 */

		dataOut.writeShort(1); /* Number of facial images (fixed to 1 for now)... */
		dataOut.write(infoBytes);
		dataOut.flush();
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("DG2File");
		result.append(" [");
		int faceCount = faceInfos.size();
		int i = 0;
		for (FaceInfo faceInfo: faceInfos) {
			result.append(faceInfo.getWidth() + "x" + faceInfo.getHeight());
			if (i < faceCount - 1) { result.append(", "); }
			i++;
		}
		result.append("]");
		return result.toString();
	}

	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (!obj.getClass().equals(this.getClass())) { return false; }
		DG2File other = (DG2File)obj;
		if (faceInfos == null) { return other.faceInfos == null; }
		return Arrays.equals(getEncoded(), other.getEncoded());
	}

	public int hashCode() {
		if (faceInfos == null) { return 7 * 0x000FACE5 + 17; } /* FIXME: never happens :) */
		return 7 * faceInfos.hashCode() + 17;
	}

	/**
	 * Gets the images in this file.
	 *
	 * @return the images
	 */
	public List<FaceInfo> getBiometricTemplates() {
		return faceInfos;
	}
	
	public BiometricTemplate getBiometricTemplate(int index) {
		return faceInfos.get(index);
	}
	
	public int getBiometricTemplateCount() {
		return faceInfos == null ? 0 : faceInfos.size();
	}
}
