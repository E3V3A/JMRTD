/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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
 * $Id: SecureMessagingWrapper.java 1491 2013-02-19 22:03:36Z martijno $
 */

package org.jmrtd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import net.sourceforge.scuba.smartcards.APDUWrapper;
import net.sourceforge.scuba.smartcards.CommandAPDU;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import net.sourceforge.scuba.tlv.TLVUtil;

/*
 * TODO: Can we use TLVInputStream instead of those readDOXX methods? -- MO
 */

/**
 * Secure messaging wrapper for apdus. Based on Section E.3 of ICAO-TR-PKI.
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 1491 $
 */
public class SecureMessagingWrapper implements APDUWrapper, Serializable {

	private static final long serialVersionUID = -2859033943345961793L;

	private static final IvParameterSpec ZERO_IV_PARAM_SPEC = new IvParameterSpec(new byte[8]);
	
	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private SecretKey ksEnc, ksMac;
	private transient Cipher cipher;
	private transient Mac mac;
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
	 *             cryptographic primitives
	 *             ("DESede/CBC/Nopadding" Cipher, "ISO9797Alg3Mac" Mac).
	 */
	public SecureMessagingWrapper(SecretKey ksEnc, SecretKey ksMac)
	throws GeneralSecurityException {
		this(ksEnc, ksMac, 0L);
	}

