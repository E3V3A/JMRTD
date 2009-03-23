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

package org.jmrtd;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.scuba.tlv.BERTLVInputStream;

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
	 * Constructor only visible to the other
	 * classes in this package.
	 */
	DataGroup() {
	}

	protected DataGroup(InputStream in) {
		try {
			BERTLVInputStream tlvIn = new BERTLVInputStream(in);	
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
			return sourceObject.toString();
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
