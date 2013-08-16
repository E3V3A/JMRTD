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
 * $Id: $
 */

package org.jmrtd.lds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.jmrtd.cbeff.BiometricDataBlock;
import org.jmrtd.cbeff.CBEFFInfo;
import org.jmrtd.cbeff.ISO781611;
import org.jmrtd.cbeff.StandardBiometricHeader;

/**
 * Iris record header and biometric subtype blocks
 * based on Section 6.5.3 and Table 2 of
 * ISO/IEC 19794-6 2005.
 * 
 * TODO: proper enums for fields.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: $
 */
public class IrisInfo extends AbstractListInfo<IrisBiometricSubtypeInfo> implements BiometricDataBlock {

	private static final long serialVersionUID = -3415309711643815511L;
	
	private static final Logger LOGGER = Logger.getLogger("org.jmrtd.lds");

	/** Format identifier 'I', 'I', 'R', 0x00. */
	private static final int FORMAT_IDENTIFIER = 0x49495200;

	/** Version number. */
	private static final int VERSION_NUMBER = 0x30313000;

	/** Format owner identifier of ISO/IEC JTC1/SC37. */
	private static final int FORMAT_OWNER_VALUE = 0x0101;

	/**
	 * ISO/IEC JTC1/SC37 uses 0x0009 according to <a href="http://www.ibia.org/cbeff/_bdb.php">IBIA</a>.
	 * (ISO FCD 19794-6 specified this as 0x0601).
	 */	
	private static final int FORMAT_TYPE_VALUE = 0x0009;

	/** Image format */
	/* TODO: reference to specification. */
	public static final int
	IMAGEFORMAT_MONO_RAW = 2, /* (0x0002) */
	IMAGEFORMAT_RGB_RAW = 4, /* (0x0004) */
	IMAGEFORMAT_MONO_JPEG = 6, /* (0x0006) */
	IMAGEFORMAT_RGB_JPEG = 8, /* (0x0008) */
	IMAGEFORMAT_MONO_JPEG_LS = 10, /* (0x000A) */
	IMAGEFORMAT_RGB_JPEG_LS = 12, /* (0x000C) */
	IMAGEFORMAT_MONO_JPEG2000 = 14, /* (0x000E) */
	IMAGEFORMAT_RGB_JPEG2000 = 16; /* (0x0010) */

	/** Constant for capture device Id, based on Table 2 in Section 5.5 in ISO 19794-6. */
	public static final int
	CAPTURE_DEVICE_UNDEF = 0;

	/** Constant for horizontal and veritical orientation, based on Table 2 in Section 5.5 in ISO 19794-6. */
	public static final int
	ORIENTATION_UNDEF = 0,
	ORIENTATION_BASE = 1,
	ORIENTATION_FLIPPED = 2;

	/** Scan type (rectilinear only), based on Table 2 in Section 5.5 in ISO 19794-6. */
	public static final int
	SCAN_TYPE_UNDEF = 0,
	SCAN_TYPE_PROGRESSIVE = 1,
	SCAN_TYPE_INTERLACE_FRAME = 2,
	SCAN_TYPE_INTERLACE_FIELD = 3,
	SCAN_TYPE_CORRECTED = 4;

	/** Iris occlusion (polar only), based on Table 2 in Section 5.5 in ISO 19794-6. */
	public static final int
	IROCC_UNDEF = 0,
	IROCC_PROCESSED = 1;

	/** Iris occlusion filling (polar only), based on Table 2 in Section 5.5 in ISO 19794-6. */
	public static final int
	IROCC_ZEROFILL = 0,
	IROC_UNITFILL = 1;

	/* TODO: reference to specification. */
	public static final int
	INTENSITY_DEPTH_UNDEF = 0;

	/* TODO: reference to specification. */
	public static final int
	TRANS_UNDEF = 0, TRANS_STD = 1;

	/* TODO: reference to specification. */
	public static final int
	IRBNDY_UNDEF = 0,
	IRBNDY_PROCESSED = 1;

