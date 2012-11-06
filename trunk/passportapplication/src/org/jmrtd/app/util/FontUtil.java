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

package org.jmrtd.app.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class with static methods for getting fonts.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: 183 $
 */
public class FontUtil {

	/** Private constructor to prevent accidental instantiation of this class. */
	private FontUtil() { }

	/**
	 * Gets a font from file.
	 *
	 * @param fontFileName the font file name (without the path)
	 * @param style the style of the font (<code>PLAIN</code>, <code>ITALIC</code>, <code>BOLD</code>)
	 * @param size the size of the font
	 * @return the font
	 * 
	 * @throws FontFormatException if the font could not be created
	 */
	public static Font getFont(String fontFileName, int style, float size) throws FontFormatException {
		try {
//			File fontFile = new File(getFontsDir(), fontFileName);
//			Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);

			URL fontURI = new URL(getFontsURL() + "/" + fontFileName);
			Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontURI.openStream());
			return baseFont.deriveFont(style, size);
		} catch (IOException e) {
			throw new FontFormatException("Could not open font file");
		}
	}

	/* ONLY PRIVATE METHODS BELOW */

	private static File getFontsDir() {
		return new File(FileUtil.getBaseDirAsFile(), "fonts");
	}

	private static URL getFontsURL() throws MalformedURLException {
		URL baseURI = FileUtil.getBaseDir();
		return new URL(baseURI + "/fonts");
	}
}
