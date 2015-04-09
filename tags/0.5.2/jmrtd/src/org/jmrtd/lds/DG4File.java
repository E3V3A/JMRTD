/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
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
 * File structure for the EF_DG4 file.
 * Based on ISO/IEC 19794-6.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG4File extends CBEFFDataGroup<IrisInfo> {

	private static final long serialVersionUID = -1290365855823447586L;

	private static final ISO781611Decoder DECODER = new ISO781611Decoder(new BiometricDataBlockDecoder<IrisInfo>() {
		public IrisInfo decode(InputStream inputStream, StandardBiometricHeader sbh, int index, int length) throws IOException {
			return new IrisInfo(sbh, inputStream);
		}
	});

	private static final ISO781611Encoder<IrisInfo> ENCODER = new ISO781611Encoder<IrisInfo>(new BiometricDataBlockEncoder<IrisInfo>() {
		public void encode(IrisInfo info, OutputStream outputStream) throws IOException {
			info.writeObject(outputStream);
		}
	});

	private boolean shouldAddRandomDataIfEmpty;

	/**
	 * Creates a new file with the specified records.
	 * 
	 * @param irisInfos records
	 */
	public DG4File(List<IrisInfo> irisInfos) {
		this(irisInfos, true);
	}

	/**
	 * Creates a new file with the specified records.
	 * 
	 * @param irisInfos records
	 * @param shouldAddRandomDataIfEmpty indicates whether the encoder should add random data when no templates are present
	 */
	public DG4File(List<IrisInfo> irisInfos, boolean shouldAddRandomDataIfEmpty) {
		super(EF_DG4_TAG, irisInfos);
		this.shouldAddRandomDataIfEmpty = shouldAddRandomDataIfEmpty;
	}

	/**
	 * Constructs a new file based on an input stream.
	 * 
	 * @param inputStream an input stream
	 * 
	 * @throws IOException on error reading from input stream
	 */
	public DG4File(InputStream inputStream) throws IOException {
		super(EF_DG4_TAG, inputStream);
	}

	protected void readContent(InputStream inputStream) throws IOException {
		ComplexCBEFFInfo cbeffInfo = DECODER.decode(inputStream);
		List<CBEFFInfo> records = cbeffInfo.getSubRecords();
		for (CBEFFInfo record: records) {
			if (!(record instanceof SimpleCBEFFInfo<?>)) {
				throw new IOException("Was expecting a SimpleCBEFFInfo, found " + record.getClass().getSimpleName());
			}
			BiometricDataBlock bdb = ((SimpleCBEFFInfo<?>)record).getBiometricDataBlock();
			if (!(bdb instanceof IrisInfo)) {
				throw new IOException("Was expecting an IrisInfo, found " + bdb.getClass().getSimpleName());
			}
			IrisInfo irisInfo = (IrisInfo)bdb;
			add(irisInfo);
		}

		/* FIXME: by symmetry, shouldn't there be a readOptionalRandomData here? */
	}

	protected void writeContent(OutputStream outputStream) throws IOException {
		ComplexCBEFFInfo cbeffInfo = new ComplexCBEFFInfo();
		List<IrisInfo> irisInfos = getSubRecords();
		for (IrisInfo irisInfo: irisInfos) {
			SimpleCBEFFInfo<IrisInfo> simpleCBEFFInfo = new SimpleCBEFFInfo<IrisInfo>(irisInfo);
			cbeffInfo.add(simpleCBEFFInfo);
		}
		ENCODER.encode(cbeffInfo, outputStream);

		/* NOTE: Supplement to ICAO Doc 9303 R7-p1_v2_sIII_0057. */
		if (shouldAddRandomDataIfEmpty) {
			writeOptionalRandomData(outputStream);
		}
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DG4File [" + super.toString() + "]";
	}

	/**
	 * Gets the embedded iris infos in this file.
	 * 
	 * @return iris infos
	 */
	public List<IrisInfo> getIrisInfos() { return getSubRecords(); }

	/**
	 * Adds an iris info to this file.
	 * 
	 * @param irisInfo an iris info
	 */
	public void addIrisInfo(IrisInfo irisInfo) { add(irisInfo); }

	/**
	 * Removes an iris info from this file.
	 * 
	 * @param index the index of the iris info to remove
	 */
	public void removeIrisInfo(int index) { remove(index); }
}
