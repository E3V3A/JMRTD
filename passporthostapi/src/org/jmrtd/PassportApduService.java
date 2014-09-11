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
 * $Id: PassportApduService.java 1494 2013-02-27 16:36:26Z martijno $
 */

package org.jmrtd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import net.sourceforge.scuba.smartcards.APDUEvent;
import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.CommandAPDU;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import net.sourceforge.scuba.tlv.TLVInputStream;
import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 * Low level card service for sending APDUs to the passport. This service is not
 * responsible for maintaining information about the state of the authentication
 * or secure messaging protocols. It merely offers the basic functionality for
 * sending passport specific APDUs to the passport.
 * 
 * Based on ICAO-TR-PKI. Defines the following commands:
 * <ul>
 * <li><code>GET CHALLENGE</code></li>
 * <li><code>EXTERNAL AUTHENTICATE</code></li>
 * <li><code>INTERNAL AUTHENTICATE</code> (using secure messaging)</li>
 * <li><code>SELECT FILE</code> (using secure messaging)</li>
 * <li><code>READ BINARY</code> (using secure messaging)</li>
 * </ul>
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 1494 $
 */
public class PassportApduService extends CardService {

	/** Shared secret type for PACE according to BSI TR-03110 v2.03 B.11.1. */
	public static final byte
	MRZ_PACE_KEY_REFERENCE = 0x01,
	CAN_PACE_KEY_REFERENCE = 0x02,
	PIN_PACE_KEY_REFERENCE = 0x03,
	PUK_PACE_REFERENCE = 0x04;

	private static final long serialVersionUID = 2451509825132976178L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	/** The applet we select when we start a session. */
	private static final byte[] APPLET_AID = { (byte) 0xA0, 0x00, 0x00, 0x02, 0x47, 0x10, 0x01 };

	/** Initialization vector used by the cipher below. */
	private static final IvParameterSpec ZERO_IV_PARAM_SPEC = new IvParameterSpec(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

	/** The general Authenticate command is used to perform the PACE protocol. See Section 3.2.2 of SAC-TR 1.01. */
	private static final byte INS_PACE_GENERAL_AUTHENTICATE = (byte)0x86;

	/** The service we decorate. */
	private CardService service;

	private byte[] atr;

	/** DESede encryption/decryption cipher. */
	private transient Cipher cipher;

	/** ISO9797Alg3Mac. */
	private transient Mac mac;

	private Collection<APDUListener> plainTextAPDUListeners;

	private int plainAPDUCount;

	/**
	 * Creates a new passport APDU sending service.
	 * 
	 * @param service
	 *            another service which will deal with sending the APDUs to the
	 *            card
	 * 
	 * @throws CardServiceException
	 *             when the available JCE providers cannot provide the necessary
	 *             cryptographic primitives:
	 *             <ul>
	 *             <li>Cipher: "DESede/CBC/Nopadding"</li>
	 *             <li>Mac: "ISO9797Alg3Mac"</li>
	 *             </ul>
	 */
	public PassportApduService(CardService service) throws CardServiceException {
		this.service = service;
		plainTextAPDUListeners = new HashSet<APDUListener>();
		plainAPDUCount = 0;
		try {
			mac = Mac.getInstance("ISO9797Alg3Mac", BC_PROVIDER);
			cipher = Cipher.getInstance("DESede/CBC/NoPadding");
		} catch (GeneralSecurityException gse) {
			gse.printStackTrace();
			throw new CardServiceException(gse.toString());
		}
	}

	/**
	 * Opens a session by connecting to the card. Since version 0.5.1 this method no longer automatically
	 * selects the MRTD applet, caller (for instance {@link PassportService}) is responsible to do this now.
	 * 
	 * @throws CardServiceException on failure to open the service
	 */
	public void open() throws CardServiceException {
		if (!service.isOpen()) {
			service.open();
		}
		atr = service.getATR();
	}

	/**
	 * Whether this service is open.
	 * 
	 * @return a boolean
	 */
	public synchronized boolean isOpen() {
		return service.isOpen();
	}

	/**
	 * TO CLARIFY: If the card responds with a status word other than 0x9000,
	 * ie. an staus word indicating an error, this method does NOT throw a
	 * CardServiceException, but it returns this as error code as result.
	 * Right? This can cause confusion, as most other method DO translate any
	 * status words indicating errors into CardServiceExceptions.
	 */
	public synchronized ResponseAPDU transmit(CommandAPDU capdu) throws CardServiceException {
		return service.transmit(capdu);
	}

	public byte[] getATR() {
		return atr;
	}

	public void close() {
		if (service != null) {
			service.close();
		}
	}

	public void setService(CardService service) {
		this.service = service;
	}

	public void addAPDUListener(APDUListener l) {
		service.addAPDUListener(l);
	}

	public void removeAPDUListener(APDUListener l) {
		service.removeAPDUListener(l);
	}

	public void sendSelectApplet() throws CardServiceException {
		short sw = sendSelectApplet(APPLET_AID);
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Could not select MRTD application. SW = " + Integer.toHexString(sw), sw);
		}
	}

