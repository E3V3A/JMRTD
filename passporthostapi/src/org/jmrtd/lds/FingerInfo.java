/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2011  The JMRTD team
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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

import net.sourceforge.scuba.util.Hex;

/**
 * Data structure for storing finger information as found in DG3.
 * Coding is based on ISO/IEC FCD 19794-4 aka Annex F.
 *
 * WARNING: Work in progress.
 *
 * TODO: proper enums for data types
 * TODO: getEncoded
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class FingerInfo extends DisplayedImageInfo implements BiometricTemplate
{	
	private long fingerDataBlockLength;
	private int fingerOrPalmPostion;
	private int viewCount;
	private int viewNumber;
	private int fingerOrPalmImageQuality;
	private int impressionType;
	private int lineLengthH, lineLengthV;
	private String mimeType;
	private BufferedImage image;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private FingerInfo() {
		super(TYPE_FINGER);
	}

	public FingerInfo(int lotsOfParams /* TODO */) {
		super(TYPE_FINGER);
	}

	/**
	 * Constructs a new finger information record.
	 * 
	 * @param in input stream
	 * 
	 * @throws IOException if input cannot be read
	 */
	FingerInfo(InputStream in, String mimeType) throws IOException {
		this();
		this.mimeType = mimeType;
		readContent(in);
	}

	protected void readContent(InputStream in) throws IOException {
		DataInputStream dataIn = in instanceof DataInputStream ? (DataInputStream)in : new DataInputStream(in);

		/* Finger Information (14) */
		fingerDataBlockLength = dataIn.readInt() & 0xFFFFFFFFL;
		
		System.out.println("DEBUG: FingerInfo.read fingerDataBlockLength = " + fingerDataBlockLength);
		
		fingerOrPalmPostion = dataIn.readUnsignedByte();
		viewCount = dataIn.readUnsignedByte();
		viewNumber = dataIn.readUnsignedByte();
		fingerOrPalmImageQuality = dataIn.readUnsignedByte();
		impressionType = dataIn.readUnsignedByte();
		lineLengthH = dataIn.readUnsignedShort();
		lineLengthV = dataIn.readUnsignedShort();
		/* int RFU = */ dataIn.readUnsignedByte(); /* Should be 0x0000 */

		long imageLength = fingerDataBlockLength - 14;
		image = readImage(in, mimeType, imageLength, false);
	}

	private static BufferedImage readImage(InputStream in, String mimeType, long imageLength, boolean isProgressiveMode) {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mimeType);
		while (readers.hasNext()) {
			try {
				ImageReader reader = (ImageReader)readers.next();
				BufferedImage image = readImage(in, reader, imageLength, isProgressiveMode);
				if (image != null) { return image; }
			} catch (Exception e) {
				/* NOTE: this reader doesn't work? Try next one... */
				continue;
			}
		}
		/* Tried all readers */
		throw new IllegalArgumentException("Could not decode \"" + mimeType + "\" image!");
	}

	private static BufferedImage readImage(InputStream in, ImageReader reader, long imageLength, boolean isProgressiveMode) {
		try {
			ImageInputStream iis = ImageIO.createImageInputStream(in);
			long posBeforeImage = iis.getStreamPosition();
			reader.setInput(iis);
			BufferedImage image = reader.read(0);
			long posAfterImage =  iis.getStreamPosition();
			if ((posAfterImage - posBeforeImage) != imageLength) {
				LOGGER.warning("Image may not have been correctly read");
			}
			return image;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	/**
	 * Writes the biometric data to <code>out</code>.
	 * 
	 * This currently re-encodes the image, even if its source was a
	 * binary representation.
	 * 
	 * @param out stream to write to
	 * 
	 * @throws IOException if writing to out fails
	 */
	protected void writeContent(OutputStream out) throws IOException {		
		ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
		writeImage(image, imageOut, mimeType);
		imageOut.flush();
		byte[] imageBytes = imageOut.toByteArray();
		
		System.out.println("DEBUG: imageBytes = " + Hex.bytesToHexString(imageBytes, 0, 100));
		
		imageOut.close();

		fingerDataBlockLength = imageBytes.length + 14;
		
		DataOutputStream dataOut = out instanceof DataOutputStream ? (DataOutputStream)out : new DataOutputStream(out);
		
		/* Finger Information (14) */
		dataOut.writeInt((int)(fingerDataBlockLength & 0xFFFFFFFFL));
		System.out.println("DEBUG: FingerInfo.write fingerDataBlockLength = " + fingerDataBlockLength);
		dataOut.writeByte(fingerOrPalmPostion);
		dataOut.writeByte(viewCount);
		dataOut.writeByte(viewNumber);
		dataOut.writeByte(fingerOrPalmImageQuality);
		dataOut.writeByte(impressionType);
		dataOut.writeShort(lineLengthH);
		dataOut.writeShort(lineLengthV);
		dataOut.writeByte(0x00); /* RFU */

		dataOut.write(imageBytes);
		dataOut.flush();
	}
	
	private void writeImage(BufferedImage image, OutputStream out, String mimeType)
	throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mimeType);
		if (!writers.hasNext()) {
			throw new IOException("No writers for \"" + mimeType + "\"");
		}
		ImageOutputStream ios = ImageIO.createImageOutputStream(out);
		while (writers.hasNext()) {
			try {
				ImageWriter writer = (ImageWriter)writers.next();
				writer.setOutput(ios);
				ImageWriteParam pm = writer.getDefaultWriteParam();
				pm.setSourceRegion(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
				writer.write(image);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			} finally {
				ios.flush();
			}
		}
	}

	public byte[] getEncoded() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			writeContent(out);
			out.flush();
			out.close();
			return out.toByteArray();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	/**
	 * Generates a textual representation of this object.
	 * 
	 * @return a textual representation of this object
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "FingerInfo";
	}

	public BufferedImage getImage()  {
		return getImage(false);
	}
	
	public BufferedImage getImage(boolean isProgressive) {
		return image;
	}

	public String getMimeType() {
		return mimeType;
	}
}
