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
import java.io.InputStream;

import sos.smartcards.BERTLVObject;

/**
 * File structure for the EF_DG1 file.
 * Datagroup 1 contains the the Machine
 * Readable Zone information.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class DG1File extends DataGroup
{
   private MRZInfo mrz;

   /**
    * Constructs a new file.
    * 
    * @param mrz the MRZ information to store in this file
    */
   public DG1File(MRZInfo mrz) {
      this.mrz = mrz;
   }
   
   DG1File(BERTLVObject in) {
      this(MRZInfo.getInstance(in.getSubObject(0x5F1F).getValueAsBytes()));
   }
   
   DG1File(InputStream in) throws IOException {
      this(BERTLVObject.getInstance(in));
   }
   
   DG1File(byte[] in) throws IOException {
      this(new ByteArrayInputStream(in));
   }

   /**
    * Gets the MRZ information stored in this file.
    * 
    * @return the MRZ information
    */
   public MRZInfo getMRZInfo() {
      return mrz;
   }

   public byte[] getEncoded() {
      try {
         BERTLVObject ef0101 =
            new BERTLVObject(EF_DG1_TAG,
               new BERTLVObject(0x5f1f, mrz.getEncoded()));
         return ef0101.getEncoded();
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }
}