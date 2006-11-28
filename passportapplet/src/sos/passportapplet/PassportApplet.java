/*
 * passportapplet - A reference implementation of the MRTD standards.
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

package sos.passportapplet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.CardRuntimeException;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.MessageDigest;
import javacard.security.RandomData;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

/**
 * PassportApplet
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class PassportApplet extends Applet implements ISO7816 {
    static byte state = 0;

    /* values of state */
    public static final byte CHALLENGED = 1;
    public static final byte MUTUAL_AUTHENTICATED = 2;
    public static final byte FILE_SELECTED = 4;
    public static final byte HAS_MUTUALAUTHENTICATION_KEYS = 8;
    public static final byte HAS_INTERNALAUTHENTICATION_KEYS = 16;
    public static final byte LOCKED = 32;

    /* for authentication */
    static final byte INS_EXTERNAL_AUTHENTICATE = (byte) 0x82;
    static final byte INS_GET_CHALLENGE = (byte) 0x84;
    static final byte CLA_PROTECTED_APDU = 0x0c;
    static final byte INS_INTERNAL_AUTHENTICATE = (byte) 0x88;

    /* for reading */
    static final byte INS_SELECT_FILE = (byte) 0xA4;
    static final byte INS_READ_BINARY = (byte) 0xB0;

    /* for writing */
    static final byte INS_UPDATE_BINARY = (byte) 0xd6;
    static final byte INS_CREATE_FILE = (byte) 0xe0;
    static final byte INS_PUT_DATA = (byte) 0xda;

    static final short KEY_LENGTH = 16;
    static final short SEED_LENGTH = 16;
    static final short MAC_LENGTH = 8;

    private static final byte PRIVMODULUS_TAG = 0x60;
    private static final byte PRIVEXPONENT_TAG = 0x61;
    private static final byte MRZ_TAG = 0x62;

    private static final short SW_OK = (short)0x9000;

    private byte[] rnd;
    private byte[] ssc;
    private FileSystem fileSystem;
    private RandomData randomData;
    private short selectedFile;
    private PassportCrypto crypto;
    private PassportInit init;
    KeyStore keyStore;
    
    private Log log;

    /**
     * Creates a new passport applet.
     */
    public PassportApplet(byte mode) {
        fileSystem = new FileSystem();

        randomData = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);

        keyStore = new KeyStore(mode);
        switch (mode) {
        case PassportCrypto.CREF_MODE:
            crypto = new CREFPassportCrypto(keyStore);
            break;
        case PassportCrypto.PERFECTWORLD_MODE:
            crypto = new PassportCrypto(keyStore);
            break;
        case PassportCrypto.JCOP41_MODE:
            crypto = new JCOP41PassportCrypto(keyStore);
            break;
        }
        init = new PassportInit(crypto);

        rnd = JCSystem.makeTransientByteArray((byte) 8, JCSystem.CLEAR_ON_RESET);
        ssc = JCSystem.makeTransientByteArray((byte) 8, JCSystem.CLEAR_ON_RESET);
        
        log = new Log(fileSystem);
    }

    /**
     * Installs an instance of the applet.
     * 
     * @param buffer
     * @param offset
     * @param length
     * @see javacard.framework.Applet#install(byte[], byte, byte)
     */
    public static void install(byte[] buffer, short offset, byte length) {
        (new PassportApplet(PassportCrypto.JCOP41_MODE)).register();
    }

    /**
     * Processes incoming APDUs.
     * 
     * @param apdu
     * @see javacard.framework.Applet#process(javacard.framework.APDU)
     */
    public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte cla = buffer[OFFSET_CLA];
        byte ins = buffer[OFFSET_INS];
        short sw1sw2 = SW_OK;
        boolean protectedApdu = cla == CLA_PROTECTED_APDU;
        short responseLength = 0;
        short le = 0;

        /* Ignore APDU that selects this applet... */
        if (selectingApplet()) {
            if (PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED)) {
                PassportUtil.minBitMask(state, MUTUAL_AUTHENTICATED);
            }
            if (PassportUtil.hasBitMask(state, CHALLENGED)) {
                PassportUtil.minBitMask(state, CHALLENGED);
            }
            log.newSession();
            return;
        }

        if (protectedApdu
                && PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED)) {
            try {
                le = crypto.unwrapCommandAPDU(ssc, apdu);
            } catch (CardRuntimeException e) {
                sw1sw2 = e.getReason();
            }
        } 
        else if (protectedApdu) {
            ISOException.throwIt(ISO7816.SW_SECURE_MESSAGING_NOT_SUPPORTED);
        }

        if (sw1sw2 == SW_OK) {
            try {
                responseLength = processAPDU(apdu, cla, ins, protectedApdu, le);
            } catch (CardRuntimeException e) {
                sw1sw2 = e.getReason();
            }
        }

        if (protectedApdu
                && PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED)) {
            responseLength = crypto.wrapResponseAPDU(ssc,
                                                     apdu,
                                                     crypto.getApduBufferOffset(responseLength),
                                                     responseLength,
                                                     sw1sw2);
        }

        if (responseLength > 0) {
            if (apdu.getCurrentState() != APDU.STATE_OUTGOING)
                apdu.setOutgoing();
            if (apdu.getCurrentState() != APDU.STATE_OUTGOING_LENGTH_KNOWN)
                apdu.setOutgoingLength(responseLength);
            apdu.sendBytes((short) 0, responseLength);
        }

        if (sw1sw2 != SW_OK) {
            ISOException.throwIt(sw1sw2);
        }
    }

	public short processAPDU(APDU apdu, byte cla, byte ins, boolean protectedApdu, short le) {
		short responseLength = 0;

		log.insByte(ins);
		switch (ins) {
		case INS_GET_CHALLENGE:
		    if (protectedApdu) {
		        ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
		    }
		    responseLength = processGetChallenge(apdu);
		    break;
		case INS_EXTERNAL_AUTHENTICATE:
		    if (protectedApdu) {
		        ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
		    }
		    responseLength = processMutualAuthenticate(apdu);
		    break;
		case INS_INTERNAL_AUTHENTICATE:
		    responseLength = processInternalAuthenticate(apdu,
		                                                 protectedApdu);
		    break;
		case INS_SELECT_FILE:
		    processSelectFile(apdu);
		    break;
		case INS_READ_BINARY:
		    responseLength = processReadBinary(apdu, le, protectedApdu);
		    break;
		case INS_UPDATE_BINARY:
		case INS_CREATE_FILE:
		case INS_PUT_DATA:
		    if (!PassportUtil.hasBitMask(state, LOCKED)) {
		        switch (ins) {
		        case INS_UPDATE_BINARY:
		            processUpdateBinary(apdu);
		            break;
		        case INS_CREATE_FILE:
		            processCreateFile(apdu);
		            break;
		        case INS_PUT_DATA:
		            processPutData(apdu);
		            break;
		        }
		    }
		    break;
		default:
		    ISOException.throwIt(SW_INS_NOT_SUPPORTED);
		    break;
		}
		return responseLength;
	}

    private void processPutData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short buffer_p = (short) (OFFSET_CDATA & 0xff);
        short lc = (short) (buffer[OFFSET_LC] & 0xff);
        short p1 = (short) (buffer[OFFSET_P1] & 0xff);
        short p2 = (short) (buffer[OFFSET_P2] & 0xff);

        // FIXME: state check

        // sanity check
        if (buffer.length < (short) (buffer_p + lc)) {
            ISOException.throwIt((short) 0x6d66);
        }

        if (p1 == 0xde && p2 == 0xad) {
            state = PassportUtil.plusBitMask(state, LOCKED);
        } else if (p1 == 0 && p2 == PRIVMODULUS_TAG) {
            BERTLVObject object = BERTLVObject.readObjects(buffer, buffer_p, lc);
            short modOffset = object.child.valueOffset;
            short modLength = object.child.valueLength;

            if (buffer[modOffset] == 0) {
                modLength--;
                modOffset++;
            }

            keyStore.rsaPrivateKey.setModulus(buffer, modOffset, modLength);
        } else if (p1 == 0 && p2 == PRIVEXPONENT_TAG) {
            BERTLVObject object = BERTLVObject.readObjects(buffer, buffer_p, lc);
            short expOffset = object.child.valueOffset;
            short expLength = object.child.valueLength;

            // leading zero
            if (buffer[expOffset] == 0) {
                expLength--;
                expOffset++;
            }

            keyStore.rsaPrivateKey.setExponent(buffer, expOffset, expLength);
        } else if (p1 == 0 && p2 == MRZ_TAG) {
            // data is BERTLV object with three objects; docNr, dataOfBirth,
            // dateOfExpiry
            BERTLVObject mrz = BERTLVObject.readObjects(buffer, buffer_p, lc);
            short docNrOffset = mrz.child.valueOffset;
            short docNrLength = mrz.child.valueLength;
            short dobOffset = mrz.child.next.valueOffset;
            short dobLength = mrz.child.next.valueLength;
            short doeOffset = mrz.child.next.next.valueOffset;
            short doeLength = mrz.child.next.next.valueLength;

            short keySeed_offset = init.computeKeySeed(buffer,
                                                       docNrOffset,
                                                       docNrLength,
                                                       dobOffset,
                                                       dobLength,
                                                       doeOffset,
                                                       doeLength);

            short macKey_p = (short) (keySeed_offset + SEED_LENGTH);
            short encKey_p = (short) (keySeed_offset + SEED_LENGTH + KEY_LENGTH);
            crypto.deriveKey(buffer,
                             keySeed_offset,
                             PassportCrypto.MAC_MODE,
                             macKey_p);
            crypto.deriveKey(buffer,
                             keySeed_offset,
                             PassportCrypto.ENC_MODE,
                             encKey_p);
            keyStore.setMutualAuthenticationKeys(buffer,
                                                 macKey_p,
                                                 buffer,
                                                 encKey_p);

            state = PassportUtil.plusBitMask(state,
                                             HAS_MUTUALAUTHENTICATION_KEYS);
            log.enabled(true);
        }
    }

    private short processInternalAuthenticate(APDU apdu, boolean protectedApdu) {
        if (!PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED)) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        short buffer_p = (short) (OFFSET_CDATA & 0xff);
        short hdr_offset = 0;
        if (protectedApdu) {
            hdr_offset = crypto.getApduBufferOffset((short) 128);
        }
        short hdr_len = 1;
        short m1_len = 106; // whatever
        short m1_offset = (short) (hdr_offset + hdr_len);
        short m2_len = 8;
        short m2_offset = (short) (m1_offset + m1_len);
        // we will write the hash over m2
        short m1m2hash_offset = (short) (m1_offset + m1_len);
        short m1m2hash_len = 20;
        short trailer_offset = (short) (m1m2hash_offset + m1m2hash_len);
        short trailer_len = 1;

        byte[] buffer = apdu.getBuffer();
        short bytesLeft = (short) (buffer[OFFSET_LC] & 0x00FF);

        if (bytesLeft != m2_len)
            ISOException.throwIt(SW_WRONG_LENGTH);

        // put m2 in place
        Util.arrayCopy(buffer, buffer_p, buffer, m2_offset, m2_len);

        // write some random data of m1_len
        // randomData.generateData(buffer, m1_offset, m1_length);
        for (short i = m1_offset; i < (short) (m1_offset + m1_len); i++) {
            buffer[i] = 0;
        }

        // calculate SHA1 hash over m1 and m2
        MessageDigest digest = MessageDigest.getInstance(MessageDigest.ALG_SHA,
                                                         false);
        digest.doFinal(buffer,
                       m1_offset,
                       (short) (m1_len + m2_len),
                       buffer,
                       m1m2hash_offset);

        // write trailer
        buffer[trailer_offset] = (byte) 0xbc;

        // write header
        buffer[hdr_offset] = (byte) 0x6a;

        // encrypt the whole buffer with our AA private key
        short plaintext_len = (short) (hdr_len + m1_len + m1m2hash_len + trailer_len);
        // sanity check
        if (plaintext_len != 128) {
            ISOException.throwIt((short) 0x6d66);
        }
        Cipher rsaCiph = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
        rsaCiph.init(keyStore.rsaPrivateKey, Cipher.MODE_ENCRYPT);
        short ciphertext_len = rsaCiph.doFinal(buffer,
                                               hdr_offset,
                                               plaintext_len,
                                               buffer,
                                               hdr_offset);
        // sanity check
        if (ciphertext_len != 128) {
            ISOException.throwIt((short) 0x6d66);
        }

        return ciphertext_len;
    }

    /**
     * Processes incoming GET_CHALLENGE APDUs.
     * 
     * Generates random 8 bytes, sends back result and stores result in rnd.
     * 
     * //ensures PassportUtil.hasBitMask(state, CHALLENGED); 
     * //ensures
     * \old(PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED)) ==>
     * !PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED))
     * 
     * @param apdu
     *            is used for sending (8 bytes) only
     */
    private short processGetChallenge(APDU apdu) {
        if (!PassportUtil.hasBitMask(state, HAS_MUTUALAUTHENTICATION_KEYS)) {
            ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        byte[] buffer = apdu.getBuffer();
        short le = apdu.setOutgoing();
        if (le != 8) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        randomData.generateData(rnd, (short) 0, le);
        Util.arrayCopy(rnd, (short) 0, buffer, (short) 0, (short) 8);

        if (PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED)) {
            state = PassportUtil.minBitMask(state, MUTUAL_AUTHENTICATED);
        }
        state = PassportUtil.plusBitMask(state, CHALLENGED);

        return le;
    }

    /**
     * Perform mutual authentication with terminal.
     * 
     * //@requires apdu != null; //@modifies apdu.getBuffer()[0..80]; //@ensures
     * PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED);
     * 
     * @param apdu
     *            the APDU
     * @return length of return APDU
     */
    private short processMutualAuthenticate(APDU apdu) {
        if (!PassportUtil.hasBitMask(state, CHALLENGED)
                || PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED)) {
            ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        short bytesLeft = (short) (buffer[OFFSET_LC] & 0x00FF);

        if (buffer.length < 120)
            ISOException.throwIt((short) 0x6d66);

        if (bytesLeft != (short) 40)
            ISOException.throwIt(SW_WRONG_LENGTH);

        short e_ifd_p = OFFSET_CDATA;
        short e_ifd_length = 32;
        short m_ifd_p = (short) (e_ifd_p + e_ifd_length);

        if(apdu.getCurrentState() == APDU.STATE_INITIAL) {
            apdu.setIncomingAndReceive();
        }
        if(apdu.getCurrentState() != APDU.STATE_FULL_INCOMING) {
            // need all data in one APDU.
            ISOException.throwIt((short)0x6d66);
        }

        // buffer[OFFSET_CDATA ... +40] consists of e_ifd || m_ifd
        // verify checksum m_ifd of cryptogram e_ifd
        crypto.initMac(Signature.MODE_VERIFY);
        if (!crypto.verifyMacFinal(buffer,
                                   e_ifd_p,
                                   e_ifd_length,
                                   buffer,
                                   m_ifd_p))
            ISOException.throwIt((short) 0x6300);

        // decrypt e_ifd into buffer[0] where buffer = rnd.ifd || rnd.icc ||
        // k.ifd
        crypto.decryptInit();
        short plaintext_len = crypto.decryptFinal(buffer,
                                             e_ifd_p,
                                             e_ifd_length,
                                             buffer,
                                             (short) 0);
        if (plaintext_len != 32) // sanity check
            ISOException.throwIt((short) 0x6d66);
        short rnd_ifd_p = 0;
        short rnd_icc_p = 8;
        short k_ifd_p = 16;
        /* pointers for writing intermediate data in buffer */
        short k_icc_p = (short) (k_ifd_p + SEED_LENGTH);
        short keySeed_p = (short) (k_icc_p + SEED_LENGTH);
        short keys_p = (short) (keySeed_p + SEED_LENGTH);

        // verify that rnd.icc equals value generated in getChallenge
        if (Util.arrayCompare(buffer, rnd_icc_p, rnd, (short) 0, (short) 8) != 0)
            ISOException.throwIt((short) 0x6d02);

        // generate keying material k.icc
        randomData.generateData(buffer, k_icc_p, SEED_LENGTH);

        // calculate keySeed for session keys
        PassportUtil.xor(buffer,
                         k_ifd_p,
                         buffer,
                         k_icc_p,
                         buffer,
                         keySeed_p,
                         SEED_LENGTH);

        // calculate session keys
        crypto.deriveKey(buffer, keySeed_p, PassportCrypto.MAC_MODE, keys_p);
        short macKey_p = keys_p;
        keys_p += KEY_LENGTH;
        crypto.deriveKey(buffer, keySeed_p, PassportCrypto.ENC_MODE, keys_p);
        short encKey_p = keys_p;
        keys_p += KEY_LENGTH;
        keyStore.setSecureMessagingKeys(buffer, macKey_p, buffer, encKey_p);

        // compute ssc
        PassportCrypto.computeSSC(buffer, rnd_icc_p, buffer, rnd_ifd_p, ssc);

        // create response in buffer where response = rnd.icc || rnd.ifd ||
        // k.icc
        PassportUtil.swap(buffer, rnd_icc_p, rnd_ifd_p, (short) 8);
        Util.arrayCopy(buffer, k_icc_p, buffer, (short) 16, SEED_LENGTH);

        // make buffer encrypted using k_enc
        crypto.encryptInit();
        short ciphertext_len = crypto.encryptFinal(buffer,
                                              (short) 0,
                                              (short) 32,
                                              buffer,
                                              (short) 0);

        // create m_icc which is a checksum of response
        crypto.initMac(Signature.MODE_SIGN);
        crypto.createMacFinal(buffer,
                              (short) 0,
                              ciphertext_len,
                              buffer,
                              ciphertext_len);

        state = PassportUtil.plusBitMask(state, MUTUAL_AUTHENTICATED);
        // state |= MUTUAL_AUTHENTICATED;

        return (short) (ciphertext_len + 8);
    }

    /**
     * Processes incoming SELECT_FILE APDUs.
     * 
     * //@ensures PassportUtil.hasBitMask(state, FILE_SELECTED);
     * 
     * @param apdu
     *            where the first 2 data bytes encode the file to select.
     */
    private void processSelectFile(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[OFFSET_LC] & 0x00FF);

        if (lc != 2)
            ISOException.throwIt(SW_WRONG_LENGTH);

        if(apdu.getCurrentState() == APDU.STATE_INITIAL) {
            apdu.setIncomingAndReceive();
        }
        if(apdu.getCurrentState() != APDU.STATE_FULL_INCOMING) {
            // need all data in one APDU.
            ISOException.throwIt((short)0x6d66);
        }
                
        short fid = Util.getShort(buffer, OFFSET_CDATA);
        
        log.selectFile(fid);

        if (fileSystem.getFile(fid) != null) {
            selectedFile = fid;
            if (!PassportUtil.hasBitMask(state, FILE_SELECTED)) {
                state = PassportUtil.plusBitMask(state, FILE_SELECTED);
            }
            return;
        }
        ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
    }

    /**
     * Processes incoming READ_BINARY APDUs. Returns data of the currently
     * selected file.
     * 
     * //@ensures state == \old(state);
     * 
     * @param apdu
     *            where the offset is carried in header bytes p1 and p2.
     * @param le
     *            expected length by terminal
     * @return length of the return APDU
     */
    private short processReadBinary(APDU apdu, short le, boolean protectedApdu) {
        if (!PassportUtil.hasBitMask(state, FILE_SELECTED)) {
            ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
        }
        byte[] buffer = apdu.getBuffer();
        byte p1 = buffer[OFFSET_P1];
        byte p2 = buffer[OFFSET_P2];

        short offset = Util.makeShort(p1, p2);

        byte[] file = fileSystem.getFile(selectedFile);
        if(file == null) {
            ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
        }
        
        short len;
        short fileSize = fileSystem.getFileSize(selectedFile);

        len = PassportUtil.min((short)(buffer.length - 37), (short) (fileSize - offset));
        // FIXME: 37 magic
        len = PassportUtil.min(len, (short) buffer.length);
        short bufferOffset = 0;
        if (protectedApdu) {
            bufferOffset = crypto.getApduBufferOffset(len);
        }
        Util.arrayCopy(file, offset, buffer, bufferOffset, len);

        return len;
    }

    /***************************************************************************
     * Processes and UPDATE_BINARY apdu. Writes data in the currently selected
     * file.
     * 
     * //@ensures state == \old(state);
     * 
     * @param apdu
     *            carries the offset where to write date in header bytes p1 and
     *            p2.
     */
    private void processUpdateBinary(APDU apdu) {
        if (!PassportUtil.hasBitMask(state, FILE_SELECTED)) {
            ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
        }
        byte[] buffer = apdu.getBuffer();
        byte p1 = buffer[OFFSET_P1];
        byte p2 = buffer[OFFSET_P2];
        short offset = Util.makeShort(p1, p2);

        short readCount = (short) (buffer[ISO7816.OFFSET_LC] & 0xff);
        if (apdu.getCurrentState() == APDU.STATE_INITIAL) {
            readCount = apdu.setIncomingAndReceive();
        }

        while (true) {
            fileSystem.writeData(selectedFile,
                                 offset,
                                 buffer,
                                 OFFSET_CDATA,
                                 readCount);
            if (apdu.getCurrentState() != APDU.STATE_FULL_INCOMING) {
                readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
            } else {
                break;
            }
            offset += readCount;
        }
    }

    /***************************************************************************
     * Processes and CREATE_FILE apdu. This functionality is only partly
     * implemented. Only non-directories (files) can be created, all options for
     * CREATE_FILE are ignored.
     * 
     * //@ensures state == \old(state);
     * 
     * @param apdu
     *            containing 6 bytes: 0x64 || (1 byte) || size (2) || fid (2)
     */
    private void processCreateFile(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[OFFSET_LC] & 0xff);

        if(apdu.getCurrentState() == APDU.STATE_INITIAL) {
            apdu.setIncomingAndReceive();
        }
        if(apdu.getCurrentState() != APDU.STATE_FULL_INCOMING) {
            // need all data in one APDU.
            ISOException.throwIt((short)0x6d66);
        }

        if (lc < (short) 6 || (buffer[OFFSET_CDATA + 1] & 0xff) < 4)
            ISOException.throwIt(SW_WRONG_LENGTH);

        if (buffer[OFFSET_CDATA] != 0x63)
            ISOException.throwIt(SW_DATA_INVALID);

        short size = Util.makeShort(buffer[(short) (OFFSET_CDATA + 2)],
                                    buffer[(short) (OFFSET_CDATA + 3)]);

        short fid = Util.makeShort(buffer[(short) (OFFSET_CDATA + 4)],
                                   buffer[(short) (OFFSET_CDATA + 5)]);

        fileSystem.createFile(fid, size);
    }
}
