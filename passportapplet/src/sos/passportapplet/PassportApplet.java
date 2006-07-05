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

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

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

    private DESKey ma_kEnc;
    private DESKey ma_kMac;
    private DESKey ma_kMac_a, ma_kMac_b;

    private byte[] rnd;

    private DESKey sm_kEnc;
    private DESKey sm_kMac_a, sm_kMac_b;

    private byte[] ksSeed;

    /* sample data */
    byte[] docNr = { 'L', '8', '9', '8', '9', '0', '2', 'C', '<' };
    byte[] dateOfBirth = { '6', '9', '0', '8', '0', '6' };
    byte[] dateOfExpiry = { '9', '4', '0', '6', '2', '3' };

    private RandomData randomData;
    private MessageDigest shaDigest;

    private byte[] ssc;

    PassportCrypto crypto;
    
    /**
     * Creates a new passport applet.
     */
    public PassportApplet(byte mode) {
        fileSystem = new FileSystem();

        randomData = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);
        shaDigest = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
 
        if(mode == PassportCrypto.CREF_MODE) {
            crypto = new CREFPassportCrypto();
        }

        crypto.createTempSpace();

        byte[] kSeed = crypto.computeKeySeed(shaDigest, docNr,
                dateOfBirth, dateOfExpiry);
        byte[] kEnc_bytes = crypto.deriveKey(shaDigest, kSeed,
                PassportCrypto.ENC_MODE);
        byte[] kMac_bytes = crypto.deriveKey(shaDigest, kSeed,
                PassportCrypto.MAC_MODE);
        crypto.setMutualAuthKeys(kMac_bytes, kEnc_bytes);
        rnd = JCSystem
                .makeTransientByteArray((byte) 8, JCSystem.CLEAR_ON_RESET);
        ssc = new byte[8]; // TODO: transient?

        ksSeed = new byte[16];
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

        /* Ignore APDU that selects this applet... */
        if (selectingApplet()) {
            // PassportCrypto.initTempSpace();
            return;
        }

        if (protectedApdu == 1) {
            try {
                crypto.unwrapCommandAPDU(ssc, apdu);
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
                responseLength = processReadBinary(apdu);
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
            if (protectedApdu == 1
                    || apdu.getCurrentState() != APDU.STATE_OUTGOING_LENGTH_KNOWN)
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

        // FIXME: is this call allowed in current state?

        short readCount = apdu.setIncomingAndReceive();
        while (bytesLeft > 0) {
            bytesLeft -= readCount;
            readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
        }

        byte[] e_ifd = new byte[32];
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, e_ifd, (short) 0,
                (short) 32);

        byte[] e_ifd_padded = new byte[40];
        Util.arrayCopy(e_ifd, (short) 0, e_ifd_padded, (short) 0, (short) 32);
        Util.arrayCopy(CREFPassportCrypto.PAD_DATA, (short) 0, e_ifd_padded,
                (short) 32, (short) 8);

        byte[] m_ifd = new byte[8];
        Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + e_ifd.length),
                m_ifd, (short) 0, (short) 8);

        // verify checksum m_ifd of cryptogram e_ifd
        // boolean b=false;
        // try {
        // sig.init(ma_kMac,Signature.MODE_VERIFY);
        // b = sig.verify(e_ifd,(short)0,(short)32,m_ifd, (short)0, (short)8);
        // }catch(CryptoException ce) {
        //
        // short reason = ce.getReason();
        // ISOException.throwIt((short) 0x6d01);
        // }
        // if(!b)
        // ISOException.throwIt((short) 0x6d01);

        if (!crypto.verifyMac(state, e_ifd, (short)0, (short)e_ifd.length, m_ifd, (short)0))
            ISOException.throwIt((short) 0x6d01);

        // decrypt e_ifd into s where s = rnd.ifd || rnd.icc || k.ifd
        byte[] s = new byte[32 + 16];
        // ciph.init(kEnc, Cipher.MODE_DECRYPT);
        crypto.decrypt(state, e_ifd, (short) 0, (short)32, s, (short) 0);

        // try {
        // ciph.doFinal(e_ifd_padded, (short) 0, (short) e_ifd_padded.length,
        // s, (short) 0);
        // } catch (CryptoException e) {
        // // see comment for PassportUtil.PAD_DATA
        // if (e.getReason() != CryptoException.ILLEGAL_USE)
        // ISOException.throwIt((short) 0x6d66);
        // }

        byte[] rnd_ifd = new byte[8];
        byte[] rnd_icc = new byte[8];
        byte[] k_ifd = new byte[16];
        Util.arrayCopy(s, (short) 0, rnd_ifd, (short) 0, (short) 8);
        Util.arrayCopy(s, (short) 8, rnd_icc, (short) 0, (short) 8);
        Util.arrayCopy(s, (short) 16, k_ifd, (short) 0, (short) 16);

        // verify that rnd.icc equals value generated in getChallenge
        if (Util.arrayCompare(rnd_icc, (short) 0, rnd, (short) 0, (short) 8) != 0)
            ISOException.throwIt((short) 0x6d02);

        // generate keying material k.icc
