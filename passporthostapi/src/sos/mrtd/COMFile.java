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
   private int versionLDS;
   private int updateLevelLDS;
   private int majorVersionUnicode;
   private int minorVersionUnicode;
   private int releaseLevelUnicode;
   private int[] tagList;

   /**
    * Constructs a new file.
    * 
    * @param versionLDS
    * @param updateLevelLDS
    * @param majorVersionUnicode
    * @param minorVersionUnicode
    * @param releaseLevelUnicode
    * @param tagList
    */
   public COMFile(int versionLDS, int updateLevelLDS,
         int majorVersionUnicode, int minorVersionUnicode,
         int releaseLevelUnicode, int[] tagList) {
      this.versionLDS = versionLDS;
      this.updateLevelLDS = updateLevelLDS;
      this.majorVersionUnicode = majorVersionUnicode;
      this.minorVersionUnicode = minorVersionUnicode;
      this.releaseLevelUnicode = releaseLevelUnicode;
   }

   public byte[] getEncoded() {
      try {
         BERTLVObject ef011E =
            new BERTLVObject(PassportASN1Service.EF_COM_TAG, null); // TODO
         return ef011E.getEncoded();
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }
}
