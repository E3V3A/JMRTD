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
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

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
public class FingerInfo extends DisplayedImageInfo
{
	private long fingerDataBlockLength;
	private int fingerOrPalmPostion;
	private int viewCount;
	private int viewNumber;
	private int fingerOrPalmImageQuality;
	private int impressionType;
	private int lineLengthH, lineLengthV;
	private BufferedImage image;

	private DataInputStream dataIn;

	private Logger logger = Logger.getLogger("org.jmrtd");
	
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
		dataIn = (in instanceof DataInputStream) ? (DataInputStream)in : new DataInputStream(in);

		/* Finger Information (14) */
		fingerDataBlockLength = dataIn.readInt() & 0xFFFFFFFFL;
		fingerOrPalmPostion = dataIn.readUnsignedByte();
		viewCount = dataIn.readUnsignedByte();
		viewNumber = dataIn.readUnsignedByte();
		fingerOrPalmImageQuality = dataIn.readUnsignedByte();
		impressionType = dataIn.readUnsignedByte();
		lineLengthH = dataIn.readUnsignedShort();
		lineLengthV = dataIn.readUnsignedShort();
		/* int RFU = */ dataIn.readUnsignedByte(); /* Should be 0x0000 */

		long imageLength = fingerDataBlockLength - 14;

		Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mimeType);
		image = null;
		while (readers.hasNext()) {
			try {
				ImageReader reader = (ImageReader)readers.next();
				ImageInputStream iis = ImageIO.createImageInputStream(dataIn);
				long posBeforeImage = iis.getStreamPosition();
				reader.setInput(iis);
				image = reader.read(0);
				long posAfterImage =  iis.getStreamPosition();
				if ((posAfterImage - posBeforeImage) != imageLength) {
					/* FIXME: send this to a logger instead of stdout. */
					logger.warning("Image may not have been correctly read");
				}
			} catch (Exception e) {
				/* NOTE: this reader doesn't work? Try next one... */
				e.printStackTrace();
				continue;
			}
		}
		/* Tried all readers */
		if (image == null) {
			throw new IOException("Could not decode \"" + mimeType + "\" image!");
		}
	}

	public byte[] getEncoded() {
		/* FIXME: TBD */
		return null;
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
}