	private long recordLength;

	private int captureDeviceId; /* 16 bit */

	private int horizontalOrientation, verticalOrientation;
	private int scanType;
	private int irisOcclusion;
	private int occlusionFilling;
	private int boundaryExtraction;

	private int irisDiameter;
	private int imageFormat;
	private int rawImageWidth, rawImageHeight;
	private int intensityDepth;
	private int imageTransformation;

	/*
	 * Length 16, starts with 'D' (serial), 'M' (MAC address) or 'P' (processor Id),
	 * or all zeroes (indicating no serial number).
	 */
	private byte[] deviceUniqueId;

	private StandardBiometricHeader sbh;

	/**
	 * Constructs a new iris info object.
	 * 
	 * @param captureDeviceId capture device identifier assigned by vendor
	 * @param horizontalOrientation horizontal orientation: {@link #ORIENTATION_UNDEF}, {@link #ORIENTATION_BASE}, or {@link #ORIENTATION_FLIPPED}
	 * @param verticalOrientation vertical orientation: {@link #ORIENTATION_UNDEF}, {@link #ORIENTATION_BASE}, or {@link #ORIENTATION_FLIPPED}
	 * @param scanType scan type
	 * @param irisOcclusion iris occlusion (polar only)
	 * @param occlusionFilling occlusion filling (polar only)
	 * @param boundaryExtraction boundary extraction (polar only)
	 * @param irisDiameter expected iris diameter in pixels (rectilinear only)
	 * @param imageFormat image format of data blob (JPEG, raw, etc.)
	 * @param rawImageWidth raw image width, pixels
	 * @param rawImageHeight raw image height, pixels
	 * @param intensityDepth intensity depth, bits, per color
	 * @param imageTransformation transformation to polar image (polar only)
	 * @param deviceUniqueId a 16 character string uniquely identifying the device or source of that data
	 * @param irisBiometricSubtypeInfos the iris biometric subtype records
	 */
	public IrisInfo(int captureDeviceId, int horizontalOrientation, int verticalOrientation,
			int scanType, int irisOcclusion, int occlusionFilling,
			int boundaryExtraction, int irisDiameter, int imageFormat,
			int rawImageWidth, int rawImageHeight, int intensityDepth, int imageTransformation,
			byte[] deviceUniqueId,
			List<IrisBiometricSubtypeInfo> irisBiometricSubtypeInfos) {
		this(null, captureDeviceId, horizontalOrientation, verticalOrientation,
				scanType, irisOcclusion, occlusionFilling,
				boundaryExtraction, irisDiameter, imageFormat,
				rawImageWidth, rawImageHeight, intensityDepth, imageTransformation,
				deviceUniqueId, irisBiometricSubtypeInfos);
	}
	
