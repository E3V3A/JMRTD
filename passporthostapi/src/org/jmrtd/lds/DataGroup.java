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
 * $Id$
 */

package org.jmrtd.lds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import net.sf.scuba.tlv.TLVInputStream;
import net.sf.scuba.tlv.TLVOutputStream;

import org.jmrtd.io.SplittableInputStream;

/**
 * Base class for data group files.
 *
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 */
public abstract class DataGroup extends AbstractLDSFile {

	private static final long serialVersionUID = -4761360877353069639L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd.lds");

	private int dataGroupTag;
	private int dataGroupLength;

	/**
	 * Constructs a datagroup. This constructor
	 * is only visible to the other classes in this package.
	 */
	DataGroup(int dataGroupTag) {
		this.dataGroupTag = dataGroupTag;
	}

	/**
	 * Constructs a datagroup from the DER encoded data in the
	 * given input stream. Tag and length are read, so the input stream
	 * is positioned just before the value.
	 * 
	 * @param inputStream an input stream
	 */
	protected DataGroup(int dataGroupTag, InputStream inputStream) throws IOException {
		this.dataGroupTag = dataGroupTag;
		readObject(inputStream);
	}

	/**
	 * Reads the contents of this datagroup, including tag and length from an inputstream.
	 * 
	 * @param inputStream the stream to read from
	 * 
	 * @throws IOException if reading from the stream fails
	 */
	protected void readObject(InputStream inputStream) throws IOException {
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		int tag = tlvIn.readTag();
		if (tag != dataGroupTag) {
			throw new IllegalArgumentException("Was expecting tag " + Integer.toHexString(dataGroupTag) + ", found " + Integer.toHexString(tag));
		}
		dataGroupLength = tlvIn.readLength();
		inputStream = new SplittableInputStream(inputStream, dataGroupLength);
		readContent(inputStream);
	}

	protected void writeObject(OutputStream outputStream) throws IOException {
		TLVOutputStream tlvOut = outputStream instanceof TLVOutputStream ? (TLVOutputStream)outputStream : new TLVOutputStream(outputStream);
		int tag = getTag();
		if (dataGroupTag != tag) { dataGroupTag = tag; }
		tlvOut.writeTag(tag);
		byte[] value = getContent();
		int length = value.length;
		if (dataGroupLength != length) { dataGroupLength = length; }
		tlvOut.writeValue(value);
	}

	/**
	 * Reads the contents of the datagroup from an inputstream.
	 * Client code implementing this method should only read the contents
	 * from the inputstream, not the tag or length of the datagroup.
	 * 
	 * @param inputStream the input stream to read from
	 */
	protected abstract void readContent(InputStream inputStream) throws IOException;

	/**
	 * Writes the contents of the datagroup to an outputstream.
	 * Client code implementing this method should only write the contents
	 * to the outputstream, not the tag or length of the datagroup.
	 * 
	 * @param outputStream the output stream to write to 
	 */
	protected abstract void writeContent(OutputStream outputStream) throws IOException;

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
	 * Gets the value part of this DG.
	 * 
	 * @return the value as byte array
	 */
	private byte[] getContent() {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			writeContent(outputStream);
			outputStream.flush();
			outputStream.close();
			return outputStream.toByteArray();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	/**
	 * The length of the value of the data group.
	 * 
	 * @return the length of the value of the data group
	 */
	public int getLength() {
		if (dataGroupLength <= 0) {
			dataGroupLength = getContent().length;
		}
		return dataGroupLength;
	}
}
