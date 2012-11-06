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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

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
	private byte[] imageBytes; /* FIXME: replace imageBytes with an InputStream (or ByteBuffer, ByteArrayOutputStream, Piped*Stream, TeeInputStream)? */
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

	/* PUBLIC CONSRTUCTORS BELOW */

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
		return imageBytes.length;
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
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!other.getClass().equals(this.getClass())) { return false; }
		AbstractImageInfo otherImageInfo = (AbstractImageInfo)other;
		return (imageBytes == otherImageInfo.imageBytes || imageBytes != null && Arrays.equals(imageBytes, otherImageInfo.imageBytes))
				// && getImageLength() == otherImageInfo.getImageLength()
				&& (mimeType == null && otherImageInfo.mimeType == null || mimeType != null && mimeType.equals(otherImageInfo.mimeType))
				&& type == otherImageInfo.type;
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
		return new ByteArrayInputStream(imageBytes);
	}

	/**
	 * Clients should call this method to read the imagebytes.
	 * 
	 * @param inputStream
	 * @param imageLength
	 * @throws IOException
	 */
	protected void readImage(InputStream inputStream, long imageLength) throws IOException {
		this.imageBytes = new byte[(int)(imageLength & 0x00000000FFFFFFFFL)];		
		int bytesRead = 0;
		while (bytesRead < imageLength) {
			int bytesJustNowRead = inputStream.read(imageBytes, bytesRead, (int)(imageLength - bytesRead));
			if (bytesJustNowRead < 0) { throw new IOException("EOF detected after " + bytesRead + " bytes. Was expecting image length " + imageLength + " bytes (i.e., " + (imageLength - bytesRead) + " more bytes)."); }
			bytesRead += bytesJustNowRead;
		}
	}

	protected void writeImage(OutputStream outputStream) throws IOException {
		outputStream.write(imageBytes);
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
		if (imageBytes == null) { this.imageBytes = null; return; }

		try {
			readImage(new ByteArrayInputStream(imageBytes), imageBytes.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected abstract void readObject(InputStream in) throws IOException;

	protected abstract void writeObject(OutputStream out) throws IOException;

	/* ONLY PRIVATE METHODS BELOW */

	private static String typeToString(int type) {
		switch (type) {
		case TYPE_PORTRAIT: return "Portrait";
		case TYPE_SIGNATURE_OR_MARK: return "Signature or usual mark";
		case TYPE_FINGER: return "Finger";
		case TYPE_IRIS: return "Iris";
		default: throw new NumberFormatException("Unknown type: " + Integer.toHexString(type));
		}
	}

	class ImageReadingState {

		private InputStream input;
		private int offsetInInput;
		private byte[] buffer;
		private int buffCount;

		public ImageReadingState(InputStream input, int offsetInInput, int length) {
			this.buffer = new byte[length];
			this.buffCount = 0;
			this.input = input;
			this.offsetInInput = offsetInInput;
		}

		public InputStream getInputStream() {
			return new InputStream() {

				private int counter = 0;

				@Override
				public int read() throws IOException {
					synchronized(input) {
						if (counter >= buffer.length) {
							return -1;
						} else if (counter < buffCount) {
							int b = buffer[counter] & 0xFF;
							counter++;
							return b;
						} else if (counter > buffCount) {
							throw new IllegalStateException("Buffer contains " + buffCount + " bytes, reader state at " + counter);
						} else {
							input.reset();
							input.skip(offsetInInput + counter);
							int b = input.read();
							if (b < 0) { throw new IllegalStateException("Input EOF reached, but only " + counter + " bytes read, was expecting " + buffer.length); }
							buffer[buffCount] = (byte)b;
							buffCount ++;
							counter ++;
							return b;
						}
					}
				}	
			};
		}
	}
}
