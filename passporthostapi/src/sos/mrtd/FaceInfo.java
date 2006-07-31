/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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

package sos.mrtd;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Data structure for storing face information as found in DG2.
 * Coding is based on ISO/IEC FCD 19794-5 (2004-03-22).
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class FaceInfo
{
   /** Gender code based on Section 5.5.3 of ISO 19794-5. */
   public static final int GENDER_UNSPECIFIED = 0x00,
                           GENDER_MALE = 0x01,
                           GENDER_FEMALE = 0x02,
                           GENDER_UNKNOWN = 0x03;
   
   /** Eye color code based on Section 5.5.4 of ISO 19794-5. */
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
   private static final int FEATURE_FEATURES_ARE_SPECIFIED_FLAG = 0x00,
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
   public static final int EXPRESSION_UNSPECIFIED = 0x0000,
                           EXPRESSION_NEUTRAL = 0x0001,
                           EXPRESSION_SMILE_CLOSED = 0x0002,
                           EXPRESSION_SMILE_OPEN = 0x0003,
                           EXPRESSION_RAISED_EYEBROWS = 0x0004,
                           EXPRESSION_EYES_LOOKING_AWAY = 0x0005,
                           EXPRESSION_SQUINTING = 0x0006,
                           EXPRESSION_FROWNING = 0x0007;

   /** Face image type code based on Section 5.7.1 of ISO 19794-5. */
   public static final int FACE_IMAGE_TYPE_UNSPECIFIED = 0x00,
                           FACE_IMAGE_TYPE_BASIC = 0x01,
                           FACE_IMAGE_TYPE_FULL_FRONTAL = 0x02,
                           FACE_IMAGE_TYPE_TOKEN_FRONTAL = 0x03,
                           FACE_IMAGE_TYPE_OTHER = 0x04;
   
   /** Image data type code based on Section 5.7.2 of ISO 19794-5. */
   private static final int IMAGE_DATA_TYPE_JPEG = 0x00,
                            IMAGE_DATA_TYPE_JPEG2000 = 0x01;
   
   /** Color space code based on Section 5.7.4 of ISO 19794-5. */
   private static final int COLOR_SPACE_UNSPECIFIED = 0x00,
                            COLOR_SPACE_RGB24 = 0x01,
                            COLOR_SPACE_YUV422 = 0x02,
                            COLOR_SPACE_GRAY8 = 0x03,
                            COLOR_SPACE_OTHER = 0x04;
   
   /** Source type based on Sectin 5.7.6 of ISO 19794-5. */
   public static final int SOURCE_TYPE_UNSPECIFIED = 0x00,
                           SOURCE_TYPE_STATIC_PHOTO_UNKNOWN_SOURCE = 0x01,
                           SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM = 0x02,
                           SOURCE_TYPE_STATIC_PHOTO_SCANNER = 0x03,
                           SOURCE_TYPE_VIDEO_FRAME_UNKNOWN_SOURCE = 0x04,
                           SOURCE_TYPE_VIDEO_FRAME_ANALOG_CAM = 0x05,
                           SOURCE_TYPE_VIDEO_FRAME_DIGITAL_CAM = 0x06,
                           SOURCE_TYPE_UNKNOWN = 0x07;
   
   private long faceImageBlockLength;
   private int gender;
   private int eyeColor;
   private int hairColor;
   private long featureMask;
   private short expression;
   private int poseYawAngle,
      poseYawAngleUncertainty,
      posePitchAngle,
      posePitchAngleUncertainty,
      poseRollAngle,
      poseRollAngleUncertainty;
   
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
   
   /**
    * Constructs a new face information structure.
    * 
    * @param in input stream
    * 
    * @throws IOException if input cannot be read
    */
   FaceInfo(InputStream in) throws IOException {
      DataInputStream dataIn = new DataInputStream(in);
      
      /* Facial Information (20) */
      faceImageBlockLength = dataIn.readInt() & 0x00000000FFFFFFFFL;
      int featurePointCount = dataIn.readUnsignedShort();
      gender = dataIn.readUnsignedByte();
      eyeColor = dataIn.readUnsignedByte();
      hairColor = dataIn.readUnsignedByte();
      featureMask = dataIn.readUnsignedByte();
      featureMask = (featureMask << 16) | dataIn.readUnsignedShort();
      expression = dataIn.readShort();
      int by = dataIn.readUnsignedByte();
      poseYawAngle = 2 * ((by <= 91) ? (by - 1) : (by - 181));
      int bp = dataIn.readUnsignedByte();
      posePitchAngle = 2 * ((bp <= 91) ? (bp - 1) : (bp - 181));
      int br = dataIn.readUnsignedByte();
      poseRollAngle = 2 * ((br <= 91) ? (br - 1) : (br - 181));
      poseYawAngleUncertainty = dataIn.readUnsignedByte();
      posePitchAngleUncertainty = dataIn.readUnsignedByte();
      poseRollAngleUncertainty = dataIn.readUnsignedByte();
      
      /* Feature Point(s) (optional) (8 * featurePointCount) */
      featurePoints = new FeaturePoint[featurePointCount];
      for (int i = 0; i < featurePointCount; i++) {
         int featureType = dataIn.readUnsignedByte();
         byte featurePoint = dataIn.readByte();
         int x = dataIn.readUnsignedShort();
         int y = dataIn.readUnsignedShort();
         dataIn.skip(2); // 2 bytes reserved
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
         System.out.println("WARNING: FaceInfo: width = " + width);
         width = 800;
      }
      if (height <= 0) {
         System.out.println("WARNING: FaceInfo: height = " + height);
         height = 600;
      }

      /* Read JPEG2000 data */
      switch (imageDataType) {
      case IMAGE_DATA_TYPE_JPEG: image = readImage(dataIn, "image/jpeg"); break;
      case IMAGE_DATA_TYPE_JPEG2000: image = readImage(dataIn, "image/jpeg2000"); break;
      default: throw new IOException("Unknown image data type!");
      }
      
      /* Set width and height for real. */
      width = image.getWidth();
      height = image.getHeight();
   }
   
   private BufferedImage readImage(InputStream in, String mimeType) throws IOException {
      ImageInputStream iis = ImageIO.createImageInputStream(in);
      Iterator readers = ImageIO.getImageReadersByMIMEType(mimeType);
      if (!readers.hasNext()) {
         throw new IOException("No \"" + mimeType + "\" readers");
      }
      ImageReader reader = (ImageReader)readers.next();
      reader.setInput(iis);
      ImageReadParam pm = reader.getDefaultReadParam();

      pm.setSourceRegion(new Rectangle(0, 0, width, height));
      BufferedImage image = reader.read(0, pm);
      return image;
   }

   /**
    * Gets the image.
    * 
    * @return image
    */
   public BufferedImage getImage() {
      return image;
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
      out.append("Gender: "); out.append(genderToString()); out.append("\n");
      out.append("Eye color: "); out.append(eyeColorToString()); out.append("\n");
      out.append("Hair color: "); out.append(hairColorToString()); out.append("\n");
      out.append("Feature mask: "); out.append(featureMaskToString()); out.append("\n");
      out.append("Expression: "); out.append(expressionToString()); out.append("\n");
      out.append("Pose angle: "); out.append(poseAngleToString()); out.append("\n");
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
      out.append("Face image type: "); out.append(faceImageTypeToString()); out.append("\n");
      out.append("Source type: "); out.append(sourceTypeToString()); out.append("\n");
      return out.toString();
   }
   
   private String genderToString() {
      switch(gender) {
         case GENDER_UNSPECIFIED: return "unspecified";
         case GENDER_MALE: return "male";
         case GENDER_FEMALE: return "female";
      }
      return "unknown";
   }
   
   private String eyeColorToString() {
      switch(eyeColor) {
      case EYE_COLOR_UNSPECIFIED: return "unspecified";
      case EYE_COLOR_BLACK: return "black";
      case EYE_COLOR_BLUE: return "blue";
      case EYE_COLOR_BROWN: return "brown";
      case EYE_COLOR_GRAY: return "gray";
      case EYE_COLOR_GREEN: return "green";
      case EYE_COLOR_MULTI_COLORED: return "multi-colored";
      case EYE_COLOR_PINK: return "pink";
      }
      return "unknown";
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
      if ((featureMask & FEATURE_FEATURES_ARE_SPECIFIED_FLAG) == 0) {
         return "";
      }
      Collection features = new ArrayList();
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
      StringBuffer out = new StringBuffer();
      for (Iterator it = features.iterator(); it.hasNext(); ) {
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
      out.append("y: "); out.append(poseYawAngle);
      if (poseYawAngleUncertainty != 0) {
         out.append(" ("); out.append(poseYawAngleUncertainty); out.append(")");
      }
      out.append(", ");
      out.append("p:"); out.append(posePitchAngle);
      if (posePitchAngleUncertainty != 0) {
         out.append(" ("); out.append(posePitchAngleUncertainty); out.append(")");
      }
      out.append(", ");
      out.append("r: "); out.append(poseRollAngle);
      if (poseRollAngleUncertainty != 0) {
         out.append(" ("); out.append(poseRollAngleUncertainty); out.append(")");
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
      switch(sourceType) {
      case SOURCE_TYPE_UNSPECIFIED: return "unspecified";
      case SOURCE_TYPE_STATIC_PHOTO_UNKNOWN_SOURCE: return "static photograph from an unknown source";
      case SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM: return "static photograph from a digital still-image camera";
      case SOURCE_TYPE_STATIC_PHOTO_SCANNER: return "static photograph fram a scanner";
      case SOURCE_TYPE_VIDEO_FRAME_UNKNOWN_SOURCE: return "single video frame from an unknown source";
      case SOURCE_TYPE_VIDEO_FRAME_ANALOG_CAM: return "single video frame from an analogue camera";
      case SOURCE_TYPE_VIDEO_FRAME_DIGITAL_CAM: return "single video frame from a digital camera";
      }
      return "unknown";
   }

   /**
    * Feature points as described in Section 5.6.3 of ISO/IEC FCD 19794-5.
    * 
    * @author Martijn Oostdijk (martijno@cs.ru.nl)
    * 
    * @version $Revision: $
    */
   public class FeaturePoint
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

   /**
    * Gets the expression.
    * 
    * @return expression
    */
   public short getExpression() {
      return expression;
   }

   /**
    * Gets the eye color.
    * 
    * @return eye color
    */
   public int getEyeColor() {
      return eyeColor;
   }

   /**
    * Gets the gender.
    * 
    * @return gender
    */
   public int getGender() {
      return gender;
   }

   /**
    * Gets the hair color.
    * 
    * @return hair color
    */
   public int getHairColor() {
      return hairColor;
   }

   /**
    * Gets the face image type.
    * 
    * @return face image type
    */
   public int getFaceImageType() {
      return faceImageType;
   }

   /**
    * Gets the quality.
    * 
    * @return quality
    */
   public int getQuality() {
      return quality;
   }

   /**
    * Gets the source type (camera, scanner, etc).
    * 
    * @return source type
    */
   public int getSourceType() {
      return sourceType;
   }
}
