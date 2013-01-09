/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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

package org.jmrtd.lds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.jmrtd.io.InputStreamBuffer;
import org.jmrtd.io.SplittableInputStream;

/**
 * Base class for image infos.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
abstract class AbstractImageInfo implements ImageInfo {

	private static final long serialVersionUID = 2870092217269116309L;

	private int type;
	private String mimeType;
	private byte[] imageBytes;
	
	private SplittableInputStream splittableInputStream;
	int imagePositionInInputStream;
	int imageLength;
	
	private int width, height;

	/* PACKAGE ONLY VISIBLE CONSTRUCTORS BELOW */

	AbstractImageInfo() {
	}

	AbstractImageInfo(int type) {
		this.type = type;
	}

	AbstractImageInfo(int type, String mimeType) {
		this(type);
		this.mimeType = mimeType;
	}

	private AbstractImageInfo(int type, int width, int height, String mimeType) {
		this(type, mimeType);
		this.width = width;
		this.height = height;
	}

	/* PUBLIC CONSRTUCTOR BELOW */

	/**
	 * Constructs an abstract image info.
	 * 
	 * @param type type of image info
	 * @param width width of image
	 * @param height height of image
	 * @param inputStream encoded image
	 * @param imageLength length of encoded image
	 * @param mimeType mime-type of encoded image
	 * 
	 * @throws IOException if reading fails
	 */
	public AbstractImageInfo(int type, int width, int height, InputStream inputStream, long imageLength, String mimeType) throws IOException {
		this(type, width, height, mimeType);
		readImage(inputStream, imageLength);
	}

	/* PUBLIC METHODS BELOW */

	/**
	 * Content type, one of
	 * {@link ImageInfo#TYPE_PORTRAIT},
	 * {@link ImageInfo#TYPE_FINGER},
	 * {@link ImageInfo#TYPE_IRIS},
	 * {@link ImageInfo#TYPE_SIGNATURE_OR_MARK}.
	 * 
	 * @return content type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Gets the mime-type of the encoded image.
	 * 
	 * @return the mime-type of the encoded image
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Gets the width of the image.
	 * 
	 * @return the width of the image
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Gets the height of the image.
	 * 
	 * @return the height of the image
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Gets the length of the encoded image.
	 * 
	 * @return the length of the encoded image
	 */
	public int getImageLength() {
		/* DEBUG: START */
		if (splittableInputStream != null) {
			return imageLength;
			/* DEBUG: END */

		} else if (imageBytes != null) {
			return imageBytes.length;
		} else {
			throw new IllegalStateException("DEBUG");
		}
	}

	/**
	 * Gets a textual representation of this image info.
	 * 
	 * @return a textual representation of this image info
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName());
		result.append(" [");
		result.append("type: " + typeToString(type) + ", ");
		result.append("size: " + getImageLength());
		result.append("]");
		return result.toString();
	}

	public int hashCode() {
		int result = 1234567891;
		result = 3 * result + 5 * type;
		result += 5 * (mimeType == null ? 1337 : mimeType.hashCode()) + 7;
		result += 7 * getImageLength() + 11;
		return result;
	}

	public boolean equals(Object other) {
		try {
			if (other == null) { return false; }
			if (other == this) { return true; }
			if (!other.getClass().equals(this.getClass())) { return false; }
			AbstractImageInfo otherImageInfo = (AbstractImageInfo)other;
			return (Arrays.equals(getImageBytes(), otherImageInfo.getImageBytes()))
					// && getImageLength() == otherImageInfo.getImageLength()
					&& (mimeType == null && otherImageInfo.mimeType == null || mimeType != null && mimeType.equals(otherImageInfo.mimeType))
					&& type == otherImageInfo.type;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Encodes this image info.
	 * 
	 * @return a byte array containing the encoded image info
	 */
	public byte[] getEncoded() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeObject(out);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
		return out.toByteArray();
	}

	/**
	 * Gets the record length.
	 * 
	 * @return the record length
	 */
	public abstract long getRecordLength();

	/**
	 * Gets the encoded image as an input stream.
	 * 
	 * @return an input stream containing the encoded image
	 */
	public InputStream getImageInputStream() {
		/* DEBUG: START */
		if (splittableInputStream != null) {
			System.out.println("DEBUG: getInputStream, from " + splittableInputStream.getPosition() + "/" + splittableInputStream.getLength() + ", from position " + imagePositionInInputStream);
			return splittableInputStream.getInputStream(imagePositionInInputStream);
			/* DEBUG: END */
		} else if (imageBytes != null) {
			return new ByteArrayInputStream(imageBytes);
		} else {
			throw new IllegalStateException("DEBUG");
		}
	}

	/**
	 * Clients should call this method after positioning the input stream to the
	 * image bytes.
	 * 
	 * @param inputStream
	 * @param imageLength
	 * @throws IOException
	 */
	protected void readImage(InputStream inputStream, long imageLength) throws IOException {
		/* DEBUG: START */
		if (inputStream instanceof SplittableInputStream) {
			this.imageBytes = null;
			this.splittableInputStream = (SplittableInputStream)inputStream;
			this.imagePositionInInputStream = splittableInputStream.getPosition();
			this.imageLength = (int)imageLength;
			splittableInputStream.skip(imageLength);
		} else {
			/* DEBUG: END */
			this.splittableInputStream = null;
			this.imageBytes = new byte[(int)imageLength];
			DataInputStream dataIn = new DataInputStream(inputStream);
			dataIn.readFully(this.imageBytes);
		}
	}

	protected void writeImage(OutputStream outputStream) throws IOException {
		outputStream.write(getImageBytes());
	}

	final protected void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	final protected void setType(int type) {
		this.type = type;
	}

	final protected void setWidth(int width) {
		this.width = width;
	}

	final protected void setHeight(int height) {
		this.height = height;
	}

	final protected void setImageBytes(byte[] imageBytes) {
		try {
			readImage(new ByteArrayInputStream(imageBytes), imageBytes.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected abstract void readObject(InputStream in) throws IOException;

	protected abstract void writeObject(OutputStream out) throws IOException;

	/* ONLY PRIVATE METHODS BELOW */

	private byte[] getImageBytes() throws IOException {
		InputStream inputStream = null;
		int length = getImageLength();
		byte[] imageBytes = new byte[length];
		inputStream = getImageInputStream();
		DataInputStream imageInputStream = new DataInputStream(inputStream);
		imageInputStream.readFully(imageBytes);		
		return imageBytes;
	}

	private static String typeToString(int type) {
		switch (type) {
		case TYPE_PORTRAIT: return "Portrait";
		case TYPE_SIGNATURE_OR_MARK: return "Signature or usual mark";
		case TYPE_FINGER: return "Finger";
		case TYPE_IRIS: return "Iris";
		default: throw new NumberFormatException("Unknown type: " + Integer.toHexString(type));
		}
	}	
}
