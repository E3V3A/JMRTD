/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Iris image header and image data
 * based on Section 6.5.3 and Table 4 of
 * ISO/IEC 19794-6 2005.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision$
 */
public class IrisImageInfo extends AbstractImageInfo {

	private static final long serialVersionUID = 833541246115625112L;
	
	/* TODO: proper enums for data types */

	/** Image quality, based on Table 3 in Section 5.5 of ISO 19794-6. */
	public static int
	IMAGE_QUAL_UNDEF = 0xFE, /* (decimal 254) */
	IMAGE_QUAL_LOW_LO = 0x1A,
	IMAGE_QUAL_LOW_HI = 0x32, /* (decimal 26-50) */
	IMAGE_QUAL_MED_LO = 0x33,
	IMAGE_QUAL_MED_HI = 0x4B, /* (decimal 51-75) */
	IMAGE_QUAL_HIGH_LO = 0x4C,
	IMAGE_QUAL_HIGH_HI = 0x64; /* (decimal 76-100) */

	private static final int
	ROT_ANGLE_UNDEF = 0xFFFF,
	ROT_UNCERTAIN_UNDEF = 0xFFFF;

	/** The imageFormat (is more precise than mimeType). Constants are in {@link IrisInfo}. */
	private int imageFormat;
	
	private int imageNumber;
	private int quality;

	// TODO: rotation angle of eye and rotation uncertainty as angles, instead of encoded.
	private int rotationAngle;
	private int rotationAngleUncertainty;

	/**
	 * Constructs an iris image info.
	 * 
	 * @param imageNumber the image number
	 * @param quality quality
	 * @param rotationAngle rotation angle
	 * @param rotationAngleUncertainty rotation angle uncertainty
	 * @param width with
	 * @param height height
	 * @param imageBytes the encoded image
	 * @param imageLength the length of the encoded image
	 * @param imageFormat the image format used for encoding
	 * 
	 * @throws IOException on error reading the image input stream
	 */
	public IrisImageInfo(int imageNumber, int quality, int rotationAngle, int rotationAngleUncertainty,
			int width, int height, InputStream imageBytes, int imageLength, int imageFormat) throws IOException {
		super(TYPE_IRIS, width, height, imageBytes, imageLength, getMimeTypeFromImageFormat(imageFormat));
		if (imageBytes == null) { throw new IllegalArgumentException("Null image bytes"); }
		this.imageNumber = imageNumber;
		this.quality = quality;
		this.rotationAngle = rotationAngle;
		this.rotationAngleUncertainty = rotationAngleUncertainty;
	}

	/**
	 * Constructs an iris image info.
	 * 
	 * @param imageNumber the image number
	 * @param width width
	 * @param height height
	 * @param imageBytes the encoded image
	 * @param imageLength the length of the encoded image
	 * @param imageFormat the image format used for encoding
	 * 
	 * @throws IOException on error reading the image stream
	 */
	public IrisImageInfo(int imageNumber, int width, int height, InputStream imageBytes, int imageLength, int imageFormat) throws IOException {
		this(imageNumber, IMAGE_QUAL_UNDEF, ROT_ANGLE_UNDEF, ROT_UNCERTAIN_UNDEF,
				width, height, imageBytes, imageLength, imageFormat);
	}

	/**
	 * Constructs a new iris image record.
	 * 
	 * @param inputStream input stream
	 * @param imageFormat the image format used for encoding
	 * 
	 * @throws IOException if input cannot be read
	 */
	IrisImageInfo(InputStream inputStream, int imageFormat) throws IOException {
		super(TYPE_IRIS);
		this.imageFormat = imageFormat;
		setMimeType(getMimeTypeFromImageFormat(imageFormat));
		readObject(inputStream);
	}

	/**
	 * Gets the image format.
	 * 
	 * @return the image format
	 */
	public int getImageFormat() {
		return imageFormat;
	}
	
	/**
	 * Gets the image number.
	 * 
	 * @return the image number
	 */
	public int getImageNumber() {
		return imageNumber;
	}

	/**
	 * Gets the quality.
	 * 
	 * @return the image quality
	 */
	public int getQuality() {
		return quality;
	}
	
	/**
	 * Gets the rotation angle.
	 * 
	 * @return the rotationAngle
	 */
	public int getRotationAngle() {
		return rotationAngle;
	}

