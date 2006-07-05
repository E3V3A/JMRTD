/*
 * passportapplet - A reference implementation of the MRTD standards.
 *
 * Copyright (C) 2006  SoS group, Radboud University
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

package sos.passportapplet;

import javacard.framework.ISOException;
import javacard.framework.Util;
import javacard.framework.ISO7816;

/**
 * FileSystem
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 * @version $Revision: 1.1 $
 */
public class FileSystem {
   private static final short EF_DG1_FID = (short) 0x0101;

   private static final short EF_DG2_FID = (short) 0x0102;

   private static final short EF_DG3_FID = (short) 0x0103;

   private static final short EF_DG4_FID = (short) 0x0104;

   private static final short EF_DG5_FID = (short) 0x0105;

   private static final short EF_DG6_FID = (short) 0x0106;

   private static final short EF_DG7_FID = (short) 0x0107;

   private static final short EF_DG8_FID = (short) 0x0108;

   private static final short EF_DG9_FID = (short) 0x0109;

   private static final short EF_DG10_FID = (short) 0x010A;

   private static final short EF_DG11_FID = (short) 0x010B;

   private static final short EF_DG12_FID = (short) 0x010C;

   private static final short EF_DG13_FID = (short) 0x010D;

   private static final short EF_DG14_FID = (short) 0x010E;

   private static final short EF_DG15_FID = (short) 0x010F;

   private static final short EF_SOD_FID = (short) 0x011D;

   private static final short EF_COM_FID = (short) 0x011E;

   private static final short EF_DG1_INDEX = (short) 0;

   private static final short EF_DG2_INDEX = (short) 1;

   private static final short EF_DG3_INDEX = (short) 2;

   private static final short EF_DG4_INDEX = (short) 3;

   private static final short EF_DG5_INDEX = (short) 4;

   private static final short EF_DG6_INDEX = (short) 5;

   private static final short EF_DG7_INDEX = (short) 6;

   private static final short EF_DG8_INDEX = (short) 7;

   private static final short EF_DG9_INDEX = (short) 8;

   private static final short EF_DG10_INDEX = (short) 9;

   private static final short EF_DG11_INDEX = (short) 10;

   private static final short EF_DG12_INDEX = (short) 11;

   private static final short EF_DG13_INDEX = (short) 12;

   private static final short EF_DG14_INDEX = (short) 13;

   private static final short EF_DG15_INDEX = (short) 14;

   private static final short EF_SOD_INDEX = (short) 15;

   private static final short EF_COM_INDEX = (short) 16;

   private Object[] files;

   //private short[] fileLengths;

   public FileSystem() {
      files = new Object[17];
      //fileLengths = new short[17];
   }

   public void createFile(short fid, short size) {
       short idx = getFileIndex(fid);
       
//       if(fileLengths[idx] == 0)
//           ISOException.throwIt((short)0x6d40);
       
       if(files[idx] != null)
           ISOException.throwIt(ISO7816.SW_FILE_INVALID);
       
       files[idx] = new byte[size];
   }
   
//   public void setFileLength(short fid, short length) {
//       if(fileLengths[getFileIndex(fid)] != 0)
//           ISOException.throwIt((short)0x6d41);
//       
//       fileLengths[getFileIndex(fid)] = length;
//       
//   }
   
   public void writeData(short fid, short file_offset, byte[] data, short data_offset, short length) {
       short idx = getFileIndex(fid);
       
       if((short)((byte[])files[idx]).length < (short)(file_offset + length))
           ISOException.throwIt((short)0x6d42);
       
       Util.arrayCopy(data, data_offset, getFile(fid), file_offset, length);
   }
   
   public byte[] getFile(short fid) {
      return (byte[]) files[getFileIndex(fid)];
   }

   public static byte exists(short fid) {
       return (getFileIndex(fid) != 0 ? (byte)1 : 0);
   }
   
   private static short getFileIndex(short fid) throws ISOException {
      switch (fid) {
      case EF_DG1_FID:
         return EF_DG1_INDEX;
      case EF_DG2_FID:
         return EF_DG2_INDEX;
      case  EF_DG3_FID:
         return EF_DG3_INDEX;
      case  EF_DG4_FID:
         return EF_DG4_INDEX;
      case  EF_DG5_FID:
         return EF_DG5_INDEX;
      case  EF_DG6_FID:
         return EF_DG6_INDEX;
      case  EF_DG7_FID:
         return EF_DG7_INDEX;
      case  EF_DG8_FID:
         return EF_DG8_INDEX;
      case  EF_DG9_FID:
         return EF_DG9_INDEX;
      case  EF_DG10_FID:
         return EF_DG10_INDEX;
      case  EF_DG11_FID:
         return EF_DG11_INDEX;
      case  EF_DG12_FID:
         return EF_DG12_INDEX;
      case  EF_DG13_FID:
         return EF_DG13_INDEX;
      case  EF_DG14_FID:
         return EF_DG14_INDEX;
      case  EF_DG15_FID:
         return EF_DG15_INDEX;
      case EF_SOD_FID:
         return EF_SOD_INDEX;
      case EF_COM_FID:
         return EF_COM_INDEX;
      default:
         ISOException.throwIt((short) 0x6F00); /*
                                                 * FIXME exception type and
                                                 * return code...
                                                 */
         return 0;
      }
   }
}
