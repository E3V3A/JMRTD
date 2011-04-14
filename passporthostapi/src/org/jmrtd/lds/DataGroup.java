/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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

import net.sourceforge.scuba.tlv.TLVInputStream;
import net.sourceforge.scuba.util.Hex;

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
	private int dataGroupLength;

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
	protected DataGroup(InputStream in) {
		try {
			TLVInputStream tlvIn = new TLVInputStream(in);	
			dataGroupTag = tlvIn.readTag();
			dataGroupLength = tlvIn.readLength();
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Could not decode: " + ioe.toString());
		}
	}

	public abstract byte[] getEncoded();

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		if (isSourceConsistent) {
			return Hex.bytesToHexString(sourceObject);
		}
		return super.toString();
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
		return dataGroupLength;
	}
}
