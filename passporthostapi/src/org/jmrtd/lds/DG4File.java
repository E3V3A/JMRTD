/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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
 * File structure for the EF_DG4 file.
 * Based on ISO/IEC FCD 19794-6 (Biometric Data Interchange Formats –
 * Part 6: Iris Image Data) aka Annex E.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG4File extends CBEFFDataGroup
{	
	private static int
	IMAGE_QUAL_UNDEF = 0xFE, /* (decimal 254) */
	IMAGE_QUAL_LOW_LO = 0x1A,
	IMAGE_QUAL_LOW_HI = 0x32, /* (decimal 26-50) */
	IMAGE_QUAL_MED_LO = 0x33,
	IMAGE_QUAL_MED_HI = 0x4B, /* (decimal 51-75) */
	IMAGE_QUAL_HIGH_LO = 0x4C,
	IMAGE_QUAL_HIGH_HI = 0x64; /* (decimal 76-100) */
	
	/** Feature identifiers */
	private static int
	EYE_UNDEF = 0,
	EYE_RIGHT = 1,
	EYE_LEFT = 2;

	private List<IrisInfo> irisInfos;
	
	/* TODO: many more constants here, amongst others bitfields... */
	
	/**
	 * Constructs a new file based on an input stream.
	 * 
	 * @param in an input stream
	 */
	public DG4File(InputStream in) {
		super(in);
		irisInfos = new ArrayList<IrisInfo>();
	}

	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject;
		}
		return null;
	}
	
	public List<IrisInfo> getIrisInfos() {
		return irisInfos;
	}

	public int getTag() {
		return EF_DG4_TAG;
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DG4File";
	}

	/**
	 * Reads biometric data block.
	 * Based on ISO/IEC FCD 19794-4 aka Annex F.
	 * 
	 * TODO: work in progress... -- MO
	 */
	protected void readBiometricData(InputStream in, int valueLength) throws IOException {
		DataInputStream dataIn = (in instanceof DataInputStream) ? (DataInputStream)in : new DataInputStream(in);

		/* Iris Record Header (61) */
		int iir0 = dataIn.readInt(); /* header (e.g. "IIR", 0x00) (4) */
		if (iir0 != 0x49495200) { throw new IllegalArgumentException("'IIR0' marker expected! Found " + Integer.toHexString(iir0)); }
		/* int version = */ dataIn.readInt(); /* version in ASCII (e.g. "010" 0x00) (4) */
		/* long length = */ dataIn.readInt(); /* & 0x00000000FFFFFFFFL (4) */;
		/* int captureDeviceID = */ dataIn.readUnsignedShort(); /* (2) */
		int irisFeatureCount = dataIn.readUnsignedByte(); /* (1) */
		/* int recordHeaderLength = */ dataIn.readUnsignedShort(); /* Should be 61? (2) */
		/* int imagePropertiesBits = */ dataIn.readUnsignedShort(); /* (2) */
		/* int irisDiameter = */ dataIn.readUnsignedShort(); /* (2) */
		int imageFormat = dataIn.readUnsignedShort(); /* (2) */
		/* int rawImageWidth = */ dataIn.readUnsignedShort(); /* (2) */
		/* int rawImageHeight = */ dataIn.readUnsignedShort(); /* (2) */
		/* int intensityDepth = */ dataIn.readUnsignedByte(); /* (1) */
		/* int imageTransform = */ dataIn.readUnsignedByte(); /* (1) */

		/*
		 * A 16 character string uniquely identifying the
		 * device or source of the data. This data can be
		 * one of:
		 * Device Serial number, identified by the first character "D"
		 * Host PC Mac address, identified by the first character "M"
		 * Host PC processor ID, identified by the first character "P"
		 * No serial number, identified by all zero’s
		 */
		byte[] deviceUniqueID = new byte[16]; /* (16) */
		dataIn.readFully(deviceUniqueID);

		byte[] globallyUniqueID = new byte[16]; /* (16) */
		dataIn.readFully(globallyUniqueID);
		
		/* Features */
		for (int featureIndex = 0; featureIndex < irisFeatureCount; featureIndex++) {
			readIrisFeature(dataIn, featureIndex, imageFormat);
		}
	}
	
	private void readIrisFeature(DataInputStream dataIn, int featureIndex, int imageFormat) throws IOException {
		/* Iris feature header */
		/* int featureID = */ dataIn.readUnsignedByte();
		int imageCount = dataIn.readUnsignedShort();
		

		
		/* Images */
		for (int imageIndex = 0; imageIndex < imageCount; imageIndex++) {
			IrisInfo info = new IrisInfo(dataIn, imageFormat);
			irisInfos.add(info);
		}
	}	
}
