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
import java.io.InputStream;

import sos.smartcards.BERTLVObject;

/**
 * Passport file structure.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public abstract class PassportFile
{
   /** ICAO specific datagroup tag. */
   public static final int EF_COM_TAG = 0x60,
                           EF_DG1_TAG = 0x61,
                           EF_DG2_TAG = 0x75,
                           EF_DG3_TAG = 0x63,
                           EF_DG4_TAG = 0x76,
                           EF_DG5_TAG = 0x65,
                           EF_DG6_TAG = 0x66,
                           EF_DG7_TAG = 0x67,
                           EF_DG8_TAG = 0x68,
                           EF_DG9_TAG = 0x69,
                           EF_DG10_TAG = 0x6A,
                           EF_DG11_TAG = 0x6B,
                           EF_DG12_TAG = 0x6C,
                           EF_DG13_TAG = 0x6D,
                           EF_DG14_TAG = 0x6E,
                           EF_DG15_TAG = 0x6F,
                           EF_DG16_TAG = 0x70,
                           EF_SOD_TAG = 0x77;
   
   /**
    * Constructor only visible to the other
    * classes in this package.
    */
   PassportFile() {
   }
   
   /**
    * Gets the passportfile encoded in <code>in</code>.
    * 
    * @param in the object we want converted
    * 
    * @return a passport file structure
    * 
    * @throws IllegalArgumentException if the input object cannot be converted
    */
   public static PassportFile getInstance(Object in) {
      try {
         BERTLVObject obj = null;
         if (in instanceof BERTLVObject) {
            obj = (BERTLVObject)in;
         } else if (in instanceof InputStream) {
            obj = BERTLVObject.getInstance((InputStream)in);
         } else if (in instanceof byte[]) {
            obj = BERTLVObject.getInstance(new ByteArrayInputStream((byte[])in));
         } else {
            throw new IllegalArgumentException("Could not decode input source");
         }
         int tag = obj.getTag();
         switch(tag) {
         case EF_COM_TAG: return new COMFile(obj);
         case EF_DG1_TAG: return new DG1File(obj);
         case EF_DG2_TAG: return new DG2File(obj);
         case EF_DG3_TAG: break;
         case EF_DG4_TAG: break;
         case EF_DG5_TAG: break;
         case EF_DG6_TAG: break;
         case EF_DG7_TAG: break;
         case EF_DG8_TAG: break;
         case EF_DG9_TAG: break;
         case EF_DG10_TAG: break;
         case EF_DG11_TAG: break;
         case EF_DG12_TAG: break;
         case EF_DG13_TAG: break;
         case EF_DG14_TAG: break;
         case EF_DG15_TAG: return new DG15File(obj);
         case EF_DG16_TAG: break;
         case EF_SOD_TAG: return new SODFile(obj);
         default: throw new IllegalArgumentException("Could not decode file "
               + Integer.toHexString(tag));
         }
         throw new IllegalArgumentException("Could not decode file "
               + Integer.toHexString(tag));
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalArgumentException("Could not decode: "
               + e.toString());
      }
   }
   
   /**
    * Gets the contents of this file as byte array,
    * includes the ICAO tag and length.
    * 
    * @return a byte array containing the file
    */
   public abstract byte[] getEncoded();
   
   /* TODO: public abstract int getTag(); */
   /* TODO: public abstract int getLength(); */
   
   /**
    * Corresponds to Table A1 in ICAO-TR-LDS_1.7_2004-05-18.
    *
    * @param tag the first byte of the EF.
    *
    * @return the file identifier.
    */
   static short lookupFIDByTag(int tag) {
      switch(tag) {
         case EF_COM_TAG: return PassportFileService.EF_COM;
         case EF_DG1_TAG: return PassportFileService.EF_DG1;
         case EF_DG2_TAG: return PassportFileService.EF_DG2;
         case EF_DG3_TAG: return PassportFileService.EF_DG3;
         case EF_DG4_TAG: return PassportFileService.EF_DG4;
         case EF_DG5_TAG: return PassportFileService.EF_DG5;
         case EF_DG6_TAG: return PassportFileService.EF_DG6;
         case EF_DG7_TAG: return PassportFileService.EF_DG7;
         case EF_DG8_TAG: return PassportFileService.EF_DG8;
         case EF_DG9_TAG: return PassportFileService.EF_DG9;
         case EF_DG10_TAG: return PassportFileService.EF_DG10;
         case EF_DG11_TAG: return PassportFileService.EF_DG11;
         case EF_DG12_TAG: return PassportFileService.EF_DG12;
         case EF_DG13_TAG: return PassportFileService.EF_DG13;
         case EF_DG14_TAG: return PassportFileService.EF_DG14;
         case EF_DG15_TAG: return PassportFileService.EF_DG15;
         case EF_DG16_TAG: return PassportFileService.EF_DG16;
         case EF_SOD_TAG: return PassportFileService.EF_SOD;
         default:
            throw new NumberFormatException("Unknown tag "
                                            + Integer.toHexString(tag));
      }
   }
}