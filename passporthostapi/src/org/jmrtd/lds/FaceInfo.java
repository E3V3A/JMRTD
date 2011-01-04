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
 * $Id$
 */

package org.jmrtd.lds;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import net.sourceforge.scuba.data.Gender;

import org.jmrtd.ImageReadUpdateListener;

/**
 * Data structure for storing face information as found in DG2.
 * Coding is based on ISO/IEC FCD 19794-5 (2004-03-22).
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision$
 */
public class FaceInfo extends DisplayedImageInfo
{
   /* Gender code based on Section 5.5.3 of ISO 19794-5: See sos.data.Gender. */

    /** Eye color code based on Section 5.5.4 of ISO 19794-5. */   
   public enum EyeColor {
       UNSPECIFIED { int toInt() { return EYE_COLOR_UNSPECIFIED;} }, 
       BLACK { int toInt() { return EYE_COLOR_BLACK; } }, 
       BLUE { int toInt() { return EYE_COLOR_BLUE; } },
       BROWN{ int toInt() { return EYE_COLOR_BROWN; } },
       GRAY{ int toInt() { return EYE_COLOR_GRAY; } },
       GREEN { int toInt() { return EYE_COLOR_GREEN; } },
       MULTI_COLORED { int toInt() { return EYE_COLOR_MULTI_COLORED; } },
       PINK { int toInt() { return EYE_COLOR_PINK; } },
       UNKNOWN { int toInt() { return EYE_COLOR_UNKNOWN; } };
       
       abstract int toInt();
       
       static EyeColor toEyeColor(int i) {
           for(EyeColor c: EyeColor.values()) {
               if(c.toInt() == i) {
                   return c;
               }
           }
           return null;
       }
   };
   
   public static final int EYE_COLOR_UNSPECIFIED = 0x00,
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
   public static final int HAIR_COLOR_UNSPECIFIED = 0x00,
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
   public enum Features { FEATURES_ARE_SPECIFIED, GLASSES, MOUSTACHE, BEARD, TEETH_VISIBLE, 
       BLINK, MOUTH_OPEN, LEFT_EYE_PATCH, RIGHT_EYE_PATCH, DARK_GLASSES, DISTORTING_MEDICAL_CONDITION };
   private static final int FEATURE_FEATURES_ARE_SPECIFIED_FLAG = 0x000001,
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
   public enum Expression { UNSPECIFIED, NEUTRAL, SMILE_CLOSED, SMILE_OPEN, RAISED_EYEBROWS, 
       EYES_LOOKING_AWAY, SQUINTING, FROWNING }; 
   public static final short EXPRESSION_UNSPECIFIED = 0x0000,
                           EXPRESSION_NEUTRAL = 0x0001,
                           EXPRESSION_SMILE_CLOSED = 0x0002,
                           EXPRESSION_SMILE_OPEN = 0x0003,
                           EXPRESSION_RAISED_EYEBROWS = 0x0004,
                           EXPRESSION_EYES_LOOKING_AWAY = 0x0005,
                           EXPRESSION_SQUINTING = 0x0006,
                           EXPRESSION_FROWNING = 0x0007;

   /** Face image type code based on Section 5.7.1 of ISO 19794-5. */
   public enum FaceImageType { UNSPECIFIED, BASIC, FULL_FRONTAL, TOKEN_FRONTAL, OTHER }; 
   public static final int FACE_IMAGE_TYPE_UNSPECIFIED = 0x00,
                           FACE_IMAGE_TYPE_BASIC = 0x01,
                           FACE_IMAGE_TYPE_FULL_FRONTAL = 0x02,
                           FACE_IMAGE_TYPE_TOKEN_FRONTAL = 0x03,
                           FACE_IMAGE_TYPE_OTHER = 0x04;
   
   /** Image data type code based on Section 5.7.2 of ISO 19794-5. */
   public enum ImageData { TYPE_JPEG, TYPE_JPEG2000 };
   private static final int IMAGE_DATA_TYPE_JPEG = 0x00,
                            IMAGE_DATA_TYPE_JPEG2000 = 0x01;
   
