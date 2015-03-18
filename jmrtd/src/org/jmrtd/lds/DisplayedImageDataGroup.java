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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.scuba.tlv.TLVInputStream;
import net.sf.scuba.tlv.TLVOutputStream;

/**
 * File structure for displayed image template files.
 * Abstract super class for DG5 - DG7.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
abstract class DisplayedImageDataGroup extends DataGroup {

	private static final long serialVersionUID = 5994136177872308962L;

	private static final int DISPLAYED_IMAGE_COUNT_TAG = 0x02;

	private int displayedImageTagToUse;
	private List<DisplayedImageInfo> imageInfos;

	/**
	 * Constructs a displayed image data group from a list of displayed images.
	 *
	 * @param dataGroupTag a tag indicating DG5, DG6, or DG7
	 * @param imageInfos a list of displayed images
	 * @param displayedImageTagToUse a tag indicating <i>Portrait</i> or <i>Signature or mark</i>
	 */
	public DisplayedImageDataGroup(int dataGroupTag, List<DisplayedImageInfo> imageInfos, int displayedImageTagToUse) {
		super(dataGroupTag);
		if (imageInfos == null) { throw new IllegalArgumentException("imageInfos cannot be null"); }
		this.displayedImageTagToUse = displayedImageTagToUse;
		this.imageInfos = new ArrayList<DisplayedImageInfo>(imageInfos);
		checkTypesConsistentWithTag();
	}

	/**
	 * Constructs a displayed image data group from binary representation.
	 * 
	 * @param dataGroupTag a tag indicating DG5, DG6, or DG7
	 * @param in an input stream
	 */
	public DisplayedImageDataGroup(int dataGroupTag, InputStream in) throws IOException {
		super(dataGroupTag, in);
		if (this.imageInfos == null) { this.imageInfos = new ArrayList<DisplayedImageInfo>(); }
		checkTypesConsistentWithTag();
	}

	protected void readContent(InputStream in) throws IOException {
		TLVInputStream tlvIn = in instanceof TLVInputStream ? (TLVInputStream)in : new TLVInputStream(in);
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
			DisplayedImageInfo imageInfo = new DisplayedImageInfo(tlvIn);
			if (i == 0) { 
				displayedImageTagToUse = imageInfo.getDisplayedImageTag();
			} else if (imageInfo.getDisplayedImageTag() != displayedImageTagToUse){
				throw new IOException("Found images with different displayed image tags inside displayed image datagroup");
			}
			add(imageInfo);
		}
	}

	protected void writeContent(OutputStream out) throws IOException {
		TLVOutputStream tlvOut = out instanceof TLVOutputStream ? (TLVOutputStream)out : new TLVOutputStream(out);
		tlvOut.writeTag(DISPLAYED_IMAGE_COUNT_TAG);
		tlvOut.writeValue(new byte[] { (byte)imageInfos.size() });
		for (DisplayedImageInfo imageInfo: imageInfos) {
			imageInfo.writeObject(tlvOut);
		}
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(getClass().getSimpleName());
		result.append(" [");
		boolean isFirst = true;
		if (imageInfos == null) {
			throw new IllegalStateException("imageInfos cannot be null");	
		}
		// result.append("size: " + imageInfos.size());
		for (DisplayedImageInfo info: imageInfos) {
			if (isFirst) { isFirst = false; } else { result.append(", "); }
			result.append(info.toString());
		}
		result.append("]");
		return result.toString();
	}

	public int hashCode() {
		return 1337 + (imageInfos == null ? 1 : imageInfos.hashCode()) + 31337;
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!getClass().equals(other.getClass())) { return false; }
		DisplayedImageDataGroup otherDG = (DisplayedImageDataGroup)other;
		return this.imageInfos == otherDG.imageInfos || this.imageInfos != null && this.imageInfos.equals(otherDG.imageInfos);
	}

	/**
	 * Gets the image infos.
	 *
	 * @return images
	 */
	public List<DisplayedImageInfo> getImages() {
		return new ArrayList<DisplayedImageInfo>(imageInfos);
	}

	private void add(DisplayedImageInfo image) {
		if (imageInfos == null) {
			imageInfos = new ArrayList<DisplayedImageInfo>();
		}
		imageInfos.add(image);
	}

	private void checkTypesConsistentWithTag() {
		for (DisplayedImageInfo imageInfo: imageInfos) {
			switch (imageInfo.getType()) {
			case ImageInfo.TYPE_SIGNATURE_OR_MARK:
				if (displayedImageTagToUse != DisplayedImageInfo.DISPLAYED_SIGNATURE_OR_MARK_TAG) {
					throw new IllegalArgumentException("\'Portrait\' image cannot be part of a \'Signature or usual mark\' displayed image datagroup");
				}
				break;
			case ImageInfo.TYPE_PORTRAIT:
				if (displayedImageTagToUse != DisplayedImageInfo.DISPLAYED_PORTRAIT_TAG) {
					throw new IllegalArgumentException("\'Signature or usual mark\' image cannot be part of a \'Portrait\' displayed image datagroup");
				}
			}
		}
	}
}