	/**
	 * Constructs a new iris info object.
	 * 
	 * @param sbh standard biometric header to use
	 * @param captureDeviceId capture device identifier assigned by vendor
	 * @param horizontalOrientation horizontal orientation: {@link #ORIENTATION_UNDEF}, {@link #ORIENTATION_BASE}, or {@link #ORIENTATION_FLIPPED}
	 * @param verticalOrientation vertical orientation: {@link #ORIENTATION_UNDEF}, {@link #ORIENTATION_BASE}, or {@link #ORIENTATION_FLIPPED}
	 * @param scanType scan type
	 * @param irisOcclusion iris occlusion (polar only)
	 * @param occlusionFilling occlusion filling (polar only)
	 * @param boundaryExtraction boundary extraction (polar only)
	 * @param irisDiameter expected iris diameter in pixels (rectilinear only)
	 * @param imageFormat image format of data blob (JPEG, raw, etc.)
	 * @param rawImageWidth raw image width, pixels
	 * @param rawImageHeight raw image height, pixels
	 * @param intensityDepth intensity depth, bits, per color
	 * @param imageTransformation transformation to polar image (polar only)
	 * @param deviceUniqueId a 16 character string uniquely identifying the device or source of that data
	 * @param irisBiometricSubtypeInfos the iris biometric subtype records
	 */
	public IrisInfo(StandardBiometricHeader sbh,
			int captureDeviceId, int horizontalOrientation, int verticalOrientation,
			int scanType, int irisOcclusion, int occlusionFilling,
			int boundaryExtraction, int irisDiameter, int imageFormat,
			int rawImageWidth, int rawImageHeight, int intensityDepth, int imageTransformation,
			byte[] deviceUniqueId,
			List<IrisBiometricSubtypeInfo> irisBiometricSubtypeInfos) {
		this.sbh = sbh;
		if (irisBiometricSubtypeInfos == null) { throw new IllegalArgumentException("Null irisBiometricSubtypeInfos"); }
		this.captureDeviceId = captureDeviceId;
		this.horizontalOrientation = horizontalOrientation;
		this.verticalOrientation = verticalOrientation;
		this.scanType = scanType;
		this.irisOcclusion = irisOcclusion;
		this.occlusionFilling = occlusionFilling;
		this.boundaryExtraction = boundaryExtraction;
		this.irisDiameter = irisDiameter;
		this.imageFormat = imageFormat;
		this.rawImageWidth = rawImageWidth;
		this.rawImageHeight = rawImageHeight;
		this.intensityDepth = intensityDepth;
		this.imageTransformation = imageTransformation;
		long headerLength = 45;
		long dataLength = 0;
		for (IrisBiometricSubtypeInfo irisBiometricSubtypeInfo: irisBiometricSubtypeInfos) {
			dataLength += irisBiometricSubtypeInfo.getRecordLength();
			add(irisBiometricSubtypeInfo);
		}
		if (deviceUniqueId == null || deviceUniqueId.length != 16) { throw new IllegalArgumentException("deviceUniqueId invalid"); }
		this.deviceUniqueId = new byte[16];
		System.arraycopy(deviceUniqueId, 0, this.deviceUniqueId, 0, deviceUniqueId.length);
		this.recordLength = headerLength + dataLength;
	}

	/**
	 * Constructs an iris info from binary encoding.
	 * 
	 * @param inputStream an input stream
	 * 
	 * @throws IOException if reading fails
	 */
	public IrisInfo(InputStream inputStream) throws IOException {
		this(null, inputStream);
	}

	/**
	 * Constructs an iris info from binary encoding.
	 * 
	 * @param sbh standard biometric header to use
	 * @param inputStream an input stream
	 * 
	 * @throws IOException if reading fails
	 */
	public IrisInfo(StandardBiometricHeader sbh, InputStream inputStream) throws IOException {
		this.sbh = sbh;
		readObject(inputStream);
	}