   /** Color space code based on Section 5.7.4 of ISO 19794-5. */
   public enum ImageColorSpace { UNSPECIFIED, RGB24, YUV422, GRAY8, OTHER }; 
   public static final int IMAGE_COLOR_SPACE_UNSPECIFIED = 0x00,
                           IMAGE_COLOR_SPACE_RGB24 = 0x01,
                           IMAGE_COLOR_SPACE_YUV422 = 0x02,
                           IMAGE_COLOR_SPACE_GRAY8 = 0x03,
                           IMAGE_COLOR_SPACE_OTHER = 0x04;
   
   /** Source type based on Section 5.7.6 of ISO 19794-5. */
   public enum SourceType { UNSPECIFIED, STATIC_PHOTO_UNKNOWN_SOURCE, STATIC_PHOTO_DIGITAL_CAM,
       STATIC_PHOTO_SCANNER, VIDEO_FRAME_UNKNOWN_SOURCE, VIDEO_FRAME_ANALOG_CAM, VIDEO_FRAME_DIGITAL_CAM,
       UNKNOWN };

   public static final int SOURCE_TYPE_UNSPECIFIED = 0x00,
                           SOURCE_TYPE_STATIC_PHOTO_UNKNOWN_SOURCE = 0x01,
                           SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM = 0x02,
                           SOURCE_TYPE_STATIC_PHOTO_SCANNER = 0x03,
                           SOURCE_TYPE_VIDEO_FRAME_UNKNOWN_SOURCE = 0x04,
                           SOURCE_TYPE_VIDEO_FRAME_ANALOG_CAM = 0x05,
                           SOURCE_TYPE_VIDEO_FRAME_DIGITAL_CAM = 0x06,
                           SOURCE_TYPE_UNKNOWN = 0x07;
   
   /** Indexes into poseAngle array. */
   private static final int YAW = 0, PITCH = 1, ROLL = 2;

   private long faceImageBlockLength;
   private Gender gender;
   private EyeColor eyeColor;
   private int hairColor;
   private long featureMask;
   private short expression;
   private int[] poseAngle;
   private int[] poseAngleUncertainty;
   private FeaturePoint[] featurePoints;
   private int faceImageType;
   private int imageDataType;
   private int width;
   private int height;
   private int imageColorSpace;
   private int sourceType;
   private int deviceType;
   private int quality;
   private BufferedImage image;
   private Collection<ImageReadUpdateListener> imageReadUpdateListeners;

   private DataInputStream dataIn;

   private FaceInfo() {
	   super(TYPE_PORTRAIT);
	   imageReadUpdateListeners = new ArrayList<ImageReadUpdateListener>();
   }

   /**
    * Constructs a new face information data structure instance.
    * 
    * @param gender gender
    * @param eyeColor eye color
    * @param hairColor hair color
    * @param expression expression
    * @param sourceType source type
    * @param image image
    */
   public FaceInfo(Gender gender, EyeColor eyeColor, int hairColor, short expression,
		   int sourceType, BufferedImage image) {
	   this();
	   this.faceImageBlockLength = 0L;
	   this.gender = gender;
	   this.eyeColor = eyeColor;
	   this.hairColor = hairColor;
	   this.expression = expression;
	   this.sourceType = sourceType;
	   this.deviceType = 0;
	   this.poseAngle = new int[3];
	   this.poseAngleUncertainty = new int[3];
	   this.image = image;
	   this.width = image.getWidth();
	   this.height = image.getHeight();
	   this.featurePoints = new FeaturePoint[0];
	   this.imageDataType = IMAGE_DATA_TYPE_JPEG;
   }

