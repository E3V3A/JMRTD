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
 * $Id: $
 */

package org.jmrtd.lds;

import java.awt.image.BufferedImage;

public class DisplayedImageInfo
{
	public static final int
	TYPE_PORTRAIT = 0,
	TYPE_SIGNATURE_OR_MARK = 1,
	TYPE_FINGER = 2,
	TYPE_IRIS = 3;

	protected int type;
	private BufferedImage image;
	
	private byte[] sourceObject; // FIXME
	private boolean isSourceConsistent; // FIXME

	public DisplayedImageInfo(int type) {
		this.type = type;
	}

	public DisplayedImageInfo(int type, BufferedImage image) {
		this(type);
		this.image = image;
	}

	public int getType() {
		return type;
	}

	public BufferedImage getImage() {
		return getImage(false);
	}

	public BufferedImage getImage(boolean isProgressive) {
		return image;
	}
}
