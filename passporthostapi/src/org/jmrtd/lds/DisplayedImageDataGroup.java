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
	protected static final int
	DISPLAYED_PORTRAIT_TAG = 0x5F40,
	DISPLAYED_SIGNATURE_OR_MARK_TAG = 0x5F43;

	private static final int DISPLAYED_IMAGE_COUNT_TAG = 0x02;

	protected int displayedImageTagToUseForEncoding;

	protected List<DisplayedImageInfo> images;

	public DisplayedImageDataGroup(int dataGroupTag, List<DisplayedImageInfo> images, int displayedImageTagToUseForEncoding) {
		super(dataGroupTag);
		this.images = new ArrayList<DisplayedImageInfo>(images);
		this.displayedImageTagToUseForEncoding = displayedImageTagToUseForEncoding;
	}

	public DisplayedImageDataGroup(int tagToExpect, InputStream in) {
		super(tagToExpect, in);
	}

	protected void readContent(TLVInputStream tlvIn) throws IOException {
		int countTag = tlvIn.readTag();
		if (countTag != DISPLAYED_IMAGE_COUNT_TAG) { /* 02 */
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
		out.writeTag(DISPLAYED_IMAGE_COUNT_TAG);
		out.writeValue(new byte[] { (byte)images.size() });
		for (DisplayedImageInfo imageInfo: images) {
			writeDisplayedImage(imageInfo, out);
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
		if (displayedImageTag != DISPLAYED_PORTRAIT_TAG /* 5F40 */
				&& displayedImageTag != DISPLAYED_SIGNATURE_OR_MARK_TAG /* 5F43 */) {
			throw new IllegalArgumentException("Expected tag 0x5F40 or 0x5F43, found " + Integer.toHexString(displayedImageTag));
		}

		/* FIXME: check whether displayedImageTag == displayedImageTagToUseForEncoding. */

		int type = -1;
		switch (displayedImageTag) {
		case DISPLAYED_PORTRAIT_TAG: type = DisplayedImageInfo.TYPE_PORTRAIT; break;
		case DISPLAYED_SIGNATURE_OR_MARK_TAG: type = DisplayedImageInfo.TYPE_SIGNATURE_OR_MARK; break;
		default: throw new IllegalArgumentException("Cannot determine type in displayed image group (tag " + Integer.toHexString(displayedImageTag));
		}

		displayedImageTagToUseForEncoding = displayedImageTag;
		/* int displayedImageLength = */ tlvIn.readLength();
		/* Displayed Facial Image: ISO 10918, JFIF option
		 * Displayed Finger: ANSI/NIST-ITL 1-2000
		 * Displayed Signature/ usual mark: ISO 10918, JFIF option
		 */
		try {
			BufferedImage image = ImageIO.read(tlvIn);
			if (image != null) {
				add(new DisplayedImageInfo(type, image));
			}
		} catch (IOException ioe) {
			// DEBUG
			ioe.printStackTrace();
		}
	}

	protected void writeDisplayedImage(DisplayedImageInfo info, TLVOutputStream out) throws IOException {
		out.writeTag(displayedImageTagToUseForEncoding);
		ImageIO.write(info.getImage(), "jpg", out);
		out.writeValueEnd();		
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(getClass().getSimpleName());
		result.append(" [");
		boolean isFirst = true;
		for (DisplayedImageInfo info: images) {
			if (isFirst) { isFirst = false; } else { result.append(", "); }
			result.append(info.toString());
		}
		result.append("]");
		return result.toString();
	}

	/**
	 * Gets the images.
	 *
	 * @return images
	 */
	public List<DisplayedImageInfo> getImages() {
		return images;
	}
	
	private void add(DisplayedImageInfo image) {
		if (images == null) {
			images = new ArrayList<DisplayedImageInfo>();
		}
		images.add(image);
	}
}
