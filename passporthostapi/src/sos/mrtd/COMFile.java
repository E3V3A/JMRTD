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

import java.io.IOException;

import sos.smartcards.BERTLVObject;
import sos.util.Hex;

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
