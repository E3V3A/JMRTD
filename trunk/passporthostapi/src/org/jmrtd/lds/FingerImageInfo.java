/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jmrtd.cbeff.CBEFFInfo;

/**
 * Data structure for storing view of a single finger
 * image, multi-finger image, or palm. This represents a
 * finger image record header as specified in Section 7.2
 * of ISO/IEC FCD 19794-4 aka Annex F.
 *
 * TODO: proper enums for data types
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision$
 */
public class FingerImageInfo extends AbstractImageInfo {

	private static final long serialVersionUID = -6625447389275461027L;

	/** Finger code, according to Table 5, 7.2.2, ISO 19794-4. */
	public static final int
	/* NOTE: in comment: max image area in sq mm, sq in, with in mm, in, length in mm, in */
	POSITION_UNKNOWN_FINGER = 0, /* 1745 40.6 1.6 38.1 1.5 */
	POSITION_RIGHT_THUMB = 1, /* 1745 40.6 1.6 38.1 1.5 */
	POSITION_RIGHT_INDEX_FINGER = 2, /* 1640 40.6 1.6 38.1 1.5 */
	POSITION_RIGHT_MIDDLE_FINGER = 3, /* 1640 40.6 1.6 38.1 1.5 */
	POSITION_RIGHT_RING_FINGER = 4, /* 1640 40.6 1.6 38.1 1.5 */
	POSITION_RIGHT_LITTLE_FINGER = 5, /* 1640 40.6 1.6 38.1 1.5 */
	POSITION_LEFT_THUMB = 6, /* 1745 40.6 1.6 38.1 1.5 */
	POSITION_LEFT_INDEX_FINGER = 7, /* 1640 40.6 1.6 38.1 1.5 */
	POSITION_LEFT_MIDDLE_FINGER = 8, /* 1640 40.6 1.6 38.1 1.5 */
	POSITION_LEFT_RING_FINGER = 9, /* 1640 40.6 1.6 38.1 1.5 */
	POSITION_LEFT_LITTLE_FINGER = 10, /* 1640 40.6 1.6 38.1 1.5 */
	POSITION_PLAIN_RIGHT_FOUR_FINGERS = 13, /* 6800 83.8 3.3 76.2 3.0 */
	POSITION_PLAIN_LEFT_FOUR_FINGERS = 14, /* 6800 83.8 3.3 76.2 3.0 */
	POSITION_PLAIN_THUMBS = 15; /* 4800 50.8 2.0 76.2 3.0 */

	/** Palm code, according to Table 6, 7.2.2, ISO 19794-4. */
	public static final int
	/* NOTE: in comment: max image area in sq mm, sq in, with in mm, in, length in mm, in */
	POSITION_UNKNOWN_PALM = 20, /* 283.87 13.97 5.5 20.32 8.0 */
	POSITION_RIGHT_FULL_PALM = 21, /* 283.87 13.97 5.5 20.32 8.0 */
	POSITION_RIGHT_WRITER_S_PALM = 22, /* 58.06 4.57 1.8 12.70 5.0 */
	POSITION_LEFT_FULL_PALM = 23, /* 283.87 13.97 5.5 20.32 8.0 */
	POSITION_LEFT_WRITER_S_PALM = 24, /* 58.06 4.57 1.8 12.70 5.0 */
	POSITION_RIGHT_LOWER_PALM = 25, /* 195.16 13.97 5.5 13.97 5.5 */
	POSITION_RIGHT_UPPER_PALM = 26, /* 195.16 13.97 5.5 13.97 5.5 */
	POSITION_LEFT_LOWER_PALM = 27, /* 195.16 13.97 5.5 13.97 5.5 */
	POSITION_LEFT_UPPER_PALM = 28, /* 195.16 13.97 5.5 13.97 5.5 */
	POSITION_RIGHT_OTHER = 29, /* 283.87 13.97 5.5 20.32 8.0 */
	POSITION_LEFT_OTHER = 30, /* 283.87 13.97 5.5 20.32 8.0 */
	POSITION_RIGHT_INTERDIGITAL = 31, /* 106.45 13.97 5.5 7.62 3.0 */
	POSITION_RIGHT_THENAR = 32, /* 77.42 7.62 3.0 10.16 4.0 */
	POSITION_RIGHT_HYPOTHENAR = 33, /* 106.45 7.62 3.0 13.97 5.5 */
	POSITION_LEFT_INTERDIGITAL = 34, /* 106.45 13.97 5.5 7.62 3.0 */
	POSITION_LEFT_THENAR = 35, /* 77.42 7.62 3.0 10.16 4.0 */
	POSITION_LEFT_HYPOTHENAR = 36; /* 106.45 7.62 3.0 13.97 5.5 */