//        byte[] k_icc = { (byte) 0x0b, (byte) 0x4f, (byte) 0x80, (byte) 0x32,
//                (byte) 0x3e, (byte) 0xb3, (byte) 0x19, (byte) 0x1c,
//                (byte) 0xb0, (byte) 0x49, (byte) 0x70, (byte) 0xcb,
//                (byte) 0x40, (byte) 0x52, (byte) 0x79, (byte) 0x0b }; // new
        byte[] k_icc = new byte[16];;
        randomData.generateData(k_icc, (short) 0, (short) 16);

        // calculate keySeed for session keys
        byte[] keySeed = new byte[16];
        PassportUtil.xor(k_ifd, k_icc, keySeed);
        // debug
        Util.arrayCopy(keySeed, (short) 0, ksSeed, (short) 0, (short) 16);

        // calculate session keys
        byte[] ksEnc_bytes = PassportCrypto.deriveKey(shaDigest, keySeed,
                PassportCrypto.ENC_MODE);
        byte[] ksMac = PassportCrypto.deriveKey(shaDigest, keySeed,
                PassportCrypto.MAC_MODE);
        crypto.setSessionKeys(ksMac, ksEnc_bytes);

        // compute ssc
        PassportCrypto.computeSSC(rnd_icc, rnd_ifd, ssc);

        // create r where r = rnd.icc || rnd.ifd || k.icc
        byte[] r = new byte[32];
        Util.arrayCopy(rnd_icc, (short) 0, r, (short) 0, (short) 8);
        Util.arrayCopy(rnd_ifd, (short) 0, r, (short) 8, (short) 8);
        Util.arrayCopy(k_icc, (short) 0, r, (short) 16, (short) 16);

        // create e_icc which is r encrypted using k_enc
        byte[] e_icc = new byte[(short) (r.length + CREFPassportCrypto.PAD_DATA.length)];
        crypto.encrypt(state, r, (short)0, (short)r.length, e_icc, (short)0);
        
        // create m_icc which is a checksum of e_icc
        byte[] m_icc = new byte[8];
        crypto.createMac(state, e_icc, (short) 0, (short) r.length, m_icc, (short) 0);

        // prepare e_icc || m_icc in buffer
        Util.arrayCopy(e_icc, (short) 0, buffer, (short) 0, (short) r.length);
        Util.arrayCopy(m_icc, (short) 0, buffer, (short) r.length,
                (short) m_icc.length);

        state |= MUTUAL_AUTHENTICATED;
        
        return (short) (r.length + m_icc.length);
    }

    /**
     * Processes incoming SELECT_FILE APDUs.
     * 
     * @param apdu
     */

    private void processSelectFile(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte cla = buffer[OFFSET_CLA];
        byte ins = buffer[OFFSET_INS];
        short lc = (short) (buffer[OFFSET_LC] & 0x00FF);
        short le = 0;
        short response_len = 0;
        short sw1sw2 = 0;

        if (lc != 2)
            ISOException.throwIt(SW_WRONG_LENGTH);

        short fid = Util.getShort(buffer, OFFSET_CDATA);

        if (fileSystem.exists(fid) == 1) {
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
    private short processReadBinary(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte cla = buffer[OFFSET_CLA];
        byte ins = buffer[OFFSET_INS];
        byte p1 = buffer[OFFSET_P1];
        byte p2 = buffer[OFFSET_P2];
        short offset = Util.makeShort(p1, p2);
        byte[] file = fileSystem.getFile(selectedFile);
        short le = apdu.setOutgoing();
        // FIXME: how do we use sendBytesLong with secure messaging???!
        // apdu.sendBytesLong(file, offset, le);

        if (buffer.length < le)
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        Util.arrayCopy(file, offset, buffer, (short) 0, le);

        return le;
    }

    // we might want to check whether a call is allowed ;)
    // private void processWriteBinary(APDU apdu) {
    // byte[] buffer = apdu.getBuffer();
    // byte cla = buffer[OFFSET_CLA];
    // byte ins = buffer[OFFSET_INS];
    // byte p1 = buffer[OFFSET_P1];
    // byte p2 = buffer[OFFSET_P2];
    // apdu.setIncomingAndReceive();
    // short lc = (short)(buffer[OFFSET_LC] & 0xff);//apdu.getIncomingLength();
    // short bytesLeft = lc;
    // short offset = Util.makeShort(p1, p2);
    //        
    // short readCount = apdu.setIncomingAndReceive();
    // while ( bytesLeft > 0) {
    // bytesLeft -= readCount;
    // readCount = apdu.receiveBytes ( ISO7816.OFFSET_CDATA );
    // fileSystem.writeData(selectedFile, currentOffset, buffer, OFFSET_CDATA,
    // readCount);
    // currentOffset += readCount;
    // }
    // }

    private void processUpdateBinary(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte cla = buffer[OFFSET_CLA];
        byte ins = buffer[OFFSET_INS];
        byte p1 = buffer[OFFSET_P1];
        byte p2 = buffer[OFFSET_P2];
        short le = 0;
        short sw1sw2 = 0;

        short lc = (short) (buffer[OFFSET_LC] & 0xff);

        if ((short) (p1 & 0x80) == (short) 0x80)
            ISOException.throwIt(SW_INCORRECT_P1P2);

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
        byte cla = buffer[OFFSET_CLA];
        byte ins = buffer[OFFSET_INS];
        byte p1 = buffer[OFFSET_P1];
        byte p2 = buffer[OFFSET_P2];
        // short le = apdu.setIncomingAndReceive(); // FIXME:mo
        short lc = (short) (buffer[OFFSET_LC] & 0xff);// apdu.getIncomingLength();

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
