/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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

package org.jmrtd.cbeff;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.scuba.tlv.TLVInputStream;
import net.sf.scuba.tlv.TLVUtil;

/**
 * ISO 7816-11 decoder for BIR.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 * 
 * @since 0.4.7
 */
public class ISO781611Decoder implements ISO781611 {

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	
	private BiometricDataBlockDecoder<?> bdbDecoder;
	
	/**
	 * Constructs an ISO7816-11 decoder that uses the given BDB decoder.
	 * 
	 * @param bdbDecoder the BDB decoder to use
	 */
	public ISO781611Decoder(BiometricDataBlockDecoder<?> bdbDecoder) {
		this.bdbDecoder = bdbDecoder;
	}

	/**
	 * Reads a BIT group from an input stream.
	 * 
	 * @param inputStream the input stream to read from
	 * 
	 * @return a complex CBEFF info representing the BIT group
	 * 
	 * @throws IOException if reading fails
	 */
	public ComplexCBEFFInfo decode(InputStream inputStream) throws IOException {
		return readBITGroup(inputStream);
	}

	/**
	 * Reads a BIT group from an input stream.
	 * 
	 * @param inputStream the input stream to read from
	 * 
	 * @return a complex CBEFF info representing the BIT group
	 * 
	 * @throws IOException if reading fails
	 */
	private ComplexCBEFFInfo readBITGroup(InputStream inputStream) throws IOException {
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		int tag = tlvIn.readTag();
		switch (tag) {
		case BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG:
			int length = tlvIn.readLength();
			return readBITGroup(tag, length, inputStream);
		default:
			throw new IllegalArgumentException("Expected tag "
				+ Integer.toHexString(BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG)
				+ ", found " + Integer.toHexString(tag));
		}
	}

	private ComplexCBEFFInfo readBITGroup(int tag, int length, InputStream inputStream) throws IOException {
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		ComplexCBEFFInfo result = new ComplexCBEFFInfo();
		if (tag != BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG) { /* 7F61 */
			throw new IllegalArgumentException("Expected tag " + Integer.toHexString(BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG) + ", found " + Integer.toHexString(tag));
		}
		int bitCountTag = tlvIn.readTag();
		if (bitCountTag != BIOMETRIC_INFO_COUNT_TAG) { /* 02 */
			throw new IllegalArgumentException("Expected tag BIOMETRIC_INFO_COUNT_TAG (" + Integer.toHexString(BIOMETRIC_INFO_COUNT_TAG) + ") in CBEFF structure, found " + Integer.toHexString(bitCountTag));
		}
		int bitCountLength = tlvIn.readLength();
		if (bitCountLength != 1) {
			throw new IllegalArgumentException("BIOMETRIC_INFO_COUNT should have length 1, found length " + bitCountLength);
		}
		int bitCount = (tlvIn.readValue()[0] & 0xFF);
		for (int i = 0; i < bitCount; i++) {
			result.add(readBIT(inputStream, i));
		}

		/* TODO: possibly more content, e.g. 0x53 tag with random as per ICAO 9303 Supplement R7-p1_v2_sIII_0057 */
		
		return result;
	}

	/**
	 * Reads a BIT from the input stream.
	 * 
	 * @param inputStream the input stream to read from
	 * @param index index of this BIT within the BIT group
	 * 
	 * @return a CBEFF info representing the BIT
	 * 
	 * @throws IOException if reading fails
	 */
	private CBEFFInfo readBIT(InputStream inputStream, int index) throws IOException {
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		int tag = tlvIn.readTag();
		int length = tlvIn.readLength();
		return readBIT(tag, length, inputStream, index);
	}
	
	private CBEFFInfo readBIT(int tag, int length, InputStream inputStream, int index) throws IOException {
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		if (tag != BIOMETRIC_INFORMATION_TEMPLATE_TAG /* 7F60 */) { 
			throw new IllegalArgumentException("Expected tag BIOMETRIC_INFORMATION_TEMPLATE_TAG (" + Integer.toHexString(BIOMETRIC_INFORMATION_TEMPLATE_TAG) + "), found " + Integer.toHexString(tag) + ", index is " + index);
		}

		int bhtTag = tlvIn.readTag();
		int bhtLength = tlvIn.readLength();

		if ((bhtTag == SMT_TAG)) {
			/* The BIT is protected... */
			readStaticallyProtectedBIT(inputStream, bhtTag, bhtLength, index);
		} else if ((bhtTag & 0xA0) == 0xA0) {
			StandardBiometricHeader sbh = readBHT(inputStream, bhtTag, bhtLength, index);
			BiometricDataBlock bdb = readBiometricDataBlock(inputStream, sbh, index);
			return new SimpleCBEFFInfo(bdb);
		} else {
			throw new IllegalArgumentException("Unsupported template tag: " + Integer.toHexString(bhtTag));
		}
		
		return null; // FIXME
	}

