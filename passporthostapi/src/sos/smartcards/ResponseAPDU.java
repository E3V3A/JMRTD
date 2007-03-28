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
 * $Id: Apdu.java 149 2006-08-14 09:12:04Z martijno $
 */

package sos.smartcards;

import sos.util.Hex;

/**
 * APDU Joehoe. Represents response apdu.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @version $Revision: 149 $
 */
public class ResponseAPDU implements ISO7816
{
   private byte[] data;
   private short sw;

   public ResponseAPDU(byte[] buffer) {
      setBuffer(buffer);
   }

   public ResponseAPDU(byte[] data, short sw) {
      if (data == null) {
         this.data = null;
      } else if (this.data == null || this.data.length < data.length) {
         this.data = new byte[data.length];
      }
      System.arraycopy(data, 0, this.data, 0, data.length);
      this.sw = sw;
   }

   /**
    * Gets the response apdu buffer.
    * 
    * @return a response apdu buffer which includes status word
    */
   public byte[] getBuffer() {
      if (data == null) { return null; }
      byte[] buffer = new byte[data.length + 2];
      System.arraycopy(data, 0, buffer, 0, data.length);
      buffer[data.length] = (byte)((sw & 0xFF00) >> 8);
      buffer[data.length + 1] = (byte)(sw & 0xFF);
      return buffer;
   }

   public byte[] getData() {
      return data;
   }

   public int getSW() {
      return sw & 0xFFFF;
   }

   /**
    * Sets this apdu to the response apdu buffer in <code>buffer</code>.
    * 
    * @param buffer a response apdu buffer which includes status word
    */
   public void setBuffer(byte[] buffer) {
      if (!(2 <= buffer.length && buffer.length <= 256)) {
         throw new IllegalArgumentException(
            "Wrong length! (length == " + buffer.length + ")");
      }
      if (this.data == null || this.data.length != (buffer.length - 2)) {
         this.data = new byte[buffer.length - 2];
      }
      System.arraycopy(buffer, 0, this.data, 0, buffer.length - 2);
      this.sw = (short)(((buffer[buffer.length - 2] & 0xFF) << 8) | (buffer[buffer.length - 1] & 0xFF));
   }

   /**
    * Generates a textual representation of this command Apdu.
    * 
    * @return a textual representation of this Apdu
    * @see java.lang.Object#toString()
    */
   public String toString() {
      return Hex.bytesToHexString(data) + " SW: " + Hex.shortToHexString(sw);
   }
}