	private ResponseAPDU transmit(SecureMessagingWrapper wrapper, CommandAPDU capdu) throws CardServiceException {
		CommandAPDU plainCapdu = capdu;
		if (wrapper != null) {
			capdu = wrapper.wrap(capdu);
		}
		ResponseAPDU rapdu = transmit(capdu);
		short sw = (short)rapdu.getSW();
		if (wrapper != null) {
			try {
				if (rapdu.getBytes().length == 2) {
					throw new CardServiceException("Exception during transmission of wrapped APDU"
							+ "\nC=" + Hex.bytesToHexString(plainCapdu.getBytes()), sw);
				} else {
					rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
				}
			} catch (Exception e) {
				if (e instanceof CardServiceException) {
					throw (CardServiceException)e;
				} else {
					throw new CardServiceException("Exception during transmission of wrapped APDU"
							+ "\nC=" + Hex.bytesToHexString(plainCapdu.getBytes())
							+ "\n" + e.getMessage()
							, sw);
				}
			} finally {
				notifyExchangedPlainTextAPDU(++plainAPDUCount, plainCapdu, rapdu);				
			}
		}

		//		if ((sw & ISO7816.SW_CORRECT_LENGTH_00) == ISO7816.SW_CORRECT_LENGTH_00) {
		//			/* Re-transmit with corrected length if incorrect length. */
		//			int ne = (sw & 0xFF);
		//			plainCapdu = new CommandAPDU(plainCapdu.getCLA(), plainCapdu.getINS(), plainCapdu.getP1(), plainCapdu.getP2(), plainCapdu.getData(), ne);
		//			if (wrapper != null) {
		//				capdu = wrapper.wrap(plainCapdu);
		//			}
		//			rapdu = transmit(capdu);
		//			if (wrapper != null) {
		//				rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
		//				notifyExchangedPlainTextAPDU(++plainAPDUCount, plainCapdu, rapdu);
		//			}
		//		}

		return rapdu;
	}

