/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

package org.jmrtd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.sourceforge.scuba.smartcards.APDUWrapper;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.tlv.BERTLVObject;
import net.sourceforge.scuba.util.Hex;

/**
 * Secure messaging wrapper for apdus. Based on Section E.3 of ICAO-TR-PKI.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class SecureMessagingWrapper implements APDUWrapper {
	private static final IvParameterSpec ZERO_IV_PARAM_SPEC = new IvParameterSpec(
			new byte[8]);

	private SecretKey ksEnc, ksMac;
	private Cipher cipher;
	private Mac mac;
	private long ssc;

	/**
	 * Constructs a secure messaging wrapper based on the secure messaging
	 * session keys. The initial value of the send sequence counter is set to
	 * <code>0L</code>.
	 * 
	 * @param ksEnc
	 *            the session key for encryption
	 * @param ksMac
	 *            the session key for macs
	 * 
	 * @throws GeneralSecurityException
	 *             when the available JCE providers cannot provide the necessary
	 *             cryptographic primitives ("DESede/CBC/Nopadding" Cipher,
	 *             "ISO9797Alg3Mac" Mac).
	 */
	public SecureMessagingWrapper(SecretKey ksEnc, SecretKey ksMac)
			throws GeneralSecurityException {
		this(ksEnc, ksMac, 0L);
	}

	/**
	 * Constructs a secure messaging wrapper based on the secure messaging
	 * session keys and the initial value of the send sequence counter.
	 * 
	 * @param ksEnc
	 *            the session key for encryption
	 * @param ksMac
	 *            the session key for macs
	 * @param ssc
	 *            the initial value of the send sequence counter
	 * 
	 * @throws GeneralSecurityException
	 *             when the available JCE providers cannot provide the necessary
	 *             cryptographic primitives ("DESede/CBC/Nopadding" Cipher,
	 *             "ISO9797Alg3Mac" Mac).
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
	public/* @ pure */long getSendSequenceCounter() {
		return ssc;
	}

   /**
    * Wraps the apdu buffer <code>capdu</code> of a command apdu.
    * As a side effect, this method increments the internal send
    * sequence counter maintained by this wrapper.
    *
    * @param commandAPDU buffer containing the command apdu.
    *
    * @return length of the command apdu after wrapping.
    */
   public CommandAPDU wrap(CommandAPDU commandAPDU) {
      try {
         return wrapCommandAPDU(commandAPDU);
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
	 * @param responseAPDU
	 *            buffer containing the response apdu.
	 * @param len
	 *            length of the actual response apdu.
	 * 
	 * @return a new byte array containing the unwrapped buffer.
	 */
	public ResponseAPDU unwrap(ResponseAPDU responseAPDU, int len) {
		try {
			byte[] rapdu = responseAPDU.getBytes();
			if (rapdu.length == 2) {
				// no sense in unwrapping - card indicates SM error
				throw new IllegalStateException(
						"Card indicates SM error, SW = " + rapdu.toString());
				//TODO wouldn't it be cleaner to throw a CardServiceException?
			}
			return new ResponseAPDU(unwrapResponseAPDU(rapdu, len));
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
   private CommandAPDU wrapCommandAPDU(CommandAPDU c)
   throws GeneralSecurityException, IOException {
       
      int lc = c.getNc();
      int le = c.getNe();

      ByteArrayOutputStream out = new ByteArrayOutputStream();

      byte[] maskedHeader = new byte[] {(byte)(c.getCLA() | (byte)0x0C), (byte)c.getINS(), (byte)c.getP1(), (byte)c.getP2()};

      byte[] paddedHeader = Util.pad(maskedHeader);

      boolean do85 = ((byte)c.getINS() == ISO7816.INS_READ_BINARY2);
      
		byte[] do8587 = new byte[0];
		byte[] do8E = new byte[0];
		byte[] do97 = new byte[0];

		if (le > 0) {
			out.reset();
			out.write((byte) 0x97);
			out.write((byte) 0x01);
			out.write((byte) le);
			do97 = out.toByteArray();
		}

      if (lc > 0) {
         byte[] data = Util.pad(c.getData());
         cipher.init(Cipher.ENCRYPT_MODE, ksEnc, ZERO_IV_PARAM_SPEC);
         byte[] ciphertext = cipher.doFinal(data);

			out.reset();
			out.write(do85 ? (byte) 0x85 : (byte) 0x87);
			out.write(BERTLVObject.getLengthAsBytes(ciphertext.length + (do85 ? 0 : 1)));
			if(!do85) { out.write(0x01); };
			out.write(ciphertext, 0, ciphertext.length);
			do8587 = out.toByteArray();
		}

		out.reset();
		out.write(paddedHeader, 0, paddedHeader.length);
		out.write(do8587, 0, do8587.length);
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
		// ssc++; // TODO dit snappen

		out.reset();
		out.write((byte) 0x8E);
		out.write(cc.length);
		out.write(cc, 0, cc.length);
		do8E = out.toByteArray();

      /* Construct protected apdu... */
      out.reset();
      out.write(do8587, 0, do8587.length);
      out.write(do97, 0, do97.length);
      out.write(do8E, 0, do8E.length);
      byte[] data = out.toByteArray();

      
      CommandAPDU wc = new CommandAPDU(maskedHeader[0], maskedHeader[1], maskedHeader[2], maskedHeader[3],
              data, 256);
      return wc;
   }

	/**
	 * Does the actual decoding of a response apdu. Based on Section E.3 of
	 * TR-PKI, especially the examples.
	 * 
	 * @param rapdu
	 *            buffer containing the apdu data.
	 * @param len
	 *            length of the apdu data.
	 * 
	 * @return a byte array containing the unwrapped apdu buffer.
	 */
	private byte[] unwrapResponseAPDU(byte[] rapdu, int len)
			throws GeneralSecurityException, IOException {
		long oldssc = ssc;
		try {
			if (rapdu == null || rapdu.length < 2 || len < 2) {
				throw new IllegalArgumentException("Invalid response APDU");
			}
			cipher.init(Cipher.DECRYPT_MODE, ksEnc, ZERO_IV_PARAM_SPEC);
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(
					rapdu));
			byte[] data = new byte[0];
			short sw = 0;
			boolean finished = false;
			byte[] cc = null;
			while (!finished) {
				int tag = in.readByte();
				switch (tag) {
				case (byte) 0x87:
					data = readDO87(in, false);
					break;
                case (byte) 0x85:
                    data = readDO87(in, true);
                    break;
				case (byte) 0x99:
					sw = readDO99(in);
					break;
				case (byte) 0x8E:
					cc = readDO8E(in);
					finished = true;
					break;
				}
			}
			if (!checkMac(rapdu, cc)) {
				throw new IllegalStateException("Invalid MAC");
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(data, 0, data.length);
			out.write((sw & 0x0000FF00) >> 8);
			out.write(sw & 0x000000FF);
			return out.toByteArray();
		} finally {
			/*
			 * If we fail to unwrap, at least make sure we have the same counter
			 * as the ICC, so that we can continue to communicate using secure
			 * messaging...
			 */
			if (ssc == oldssc) {
				ssc++;
			}
		}
	}

	/**
	 * The <code>0x87</code> tag has already been read.
	 * 
	 * @param in
	 *            inputstream to read from.
	 */
	private byte[] readDO87(DataInputStream in, boolean do85) throws IOException,
			GeneralSecurityException {
		/* Read length... */
		int length = 0;
		int buf = in.readUnsignedByte();
		if ((buf & 0x00000080) != 0x00000080) {
			/* Short form */
			length = buf;
            if(!do85) {
			  buf = in.readUnsignedByte(); /* should be 0x01... */
			  if (buf != 0x01) {
				throw new IllegalStateException(
						"DO'87 expected 0x01 marker, found "
								+ Hex.byteToHexString((byte) buf));
			  }
            }
		} else {
			/* Long form */
			int lengthBytesCount = buf & 0x0000007F;
			for (int i = 0; i < lengthBytesCount; i++) {
				length = (length << 8) | in.readUnsignedByte();
			}
            if(!do85) {
              buf = in.readUnsignedByte(); /* should be 0x01... */
			  if (buf != 0x01) {
				throw new IllegalStateException("DO'87 expected 0x01 marker");
			  }
            }
		}
        if(!do85) {
		  length--; /* takes care of the extra 0x01 marker... */
        }
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
	 * @param in
	 *            inputstream to read from.
	 */
	private short readDO99(DataInputStream in) throws IOException {
		int length = in.readUnsignedByte();
		if (length != 2) {
			throw new IllegalStateException("DO'99 wrong length");
		}
		byte sw1 = in.readByte();
		byte sw2 = in.readByte();
		return (short) (((sw1 & 0x000000FF) << 8) | (sw2 & 0x000000FF));
	}

	/**
	 * The <code>0x8E</code> tag has already been read.
	 * 
	 * @param in
	 *            inputstream to read from.
	 */
	private byte[] readDO8E(DataInputStream in) throws IOException,
			GeneralSecurityException {
		int length = in.readUnsignedByte();
		if (length != 8) {
			throw new IllegalStateException("DO'8E wrong length");
		}
		byte[] cc1 = new byte[8];
		in.readFully(cc1);
		return cc1;
	}

	private boolean checkMac(byte[] rapdu, byte[] cc1)
			throws GeneralSecurityException {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(out);
			ssc++;
			dataOut.writeLong(ssc);
			byte[] paddedData = Util.pad(rapdu, 0, rapdu.length - 2 - 8 - 2);
			dataOut.write(paddedData, 0, paddedData.length);
			dataOut.flush();
			mac.init(ksMac);
			byte[] cc2 = mac.doFinal(out.toByteArray());
			dataOut.close();
			return Arrays.equals(cc1, cc2);
		} catch (IOException ioe) {
			return false;
		}
	}
}
