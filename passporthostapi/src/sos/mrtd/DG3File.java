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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * File structure for the EF_DG3 file.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG3File extends CBEFFDataGroup
{
	/**
	 * Creates a new file based on an input stream.
	 *
	 * @param in an input stream
	 */
	public DG3File(InputStream in) {
		super(in);
	}
	
	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject;
		}
		return null;
	}

	public int getTag() {
		return EF_DG3_TAG;
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		return "DG3File";
	}

	protected void readBiometricData(InputStream in, int length) throws IOException {
		DataInputStream dataIn = new DataInputStream(in);
		sourceObject = new byte[length];
		dataIn.readFully(sourceObject);
		isSourceConsistent = true;
	}
}
