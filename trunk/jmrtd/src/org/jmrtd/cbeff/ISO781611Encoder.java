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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

import net.sf.scuba.tlv.TLVOutputStream;

/**
 * ISO 7816-11 encoder for BIR.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
public class ISO781611Encoder<B extends BiometricDataBlock> implements ISO781611 {

	private BiometricDataBlockEncoder<B> bdbEncoder;
	
	/**
	 * Constructs an ISO7816-11 encoder that uses the given BDB encoder.
	 * 
	 * @param bdbEncoder the BDB encoder to use
	 */
	public ISO781611Encoder(BiometricDataBlockEncoder<B> bdbEncoder) {
		this.bdbEncoder = bdbEncoder;
	}
	
	/**
	 * Writes a BIT group to an output stream.
	 * 
	 * @param cbeffInfo a CBEFF info containing the BIT group
	 * @param outputStream the output stream to write to
	 * 
	 * @throws IOException if something goes wrong
	 */
	public void encode(CBEFFInfo cbeffInfo, OutputStream outputStream) throws IOException {
		if (cbeffInfo instanceof SimpleCBEFFInfo) {
			writeBITGroup(Arrays.asList(new CBEFFInfo[] { cbeffInfo }), outputStream);
		} else if (cbeffInfo instanceof ComplexCBEFFInfo) {
			ComplexCBEFFInfo complexCBEFFInfo = (ComplexCBEFFInfo)cbeffInfo;
			writeBITGroup(complexCBEFFInfo.getSubRecords(), outputStream);
		}
	}
	
	private void writeBITGroup(List<CBEFFInfo> records, OutputStream outputStream) throws IOException {
		TLVOutputStream tlvOut = outputStream instanceof TLVOutputStream ? (TLVOutputStream)outputStream : new TLVOutputStream(outputStream);
		tlvOut.writeTag(BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG); /* 7F61 */
		tlvOut.writeTag(BIOMETRIC_INFO_COUNT_TAG); /* 0x02 */
		int count = records.size();
		tlvOut.writeValue(new byte[] { (byte)count });

		for (int index = 0; index < count; index++) {
			@SuppressWarnings("unchecked")
			SimpleCBEFFInfo<B> simpleCBEFFInfo = (SimpleCBEFFInfo<B>)records.get(index);
			writeBIT(tlvOut, index, simpleCBEFFInfo);
		}
		tlvOut.writeValueEnd(); /* BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG, i.e. 7F61 */
	}

	private void writeBIT(TLVOutputStream tlvOut, int index, SimpleCBEFFInfo<B> cbeffInfo) throws IOException {
		if (!(cbeffInfo instanceof SimpleCBEFFInfo)) {
			throw new IllegalArgumentException("Encoder does not support level > 2 nesting");
		}
		tlvOut.writeTag(BIOMETRIC_INFORMATION_TEMPLATE_TAG); /* 7F60 */
		writeBHT(tlvOut, index, cbeffInfo);
		writeBiometricDataBlock(tlvOut, cbeffInfo.getBiometricDataBlock());
		tlvOut.writeValueEnd(); /* BIOMETRIC_INFORMATION_TEMPLATE_TAG, i.e. 7F60 */
	}
	
	private void writeBHT(TLVOutputStream tlvOut, int index, SimpleCBEFFInfo<B> cbeffInfo) throws IOException {
		tlvOut.writeTag((BIOMETRIC_HEADER_TEMPLATE_BASE_TAG /* + index */) & 0xFF); /* A1 */
				
		B bdb = cbeffInfo.getBiometricDataBlock();

		/* SBH */
		StandardBiometricHeader sbh = bdb.getStandardBiometricHeader();
		SortedMap<Integer, byte[]> elements = sbh.getElements();
		for (SortedMap.Entry<Integer, byte[]> entry: elements.entrySet()) {
			tlvOut.writeTag(entry.getKey());
			tlvOut.writeValue(entry.getValue());
		}		
		tlvOut.writeValueEnd(); /* BIOMETRIC_HEADER_TEMPLATE_BASE_TAG, i.e. A1 */
	}

	private void writeBiometricDataBlock(TLVOutputStream tlvOut, B bdb) throws IOException {
		tlvOut.writeTag(BIOMETRIC_DATA_BLOCK_TAG); /* 5F2E or 7F2E */
		
		bdbEncoder.encode(bdb, tlvOut);
		tlvOut.writeValueEnd(); /* BIOMETRIC_DATA_BLOCK_TAG, i.e. 5F2E or 7F2E */
	}
}
