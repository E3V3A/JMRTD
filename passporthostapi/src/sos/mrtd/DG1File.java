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
import sos.tlv.BERTLVObject;

/**
 * File structure for the EF_DG1 file.
 * Datagroup 1 contains the Machine
 * Readable Zone information.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG1File extends DataGroup
{
	private static final short MRZ_INFO_TAG = 0x5F1F;
	private MRZInfo mrz;

	/**
	 * Creates a new file based on MRZ information.
	 * 
	 * @param mrz the MRZ information to store in this file
	 */
	public DG1File(MRZInfo mrz) {
		this.mrz = mrz;
	}

	/**
	 * Creates a new file based on an input stream.
	 *
	 * @param in an input stream
	 *
	 * @throws IOException if something goes wrong
	 */
	public DG1File(InputStream in) throws IOException {
		BERTLVInputStream tlvIn = new BERTLVInputStream(in);
		int tag = tlvIn.readTag();
		if (tag != PassportFile.EF_DG1_TAG) { throw new IllegalArgumentException("Expected EF_DG1_TAG"); }
		tlvIn.skipToTag(MRZ_INFO_TAG);
		tlvIn.readLength();
		isSourceConsistent = false;
		this.mrz = new MRZInfo(tlvIn);
	}

	public int getTag() {
		return EF_DG1_TAG;
	}

	/**
	 * Gets the MRZ information stored in this file.
	 * 
	 * @return the MRZ information
	 */
	public MRZInfo getMRZInfo() {
		return mrz;
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DG1File " + mrz.toString().replaceAll("\n", "").trim();
	}

	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject.getEncoded();
		}
		try {
			BERTLVObject ef0101 =
				new BERTLVObject(EF_DG1_TAG,
						new BERTLVObject(0x5F1F, mrz.getEncoded()));
			sourceObject = ef0101;
			ef0101.reconstructLength();
			isSourceConsistent = true;
			return ef0101.getEncoded();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
