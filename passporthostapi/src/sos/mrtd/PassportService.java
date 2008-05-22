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
 * $Id:PassportService.java 352 2008-05-19 06:55:21Z martijno $
 */

package sos.mrtd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collection;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.smartcardio.CardException;

import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.tlv.BERTLVInputStream;

/**
 * Card service for reading datagroups and using the BAC and AA protocols
 * on the passport.
 * Defines secure messaging.
 * Defines active authentication.
 * 
 * Based on ICAO-TR-PKI and ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt; doBAC(...) ==&gt; doAA() ==&gt; close()
 *    </pre> 
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision:352 $
 */
public class PassportService extends PassportApduService
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

	private static final int SESSION_STOPPED_STATE = 0;
	private static final int SESSION_STARTED_STATE = 1;
	private static final int BAC_AUTHENTICATED_STATE = 2;
	private static final int AA_AUTHENTICATED_STATE = 3;

	private int state;

	private Collection<AuthListener> authListeners;

	protected SecureMessagingWrapper wrapper;
	private Signature aaSignature;
	private MessageDigest aaDigest;
	private Cipher aaCipher;

	/**
	 * Creates a new passport service for accessing the passport.
	 * 
	 * @param service another service which will deal with sending
	 *        the apdus to the card.
	 *
	 * @throws GeneralSecurityException when the available JCE providers
	 *         cannot provide the necessary cryptographic primitives.
	 */
	public PassportService(CardService service)
	throws CardServiceException {
		super(service);
		try {
			aaSignature = Signature.getInstance("SHA1WithRSA/ISO9796-2"); /* FIXME: SHA1WithRSA also works? */
			aaDigest = MessageDigest.getInstance("SHA1");
			aaCipher = Cipher.getInstance("RSA/NONE/NoPadding");
			authListeners = new ArrayList<AuthListener>();
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		}
		state = SESSION_STOPPED_STATE;
	}

	/**
	 * Hack to construct a passport service from a service that is already open.
	 * This should be removed some day.
	 * 
	 * @param service underlying service
	 * @param wrapper encapsulates secure messaging state
	 */
