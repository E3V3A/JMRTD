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
import javacard.security.RandomData;

/**
 * PassportApplet
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class PassportApplet extends Applet implements ISO7816 {
    private static byte state = 0;

    public static final byte CHALLENGED = 1;
    public static final byte MUTUAL_AUTHENTICATED = 2;
    public static final byte FILE_SELECTED = 4;

    private static final byte INS_MUTUAL_AUTHENTICATE = (byte) 0x82;
    private static final byte INS_GET_CHALLENGE = (byte) 0x84;
    private static final byte INS_SELECT_FILE = (byte) 0xA4;
    private static final byte INS_READ_BINARY = (byte) 0xB0;
    private static final byte INS_UPDATE_BINARY = (byte) 0xd6;
    private static final byte INS_CREATE_FILE = (byte) 0xe0;
    private static final byte CLA_PROTECTED_APDU = 0x0c;

    private short selectedFile;
    private FileSystem fileSystem;

    private byte[] rnd;

    /* sample data */
    byte[] docNr = { 'L', '8', '9', '8', '9', '0', '2', 'C', '<' };
    byte[] dateOfBirth = { '6', '9', '0', '8', '0', '6' };
    byte[] dateOfExpiry = { '9', '4', '0', '6', '2', '3' };

    private RandomData randomData;

    private byte[] ssc;

    PassportCrypto crypto;
    
    /**
     * Creates a new passport applet.
     */
    public PassportApplet(byte mode) {
        fileSystem = new FileSystem();

        randomData = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);
        
        switch(mode) {
        case PassportCrypto.CREF_MODE:
            crypto = new CREFPassportCrypto();
            break;
        case PassportCrypto.JCOP_MODE:
            crypto = new JCOPPassportCrypto();
            break;                
        }

        byte[] kSeed = PassportInit.computeKeySeed(docNr,
                dateOfBirth, dateOfExpiry);
        byte[] kEnc_bytes = PassportCrypto.deriveKey(kSeed,
                PassportCrypto.ENC_MODE);
        byte[] kMac_bytes = PassportCrypto.deriveKey(kSeed,
                PassportCrypto.MAC_MODE);
        crypto.setMutualAuthKeys(kMac_bytes, kEnc_bytes);
        rnd = JCSystem.makeTransientByteArray((byte) 8, JCSystem.CLEAR_ON_RESET);
        ssc = JCSystem.makeTransientByteArray((byte) 8, JCSystem.CLEAR_ON_RESET);

        register();
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
        new PassportApplet(PassportCrypto.CREF_MODE);
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
        short sw1sw2 = 0;
        byte protectedApdu = (byte) (cla == CLA_PROTECTED_APDU ? 1 : 0);
        short responseLength = 0;
        short le=0;
        
        /* Ignore APDU that selects this applet... */
        if (selectingApplet()) {
            // PassportCrypto.initTempSpace();
            return;
        }

        if (protectedApdu == 1) {
            try {
                le = crypto.unwrapCommandAPDU(ssc, apdu);
            } catch (CardRuntimeException e) {
                sw1sw2 = e.getReason();
                ISOException.throwIt((short) 0x6d66);
            }
        }

        try {
            switch (ins) {
            case INS_GET_CHALLENGE:
                responseLength = processGetChallenge(apdu);
                break;
            case INS_MUTUAL_AUTHENTICATE:
                responseLength = processMutualAuthenticate(apdu);
                break;
            case INS_SELECT_FILE:
                processSelectFile(apdu);
                break;
            case INS_READ_BINARY:
                responseLength = processReadBinary(apdu, le);
                break;
            case INS_UPDATE_BINARY:
                processUpdateBinary(apdu);
                break;
            case INS_CREATE_FILE:
                processCreateFile(apdu);
                break;
            default:
                ISOException.throwIt(SW_INS_NOT_SUPPORTED);
                break;
            }
        } catch (CardRuntimeException e) {
            sw1sw2 = e.getReason();
        }

        if (protectedApdu == 1) {
            responseLength = crypto.wrapResponseAPDU(ssc, apdu, responseLength, sw1sw2);
        }

        if (responseLength > 0) {
            if (apdu.getCurrentState() != APDU.STATE_OUTGOING)
                apdu.setOutgoing();
            if (apdu.getCurrentState() != APDU.STATE_OUTGOING_LENGTH_KNOWN)
                apdu.setOutgoingLength(responseLength);
            apdu.sendBytes((short) 0, responseLength);
        }
    }

    /**
     * Processes incoming GET_CHALLENGE APDUs.
     * 
     * Generates random 8 bytes, sends back result and stores result in rnd
     * 
     * @param apdu
     */
    private short processGetChallenge(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short le = apdu.setOutgoing();
        if (le != 8) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        // FIXME: is this call allowed in current state?
        
        randomData.generateData(rnd, (short)0, le);
        Util.arrayCopy(rnd, (short) 0, buffer, (short) 0, (short) 8);

        state |= CHALLENGED;

        return le;
    }

    /**
     * Perform mutual authentication with terminal
     * 
     * @param apdu
     */
    private short processMutualAuthenticate(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);

        if (buffer.length < 40)
            ISOException.throwIt((short) 0x6d66);

        if (bytesLeft != (short) 40)
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        
        short e_ifd_p = OFFSET_CDATA;
        short e_ifd_length = 32;
        short m_ifd_p = (short)(e_ifd_p + e_ifd_length);
        
