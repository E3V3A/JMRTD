/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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
 * $Id: PassportFile.java 1320 2011-04-25 19:53:43Z martijno $
 */

package org.jmrtd.lds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Base class for all files (EF_COM, EF_SOD, and data groups) in the LDS.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 1320 $
 */
abstract class AbstractLDSFile implements LDSFile {

	private static final long serialVersionUID = -4908935713109830409L;

	/**
	 * Constructor only visible to the other
	 * classes in this package.
	 */
	AbstractLDSFile() {
	}

	/**
	 * Gets the contents of this file as byte array,
	 * includes the ICAO tag and length.
	 * 
	 * @return a byte array containing the file
	 */
	public byte[] getEncoded() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			writeObject(out);
			out.flush();
			out.close();
			return out.toByteArray();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Reads the file from an input stream.
	 * 
	 * @param inputStream the input stream to read from
	 * 
	 * @throws IOException if reading fails
	 */
	protected abstract void readObject(InputStream inputStream) throws IOException;

	/**
	 * Writes the file to an output stream.
	 * 
	 * @param outputStream the output stream to write to
	 * 
	 * @throws IOException if writing fails
	 */
	protected abstract void writeObject(OutputStream outputStream) throws IOException;
}
