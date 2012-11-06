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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import net.sourceforge.scuba.data.Country;

/**
 * Utility class with methods for reading icons and other images from file.
 * Assumes file structure (relative to class path) something like this:
 * <ul>
 *    <li>images/</li>
 *    <ul>
 *       <li>flags/</li>
 *       <ul>
 *          <li><i>alpha2countrycode</i>.png</li>
 *       </ul>
 *       <li>icons/</li>
 *       <ul>
 *          <li><i>action</i>.png</li>
 *       </ul>
 *       <li><i>image</i>.png</li>
 *    </ul>
 * </ul>
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: 183 $
 */
public class IconUtil {

	private static final Image
	DEFAULT_16X16_IMAGE =  new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
	DEFAULT_16X11_IMAGE =  new BufferedImage(16, 11, BufferedImage.TYPE_INT_ARGB);

	private static Map<String, Image> famFamFamSilkCache = new HashMap<String, Image>();
	private static Map<Country, Image> flagCache = new HashMap<Country, Image>();

	/** Private constructor to prevent accidental instantiation of this class. */
	private IconUtil() { }
	
	/**
	 * Gets an image from file (residing in the images folder
	 * of the application/project to which <code>c</code> belongs).
	 * 
	 * @param imageName the name of the image (without the extension)
	 * @param c a class
	 * 
	 * @return the image
	 */
	public static Image getImage(String imageName, Class<?> c) {
		try {
			URL imagesDir = getImagesDir(c);
			String fileName = imageName.toLowerCase() + ".png";
			URL imageFile = new URL(imagesDir + "/" + fileName);
			Image image = ImageIO.read(imageFile);
			return image;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Gets an image from file
	 * from the application's images folder.
	 * 
	 * @param imageName the name of the image (without extension)
	 * 
	 * @return the image
	 */
	public static Image getImage(String imageName) {
		try {
			URL imagesDir = getImagesDir();
			String fileName = imageName.toLowerCase() + ".png";
			URL imageFile = new URL(imagesDir + "/" + fileName);
			Image image = ImageIO.read(imageFile);
			return image;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Gets a flag image of a country from file.
	 * 
	 * @param country the country
	 *
	 * @return the image
	 */
	public static Image getFlagImage(Country country) {
		Image image = flagCache.get(country);
		if (image != null) { return image; }
		image = getImageFromCollection("flags", country.toString().toLowerCase(), DEFAULT_16X11_IMAGE);
		flagCache.put(country, image);
		return image;
	}

	/**
	 * Gets an icon from file.
	 * 
	 * @param iconName name without the .png or .gif
	 * @return an image
	 */
	public static Image getFamFamFamSilkIcon(String iconName) {
		Image image = famFamFamSilkCache.get(iconName);
		if (image != null) { return image; }
		image = getImageFromCollection("famfamfam_silk", iconName.toLowerCase(), DEFAULT_16X16_IMAGE);
		famFamFamSilkCache.put(iconName, image);
		return image;
	}

	/* ONLY PRIVATE METHODS BELOW */
	
	private static Image getImageFromCollection(String collectionName, String imageName, Image defaultImage) {
		/* TODO: check if directory with name collectionName or zip file with name collectionName.zip exists */
		return getImageFromZippedCollection(collectionName, imageName, defaultImage);
	}

	private static Image getImageFromZippedCollection(String collectionName, String imageName, Image defaultImage) {
		try {
			URL collectionURL = new URL(getIconsDir() + "/" + collectionName + ".zip");
			ZipInputStream zipIn = new ZipInputStream(collectionURL.openStream());
			ZipEntry entry;
			while ((entry = zipIn.getNextEntry()) != null) {
				String fileName = imageName + ".png";
				String entryName = entry.getName();
				if (entryName != null && entryName.equals(fileName)) {
					Image flagImage = ImageIO.read(zipIn);
					return flagImage;
				}
			}
		} catch (FileNotFoundException fnfe) {
			return defaultImage;
		} catch (Exception e) {
			e.printStackTrace();
			return defaultImage;
		}
		return defaultImage;
	}
	
	private static URL getImagesDir() {
		try {
			URL basePathURL = FileUtil.getBaseDir();
			URL imagesDirURL = new URL(basePathURL + "/images");
			return imagesDirURL;
		} catch (Exception e) {
			return null;
		}
	}
	
	private static URL getImagesDir(Class<?> c) {
		try {
			URL basePathURL = FileUtil.getBaseDir(c);
			URL imagesDirURL = new URL(basePathURL + "/images");
			return imagesDirURL;
		} catch (Exception e) {
			return null;
		}
	}
	
	private static URL getIconsDir() {
		return getImagesDir(IconUtil.class);
	}
}
