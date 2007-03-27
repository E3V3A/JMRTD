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

package sos.smartcards;

import sos.util.Hex;

/**
 * Apdu Joehoe. Represents combined command apdu and response apdu similar to
 * the Apdu class in Sun's Java Card kit.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @version $Revision$
 */
public class CommandAPDU implements ISO7816
{
   private byte[] commandApduBuffer;

   /**
    * Constructs a &quot;case 1&quot; APDU.
    * 
    * @param cla class byte
    * @param ins instruction byte
    * @param p1 parameter byte 1
    * @param p2 parameter byte 2
    */
   public CommandAPDU(byte cla, byte ins, byte p1, byte p2) {
      this(cla, ins, p1, p2, null, -1);
   }

   /**
    * Constructs a &quot;case 2&quot; APDU.
    * 
    * @param cla class byte
    * @param ins instruction byte
    * @param p1 parameter byte 1
    * @param p2 parameter byte 2
    * @param le expected length of response
    */
   public CommandAPDU(byte cla, byte ins, byte p1, byte p2, int le) {
      this(cla, ins, p1, p2, null, le);
   }

   /**
    * Constructs a &quot;case 3&quot; APDU.
    * 
    * @param cla class byte
    * @param ins instruction byte
    * @param p1 parameter byte 1
    * @param p2 parameter byte 2
    * @param data command data
    */
   public CommandAPDU(byte cla, byte ins, byte p1, byte p2, byte[] data) {
      this(cla, ins, p1, p2, data, -1);
   }

   /**
    * Constructs a &quot;case 4&quot; APDU.
    * 
    * @param cla class byte
    * @param ins instruction byte
    * @param p1 parameter byte 1
    * @param p2 parameter byte 2
    * @param data command data
    * @param le expected length of response
    */
   public CommandAPDU(byte cla, byte ins, byte p1, byte p2, byte[] data, int le) {
      this(cla, ins, p1, p2, data == null ? 0 : data.length, data, le);
   }

   /**
    * Constructs an APDU.
    * 
    * @param cla class bytes
    * @param ins instruction byte
    * @param p1 parameter byte 1
    * @param p2 parameter byte 2
    * @param lc length of command data
    * @param data command data
    * @param le expected length of response
    */
   public CommandAPDU(byte cla, byte ins, byte p1, byte p2, int lc,
         byte[] data, int le) {
      if (data == null || lc == 0) {
         commandApduBuffer = new byte[4 + ((le < 0) ? 0 : 1)];
      } else {
         /* @ assert lc > 0; */
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
   }

   /**
    * Constructs an APDU.
    * 
    * @param buffer command APDU buffer
    */
   public CommandAPDU(byte[] buffer) {
      setCommandApduBuffer(buffer);
   }



   /**
    * Wraps this apdu using wrapper implementation <code>w</code>.
    * 
    * @param w the wrapper to use
    * @return the wrapped apdu buffer
    */
   public byte[] wrapWith(APDUWrapper w) {
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
      if (!(4 <= buffer.length && buffer.length <= 256)) { throw new IllegalArgumentException(
            "Wrong length!"); }
      commandApduBuffer = buffer;
   }

   /**
    * Gets the length of the command data, or 0 if not present.
    * 
    * @return length of the command data
    */
   public int getLc() {
      if (commandApduBuffer == null) { throw new IllegalStateException(
            "buffer is null!"); }
      if (commandApduBuffer.length <= 5) { return 0; }
      int lc = commandApduBuffer[OFFSET_LC];
      return lc;
   }

   /**
    * Gets the expected length of the response data as indicated in the command
    * apdu buffer.
    * 
    * @return expected length of response data
    */
   public int getLe() {
      if (commandApduBuffer == null) { throw new IllegalStateException(
            "buffer is null!"); }
      if (commandApduBuffer.length <= 4) { return -1; }
      int le = commandApduBuffer[commandApduBuffer.length - 1];
      if (commandApduBuffer.length == 5) { return le; }
      int lc = commandApduBuffer[OFFSET_LC] & 0x000000FF;
      if (commandApduBuffer.length <= 5 + lc) { return -1; }
      return le;
   }

   /**
    * Generates a textual representation of this command Apdu.
    * 
    * @return a textual representation of this Apdu
    * @see java.lang.Object#toString()
    */
   public String toString() {
      return Hex.bytesToHexString(commandApduBuffer);
   }
}