	/**
	 * Reads this iris info from input stream.
	 * 
	 * @param inputStream an input stream
	 * 
	 * @throws IOException if reading fails
	 */
	public void readObject(InputStream inputStream) throws IOException {

		/* Iris Record Header (45) */

		DataInputStream dataIn = inputStream instanceof DataInputStream ? (DataInputStream)inputStream : new DataInputStream(inputStream);

		int iir0 = dataIn.readInt(); /* format id (e.g. "IIR" 0x00) */				/* 4 */
		if (iir0 != FORMAT_IDENTIFIER) { throw new IllegalArgumentException("'IIR' marker expected! Found " + Integer.toHexString(iir0)); }

		int version = dataIn.readInt(); /* version (e.g. "010" 0x00) */				/* + 4 = 8 */
		if (version != VERSION_NUMBER) { throw new IllegalArgumentException("'010' version number expected! Found " + Integer.toHexString(version)); }

		this.recordLength = dataIn.readInt(); /* & 0x00000000FFFFFFFFL */			/* + 4 = 12 */
		long headerLength = 45;
		long dataLength = this.recordLength - headerLength;

		captureDeviceId = dataIn.readUnsignedShort();								/* + 2 = 14 */
		int count = dataIn.readUnsignedByte();							/* + 1 = 15 */

		int recordHeaderLength = dataIn.readUnsignedShort(); /* Should be 45. */	/* + 2 = 17 */
		if (recordHeaderLength != headerLength) { throw new IllegalArgumentException("Expected header length " + headerLength + ", found " + recordHeaderLength); }

		/*
		 *  16 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1
		 * [  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  ]
		 *                                             1  1  = 0x0003 horizontalOrientation (>> 0)
		 *                                       1  1  0  0  = 0x000C verticalOrientation (>> 2)
		 *                              1  1  1  0  0  0  0  = 0x0070 scanType (>> 4)
		 *                           1  0  0  0  0  0  0  0  = 0x0080 irisOcclusion (>> 7)
		 *                        1  0  0  0  0  0  0  0  0  = 0x0100 occlusionFilling (>> 8)
		 *                     1  0  0  0  0  0  0  0  0  0  = 0x0200 boundaryExtraction (>> 9)
		 */
		int imagePropertiesBits = dataIn.readUnsignedShort(); 						/* + 2 = 19 */
		horizontalOrientation = imagePropertiesBits & 0x0003;
		verticalOrientation = (imagePropertiesBits & 0x00C) >> 2;
		scanType = (imagePropertiesBits & 0x0070) >> 4;
		irisOcclusion = (imagePropertiesBits & 0x0080) >> 7;
		occlusionFilling = (imagePropertiesBits & 0x0100) >> 8;
		boundaryExtraction = (imagePropertiesBits & 0x0200) >> 9;

		irisDiameter = dataIn.readUnsignedShort();									/* + 2 = 21 */
		imageFormat = dataIn.readUnsignedShort();									/* + 2 = 23 */
		rawImageWidth = dataIn.readUnsignedShort();									/* + 2 = 25 */
		rawImageHeight = dataIn.readUnsignedShort();								/* + 2 = 27 */
		intensityDepth = dataIn.readUnsignedByte();									/* + 1 = 28*/
		imageTransformation =  dataIn.readUnsignedByte();							/* + 1 = 29 */

		/*
		 * A 16 character string uniquely identifying the
		 * device or source of the data. This data can be
		 * one of:
		 * Device Serial number, identified by the first character "D"
		 * Host PC Mac address, identified by the first character "M"
		 * Host PC processor ID, identified by the first character "P"
		 * No serial number, identified by all zeros
		 */
		deviceUniqueId = new byte[16];												/* + 16 = 45 */
		dataIn.readFully(deviceUniqueId);

		long constructedDataLength = 0L;

		/* A record contains biometric subtype (or: 'feature') blocks (which contain image data blocks)... */
		for (int i = 0; i < count; i++) {
			IrisBiometricSubtypeInfo biometricSubtypeInfo = new IrisBiometricSubtypeInfo(inputStream, imageFormat);
			constructedDataLength += biometricSubtypeInfo.getRecordLength();
			add(biometricSubtypeInfo);
		}
		if (dataLength != constructedDataLength) {
			LOGGER.warning("DEBUG: constructedDataLength and dataLength differ: " + "dataLength = " + dataLength + ", constructedDataLength = " + constructedDataLength);
//			throw new IllegalStateException("DEBUG: constructed DataLength and dataLength differ: " + "dataLength = " + dataLength + ", constructedDataLength = " + constructedDataLength);
		}
	}

