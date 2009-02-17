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

import sos.tlv.BERTLVInputStream;

/**
 * File structure for Common Biometric Exchange File Format (CBEFF) formated files.
 * Abstract super class for DG2 - DG4.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
abstract class CBEFFDataGroup extends DataGroup
{
	static final int BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG = 0x7F61;
	static final int BIOMETRIC_INFORMATION_TEMPLATE_TAG = 0x7F60;

	static final int BIOMETRIC_INFO_COUNT_TAG = 0x02;
	static final int BIOMETRIC_HEADER_TEMPLATE_BASE_TAG = (byte) 0xA1;
	static final int BIOMETRIC_DATA_BLOCK_TAG = 0x5F2E;
	static final int BIOMETRIC_DATA_BLOCK_TAG_ALT = 0x7F2E;

	static final int FORMAT_OWNER_TAG = 0x87;
	static final int FORMAT_TYPE_TAG = 0x88;

	protected CBEFFDataGroup() {
	}

	/**
	 * Constructs a data group structure by parsing <code>in</code>.
	 * 
	 * @param in a TLV encoded input stream
	 */
	public CBEFFDataGroup(InputStream in) {
		super(in);
		try {
			BERTLVInputStream tlvIn = new BERTLVInputStream(in);	
			int bioInfoGroupTemplateTag = tlvIn.readTag();
			if (bioInfoGroupTemplateTag != BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG) { /* 7F61 */
				throw new IllegalArgumentException("Expected tag BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG (" + Integer.toHexString(BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG) + ") in CBEFF structure, found " + Integer.toHexString(bioInfoGroupTemplateTag));
			}
			tlvIn.readLength();
			int bioInfoCountTag = tlvIn.readTag();
			if (bioInfoCountTag != BIOMETRIC_INFO_COUNT_TAG) { /* 02 */
				throw new IllegalArgumentException("Expected tag BIOMETRIC_INFO_COUNT_TAG (" + Integer.toHexString(BIOMETRIC_INFO_COUNT_TAG) + ") in CBEFF structure, found " + Integer.toHexString(bioInfoCountTag));
			}
			int tlvBioInfoCountLength = tlvIn.readLength();
			if (tlvBioInfoCountLength != 1) {
				throw new IllegalArgumentException("BIOMETRIC_INFO_COUNT should have length 1");
			}
			int bioInfoCount = (tlvIn.readValue()[0] & 0xFF);
			for (int i = 0; i < bioInfoCount; i++) {
				readBiometricInformationTemplate(tlvIn, i);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Could not decode: " + e.toString());
		}
		isSourceConsistent = false;
	}

	private void readBiometricInformationTemplate(BERTLVInputStream tlvIn, int templateIndex) throws IOException {
		int bioInfoTemplateTag = tlvIn.readTag();
		if (bioInfoTemplateTag != BIOMETRIC_INFORMATION_TEMPLATE_TAG /* 7F60 */) { 
			throw new IllegalArgumentException("Expected tag BIOMETRIC_INFORMATION_TEMPLATE_TAG (" + Integer.toHexString(BIOMETRIC_INFORMATION_TEMPLATE_TAG) + "), found " + Integer.toHexString(bioInfoTemplateTag));
		}
		tlvIn.readLength();
		
		/* We'll just skip this header for now. */
		int bioHeaderTemplateTag = tlvIn.readTag(); /* A1, A2, ... */
		int expectedBioHeaderTemplateTag = (BIOMETRIC_HEADER_TEMPLATE_BASE_TAG + templateIndex) & 0xFF;
		if (bioHeaderTemplateTag != expectedBioHeaderTemplateTag) {
			throw new IllegalArgumentException("Expected tag BIOMETRIC_HEADER_TEMPLATE_TAG (" + Integer.toHexString(expectedBioHeaderTemplateTag) + "), found " + Integer.toHexString(bioHeaderTemplateTag));
		}
		int bioHeaderTemplateLength = tlvIn.readLength();
		tlvIn.skip(bioHeaderTemplateLength);
		
		/* And go straight to the data. */
		readBiometricDataBlock(tlvIn);
	}

	private void readBiometricDataBlock(BERTLVInputStream tlvIn) throws IOException {
		int bioDataBlockTag = tlvIn.readTag();
		if (bioDataBlockTag != BIOMETRIC_DATA_BLOCK_TAG /* 5F2E */ &&
				bioDataBlockTag != BIOMETRIC_DATA_BLOCK_TAG_ALT /* 7F2E */) {
			throw new IllegalArgumentException("Expected tag BIOMETRIC_DATA_BLOCK_TAG (" + Integer.toHexString(BIOMETRIC_DATA_BLOCK_TAG) + ") or BIOMETRIC_DATA_BLOCK_TAG_ALT (" + Integer.toHexString(BIOMETRIC_DATA_BLOCK_TAG_ALT) + "), found " + Integer.toHexString(bioDataBlockTag));
		}
		int length = tlvIn.readLength();
		readBiometricData(tlvIn, length);
	}

	/**
	 * Reads the biometric data block. This method should be implemented by concrete
	 * subclasses (DG2 - DG4 structures). It is assumed that the caller has already read
	 * the biometric data block tag (5F2E or 7F2E) and the length.
	 * 
	 * @param in the input stream positioned so that biometric data block tag and length are already read
	 * @param length the length
	 * @throws IOException if reading fails
	 */
	protected abstract void readBiometricData(InputStream in, int length) throws IOException;

	public abstract byte[] getEncoded();

	public abstract String toString();
}