	/**
	 *  A1, A2, ...
	 *  Will contain DOs as described in ISO 7816-11 Annex C.
	 */
	private StandardBiometricHeader readBHT(InputStream inputStream, int bhtTag, int bhtLength, int index) throws IOException {
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		int expectedBHTTag = (BIOMETRIC_HEADER_TEMPLATE_BASE_TAG /* + index */) & 0xFF;
		if (bhtTag != expectedBHTTag) {
			String warning = "Expected tag " + Integer.toHexString(expectedBHTTag) + ", found " + Integer.toHexString(bhtTag);
			if (LOGGER != null) { LOGGER.warning(warning); }
		}
		Map<Integer, byte[]> elements = new HashMap<Integer, byte[]>();
		int bytesRead = 0;
		while (bytesRead < bhtLength) {
			int tag = tlvIn.readTag(); bytesRead += TLVUtil.getTagLength(tag);
			int length = tlvIn.readLength(); bytesRead += TLVUtil.getLengthLength(length);
			byte[] value = tlvIn.readValue(); bytesRead += value.length;
			elements.put(tag, value);
		}
		return new StandardBiometricHeader(elements);
	}

	/**
	 * Reads a biometric information template protected with secure messaging.
	 * Described in ISO7816-11 Annex D.
	 *
	 * @param inputStream source to read from
	 * @param tag should be <code>0x7D</code>
	 * @param length the length of the BIT
	 * @param index index of the template
	 *
	 * @throws IOException on failure
	 */
	private void readStaticallyProtectedBIT(InputStream inputStream, int tag, int length, int index) throws IOException {
		TLVInputStream tlvBHTIn = new TLVInputStream(new ByteArrayInputStream(decodeSMTValue(inputStream)));
		int headerTemplateTag = tlvBHTIn.readTag();
		int headerTemplateLength = tlvBHTIn.readLength();
		StandardBiometricHeader sbh = readBHT(tlvBHTIn, headerTemplateTag, headerTemplateLength, index);
		InputStream biometricDataBlockIn = new ByteArrayInputStream(decodeSMTValue(inputStream));
		readBiometricDataBlock(biometricDataBlockIn, sbh, index);
	} /* FIXME: return type??? */

	private byte[] decodeSMTValue(InputStream inputStream) throws IOException {
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		int doTag = tlvIn.readTag();
		int doLength = tlvIn.readLength();
		switch (doTag) {
		case SMT_DO_PV /* 0x81 */:
			/* NOTE: Plain value, just return whatever is in the payload */
			return tlvIn.readValue();
		case SMT_DO_CG /* 0x85 */:
			/* NOTE: content of payload is encrypted */
			throw new AccessControlException("Access denied. Biometric Information Template is statically protected.");
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

	private BiometricDataBlock readBiometricDataBlock(InputStream inputStream, StandardBiometricHeader sbh, int index) throws IOException {
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		int bioDataBlockTag = tlvIn.readTag();
		if (bioDataBlockTag != BIOMETRIC_DATA_BLOCK_TAG /* 5F2E */ &&
				bioDataBlockTag != BIOMETRIC_DATA_BLOCK_CONSTRUCTED_TAG /* 7F2E */) {
			throw new IllegalArgumentException("Expected tag BIOMETRIC_DATA_BLOCK_TAG (" + Integer.toHexString(BIOMETRIC_DATA_BLOCK_TAG) + ") or BIOMETRIC_DATA_BLOCK_TAG_ALT (" + Integer.toHexString(BIOMETRIC_DATA_BLOCK_CONSTRUCTED_TAG) + "), found " + Integer.toHexString(bioDataBlockTag));
		}
		// this.biometricDataBlockTag = bioDataBlockTag;
		int length = tlvIn.readLength();		
		BiometricDataBlock bdb = bdbDecoder.decode(inputStream, sbh, index, length);
		return bdb;
	}
}
