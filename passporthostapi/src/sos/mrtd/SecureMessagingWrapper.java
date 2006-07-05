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

package sos.mrtd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import sos.smartcards.Apdu;
import sos.smartcards.ISO7816;

/**
 * Secure messaging wrapper for apdus.
 * Based on Section E.3 of ICAO-TR-PKI.
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 1.17 $
 */
public class SecureMessagingWrapper implements Apdu.Wrapper
{
   private static final IvParameterSpec ZERO_IV_PARAM_SPEC =
      new IvParameterSpec(new byte[8]);

   private SecretKey ksEnc, ksMac;
   private Cipher cipher;
   private Mac mac;
   private long ssc;

   /**
    * Constructs a secure messaging wrapper based on the secure messaging
    * session keys. The initial value of the send sequence counter is set
    * to <code>0L</code>.
    *
    * @param ksEnc the session key for encryption
    * @param ksMac the session key for macs
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives
    *         ("DESede/CBC/Nopadding" Cipher, "ISO9797Alg3Mac" Mac).
    */
   public SecureMessagingWrapper(SecretKey ksEnc, SecretKey ksMac)
   throws GeneralSecurityException {
      this(ksEnc, ksMac, 0L);
   }

   /**
    * Constructs a secure messaging wrapper based on the secure messaging
    * session keys and the initial value of the send sequence counter.
    *
    * @param ksEnc the session key for encryption
    * @param ksMac the session key for macs
    * @param ssc the initial value of the send sequence counter
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives
    *         ("DESede/CBC/Nopadding" Cipher, "ISO9797Alg3Mac" Mac).
    */
   public SecureMessagingWrapper(SecretKey ksEnc, SecretKey ksMac, long ssc)
   throws GeneralSecurityException {
      this.ksEnc = ksEnc;
      this.ksMac = ksMac;
      this.ssc = ssc;
      cipher = Cipher.getInstance("DESede/CBC/NoPadding");
      mac = Mac.getInstance("ISO9797Alg3Mac");
   }

   /**
    * Gets the current value of the send sequence counter.
    *
    * @return the current value of the send sequence counter.
    */
   public /*@ pure */ long getSendSequenceCounter() {
      return ssc;
   }

   /**
    * Wraps the apdu buffer <code>capdu</code> of a command apdu.
    * As a side effect, this method increments the internal send
    * sequence counter maintained by this wrapper.
    *
    * @param capdu buffer containing the command apdu.
    *
    * @return length of the command apdu after wrapping.
    */
   public byte[] wrap(byte[] capdu) {
      try {
         byte[] wrappedApdu = wrapCommandAPDU(capdu, capdu.length);
         // System.arraycopy(wrappedApdu, 0, capdu, 0, wrappedApdu.length);
         return wrappedApdu;
      } catch (GeneralSecurityException gse) {
         gse.printStackTrace();
         throw new IllegalStateException(gse.toString());
      } catch (IOException ioe) {
         ioe.printStackTrace();
         throw new IllegalStateException(ioe.toString());
      }
   }

   /**
    * Unwraps the apdu buffer <code>rapdu</code> of a response apdu.
    *
    * @param rapdu buffer containing the response apdu.
    * @param len length of the actual response apdu.
    *
    * @return a new byte array containing the unwrapped buffer.
    */
   public byte[] unwrap(byte[] rapdu, int len) {
      try {
         return unwrapResponseAPDU(rapdu, len);
      } catch (GeneralSecurityException gse) {
         gse.printStackTrace();
         throw new IllegalStateException(gse.toString());
      } catch (IOException ioe) {
         ioe.printStackTrace();
         throw new IllegalStateException(ioe.toString());
      }
   }

