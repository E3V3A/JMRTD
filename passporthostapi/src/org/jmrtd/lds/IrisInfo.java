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

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import net.sourceforge.scuba.util.Images;

/**
 * Data structure for storing iris image information as found in DG4.
 * Coding is based on ISO/IEC FCD 19794-6 aka Annex E.
 *
 * WARNING: Work in progress.
 *
 * TODO: proper enums for data types
 * TODO: getEncoded
 * 
 * TODO: this is just the iris image, need a class for iris feature (containing multiple images of same eye)? See DG4File class.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class IrisInfo extends DisplayedImageInfo implements BiometricTemplate
{
	private static final String DEFAULT_MIME_TYPE = "image/jpeg";
	
	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	public static final int
	IMAGEFORMAT_MONO_RAW = 2, /* (0x0002) */
	IMAGEFORMAT_RGB_RAW = 4, /* (0x0004) */
	IMAGEFORMAT_MONO_JPEG = 6, /* (0x0006) */
	IMAGEFORMAT_RGB_JPEG = 8, /* (0x0008) */
	IMAGEFORMAT_MONO_JPEG_LS = 10, /* (0x000A) */
	IMAGEFORMAT_RGB_JPEG_LS = 12, /* (0x000C) */
	IMAGEFORMAT_MONO_JPEG2000 = 14, /* (0x000E) */
	IMAGEFORMAT_RGB_JPEG2000 = 16; /* (0x0010) */

	private BufferedImage image;

	private DataInputStream dataIn;

	private IrisInfo() {
		super(TYPE_IRIS);
	}

	public IrisInfo(int lotsOfParams /* TODO */) {
		super(TYPE_IRIS);
	}

	/**
	 * Constructs a new iris image record.
	 * 
	 * @param in input stream
	 * 
	 * @throws IOException if input cannot be read
	 */
	IrisInfo(InputStream in, int imageFormat) throws IOException {
		this();
		dataIn = (in instanceof DataInputStream) ? (DataInputStream)in : new DataInputStream(in);
		String mimeType = toMimeType(imageFormat);

		/* int imageNumber = */ dataIn.readUnsignedShort();
		/* int quality = */ dataIn.readUnsignedByte();

		/*
		 * (65536*angle/360) modulo 65536
		 * ROT_ANGLE_UNDEF = 0xFFFF
		 * Where angle is measured in degrees from
		 * horizontal
		 * Used only for rectilinear images. For polar images
		 * entry shall be ROT_ANGLE_UNDEF
		 */
		/* int rotAngleEye = */ dataIn.readShort();

		/*
		 * Rotation uncertainty = (unsigned short) round
		 * (65536 * uncertainty/180)
		 * Where 0 <= uncertainty < 180
		 * ROT_UNCERTAIN_UNDEF = 0xFFFF
		 * Where uncertainty is measured in degrees and is
		 * the absolute value of maximum error
		 */
		/* int rotUncertainty = */ dataIn.readUnsignedShort();
		
		long imageLength = dataIn.readInt() & 0xFFFFFFFFL;

		image = Images.readImage(in, mimeType, imageLength, false);
		
		/* Tried all readers */
		if (image == null) {
			throw new IOException("Could not decode \"" + mimeType + "\" image!");
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
		return "IrisInfo";
	}

	public BufferedImage getImage()  {
		return getImage(false);
	}

	public BufferedImage getImage(boolean isProgressive) {
		return image;
	}

	private static String toMimeType(int imageFormat) {
		String mimeType = null;
		switch (imageFormat) {
		case IMAGEFORMAT_MONO_RAW:
		case IMAGEFORMAT_RGB_RAW: mimeType = "image/x-raw"; break;
		case IMAGEFORMAT_MONO_JPEG:
		case IMAGEFORMAT_RGB_JPEG:
		case IMAGEFORMAT_MONO_JPEG_LS:
		case IMAGEFORMAT_RGB_JPEG_LS:  mimeType = "image/jpeg"; break;
		case IMAGEFORMAT_MONO_JPEG2000:
		case IMAGEFORMAT_RGB_JPEG2000: mimeType = "image/jpeg2000"; break;
		}
		return mimeType;
	}

	public byte[] getEncoded() {
		// TODO Auto-generated method stub
		return null; // FIXME
	}

	public String getMimeType() {
		return DEFAULT_MIME_TYPE; // FIXME
	}
}
