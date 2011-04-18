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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.sourceforge.scuba.tlv.TLVInputStream;
import net.sourceforge.scuba.tlv.TLVOutputStream;

/**
 * File structure for displayed image template files.
 * Abstract super class for DG5 - DG7.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
abstract class DisplayedImageDataGroup extends DataGroup
{
	private static final int DISPLAYED_IMAGE_COUNT = 0x02;
	private static final int DISPLAYED_PORTRAIT_TAG = 0x5F40;
	private static final int DISPLAYED_SIGNATURE_OR_MARK_TAG = 0x5F43;

	private List<DisplayedImageInfo> images;

	protected DisplayedImageDataGroup() {
		this.images = new ArrayList<DisplayedImageInfo>();
	}
	
	public DisplayedImageDataGroup(InputStream in, int tagToExpect) {
		super(in, tagToExpect);
	}

	protected void readContent(TLVInputStream tlvIn) throws IOException {
		int countTag = tlvIn.readTag();
		if (countTag != DISPLAYED_IMAGE_COUNT) { /* 02 */
			throw new IllegalArgumentException("Expected tag 0x02 in displayed image structure, found " + Integer.toHexString(countTag));
		}
		int countLength = tlvIn.readLength();
		if (countLength != 1) {
			throw new IllegalArgumentException("DISPLAYED_IMAGE_COUNT should have length 1");
		}
		int count = (tlvIn.readValue()[0] & 0xFF);
		for (int i = 0; i < count; i++) {
			readDisplayedImage(tlvIn);
		}
	}
	
	protected void writeContent(TLVOutputStream out) throws IOException {
		out.writeTag(DISPLAYED_IMAGE_COUNT);
		out.writeValue(new byte[] { (byte)images.size() });
		for (DisplayedImageInfo imageInfo: images) {
			writeDisplayedImage(out);
		}
	}

	/**
	 * Reads the displayed image. This method should be implemented by concrete
	 * subclasses. The 5F2E or 7F2E tag and the length are already read.
	 * 
	 * @param tlvIn the input stream positioned so that biometric data block tag and length are already read
	 * @param length the length
	 * @throws IOException if reading fails
	 */
	protected void readDisplayedImage(TLVInputStream tlvIn) throws IOException {
		int displayedImageTag = tlvIn.readTag();
		if (displayedImageTag != DISPLAYED_PORTRAIT_TAG /* 5F40 */ &&
				displayedImageTag != DISPLAYED_SIGNATURE_OR_MARK_TAG /* 5F43 */) {
			throw new IllegalArgumentException("Expected tag 0x5F40 or 0x5F43, found " + Integer.toHexString(displayedImageTag));
		}
		/* int displayedImageLength = */ tlvIn.readLength();
		/* Displayed Facial Image: ISO 10918, JFIF option
		 * Displayed Finger: ANSI/NIST-ITL 1-2000
		 * Displayed Signature/ usual mark: ISO 10918, JFIF option
		 */
		BufferedImage image = ImageIO.read(tlvIn);
		int type = -1;
		if (image != null) {
			switch (displayedImageTag) {
			case DISPLAYED_PORTRAIT_TAG: type = DisplayedImageInfo.TYPE_PORTRAIT; break;
			case DISPLAYED_SIGNATURE_OR_MARK_TAG: type = DisplayedImageInfo.TYPE_SIGNATURE_OR_MARK; break;
			default: throw new IllegalArgumentException("Unknown type in displayed image group (tag " + Integer.toHexString(displayedImageTag));
			}
			images.add(new DisplayedImageInfo(type, image));
		}
	}
	
	protected abstract void writeDisplayedImage(TLVOutputStream out) throws IOException;

	/**
	 * Gets the images.
	 *
	 * @return images
	 */
	public List<DisplayedImageInfo> getImages() {
		return images;
	}
}
