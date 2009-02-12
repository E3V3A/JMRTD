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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

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
public class DG1File extends DataGroup implements Serializable
{
	private static final long serialVersionUID = 5091606125728809058L;

	private static final short MRZ_INFO_TAG = 0x5F1F;
	
	private MRZInfo mrzInfo;

	/**
	 * Creates a new file based on MRZ information.
	 * 
	 * @param mrzInfo the MRZ information to store in this file
	 */
	public DG1File(MRZInfo mrzInfo) {
		this.mrzInfo = mrzInfo;
	}

	/**
	 * Creates a new file based on an input stream.
	 *
	 * @param in an input stream
	 *
	 * @throws IOException if something goes wrong
	 */
	public DG1File(InputStream in) throws IOException {
		read(in);
	}

	/**
	 * The data group tag.
	 * 
	 * @return the tag of the data group
	 */
	public int getTag() {
		return EF_DG1_TAG;
	}

	/**
	 * Gets the MRZ information stored in this file.
	 * 
	 * @return the MRZ information
	 */
	public MRZInfo getMRZInfo() {
		return mrzInfo;
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DG1File " + mrzInfo.toString().replaceAll("\n", "").trim();
	}

	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (!(obj.getClass().equals(this.getClass()))) { return false; }
		DG1File other = (DG1File)obj;
		return mrzInfo.equals(other.mrzInfo);
	}

	public int hashCode() {
		return 3 * mrzInfo.hashCode() + 57;
	}

	private void read(InputStream in) throws IOException {
		BERTLVInputStream tlvIn = new BERTLVInputStream(in);
		int tag = tlvIn.readTag();
		if (tag != PassportFile.EF_DG1_TAG) { throw new IOException("Expected EF_DG1_TAG"); }
		tlvIn.skipToTag(MRZ_INFO_TAG);
		tlvIn.readLength();
		isSourceConsistent = false;
		this.mrzInfo = new MRZInfo(tlvIn);
	}

	private void write(OutputStream out) throws IOException {
		if (isSourceConsistent) {
			out.write(sourceObject);
			out.flush();
		}

		try {
			BERTLVObject ef0101 =
				new BERTLVObject(EF_DG1_TAG,
						new BERTLVObject(0x5F1F, mrzInfo.getEncoded()));

			ef0101.reconstructLength();
			byte[] ef0101bytes = ef0101.getEncoded();
			sourceObject = ef0101bytes;
			isSourceConsistent = true;
			out.write(ef0101bytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the contents of this file as byte array,
	 * includes the ICAO tag and length.
	 * 
	 * @return a byte array containing the file
	 */
	public byte[] getEncoded() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			write(out);
			out.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return out.toByteArray();
	}
}