//        if((state ^ MUTUAL_AUTHENTICATED) == 0)
//            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
       
        // FIXME: is this call allowed in current state?

        short readCount = apdu.setIncomingAndReceive();
        while (bytesLeft > 0) {
            bytesLeft -= readCount;
            readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
        }

        // buffer[OFFSET_CDATA ... +40] consists of e_ifd || m_ifd
        // verify checksum m_ifd of cryptogram e_ifd
        if (!crypto.verifyMac(state, buffer, e_ifd_p, e_ifd_length, buffer, m_ifd_p))
            ISOException.throwIt((short) 0x6d01);

        // decrypt e_ifd into buffer where buffer = rnd.ifd || rnd.icc || k.ifd
        short plaintext_len = crypto.decrypt(state, buffer, e_ifd_p, e_ifd_length, buffer, (short)0);
        if(plaintext_len != 32) // sanity check
            ISOException.throwIt((short)0x6d66);
        short rnd_ifd_p = 0;
        short rnd_icc_p = 8;
        short k_ifd_p = 16;
        
        // verify that rnd.icc equals value generated in getChallenge
        if (Util.arrayCompare(buffer, rnd_icc_p, rnd, (short)0, (short)8) != 0)
            ISOException.throwIt((short) 0x6d02);

        // generate keying material k.icc
        byte[] k_icc = new byte[16];;
        randomData.generateData(k_icc, (short) 0, (short) 16);

        // calculate keySeed for session keys
        byte[] keySeed = new byte[16];
        PassportUtil.xor(buffer, k_ifd_p, k_icc, (short)0, keySeed, (short)0, (short)16);

        // calculate session keys
        byte[] ksEnc_bytes = PassportCrypto.deriveKey(keySeed,
                PassportCrypto.ENC_MODE);
        byte[] ksMac = PassportCrypto.deriveKey(keySeed,
                PassportCrypto.MAC_MODE);
        crypto.setSessionKeys(ksMac, ksEnc_bytes);

        // compute ssc
        PassportCrypto.computeSSC(buffer, rnd_icc_p, buffer, rnd_ifd_p, ssc);

        // create response in buffer where response = rnd.icc || rnd.ifd || k.icc
        PassportUtil.swap(buffer, rnd_icc_p, rnd_ifd_p, (short)8);
        Util.arrayCopy(k_icc, (short) 0, buffer, (short) 16, (short) 16);

        // make buffer encrypted using k_enc
        short ciphertext_len = crypto.encrypt(state, PassportCrypto.DONT_PAD_INPUT,
                                              buffer, (short)0, (short)32, buffer, (short)0);
        
        // create m_icc which is a checksum of response
        crypto.createMac(state, buffer, (short) 0, ciphertext_len, buffer, ciphertext_len);

        state |= MUTUAL_AUTHENTICATED;
        
        return (short)(ciphertext_len + 8);
    }

    /**
     * Processes incoming SELECT_FILE APDUs.
     * 
     * @param apdu
     */

    private void processSelectFile(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[OFFSET_LC] & 0x00FF);
        
        if (lc != 2)
            ISOException.throwIt(SW_WRONG_LENGTH);

        short fid = Util.getShort(buffer, OFFSET_CDATA);

        if (fileSystem.getFile(fid) != null) {
            selectedFile = fid;
            state |= FILE_SELECTED;
        }
    }

    /**
     * Processes incoming READ_BINARY APDUs.
     * 
     * TODO secure messaging...
     * 
     * @param apdu
     */
    private short processReadBinary(APDU apdu, short le) {
        byte[] buffer = apdu.getBuffer();
        byte p1 = buffer[OFFSET_P1];
        byte p2 = buffer[OFFSET_P2];
        short offset = Util.makeShort(p1, p2);
        byte[] file = fileSystem.getFile(selectedFile);
        //short le = apdu.setOutgoing();
        short len;
        // FIXME: how do we use sendBytesLong with secure messaging???!

        if (buffer.length < le)
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        
        len = PassportUtil.min(le, (short)(file.length - offset));
        len = PassportUtil.min(len, (short)0xe0); //FIXME: magic
        len = PassportUtil.min(len, (short)buffer.length);
        Util.arrayCopy(file, offset, buffer, (short) 0, len);

        return len;
    }

    private void processUpdateBinary(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte p1 = buffer[OFFSET_P1];
        byte p2 = buffer[OFFSET_P2];

        short lc = (short) (buffer[OFFSET_LC] & 0xff);

        if ((state & FILE_SELECTED) != FILE_SELECTED)
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);

        short offset = Util.makeShort(p1, p2);
        fileSystem.writeData(selectedFile, offset, buffer, OFFSET_CDATA, lc);
    }

    /*
     * Byte Information 1 always 63 (hex) 2 length of subsequent header (bytes 3
     * to end) 3..4 file size 5..6 file identifier. Files in the range
     * fd01..fd1e can be referenced with Short File Identifiers (least
     * significate 5 bits). 7..8 key identifiers for use in access conditions 9
     * access conditions for read / update 10 access conditions for create /
     * delete 11 access conditions for invalidate / rehabilitate 12 file status.
     * In general, leave this as 1. 13 Length of following header (e.g. 1 if
     * byte 14 is the last byte) 14 file type. 00=transparent, 01=fixed length
     * record, 02=variable length record, 03 = cyclical. 15 record size
     */
    private void processCreateFile(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[OFFSET_LC] & 0xff);

        if (lc < (short) 6 || (buffer[OFFSET_CDATA + 1] & 0xff) < 4)
            ISOException.throwIt(SW_WRONG_LENGTH);

        if (buffer[OFFSET_CDATA] != 0x63)
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);

        short size = Util.makeShort(buffer[(short) (OFFSET_CDATA + 2)],
                buffer[(short) (OFFSET_CDATA + 3)]);

        short fid = Util.makeShort(buffer[(short) (OFFSET_CDATA + 4)],
                buffer[(short) (OFFSET_CDATA + 5)]);

        fileSystem.createFile(fid, size);
    }

    public final static byte getState() {
        return state;
    }
}