   /**
    * Constructs a new face information structure.
    * 
    * @param in input stream
    * 
    * @throws IOException if input cannot be read
    */
   FaceInfo(InputStream in) throws IOException {
	   this();
	   dataIn = (in instanceof DataInputStream) ? (DataInputStream)in : new DataInputStream(in);

	   /* Facial Information (20) */
	   faceImageBlockLength = dataIn.readInt() & 0xFFFFFFFFL;
	   int featurePointCount = dataIn.readUnsignedShort();
	   gender = Gender.getInstance(dataIn.readUnsignedByte());
	   eyeColor = EyeColor.toEyeColor(dataIn.readUnsignedByte());
	   hairColor = dataIn.readUnsignedByte();
	   featureMask = dataIn.readUnsignedByte();
	   featureMask = (featureMask << 16) | dataIn.readUnsignedShort();
	   expression = dataIn.readShort();
	   poseAngle = new int[3];
	   int by = dataIn.readUnsignedByte();
	   poseAngle[YAW] = 2 * ((by <= 91) ? (by - 1) : (by - 181));
	   int bp = dataIn.readUnsignedByte();
	   poseAngle[PITCH] = 2 * ((bp <= 91) ? (bp - 1) : (bp - 181));
	   int br = dataIn.readUnsignedByte();
	   poseAngle[ROLL] = 2 * ((br <= 91) ? (br - 1) : (br - 181));
	   poseAngleUncertainty = new int[3];
	   poseAngleUncertainty[YAW] = dataIn.readUnsignedByte();
	   poseAngleUncertainty[PITCH] = dataIn.readUnsignedByte();
	   poseAngleUncertainty[ROLL] = dataIn.readUnsignedByte();

	   /* Feature Point(s) (optional) (8 * featurePointCount) */
	   featurePoints = new FeaturePoint[featurePointCount];
	   for (int i = 0; i < featurePointCount; i++) {
		   int featureType = dataIn.readUnsignedByte();
		   byte featurePoint = dataIn.readByte();
		   int x = dataIn.readUnsignedShort();
		   int y = dataIn.readUnsignedShort();
		   long skippedBytes = 0;
		   while (skippedBytes < 2) { skippedBytes += dataIn.skip(2); } // 2 bytes reserved
		   featurePoints[i] = new FeaturePoint(featureType, featurePoint, x, y);
	   }

	   /* Image Information */
	   faceImageType = dataIn.readUnsignedByte();
	   imageDataType = dataIn.readUnsignedByte();
	   width = dataIn.readUnsignedShort();
	   height = dataIn.readUnsignedShort();
	   imageColorSpace = dataIn.readUnsignedByte();
	   sourceType = dataIn.readUnsignedByte();
	   deviceType = dataIn.readUnsignedShort();
	   quality = dataIn.readUnsignedShort();

	   /* Temporarily fix width and height if 0. */
	   if (width <= 0) {
		   width = 800;
	   }
	   if (height <= 0) {
		   height = 600;
	   }

	   /*
	    * Read image data, image data type code based on Section 5.8.1
	    * ISO 19794-5
	    */
	   image = null;
   }