	/** Finger or palm impression type, according to Table 7 in ISO 19794-4. */
	public static final int
	IMPRESSION_TYPE_LIVE_SCAN_PLAIN = 0,
	IMPRESSION_TYPE_LIVE_SCAN_ROLLED = 1,
	IMPRESSION_TYPE_NON_LIVE_SCAN_PLAIN = 2,
	IMPRESSION_TYPE_NON_LIVE_SCAN_ROLLED = 3,
	IMPRESSION_TYPE_LATENT = 7,
	IMPRESSION_TYPE_SWIPE = 8,
	IMPRESSION_TYPE_LIVE_SCAN_CONTACTLESS = 9;

	private static final byte[] FORMAT_TYPE_VALUE = { 0x00, 0x09 };

	private long recordLength;
	private int position;
	private int viewCount;
	private int viewNumber;
	private int quality;
	private int impressionType;

	private int compressionAlgorithm;

	/**
	 * Constructs a finger image info.
	 * 
	 * @param position finger position according to ISO 19794-4
	 * @param viewCount number of views
	 * @param viewNumber the view number
	 * @param quality quality
	 * @param impressionType impression type accordign to ISO 19794-4
	 * @param width width
	 * @param height height
	 * @param imageBytes encoded image bytes
	 * @param imageLength length of encoded image
	 * @param compressionAlgorithm image encoding type according to ISO 19794-4
	 */
	public FingerImageInfo(int position,
			int viewCount, int viewNumber, int quality, int impressionType,
			int width, int height, InputStream imageBytes, int imageLength, int compressionAlgorithm) throws IOException {
		super(TYPE_FINGER, width, height, imageBytes, imageLength, FingerInfo.toMimeType(compressionAlgorithm));
		if (0 > quality || quality > 100) { throw new IllegalArgumentException("Quality needs to be a number between 0 and 100"); }
		if (imageBytes == null) { throw new IllegalArgumentException("Null image"); }
		this.position = position;
		this.viewCount = viewCount;
		this.viewNumber = viewNumber;
		this.quality = quality;
		this.impressionType = impressionType;
		this.compressionAlgorithm = compressionAlgorithm;
		this.recordLength = imageLength + 14;
	}

	/**
	 * Constructs a new finger information record.
	 * 
	 * @param inputStream input stream
	 * @param compressionAlgorithm image format type (which is given in the general record header, not for each individual image)
	 * 
	 * @throws IOException if input cannot be read
	 */
	public FingerImageInfo(InputStream inputStream, int compressionAlgorithm) throws IOException {
		super(TYPE_FINGER, FingerInfo.toMimeType(compressionAlgorithm));
		this.compressionAlgorithm = compressionAlgorithm;
		this.compressionAlgorithm = compressionAlgorithm;
		readObject(inputStream);
	}

	/**
	 * Gets the quality of the overall scanned finger/palm image as a number
	 * between 0 and 100. As specified in 7.2.5 of ISO 19794-4.
	 * 
	 * @return the quality of the overall scanned finger/palm image as a number between 0 and 100
	 */
	public int getQuality() {
		return quality;
	}

