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

package org.jmrtd.lds;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * File structure for the EF_DG3 file.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG3File extends CBEFFDataGroup
{

	private List<FingerInfo> fingerPrints;

	/**
	 * Creates a new file based on an input stream.
	 *
	 * @param in an input stream
	 */
	public DG3File(InputStream in) {
		super(in);
	}

	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject;
		}
		return null;
	}

	public int getTag() {
		return EF_DG3_TAG;
	}

	public List<FingerInfo> getFingerPrints() {
		return fingerPrints;
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DG3File";
	}

	/**
	 * Reads biometric data block.
	 * Based on ISO/IEC FCD 19794-4 aka Annex F.
	 * 
	 * TODO: work in progress... -- MO
	 */
	protected void readBiometricData(InputStream in, int valueLength) throws IOException {
		DataInputStream dataIn = (in instanceof DataInputStream) ? (DataInputStream)in : new DataInputStream(in);
		/* General Record Header (32) */
		int fir0 = dataIn.readInt(); /* header (e.g. "FIR", 0x00) (4) */
		if (fir0 != 0x46495200) { throw new IllegalArgumentException("'FIR0' marker expected! Found " + Integer.toHexString(fir0)); }
		/* int version = */ dataIn.readInt(); /* version in ASCII (e.g. "010" 0x00) (4) */
		/* long length = */ readUnsignedLong(dataIn, 6); /* & 0x0000FFFFFFFFFFFFL */;
		/* int captureDeviceID = */ dataIn.readUnsignedShort();
		/* int imageAcquisitionLevel = */ dataIn.readUnsignedShort();
		int fingerCount = dataIn.readUnsignedByte();
		int scaleUnits = dataIn.readUnsignedByte(); /* 1 -> PPI, 2 -> PPCM */
		int scanResH = dataIn.readUnsignedShort();
		int scanResV = dataIn.readUnsignedShort();
		int imgResH = dataIn.readUnsignedShort(); /* should be <= scanResH */
		int imgResV = dataIn.readUnsignedShort(); /* should be <= scanResV */
		int depth = dataIn.readUnsignedByte(); /* 1 - 16 bits, i.e. 2 - 65546 gray levels */
		int compressionAlg = dataIn.readUnsignedByte(); /* 0 Uncompressed – no bit packing
														   1 Uncompressed – bit packed
														   2 Compressed – WSQ
														   3 Compressed – JPEG
														   4 Compressed – JPEG2000
														   5 PNG
		 */
		/* int RFU = */ dataIn.readUnsignedShort(); /* Should be 0x0000 */

		String mimeType = null;
		switch (compressionAlg) {
		case 0: break;
		case 1: break;
		case 2: mimeType = "images/x-wsq"; break;
		case 3: mimeType = "image/jpeg"; break;
		case 4: mimeType = "image/jpeg2000"; break;
		case 5: mimeType = "image/png";  break;
		}

		for (int i = 0; i < fingerCount; i++) {
			FingerInfo fingerInfo = new FingerInfo(dataIn, mimeType);
			addFingerInfo(fingerInfo);
		}
	}

	private void addFingerInfo(FingerInfo fingerInfo) {
		if (fingerPrints == null) { fingerPrints = new ArrayList<FingerInfo>(); }
		fingerPrints.add(fingerInfo);
	}

	private long readUnsignedLong(DataInputStream dataIn, int byteCount) throws IOException {
		byte[] buf = new byte[byteCount];
		dataIn.readFully(buf);
		long result = 0;
		for (int i = 0; i < buf.length; i++) {
			result *= 10;
			result += buf[i] & 0xFF;
		}
		return result;
	}
}
