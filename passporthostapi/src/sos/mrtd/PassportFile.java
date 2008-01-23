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

import sos.tlv.BERTLVObject;

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
   
   /* 
    * We're using a dual representation with a "dirty-bit": When the DG is
    * read from a passport we need to store the binary information as-is
    * since our constructed getEncoded() method might not result in exactly
    * the same byte[] (messing up any cryptographic hash computations needed
    * to validate the security object). -- MO
    */
   BERTLVObject sourceObject;
   boolean isSourceConsistent;
   
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
//    public static PassportFile getInstance(Object in) {
//      try {
//         BERTLVObject obj = null;
//         if (in instanceof BERTLVObject) {
//            obj = (BERTLVObject)in;
//         } else if (in instanceof InputStream) {
//            obj = BERTLVObject.getInstance((InputStream)in);
//         } else if (in instanceof byte[]) {
//            obj = BERTLVObject.getInstance(new ByteArrayInputStream((byte[])in));
//         } else {
//            throw new IllegalArgumentException("Could not decode input source");
//         }
//         int tag = obj.getTag();
//         switch(tag) {
//         case EF_COM_TAG: return new COMFile(obj);
//         case EF_DG1_TAG: return new DG1File(obj);
//         case EF_DG2_TAG: return new DG2File(obj);
//         case EF_DG3_TAG: return new DataGroup(obj);
//         case EF_DG4_TAG: return new DataGroup(obj);
//         case EF_DG5_TAG: return new DataGroup(obj);
//         case EF_DG6_TAG: return new DataGroup(obj);
//         case EF_DG7_TAG: return new DataGroup(obj);
//         case EF_DG8_TAG: return new DataGroup(obj);
//         case EF_DG9_TAG: return new DataGroup(obj);
//         case EF_DG10_TAG: return new DataGroup(obj);
//         case EF_DG11_TAG: return new DataGroup(obj);
//         case EF_DG12_TAG: return new DataGroup(obj);
//         case EF_DG13_TAG: return new DataGroup(obj);
//         case EF_DG14_TAG: return new DataGroup(obj);
//         case EF_DG15_TAG: System.out.println("DEBUG: hier");
//            return new DG15File(new ByteArrayInputStream(obj.getEncoded()));
//         case EF_DG16_TAG: return new DataGroup(obj);
//         case EF_SOD_TAG: return new SODFile(obj);
//         default: throw new IllegalArgumentException("Unknown file tag "
//               + Integer.toHexString(tag));
//         }
//      } catch (Exception e) {
//         e.printStackTrace();
//         return null;
//      }
//   }

   /**
    * Gets the contents of this file as byte array,
    * includes the ICAO tag and length.
    * 
    * @return a byte array containing the file
    */
   /*@ ensures
    *@    isSourceConsistent ==> \result.equals(sourceObject.getEncoded());
    */
   public abstract byte[] getEncoded();

   /**
    * Corresponds to Table A1 in ICAO-TR-LDS_1.7_2004-05-18.
    *
    * @param tag the first byte of the EF.
    *
    * @return the file identifier.
    */
   public static short lookupFIDByTag(int tag) {
      switch(tag) {
         case EF_COM_TAG: return PassportService.EF_COM;
         case EF_DG1_TAG: return PassportService.EF_DG1;
         case EF_DG2_TAG: return PassportService.EF_DG2;
         case EF_DG3_TAG: return PassportService.EF_DG3;
         case EF_DG4_TAG: return PassportService.EF_DG4;
         case EF_DG5_TAG: return PassportService.EF_DG5;
         case EF_DG6_TAG: return PassportService.EF_DG6;
         case EF_DG7_TAG: return PassportService.EF_DG7;
         case EF_DG8_TAG: return PassportService.EF_DG8;
         case EF_DG9_TAG: return PassportService.EF_DG9;
         case EF_DG10_TAG: return PassportService.EF_DG10;
         case EF_DG11_TAG: return PassportService.EF_DG11;
         case EF_DG12_TAG: return PassportService.EF_DG12;
         case EF_DG13_TAG: return PassportService.EF_DG13;
         case EF_DG14_TAG: return PassportService.EF_DG14;
         case EF_DG15_TAG: return PassportService.EF_DG15;
         case EF_DG16_TAG: return PassportService.EF_DG16;
         case EF_SOD_TAG: return PassportService.EF_SOD;
         default:
            throw new NumberFormatException("Unknown tag " + Integer.toHexString(tag));
      }
   }
   
   public static int lookupDataGroupNumberByTag(int tag) {
      switch (tag) {
         case EF_DG1_TAG: return 1;
         case EF_DG2_TAG: return 2;
         case EF_DG3_TAG: return 3;
         case EF_DG4_TAG: return 4;
         case EF_DG5_TAG: return 5;
         case EF_DG6_TAG: return 6;
         case EF_DG7_TAG: return 7;
         case EF_DG8_TAG: return 8;
         case EF_DG9_TAG: return 9;
         case EF_DG10_TAG: return 10;
         case EF_DG11_TAG: return 11;
         case EF_DG12_TAG: return 12;
         case EF_DG13_TAG: return 13;
         case EF_DG14_TAG: return 14;
         case EF_DG15_TAG: return 15;
         case EF_DG16_TAG: return 16;
         default:
            throw new NumberFormatException("Unknown tag " + Integer.toHexString(tag));   
      }
   }
}
