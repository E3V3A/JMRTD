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
   private byte gender;
   private byte eyeColor;
   private byte hairColor;
   private long featureMask;
   private short expression;
   private long poseAngle;
   private long poseAngleUncertainty;
   
   private FeaturePoint[] featurePoints;
   
   private byte faceImageType;
   private byte imageDataType;
   private int width;
   private int height;
   private byte imageColorSpace;
   private int sourceType;
   private int deviceType;
   private int quality;
   
   private BufferedImage image;
   
   FaceInfo(InputStream in) throws IOException {
      DataInputStream dataIn = new DataInputStream(in);
      
      /* Facial Information (20) */
      faceImageBlockLength = dataIn.readInt() & 0x00000000FFFFFFFFL;
      int featurePointCount = dataIn.readUnsignedShort();
      gender = dataIn.readByte();
      eyeColor = dataIn.readByte();
      hairColor = dataIn.readByte();
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
         byte featureType = dataIn.readByte();
         byte featurePoint = dataIn.readByte();
         int x = dataIn.readUnsignedShort();
         int y = dataIn.readUnsignedShort();
         dataIn.skip(2); // 2 bytes reserved
         featurePoints[i] = new FeaturePoint(featureType, featurePoint, x, y);
      }
      
      /* Image Information */
      faceImageType = dataIn.readByte();
      imageDataType = dataIn.readByte();
      width = dataIn.readUnsignedShort();
      height = dataIn.readUnsignedShort();
      imageColorSpace = dataIn.readByte();
      sourceType = dataIn.readUnsignedByte();
      deviceType = dataIn.readUnsignedShort();
      quality = dataIn.readUnsignedShort();
      
      /* Read JPEG2000 data */
      ImageInputStream iis = ImageIO.createImageInputStream(dataIn);
      Iterator readers = ImageIO.getImageReadersByMIMEType("image/jpeg2000");
      if (!readers.hasNext()) {
         throw new IllegalStateException("No jpeg 2000 readers");
      }
      ImageReader reader = (ImageReader)readers.next();
      reader.setInput(iis);
      ImageReadParam pm = reader.getDefaultReadParam();
      pm.setSourceRegion(new Rectangle(0, 0, width, height));
      image = reader.read(0, pm);
   }

   public BufferedImage getImage() {
      return image;
   }
   
   public FeaturePoint[] getFeaturePoints() {
      return featurePoints;
   }
   
   public class FeaturePoint
   {
      private int featureType;
      private int featurePoint;
      private int x;
      private int y;
      
      public FeaturePoint(int featureType, int featurePoint, int x, int y) {
         this.featureType = featureType;
         this.featurePoint = featurePoint;
         this.x = x;
         this.y = y;
      }

      public int getFeaturePoint() {
         return featurePoint;
      }

      public int getFeatureType() {
         return featureType;
      }

      public int getX() {
         return x;
      }

      public int getY() {
         return y;
      } 
   }
}
