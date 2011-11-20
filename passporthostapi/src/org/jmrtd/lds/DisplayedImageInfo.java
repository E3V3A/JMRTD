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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sourceforge.scuba.tlv.TLVInputStream;
import net.sourceforge.scuba.tlv.TLVOutputStream;
import net.sourceforge.scuba.tlv.TLVUtil;

public class DisplayedImageInfo extends AbstractImageInfo
{
	protected static final int
	DISPLAYED_PORTRAIT_TAG = 0x5F40,
	DISPLAYED_SIGNATURE_OR_MARK_TAG = 0x5F43;
	
	private int displayedImageTag;
	
	public DisplayedImageInfo(int type, byte[] imageBytes) {
		super(type);
		displayedImageTag = getDisplayedImageTagFromType(type);
		setMimeType(getMimeTypeFromType(type));
		setImageBytes(imageBytes);
	}
	
	public DisplayedImageInfo(InputStream in) throws IOException {
		readObject(in);
	}
	
	/**
	 * Reads the displayed image. This method should be implemented by concrete
	 * subclasses. The 5F2E or 7F2E tag and the length are already read.
	 * 
	 * @param in the input stream positioned so that biometric data block tag and length are already read
	 *
	 * @throws IOException if reading fails
	 */
	protected void readObject(InputStream in) throws IOException {
		TLVInputStream tlvIn = in instanceof TLVInputStream ? (TLVInputStream)in : new TLVInputStream(in);
		
		displayedImageTag = tlvIn.readTag();
		if (displayedImageTag != DISPLAYED_PORTRAIT_TAG /* 5F40 */
				&& displayedImageTag != DISPLAYED_SIGNATURE_OR_MARK_TAG /* 5F43 */) {
			throw new IllegalArgumentException("Expected tag 0x5F40 or 0x5F43, found " + Integer.toHexString(displayedImageTag));
		}

		int type = getTypeFromDisplayedImageTag(displayedImageTag);
		setType(type);
		setMimeType(getMimeTypeFromType(type));

		long imageLength = tlvIn.readLength();

		readImage(tlvIn, imageLength);
	}

	protected void writeObject(OutputStream out) throws IOException {
		TLVOutputStream tlvOut = out instanceof TLVOutputStream ? (TLVOutputStream)out : new TLVOutputStream(out);
		tlvOut.writeTag(getDisplayedImageTagFromType(getType()));
		writeImage(tlvOut);
		tlvOut.writeValueEnd();		
	}
	
	int getDisplayedImageTag() {
		return displayedImageTag;
	}
	
	public long getRecordLength() {
		long length = 0;
		int imageLength = getImageLength();
		length += TLVUtil.getTagLength(getDisplayedImageTagFromType(getType()));
		length += TLVUtil.getLengthLength(imageLength);
		length += imageLength;
		return length;
	}
	
	/* ONLY PRIVATE METHODS BELOW */

	/**
	 * As per A1.11.4 in Doc 9303 Part 3 Vol 2:
	 * 
	 * Displayed Facial Image: ISO 10918, JFIF option
	 * Displayed Finger: ANSI/NIST-ITL 1-2000
	 * Displayed Signature/ usual mark: ISO 10918, JFIF option
	 */	
	private static String getMimeTypeFromType(int type) {
		switch (type) {
		case TYPE_PORTRAIT: return "image/jpeg";
		case TYPE_FINGER: return "image/x-wsq";
		case TYPE_SIGNATURE_OR_MARK: return "image/jpeg";
		default: throw new NumberFormatException("Unknown type: " + Integer.toHexString(type));
		}
	}
	
	private static int getDisplayedImageTagFromType(int type) {
		switch(type) {
		case TYPE_PORTRAIT: return DISPLAYED_PORTRAIT_TAG;
		case TYPE_SIGNATURE_OR_MARK: return DISPLAYED_SIGNATURE_OR_MARK_TAG;
		default: throw new NumberFormatException("Unknown type: " + Integer.toHexString(type));
		}
	}
	
	private static int getTypeFromDisplayedImageTag(int tag) {
		switch(tag) {
		case DISPLAYED_PORTRAIT_TAG: return DisplayedImageInfo.TYPE_PORTRAIT;
		case DISPLAYED_SIGNATURE_OR_MARK_TAG: return DisplayedImageInfo.TYPE_SIGNATURE_OR_MARK;
		default: throw new NumberFormatException("Unknown tag: " + Integer.toHexString(tag));
		}
	}
}
