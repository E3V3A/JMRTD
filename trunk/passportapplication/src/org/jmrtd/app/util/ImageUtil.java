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

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;

/**
 * Utility class for managing images.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 187 $
 */
public class ImageUtil {

	public static String
	JPEG_MIME_TYPE = "image/jpeg",
	JPEG2000_MIME_TYPE = "image/jp2",
	JPEG2000_ALT_MIME_TYPE = "image/jpeg2000",
	WSQ_MIME_TYPE = "image/x-wsq";

	private static final Logger LOGGER = Logger.getLogger("net.sourceforge.scuba.util");

	/**
	 * Reads an image.
	 * 
	 * @param inputStream an input stream
	 * @param imageLength the length of the encoded image
	 * @param mimeType the mime-type of the encoded image
	 *
	 * @return the image
	 * 
	 * @throws IOException if the image cannot be read
	 */
	public static BufferedImage read(InputStream inputStream, long imageLength, String mimeType) throws IOException {
		/* DEBUG */
		synchronized(inputStream) {
			DataInputStream dataIn = new DataInputStream(inputStream);
			byte[] bytes = new byte[(int)imageLength];
			dataIn.readFully(bytes);
			inputStream = new ByteArrayInputStream(bytes);
		}
		/* END DEBUG */
		
		Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mimeType);
		ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
		while (readers.hasNext()) {
			try {
				ImageReader reader = readers.next();
//				LOGGER.info("Using image reader " + reader + " for type " + mimeType);
				BufferedImage image = read(iis, imageLength, reader);
				if (image != null) { return image; }
			} catch (Exception e) {
				e.printStackTrace();
				/* NOTE: this reader doesn't work? Try next one... */
				continue;
			}
		}
		/* Tried all readers */
		throw new IOException("Could not decode \"" + mimeType + "\" image!");
	}

	/**
	 * Writes an image.
	 * 
	 * @param image the image to write
	 * @param mimeType the mime-type of the encoded image
	 * @param out the output stream to write to
	 * 
	 * @throws IOException if the image cannot be written
	 */
	public static void write(Image image, String mimeType, OutputStream out) throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mimeType);
		if (!writers.hasNext()) {
			throw new IOException("No writers for \"" + mimeType + "\"");
		}
		ImageOutputStream ios = ImageIO.createImageOutputStream(out);
		while (writers.hasNext()) {
			try {
				ImageWriter writer = (ImageWriter)writers.next();
				write(image, ios, writer);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				/* NOTE: this writer doesn't work? Try next one... */
				continue;
			} finally {
				ios.flush();
			}
		}
	}

	/* ONLY PRIVATE METHODS BELOW */

	private static BufferedImage read(ImageInputStream iis, long imageLength, ImageReader reader) throws IOException {
		long posBeforeImage = iis.getStreamPosition();
		reader.setInput(iis);
		BufferedImage image = reader.read(0);
		long posAfterImage = iis.getStreamPosition();
		if ((posAfterImage - posBeforeImage) != imageLength) {
			throw new IOException("Image may not have been correctly read");
		}
		return image;
	}

	private static void write(Image image, ImageOutputStream ios, ImageWriter writer) throws IOException {
		writer.setOutput(ios);
		ImageWriteParam pm = writer.getDefaultWriteParam();
		RenderedImage renderedImage = toBufferedImage(image);
		pm.setSourceRegion(new Rectangle(0, 0, renderedImage.getWidth(), renderedImage.getHeight()));
		writer.write(renderedImage);
	}

	/**
	 * This method returns a buffered image with the contents of an image.
	 * From Java Developers Almanac site.
	 * TODO: Check license.
	 */
	private static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage)image;
		}

		// This code ensures that all the pixels in the image are loaded
		image = new ImageIcon(image).getImage();

		// Determine if the image has transparent pixels; for this method's
		// implementation, see e661 Determining If an Image Has Transparent Pixels
		boolean hasAlpha = hasAlpha(image);

		// Create a buffered image with a format that's compatible with the screen
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try {
			// Determine the type of transparency of the new buffered image
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.BITMASK;
			}

			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(
					image.getWidth(null), image.getHeight(null), transparency);
		} catch (HeadlessException e) {
			// The system does not have a screen
		}

		if (bimage == null) {
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		}

		// Copy image to buffered image
		Graphics g = bimage.createGraphics();

		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return bimage;
	}

	// This method returns true if the specified image has transparent pixels
	private static boolean hasAlpha(Image image) {
		// If buffered image, the color model is readily available
		if (image instanceof BufferedImage) {
			BufferedImage bimage = (BufferedImage)image;
			return bimage.getColorModel().hasAlpha();
		}

		// Use a pixel grabber to retrieve the image's color model;
		// grabbing a single pixel is usually sufficient
		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
		}

		// Get the image's color model
		ColorModel cm = pg.getColorModel();
		return cm.hasAlpha();
	}
	
	public static byte[] createTrivialJPEGBytes(int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", out);
			out.flush();
			byte[] bytes = out.toByteArray();
			return bytes;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
