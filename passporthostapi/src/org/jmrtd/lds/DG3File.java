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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.jmrtd.cbeff.BiometricDataBlock;
import org.jmrtd.cbeff.BiometricDataBlockDecoder;
import org.jmrtd.cbeff.BiometricDataBlockEncoder;
import org.jmrtd.cbeff.CBEFFInfo;
import org.jmrtd.cbeff.ComplexCBEFFInfo;
import org.jmrtd.cbeff.ISO781611Decoder;
import org.jmrtd.cbeff.ISO781611Encoder;
import org.jmrtd.cbeff.SimpleCBEFFInfo;
import org.jmrtd.cbeff.StandardBiometricHeader;

/**
 * File structure for the EF_DG3 file.
 * Partially specified in ISO/IEC FCD 19794-4 aka Annex F.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG3File extends CBEFFDataGroup<FingerInfo> {

	private static final ISO781611Decoder DECODER = new ISO781611Decoder(new BiometricDataBlockDecoder<FingerInfo>() {
		public FingerInfo decode(InputStream in, StandardBiometricHeader sbh, int index, int length) throws IOException {
			return new FingerInfo(sbh, in);
		}
	});

	private static final ISO781611Encoder<FingerInfo> ENCODER = new ISO781611Encoder<FingerInfo>(new BiometricDataBlockEncoder<FingerInfo>() {
		public void encode(FingerInfo info, OutputStream out) throws IOException {
			info.writeObject(out);
		}
	});


	/**
	 * Creates a new file with the specified records.
	 * 
	 * @param fingerInfos records
	 */
	public DG3File(List<FingerInfo> fingerInfos) {
		super(EF_DG3_TAG, fingerInfos);
	}

	/**
	 * Creates a new file based on an input stream.
	 *
	 * @param in an input stream
	 */
	public DG3File(InputStream in) {
		super(EF_DG3_TAG, in);
	}

	protected void readContent(InputStream in) throws IOException {
		ComplexCBEFFInfo cbeffInfo = DECODER.decode(in);

		List<CBEFFInfo> records = cbeffInfo.getSubRecords();
		for (CBEFFInfo record: records) {
			if (!(record instanceof SimpleCBEFFInfo<?>)) {
				throw new IOException("Was expecting a SimpleCBEFFInfo, found " + record.getClass().getSimpleName());
			}
			BiometricDataBlock bdb = ((SimpleCBEFFInfo<?>)record).getBiometricDataBlock();
			if (!(bdb instanceof FingerInfo)) {
				throw new IOException("Was expecting a FingerInfo, found " + bdb.getClass().getSimpleName());
			}
			FingerInfo fingerInfo = (FingerInfo)bdb;
			add(fingerInfo);
		}
		
		/* FIXME: by symmetry, shouldn't there be a readOptionalRandomData here? */
	}

	protected void writeContent(OutputStream out) throws IOException {
		ComplexCBEFFInfo cbeffInfo = new ComplexCBEFFInfo();
		List<FingerInfo> fingerInfos = getSubRecords();
		for (FingerInfo fingerInfo: fingerInfos) {
			SimpleCBEFFInfo<FingerInfo> simpleCBEFFInfo = new SimpleCBEFFInfo<FingerInfo>(fingerInfo);
			cbeffInfo.add(simpleCBEFFInfo);
		}
		ENCODER.encode(cbeffInfo, out);

		/* NOTE: Supplement to ICAO Doc 9303 R7-p1_v2_sIII_0057. */
		writeOptionalRandomData(out);
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DG3File [" + super.toString() + "]";
	}

	public List<FingerInfo> getFingerInfos() { return getSubRecords(); }
	public void addFingerInfo(FingerInfo fingerInfo) { add(fingerInfo); }
	public void removeFingerInfo(int index) { remove(index); }
}
