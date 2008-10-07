/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

package sos.mrtd;

import java.awt.image.BufferedImage;
import java.io.InputStream;

public class DG6File extends DisplayedImageDataGroup
{
	public DG6File(BufferedImage image) {
		super(image);
	}

	public DG6File(InputStream in) {
		super(in);
	}
	
	public int getTag() {
		return EF_DG6_TAG;
	}

	public byte[] getEncoded() {
		// TODO Auto-generated method stub
		return null;
	}

	public String toString() {
		return "DG6File";
	}
}