	/**
	 * Writes this iris info to an output stream.
	 * 
	 * @param outputStream an output stream
	 * 
	 * @throws IOException if writing fails
	 */
	public void writeObject(OutputStream outputStream) throws IOException {

		int headerLength = 45;

		int dataLength = 0;
		List<IrisBiometricSubtypeInfo> biometricSubtypeInfos = getSubRecords();
		for (IrisBiometricSubtypeInfo biometricSubtypeInfo: biometricSubtypeInfos) {
			dataLength += biometricSubtypeInfo.getRecordLength();
		}

		int recordLength = headerLength + dataLength;

		/* Iris Record Header (45) */

		DataOutputStream dataOut = outputStream instanceof DataOutputStream ? (DataOutputStream)outputStream : new DataOutputStream(outputStream);

		dataOut.writeInt(FORMAT_IDENTIFIER); /* header (e.g. "IIR", 0x00) */		/* 4 */
		dataOut.writeInt(VERSION_NUMBER); /* version in ASCII (e.g. "010" 0x00) */	/* +4 = 8 */

		dataOut.writeInt(recordLength); /* NOTE: bytes 9-12, i.e. 4 bytes, despite "unsigned long" in ISO/IEC FCD 19749-6. */ /* +4 = 12 */

		dataOut.writeShort(captureDeviceId);										/* +2 = 14 */

		dataOut.writeByte(biometricSubtypeInfos.size());									/* +1 = 15 */
		dataOut.writeShort(headerLength);											/* +2 = 17 */

		int imagePropertiesBits = 0;
		imagePropertiesBits |= (horizontalOrientation & 0x0003);
		imagePropertiesBits |= ((verticalOrientation << 2) & 0x00C);
		imagePropertiesBits |= ((scanType << 4)& 0x0070);
		imagePropertiesBits |= ((irisOcclusion << 7) & 0x0080);
		imagePropertiesBits |= ((occlusionFilling << 8) & 0x0100);
		imagePropertiesBits |= ((boundaryExtraction << 9) & 0x0200);
		dataOut.writeShort(imagePropertiesBits);									/* +2 = 19 */

		dataOut.writeShort(irisDiameter);											/* +2 = 21 */
		dataOut.writeShort(imageFormat);											/* +2 = 23 */
		dataOut.writeShort(rawImageWidth);											/* +2 = 25 */
		dataOut.writeShort(rawImageHeight);											/* +2 = 27 */
		dataOut.writeByte(intensityDepth);											/* +1 = 28 */
		dataOut.writeByte(imageTransformation);										/* +1 = 29 */
		dataOut.write(deviceUniqueId); /* array of length 16 */						/* + 16 = 45 */

		for (IrisBiometricSubtypeInfo biometricSubtypeInfo: biometricSubtypeInfos) {
			biometricSubtypeInfo.writeObject(outputStream);
		}
	}

	/**
	 * Gets the capture device id.
	 * 
	 * @return the captureDeviceId
	 */
	public int getCaptureDeviceId() {
		return captureDeviceId;
	}

	/**
	 * Gets the horizontal orientation
	 * 
	 * @return the horizontalOrientation, one of {@link #ORIENTATION_UNDEF}, {@link #ORIENTATION_BASE}, or {@link #ORIENTATION_FLIPPED}
	 */
	public int getHorizontalOrientation() {
		return horizontalOrientation;
	}

	/**
	 * Gets the vertical orientation
	 * 
	 * @return the verticalOrientation, one of {@link #ORIENTATION_UNDEF}, {@link #ORIENTATION_BASE}, or {@link #ORIENTATION_FLIPPED}
	 */
	public int getVerticalOrientation() {
		return verticalOrientation;
	}

	/**
	 * Gets the scan type.
	 * 
	 * @return the scanType, one of {@link #SCAN_TYPE_UNDEF}, {@link #SCAN_TYPE_PROGRESSIVE}, {@link #SCAN_TYPE_INTERLACE_FRAME}, {@link #SCAN_TYPE_INTERLACE_FIELD}, or {@link #SCAN_TYPE_CORRECTED}
	 */
	public int getScanType() {
		return scanType;
	}

	/**
	 * Gets the iris occlusion.
	 * 
	 * @return the irisOcclusion
	 */
	public int getIrisOcclusion() {
		return irisOcclusion;
	}

	/**
	 * Gets the iris occlusing filling.
	 * 
	 * @return the occlusionFilling
	 */
	public int getOcclusionFilling() {
		return occlusionFilling;
	}

	/**
	 * Gets the boundary extraction.
	 * 
	 * @return the boundaryExtraction
	 */
	public int getBoundaryExtraction() {
		return boundaryExtraction;
	}

