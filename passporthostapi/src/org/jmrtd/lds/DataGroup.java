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

import net.sourceforge.scuba.tlv.TLVInputStream;
import net.sourceforge.scuba.tlv.TLVOutputStream;

/**
 * Super class for data group files.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public abstract class DataGroup extends PassportFile
{
	private int dataGroupTag;

	/**
	 * Constructs a datagroup. This constructor
	 * is only visible to the other classes in this package.
	 */
	DataGroup() {
	}

	/**
	 * Constructs a datagroup from the DER encoded data in the
	 * given input stream. Tag and length are read, so the input stream
	 * is positioned just before the value.
	 * 
	 * @param in an input stream
	 */
	protected DataGroup(InputStream in, int dataGroupTag) {
		try {
			this.dataGroupTag = dataGroupTag;
			readObject(in);
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Could not decode: " + ioe.toString());
		}
	}

	protected void readObject(InputStream in) throws IOException {
		TLVInputStream tlvIn = new TLVInputStream(in);	
		int tag = tlvIn.readTag();
		if (tag != dataGroupTag) {
			throw new IllegalArgumentException("Was expecting tag " + Integer.toHexString(dataGroupTag) + ", found " + Integer.toHexString(tag));
		}
		tlvIn.readLength();
		readContent(tlvIn);
		/* isSourceConsistent = true; */
	}

	protected void writeObject(OutputStream out) throws IOException {
		TLVOutputStream tlvOut = out instanceof TLVOutputStream ? (TLVOutputStream)out : new TLVOutputStream(out);
		tlvOut.writeTag(getTag());
		writeContent(tlvOut);
		tlvOut.writeValueEnd(); /* dataGroupTag */
	}

	/*
	 * Naive implementation would be to read dataGroupLength bytes from in into a byte[].
	 */
	protected abstract void readContent(TLVInputStream in) throws IOException;

	/*
	 * Naive implementation would be that byte[] to out.
	 */
	protected abstract void writeContent(TLVOutputStream out) throws IOException;

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DataGroup [" + Integer.toHexString(getTag()) + " (" + getLength() + ")]";
	}

	/**
	 * The data group tag.
	 * 
	 * @return the tag of the data group
	 */
	public int getTag() {
		return dataGroupTag;
	}

	/**
	 * The length of the value of the data group.
	 * 
	 * @return the length of the value of the data group
	 */
	public int getLength() {
		if (isSourceConsistent) {
			return sourceObject.length;
		} else {
			throw new IllegalStateException("Length not yet known");
		}
	}
}
