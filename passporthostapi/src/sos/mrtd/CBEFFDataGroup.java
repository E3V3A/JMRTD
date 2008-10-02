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

import java.io.IOException;
import java.io.InputStream;

import sos.tlv.BERTLVInputStream;

/**
 * File structure for CBEFF formated files.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public abstract class CBEFFDataGroup extends DataGroup
{
	static final short BIOMETRIC_INFO_GROUP_TAG = 0x7F61;
	static final short BIOMETRIC_INFO_TAG = 0x7F60;

	static final byte BIOMETRIC_INFO_COUNT_TAG = 0x02;
	static final byte BIOMETRIC_HEADER_BASE_TAG = (byte) 0xA1;
	static final short BIOMETRIC_DATA_TAG = 0x5F2E;

	static final int FORMAT_OWNER_TAG = 0x87;
	static final int FORMAT_TYPE_TAG = 0x88;

	CBEFFDataGroup() {
	}
	
	public CBEFFDataGroup(InputStream in) {
		try {
			BERTLVInputStream tlvIn = new BERTLVInputStream(in);			
			tlvIn.skipToTag(BIOMETRIC_INFO_GROUP_TAG); /* 7F61 */
			tlvIn.readLength();
			tlvIn.skipToTag(BIOMETRIC_INFO_COUNT_TAG); /* 02 */
			int tlvBioInfoCountLength = tlvIn.readLength();
			if (tlvBioInfoCountLength != 1) { throw new IllegalArgumentException("BIOMETRIC_INFO_COUNT should have length 1"); }
			int bioInfoCount = (tlvIn.readValue()[0] & 0xFF);
			for (int i = 0; i < bioInfoCount; i++) {
				readBioInfoTemplate(tlvIn);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Could not decode: " + e.toString());
		}
		isSourceConsistent = false;
	}

	protected void readBioInfoTemplate(BERTLVInputStream tlvIn) throws IOException {
		tlvIn.skipToTag(BIOMETRIC_DATA_TAG); /* 5F2E */
		int length = tlvIn.readLength();
		readBioData(tlvIn, length);
	}

	protected abstract void readBioData(BERTLVInputStream tlvIn, int valueLength) throws IOException;
	

	public abstract int getTag();

	public abstract byte[] getEncoded();
	
	public abstract String toString();
}