	/**
	 * Gets the iris diameter.
	 * 
	 * @return the irisDiameter
	 */
	public int getIrisDiameter() {
		return irisDiameter;
	}

	/**
	 * Gets the image format.
	 * 
	 * @return the imageFormat
	 */
	public int getImageFormat() {
		return imageFormat;
	}

	/**
	 * Gets the raw image width.
	 * 
	 * @return the rawImageWidth
	 */
	public int getRawImageWidth() {
		return rawImageWidth;
	}

	/**
	 * Gets the raw image height.
	 * 
	 * @return the rawImageHeight
	 */
	public int getRawImageHeight() {
		return rawImageHeight;
	}

	/**
	 * Gets the intensity depth.
	 * 
	 * @return the intensityDepth
	 */
	public int getIntensityDepth() {
		return intensityDepth;
	}

	/**
	 * Gets the image transformation.
	 * 
	 * @return the imageTransformation
	 */
	public int getImageTransformation() {
		return imageTransformation;
	}

	/**
	 * Gets the device unique id.
	 * 
	 * @return the deviceUniqueId
	 */
	public byte[] getDeviceUniqueId() {
		return deviceUniqueId;
	}

	/**
	 * Gets the standard biometric header of this iris info.
	 * 
	 * @return the standard biometric header
	 */
	public StandardBiometricHeader getStandardBiometricHeader() {
		if (sbh == null) {
			byte[] biometricType = { (byte)CBEFFInfo.BIOMETRIC_TYPE_FINGERPRINT };
			byte[] biometricSubtype = { (byte)getBiometricSubtype() };
			byte[] formatOwner = { (byte)((FORMAT_OWNER_VALUE & 0xFF00) >> 8), (byte)(FORMAT_OWNER_VALUE & 0xFF) };
			byte[] formatType = { (byte)((FORMAT_TYPE_VALUE & 0xFF00) >> 8), (byte)(FORMAT_TYPE_VALUE & 0xFF) };

			SortedMap<Integer, byte[]> elements = new TreeMap<Integer, byte[]>();
			elements.put(ISO781611.BIOMETRIC_TYPE_TAG, biometricType);
			elements.put(ISO781611.BIOMETRIC_SUBTYPE_TAG, biometricSubtype);
			elements.put(ISO781611.FORMAT_OWNER_TAG, formatOwner);
			elements.put(ISO781611.FORMAT_TYPE_TAG, formatType);

			sbh = new StandardBiometricHeader(elements);
		}
		return sbh;
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
		result.append("IrisInfo [");
		/* TODO: contents */
		result.append("]");
		return result.toString();
	}

	/**
	 * Gets the iris biometric subtype infos embedded in this iris info.
	 * 
	 * @return iris biometric subtype infos
	 */
	public List<IrisBiometricSubtypeInfo> getIrisBiometricSubtypeInfos() { return getSubRecords(); }
	
	/**
	 * Adds an iris biometric subtype info to this iris info.
	 * 
	 * @param irisBiometricSubtypeInfo
	 */
	public void addIrisBiometricSubtypeInfo(IrisBiometricSubtypeInfo irisBiometricSubtypeInfo) { add(irisBiometricSubtypeInfo); }

	/**
	 * Removes an iris biometric subtype info from this iris info.
	 * 
	 * @param index the index of the biometric subtype info to remove
	 */
	public void removeIrisBiometricSubtypeInfo(int index) { remove(index); }

	/* ONLY PRIVATE METHODS BELOW */
	
	private int getBiometricSubtype() {
		int result = CBEFFInfo.BIOMETRIC_SUBTYPE_NONE;
		List<IrisBiometricSubtypeInfo> irisBiometricSubtypeInfos = getSubRecords();
		for (IrisBiometricSubtypeInfo irisBiometricSubtypeInfo: irisBiometricSubtypeInfos) {
			result &= irisBiometricSubtypeInfo.getBiometricSubtype();
		}
		return result;
	}
}
