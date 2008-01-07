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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;

/**
 * High level card passport service for using the passport.
 * Defines high-level commands to access the information on the passport.
 * Based on ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt;
 *       doBAC(...) ==&gt; readBlahFile() ==&gt; close()
 *    </pre> 
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class PassportService extends PassportAuthService
{
   /** Data group 1 contains the MRZ. */
   public static final short EF_DG1 = 0x0101;
   /** Data group 2 contains face image data. */
   public static final short EF_DG2 = 0x0102;
   /** Data group 3 contains finger print data. */
   public static final short EF_DG3 = 0x0103;
   /** Data group 4 contains iris data. */
   public static final short EF_DG4 = 0x0104;
   /** Data group 5 contains displayed portrait. */
   public static final short EF_DG5 = 0x0105;
   /** Data group 6 is RFU. */
   public static final short EF_DG6 = 0x0106;
   /** Data group 7 contains displayed signature. */
   public static final short EF_DG7 = 0x0107;
   /** Data group 8 contains data features. */
   public static final short EF_DG8 = 0x0108;
   /** Data group 9 contains structure features. */
   public static final short EF_DG9 = 0x0109;
   /** Data group 10 contains substance features. */
   public static final short EF_DG10 = 0x010A;
   /** Data group 11 contains additional personal details. */
   public static final short EF_DG11 = 0x010B;
   /** Data group 12 contains additional document details. */
   public static final short EF_DG12 = 0x010C;
   /** Data group 13 contains optional details. */
   public static final short EF_DG13 = 0x010D;
   /** Data group 14 is RFU. */
   public static final short EF_DG14 = 0x010E;
   /** Data group 15 contains the public key used for Active Authentication. */
   public static final short EF_DG15 = 0x010F;
   /** Data group 16 contains person(s) to notify. */
   public static final short EF_DG16 = 0x0110;
   /** The security document. */
   public static final short EF_SOD = 0x011D;
   /** File indicating which data groups are present. */
   public static final short EF_COM = 0x011E;

   /** The file read block size, some passports cannot handle large values */
   public static int maxFileSize = 255;

   private PassportAuthService passportAuthService;

   /**
    * Creates a new passport service for accessing the passport.
    * 
    * @param service another service which will deal
    *        with sending the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public PassportService(CardService service) throws CardServiceException {
      super(service);
      if (service instanceof PassportService) {
         this.passportAuthService =
            ((PassportService)service).passportAuthService;
      } else {
         this.passportAuthService = new PassportAuthService(service);
      }
      addAuthenticationListener(passportAuthService);

   }

   /**
    * Gets the data group presence list.
    * 
    * @return the file containing the data group presence list
    * @throws IOException if the file cannot be read
    */
   public COMFile readCOMFile() throws CardServiceException {
      return (COMFile)getFileByTag(PassportFile.EF_COM_TAG);
   }

   /**
    * Gets the data group indicated by <code>tag</code>.
    * 
    * @param tag should be a valid ICAO datagroup tag
    * 
    * @return the data group file
    * 
    * @throws IOException if the file cannot be read
    */
   public DataGroup readDataGroup(int tag) throws CardServiceException {
      return (DataGroup)getFileByTag(tag);
   }

   /**
    * Gets DG1.
    * 
    * @return the data group file
    * 
    * @throws IOException if the file cannot be read
    */
   public DG1File readDG1() throws CardServiceException {
      return (DG1File)readDataGroup(PassportFile.EF_DG1_TAG);
   }

   /**
    * Gets DG2.
    * 
    * @return the data group file
    * 
    * @throws IOException if the file cannot be read
    */   
   public DG2File readDG2() throws CardServiceException {
      return (DG2File)readDataGroup(PassportFile.EF_DG2_TAG);
   }

   /**
    * Gets DG15.
    * 
    * @return the data group file
    * 
    * @throws IOException if the file cannot be read
    */
   public DG15File readDG15() throws CardServiceException {
      return (DG15File)readDataGroup(PassportFile.EF_DG15_TAG);
   }

   /**
    * Gets the document security object.
    * 
    * @return the document security object
    * 
    * @throws IOException if the file cannot be read
    */
   public SODFile getSODFile() throws CardServiceException {
      return (SODFile)getFileByTag(PassportFile.EF_SOD_TAG);
   }

   /**
    * Gets the file indicated by tag.
    * 
    * @param tag ICAO file tag
    * 
    * @return the file
    * 
    * @throws IOException if the file cannot be read
    * 
    *
    * @deprecated Shall be made private!
    *
    */
   public PassportFile getFileByTag(int tag) throws CardServiceException {
      short fid = PassportFile.lookupFIDByTag(tag);
      return getFileByFID(fid);
   }

   /**
    * @deprecated Shall be made private!
    */
   public PassportFile getFileByFID(short fid) throws CardServiceException {
      return PassportFile.getInstance(readFile(fid));
   }

   private synchronized byte[] readFile(short fid) throws CardServiceException {
      return readFile(fid, 0, -1);
   }
   
   /**
    * Reads the file with id <code>fid</code>.
    *
    * @param fid the file to read
    * @param offset starting offset in file
    * @param length the number of bytes to read, or -1 to read until EOF
    *
    * @return the contents of the file.
    */
   private synchronized byte[] readFile(short fid, int offset, int length) throws CardServiceException {
      SecureMessagingWrapper wrapper = getWrapper();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      service.sendSelectFile(wrapper, fid);
      int blockSize = maxFileSize;
      while (true) {
         int len = length < 0 ? blockSize : Math.min(blockSize, length - offset);
         byte[] data = service.sendReadBinary(wrapper, (short)offset, len);
         if (data == null || data.length == 0) { break; }
         out.write(data, 0, data.length);
         offset += data.length;
         if (length < 0) { continue; }
         if (offset >= length) { break; }
      }
      byte[] file = out.toByteArray();
      return file;
   }
   
   private class PassportFileInputStream extends InputStream
   {
      private byte[] buffer;
      private int offsetBufferInFile;
      private int indexInBuffer;
  
      public PassportFileInputStream(PassportService service, short fid) {
         buffer = new byte[maxFileSize];
         offsetBufferInFile = 0;
         indexInBuffer = 0;
         /* TODO: read T and L and determine length of V. */
      }
      
      public int read() throws IOException {
         int result = -1;
         if (indexInBuffer < buffer.length) {
            result = buffer[indexInBuffer];
            indexInBuffer++;
         } else {
            
         }
         return result;
      }
   }
}