	/**
	 * Gets the finger/palm position. As specified in Section 7.2.2 of ISO 19794-4.
	 * 
	 * @return a constant representing the position (see constant definitions starting with <code>POSITION_</code>)
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Gets the compression algorithm. One of
	 * {@link FingerInfo#COMPRESSION_UNCOMPRESSED_BIT_PACKED},
	 * {@link FingerInfo#COMPRESSION_UNCOMPRESSED_NO_BIT_PACKING},
	 * {@link FingerInfo#COMPRESSION_JPEG},
	 * {@link FingerInfo#COMPRESSION_JPEG2000},
	 * {@link FingerInfo#COMPRESSION_PNG},
	 * {@link FingerInfo#COMPRESSION_WSQ}.
	 * As specified in Section 7.1.13 of ISO 19794-4.
	 *
	 * @return a constant representing the used image compression algorithm
	 */
	public int getCompressionAlgorithm() {
		return compressionAlgorithm;
	}

	/**
	 * Gets the total number of specific views available for this finger.
	 * As specified in Section 7.2.3 of ISO 19794-4.
	 * 
	 * @return the total number of specific views available for this finger
	 */
	public int getViewCount() {
		return viewCount;
	}

	/**
	 * Gets the specific image view number associated with the finger.
	 * As specified in Section 7.2.4 of ISO 19794-4.
	 * 
	 * @return the specific image view number associated with the finger
	 */
	public int getViewNumber() {
		return viewNumber;
	}

	/**
	 * Gets the impression type. As specified in Section 7.2.6 of ISO 19794-4.
	 *
	 * @return a constant indicating the impression type (see constant definitions starting with <code>IMPRESSION_TYPE_</code>)
	 */
	public int getImpressionType() {
		return impressionType;
	}

	protected void readObject(InputStream inputStream) throws IOException {
		DataInputStream dataIn = inputStream instanceof DataInputStream ? (DataInputStream)inputStream : new DataInputStream(inputStream);

		/* Finger image header (14), see Table 4, 7.2 in Annex F. */
		/* NOTE: sometimes called "finger header", "finger record header" */
		this.recordLength = dataIn.readInt() & 0xFFFFFFFFL;
		this.position = dataIn.readUnsignedByte();
		this.viewCount = dataIn.readUnsignedByte();
		this.viewNumber = dataIn.readUnsignedByte();
		this.quality = dataIn.readUnsignedByte();
		this.impressionType = dataIn.readUnsignedByte();
		setWidth(dataIn.readUnsignedShort());
		setHeight(dataIn.readUnsignedShort());
		/* int RFU = */ dataIn.readUnsignedByte(); /* Should be 0x0000 */

		long imageLength = recordLength - 14;

		readImage(inputStream, imageLength);
	}

	/**
	 * Writes the biometric data to <code>out</code>.
	 * 
	 * Based on Table 4 in Section 8.3 of ISO/IEC FCD 19794-4.
	 * 
	 * @param out stream to write to
	 * 
	 * @throws IOException if writing to out fails
	 */
	protected void writeObject(OutputStream out) throws IOException {		
		ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
		writeImage(imageOut);
		imageOut.flush();
		byte[] imageBytes = imageOut.toByteArray();
		imageOut.close();
		
		long fingerDataBlockLength = imageBytes.length + 14;

		DataOutputStream dataOut = out instanceof DataOutputStream ? (DataOutputStream)out : new DataOutputStream(out);

		/* Finger Information (14) */
		dataOut.writeInt((int)(fingerDataBlockLength & 0xFFFFFFFFL));
		dataOut.writeByte(position);
		dataOut.writeByte(viewCount);
		dataOut.writeByte(viewNumber);
		dataOut.writeByte(quality);
		dataOut.writeByte(impressionType);
		dataOut.writeShort(getWidth());
		dataOut.writeShort(getHeight());
		dataOut.writeByte(0x00); /* RFU */

		dataOut.write(imageBytes);
		dataOut.flush();
	}

	/**
	 * Gets the record length.
	 * 
	 * @return the record length
	 */
	public long getRecordLength() {
		/* Should be equal to (getImageLength() + 14) */
		return recordLength;
	}
	
	/**
	 * Gets the format type.
	 * 
	 * @return a byte array of length 2
	 */
	public byte[] getFormatType() {
		return FORMAT_TYPE_VALUE;
	}
	