//	public PassportService(CardService service, SecureMessagingWrapper wrapper)
//	throws CardServiceException {
//	this(service);
//	this.wrapper = wrapper;
//	if (state < BAC_AUTHENTICATED_STATE) {
//	state = BAC_AUTHENTICATED_STATE;
//	}
//	}

	/**
	 * Opens a session. This is done by connecting to the card, selecting the
	 * passport applet.
	 */
	public void open() throws CardServiceException {
		if (isOpen()) {
			return;
		}
		super.open();
		state = SESSION_STARTED_STATE;
	}

	public boolean isOpen() {
		return (state != SESSION_STOPPED_STATE);
	}

	/**
	 * Performs the <i>Basic Access Control</i> protocol.
	 *
	 * @param documentNumber the document number
	 * @param dateOfBirth card holder's birth date
	 * @param dateOfExpiry document's expiry date
	 * 
	 * @throws CardServiceException if authentication failed
	 */
	public synchronized void doBAC(String documentNumber, String dateOfBirth, String dateOfExpiry)
	throws CardServiceException {
		try {
			byte[] keySeed = Util.computeKeySeed(documentNumber, dateOfBirth, dateOfExpiry);
			SecretKey kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
			SecretKey kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
			byte[] rndICC = sendGetChallenge();
			byte[] rndIFD = new byte[8]; /* TODO: random */
			byte[] kIFD = new byte[16]; /* TODO: random */
			byte[] response = sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
			byte[] kICC = new byte[16];
			System.arraycopy(response, 16, kICC, 0, 16);
			keySeed = new byte[16];
			for (int i = 0; i < 16; i++) {
				keySeed[i] = (byte) ((kIFD[i] & 0x000000FF) ^ (kICC[i] & 0x000000FF));
			}
			SecretKey ksEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
			SecretKey ksMac = Util.deriveKey(keySeed, Util.MAC_MODE);
			long ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
			wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);
			BACEvent event = new BACEvent(this, rndICC, rndIFD, kICC, kIFD, true);
			notifyBACPerformed(event);
			state = BAC_AUTHENTICATED_STATE;
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		} catch (UnsupportedEncodingException uee) {
			throw new CardServiceException(uee.toString());
		}
	}

	/**
	 * Adds an authentication event listener.
	 * 
	 * @param l listener
	 */
	public void addAuthenticationListener(AuthListener l) {
		authListeners.add(l);
	}

	/**
	 * Removes an authentication event listener.
	 * 
	 * @param l listener
	 */
	public void removeAuthenticationListener(AuthListener l) {
		authListeners.remove(l);
	}

	/**
	 * Notifies listeners about BAC events.
	 * 
	 * @param event BAC event
	 */
	protected void notifyBACPerformed(BACEvent event) {
		for (AuthListener l : authListeners) {
			l.performedBAC(event);
		}
	}

	/**
	 * Performs the <i>Active Authentication</i> protocol.
	 * 
	 * @param pubkey the public key to use (usually read from the card)
	 * 
	 * @return a boolean indicating whether the card was authenticated
	 * 
	 * @throws GeneralSecurityException if something goes wrong
	 */
	public boolean doAA(PublicKey pubkey) throws CardServiceException {
		try {
			aaCipher.init(Cipher.DECRYPT_MODE, pubkey);
			aaSignature.initVerify(pubkey);
			byte[] m2 = new byte[8]; /* TODO: random rndIFD */
			byte[] response = sendInternalAuthenticate(wrapper, m2);
			int digestLength = aaDigest.getDigestLength(); /* should always be 20 */
			byte[] plaintext = aaCipher.doFinal(response);
			byte[] m1 = Util.recoverMessage(digestLength, plaintext);
			aaSignature.update(m1);
			aaSignature.update(m2);
			boolean success = aaSignature.verify(response);
			AAEvent event = new AAEvent(this, pubkey, m1, m2, success);
			notifyAAPerformed(event);
			if (success) {
				state = AA_AUTHENTICATED_STATE;
			}
			return success;
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		}
	}

	/**
	 * Notifies listeners about AA event.
	 * 
	 * @param event AA event.
	 */
	protected void notifyAAPerformed(AAEvent event) {
		for (AuthListener l: authListeners) {
			l.performedAA(event);
		}
	}

	public void close() {
		try {
			wrapper = null;
			super.close();
		} finally {
			state = SESSION_STOPPED_STATE;
		}
	}

	/**
	 * Gets the wrapper. Returns <code>null</code> until
	 * BAC has been performed.
	 * 
	 * @return the wrapper
	 */
	public SecureMessagingWrapper getWrapper() {
		return wrapper;
	}

	/**
	 * @deprecated hack
	 * @param wrapper wrapper
	 */
	public void setWrapper(SecureMessagingWrapper wrapper) {
		this.wrapper = wrapper;
		BACEvent event = new BACEvent(this, null, null, null, null, true);
		notifyBACPerformed(event);
	}


	/**
	 * Gets the file indicated by a file identifier.
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
		// if (!isSelectedFID(fid)) { sendSelectFile(wrapper, fid); }
		sendSelectFile(wrapper, fid);
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
				markedOffset = -1;
			} catch (IOException ioe) {
				ioe.printStackTrace();
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

		public void reset() throws IOException {
			if (markedOffset < 0) { throw new IOException("Mark not set"); }
			offsetBufferInFile = markedOffset;
			offsetInBuffer = 0;
			try {
				buffer = readFromFile(fid, offsetBufferInFile, 8);
			} catch (CardServiceException ce) {
				buffer = new byte[0]; /* NOTE: forces a readFromFile() on next read() */
			}
		}

		public boolean markSupported() {
			return true;
		}
	}
}
