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

import java.io.InputStream;

/**
 * Common interface type for records containing an encoded image.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 */
public interface ImageInfo extends LDSElement {

	/** Mime-types. */
	static String
	JPEG_MIME_TYPE = "image/jpeg",
	JPEG2000_MIME_TYPE = "image/jp2",
	WSQ_MIME_TYPE = "image/x-wsq";
	
	/** Type of image. */
	static final int
	TYPE_PORTRAIT = 0,
	TYPE_SIGNATURE_OR_MARK = 1,
	TYPE_FINGER = 2,
	TYPE_IRIS = 3;
	
	/**
	 * Gets the (biometric) type of the image.
	 * One of
	 * {@link #TYPE_PORTRAIT},
	 * {@link #TYPE_SIGNATURE_OR_MARK},
	 * {@link #TYPE_FINGER},
	 * {@link #TYPE_IRIS}.
	 * 
	 * @return type of image
	 */
	int getType();
	
	/**
	 * Gets the mime-type of the encoded image as a <code>String</code>.
	 * 
	 * @return mime-type string
	 */
	String getMimeType();
	
	/**
	 * Gets the width of the image in pixels.
	 * 
	 * @return image width
	 */
	int getWidth();
	
	/**
	 * Gets the height of the image in pixels.
	 * 
	 * @return image height
	 */
	int getHeight();
	
	/**
	 * Gets the length of the total record (header and data) in bytes.
	 * 
	 * @return the length of the record
	 */
	long getRecordLength();
	
	/**
	 * Gets the length of the encoded image in bytes.
	 * 
	 * @return the length of the image bytes
	 */
	int getImageLength();
	
	/**
	 * Gets an input stream from which the image bytes can be read.
	 * 
	 * @return image input stream
	 */
	InputStream getImageInputStream();
}
