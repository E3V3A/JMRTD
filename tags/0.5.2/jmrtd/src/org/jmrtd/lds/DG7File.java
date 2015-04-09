/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
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
import java.util.List;

/**
 * File structure for the EF_DG7 file.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG7File extends DisplayedImageDataGroup {

	private static final long serialVersionUID = 7189545112850471359L;

	/**
	 * Constructs a new file from a list of displayed images.
	 * 
	 * @param images the displayed images, all of which should be of type <i>Signature or mark</i>
	 */
	public DG7File(List<DisplayedImageInfo> images) {
		super(EF_DG7_TAG, images, DisplayedImageInfo.DISPLAYED_SIGNATURE_OR_MARK_TAG);
	}

	/**
	 * Constructs a new file from binary representation.
	 * 
	 * @param inputStream an input stream
	 * 
	 * @throws IOException on error reading input stream
	 */
	public DG7File(InputStream inputStream) throws IOException {
		super(EF_DG7_TAG, inputStream);
	}
}
