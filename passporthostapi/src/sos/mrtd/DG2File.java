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
 * $Id: $
 */

package sos.mrtd;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import sos.smartcards.BERTLVObject;

/**
 * File structure for the EF_DG2 file.
 * Datagroup 2 contains the facial features of
 * the document holder.
 * See A 13.3 in MRTD's LDS document.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class DG2File extends PassportFile
{
   private static final short BIOMETRIC_INFO_GROUP_TAG = 0x7F61;
   private static final short BIOMETRIC_INFO_TAG = 0x7F60;
   private static byte BIOMETRIC_INFO_COUNT_TAG = 0x02;
   private static byte BIOMETRIC_HEADER_BASE_TAG = (byte) 0xA1;
   private static short BIOMETRIC_DATA_TAG = 0x5F2E;

   private static byte FORMAT_OWNER_TAG = (byte) 0x87;
   private static byte FORMAT_TYPE_TAG = (byte) 0x88;

   // Facial Record Header, Sect. 5.4, ISO SC37
   private static byte[] FORMAT_IDENTIFIER = { 'F', 'A', 'C', 0x00 };
   private static byte[] VERSION_NUMBER = { '0', '1', '0', 0x00 };

   private List<FaceInfo> faces;

   /**
    * Constructs a new file.
    */
   public DG2File() {
      faces = new ArrayList<FaceInfo>();
   }

   // TODO: not tested...
   DG2File(InputStream in) {
      try {
         BERTLVObject object = BERTLVObject.getInstance(in);
         byte[] facialRecordData = object.getSubObject(0x5F2E).getValueAsBytes();
         if (facialRecordData == null) {
            throw new IllegalArgumentException("Could not decode facial record");
         }
         DataInputStream dataIn =
            new DataInputStream(new ByteArrayInputStream(facialRecordData));
         /* Facial Record Header (14) */
         dataIn.skip(4); // 'F', 'A', 'C', 0
         dataIn.skip(4); // version in ascii (e.g. "010")
         long length = dataIn.readInt() & 0x000000FFFFFFFFL;
         int faceCount = dataIn.readUnsignedShort();
         for (int i = 0; i < faceCount; i++) {
            addFaceInfo(new FaceInfo(dataIn));
         }
      } catch (Exception e) {
         throw new IllegalArgumentException("Could not decode: " + e.toString());
      }
   }

   public void addFaceInfo(FaceInfo fi) {
      faces.add(fi);
   }

   private byte[] formatOwner(Image i) {
      // FIXME
      byte[] ownr = { 0x01, 0x01 };
      return ownr;
   }

   private byte[] formatType(Image i) {
      // FIXME
      byte[] fmt = { 0x00, 0x08 };
      return fmt;
   }

   public byte[] getEncoded() {
      try {
         BERTLVObject group = new BERTLVObject(BIOMETRIC_INFO_GROUP_TAG,
               new BERTLVObject(BIOMETRIC_INFO_COUNT_TAG,
                     (byte) faces.size()));
         BERTLVObject dg2 = new BERTLVObject(EF_DG2_TAG, group);
         for (FaceInfo info: faces) {
            BERTLVObject header = new BERTLVObject(BIOMETRIC_HEADER_BASE_TAG++,
                  new BERTLVObject(FORMAT_TYPE_TAG,
                        formatType(info.getImage())));
            header.addSubObject(new BERTLVObject(FORMAT_OWNER_TAG,
                  formatOwner(info.getImage())));

            BERTLVObject face = new BERTLVObject(BIOMETRIC_INFO_TAG, header);
            group.addSubObject(face);
            // NOTE: multiple FaceInfos may be embedded in two ways:
            // 1) as multiple images in the same record (see Fig. 3 in ISO/IEC
            // 19794-5)
            // 2) as multiple records (see A 13.3 in LDS technical report).
            // We choose option 2, because otherwise we have to precalc the
            // total length of all FaceInfos, which sucks.
            byte[] faceInfoBytes = info.getEncoded();
            int lengthOfRecord = FORMAT_IDENTIFIER.length
            + VERSION_NUMBER.length + 4 + 2;
            short nrOfImagesInRecord = 1;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.write(FORMAT_IDENTIFIER);
            dataOut.write(VERSION_NUMBER);
            dataOut.writeInt(lengthOfRecord);
            dataOut.writeShort(nrOfImagesInRecord);
            dataOut.write(faceInfoBytes);
            dataOut.flush();
            byte[] facialRecord = out.toByteArray();

            face.addSubObject(new BERTLVObject(BIOMETRIC_DATA_TAG, facialRecord));
         }

         return dg2.getEncoded();
      } catch (IOException ioe) {
         return null;
      }
   }

   public int getFaceCount() {
      return faces.size();
   }
   
   public Iterator<FaceInfo> getFaces() {
      return faces.listIterator();
   }

   /**
    * Main method for testing.
    * 
    * @param arg command line arguments
    */
   public static void main(String[] arg) {
      try {
         BufferedImage image = null;
         DG2File dg2 = new DG2File();
         for (String fileName: arg) {
            try {
               image = ImageIO.read(new File(fileName));
            } catch (IOException e) {
               e.printStackTrace();
            }
            FaceInfo info = new FaceInfo(MRZInfo.Gender.MALE,
                  FaceInfo.EyeColor.BLUE,
                  FaceInfo.HAIR_COLOR_BLACK,
                  FaceInfo.EXPRESSION_FROWNING,
                  FaceInfo.SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM,
                  image);

            dg2.addFaceInfo(info);

         }
         System.out.println(BERTLVObject.getInstance(new ByteArrayInputStream(dg2.getEncoded())));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