	/**
	 * Gets the biometric sub-type.
	 * 
	 * @return the ICAO/CBEFF (BHT) biometric sub-type
	 */
	public int getBiometricSubtype() {
		return toBiometricSubtype(position);
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
		result.append("FingerImageInfo [");
		result.append("quality: " + quality + ", ");
		result.append("position: " + positionToString(position)+ ", ");
		result.append("impression type: " + impressionTypeToString(impressionType)+ ", ");
		result.append("horizontal line length: " + getWidth() + ", ");
		result.append("vertical line length: " + getHeight() + ", ");
		result.append("image: ");
		result.append(getWidth() + " x " + getHeight());
		result.append(" \"" + FingerInfo.toMimeType(compressionAlgorithm) + "\"");
		result.append("]");
		return result.toString();
	}

	private static String positionToString(int position) {
		switch (position) {
		case POSITION_UNKNOWN_FINGER: return "Unknown finger";
		case POSITION_RIGHT_THUMB: return "Right thumb";
		case POSITION_RIGHT_INDEX_FINGER: return "Right index finger";
		case POSITION_RIGHT_MIDDLE_FINGER: return "Right middle finger";
		case POSITION_RIGHT_RING_FINGER: return "Right ring finger";
		case POSITION_RIGHT_LITTLE_FINGER: return "Right little finger";
		case POSITION_LEFT_THUMB: return "Left thumb";
		case POSITION_LEFT_INDEX_FINGER: return "Left index finger";
		case POSITION_LEFT_MIDDLE_FINGER: return "Left middle finger";
		case POSITION_LEFT_RING_FINGER: return "Left ring finger";
		case POSITION_LEFT_LITTLE_FINGER: return "Left little finger";
		case POSITION_PLAIN_RIGHT_FOUR_FINGERS: return "Right four fingers";
		case POSITION_PLAIN_LEFT_FOUR_FINGERS: return "Left four fingers";
		case POSITION_PLAIN_THUMBS: return "Plain thumbs";
		case POSITION_UNKNOWN_PALM: return "Unknown palm";
		case POSITION_RIGHT_FULL_PALM: return "Right full palm";
		case POSITION_RIGHT_WRITER_S_PALM: return "Right writer's palm";
		case POSITION_LEFT_FULL_PALM: return "Left full palm";
		case POSITION_LEFT_WRITER_S_PALM: return "Left writer's palm";
		case POSITION_RIGHT_LOWER_PALM: return "Right lower palm";
		case POSITION_RIGHT_UPPER_PALM: return "Right upper palm";
		case POSITION_LEFT_LOWER_PALM: return "Left lower palm";
		case POSITION_LEFT_UPPER_PALM: return "Left upper palm";
		case POSITION_RIGHT_OTHER: return "Right other";
		case POSITION_LEFT_OTHER: return "Left other";
		case POSITION_RIGHT_INTERDIGITAL: return "Right interdigital";
		case POSITION_RIGHT_THENAR: return "Right thenar";
		case POSITION_RIGHT_HYPOTHENAR: return "Right hypothenar";
		case POSITION_LEFT_INTERDIGITAL: return "Left interdigital";
		case POSITION_LEFT_THENAR: return "Left thenar";
		case POSITION_LEFT_HYPOTHENAR: return "Left hypothenar";
		default: return null;
		}
	}

	private static String impressionTypeToString(int impressionType) {
		switch (impressionType) {
		case IMPRESSION_TYPE_LIVE_SCAN_PLAIN: return "Live scan plain";
		case IMPRESSION_TYPE_LIVE_SCAN_ROLLED: return "Live scan rolled";
		case IMPRESSION_TYPE_NON_LIVE_SCAN_PLAIN: return "Non-live scan plain";
		case IMPRESSION_TYPE_NON_LIVE_SCAN_ROLLED: return "Non-live scan rolled";
		case IMPRESSION_TYPE_LATENT: return "Latent";
		case IMPRESSION_TYPE_SWIPE: return "Swipe";
		case IMPRESSION_TYPE_LIVE_SCAN_CONTACTLESS: return "Live scan contactless";
		default: return null;
		}
	}
	
