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
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import sos.tlv.BERTLVInputStream;
import sos.tlv.BERTLVObject;

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
public class DG2File extends DataGroup
{
   private static final short BIOMETRIC_INFO_GROUP_TAG = 0x7F61;
   private static final short BIOMETRIC_INFO_TAG = 0x7F60;
   
   private static final byte BIOMETRIC_INFO_COUNT_TAG = 0x02;
   private static final byte BIOMETRIC_HEADER_BASE_TAG = (byte) 0xA1;
   private static final short BIOMETRIC_DATA_TAG = 0x5F2E;

   private static final byte FORMAT_OWNER_TAG = (byte) 0x87;
   private static final byte FORMAT_TYPE_TAG = (byte) 0x88;

   // Facial Record Header, Sect. 5.4, ISO SC37
   private static final byte[] FORMAT_IDENTIFIER = { 'F', 'A', 'C', 0x00 };
   private static final byte[] VERSION_NUMBER = { '0', '1', '0', 0x00 };

   private List<FaceInfo> faces;

   /**
    * Constructs a new file.
    */
   public DG2File() {
      faces = new ArrayList<FaceInfo>();
   }

   public DG2File(InputStream in) {
      this();
      try {
         BERTLVInputStream tlvIn = new BERTLVInputStream(in);
         int tlvLength = tlvIn.skipToTag(BIOMETRIC_DATA_TAG);
         DataInputStream dataIn =
            new DataInputStream(tlvIn);
         /* Facial Record Header (14) */
         dataIn.skip(4); // 'F', 'A', 'C', 0
         dataIn.skip(4); // version in ascii (e.g. "010")
         long length = dataIn.readInt() & 0x000000FFFFFFFFL;
         int faceCount = dataIn.readUnsignedShort();
         for (int i = 0; i < faceCount; i++) {
            addFaceInfo(new FaceInfo(dataIn));
         }
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalArgumentException("Could not decode: " + e.toString());
      }
      isSourceConsistent = true;
   }
   
   public int getTag() {
      return EF_DG2_TAG;
   }
   
   public void addFaceInfo(FaceInfo fi) {
      faces.add(fi);
      isSourceConsistent = false;
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
      if (isSourceConsistent) {
         return sourceObject.getEncoded();
      }
      try {
         BERTLVObject group = new BERTLVObject(BIOMETRIC_INFO_GROUP_TAG,
               new BERTLVObject(BIOMETRIC_INFO_COUNT_TAG,
                     (byte) faces.size()));
         BERTLVObject dg2 = new BERTLVObject(EF_DG2_TAG, group);
         byte bioHeaderTag = BIOMETRIC_HEADER_BASE_TAG;
         for (FaceInfo info: faces) {
            BERTLVObject header = new BERTLVObject(bioHeaderTag++,
                  new BERTLVObject(FORMAT_TYPE_TAG,
                        formatType(info.getImage())));
            header.addSubObject(new BERTLVObject(FORMAT_OWNER_TAG,
                  formatOwner(info.getImage())));

            BERTLVObject face = new BERTLVObject(BIOMETRIC_INFO_TAG, header);
            
            // NOTE: multiple FaceInfos may be embedded in two ways:
            // 1) as multiple images in the same record (see Fig. 3 in ISO/IEC
            // 19794-5)
            // 2) as multiple records (see A 13.3 in LDS technical report).
            // We choose option 2, because otherwise we have to precalc the
            // total length of all FaceInfos, which sucks.
            int lengthOfRecord =
               FORMAT_IDENTIFIER.length + VERSION_NUMBER.length + 4 + 2;
            short nrOfImagesInRecord = 1;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.write(FORMAT_IDENTIFIER);
            dataOut.write(VERSION_NUMBER);
            dataOut.writeInt(lengthOfRecord);
            dataOut.writeShort(nrOfImagesInRecord);
            dataOut.write(info.getEncoded());
            dataOut.flush();
            byte[] facialRecord = out.toByteArray();

            face.addSubObject(new BERTLVObject(BIOMETRIC_DATA_TAG, facialRecord));
            group.addSubObject(face);
         }
         sourceObject = dg2;
         isSourceConsistent = true;
         return dg2.getEncoded();
      } catch (Exception ioe) {
         return null;
      }
   }
   
   public List<FaceInfo> getFaces() {
      return faces;
   }
}
