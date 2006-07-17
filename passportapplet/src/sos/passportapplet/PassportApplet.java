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

    /* values of state */
    public static final byte CHALLENGED = 1;
    public static final byte MUTUAL_AUTHENTICATED = 2;
    public static final byte FILE_SELECTED = 4;
    public static final byte HAS_MUTUALAUTHENTICATION_KEYS = 8;

    /* for authentication */
    private static final byte INS_MUTUAL_AUTHENTICATE = (byte) 0x82;
    private static final byte INS_GET_CHALLENGE = (byte) 0x84;
    private static final byte CLA_PROTECTED_APDU = 0x0c;
    
    /* for reading */
    private static final byte INS_SELECT_FILE = (byte) 0xA4;
    private static final byte INS_READ_BINARY = (byte) 0xB0;
    
    /* for writing */
    private static final byte INS_UPDATE_BINARY = (byte) 0xd6;
    private static final byte INS_CREATE_FILE = (byte) 0xe0;
    private static final byte INS_SET_DOCNR_DOB_DOE = (byte)0x10;

    static final short KEY_LENGTH = 16;
    static final short SEED_LENGTH = 16;
    static final short MAC_LENGTH = 8;
    
    private byte[] rnd;
    private byte[] ssc;
    private FileSystem fileSystem;

    private RandomData randomData;
    private short selectedFile;
    private PassportCrypto crypto;
    
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
        new PassportApplet(PassportCrypto.JCOP_MODE);
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
            case INS_SET_DOCNR_DOB_DOE:
                processSetMutualAuthKeys(apdu);
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
        
        if(sw1sw2 != 0) {
            ISOException.throwIt(sw1sw2);
        }
    }

    /**
     * Initializes the passport with the mutual authentication keys, which are 
     * computed on card.
     * 
     * //@ ensures state == HAS_MUTUALAUTHENTICATION_KEYS
     * 
     * The data part of the APDU must contain:
     *    docNr (9 bytes) || dateOfBirth (6) || dateOfExpiry (6)
     * 
     * @param apdu containing the initialization data
     */    
    private void processSetMutualAuthKeys(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short)(buffer[OFFSET_LC] & 0xff);
        if(state != 0) {
            ISOException.throwIt(SW_COMMAND_NOT_ALLOWED);
        }
        if (lc != (short)(PassportInit.DOCNR_LEN + PassportInit.DOB_LEN + PassportInit.DOE_LEN)) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }
        short readCount = apdu.setIncomingAndReceive();
        short bytesLeft = 0;
        while (bytesLeft > 0) {
            bytesLeft -= readCount;
            readCount = apdu.receiveBytes(OFFSET_CDATA);
        }
        
        short keySeed_offset = PassportInit.computeKeySeed(buffer, OFFSET_CDATA);
                
        short macKey_p = (short)(keySeed_offset + SEED_LENGTH);
        short encKey_p = (short)(keySeed_offset + SEED_LENGTH + KEY_LENGTH);
        PassportCrypto.deriveKey(buffer, keySeed_offset, PassportCrypto.MAC_MODE, macKey_p);
        PassportCrypto.deriveKey(buffer, keySeed_offset, PassportCrypto.ENC_MODE, encKey_p);
        crypto.setMutualAuthKeys(buffer, macKey_p, encKey_p);

        state = HAS_MUTUALAUTHENTICATION_KEYS;
    }

    /**
     * Processes incoming GET_CHALLENGE APDUs.
     * 
     * Generates random 8 bytes, sends back result and stores result in rnd.
     * 
     * //@ensures PassportUtil.hasBitMask(state, CHALLENGED); 
     * 
     * @param apdu is used for sending (8 bytes) only
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
     * Perform mutual authentication with terminal.
     * 
     * //@requires apdu != null;
     * //@modifies apdu.getBuffer()[0..80];
     * //@ensures PassportUtil.hasBitMask(state, MUTUAL_AUTHENTICATED);
     *  
     * @param apdu the APDU
     * @return length of return APDU 
     */
    private short processMutualAuthenticate(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short bytesLeft = (short) (buffer[OFFSET_LC] & 0x00FF);

        if (buffer.length < 120)
            ISOException.throwIt((short) 0x6d66);

        if (bytesLeft != (short) 40)
            ISOException.throwIt(SW_WRONG_LENGTH);
        
        short e_ifd_p = OFFSET_CDATA;
        short e_ifd_length = 32;
        short m_ifd_p = (short)(e_ifd_p + e_ifd_length);
        
        // FIXME: is this call allowed in current state?

        short readCount = apdu.setIncomingAndReceive();
        while (bytesLeft > 0) {
            bytesLeft -= readCount;
            readCount = apdu.receiveBytes(OFFSET_CDATA);
        }

        // buffer[OFFSET_CDATA ... +40] consists of e_ifd || m_ifd
        // verify checksum m_ifd of cryptogram e_ifd
        if (!crypto.verifyMac(state, buffer, e_ifd_p, e_ifd_length, buffer, m_ifd_p))
            ISOException.throwIt((short) 0x6d01);

        // decrypt e_ifd into buffer[0] where buffer = rnd.ifd || rnd.icc || k.ifd
        short plaintext_len = crypto.decrypt(state, buffer, e_ifd_p, e_ifd_length, buffer, (short)0);
        if(plaintext_len != 32) // sanity check
            ISOException.throwIt((short)0x6d66);
        short rnd_ifd_p = 0;
        short rnd_icc_p = 8;
        short k_ifd_p = 16;
        /* pointers for writing intermediate data in buffer */
        short k_icc_p = (short)(k_ifd_p + SEED_LENGTH);
        short keySeed_p = (short)(k_icc_p + SEED_LENGTH); 
        short keys_p = (short)(keySeed_p + SEED_LENGTH);
        
        // verify that rnd.icc equals value generated in getChallenge
        if (Util.arrayCompare(buffer, rnd_icc_p, rnd, (short)0, (short)8) != 0)
            ISOException.throwIt((short) 0x6d02);

        // generate keying material k.icc
        randomData.generateData(buffer, k_icc_p, SEED_LENGTH);

        // calculate keySeed for session keys
        PassportUtil.xor(buffer, k_ifd_p, buffer, k_icc_p, buffer, keySeed_p, SEED_LENGTH);

        // calculate session keys
        PassportCrypto.deriveKey(buffer, keySeed_p, PassportCrypto.MAC_MODE, keys_p);
        short macKey_p = keys_p;
        keys_p += KEY_LENGTH;
        PassportCrypto.deriveKey(buffer, keySeed_p, PassportCrypto.ENC_MODE, keys_p);
        short encKey_p = keys_p;
        keys_p += KEY_LENGTH;
        crypto.setSessionKeys(buffer, macKey_p, encKey_p);

        // compute ssc
        PassportCrypto.computeSSC(buffer, rnd_icc_p, buffer, rnd_ifd_p, ssc);

        // create response in buffer where response = rnd.icc || rnd.ifd || k.icc
        PassportUtil.swap(buffer, rnd_icc_p, rnd_ifd_p, (short)8);
        Util.arrayCopy(buffer, k_icc_p, buffer, (short) 16, SEED_LENGTH);
        
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
     * //@ensures PassportUtil.hasBitMask(state, FILE_SELECTED);
     * 
     * @param apdu where the first 2 data bytes encode the file to select.
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
     * Processes incoming READ_BINARY APDUs. Returns data of the currently selected 
     * file.
     * 
     * //@ensures state == \old(state); 
     * 
     * @param apdu where the offset is carried in header bytes p1 and p2.
     * @param le expected length by terminal
     * @return length of the return APDU
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
            ISOException.throwIt(SW_WRONG_LENGTH);
        
        len = PassportUtil.min(le, (short)(file.length - offset));
        len = PassportUtil.min(len, (short)0xe0); //FIXME: magic
        len = PassportUtil.min(len, (short)buffer.length);
        Util.arrayCopy(file, offset, buffer, (short) 0, len);

        return len;
    }

    /***
     * Processes and UPDATE_BINARY apdu. Writes data in the currently selected file.
     * 
     * //@ensures state == \old(state);
     * 
     * @param apdu carries the offset where to write date in header bytes p1 and p2.
     */
    private void processUpdateBinary(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte p1 = buffer[OFFSET_P1];
        byte p2 = buffer[OFFSET_P2];

        short lc = (short) (buffer[OFFSET_LC] & 0xff);

        if(!PassportUtil.hasBitMask(state, FILE_SELECTED)) {
            ISOException.throwIt(SW_COMMAND_NOT_ALLOWED);
        }
        
        short offset = Util.makeShort(p1, p2);
        fileSystem.writeData(selectedFile, offset, buffer, OFFSET_CDATA, lc);
    }

    /***
     * Processes and CREATE_FILE apdu. This functionality is only partly implemented.
     * Only non-directories (files) can be created, all options for CREATE_FILE are 
     * ignored.
     * 
     * //@ensures state == \old(state);
     * 
     * @param apdu containing 6 bytes: 0x64 || (1 byte) || size (2) || fid (2)
     */
    private void processCreateFile(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[OFFSET_LC] & 0xff);

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
