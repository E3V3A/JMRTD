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

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.scuba.tlv.TLVOutputStream;

/**
 * File structure for the EF_DG7 file.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG7File extends DisplayedImageDataGroup
{

	public DG7File(InputStream in) {
		super(in, EF_DG7_TAG);
	}

	public int getTag() {
		return EF_DG7_TAG;
	}

	public String toString() {
		return "DG7File";
	}
	
	protected void writeDisplayedImage(TLVOutputStream out) throws IOException {
		// FIXME: which tag? See readDisplayedImage in DisplayedImageDataGroup
	}
}
