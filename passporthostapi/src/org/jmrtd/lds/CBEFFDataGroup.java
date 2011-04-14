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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sourceforge.scuba.tlv.TLVInputStream;

/**
 * File structure for Common Biometric Exchange File Format (CBEFF) formated files.
 * Abstract super class for DG2 - DG4.
 * 
 * Some information based on ISO/IEC 7816-11:2004(E) and/or
 * NISTIR 6529-A.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
abstract class CBEFFDataGroup extends DataGroup
{
	private Logger logger = Logger.getLogger("org.jmrtd");

	static final int BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG = 0x7F61;
	static final int BIOMETRIC_INFORMATION_TEMPLATE_TAG = 0x7F60;

	static final int BIOMETRIC_INFO_COUNT_TAG = 0x02;
	static final int BIOMETRIC_HEADER_TEMPLATE_BASE_TAG = (byte) 0xA1;
	static final int BIOMETRIC_DATA_BLOCK_TAG = 0x5F2E;
	static final int BIOMETRIC_DATA_BLOCK_TAG_ALT = 0x7F2E;

	static final int FORMAT_OWNER_TAG = 0x87;
	static final int FORMAT_TYPE_TAG = 0x88;

	/** From ISO7816-11: Secure Messaging Template tags. */
	static final int
	SMT_TAG = 0x7D,
	SMT_DO_PV = 0x81,
	SMT_DO_CG = 0x85,
	SMT_DO_CC = 0x8E,
	SMT_DO_DS = 0x9E;

	protected List<byte[]> templates;

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
			TLVInputStream tlvIn = new TLVInputStream(in);	
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
				throw new IllegalArgumentException("BIOMETRIC_INFO_COUNT should have length 1, found length " + tlvBioInfoCountLength);
			}
			int bioInfoCount = (tlvIn.readValue()[0] & 0xFF);
			for (int i = 0; i < bioInfoCount; i++) {
				readBIT(tlvIn, i);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Could not decode: " + e.toString());
		}
		isSourceConsistent = false;
	}

	private void readBIT(TLVInputStream tlvIn, int templateIndex) throws IOException {
		int bioInfoTemplateTag = tlvIn.readTag();
		if (bioInfoTemplateTag != BIOMETRIC_INFORMATION_TEMPLATE_TAG /* 7F60 */) { 
			throw new IllegalArgumentException("Expected tag BIOMETRIC_INFORMATION_TEMPLATE_TAG (" + Integer.toHexString(BIOMETRIC_INFORMATION_TEMPLATE_TAG) + "), found " + Integer.toHexString(bioInfoTemplateTag));
		}
		tlvIn.readLength();

		int headerTemplateTag = tlvIn.readTag();
		int headerTemplateLength = tlvIn.readLength();
		
		if ((headerTemplateTag == SMT_TAG)) {
			/* The BIT is protected... */
			readStaticallyProtectedBIT(headerTemplateTag, headerTemplateLength, templateIndex, tlvIn);
		} else if ((headerTemplateTag & 0xA0) == 0xA0) {
			readBHT(headerTemplateTag, headerTemplateLength, templateIndex, tlvIn);
			readBiometricDataBlock(tlvIn);
		} else {
			throw new IllegalArgumentException("Unsupported template tag: " + Integer.toHexString(headerTemplateTag));
		}
	}
	
	/**
	 *  A1, A2, ...
	 *  Will contain DOs as described in ISO 7816-11 Annex C.
	 */
	private void readBHT(int headerTemplateTag, int headerTemplateLength, int templateIndex, TLVInputStream tlvIn) throws IOException {
		int expectedBioHeaderTemplateTag = (BIOMETRIC_HEADER_TEMPLATE_BASE_TAG + templateIndex) & 0xFF;
		if (headerTemplateTag != expectedBioHeaderTemplateTag) {
			String warning = "Expected tag BIOMETRIC_HEADER_TEMPLATE_TAG (" + Integer.toHexString(expectedBioHeaderTemplateTag) + "), found " + Integer.toHexString(headerTemplateTag);
			logger.warning(warning);
			// throw new IllegalArgumentException(warning);
		}
		/* We'll just skip this header for now. */
		long skippedBytes = 0;
		while (skippedBytes < headerTemplateLength) { skippedBytes += tlvIn.skip(headerTemplateLength); }
	}

	/**
	 * Reads a biometric information template protected with secure messaging.
	 * Described in ISO7816-11 Annex D.
	 *
	 * @param tag should be <code>0x7D</code>
	 * @param length the length of the BIT
	 * @param templateIndex index of the template
	 * @param tlvIn source to read from
	 *
	 * @throws IOException on failure
	 */
	private void readStaticallyProtectedBIT(int tag, int length, int templateIndex, TLVInputStream tlvIn) throws IOException {
		TLVInputStream tlvBHTIn = new TLVInputStream(new ByteArrayInputStream(decodeSMTValue(tlvIn)));
		int headerTemplateTag = tlvBHTIn.readTag();
		int headerTemplateLength = tlvBHTIn.readLength();
		readBHT(headerTemplateTag, headerTemplateLength, templateIndex, tlvBHTIn);
		TLVInputStream tlvBiometricDataBlockIn = new TLVInputStream(new ByteArrayInputStream(decodeSMTValue(tlvIn)));
		readBiometricDataBlock(tlvBiometricDataBlockIn);
	}

	private byte[] decodeSMTValue(TLVInputStream tlvIn) throws IOException {
		int doTag = tlvIn.readTag();
		int doLength = tlvIn.readLength();
		switch (doTag) {
		case SMT_DO_PV /* 0x81 */:
			/* NOTE: Plain value, just return whatever is in the payload */
			return tlvIn.readValue();
		case SMT_DO_CG /* 0x85 */:
			/* NOTE: content of payload is encrypted */
			return tlvIn.readValue();
		case SMT_DO_CC /* 0x8E */:
			/* NOTE: payload contains a MAC */
			long skippedBytes = 0;
			while (skippedBytes < doLength) { skippedBytes += tlvIn.skip(doLength); }
			break;
		case SMT_DO_DS /* 0x9E */:
			/* NOTE: payload contains a signature */
			skippedBytes = 0;
			while (skippedBytes < doLength) { skippedBytes += tlvIn.skip(doLength); }
			break;
		}
		return null;
	}

	private void readBiometricDataBlock(TLVInputStream tlvIn) throws IOException {
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
	protected void readBiometricData(InputStream in, int length) throws IOException {
		DataInputStream dataIn = new DataInputStream(in);
		byte[] data = new byte[length];
		dataIn.readFully(data);
		if (templates == null) { templates = new ArrayList<byte[]>(); }
		templates.add(data);
	}
	
	/* EXPERMINTAL */

	private void writeBIT(DataOutputStream out, int templateIndex) throws IOException {
		out.write(BIOMETRIC_INFORMATION_TEMPLATE_TAG);
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		writeBHT(new DataOutputStream(bOut), templateIndex);
		byte[] bOutBytes = bOut.toByteArray();
		out.write(bOutBytes.length);
		out.write(bOutBytes);
	}
	
	private void writeBHT(DataOutputStream out, int templateIndex) throws IOException {
		
	}

	public abstract byte[] getEncoded();

	public abstract String toString();
}