	/**
	 * Constructs a secure messaging wrapper based on the secure messaging
	 * session keys and the initial value of the send sequence counter.
	 * Used in BAC and EAC 1.
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
	public SecureMessagingWrapper(SecretKey ksEnc, SecretKey ksMac, long ssc) throws GeneralSecurityException {
		this(ksEnc, ksMac, "DESede/CBC/NoPadding", "ISO9797Alg3Mac", ssc);
	}

	public SecureMessagingWrapper(SecretKey ksEnc, SecretKey ksMac, String cipherAlg, String macAlg) throws GeneralSecurityException {
		this(ksEnc, ksMac, cipherAlg, macAlg, 0L);
	}

	public SecureMessagingWrapper(SecretKey ksEnc, SecretKey ksMac, String cipherAlg, String macAlg, long ssc) throws GeneralSecurityException {
		this.ksEnc = ksEnc;
		this.ksMac = ksMac;
		this.ssc = ssc;
		LOGGER.info("DEBUG: cipherAlg = " + cipherAlg);
		LOGGER.info("DEBUG: macAlg = " + macAlg);
		cipher = Cipher.getInstance(cipherAlg);
		mac = Mac.getInstance(macAlg);		
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
				// no sense in unwrapping - card indicates some kind of error
				throw new IllegalStateException("Card indicates SM error, SW = " + Integer.toHexString(responseAPDU.getSW() & 0xFFFF));
				/* FIXME: wouldn't it be cleaner to throw a CardServiceException? */
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
	private CommandAPDU wrapCommandAPDU(CommandAPDU commandAPDU) throws GeneralSecurityException, IOException {
		int lc = commandAPDU.getNc();
		int le = commandAPDU.getNe();

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();

		byte[] maskedHeader = new byte[] { (byte)(commandAPDU.getCLA() | (byte)0x0C), (byte)commandAPDU.getINS(), (byte)commandAPDU.getP1(), (byte)commandAPDU.getP2() };

		/* FIXME: should we pad here only for 3DES or also for AES? */
		byte[] paddedHeader = Util.pad(maskedHeader);

		boolean hasDO85 = ((byte)commandAPDU.getINS() == ISO7816.INS_READ_BINARY2);

		byte[] do8587 = new byte[0];
		/* byte[] do8E = new byte[0]; */ /* FIXME: FindBugs told me this is a dead store -- MO */
		byte[] do97 = new byte[0];

		if (le > 0) {
			bOut.reset();
			bOut.write((byte)0x97);
			bOut.write((byte)0x01);
			bOut.write((byte)le);
			do97 = bOut.toByteArray();
		}

		if (lc > 0) {
			byte[] data = Util.pad(commandAPDU.getData());
			cipher.init(Cipher.ENCRYPT_MODE, ksEnc, ZERO_IV_PARAM_SPEC);
			byte[] ciphertext = cipher.doFinal(data);

			bOut.reset();
			bOut.write(hasDO85 ? (byte)0x85 : (byte)0x87);
			bOut.write(TLVUtil.getLengthAsBytes(ciphertext.length + (hasDO85 ? 0 : 1)));
			if(!hasDO85) { bOut.write(0x01); };
			bOut.write(ciphertext, 0, ciphertext.length);
			do8587 = bOut.toByteArray();
		}

		bOut.reset();
		bOut.write(paddedHeader, 0, paddedHeader.length);
		bOut.write(do8587, 0, do8587.length);
		bOut.write(do97, 0, do97.length);
		byte[] m = bOut.toByteArray();

		bOut.reset();
		DataOutputStream dataOut = new DataOutputStream(bOut);
		ssc++;
		dataOut.writeLong(ssc);
		dataOut.write(m, 0, m.length);
		dataOut.flush();
		byte[] n = Util.pad(bOut.toByteArray());

		/* Compute cryptographic checksum... */
		mac.init(ksMac);
		byte[] cc = mac.doFinal(n);
		int ccLength = cc.length;
		if (ccLength != 8) {
			LOGGER.warning("Found mac length of " + ccLength + ", only using first 8 bytes");
			ccLength = 8;
		}

		bOut.reset();
		bOut.write((byte) 0x8E);
		bOut.write(ccLength);
		bOut.write(cc, 0, ccLength);
		byte[] do8E = bOut.toByteArray();

		/* Construct protected apdu... */
		bOut.reset();
		bOut.write(do8587);
		bOut.write(do97);
		bOut.write(do8E);
		byte[] data = bOut.toByteArray();

		CommandAPDU wc = new CommandAPDU(maskedHeader[0], maskedHeader[1], maskedHeader[2], maskedHeader[3], data, 256);
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
	private byte[] unwrapResponseAPDU(byte[] rapdu, int len) throws GeneralSecurityException, IOException {
		long oldssc = ssc;
		try {
			if (rapdu == null || rapdu.length < 2 || len < 2) {
				throw new IllegalArgumentException("Invalid response APDU");
			}
			cipher.init(Cipher.DECRYPT_MODE, ksEnc, ZERO_IV_PARAM_SPEC);
			DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(rapdu));
			byte[] data = new byte[0];
			short sw = 0;
			boolean finished = false;
			byte[] cc = null;
			while (!finished) {
				int tag = inputStream.readByte();
				switch (tag) {
				case (byte) 0x87:
					data = readDO87(inputStream, false);
				break;
				case (byte) 0x85:
					data = readDO87(inputStream, true);
				break;
				case (byte) 0x99:
					sw = readDO99(inputStream);
				break;
				case (byte) 0x8E:
					cc = readDO8E(inputStream);
				finished = true;
				break;
				}
			}
			if (!checkMac(rapdu, cc)) {
				throw new IllegalStateException("Invalid MAC");
			}
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			bOut.write(data, 0, data.length);
			bOut.write((sw & 0xFF00) >> 8);
			bOut.write(sw & 0x00FF);
			return bOut.toByteArray();
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
	 * @param inputStream
	 *            inputstream to read from.
	 */
	private byte[] readDO87(DataInputStream inputStream, boolean do85) throws IOException, GeneralSecurityException {
		/* Read length... */
		int length = 0;
		int buf = inputStream.readUnsignedByte();
		if ((buf & 0x00000080) != 0x00000080) {
			/* Short form */
			length = buf;
			if(!do85) {
				buf = inputStream.readUnsignedByte(); /* should be 0x01... */
				if (buf != 0x01) {
					throw new IllegalStateException("DO'87 expected 0x01 marker, found " + Integer.toHexString(buf & 0xFF));
				}
			}
		} else {
			/* Long form */
			int lengthBytesCount = buf & 0x0000007F;
			for (int i = 0; i < lengthBytesCount; i++) {
				length = (length << 8) | inputStream.readUnsignedByte();
			}
			if(!do85) {
				buf = inputStream.readUnsignedByte(); /* should be 0x01... */
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
		inputStream.readFully(ciphertext);
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
	private byte[] readDO8E(DataInputStream in) throws IOException, GeneralSecurityException {
		int length = in.readUnsignedByte();
		if (length != 8) {
			throw new IllegalStateException("DO'8E wrong length");
		}
		byte[] cc1 = new byte[8];
		in.readFully(cc1);
		return cc1;
	}

	private boolean checkMac(byte[] rapdu, byte[] cc1) throws GeneralSecurityException {
		try {
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(bOut);
			ssc++;
			dataOut.writeLong(ssc);
			byte[] paddedData = Util.pad(rapdu, 0, rapdu.length - 2 - 8 - 2);
			dataOut.write(paddedData, 0, paddedData.length);
			dataOut.flush();
			dataOut.close();
			mac.init(ksMac);
			byte[] cc2 = mac.doFinal(bOut.toByteArray());
			if (cc2.length > 8 && cc1.length == 8) {
				byte[] newCC2 = new byte[8];
				System.arraycopy(cc2, 0, newCC2, 0, newCC2.length);
				cc2 = newCC2;
			}
			return Arrays.equals(cc1, cc2);
		} catch (IOException ioe) {
			return false;
		}
	}
}
