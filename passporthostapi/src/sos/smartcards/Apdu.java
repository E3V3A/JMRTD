/*
 * JMRTD - A Java API for accessing machine readable travel documents.
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

package sos.smartcards;

import sos.util.Hex;

/**
 * Apdu Joehoe. Represents combined command apdu and response apdu similar to
 * the Apdu class in Sun's Java Card kit.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: 1.13 $
 */
public class Apdu implements ISO7816 {

   private byte[] commandApduBuffer;
   private byte[] responseApduBuffer;

   public Apdu(byte cla, byte ins, byte p1, byte p2) {
      this(cla, ins, p1, p2, null, -1);
   }
   
   public Apdu(byte cla, byte ins, byte p1, byte p2, int le) {
      this(cla, ins, p1, p2, null, le);
   }
   
   public Apdu(byte cla, byte ins, byte p1, byte p2, byte[] data) {
      this(cla, ins, p1, p2, data, -1);
   }

   public Apdu(byte cla, byte ins, byte p1, byte p2, byte[] data, int le) {
       this(cla, ins, p1, p2, data == null ? 0 : data.length, data, le);
   }

   public Apdu(byte cla, byte ins, byte p1, byte p2, int lc, byte[] data, int le) {
      if (data == null || lc == 0) {
         commandApduBuffer = new byte[4 + ((le < 0) ? 0 : 1)];
      } else {
         /*@ assert lc > 0; */
         commandApduBuffer = new byte[4 + 1 + lc + ((le < 0) ? 0 : 1)];
         
         commandApduBuffer[OFFSET_LC] = (byte)lc;
         System.arraycopy(data, 0, commandApduBuffer, OFFSET_CDATA, lc);
      }
      commandApduBuffer[OFFSET_CLA] = cla;
      commandApduBuffer[OFFSET_INS] = ins;
      commandApduBuffer[OFFSET_P1] = p1;
      commandApduBuffer[OFFSET_P2] = p2;
      if (le >= 0) {
         commandApduBuffer[commandApduBuffer.length - 1] = (byte)le;
      }
      responseApduBuffer = null;
   }
   
   public Apdu(byte[] buffer) {
      setCommandApduBuffer(buffer);
      responseApduBuffer = null;
   }
   
   /**
    * Wrapper interface for command Apdu wrapping.
    * 
    * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
    * @author Martijn Oostdijk (martijno@cs.ru.nl)
    * 
    * @version $Revision: 1.13 $
    */
   public interface Wrapper {

      /**
       * Wraps the command apdu buffer.
       * 
       * @param buffer
       *           should contain a header (length 4), an explicit lc (0 if no
       *           cdata), the cdata (of length lc), and an explicit le (0 if
       *           not specified).
       * 
       * @return wrapped apdu buffer
       */
      public byte[] wrap(byte[] buffer);
   }

   /**
    * Wraps this apdu using wrapper implementation <code>w</code>.
    * 
    * @param w
    *           the wrapper to use
    * 
    * @return the wrapped apdu buffer
    */
   public byte[] wrapWith(Wrapper w) {
      byte[] wrappedApdu = w.wrap(commandApduBuffer);
      setCommandApduBuffer(wrappedApdu);
      return wrappedApdu;
   }

   /**
    * Construct a byte array representation of this command apdu.
    * 
    * @return command apdu buffer of this apdu
    */
   public byte[] getCommandApduBuffer() {
      return commandApduBuffer;
   }

   private void setCommandApduBuffer(byte[] buffer) {
      if (!(4 <= buffer.length && buffer.length <= 256)) {
         throw new IllegalArgumentException("Wrong length!");
      }
      commandApduBuffer = buffer;
   }
   
   /**
    * Gets the response apdu buffer.
    * 
    * @return a response apdu buffer which includes status word
    */
   public byte[] getResponseApduBuffer() {
      return responseApduBuffer;
   }
   
   /**
    * Sets this apdu to the response apdu buffer in <code>buffer</code>.
    * 
    * @param buffer
    *           a response apdu buffer which includes status word
    */
   public void setResponseApduBuffer(byte[] buffer) {
      if (!(2 <= buffer.length && buffer.length <= 256)) {
         throw new IllegalArgumentException("Wrong length!");
      }
      responseApduBuffer = buffer;
   }
   
   public int getLc() {
      if (commandApduBuffer == null) {
         throw new IllegalStateException("buffer is null!");
      }
      if (commandApduBuffer.length <= 5) {
         return 0;
      }
      int lc = commandApduBuffer[OFFSET_LC];
      return lc;
   }
   
   public int getLe() {
      if (commandApduBuffer == null) {
         throw new IllegalStateException("buffer is null!");
      }
      if (commandApduBuffer.length <= 4) {
         return -1;
      }
      int le = commandApduBuffer[commandApduBuffer.length - 1];
      if (commandApduBuffer.length == 5) {
         return le;
      }
      int lc = commandApduBuffer[OFFSET_LC] & 0x000000FF;
      if (commandApduBuffer.length <= 5 + lc) {
         return -1;
      }
      return le;
   }
   
   public String toString() {
      return Hex.bytesToHexString(commandApduBuffer);
   }
}
