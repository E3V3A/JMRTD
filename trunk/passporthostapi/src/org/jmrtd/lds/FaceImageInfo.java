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
 * $Id: FaceImageInfo.java -1M 2011-06-21 13:34:07Z (local) $
 */

package org.jmrtd.lds;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.sourceforge.scuba.data.Gender;

/**
 * Data structure for storing facial image data. This represents
 * a facial record data block as specified in Section 5.5, 5.6,
 * and 5.7 of ISO/IEC FCD 19794-5 (2004-03-22, AKA Annex D).
 *
 * A facial record data block contains a single facial image.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class FaceImageInfo extends AbstractImageInfo {

	private static final long serialVersionUID = -1751069410327594067L;
	
	/** Eye color code based on Section 5.5.4 of ISO 19794-5. */   
	public enum EyeColor {
		UNSPECIFIED { public int toInt() { return EYE_COLOR_UNSPECIFIED;} }, 
		BLACK { public int toInt() { return EYE_COLOR_BLACK; } }, 
		BLUE { public int toInt() { return EYE_COLOR_BLUE; } },
		BROWN{ public int toInt() { return EYE_COLOR_BROWN; } },
		GRAY{ public int toInt() { return EYE_COLOR_GRAY; } },
		GREEN { public int toInt() { return EYE_COLOR_GREEN; } },
		MULTI_COLORED { public int toInt() { return EYE_COLOR_MULTI_COLORED; } },
		PINK { public int toInt() { return EYE_COLOR_PINK; } },
		UNKNOWN { public int toInt() { return EYE_COLOR_UNKNOWN; } };

		public abstract int toInt();

		static EyeColor toEyeColor(int i) {
			for(EyeColor c: EyeColor.values()) {
				if(c.toInt() == i) {
					return c;
				}
			}
			return null;
		}
	};

	public static final int
	EYE_COLOR_UNSPECIFIED = 0x00,
	EYE_COLOR_BLACK = 0x01,
	EYE_COLOR_BLUE = 0x02,
	EYE_COLOR_BROWN = 0x03,
	EYE_COLOR_GRAY = 0x04,
	EYE_COLOR_GREEN = 0x05,
	EYE_COLOR_MULTI_COLORED = 0x06,
	EYE_COLOR_PINK = 0x07,
	EYE_COLOR_UNKNOWN = 0x08;

	/** Hair color code based on Section 5.5.5 of ISO 19794-5. */
	public enum HairColor { UNSPECIFIED, BALD, BLACK, BLONDE, BROWN, GRAY, WHITE, RED, GREEN, BLUE, UNKNOWN };
	public static final int
	HAIR_COLOR_UNSPECIFIED = 0x00,
	HAIR_COLOR_BALD = 0x01,
	HAIR_COLOR_BLACK = 0x02,
	HAIR_COLOR_BLONDE = 0x03,
	HAIR_COLOR_BROWN = 0x04,
	HAIR_COLOR_GRAY = 0x05,
	HAIR_COLOR_WHITE = 0x06,
	HAIR_COLOR_RED = 0x07,
	HAIR_COLOR_GREEN = 0x08,
	HAIR_COLOR_BLUE = 0x09,
	HAIR_COLOR_UNKNOWN = 0xFF;

	/** Feature flags meaning based on Section 5.5.6 of ISO 19794-5. */
	public enum Features { FEATURES_ARE_SPECIFIED, GLASSES, MOUSTACHE, BEARD, TEETH_VISIBLE, BLINK, MOUTH_OPEN, LEFT_EYE_PATCH, RIGHT_EYE_PATCH, DARK_GLASSES, DISTORTING_MEDICAL_CONDITION };
	private static final int
	FEATURE_FEATURES_ARE_SPECIFIED_FLAG = 0x000001,
	FEATURE_GLASSES_FLAG = 0x000002,
	FEATURE_MOUSTACHE_FLAG = 0x000004,
	FEATURE_BEARD_FLAG = 0x000008,
	FEATURE_TEETH_VISIBLE_FLAG = 0x000010,
	FEATURE_BLINK_FLAG = 0x000020,
	FEATURE_MOUTH_OPEN_FLAG = 0x000040,
	FEATURE_LEFT_EYE_PATCH_FLAG = 0x000080,
	FEATURE_RIGHT_EYE_PATCH = 0x000100,
	FEATURE_DARK_GLASSES = 0x000200,
	FEATURE_DISTORTING_MEDICAL_CONDITION = 0x000400;

	/** Expression code based on Section 5.5.7 of ISO 19794-5. */
	public enum Expression { UNSPECIFIED, NEUTRAL, SMILE_CLOSED, SMILE_OPEN, RAISED_EYEBROWS, EYES_LOOKING_AWAY, SQUINTING, FROWNING }; 
	public static final short
	EXPRESSION_UNSPECIFIED = 0x0000,
	EXPRESSION_NEUTRAL = 0x0001,
	EXPRESSION_SMILE_CLOSED = 0x0002,
	EXPRESSION_SMILE_OPEN = 0x0003,
	EXPRESSION_RAISED_EYEBROWS = 0x0004,
	EXPRESSION_EYES_LOOKING_AWAY = 0x0005,
	EXPRESSION_SQUINTING = 0x0006,
	EXPRESSION_FROWNING = 0x0007;

	/** Face image type code based on Section 5.7.1 of ISO 19794-5. */
	public enum FaceImageType { BASIC, FULL_FRONTAL, TOKEN_FRONTAL }; 
	public static final int
	FACE_IMAGE_TYPE_BASIC = 0x00,
	FACE_IMAGE_TYPE_FULL_FRONTAL = 0x01,
	FACE_IMAGE_TYPE_TOKEN_FRONTAL = 0x02;

	/** Image data type code based on Section 5.7.2 of ISO 19794-5. */
	public enum ImageDataType { TYPE_JPEG, TYPE_JPEG2000 };
	public static final int
	IMAGE_DATA_TYPE_JPEG = 0x00,
	IMAGE_DATA_TYPE_JPEG2000 = 0x01;

	/** Color space code based on Section 5.7.4 of ISO 19794-5. */
	public enum ImageColorSpace { UNSPECIFIED, RGB24, YUV422, GRAY8, OTHER }; 
	public static final int
	IMAGE_COLOR_SPACE_UNSPECIFIED = 0x00,
	IMAGE_COLOR_SPACE_RGB24 = 0x01,
	IMAGE_COLOR_SPACE_YUV422 = 0x02,
	IMAGE_COLOR_SPACE_GRAY8 = 0x03,
	IMAGE_COLOR_SPACE_OTHER = 0x04;

	/** Source type based on Section 5.7.6 of ISO 19794-5. */
	public enum SourceType { UNSPECIFIED, STATIC_PHOTO_UNKNOWN_SOURCE, STATIC_PHOTO_DIGITAL_CAM, STATIC_PHOTO_SCANNER, VIDEO_FRAME_UNKNOWN_SOURCE, VIDEO_FRAME_ANALOG_CAM, VIDEO_FRAME_DIGITAL_CAM, UNKNOWN };

	public static final int
	SOURCE_TYPE_UNSPECIFIED = 0x00,
	SOURCE_TYPE_STATIC_PHOTO_UNKNOWN_SOURCE = 0x01,
	SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM = 0x02,
	SOURCE_TYPE_STATIC_PHOTO_SCANNER = 0x03,
	SOURCE_TYPE_VIDEO_FRAME_UNKNOWN_SOURCE = 0x04,
	SOURCE_TYPE_VIDEO_FRAME_ANALOG_CAM = 0x05,
	SOURCE_TYPE_VIDEO_FRAME_DIGITAL_CAM = 0x06,
	SOURCE_TYPE_UNKNOWN = 0x07;

	/** Indexes into poseAngle array. */
	private static final int YAW = 0, PITCH = 1, ROLL = 2;
	
	private long recordLength;
	private Gender gender;
	private EyeColor eyeColor;
	private int hairColor;
	private int featureMask;
	private int expression;
	private int[] poseAngle;
	private int[] poseAngleUncertainty;
	private FeaturePoint[] featurePoints;
	private int faceImageType;
	private int imageDataType;
	private int colorSpace;
	private int sourceType;
	private int deviceType;
	private int quality;

	/**
	 * Constructs a new face information data structure instance.
	 * 
	 * @param gender gender
	 * @param eyeColor eye color
	 * @param hairColor hair color
	 * @param featureMask feature mask (least significant 3 bytes)
	 * @param expression expression
	 * @param poseAngle (encoded) pose angle
	 * @param poseAngleUncertainty pose angle uncertainty
	 * @param sourceType source type
	 * @param deviceType capture device type (unspecified is <code>0x00</code>)
	 * @param imageInputStream encoded image bytes
	 * @param imageLength length of encoded image
	 * @param imageDataType either IMAGE_DATA_TYPE_JPEG or IMAGE_DATA_TYPE_JPEG2000
	 */
	public FaceImageInfo(Gender gender, EyeColor eyeColor,
			int featureMask,
			int hairColor,
			int expression,
			int[] poseAngle, int[] poseAngleUncertainty,
			int faceImageType,
			int colorSpace,
			int sourceType,
			int deviceType,
			int quality,
			FeaturePoint[] featurePoints,
			int width, int height,
			InputStream imageInputStream, int imageLength, int imageDataType) throws IOException {
		super(TYPE_PORTRAIT, width, height, imageInputStream, imageLength, toMimeType(imageDataType));
		if (imageInputStream == null) { throw new IllegalArgumentException("Null image"); }
		this.gender = gender;
		this.eyeColor = eyeColor;
		this.featureMask = featureMask;
		this.hairColor = hairColor;
		this.expression = expression;
		this.colorSpace = colorSpace;
		this.sourceType = sourceType;
		this.deviceType = deviceType;
		int featurePointCount = featurePoints == null ? 0 : featurePoints.length;
		this.featurePoints = new FeaturePoint[featurePointCount];
		if (featurePointCount > 0) {
			System.arraycopy(featurePoints, 0, this.featurePoints, 0, featurePointCount);
		}
		this.poseAngle = new int[3];
		System.arraycopy(poseAngle, 0, this.poseAngle, 0, 3);
		this.poseAngleUncertainty = new int[3];
		System.arraycopy(poseAngleUncertainty, 0, this.poseAngleUncertainty, 0, 3);
		this.featurePoints = new FeaturePoint[featurePointCount];
		this.imageDataType = imageDataType;
		this.recordLength = 20 + 8 * featurePointCount + 12 + imageLength;
	}

	/**
	 * Constructs a new face information structure from binary encoding.
	 * 
	 * @param inputStream an input stream
	 * 
	 * @throws IOException if input cannot be read
	 */
	public FaceImageInfo(InputStream inputStream) throws IOException {
		super(TYPE_PORTRAIT);
		readObject(inputStream);
	}

	protected void readObject(InputStream inputStream) throws IOException {
		DataInputStream dataIn = (inputStream instanceof DataInputStream) ? (DataInputStream)inputStream : new DataInputStream(inputStream);

		/* Facial Information Block (20), see ISO 19794-5 5.5 */
		recordLength = dataIn.readInt() & 0xFFFFFFFFL; /* 4 */
		int featurePointCount = dataIn.readUnsignedShort(); /* +2 = 6 */
		gender = Gender.getInstance(dataIn.readUnsignedByte()); /* +1 = 7 */
		eyeColor = EyeColor.toEyeColor(dataIn.readUnsignedByte()); /* +1 = 8 */
		hairColor = dataIn.readUnsignedByte(); /* +1 = 9 */
		featureMask = dataIn.readUnsignedByte(); /* +1 = 10 */
		featureMask = (featureMask << 16) | dataIn.readUnsignedShort(); /* +2 = 12 */
		expression = dataIn.readShort(); /* +2 = 14 */
		poseAngle = new int[3];
		int by = dataIn.readUnsignedByte(); /* +1 = 15 */
		poseAngle[YAW] = by; // FIXME: used to be: 2 * ((by <= 91) ? (by - 1) : (by - 181));
		int bp = dataIn.readUnsignedByte(); /* +1 = 16 */
		poseAngle[PITCH] = bp; // FIXME: used to be: 2 * ((bp <= 91) ? (bp - 1) : (bp - 181));
		int br = dataIn.readUnsignedByte(); /* +1 = 17 */
		poseAngle[ROLL] = br; // FIXME: used to be: 2 * ((br <= 91) ? (br - 1) : (br - 181));
		poseAngleUncertainty = new int[3];
		poseAngleUncertainty[YAW] = dataIn.readUnsignedByte(); /* +1 = 18 */
		poseAngleUncertainty[PITCH] = dataIn.readUnsignedByte(); /* +1 = 19 */
		poseAngleUncertainty[ROLL] = dataIn.readUnsignedByte(); /* +1 = 20 */

		/* Feature Point(s) (optional) (8 * featurePointCount), see ISO 19794-5 5.8 */
		featurePoints = new FeaturePoint[featurePointCount];
		for (int i = 0; i < featurePointCount; i++) {
			int featureType = dataIn.readUnsignedByte(); /* 1 */
			byte featurePoint = dataIn.readByte(); /* +1 = 2 */
			int x = dataIn.readUnsignedShort(); /* +2 = 4 */
			int y = dataIn.readUnsignedShort(); /* +2 = 6 */
			long skippedBytes = 0;
			while (skippedBytes < 2) { skippedBytes += dataIn.skip(2); } /* +2 = 8, NOTE: 2 bytes reserved */
			featurePoints[i] = new FeaturePoint(featureType, featurePoint, x, y);
		}

		/* Image Information */
		faceImageType = dataIn.readUnsignedByte(); /* 1 */
		imageDataType = dataIn.readUnsignedByte(); /* +1 = 2 */
		setWidth(dataIn.readUnsignedShort()); /* +2 = 4 */
		setHeight(dataIn.readUnsignedShort()); /* +2 = 6 */
		colorSpace = dataIn.readUnsignedByte(); /* +1 = 7 */
		sourceType = dataIn.readUnsignedByte(); /* +1 = 8 */
		deviceType = dataIn.readUnsignedShort(); /* +2 = 10 */
		quality = dataIn.readUnsignedShort(); /* +2 = 12 */

		/* Temporarily fix width and height if 0. */
		if (getWidth() <= 0) {
			setWidth(800);
		}
		if (getHeight() <= 0) {
			setHeight(600);
		}

		/*
		 * Read image data, image data type code based on Section 5.8.1
		 * ISO 19794-5.
		 */
		setMimeType(toMimeType(imageDataType));
		long imageLength = recordLength - 20 - 8 * featurePointCount - 12;

		readImage(inputStream, imageLength);
	}

	/**
	 * Writes this face image info to output stream.
	 * 
	 * @param outputStream an output stream
	 * 
	 * @throws IOException if writing fails
	 */
	public void writeObject(OutputStream outputStream) throws IOException {
		ByteArrayOutputStream recordOut = new ByteArrayOutputStream();
		writeFacialRecordData(recordOut);
		byte[] facialRecordData = recordOut.toByteArray();
		long faceImageBlockLength = facialRecordData.length + 4;		
		DataOutputStream dataOut = new DataOutputStream(outputStream);
		dataOut.writeInt((int)faceImageBlockLength);
		dataOut.write(facialRecordData);
		dataOut.flush();
	}

	/**
	 * Gets the record length.
	 * 
	 * @return the record length
	 */
	public long getRecordLength() {
		/* Should be equal to (20 + 8 * featurePoints.length + 12 + getImageLength()). */
		return recordLength;
	}

	/**
	 * Gets the available feature points of this face.
	 * 
	 * @return feature points
	 */
	public FeaturePoint[] getFeaturePoints() {
		return featurePoints;
	}

	/**
	 * Gets the expression
	 * (neutral, smiling, eyebrow raised, etc).
	 * 
	 * @return expression
	 */
	public int getExpression() {
		return expression;
	}

	/**
	 * Gets the eye color
	 * (black, blue, brown, etc).
	 * 
	 * @return eye color
	 */
	public EyeColor getEyeColor() {
		return eyeColor;
	}

	/**
	 * Gets the gender
	 * (male, female, etc).
	 * 
	 * @return gender
	 */
	public Gender getGender() {
		return gender;
	}

	/**
	 * Gets the hair color
	 * (bald, black, blonde, etc).
	 * 
	 * @return hair color
	 */
	public int getHairColor() {
		return hairColor;
	}

	/**
	 * Gets the face image type
	 * (full frontal, token frontal, etc).
	 * 
	 * @return face image type
	 */
	public int getFaceImageType() {
		return faceImageType;
	}
	
	public int getFeatureMask() {
		return featureMask;
	}

	/**
	 * Gets the quality as unsigned integer.
	 * 
	 * @return quality
	 */
	public int getQuality() {
		return quality;
	}

	/**
	 * Gets the source type
	 * (camera, scanner, etc).
	 * 
	 * @return source type
	 */
	public int getSourceType() {
		return sourceType;
	}

	public int getImageDataType() {
		return imageDataType;
	}
	
	/**
	 * Gets the image color space
	 * (rgb, grayscale, etc).
	 * 
	 * @return image color space
	 */
	public int getColorSpace() {
		return colorSpace;
	}

	/**
	 * Gets the device type.
	 * 
	 * @return device type
	 */
	public int getDeviceType() {
		return deviceType;
	}

	/**
	 * Gets the pose angle as an integer array of length 3,
	 * containing yaw, pitch, and roll angle in encoded form.
	 * 
	 * @return an integer array of length 3
	 */
	public int[] getPoseAngle() {
		int[] result = new int[3];
		System.arraycopy(poseAngle, 0, result, 0, result.length);
		return result;
	}

	/**
	 * Gets the pose angle uncertainty as an integer array of length 3,
	 * containing yaw, pitch, and roll angle uncertainty.
	 * 
	 * @return an integer array of length 3
	 */
	public int[] getPoseAngleUncertainty() {
		int[] result = new int[3];
		System.arraycopy(poseAngleUncertainty, 0, result, 0, result.length);
		return result;
	}
	
	/**
	 * Generates a textual representation of this object.
	 * 
	 * @return a textual representation of this object
	 * 
	 * @see java.lang.Object#toString()
	 */
	/* TODO: rename this method, distinguish between a pretty print version to be used in JMRTD GUI and a proper toString() */
	public String toString() {
		StringBuffer out = new StringBuffer();
		out.append("Image size: "); out.append(getWidth() + " x " + getHeight()); out.append("\n");
		out.append("Gender: "); out.append(gender); out.append("\n");
		out.append("Eye color: "); out.append(eyeColor); out.append("\n");
		out.append("Hair color: "); out.append(hairColorToString()); out.append("\n");
		out.append("Feature mask: "); out.append(featureMaskToString()); out.append("\n");
		out.append("Expression: "); out.append(expressionToString()); out.append("\n");
		out.append("Pose angle: "); out.append(poseAngleToString()); out.append("\n");
		out.append("Face image type: "); out.append(faceImageTypeToString()); out.append("\n");
		out.append("Source type: "); out.append(sourceTypeToString()); out.append("\n");
		out.append("Feature points: "); out.append("\n");
		if (featurePoints == null || featurePoints.length == 0) {
			out.append("   (none)\n");
		} else {
			for (int i = 0; i < featurePoints.length; i++) {
				out.append("   ");
				out.append(featurePoints[i].toString());
				out.append("\n");
			}
		}
		return out.toString();
	}

	private void writeFacialRecordData(OutputStream outputStream) throws IOException {
		DataOutputStream dataOut = new DataOutputStream(outputStream);

		/* Facial Information (16) */
		dataOut.writeShort(featurePoints.length);						/* 2 */
		dataOut.writeByte(gender.toInt());								/* 1 */
		dataOut.writeByte(eyeColor.toInt());							/* 1 */
		dataOut.writeByte(hairColor);									/* 1 */
		dataOut.writeByte((byte)((featureMask & 0xFF0000L) >> 16));		/* 1 */
		dataOut.writeByte((byte)((featureMask & 0x00FF00L) >> 8));		/* 1 */
		dataOut.writeByte((byte)(featureMask & 0x0000FFL));				/* 1 */
		dataOut.writeShort(expression);									/* 2 */
		for (int i = 0; i < 3; i++) {									/* 3 */
			int b = poseAngle[i];
			//	FIXME: used to be:			(0 <= poseAngle[i] && poseAngle[i] <= 180) ? poseAngle[i] / 2 + 1 : 181 + poseAngle[i] / 2;
			dataOut.writeByte(b);
		}
		for (int i = 0; i < 3; i++) {									/* 3 */
			dataOut.writeByte(poseAngleUncertainty[i]);
		}

		/* Feature Point(s) (optional) (8 * featurePointCount) */
		for (int i = 0; i < featurePoints.length; i++) {
			FeaturePoint fp = featurePoints[i];
			dataOut.writeByte(fp.getType());
			dataOut.writeByte((fp.getMajorCode() << 4) | fp.getMinorCode());
			dataOut.writeShort(fp.getX());
			dataOut.writeShort(fp.getY());
			dataOut.writeShort(0x00); /* 2 bytes RFU */
		}

		/* Image Information (12) */
		dataOut.writeByte(faceImageType);		/* 1 */
		dataOut.writeByte(imageDataType);		/* 1 */
		dataOut.writeShort(getWidth());			/* 2 */
		dataOut.writeShort(getHeight());		/* 2 */
		dataOut.writeByte(colorSpace);		/* 1 */
		dataOut.writeByte(sourceType);			/* 1 */
		dataOut.writeShort(deviceType);			/* 2 */
		dataOut.writeShort(quality);			/* 2 */

		/*
		 * Image data type code based on Section 5.8.1
		 * ISO 19794-5
		 */		
		writeImage(dataOut);
		dataOut.flush();
		dataOut.close();
	}
	
	private String hairColorToString() {
		switch(hairColor) {
		case HAIR_COLOR_UNSPECIFIED: return "unspecified";
		case HAIR_COLOR_BALD: return "bald";
		case HAIR_COLOR_BLACK: return "black";
		case HAIR_COLOR_BLONDE: return "blonde";
		case HAIR_COLOR_BROWN: return "brown";
		case HAIR_COLOR_GRAY: return "gray";
		case HAIR_COLOR_WHITE: return "white";
		case HAIR_COLOR_RED: return "red";
		case HAIR_COLOR_GREEN: return "green";
		case HAIR_COLOR_BLUE: return "blue";
		}
		return "unknown";
	}

	private String featureMaskToString() {
		if ((featureMask & FEATURE_FEATURES_ARE_SPECIFIED_FLAG) == 0) { return ""; }
		Collection<String> features = new ArrayList<String>();
		if ((featureMask & FEATURE_GLASSES_FLAG) != 0) {
			features.add("glasses");
		}
		if ((featureMask & FEATURE_MOUSTACHE_FLAG) != 0) {
			features.add("moustache");
		}
		if ((featureMask & FEATURE_BEARD_FLAG) != 0) {
			features.add("beard");
		}
		if ((featureMask & FEATURE_TEETH_VISIBLE_FLAG) != 0) {
			features.add("teeth visible");
		}
		if ((featureMask & FEATURE_BLINK_FLAG) != 0) {
			features.add("blink");
		}
		if ((featureMask & FEATURE_MOUTH_OPEN_FLAG) != 0) {
			features.add("mouth open");
		}
		if ((featureMask & FEATURE_LEFT_EYE_PATCH_FLAG) != 0) {
			features.add("left eye patch");
		}
		if ((featureMask & FEATURE_RIGHT_EYE_PATCH) != 0) {
			features.add("right eye patch");
		}
		if ((featureMask & FEATURE_DARK_GLASSES) != 0) {
			features.add("dark glasses");
		}
		if ((featureMask & FEATURE_DISTORTING_MEDICAL_CONDITION) != 0) {
			features.add("distorting medical condition (which could impact feature point detection)");
		}
		StringBuffer out = new StringBuffer();
		for (Iterator<String> it = features.iterator(); it.hasNext();) {
			out.append(it.next().toString());
			if (it.hasNext()) {
				out.append(", ");
			}
		}
		return out.toString();
	}

	private String expressionToString() {
		switch (expression) {
		case EXPRESSION_UNSPECIFIED:
			return "unspecified";
		case EXPRESSION_NEUTRAL:
			return "neutral (non-smiling) with both eyes open and mouth closed";
		case EXPRESSION_SMILE_CLOSED:
			return "a smile where the inside of the mouth and/or teeth is not exposed (closed jaw)";
		case EXPRESSION_SMILE_OPEN:
			return "a smile where the inside of the mouth and/or teeth is exposed";
		case EXPRESSION_RAISED_EYEBROWS:
			return "raised eyebrows";
		case EXPRESSION_EYES_LOOKING_AWAY:
			return "eyes looking away from the camera";
		case EXPRESSION_SQUINTING:
			return "squinting";
		case EXPRESSION_FROWNING:
			return "frowning";
		}
		return "unknown";
	}

	private String poseAngleToString() {
		StringBuffer out = new StringBuffer();
		out.append("(");
		out.append("y: "); out.append(poseAngle[YAW]);
		if (poseAngleUncertainty[YAW] != 0) {
			out.append(" ("); out.append(poseAngleUncertainty[YAW]); out.append(")");
		}
		out.append(", ");
		out.append("p:"); out.append(poseAngle[PITCH]);
		if (poseAngleUncertainty[PITCH] != 0) {
			out.append(" ("); out.append(poseAngleUncertainty[PITCH]); out.append(")");
		}
		out.append(", ");
		out.append("r: "); out.append(poseAngle[ROLL]);
		if (poseAngleUncertainty[ROLL] != 0) {
			out.append(" ("); out.append(poseAngleUncertainty[ROLL]); out.append(")");
		}
		out.append(")");
		return out.toString();
	}

	private String faceImageTypeToString() {
		switch (faceImageType) {
		case FACE_IMAGE_TYPE_BASIC: return "basic";
		case FACE_IMAGE_TYPE_FULL_FRONTAL: return "full frontal";
		case FACE_IMAGE_TYPE_TOKEN_FRONTAL: return "token frontal";
		}
		return "unknown";
	}

	private String sourceTypeToString() {
		switch (sourceType) {
		case SOURCE_TYPE_UNSPECIFIED:
			return "unspecified";
		case SOURCE_TYPE_STATIC_PHOTO_UNKNOWN_SOURCE:
			return "static photograph from an unknown source";
		case SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM:
			return "static photograph from a digital still-image camera";
		case SOURCE_TYPE_STATIC_PHOTO_SCANNER:
			return "static photograph from a scanner";
		case SOURCE_TYPE_VIDEO_FRAME_UNKNOWN_SOURCE:
			return "single video frame from an unknown source";
		case SOURCE_TYPE_VIDEO_FRAME_ANALOG_CAM:
			return "single video frame from an analogue camera";
		case SOURCE_TYPE_VIDEO_FRAME_DIGITAL_CAM:
			return "single video frame from a digital camera";
		}
		return "unknown";
	}

	/*
   private String imageColorSpaceToString() {
      switch(imageColorSpace) {
      case IMAGE_COLOR_SPACE_UNSPECIFIED: return "unspecified";
      case IMAGE_COLOR_SPACE_RGB24: return "24 bit RGB";
      case IMAGE_COLOR_SPACE_YUV422: return "YUV422";
      case IMAGE_COLOR_SPACE_GRAY8: return "8 bit grayscale";
      case IMAGE_COLOR_SPACE_OTHER: return "other";
      }
      if (imageColorSpace >= 128) {
         return "unknown (vendor specific)";
      }
      return "unknown";
   }
	 */

	private static String toMimeType(int compressionAlg) {
		switch (compressionAlg) {
		case IMAGE_DATA_TYPE_JPEG: return JPEG_MIME_TYPE;
		case IMAGE_DATA_TYPE_JPEG2000: return JPEG2000_MIME_TYPE;
		}
		return null;
	}

	private static String toMimeType(ImageDataType imageDataType) {
		switch(imageDataType) {
		case TYPE_JPEG: return "image/jpeg";
		case TYPE_JPEG2000: return "image/jpeg2000"; /* FIXME; Check ietf rfc3745, shouldn't this be "image/jp2"? */
		}
		return null;
	}

	private static int fromMimeType(String mimeType) {
		if ("image/jpeg".equals(mimeType)) { return IMAGE_DATA_TYPE_JPEG; }
		if ("image/jpeg2000".equals(mimeType) || "image/jp2".equals(mimeType)) { return IMAGE_DATA_TYPE_JPEG2000; }
		throw new IllegalArgumentException("Did not recognize mimeType");
	}

	/**
	 * Feature points as described in Section 5.6.3 of ISO/IEC FCD 19794-5.
	 * 
	 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
	 * 
	 * @version $Revision: -1M $
	 */
	public static class FeaturePoint
	{
		private int type;
		private int majorCode;
		private int minorCode;
		private int x;
		private int y;

		/**
		 * Constructs a new feature point.
		 * 
		 * @param type feature point type
		 * @param majorCode major code
		 * @param minorCode minor code
		 * @param x X-coordinate
		 * @param y Y-coordinate
		 */
		public FeaturePoint(int type, int majorCode, int minorCode,
				int x, int y) {
			this.type = type;
			this.majorCode = majorCode;
			this.minorCode = minorCode;
			this.x = x;
			this.y = y;
		}

		/**
		 * Constructs a new feature point.
		 * 
		 * @param type feature point type
		 * @param code combined major and minor code
		 * @param x X-coordinate
		 * @param y Y-coordinate
		 */
		FeaturePoint(int type, byte code, int x, int y) {
			this(type, (int)((code & 0xF0) >> 4), (int)(code & 0x0F), x ,y);
		}

		/**
		 * Gets the major code of this point.
		 * 
		 * @return major code
		 */
		public int getMajorCode() {
			return majorCode;
		}

		/**
		 * Gets the minor code of this point.
		 * 
		 * @return minor code
		 */
		public int getMinorCode() {
			return minorCode;
		}

		/**
		 * Gets the type of this point.
		 * 
		 * @return type
		 */
		public int getType() {
			return type;
		}

		/**
		 * Gets the X-coordinate of this point.
		 * 
		 * @return X-coordinate
		 */
		public int getX() {
			return x;
		}

		/**
		 * Gets the Y-coordinate of this point.
		 * 
		 * @return Y-coordinate
		 */
		public int getY() {
			return y;
		}

		/**
		 * Generates a textual representation of this point.
		 * 
		 * @return a textual representation of this point
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer out = new StringBuffer();
			out.append("( point: "); out.append(getMajorCode()); out.append("."); out.append(getMinorCode());
			out.append(", ");
			out.append("type: "); out.append(Integer.toHexString(type)); out.append(", ");
			out.append("("); out.append(x); out.append(", ");
			out.append(y); out.append(")");
			out.append(")");
			return out.toString();
		}
	}
}
