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
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Data structure for storing face information as found in DG2.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class FaceInfo
{   
   private long faceImageBlockLength;
   private int gender;
   private int eyeColor;
   private int hairColor;
   private long featureMask;
   private short expression;
   private long poseAngle;
   private long poseAngleUncertainty;
   
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
      poseAngle = dataIn.readUnsignedByte();
      poseAngle = (poseAngle << 16) | dataIn.readUnsignedShort();
      poseAngleUncertainty = dataIn.readUnsignedByte();
      poseAngleUncertainty = (poseAngleUncertainty << 16) | dataIn.readUnsignedShort();
      
      /* Feature Point(s) (optional) (8 * featurePointCount) */
      featurePoints = new FeaturePoint[featurePointCount];
      for (int i = 0; i < featurePointCount; i++) {
         int featureType = dataIn.readUnsignedByte();
         int featurePoint = dataIn.readUnsignedByte();
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

      /* Read JPEG2000 data */ // TODO: check if it's really jpeg2000 first...
      image = readJPEG2000(dataIn);
      
      width = image.getWidth();
      height = image.getHeight();
   }
   
   private BufferedImage readJPEG2000(InputStream in) throws IOException {
      ImageInputStream iis = ImageIO.createImageInputStream(in);
      Iterator readers = ImageIO.getImageReadersByMIMEType("image/jpeg2000");
      if (!readers.hasNext()) {
         throw new IllegalStateException("No jpeg 2000 readers");
      }
      ImageReader reader = (ImageReader)readers.next();
      reader.setInput(iis);
      ImageReadParam pm = reader.getDefaultReadParam();

      pm.setSourceRegion(new Rectangle(0, 0, width, height));
      BufferedImage image = reader.read(0, pm);
      return image;
   }

   public BufferedImage getImage() {
      return image;
   }
   
   public FeaturePoint[] getFeaturePoints() {
      return featurePoints;
   }
   
   public String toString() {
      StringBuffer out = new StringBuffer();
      out.append("Image size: "); out.append(width + " x " + height); out.append("\n");
      out.append("Gender: "); out.append(gender); out.append("\n");
      out.append("Eye color: "); out.append(eyeColor); out.append("\n");
      out.append("Hair color: "); out.append(hairColor); out.append("\n");
      out.append("Feature mask: "); out.append(Long.toHexString(featureMask)); out.append("\n");
      out.append("Expression: "); out.append(expression); out.append("\n");
      out.append("Pose angle: "); out.append(poseAngle); out.append("\n");
      out.append("Pose angle uncertainty: "); out.append(poseAngleUncertainty); out.append("\n");
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
      out.append("Face image type: "); out.append(faceImageType); out.append("\n");
      out.append("Image data type: "); out.append(imageDataType); out.append("\n");
      out.append("Image color space: "); out.append(imageColorSpace); out.append("\n");
      out.append("Source type: "); out.append(sourceType); out.append("\n");
      out.append("Device type: "); out.append(deviceType); out.append("\n");
      out.append("Quality: "); out.append(quality); out.append("\n");
      return out.toString();
   }
   
   public class FeaturePoint
   {
      private int type;
      private int code;
      private int x;
      private int y;
      
      public FeaturePoint(int type, int code, int x, int y) {
         this.type = type;
         this.code = code;
         this.x = x;
         this.y = y;
      }

      public int getMajorCode() {
         return (int)((code & 0xF0) >> 4);
      }
      
      public int getMinorCode() {
         return (int)(code & 0x0F);
      }

      public int getType() {
         return type;
      }

      public int getX() {
         return x;
      }

      public int getY() {
         return y;
      }
      
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
