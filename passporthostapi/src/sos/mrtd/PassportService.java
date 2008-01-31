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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.tlv.BERTLVInputStream;

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
   public static int maxBlockSize = 255;

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
   }

   /**
    * Gets the file indicated by tag.
    * 
    * @param tag ICAO file tag
    * 
    * @return the file
    * 
    * @throws IOException if the file cannot be read
    */
   public InputStream readFile(short fid) throws CardServiceException {
      return new CardFileInputStream(fid);
   }
   
   public InputStream readDataGroup(int tag) throws CardServiceException {
      short fid = PassportFile.lookupFIDByTag(tag);
      return readFile(fid);
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
   private synchronized byte[] readFromFile(short fid, int offset, int length) throws CardServiceException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      if (!isSelectedFID(fid)) { sendSelectFile(wrapper, fid); }
      int blockSize = maxBlockSize;
      while (true) {
         int len = length < 0 ? blockSize : Math.min(blockSize, length);
         byte[] data = sendReadBinary(wrapper, (short)offset, len);
         if (data == null || data.length == 0) { break; } /* Reached EOF */
         out.write(data, 0, data.length);
         offset += data.length;
         if (length < 0) { continue; }
         if (offset >= length) { break; } /* (More than) length bytes read. */
      }
      byte[] file = out.toByteArray();
      return file;
   }

   private class CardFileInputStream extends InputStream
   {
      private short fid;
      private byte[] buffer;
      private int offsetBufferInFile;
      private int offsetInBuffer;
      private int markedOffset;
      private int fileLength;

      public CardFileInputStream(short fid) throws CardServiceException {
         this.fid = fid;
         sendSelectFile(wrapper, fid);
         buffer = readFromFile(fid, 0, 8); /* Tag at most 2, length at most 5? */
         try {
            ByteArrayInputStream baIn = new ByteArrayInputStream(buffer);
            BERTLVInputStream tlvIn = new BERTLVInputStream(baIn);
            tlvIn.readTag();
            fileLength = tlvIn.readLength();
            fileLength += (buffer.length - tlvIn.available());
            offsetBufferInFile = 0;
            offsetInBuffer = 0;
            markedOffset = 0;
         } catch (IOException ioe) {
            throw new CardServiceException(ioe.toString());
         }
      }

      public int read() throws IOException {
         int result = -1;
         if (offsetInBuffer >= buffer.length) {
            int blockSize = Math.min(maxBlockSize, available());
            try {
               offsetBufferInFile += offsetInBuffer;
               offsetInBuffer = 0;
               buffer = readFromFile(fid, offsetBufferInFile, blockSize);
            } catch (CardServiceException cse) {
               throw new IOException(cse.toString());
            }
         }
         if (offsetInBuffer < buffer.length) {
            result = buffer[offsetInBuffer] & 0xFF;
            offsetInBuffer++;
         }
         return result;
      }
      
      public long skip(long n) {
         int available = available();
         if (n > available) { n = available; }
         if (n < (buffer.length - offsetInBuffer)) {
            offsetInBuffer += n;
         } else {
            int absoluteOffset = offsetBufferInFile + offsetInBuffer;
            offsetBufferInFile = (int)(absoluteOffset + n);
            offsetInBuffer = 0;
         }
         return n;
      }
      
      public synchronized int available() {
         return fileLength - (offsetBufferInFile + offsetInBuffer);
      }
      
      public void mark(int readLimit) {
         markedOffset = offsetBufferInFile + offsetInBuffer;
      }
      
      public void reset() {
         offsetBufferInFile = markedOffset;
         offsetInBuffer = 0;
      }
      
      public boolean markSupported() {
         return true;
      }
   }
}
