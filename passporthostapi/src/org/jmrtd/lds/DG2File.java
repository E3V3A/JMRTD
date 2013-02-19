/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
import org.jmrtd.io.SplittableInputStream;

/**
 * File structure for the EF_DG2 file.
 * Datagroup 2 contains the facial features of the document holder.
 * See A 13.3 in MRTD's LDS document (or equivalent in Doc 9303).
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG2File extends CBEFFDataGroup<FaceInfo> {

	private static final long serialVersionUID = 414300652684010416L;

	private static final ISO781611Decoder DECODER = new ISO781611Decoder(new BiometricDataBlockDecoder<FaceInfo>() {
		public FaceInfo decode(InputStream inputStream, StandardBiometricHeader sbh, int index, int length) throws IOException {
			return new FaceInfo(sbh, inputStream);
		}
	});

	private static final ISO781611Encoder<FaceInfo> ENCODER = new ISO781611Encoder<FaceInfo>(new BiometricDataBlockEncoder<FaceInfo>() {
		public void encode(FaceInfo info, OutputStream out) throws IOException {
			info.writeObject(out);
		}
	});
	
	/**
	 * Creates a new file with the specified records.
	 * 
	 * @param faceInfos records
	 */
	public DG2File(List<FaceInfo> faceInfos) {
		super(EF_DG2_TAG, faceInfos);
	}

	/**
	 * Creates a new file based on an input stream.
	 *
	 * @param inputStream an input stream
	 */
	public DG2File(InputStream inputStream) throws IOException {
		super(EF_DG2_TAG, inputStream);
	}

	protected void readContent(InputStream inputStream) throws IOException {
		ComplexCBEFFInfo complexCBEFFInfo = DECODER.decode(inputStream);
		List<CBEFFInfo> records = complexCBEFFInfo.getSubRecords();
		for (CBEFFInfo cbeffInfo: records) {
			if (!(cbeffInfo instanceof SimpleCBEFFInfo<?>)) {
				throw new IOException("Was expecting a SimpleCBEFFInfo, found " + cbeffInfo.getClass().getSimpleName());
			}
			SimpleCBEFFInfo<?> simpleCBEFFInfo = (SimpleCBEFFInfo<?>)cbeffInfo;
			BiometricDataBlock bdb = simpleCBEFFInfo.getBiometricDataBlock();
			if (!(bdb instanceof FaceInfo)) {
				throw new IOException("Was expecting a FaceInfo, found " + bdb.getClass().getSimpleName());
			}
			FaceInfo faceInfo = (FaceInfo)bdb;
			add(faceInfo);
		}
		
		/* FIXME: by symmetry, shouldn't there be a readOptionalRandomData here? */
	}

	protected void writeContent(OutputStream outputStream) throws IOException {
		ComplexCBEFFInfo cbeffInfo = new ComplexCBEFFInfo();
		List<FaceInfo> faceInfos = getSubRecords();
		for (FaceInfo faceInfo: faceInfos) {
			SimpleCBEFFInfo<FaceInfo> simpleCBEFFInfo = new SimpleCBEFFInfo<FaceInfo>(faceInfo);
			cbeffInfo.add(simpleCBEFFInfo);
		}
		ENCODER.encode(cbeffInfo, outputStream);
	}


	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DG2File [" + super.toString() + "]";
	}

	/**
	 * Gets the face infos embedded in this file.
	 * 
	 * @return face infos
	 */
	public List<FaceInfo> getFaceInfos() { return getSubRecords(); }

	/**
	 * Adds a face info to this file.
	 * 
	 * @param faceInfo the face info to add
	 */
	public void addFaceInfo(FaceInfo faceInfo) { add(faceInfo); }
	
	/**
	 * Removes a face info from this file.
	 * 
	 * @param index the index of the face info to remove
	 */
	public void removeFaceInfo(int index) { remove(index); }
}