   /**
    * Does the actual encoding of a command apdu.
    * Based on Section E.3 of ICAO-TR-PKI, especially the examples.
    *
    * @param capdu buffer containing the apdu data. It must be large enough
    *             to receive the wrapped apdu.
    * @param len length of the apdu data.
    *
    * @return a byte array containing the wrapped apdu buffer.
    */
   /*@ requires apdu != null && 4 <= len && len <= apdu.length;
    */
   private byte[] wrapCommandAPDU(byte[] capdu, int len)
   throws GeneralSecurityException, IOException {
      if (capdu == null || capdu.length < 4 || len < 4) {
         throw new IllegalArgumentException("Invalid type");
      }

      /* Determine lc and le... */
      int lc = 0;
      int le = capdu[len - 1] & 0x000000FF;
      if (len == 4) {
         lc = 0;
         le = 0;
      } else if (len == 5) {
         /* No command data, byte at index 5 is le. */
         lc = 0;
      } else if (len > 5) {
         /* Byte at index 5 is not le, so it must be lc. */
         lc = capdu[ISO7816.OFFSET_LC] & 0x000000FF;
      }
      if (4 + lc >= len) {
         /* Value of lc covers rest of apdu length, there is no le. */
         le = 0;
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();

      byte[] maskedHeader = new byte[4];
      System.arraycopy(capdu, 0, maskedHeader, 0, 4);
      maskedHeader[ISO7816.OFFSET_CLA] = (byte)0x0C;
      byte[] paddedHeader = Util.pad(maskedHeader);

      byte[] do87 = new byte[0];
      byte[] do8E = new byte[0];
      byte[] do97 = new byte[0];

      if (le > 0) {
         out.reset();
         out.write((byte)0x97);
         out.write((byte)0x01);
         out.write((byte)le);
         do97 = out.toByteArray();
      }

      if (lc > 0) {
         byte[] data = Util.pad(capdu, ISO7816.OFFSET_CDATA, lc);
         cipher.init(Cipher.ENCRYPT_MODE, ksEnc, ZERO_IV_PARAM_SPEC);
         byte[] ciphertext = cipher.doFinal(data);

         out.reset();
         out.write((byte)0x87);
         out.write((byte)(ciphertext.length + 1));
         out.write(0x01);
         out.write(ciphertext, 0, ciphertext.length);
         do87 = out.toByteArray();
      }

      out.reset();
      out.write(paddedHeader, 0, paddedHeader.length);
      out.write(do87, 0, do87.length);
      out.write(do97, 0, do97.length);
      byte[] m = out.toByteArray();

      out.reset();
      DataOutputStream dataOut = new DataOutputStream(out);
      ssc++;
      dataOut.writeLong(ssc);
      dataOut.write(m, 0, m.length);
      dataOut.flush();
      byte[] n = Util.pad(out.toByteArray());

      /* Compute cryptographic checksum... */
      mac.init(ksMac);
      byte[] cc = mac.doFinal(n);
      ssc++; // TODO dit snappen

      out.reset();
      out.write((byte)0x8E);
      out.write(cc.length);
      out.write(cc, 0, cc.length);
      do8E = out.toByteArray();

      /* Construct protected apdu... */
      out.reset();
      out.write(maskedHeader, 0, 4);
      out.write((byte)(do87.length + do97.length + do8E.length));
      out.write(do87, 0, do87.length);
      out.write(do97, 0, do97.length);
      out.write(do8E, 0, do8E.length);
      out.write(0x00);

      return out.toByteArray();
   }

   /**
    * Does the actual decoding of a response apdu.
    * Based on Section E.3 of TR-PKI, especially the examples.
    *
    * @param rapdu buffer containing the apdu data.
    * @param len length of the apdu data.
    *
    * @return a byte array containing the unwrapped apdu buffer.
    */
   private byte[] unwrapResponseAPDU(byte[] rapdu, int len)
   throws GeneralSecurityException, IOException {
      if (rapdu == null || rapdu.length < 2 || len < 2) {
         throw new IllegalArgumentException("Invalid type");
      }
      cipher.init(Cipher.DECRYPT_MODE, ksEnc, ZERO_IV_PARAM_SPEC);
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(rapdu));
      byte[] data = new byte[0];
      short sw = 0;
      boolean finished = false;
      while (!finished) {
         int tag = in.readByte();
         switch (tag) {
            case (byte)0x87: data = readDO87(in); break;
            case (byte)0x99: sw = readDO99(in); break;
            case (byte)0x8E: readDO8E(in); finished = true; break;
         }
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      out.write(data, 0, data.length);
      out.write((sw & 0x0000FF00) >> 8);
      out.write(sw & 0x000000FF);
      return out.toByteArray();
   }

   /**
    * The <code>0x87</code> tag has already been read.
    *
    * @param in inputstream to read from.
    */
   private byte[] readDO87(DataInputStream in)
   throws IOException, GeneralSecurityException {
      /* Read length... */
      int length = 0;
      int buf = in.readUnsignedByte();
      if ((buf & 0x00000080) != 0x00000080) {
         /* Short form */
         length = buf;
         buf = in.readUnsignedByte(); /* should be 0x01... */
         if (buf != 0x01) {
            throw new IllegalStateException("DO'87 expected 0x01 marker "
                  + Integer.toHexString(buf));
         }
      } else {
         /* Long form */
         int lengthBytesCount = buf & 0x0000007F;
         for (int i = 0; i < lengthBytesCount; i++) {
            length = (length << 8) | in.readUnsignedByte();
         }
         buf = in.readUnsignedByte(); /* should be 0x01... */
         if (buf != 0x01) {
            throw new IllegalStateException("DO'87 expected 0x01 marker");
         }
      }
      length--; /* takes care of the extra 0x01 marker... */
      
      /* Read, decrypt, unpad the data... */
      byte[] ciphertext = new byte[length];
      in.read(ciphertext, 0, length);
      byte[] paddedData = cipher.doFinal(ciphertext);
      byte[] data = Util.unpad(paddedData);
      return data;
   }

   /**
    * The <code>0x99</code> tag has already been read.
    *
    * @param in inputstream to read from.
    */
   private short readDO99(DataInputStream in) throws IOException {
      int length = in.readUnsignedByte();
      if (length != 2) {
         throw new IllegalStateException("DO'99 wrong length");
      }
      byte sw1 = in.readByte();
      byte sw2 = in.readByte();
      return (short)(((sw1 & 0x000000FF) << 8) | (sw2 & 0x000000FF));
   }

   /**
    * The <code>0x8E</code> tag has already been read.
    * FIXME: we should actually check the mac, not just the length :).
    *
    * @param in inputstream to read from.
    */
   private void readDO8E(DataInputStream in) throws IOException {
      int length = in.readUnsignedByte();
      if (length != 8) {
         throw new IllegalStateException("DO'8E wrong length");
      }
   }
}

