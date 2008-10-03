/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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
 * $Id: $
 */

package sos.mrtd;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import sos.tlv.BERTLVObject;

/**
 * File structure for the EF_DG2 file.
 * Datagroup 2 contains the facial features of
 * the document holder.
 * See A 13.3 in MRTD's LDS document.
 * 
 * NOTE: multiple FaceInfos may be embedded in two ways:
 * 1) as multiple images in the same record (see Fig. 3 in ISO/IEC
 * 19794-5)
 * 2) as multiple records (see A 13.3 in LDS technical report).
 * For writing we choose option 2, because otherwise we have to
 * precalc the total length of all FaceInfos, which sucks. -- CB
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class DG2File extends CBEFFDataGroup
{
	// Facial Record Header, Sect. 5.4, ISO SC37
	private static final byte[] FORMAT_IDENTIFIER = { 'F', 'A', 'C', 0x00 };
	private static final byte[] VERSION_NUMBER = { '0', '1', '0', 0x00 };

	private List<FaceInfo> faces;

	/**
	 * Constructs a new file.
	 */
	public DG2File() {
		isSourceConsistent = false;
	}

	public DG2File(InputStream in) {
		super(in);
	}

	protected void readBiometricData(InputStream in, int valueLength) throws IOException {
		DataInputStream dataIn = new DataInputStream(new BufferedInputStream(in, valueLength + 1));
		/* Facial Record Header (14) */
		int fac0 = dataIn.readInt(); // header (e.g. "FAC", 0x00)
		int version = dataIn.readInt(); // version in ASCII (e.g. "010" 0x00)
		long length = dataIn.readInt() & 0x000000FFFFFFFFL;
		int faceCount = dataIn.readUnsignedShort();
		for (int i = 0; i < faceCount; i++) {
			addFaceInfo(new FaceInfo(dataIn));
		}
	}

	public int getTag() {
		return EF_DG2_TAG;
	}

	public void addFaceInfo(FaceInfo fi) {
		if (faces == null) { faces = new ArrayList<FaceInfo>(); }
		faces.add(fi);
		isSourceConsistent = false;
	}
	
	public void removeFaceInfo(int index) {
		faces.remove(index);
		isSourceConsistent = false;
	}

	private byte[] formatOwner(Image i) {
		// FIXME
		byte[] ownr = { 0x01, 0x01 };
		return ownr;
	}

	private byte[] formatType(Image i) {
		// FIXME
		byte[] fmt = { 0x00, 0x08 };
		return fmt;
	}

	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject.getEncoded();
		}
		try {
			/* FIXME: some of this should be moved to CBEFFDataGroup! */
			/* FIXME: Consider using a BERTLVOutputStream instead of BERTLVObject here? */
			BERTLVObject group = new BERTLVObject(BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG /* 7F61 */,
					new BERTLVObject(BIOMETRIC_INFO_COUNT_TAG /* 02 */,
							(byte)faces.size()));
			
			group.reconstructLength();
			
			byte bioHeaderTag = BIOMETRIC_HEADER_TEMPLATE_BASE_TAG; /* A1 */
			for (FaceInfo info: faces) {
				BERTLVObject header = new BERTLVObject(bioHeaderTag++ & 0xFF,
						new BERTLVObject(FORMAT_OWNER_TAG, formatOwner(info.getImage())));
				header.addSubObject(new BERTLVObject(FORMAT_TYPE_TAG, formatType(info.getImage())));

				BERTLVObject faceObject = new BERTLVObject(BIOMETRIC_INFORMATION_TEMPLATE_TAG /* 7F60 */, header);

				int lengthOfRecord =
					FORMAT_IDENTIFIER.length + VERSION_NUMBER.length + 4 + 2;
				short nrOfImagesInRecord = 1;
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream dataOut = new DataOutputStream(out);
				dataOut.write(FORMAT_IDENTIFIER);
				dataOut.write(VERSION_NUMBER);
				dataOut.writeInt(lengthOfRecord);
				dataOut.writeShort(nrOfImagesInRecord);
				dataOut.write(info.getEncoded());
				dataOut.flush();
				byte[] facialRecord = out.toByteArray();

				faceObject.addSubObject(new BERTLVObject(BIOMETRIC_DATA_BLOCK_TAG /* 5F2E */, facialRecord));
				group.addSubObject(faceObject);
			}
			BERTLVObject dg2 = new BERTLVObject(EF_DG2_TAG, group);
			dg2.reconstructLength();
			sourceObject = dg2;
			isSourceConsistent = true;
			return dg2.getEncoded();
		} catch (Exception ioe) {
			return null;
		}
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("DG2File");
		result.append(" [");
		int faceCount = faces.size();
		int i = 0;
		for (FaceInfo faceInfo: faces) {
			result.append(faceInfo.getWidth() + "x" + faceInfo.getHeight());
			if (i < faceCount - 1) { result.append(", "); }
			i++;
		}
		result.append("]");
		return result.toString();
		
	}

	public List<FaceInfo> getFaces() {
		return faces;
	}
}