	/**
	 * Gets the rotation angle uncertainty.
	 * 
	 * @return the rotationAngleUncertainty
	 */
	public int getRotationAngleUncertainty() {
		return rotationAngleUncertainty;
	}
	
	/**
	 * Gets the record length.
	 * 
	 * @return the record length
	 */
	public long getRecordLength() {
		return 11 + getImageLength();
	}

	/**
	 * Generates a textual representation of this object.
	 * 
	 * @return a textual representation of this object
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("IrisImageInfo [");
		result.append("image number: " + imageNumber + ", ");
		result.append("quality: " + quality + ", ");
		result.append("image: ");
		result.append(getWidth() + " x " + getHeight());
		result.append("mime-type: " + getMimeTypeFromImageFormat(imageFormat));
		result.append("]");
		return result.toString();
	}

	protected void readObject(InputStream inputStream) throws IOException {
		DataInputStream dataIn = inputStream instanceof DataInputStream ? (DataInputStream)inputStream : new DataInputStream(inputStream);

		this.imageNumber = dataIn.readUnsignedShort();				/* 2 */
		this.quality = dataIn.readUnsignedByte();					/* + 1 = 3 */

		/*
		 * (65536*angle/360) modulo 65536
		 * ROT_ANGLE_UNDEF = 0xFFFF
		 * Where angle is measured in degrees from
		 * horizontal
		 * Used only for rectilinear images. For polar images
		 * entry shall be ROT_ANGLE_UNDEF
		 */
		rotationAngle = dataIn.readShort();							/* + 2 + 5 */

		/*
		 * Rotation uncertainty = (unsigned short) round
		 * (65536 * uncertainty/180)
		 * Where 0 <= uncertainty < 180
		 * ROT_UNCERTAIN_UNDEF = 0xFFFF
		 * Where uncertainty is measured in degrees and is
		 * the absolute value of maximum error
		 */
		rotationAngleUncertainty = dataIn.readUnsignedShort();		/* + 2 = 7 */

		/*
		 * Size of image data, bytes, 0 - 4294967295.
		 */
		long imageLength = dataIn.readInt() & 0x00000000FFFFFFFFL;	/* + 4 = 11 */

		readImage(inputStream, imageLength);
	}

	protected void writeObject(OutputStream out) throws IOException {

		DataOutputStream dataOut = out instanceof DataOutputStream ? (DataOutputStream)out : new DataOutputStream(out);

		dataOut.writeShort(this.imageNumber);					/* 2 */
		dataOut.writeByte(this.quality);						/* + 1 = 3 */

		/*
		 * (65536*angle/360) modulo 65536
		 * ROT_ANGLE_UNDEF = 0xFFFF
		 * Where angle is measured in degrees from
		 * horizontal
		 * Used only for rectilinear images. For polar images
		 * entry shall be ROT_ANGLE_UNDEF
		 */
		dataOut.writeShort(rotationAngle);						/* + 2 = 5 */

		/*
		 * Rotation uncertainty = (unsigned short) round
		 * (65536 * uncertainty/180)
		 * Where 0 <= uncertainty < 180
		 * ROT_UNCERTAIN_UNDEF = 0xFFFF
		 * Where uncertainty is measured in degrees and is
		 * the absolute value of maximum error
		 */
		dataOut.writeShort(rotationAngleUncertainty);			/* + 2 = 7 */

		dataOut.writeInt(getImageLength());						/* + 4 = 11 */
		writeImage(dataOut);
	}

	private static String getMimeTypeFromImageFormat(int imageFormat) {
		switch (imageFormat) {
		case IrisInfo.IMAGEFORMAT_MONO_RAW:
		case IrisInfo.IMAGEFORMAT_RGB_RAW: return WSQ_MIME_TYPE;
		case IrisInfo.IMAGEFORMAT_MONO_JPEG:
		case IrisInfo.IMAGEFORMAT_RGB_JPEG:
		case IrisInfo.IMAGEFORMAT_MONO_JPEG_LS:
		case IrisInfo.IMAGEFORMAT_RGB_JPEG_LS: return JPEG_MIME_TYPE;
		case IrisInfo.IMAGEFORMAT_MONO_JPEG2000:
		case IrisInfo.IMAGEFORMAT_RGB_JPEG2000: return JPEG2000_MIME_TYPE;
		}
		return null;
	}
}
