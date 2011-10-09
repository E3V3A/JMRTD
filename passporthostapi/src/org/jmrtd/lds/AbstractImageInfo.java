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
 * Base class for ImageInfos.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: $
 */
abstract class AbstractImageInfo implements ImageInfo {

	private int type;
	private String mimeType;
	private byte[] imageBytes; /* FIXME: replace imageBytes with an InputStream (or ByteBuffer, ByteArrayOutputStream, Piped*Stream, TeeInputStream)? */
	private int width, height;

	AbstractImageInfo() {
	}

	AbstractImageInfo(int type) {
		this.type = type;
	}

	AbstractImageInfo(int type, String mimeType) {
		this(type);
		this.mimeType = mimeType;
	}

	AbstractImageInfo(int type, int width, int height, String mimeType) {
		this(type, mimeType);
		this.width = width;
		this.height = height;
	}

	public AbstractImageInfo(int type, int width, int height, byte[] imageBytes, String mimeType) {
		this(type, width, height, mimeType);
		if (imageBytes != null) {
			this.imageBytes = new byte[imageBytes.length];
			System.arraycopy(imageBytes, 0, this.imageBytes, 0, imageBytes.length);
		}
	}

	public AbstractImageInfo(int type, int width, int height, InputStream in, long imageLength, String mimeType) throws IOException {
		this(type, width, height, mimeType);
		readImage(in, imageLength);
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

	public String getMimeType() {
		return mimeType;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getImageLength() {
		return imageBytes.length;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName());
		result.append(" [");
		result.append("type: " + typeToString(type) + ", ");
		result.append("size: " + (imageBytes == null ? 0 : imageBytes.length));
		result.append("]");
		return result.toString();
	}

	public int hashCode() {
		int result = 1234567891;
		result = 3 * result + 5 * type;
		result += 5 * (mimeType == null ? 1337 : mimeType.hashCode()) + 7;
		result += 7 * (imageBytes == null ? 1337 : imageBytes.length) + 11;
		return result;
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!other.getClass().equals(this.getClass())) { return false; }
		AbstractImageInfo otherImageInfo = (AbstractImageInfo)other;
		return (imageBytes == otherImageInfo.imageBytes || imageBytes != null && Arrays.equals(imageBytes, otherImageInfo.imageBytes))
		&& (mimeType == otherImageInfo.mimeType || mimeType != null && mimeType.equals(otherImageInfo.mimeType))
		&& type == otherImageInfo.type;
	}

	public byte[] getEncoded() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeObject(out);
		} catch (IOException ioe) {
			return null;
		}
		return out.toByteArray();
	}

	public InputStream getImageInputStream() {
		return new ByteArrayInputStream(imageBytes);
	}

	protected void readImage(InputStream in, long imageLength) throws IOException {
		imageBytes = new byte[(int)(imageLength & 0x00000000FFFFFFFFL)];
		int bytesRead = 0;
		while (bytesRead < imageLength) {
			int bytesJustNowRead = in.read(imageBytes, bytesRead, (int)(imageLength - bytesRead));
			if (bytesJustNowRead < 0) { throw new IOException("EOF detected after " + bytesRead + " bytes. Was expecting image length " + imageLength + " bytes (i.e., " + (imageLength - bytesRead) + " more bytes)."); }
			bytesRead += bytesJustNowRead;
		}
	}

	protected void writeImage(OutputStream out) throws IOException {
		out.write(imageBytes);
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
		if (imageBytes == null) {
			this.imageBytes = null;
		} else {
			this.imageBytes = new byte[imageBytes.length];
			System.arraycopy(imageBytes, 0, this.imageBytes, 0, imageBytes.length);
		}
	}

	protected abstract void readObject(InputStream in) throws IOException;

	protected abstract void writeObject(OutputStream out) throws IOException;

	public abstract long getRecordLength();
	
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
}
