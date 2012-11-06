/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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

package org.jmrtd.android;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageUtil {

	public static String
	JPEG_MIME_TYPE = "image/jpeg",
	JPEG2000_MIME_TYPE = "image/jp2",
	JPEG2000_ALT_MIME_TYPE = "image/jpeg2000",
	WSQ_MIME_TYPE = "image/x-wsq";

	private ImageUtil() {
	}

	public static Bitmap read(InputStream inputStream, String mimeType) throws IOException {
		if (JPEG2000_MIME_TYPE.equalsIgnoreCase(mimeType) || JPEG2000_ALT_MIME_TYPE.equalsIgnoreCase(mimeType)) {
			org.jmrtd.jj2000.Bitmap bitmap = org.jmrtd.jj2000.JJ2000Decoder.decode(inputStream);
			return toAndroidBitmap(bitmap);
		} else if (WSQ_MIME_TYPE.equalsIgnoreCase(mimeType)) {
			org.jnbis.Bitmap bitmap = org.jnbis.WSQDecoder.decode(inputStream);
			return toAndroidBitmap(bitmap);
		} else {
			return BitmapFactory.decodeStream(inputStream);
		}
	}

	/* ONLY PRIVATE METHODS BELOW */

	private static Bitmap toAndroidBitmap(org.jmrtd.jj2000.Bitmap bitmap) {
		int[] intData = bitmap.getPixels();
		return Bitmap.createBitmap(intData, 0, bitmap.getWidth(), bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
	}

	private static Bitmap toAndroidBitmap(org.jnbis.Bitmap bitmap) {
		byte[] byteData = bitmap.getPixels();
		int[] intData = new int[byteData.length];
		for (int j = 0; j < byteData.length; j++) {
			intData[j] = 0xFF000000 | ((byteData[j] & 0xFF) << 16) | ((byteData[j] & 0xFF) << 8) | (byteData[j] & 0xFF);
		}
		return Bitmap.createBitmap(intData, 0, bitmap.getWidth(), bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
	}
}
