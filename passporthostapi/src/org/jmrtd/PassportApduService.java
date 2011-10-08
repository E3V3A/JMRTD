/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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

import java.security.GeneralSecurityException;
import java.security.Provider;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.IResponseAPDU;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.smartcards.ScubaSmartcards;
import net.sourceforge.scuba.util.Hex;

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
 * @author Cees-Bart Breunesse (ceesb@riscure.com)
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class PassportApduService<C,R> extends CardService<C,R>
{
	private static final long serialVersionUID = 2451509825132976178L;

	private static final Provider JMRTD_PROVIDER = JMRTDSecurityProvider.getInstance();
	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	/** The applet we select when we start a session. */
	private static final byte[] APPLET_AID = { (byte) 0xA0, 0x00, 0x00, 0x02, 0x47, 0x10, 0x01 };

	/** Initialization vector used by the cipher below. */
	private static final IvParameterSpec ZERO_IV_PARAM_SPEC = new IvParameterSpec(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

	/** The service we decorate. */
	private CardService<C,R> service;

	/** DESede encryption/decryption cipher. */
	private transient Cipher cipher;

	/** ISO9797Alg3Mac. */
	private transient Mac mac;

	/** Usage of the ScubaSmartcard Abstractions  */
	private ScubaSmartcards<C, R> sc;

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
	public PassportApduService(CardService<C,R> service) throws CardServiceException {
		sc = ScubaSmartcards.getInstance();
		this.service = service;
		try {
			mac = Mac.getInstance("ISO9797Alg3Mac");
			cipher = Cipher.getInstance("DESede/CBC/NoPadding");
		} catch (GeneralSecurityException gse) {
			gse.printStackTrace();
			throw new CardServiceException(gse.toString());
		}
	}

	/**
	 * Opens a session by connecting to the card and selecting the passport
	 * applet.
	 */
	public void open() throws CardServiceException {
		if (!service.isOpen()) {
			service.open();
		}
		System.out.println("DEBUG: sending select!");
		sendSelectApplet();
	}

	/**
	 * Whether this service is open.
	 * 
	 * @return a boolean
	 */
	public synchronized boolean isOpen() {
		return service.isOpen();
	}

	private void sendSelectApplet() throws CardServiceException {
		int sw = sendSelectApplet(APPLET_AID);
		if (sw != 0x00009000) {
			throw new CardServiceException("Could not select passport");
		}
	}

	/**
	 * TO CLARIFY: If the card responds with a status word other than 0x9000,
	 * ie. an staus word indicating an error, this method does NOT throw a
	 * CardServiceException, but it returns this as error code as result.
	 * Right? This can cause confusion, as most other method DO translate any
	 * status words indicating errors into CardServiceExceptions.
	 */
	public synchronized R transmit(C capdu)
	throws CardServiceException {
		return service.transmit(capdu);
	}

	private R transmit(SecureMessagingWrapper<C,R> wrapper, C capdu) throws CardServiceException {
		if (wrapper != null) {
			capdu = wrapper.wrap(capdu);
		}
		R rapdu = transmit(capdu);
		if (wrapper != null) {
			rapdu = wrapper.unwrap(rapdu, sc.accesR(rapdu).getBytes().length);
		}
		return rapdu;
	}

	public void close() {
		if (service != null) {
			service.close();
		}
	}

	public void setService(CardService<C,R> service) {
		this.service = service;
	}

	public void addAPDUListener(APDUListener<C,R> l) {
		service.addAPDUListener(l);
	}

	public void removeAPDUListener(APDUListener<C,R> l) {
		service.removeAPDUListener(l);
	}

	C createSelectAppletAPDU(byte[] aid) {

		byte[] data = aid;
		// CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816,
		// ISO7816.INS_SELECT_FILE, (byte) 0x04, (byte) 0x0C, data,
		// (byte) 0x01);

		//C apdu = new CommandAPDU(ISO7816.CLA_ISO7816,ISO7816.INS_SELECT_FILE, (byte) 0x04, (byte) 0x0C, data);
		C apdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816,ISO7816.INS_SELECT_FILE, (byte) 0x04, (byte) 0x0C, data);
		return apdu;
	}

	C createSelectFileAPDU(short fid) {
		byte[] fiddle = { (byte) ((fid >> 8) & 0x000000FF),
				(byte) (fid & 0x000000FF) };
		return createSelectFileAPDU(fiddle);
	}

	private C createSelectFileAPDU(byte[] fid) {
		C apdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_SELECT_FILE, (byte) 0x02, (byte) 0x0c, fid, 0);
		return apdu;
	}

	public C createReadBinaryAPDU(int offset, int le, boolean longRead) {
		byte off1 = (byte) ((offset & 0x0000FF00) >> 8);
		byte off2 = (byte) (offset & 0x000000FF);
		if (longRead) {
			byte[] data = new byte[] { 0x54, 0x02, off1, off2 };
			return sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_READ_BINARY2, 0, 0, data, le);
		} else {
			return sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_READ_BINARY, off1, off2, le);
		}
	}

	protected C createGetChallengeAPDU() {
		byte p1 = (byte) 0x00;
		byte p2 = (byte) 0x00;
		int le = 8;
		C apdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_GET_CHALLENGE, p1, p2, le);
		return apdu;
	}

	C createInternalAuthenticateAPDU(byte[] rndIFD) {
		if (rndIFD == null || rndIFD.length != 8) {
			throw new IllegalArgumentException("rndIFD wrong length");
		}
		byte p1 = (byte) 0x00;
		byte p2 = (byte) 0x00;
		byte[] data = rndIFD;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                
		int le = 256; /* FIXME: needs to be 256 for OmniKey for some reason? -- MO */

		C apdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_INTERNAL_AUTHENTICATE, p1, p2, data, le);
		return apdu;
	}

	/**
	 * Creates an <code>EXTERNAL AUTHENTICATE</code> command.
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
	 * @return the apdu to be sent to the card.
	 */
	C createMutualAuthAPDU(byte[] rndIFD, byte[] rndICC, byte[] kIFD,
			SecretKey kEnc, SecretKey kMac) throws GeneralSecurityException {
		if (rndIFD == null || rndIFD.length != 8) {
			throw new IllegalArgumentException("rndIFD wrong length");
		}
		if (rndICC == null || rndICC.length != 8) {
			// throw new IllegalArgumentException("rndICC wrong length");
			rndICC = new byte[8];
		}
		if (kIFD == null || kIFD.length != 16) {
			throw new IllegalArgumentException("kIFD wrong length");
		}
		if (kEnc == null) {
			throw new IllegalArgumentException("kEnc == null");
		}
		if (kMac == null) {
			throw new IllegalArgumentException("kMac == null");
		}

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
			throw new IllegalStateException("Cryptogram wrong length "
					+ ciphertext.length);
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
		int le = 40;

		C apdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_EXTERNAL_AUTHENTICATE, p1, p2, data, le);
		return apdu;
	}

	/**
	 * Creates the EXTERNAL AUTHENTICATE command for EAC.
	 * 
	 * @param signature
	 *            the challange signed by the terminal
	 * @return command APDU
	 */
	C createMutualAuthAPDU(byte[] signature) {
		return sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_EXTERNAL_AUTHENTICATE, 0, 0, signature);
	}

	/**
	 * Sends a <code>SELECT APPLET</code> command to the card.
	 * 
	 * @param aid
	 *            the applet to select
	 * 
	 * @return status word
	 */
	public synchronized int sendSelectApplet(byte[] aid)
	throws CardServiceException {
		R r = transmit(createSelectAppletAPDU(aid));

		return sc.accesR(r).getSW(); 
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
	public synchronized void sendSelectFile(SecureMessagingWrapper<C,R> wrapper, short fid) throws CardServiceException {
		C capdu = createSelectFileAPDU(fid);
		R rapdu = transmit(wrapper, capdu);

		if( rapdu == null ) {
			return;
		}

		short sw = (short) sc.accesR(rapdu).getSW();
		if (sw == ISO7816.SW_FILE_NOT_FOUND) {
			throw new CardServiceException("File not found.");
		}
		if (sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Error occured, SW=" + Integer.toHexString(sw) + " command was " + Hex.bytesToHexString(sc.accesC(capdu).getBytes()));
		}
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
	 */
	public synchronized byte[] sendReadBinary(short offset, int le)
	throws CardServiceException {
		return sendReadBinary(null, offset, le, false);
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
	 * @param longRead
	 *            whether it should be a long (INS=B1) read
	 * 
	 * @return a byte array of length <code>le</code> with (the specified part
	 *         of) the contents of the currently selected file
	 */
	public synchronized byte[] sendReadBinary(SecureMessagingWrapper<C,R> wrapper,
			int offset, int le, boolean longRead) throws CardServiceException {

		boolean repeatOnEOF = false;
		R rapdu = null;
		IResponseAPDU rAcc = null;
		do {
			repeatOnEOF = false;
			// In case the data ended right on the block boundary
			if (le == 0) {
				return null;
			}
			// In the case of long read 2/3 less bytes of the actual data will be returned,
			// because a tag and length will be sent along, here we need to account for this
			if(longRead) {
				if(le < 128) {
					le += 2;
				}else if (le < 256) {
					le += 3;
				}
				if(le > 256) le = 256;
			}
			C capdu = createReadBinaryAPDU(offset, le, longRead);
			rapdu = transmit(wrapper, capdu);

			rAcc = sc.accesR(rapdu);

			int sw = rAcc.getSW();
			if (sw == ISO7816.SW_END_OF_FILE) {
				le--;
				repeatOnEOF = true;
			}
			if (sw == ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED) {
				throw new CardServiceException("Security status not satisfied", sw);
			}
		} while (repeatOnEOF);
		byte[] r = rAcc.getData();
		if(longRead && (short)rAcc.getSW() == ISO7816.SW_NO_ERROR) {
			// Strip the response off the tag 0x53 and the length field
			byte[] data = r;
			int index = 0;
			if(data[index++] != (byte)0x53) {
				throw new CardServiceException("Malformed read binary long response data.");
			}
			if((byte)(data[index] & 0x80) == (byte)0x80) {
				index += (data[index] & 0xF);
			}
			index ++;
			r = new byte[data.length - index];
			System.arraycopy(data, index, r, 0, r.length);
		}
		return r;
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
	public synchronized byte[] sendGetChallenge(SecureMessagingWrapper<C,R> wrapper) throws CardServiceException {
		C capdu = createGetChallengeAPDU();
		R rapdu = transmit(wrapper, capdu);

		return sc.accesR(rapdu).getData();
	}

	/**
	 * Sends an <code>INTERNAL AUTHENTICATE</code> command to the passport.
	 * 
	 * @param wrapper
	 *            secure messaging wrapper
	 * @param rndIFD
	 *            the challenge to send
	 * 
	 * @return the response from the passport (status word removed)
	 */
	public synchronized byte[] sendInternalAuthenticate(
			SecureMessagingWrapper<C,R> wrapper, byte[] rndIFD)
	throws CardServiceException {
		C capdu = createInternalAuthenticateAPDU(rndIFD);
		R rapdu = transmit(wrapper, capdu);

		return sc.accesR(rapdu).getData();
	}

	/**
	 * Sends an <code>EXTERNAL AUTHENTICATE</code> command to the passport. The
	 * resulting byte array has length 32 and contains <code>rndICC</code>
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
	public synchronized byte[] sendMutualAuth(byte[] rndIFD, byte[] rndICC,
			byte[] kIFD, SecretKey kEnc, SecretKey kMac)
	throws CardServiceException {
		try {
			R rapdu = transmit(createMutualAuthAPDU(rndIFD, rndICC,	kIFD, kEnc, kMac));
			IResponseAPDU rAcc = sc.accesR(rapdu);
			byte[] rapduBytes = rAcc.getBytes();
			if (rapduBytes == null) {
				throw new CardServiceException("Mutual authentication failed");
			}
			String errorCode = Hex.shortToHexString((short) rAcc.getSW());
			if (rapduBytes.length == 2) {
				throw new CardServiceException(
						"Mutual authentication failed: error code:  "
						+ errorCode, rAcc.getSW());
			}

			if (rapduBytes.length != 42) {
				throw new CardServiceException(
						"Mutual authentication failed: expected length: 42, actual length: "
						+ rapduBytes.length + ", error code: "
						+ errorCode, rAcc.getSW());
			}

			/*
			 * byte[] eICC = new byte[32]; System.arraycopy(rapdu, 0, eICC, 0,
			 * 32);
			 * 
			 * byte[] mICC = new byte[8]; System.arraycopy(rapdu, 32, mICC, 0,
			 * 8);
			 */

			/* Decrypt the response. */
			cipher.init(Cipher.DECRYPT_MODE, kEnc, ZERO_IV_PARAM_SPEC);
			byte[] result = cipher.doFinal(rapduBytes, 0,
					rapduBytes.length - 8 - 2);
			if (result.length != 32) {
				throw new IllegalStateException("Cryptogram wrong length "
						+ result.length);
			}
			return result;
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		}
	}

	/**
	 * Sends the EXTERNAL AUTHENTICATE commands for EAC terminal verification
	 * 
	 * @param wrapper
	 *            secure messaging wrapper
	 * @param signature
	 *            terminal signature
	 * @throws CardServiceException
	 *             if the resulting status word different from 9000
	 */
	public synchronized void sendMutualAuthenticate(
			SecureMessagingWrapper<C,R> wrapper, byte[] signature)
	throws CardServiceException {

		C capdu = createMutualAuthAPDU(signature);
		R rapdu = transmit(wrapper, capdu);
		int sw = sc.accesR(rapdu).getSW();
		if ((short) sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException(
			"Sending External Authenticate failed.");
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
	public synchronized void sendMSEKAT(SecureMessagingWrapper<C,R> wrapper,
			byte[] keyData, byte[] idData) throws CardServiceException {
		byte[] data = new byte[keyData.length
		                       + ((idData != null) ? idData.length : 0)];
		System.arraycopy(keyData, 0, data, 0, keyData.length);
		if (idData != null) {
			System.arraycopy(idData, 0, data, keyData.length, idData.length);
		}

		C capdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_MSE, 0x41, 0xA6, data);
		R rapdu = transmit(wrapper, capdu);
		int sw = sc.accesR(rapdu).getSW();
		if ((short) sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending MSE KAT failed.");
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
	public synchronized void sendMSEDST(SecureMessagingWrapper<C,R> wrapper,
			byte[] data) throws CardServiceException {

		C capdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_MSE, 0x81, 0xB6, data);
		R rapdu = transmit(wrapper, capdu);
		int sw = sc.accesR(rapdu).getSW();
		if ((short) sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending MSE KAT failed.");
		}
	}

	/**
	 * The MSE AT APDU, see EAC 1.11 spec, Section B.2
	 * 
	 * @param wrapper
	 *            secure messaging wrapper
	 * @param data
	 *            public key reference data object (tag 0x83)
	 * @throws CardServiceException
	 *             on error
	 */
	public synchronized void sendMSEAT(SecureMessagingWrapper<C,R> wrapper,
			byte[] data) throws CardServiceException {

		C capdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_MSE, 0x81, 0xA4, data);
		R rapdu = transmit(wrapper, capdu);
		int sw = sc.accesR(rapdu).getSW();
		if ((short) sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending MSE AT failed.");
		}
	}

	public synchronized void sendPSOExtendedLengthMode(SecureMessagingWrapper<C,R> wrapper,
			byte[] certBodyData, byte[] certSignatureData)
	throws CardServiceException {
		byte[] certData = new byte[certBodyData.length
		                           + certSignatureData.length];
		System.arraycopy(certBodyData, 0, certData, 0, certBodyData.length);
		System.arraycopy(certSignatureData, 0, certData, certBodyData.length,
				certSignatureData.length);

		C capdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_PSO, 0, 0xBE, certData);
		R rapdu = transmit(wrapper, capdu);
		int sw = sc.accesR(rapdu).getSW();
		if ((short) sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending PSO failed.");
		}
	}

	public synchronized void sendPSOChainMode(SecureMessagingWrapper<C,R> wrapper,
			byte[] certBodyData, byte[] certSignatureData)
	throws CardServiceException {
		byte[] certData = new byte[certBodyData.length
		                           + certSignatureData.length];
		System.arraycopy(certBodyData, 0, certData, 0, certBodyData.length);
		System.arraycopy(certSignatureData, 0, certData, certBodyData.length,
				certSignatureData.length);
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
				C capdu = createPSOAPDU(certData, offset, blockSize, false);
				R rapdu = transmit(wrapper, capdu);

				int sw = sc.accesR(rapdu).getSW();
				if ((short) sw != ISO7816.SW_NO_ERROR) {
					throw new CardServiceException("Sending PSO failed.");
				}
				length -= blockSize;
				offset += blockSize;
				i++;
			}
		}
		C capdu = createPSOAPDU(certData, offset, length, true);
		R rapdu = transmit(wrapper, capdu);

		int sw = sc.accesR(rapdu).getSW();
		if ((short) sw != ISO7816.SW_NO_ERROR) {
			throw new CardServiceException("Sending PSO failed.");
		}

	}

	/**
	 * Create (possibly chained) APDU for PSO verify certificate (p2 = 0xBE)
	 * 
	 * @param certData
	 *            certificate data
	 * @param offset
	 *            offset to certificate data
	 * @param length
	 *            length of the data to send
	 * @param last
	 *            whether this is the last APDU in chain
	 * @return command APDU
	 */
	C createPSOAPDU(byte[] certData, int offset, int length,
			boolean last) {
		byte p1 = (byte) 0x00;
		byte p2 = (byte) 0xBE;
		byte[] data = new byte[length];
		System.arraycopy(certData, offset, data, 0, length);

		C apdu = sc.createCommandAPDU(ISO7816.CLA_ISO7816 | (last ? 0x00 : 0x10), ISO7816.INS_PSO, p1, p2, data);
		return apdu;
	}
}