	/**
	 * Sends a <code>SELECT APPLET</code> command to the card.
	 * 
	 * @param aid
	 *            the applet to select
	 * 
	 * @return status word
	 */
	public synchronized short sendSelectApplet(byte[] aid) throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816,ISO7816.INS_SELECT_FILE, (byte) 0x04, (byte) 0x0C, aid);
		ResponseAPDU rapdu = transmit(capdu);
		return (short)rapdu.getSW(); 
	}

	public synchronized void sendSelectFile(short fid) throws CardServiceException {
		sendSelectFile(null, fid);
	}

	/**
	 * Sends a <code>SELECT FILE</code> command to the passport. Secure
	 * messaging will be applied to the command and response apdu.
	 * 
	 * @param wrapper
	 *            the secure messaging wrapper to use
	 * @param fid
	 *            the file to select
	 */
	public synchronized void sendSelectFile(SecureMessagingWrapper wrapper, short fid) throws CardServiceException {
		byte[] fiddle = { (byte) ((fid >> 8) & 0xFF), (byte) (fid & 0xFF) };
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_SELECT_FILE, (byte) 0x02, (byte) 0x0c, fiddle, 0);
		ResponseAPDU rapdu = transmit(wrapper, capdu);

		if( rapdu == null ) {
			return;
		}

		checkStatusWordAfterFileOperation(capdu, rapdu);
	}

	/**
	 * Sends a <code>READ BINARY</code> command to the passport.
	 * 
	 * @param offset
	 *            offset into the file
	 * @param le
	 *            the expected length of the file to read
	 * 
	 * @return a byte array of length <code>le</code> with (the specified part
	 *         of) the contents of the currently selected file
	 *         
	 * @throws CardServiceException if the command was not successful
	 */
	public synchronized byte[] sendReadBinary(short offset, int le, boolean longRead) throws CardServiceException {
		return sendReadBinary(null, offset, le, longRead);
	}

	/**
	 * Sends a <code>READ BINARY</code> command to the passport. Secure
	 * messaging will be applied to the command and response apdu.
	 * 
	 * @param wrapper
	 *            the secure messaging wrapper to use
	 * @param offset
	 *            offset into the file
	 * @param le
	 *            the expected length of the file to read
	 * @param isExtendedLength
	 *            whether it should be a long (INS=B1) read
	 * 
	 * @return a byte array of length at most <code>le</code> with (the specified part
	 *         of) the contents of the currently selected file
	 * 
	 * @throws CardServiceException if the command was not successful
	 */
	public synchronized byte[] sendReadBinary(SecureMessagingWrapper wrapper, int offset, int le, boolean isExtendedLength) throws CardServiceException {
		//		boolean retrySending = false;
		CommandAPDU capdu = null;
		ResponseAPDU rapdu = null;
		//		do {
		//			retrySending = false;
		// In case the data ended right on the block boundary
		if (le == 0) {
			return null;
		}
		// In the case of long read 2/3 less bytes of the actual data will be returned,
		// because a tag and length will be sent along, here we need to account for this
		if (isExtendedLength) {
			if (le < 128) {
				le += 2;
			} else if (le < 256) {
				le += 3;
			}
			if (le > 256) { le = 256; }
		}
		byte offsetHi = (byte)((offset & 0xFF00) >> 8);
		byte offsetLo = (byte)(offset & 0xFF);
		if (isExtendedLength) {
			byte[] data = new byte[] { 0x54, 0x02, offsetHi, offsetLo };
			capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_READ_BINARY2, 0, 0, data, le);
		} else {
			capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_READ_BINARY, offsetHi, offsetLo, le);
		}

		short sw = ISO7816.SW_UNKNOWN;
		try {
			rapdu = transmit(wrapper, capdu);
			sw = (short)rapdu.getSW();
		} catch (CardServiceException cse) {
			sw = (short)cse.getSW();
		}

		/* There are 3 cases according to R2-p1_v2_sIII_0039... */
		//			if (sw == ISO7816.SW_NO_ERROR) {
		//				/* sw == 0x9000, no need to try again. */
		//				retrySending = false;
		//			} else if (sw == ISO7816.SW_END_OF_FILE) {
		//				/* sw == 0x6282 means EOF, try again with shorter le. */
		//				le--;
		//				retrySending = true;
		//			} else if ((sw & ISO7816.SW_CORRECT_LENGTH_00) == ISO7816.SW_CORRECT_LENGTH_00) {
		//				/* sw == 0x6Cxx means xx is correct length, try again with that le. */
		//				/* NOTE: the transmit method also does retransmission on 6Cxx. */
		//				le = sw & 0xFF;
		//				retrySending = true;
		//			} else {
		//				/* All other cases. */
		//				if (sw == ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED) {
		//					/* No access, fail READ BINARY, throw a CSE. */
		//					throw new CardServiceException("Security status not satisfied during READ BINARY", sw);
		//				} else {
		//					/* Unexpected sw, don't throw exception, but log. */
		//					LOGGER.warning("Unhandled case for status word in READ BINARY, sw == " + Integer.toHexString(sw));
		//					retrySending = false;
		//				}
		//			}
		//		} while (retrySending);

		byte[] rapduBytes = rapdu == null ? null : rapdu.getData();
		//		short sw = (short)rapdu.getSW(); /* NOTE: Update the SW to the last resent APDU. */
		if (isExtendedLength && sw == ISO7816.SW_NO_ERROR) {
			/* Strip the response off the tag 0x53 and the length field. */
			byte[] data = rapduBytes;
			int index = 0;
			if(data[index++] != (byte)0x53) {
				throw new CardServiceException("Malformed read binary long response data", sw);
			}
			if((byte)(data[index] & 0x80) == (byte)0x80) {
				index += (data[index] & 0xF);
			}
			index ++;
			rapduBytes = new byte[data.length - index];
			System.arraycopy(data, index, rapduBytes, 0, rapduBytes.length);
		}

		if (rapduBytes == null | rapduBytes.length == 0) {
			LOGGER.warning("DEBUG: rapduBytes = " + Arrays.toString(rapduBytes) + ", le = " + le + ", sw = " + Integer.toHexString(sw));
		}

		checkStatusWordAfterFileOperation(capdu, rapdu);
		return rapduBytes;
	}

	/**
	 * Sends a <code>GET CHALLENGE</code> command to the passport.
	 * 
	 * @return a byte array of length 8 containing the challenge
	 */
	public synchronized byte[] sendGetChallenge() throws CardServiceException {
		return sendGetChallenge(null);
	}

	/**
	 * Sends a <code>GET CHALLENGE</code> command to the passport.
	 * 
	 * @return a byte array of length 8 containing the challenge
	 */
	public synchronized byte[] sendGetChallenge(SecureMessagingWrapper wrapper) throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_GET_CHALLENGE, 0x00, 0x00, 8);
		ResponseAPDU rapdu = transmit(wrapper, capdu);
		return rapdu.getData();
	}

	/**
	 * Sends an <code>INTERNAL AUTHENTICATE</code> command to the passport.
	 * This is part of AA.
	 * 
	 * @param wrapper
	 *            secure messaging wrapper
	 * @param rndIFD
	 *            the challenge to send
	 * 
	 * @return the response from the passport (status word removed)
	 */
	public synchronized byte[] sendInternalAuthenticate(SecureMessagingWrapper wrapper, byte[] rndIFD) throws CardServiceException {
		if (rndIFD == null || rndIFD.length != 8) { throw new IllegalArgumentException("rndIFD wrong length"); }
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_INTERNAL_AUTHENTICATE, 0x00, 0x00, rndIFD, 256);
		ResponseAPDU rapdu = transmit(wrapper, capdu);
		return rapdu.getData();
	}

	/**
	 * Sends an <code>EXTERNAL AUTHENTICATE</code> command to the passport.
	 * This is part of BAC.
	 * The resulting byte array has length 32 and contains <code>rndICC</code>
	 * (first 8 bytes), <code>rndIFD</code> (next 8 bytes), their key material "
	 * <code>kICC</code>" (last 16 bytes).
	 * 
	 * @param rndIFD
	 *            our challenge
	 * @param rndICC
	 *            their challenge
	 * @param kIFD
	 *            our key material
	 * @param kEnc
	 *            the static encryption key
	 * @param kMac
	 *            the static mac key
	 * 
	 * @return a byte array of length 32 containing the response that was sent
	 *         by the passport, decrypted (using <code>kEnc</code>) and verified
	 *         (using <code>kMac</code>)
	 */
	public synchronized byte[] sendMutualAuth(byte[] rndIFD, byte[] rndICC, byte[] kIFD, SecretKey kEnc, SecretKey kMac) throws CardServiceException {
		try {
			if (rndIFD == null || rndIFD.length != 8) { throw new IllegalArgumentException("rndIFD wrong length"); }
			if (rndICC == null || rndICC.length != 8) { rndICC = new byte[8]; }
			if (kIFD == null || kIFD.length != 16) { throw new IllegalArgumentException("kIFD wrong length"); }
			if (kEnc == null) { throw new IllegalArgumentException("kEnc == null"); }
			if (kMac == null) { throw new IllegalArgumentException("kMac == null"); }

			cipher.init(Cipher.ENCRYPT_MODE, kEnc, ZERO_IV_PARAM_SPEC);
			/*
			 * cipher.update(rndIFD); cipher.update(rndICC); cipher.update(kIFD); //
			 * This doesn't work, apparently we need to create plaintext array. //
			 * Probably has something to do with ZERO_IV_PARAM_SPEC.
			 */
			byte[] plaintext = new byte[32];
			System.arraycopy(rndIFD, 0, plaintext, 0, 8);
			System.arraycopy(rndICC, 0, plaintext, 8, 8);
			System.arraycopy(kIFD, 0, plaintext, 16, 16);
			byte[] ciphertext = cipher.doFinal(plaintext);
			if (ciphertext.length != 32) {
				throw new IllegalStateException("Cryptogram wrong length " + ciphertext.length);
			}

			mac.init(kMac);
			byte[] mactext = mac.doFinal(Util.pad(ciphertext));
			if (mactext.length != 8) {
				throw new IllegalStateException("MAC wrong length");
			}

			byte p1 = (byte) 0x00;
			byte p2 = (byte) 0x00;

			byte[] data = new byte[32 + 8];
			System.arraycopy(ciphertext, 0, data, 0, 32);
			System.arraycopy(mactext, 0, data, 32, 8);
			int le = 40; /* 40 means max ne is 40 (0x28). */ 
			CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_EXTERNAL_AUTHENTICATE, p1, p2, data, le);
			ResponseAPDU rapdu = transmit(capdu);

			byte[] rapduBytes = rapdu.getBytes();
			short sw = (short)rapdu.getSW();
			if (rapduBytes == null) {
				throw new CardServiceException("Mutual authentication failed", sw);
			}

			/* Some MRTDs apparently don't support 40 here, try again with 0. See R2-p1_v2_sIII_0035 (and other issues). */
			if (sw != ISO7816.SW_NO_ERROR) {
				le = 0; /* 0 means ne is max 256 (0xFF). */
				capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_EXTERNAL_AUTHENTICATE, p1, p2, data, le);
				rapdu = transmit(capdu);
				rapduBytes = rapdu.getBytes();
				sw = (short)rapdu.getSW();
			}

			if (rapduBytes.length != 42) {
				throw new CardServiceException("Mutual authentication failed: expected length: 40 + 2, actual length: " + rapduBytes.length, sw);
			}

			/*
			 * byte[] eICC = new byte[32]; System.arraycopy(rapdu, 0, eICC, 0, 32);
			 * 
			 * byte[] mICC = new byte[8]; System.arraycopy(rapdu, 32, mICC, 0, 8);
			 */

			/* Decrypt the response. */
			cipher.init(Cipher.DECRYPT_MODE, kEnc, ZERO_IV_PARAM_SPEC);
			byte[] result = cipher.doFinal(rapduBytes, 0, rapduBytes.length - 8 - 2);
			if (result.length != 32) {
				throw new IllegalStateException("Cryptogram wrong length " + result.length);
			}
			return result;
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		}
	}

	/**
	 * Sends the EXTERNAL AUTHENTICATE command.
	 * This is used in EAC-TA.
	 * 
	 * @param wrapper
	 *            secure messaging wrapper
	 * @param signature
	 *            terminal signature
	 * @throws CardServiceException
	 *             if the resulting status word different from 9000
	 */
	public synchronized void sendMutualAuthenticate(SecureMessagingWrapper wrapper, byte[] signature)
			throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_EXTERNAL_AUTHENTICATE, 0, 0, signature);
		ResponseAPDU rapdu = transmit(wrapper, capdu);
		short sw = (short)rapdu.getSW();
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending External Authenticate failed.", sw);
		}
	}

	/**
	 * The MSE KAT APDU, see EAC 1.11 spec, Section B.1
	 * 
	 * @param wrapper
	 *            secure messaging wrapper
	 * @param keyData
	 *            key data object (tag 0x91)
	 * @param idData
	 *            key id data object (tag 0x84), can be null
	 * @throws CardServiceException
	 *             on error
	 */
	public synchronized void sendMSEKAT(SecureMessagingWrapper wrapper,
			byte[] keyData, byte[] idData) throws CardServiceException {
		byte[] data = new byte[keyData.length
		                       + ((idData != null) ? idData.length : 0)];
		System.arraycopy(keyData, 0, data, 0, keyData.length);
		if (idData != null) {
			System.arraycopy(idData, 0, data, keyData.length, idData.length);
		}

		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_MSE, 0x41, 0xA6, data);
		ResponseAPDU rapdu = transmit(wrapper, capdu);
		short sw = (short)rapdu.getSW();
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending MSE KAT failed", sw);
		}
	}

	/**
	 * The MSE DST APDU, see EAC 1.11 spec, Section B.2
	 * 
	 * @param wrapper
	 *            secure messaging wrapper
	 * @param data
	 *            public key reference data object (tag 0x83)
	 * @throws CardServiceException
	 *             on error
	 */
	public synchronized void sendMSESetDST(SecureMessagingWrapper wrapper, byte[] data) throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_MSE, 0x81, 0xB6, data);
		ResponseAPDU rapdu = transmit(wrapper, capdu);
		short sw = (short)rapdu.getSW();
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending MSE Set DST failed", sw);
		}
	}

	/**
	 * The MSE AT APDU for TA, see EAC 1.11 spec, Section B.2.
	 * Note that caller is responsible for prefixing the byte[] params with specified tags.
	 * 
	 * @param wrapper secure messaging wrapper
	 * @param data public key reference data object (should already be prefixed with tag 0x83)
	 *
	 * @throws CardServiceException on error
	 */
	public synchronized void sendMSESetATExtAuth(SecureMessagingWrapper wrapper, byte[] data) throws CardServiceException {
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_MSE, 0x81, 0xA4, data);
		ResponseAPDU rapdu = transmit(wrapper, capdu);
		short sw = (short)rapdu.getSW();
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending MSE AT failed", sw);
		}
	}

	/**
	 * The MSE AT APDU for PACE, see ICAO TR-SAC-1.01, Section 3.2.1, BSI TR 03110 v2.03 B11.1.
	 * Note that (for now) caller is responsible for prefixing the byte[] params with specified tags.
	 * 
	 * @param wrapper secure messaging wrapper
	 * @param oid OID of the protocol to select (this method will prefix <code>0x80</code>)
	 * @param refPublicKeyOrSecretKey value specifying whether to use MRZ (<code>0x01</code>) or CAN (<code>0x02</code>) (this method will prefix <code>0x83</code>)
	 * @param refPrivateKeyOrForComputingSessionKey indicates a private key or reference for computing a session key (this method will prefix <code>0x84</code>)
	 *
	 * @throws CardServiceException on error
	 */
	public synchronized void sendMSESetATMutualAuth(SecureMessagingWrapper wrapper, String oid,
			int refPublicKeyOrSecretKey, byte[] refPrivateKeyOrForComputingSessionKey) throws CardServiceException {

		if (oid == null) { throw new IllegalArgumentException("OID cannot be null"); }

		/*
		 * 0x80 Cryptographic mechanism reference
		 * Object Identifier of the protocol to select (value only, tag 0x06 is omitted).
		 */
		byte[] oidBytes = null;
		try {
			TLVInputStream oidTLVIn = new TLVInputStream(new ByteArrayInputStream(new ASN1ObjectIdentifier(oid).getEncoded()));
			oidTLVIn.readTag(); /* Should be 0x06 */
			oidTLVIn.readLength();
			oidBytes = oidTLVIn.readValue();
			oidTLVIn.close();
			oidBytes = Util.wrapDO((byte)0x80, oidBytes); /* FIXME: define constant for 0x80. */
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Illegal OID: " + oid + " (" + ioe.getMessage() + ")");
		}

		/*
		 * 0x83 Reference of a public key / secret key.
		 * The password to be used is indicated as follows: 0x01: MRZ, 0x02: CAN.
		 */
		if (!(refPublicKeyOrSecretKey == MRZ_PACE_KEY_REFERENCE
				|| refPublicKeyOrSecretKey == CAN_PACE_KEY_REFERENCE
				|| refPublicKeyOrSecretKey == PIN_PACE_KEY_REFERENCE
				|| refPublicKeyOrSecretKey == PUK_PACE_REFERENCE)) { throw new IllegalArgumentException("Unsupported key type reference (MRZ, CAN, etc), found " + refPublicKeyOrSecretKey); }

		byte[] refPublicKeyOrSecretKeyBytes = Util.wrapDO((byte)0x83, new byte[] { (byte)refPublicKeyOrSecretKey }); /* FIXME: define constant for 0x83 */

		/*
		 * 0x84 Reference of a private key / Reference for computing a
		 * session key.
		 * This data object is REQUIRED to indicate the identifier
		 * of the domain parameters to be used if the domain
		 * parameters are ambiguous, i.e. more than one set of
		 * domain parameters is available for PACE.
		 */
		if (refPrivateKeyOrForComputingSessionKey != null) {
			refPrivateKeyOrForComputingSessionKey = Util.wrapDO((byte)0x84, refPrivateKeyOrForComputingSessionKey);
		}

		/* Construct data. */
		ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
		try {
			dataOutputStream.write(oidBytes);
			dataOutputStream.write(refPublicKeyOrSecretKeyBytes);
			if (refPrivateKeyOrForComputingSessionKey != null) {
				dataOutputStream.write(refPrivateKeyOrForComputingSessionKey);
			}
		} catch (IOException ioe) {
			/* NOTE: should never happen. */
			LOGGER.severe("Error while copying data");
			ioe.printStackTrace();
			throw new IllegalStateException("Error while copying data");
		}
		byte[] data = dataOutputStream.toByteArray();

		/* Tranceive APDU. */
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_MSE, 0xC1, 0xA4, data);
		ResponseAPDU rapdu = transmit(wrapper, capdu);

		/* Handle error status word. */
		short sw = (short)rapdu.getSW();
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending MSE AT failed", sw);
		}
	}

	/**
	 * Sends a General Authenticate command.
	 * 
	 * FIXME: WORK IN PROGRESS...
	 * 
	 * @param wrapper secure messaging wrapper
	 * @param data data to be sent, without the <code>0x7C</code> prefix (this method will add it)
	 * @return dynamic authentication data without the <code>0x7C</code> prefix (this method will remove it)
	 * 
	 * @throws CardServiceException on error
	 */
	public synchronized byte[] sendGeneralAuthenticate(SecureMessagingWrapper wrapper, byte[] data, boolean isLast) throws CardServiceException {
		/* Tranceive APDU. */
		byte[] commandData = Util.wrapDO((byte)0x7C, data);
		CommandAPDU capdu = new CommandAPDU(isLast ? ISO7816.CLA_ISO7816 : ISO7816.CLA_COMMAND_CHAINING, INS_PACE_GENERAL_AUTHENTICATE, 0x00, 0x00, commandData, 256);
		ResponseAPDU rapdu = transmit(wrapper, capdu);

		/* Handle error status word. */
		short sw = (short)rapdu.getSW();
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending general authenticate failed", sw);
		}
		byte[] responseData = rapdu.getData();
		responseData = Util.unwrapDO((byte)0x7C, responseData);
		return responseData;
	}

	public synchronized void sendPSOExtendedLengthMode(SecureMessagingWrapper wrapper, byte[] certBodyData, byte[] certSignatureData)
			throws CardServiceException {
		byte[] certData = new byte[certBodyData.length + certSignatureData.length];
		System.arraycopy(certBodyData, 0, certData, 0, certBodyData.length);
		System.arraycopy(certSignatureData, 0, certData, certBodyData.length, certSignatureData.length);

		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_PSO, 0, 0xBE, certData);
		ResponseAPDU rapdu = transmit(wrapper, capdu);
		short sw = (short)rapdu.getSW();
		if (sw != ISO7816.SW_NO_ERROR) { throw new CardServiceException("Sending PSO failed", sw); }
	}

	public synchronized void sendPSOChainMode(SecureMessagingWrapper wrapper, byte[] certBodyData, byte[] certSignatureData)
			throws CardServiceException {
		byte[] certData = new byte[certBodyData.length + certSignatureData.length];
		System.arraycopy(certBodyData, 0, certData, 0, certBodyData.length);
		System.arraycopy(certSignatureData, 0, certData, certBodyData.length, certSignatureData.length);
		int maxBlock = 223;
		int blockSize = 223;
		int offset = 0;
		int length = certData.length;
		if (certData.length > maxBlock) {
			int numBlock = certData.length / blockSize;
			if (numBlock * blockSize < certData.length)
				numBlock++;
			int i = 0;
			while (i < numBlock - 1) {
				CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816 | 0x10, ISO7816.INS_PSO, 0x00, 0xBE, certData, offset, length);
				ResponseAPDU rapdu = transmit(wrapper, capdu);
				short sw = (short)rapdu.getSW();
				if (sw != ISO7816.SW_NO_ERROR) {
					throw new CardServiceException("Sending PSO failed", sw);
				}
				length -= blockSize;
				offset += blockSize;
				i++;
			}
		}
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816 | 0x00, ISO7816.INS_PSO, 0x00, 0xBE, certData, offset, length);
		ResponseAPDU rapdu = transmit(wrapper, capdu);

		short sw = (short)rapdu.getSW();
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending PSO failed", sw);
		}
	}

	public void addPlainTextAPDUListener(APDUListener l) {
		if (plainTextAPDUListeners != null) { plainTextAPDUListeners.add(l); }
	}

	public void removePlainTextAPDUListener(APDUListener l) {
		if (plainTextAPDUListeners != null) { plainTextAPDUListeners.add(l); }
	}

	/**
	 * Notifies listeners about APDU event.
	 * 
	 * @param capdu APDU event
	 */
	protected void notifyExchangedPlainTextAPDU(int count, CommandAPDU capdu, ResponseAPDU rapdu) {
		for (APDUListener listener: plainTextAPDUListeners) {
			listener.exchangedAPDU(new APDUEvent(this, "PLAINTEXT", count, capdu, rapdu));
		}
	}

	private static void checkStatusWordAfterFileOperation(CommandAPDU capdu, ResponseAPDU rapdu) throws CardServiceException {
		short sw = (short)rapdu.getSW();
		String commandResponseMessage = "CAPDU = " + Hex.bytesToHexString(capdu.getBytes()) + ", RAPDU = " + Hex.bytesToHexString(rapdu.getBytes());
		switch(sw) {
		case ISO7816.SW_NO_ERROR:
			return;
		case ISO7816.SW_FILE_NOT_FOUND:
			throw new CardServiceException("File not found, " + commandResponseMessage, sw);
		case ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED:
		case ISO7816.SW_CONDITIONS_NOT_SATISFIED:
		case ISO7816.SW_COMMAND_NOT_ALLOWED:
			throw new CardServiceException("Access to file denied, " + commandResponseMessage, sw);
		}
		throw new CardServiceException("Error occured, " + commandResponseMessage, sw);
	}
}
