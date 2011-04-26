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
	/* FIXME: meaningful equals(), hashCode(), toString() */
	
	private static final byte[] FORMAT_IDENTIFIER = { 'F', 'I', 'R', 0x00 };
	private static final byte[] VERSION_NUMBER = { '0', '1', '0', 0x00 };

	private List<FingerInfo> fingerInfos;

	/**
	 * Creates a new file with zero images.
	 */
	public DG3File() {
		super(EF_DG3_TAG, BIOMETRIC_DATA_BLOCK_TAG);
		if (fingerInfos == null) { fingerInfos = new ArrayList<FingerInfo>(); }
		isSourceConsistent = false;
	}
	
	/**
	 * Creates a new file based on an input stream.
	 *
	 * @param in an input stream
	 */
	public DG3File(InputStream in) {
		super(EF_DG3_TAG, in);
	}

	public List<FingerInfo> getBiometricTemplates() {
		return fingerInfos;
	}

	public BiometricTemplate getBiometricTemplate(int index) {
		return fingerInfos.get(index);
	}
	
	public int getBiometricTemplateCount() {
		return fingerInfos == null ? 0 : fingerInfos.size();
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
		/* General record header according to Table 2 in Section 7.1 of 19794-4. */
		
		DataInputStream dataIn = (in instanceof DataInputStream) ? (DataInputStream)in : new DataInputStream(in);
		/* General Record Header (32) */
		int fir0 = dataIn.readInt(); /* header (e.g. "FIR", 0x00) (4) */
		if (fir0 != 0x46495200) { throw new IllegalArgumentException("'FIR0' marker expected! Found " + Integer.toHexString(fir0)); }
		/* int version = */ dataIn.readInt(); /* version in ASCII (e.g. "010" 0x00) (4) */
		long recordLength = readUnsignedLong(dataIn, 6); /* & 0x0000FFFFFFFFFFFFL */;
		System.out.println("DEBUG: DG3File.read length = " + recordLength);
		/* int captureDeviceID = */ dataIn.readUnsignedShort(); /* all zeros means 'unreported', only lower 12-bits used, see 7.1.4 */
		/* int imageAcquisitionLevel = */ dataIn.readUnsignedShort();
		int fingerCount = dataIn.readUnsignedByte();
		/* int scaleUnits = */ dataIn.readUnsignedByte(); /* 1 -> PPI, 2 -> PPCM */
		/* int scanResH = */ dataIn.readUnsignedShort();
		/* int scanResV = */ dataIn.readUnsignedShort();
		/* int imgResH = */ dataIn.readUnsignedShort(); /* should be <= scanResH */
		/* int imgResV = */ dataIn.readUnsignedShort(); /* should be <= scanResV */
		/* int depth = */ dataIn.readUnsignedByte(); /* 1 - 16 bits, i.e. 2 - 65546 gray levels */
		int compressionAlg = dataIn.readUnsignedByte(); /* 0 Uncompressed, no bit packing
														   1 Uncompressed, bit packed
														   2 Compressed, WSQ
														   3 Compressed, JPEG
														   4 Compressed, JPEG2000
														   5 PNG
		 */
		/* int RFU = */ dataIn.readUnsignedShort(); /* Should be 0x0000 */

		String mimeType = toMimeType(compressionAlg);


		for (int i = 0; i < fingerCount; i++) {
			FingerInfo info = new FingerInfo(dataIn, mimeType);
			addFingerInfo(info);
		}
	}

	protected void writeBiometricData(OutputStream out, BiometricTemplate info) throws IOException {
		/* NOTE: Should be 32... */
		long headerLength = FORMAT_IDENTIFIER.length + VERSION_NUMBER.length + 6 + 2 + 2 + 1 + 1 + 2 + 2 + 2 + 2 + 1 + 1 + 2;

		byte[] infoBytes = info.getEncoded();
		long infoLength = infoBytes.length;

		DataOutputStream dataOut = out instanceof DataOutputStream ? (DataOutputStream)out : new DataOutputStream(out);
		dataOut.write(FORMAT_IDENTIFIER); /* 4 */
		dataOut.write(VERSION_NUMBER); /* + 4 = 8 */
		writeLong(headerLength + infoLength, dataOut, 6); /* + 6 = 14 */
		dataOut.writeShort(0); /* captureDeviceId */ /* + 2 = 16 */
		dataOut.writeShort(0); /* imageAcquisitionLevel */ /* +2 = 18 */
		int fingerCount = 1;
		dataOut.writeByte(fingerCount); /* + 1 = 19 */
		dataOut.writeByte(1); /* scaleUnits, 1 -> PPI, 2 -> PPCM */ /* + 1 = 20 */
		dataOut.writeShort(1); /* scanResH */ /* + 2 = 22 */
		dataOut.writeShort(1); /* scanResV */ /* + 2 = 24 */
		dataOut.writeShort(1); /* imgResH */ /* + 2 = 26 */
		dataOut.writeShort(1); /* imgResV */ /* + 2 = 28 */
		dataOut.writeByte(8); /* depth */ /* + 1 = 29 */

		dataOut.writeByte(fromMimeType(info.getMimeType())); /* + 1 = 30 */
		dataOut.writeShort(0x0000); /* RFU */ /* + 2 = 32 */
		dataOut.write(infoBytes);
		dataOut.flush();
	}

	private void addFingerInfo(FingerInfo fingerInfo) {
		if (fingerInfos == null) { fingerInfos = new ArrayList<FingerInfo>(); }
		fingerInfos.add(fingerInfo);
	}

	private static long readUnsignedLong(InputStream in, int byteCount) throws IOException {
		DataInputStream dataIn = in instanceof DataInputStream ? (DataInputStream)in : new DataInputStream(in);
		byte[] buf = new byte[byteCount];
		dataIn.readFully(buf);
		long result = 0L;
		for (int i = 0; i < byteCount; i++) {
			result <<= 8;
			result += (int)(buf[i] & 0xFF);
		}
		return result;
	}

	private static void writeLong(long value, OutputStream dataOut, int byteCount) throws IOException {
		if (byteCount <= 0) { return; }
		for (int i = 0; i < (byteCount - 8); i++) {
			dataOut.write(0);
		}
		if (byteCount > 8) { byteCount = 8; }
		for (int i = (byteCount - 1); i >= 0; i--) {
			long mask = (long)(0xFFL << (i * 8));
			byte b = (byte)((value & mask) >> (i * 8));
			dataOut.write(b);
		}		
	}

	/**
	 * Compression algorithm codes based on Table 3 in Section 7.1.13 of 19794-4.
	 * 
	 * 0 Uncompressed, no bit packing
	 * 1 Uncompressed, bit packed
	 * 2 Compressed, WSQ
	 * 3 Compressed, JPEG
	 * 4 Compressed, JPEG2000
	 * 5 PNG 
	 * 
	 * @param compressionAlg
	 * 
	 * @return MIME type string
	 */
	private String toMimeType(int compressionAlg) {
		switch (compressionAlg) {
		case 0: break;
		case 1: break;
		case 2: return "image/x-wsq";
		case 3: return "image/jpeg";
		case 4: return "image/jpeg2000";
		case 5: return "image/png";
		}
		return null;
	}

	private int fromMimeType(String mimeType) {
		if ("image/x-wsq".equals(mimeType)) { return 2; }
		if ("image/jpeg".equals(mimeType)) { return 3; }
		if ("image/jpeg2000".equals(mimeType)) { return 4; }
		if ("images/png".equals(mimeType)) { return 5; }
		throw new IllegalArgumentException("Did not recognize mimeType");
	}
}
