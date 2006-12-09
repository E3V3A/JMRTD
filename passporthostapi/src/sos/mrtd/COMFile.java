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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import sos.smartcards.BERTLVObject;

/**
 * File structure for the EF_COM file.
 * This file contains the common data (version and
 * data group presence table) information.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class COMFile extends PassportFile
{
   private String versionLDS;
   private String updateLevelLDS;
   private String majorVersionUnicode;
   private String minorVersionUnicode;
   private String releaseLevelUnicode;
   private byte[] tagList;

   /**
    * Constructs a new file.
    * 
    * @param versionLDS a numerical string of length 2
    * @param updateLevelLDS a numerical string of length 2
    * @param majorVersionUnicode a numerical string of length 2
    * @param minorVersionUnicode a numerical string of length 2
    * @param releaseLevelUnicode a numerical string of length 2
    * @param tagList a list of ICAO datagroup tags
    * 
    * @throws IllegalArgumentException if the input is not well-formed
    */
   public COMFile(String versionLDS, String updateLevelLDS,
         String majorVersionUnicode, String minorVersionUnicode,
         String releaseLevelUnicode, byte[] tagList) {
      if (versionLDS == null || versionLDS.length() != 2
            || updateLevelLDS == null || updateLevelLDS.length() != 2
            || majorVersionUnicode == null || majorVersionUnicode.length() != 2
            || minorVersionUnicode == null || minorVersionUnicode.length() != 2
            || releaseLevelUnicode == null || releaseLevelUnicode.length() != 2
            || tagList == null) {
         throw new IllegalArgumentException();
      }
      this.versionLDS = versionLDS;
      this.updateLevelLDS = updateLevelLDS;
      this.majorVersionUnicode = majorVersionUnicode;
      this.minorVersionUnicode = minorVersionUnicode;
      this.releaseLevelUnicode = releaseLevelUnicode;
      this.tagList = new byte[tagList.length];
      System.arraycopy(tagList, 0, this.tagList, 0, tagList.length);
   }

   /**
    * Constructs a new EF_COM file based on the encoded
    * value in <code>in</code>.
    * 
    * @param in should contain a TLV object with appropriate
    *           tag and contents
    * 
    * @throws IOException if the input could not be decoded
    */
   COMFile(ByteArrayInputStream in) throws IOException {
      BERTLVObject object = BERTLVObject.getInstance(in);
      if (object.getTag() != PassportASN1Service.EF_COM_TAG) {
         throw new IOException("Wrong tag!");
      }
      BERTLVObject versionLDSObject = object.getSubObject(0x5F01);
      BERTLVObject versionUnicodeObject = object.getSubObject(0x5F36);
      BERTLVObject tagListObject = object.getSubObject(0x5C);
      byte[] versionLDSBytes = versionLDSObject.getValueAsBytes();
      if (versionLDSBytes.length != 4) {
         throw new IOException("Wrong length of LDS version object");
      }
      versionLDS = new String(versionLDSBytes, 0, 2);
      updateLevelLDS = new String(versionLDSBytes, 2, 2);
      byte[] versionUnicodeBytes = versionUnicodeObject.getValueAsBytes();
      if (versionLDSBytes.length != 6) {
         throw new IOException("Wrong length of unicode version object");
      }
      majorVersionUnicode = new String(versionUnicodeBytes, 0, 2);
      minorVersionUnicode = new String(versionUnicodeBytes, 2, 2);
      releaseLevelUnicode = new String(versionUnicodeBytes, 4, 2);
      tagList = tagListObject.getValueAsBytes();
   }

   /**
    * Gets the LDS version as a dot seperated string
    * containing version and update level.
    * 
    * @return a string of the form "aa.bb"
    */
   public String getLDSVersion() {
      return versionLDS + "." + updateLevelLDS;
   }
   
   /**
    * Gets the unicode version as a dot seperated string
    * containing major version, minor version, and release level.
    * 
    * @return a string of the form "aa.bb.cc"
    */
   public String getUnicodeVersion() {
      return majorVersionUnicode
      + "." + minorVersionUnicode
      + "." + releaseLevelUnicode;
   }

   /**
    * Gets the ICAO datagroup tags as a list of bytes.
    * 
    * @return a list of bytes
    */
   public byte[] getTagList() {
      return tagList;
   }
   
   public byte[] getEncoded() {
      try {
         byte[] versionLDSBytes = (versionLDS + updateLevelLDS).getBytes();
         BERTLVObject versionLDSObject = new BERTLVObject(0x5F01, versionLDSBytes);
         byte[] versionUnicodeBytes =
            (majorVersionUnicode + minorVersionUnicode + releaseLevelUnicode).getBytes();
         BERTLVObject versionUnicodeObject = new BERTLVObject(0x5F36, versionUnicodeBytes);
         BERTLVObject tagListObject = new BERTLVObject(0x5C, tagList);
         BERTLVObject[] value = { versionLDSObject, versionUnicodeObject, tagListObject };
         BERTLVObject ef011E =
            new BERTLVObject(PassportASN1Service.EF_COM_TAG, value);
         return ef011E.getEncoded();
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }
}