   public byte[] getEncoded() {
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         DataOutputStream dataOut = new DataOutputStream(out);

         /* Facial Information (20) */
         // dataOut.writeInt((int)faceImageBlockLength);
         dataOut.writeShort(featurePoints.length);
         dataOut.writeByte(gender.toInt());
         dataOut.writeByte(eyeColor.toInt());
         dataOut.writeByte(hairColor);
         dataOut.writeByte((byte)((featureMask & 0xFF0000L) >> 16));
         dataOut.writeByte((byte)((featureMask & 0x00FF00L) >> 8));
         dataOut.writeByte((byte)(featureMask & 0x0000FFL));
         dataOut.writeShort(expression);
         for (int i = 0; i < 3; i++) {
            int b = (0 <= poseAngle[i] && poseAngle[i] <= 180) ?
                  poseAngle[i] / 2 + 1 : 181 + poseAngle[i] / 2;
                  dataOut.writeByte(b);
         }
         for (int i = 0; i < 3; i++) {
            dataOut.writeByte(poseAngleUncertainty[i]);
         }

         /* Feature Point(s) (optional) (8 * featurePointCount) */
         for (int i = 0; i < featurePoints.length; i++) {
            FeaturePoint fp = featurePoints[i];
            dataOut.writeByte(fp.getType());
            dataOut.writeByte((fp.getMajorCode() << 4) | fp.getMinorCode());
            dataOut.writeShort(fp.getX());
            dataOut.writeShort(fp.getY());
            dataOut.writeShort(0x00); // 2 bytes reserved
         }

         /* Image Information */
         dataOut.writeByte(faceImageType);
         dataOut.writeByte(imageDataType);
         dataOut.writeShort(width);
         dataOut.writeShort(height);
         dataOut.writeByte(imageColorSpace);
         dataOut.writeByte(sourceType);
         dataOut.writeShort(deviceType);
         dataOut.writeShort(quality);

         /*
          * Read image data, image data type code based on Section 5.8.1
          * ISO 19794-5
          */
         if (image == null) { getImage(); }
         switch (imageDataType) {
         case IMAGE_DATA_TYPE_JPEG:
            writeImage(image, dataOut, "image/jpeg");
            break;
         case IMAGE_DATA_TYPE_JPEG2000:
            writeImage(image, dataOut, "image/jpeg2000");
            break;
         default:
            throw new IOException("Unknown image data type!");
         }

         dataOut.flush();
         byte[] facialRecordData = out.toByteArray();
         dataOut.close();

         faceImageBlockLength = facialRecordData.length;

         out = new ByteArrayOutputStream();
         dataOut = new DataOutputStream(out);
         dataOut.writeInt((int)faceImageBlockLength);
         dataOut.write(facialRecordData);
         dataOut.flush();
         facialRecordData = out.toByteArray();
         dataOut.close();

         return facialRecordData;
      } catch (IOException ioe) {
         ioe.printStackTrace();
         return null;
      }
   }

   private BufferedImage processImage(InputStream in, String mimeType, boolean isProgressiveMode) {
	   Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mimeType);
	   while (readers.hasNext()) {
		   try {
			   ImageReader reader = (ImageReader)readers.next();
			   BufferedImage image = processImage(in, reader, isProgressiveMode);
			   if (image != null) { return image; }
		   } catch (Exception e) {
			   /* NOTE: this reader doesn't work? Try next one... */
			   continue;
		   }
	   }
	   /* Tried all readers */
	   throw new IllegalArgumentException("Could not decode \"" + mimeType + "\" image!");
   }

   private BufferedImage processImage(InputStream in, ImageReader reader, boolean isProgressiveMode) {
	   ImageReadParam pm = reader.getDefaultReadParam();
	   pm.setSourceRegion(new Rectangle(0, 0, width, height));

	   if (!isProgressiveMode) {
		   try {
			   ImageInputStream iis = ImageIO.createImageInputStream(in);
			   reader.setInput(iis);
			   BufferedImage resultImage = reader.read(0, pm);
			   image = resultImage;
			   return resultImage;
		   } catch (IOException ioe) {
			   ioe.printStackTrace();
			   return null;
		   }
	   }

	   /* Approximations... */
	   int totalLength = (int)(faceImageBlockLength & 0xFFFFFFFF);
	   int leadSize = 977;
	   int stepSize = 19; // initialy
	   double base = 1.56;
	   byte[] buf = new byte[totalLength];
	   int offset = 0;
	   try {
		   int bytesRead = in.read(buf, offset, Math.min(leadSize, buf.length - offset));
		   offset += bytesRead;
	   } catch (IOException ioe) {
		   ioe.printStackTrace();
	   }
	   BufferedImage resultImage = null;
	   while (offset < totalLength) {
		   try {
			   int bytesRead = in.read(buf, offset, Math.min(stepSize, buf.length - offset));
			   if (bytesRead < 0) { break; }
			   ByteArrayInputStream approxIn = new ByteArrayInputStream(buf, 0, offset + bytesRead);
			   ImageInputStream iis = ImageIO.createImageInputStream(approxIn);
			   reader.setInput(iis);
			   offset += bytesRead;
			   stepSize = (int)(base * (double)stepSize);
		   } catch (IOException ioe) {
			   /* NOTE: ignoring expected exception in image decoding... */
		   }
		   try {
			   resultImage = reader.read(0, pm);
		   } catch (Throwable e) {
			   /* NOTE: ignoring expected exception in image decoding... */
		   }
		   if (resultImage != null) {
			   image = resultImage;
			   notifyImageReadUpdateListeners(resultImage, offset / (double)totalLength);
		   }
	   }	   
	   return resultImage;
   }

   /**
    * Writes image to output stream.
    *
    * @param image
    * @param out
    * @param mimeType
    * @throws IOException
    */
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
			   pm.setSourceRegion(new Rectangle(0, 0, width, height));
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

   /**
    * Gets the image.
    * 
    * @return image
    */
   public BufferedImage getImage(boolean isProgressiveMode) {
	   BufferedImage resultImage = null;
	   if (image != null) { return image; }
	   switch (imageDataType) {
	   case IMAGE_DATA_TYPE_JPEG:
		   resultImage = processImage(dataIn, "image/jpeg", isProgressiveMode);
		   break;
	   case IMAGE_DATA_TYPE_JPEG2000:
		   resultImage = processImage(dataIn, "image/jpeg2000", isProgressiveMode);
		   break;
	   default:
		   throw new IllegalStateException("Unknown image data type!");
	   }

	   /* Set width and height for real. */
	   width = resultImage.getWidth();
	   height = resultImage.getHeight();

	   return resultImage;
   }
   
   public BufferedImage getImage() {
	   return getImage(false);
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
    * Generates a textual representation of this object.
    * 
    * @return a textual representation of this object
    * 
    * @see java.lang.Object#toString()
    */
   public String toString() {
	   StringBuffer out = new StringBuffer();
	   out.append("Image size: "); out.append(width + " x " + height); out.append("\n");
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
		   features
		   .add("distorting medical condition (which could impact feature point detection)");
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
	   case FACE_IMAGE_TYPE_UNSPECIFIED: return "unspecified (basic)";
	   case FACE_IMAGE_TYPE_BASIC: return "basic";
	   case FACE_IMAGE_TYPE_FULL_FRONTAL: return "full frontal";
	   case FACE_IMAGE_TYPE_TOKEN_FRONTAL: return "token frontal";
	   case FACE_IMAGE_TYPE_OTHER: return "other";
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

   /**
    * Gets the width of this face.
    * 
    * @return width
    */
   public int getWidth() {
	   return width;
   }

   /**
    * Gets the height of this face.
    * 
    * @return height;
    */
   public int getHeight() {
	   return height;
   }

   /**
    * Gets the expression
    * (neutral, smiling, eyebrow raised, etc).
    * 
    * @return expression
    */
   public short getExpression() {
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

   /**
    * Gets the image color space
    * (rgb, grayscale, etc).
    * 
    * @return image color space
    */
   public int getImageColorSpace() {
	   return imageColorSpace;
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
    * containing yaw, pitch, and roll angle in degrees.
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
    * containing yaw, pitch, and roll angle uncertainty in degrees.
    * 
    * @return an integer array of length 3
    */
   public int[] getPoseAngleUncertainty() {
	   int[] result = new int[3];
	   System.arraycopy(poseAngleUncertainty, 0, result, 0, result.length);
	   return result;
   }

   /**
    * Adds a listener which will be notified when new image data is available.
    *
    * @param l the listener
    */
   public void addImageReadUpdateListener(ImageReadUpdateListener l) {
	   imageReadUpdateListeners.add(l);
   }

   /**
    * Removes a listener.
    *
    * @param l the listener
    */
   public void removeImageReadUpdateListener(ImageReadUpdateListener l) {
	   imageReadUpdateListeners.remove(l);
   }

   private void notifyImageReadUpdateListeners(BufferedImage image, double percentage) {
	   for (ImageReadUpdateListener l: imageReadUpdateListeners) {
		   l.passComplete(image, percentage);
	   }
   }

   /**
    * Feature points as described in Section 5.6.3 of ISO/IEC FCD 19794-5.
    * 
    * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
    * 
    * @version $Revision$
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