	/**
	 * Converts from ISO (FRH) coding to ICAO/CBEFF (BHT) coding.
	 * 
	 * <table>
	 * <tr> <td>Finger</td>       <td>BHT coding</td> <td>FRH coding</td> </tr>
	 * <tr> <td>Right thumb</td>  <td> 5</td>         <td> 1</td> </tr>
	 * <tr> <td>Right index</td>  <td> 9</td>         <td> 2</td> </tr>
	 * <tr> <td>Right middle</td> <td>13</td>         <td> 3</td> </tr>
	 * <tr> <td>Right ring</td>   <td>17</td>         <td> 4</td> </tr>
	 * <tr> <td>Right little</td> <td>21</td>         <td> 5</td> </tr>
	 * <tr> <td>Left thumb</td>   <td> 6</td>         <td> 6</td> </tr>
	 * <tr> <td>Left index</td>   <td>10</td>         <td> 7</td> </tr>
	 * <tr> <td>Left middle</td>  <td>14</td>         <td> 8</td> </tr>
	 * <tr> <td>Left ring</td>    <td>18</td>         <td> 9</td> </tr>
	 * <tr> <td>Left little</td>  <td>22</td>         <td>10</td> </tr>
	 * </table>
	 * 
	 * @param an ISO finger position
	 * 
	 * @return an ICAO biometric subtype
	 */
	private static int toBiometricSubtype(int position) {
		switch (position) {
		case FingerImageInfo.POSITION_UNKNOWN_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE;
		case FingerImageInfo.POSITION_RIGHT_THUMB: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_THUMB;
		case FingerImageInfo.POSITION_RIGHT_INDEX_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_POINTER_FINGER;
		case FingerImageInfo.POSITION_RIGHT_MIDDLE_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_MIDDLE_FINGER;
		case FingerImageInfo.POSITION_RIGHT_RING_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RING_FINGER;
		case FingerImageInfo.POSITION_RIGHT_LITTLE_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LITTLE_FINGER;
		case FingerImageInfo.POSITION_LEFT_THUMB: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_THUMB;
		case FingerImageInfo.POSITION_LEFT_INDEX_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_POINTER_FINGER;
		case FingerImageInfo.POSITION_LEFT_MIDDLE_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_MIDDLE_FINGER;
		case FingerImageInfo.POSITION_LEFT_RING_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RING_FINGER;
		case FingerImageInfo.POSITION_LEFT_LITTLE_FINGER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LITTLE_FINGER;
		case FingerImageInfo.POSITION_PLAIN_RIGHT_FOUR_FINGERS: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT;
		case FingerImageInfo.POSITION_PLAIN_LEFT_FOUR_FINGERS: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		case FingerImageInfo.POSITION_PLAIN_THUMBS: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_THUMB;
		case FingerImageInfo.POSITION_UNKNOWN_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE;
		case FingerImageInfo.POSITION_RIGHT_FULL_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT;
		case FingerImageInfo.POSITION_RIGHT_WRITER_S_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE;
		case FingerImageInfo.POSITION_LEFT_FULL_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		case FingerImageInfo.POSITION_LEFT_WRITER_S_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		case FingerImageInfo.POSITION_RIGHT_LOWER_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT;
		case FingerImageInfo.POSITION_RIGHT_UPPER_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT;
		case FingerImageInfo.POSITION_LEFT_LOWER_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		case FingerImageInfo.POSITION_LEFT_UPPER_PALM: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		case FingerImageInfo.POSITION_RIGHT_OTHER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT;
		case FingerImageInfo.POSITION_LEFT_OTHER: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		case FingerImageInfo.POSITION_RIGHT_INTERDIGITAL: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT;
		case FingerImageInfo.POSITION_RIGHT_THENAR: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT;
		case FingerImageInfo.POSITION_RIGHT_HYPOTHENAR: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT;
		case FingerImageInfo.POSITION_LEFT_INTERDIGITAL: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		case FingerImageInfo.POSITION_LEFT_THENAR: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		case FingerImageInfo.POSITION_LEFT_HYPOTHENAR: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT;
		default: return CBEFFInfo.BIOMETRIC_SUBTYPE_NONE;
		}
	}
}
