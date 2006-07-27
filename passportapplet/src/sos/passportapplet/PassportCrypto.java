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
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.CryptoException;
import javacard.security.MessageDigest;
import javacard.security.KeyBuilder;
import javacard.security.RSAPrivateKey;
import javacard.security.RSAPublicKey;

public abstract class PassportCrypto {
    public static final byte ENC_MODE = 1;
    public static final byte MAC_MODE = 2;

    public static final byte CREF_MODE = 3;
    public static final byte JCOP_MODE = 4;

    public static final byte INPUT_IS_NOT_PADDED = 5;
    public static final byte INPUT_IS_PADDED = 6;

    public static final byte PAD_INPUT = 7;
    public static final byte DONT_PAD_INPUT = 8;

    private static MessageDigest shaDigest;
    static byte[] tempSpace_unwrapCommandAPDU;
    RSAPrivateKey rsaPrivateKey;
    RSAPublicKey rsaPublicKey;

    public PassportCrypto() {
        tempSpace_unwrapCommandAPDU = JCSystem.makeTransientByteArray((short) 8,
                                                                      JCSystem.CLEAR_ON_RESET);
        shaDigest = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
        
        rsaPrivateKey = (RSAPrivateKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_1024,  false);
        rsaPublicKey =  (RSAPublicKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024,  false);
    }

//    public void setAAPublicKey(byte[] buffer, short exp_offset, short exp_length) {
//        rsaPublicKey.setExponent(buffer, offset, RSA_EXP_LENGTH);
//        rsaPublicKey.setModulus(buffer, offset + RSA_EXP_LENGTH, RSA_MOD_LENGTH);
//    }
//    
//    public void setAAPrivateKey(byte[] buffer, short offset, short length) {
//        rsaPrivateKey.setExponent(buffer, offset, RSA_EXP_LENGTH);
//        rsaPrivateKey.setModulus(buffer, offset + RSA_EXP_LENGTH, RSA_MOD_LENGTH);
//    }
    
    public abstract boolean verifyMac(byte state, byte[] msg, short msg_offset,
            short msg_len, byte[] mac, short mac_offset);

    public abstract void createMac(byte state, byte[] msg, short msg_offset,
            short msg_len, byte[] mac, short mac_offset);

    public abstract void setSessionKeys(byte[] keys, short macKey_p, short encKey_p);

    public abstract void setMutualAuthKeys(byte[] keys, short macKey_p, short encKey_p);

    public abstract short decrypt(byte state, byte[] ctext, short ctext_offset,
            short ctext_len, byte[] ptext, short ptext_offset);

    public abstract short encrypt(byte state, byte padding, byte[] ptext,
            short ptext_offset, short ptext_len, byte[] ctext,
            short ctext_offset);

    public short unwrapCommandAPDU(byte[] ssc, APDU aapdu) {
        byte[] apdu = aapdu.getBuffer();
        short apdu_p = (short) (ISO7816.OFFSET_CDATA & 0xff);
        // offset
        short lc = (short) (apdu[ISO7816.OFFSET_LC] & 0xff);
        short le = 0;
        short do87DataLen = 0;
        short do87Data_p = 0;
        short do87LenBytes = 0;
        short hdrLen = 4;
        short hdrPadLen = (short) (8 - hdrLen);
        short apduLength = (short) (hdrLen + 1 + lc);

        aapdu.setIncomingAndReceive();

        // sanity check
        if (apdu.length < (short) (apduLength + hdrPadLen + ssc.length)) {
            ISOException.throwIt((short)0x6d66);
        }

        // pad the header now (needed to calculate the mac) so we don't have to
        // modify pointers to locations in the apdu later.
        Util.arrayCopy(apdu, (short) (hdrLen + 1), // toss away lc
                       apdu, (short) (hdrLen + hdrPadLen), lc);
        Util.arrayCopy(CREFPassportCrypto.PAD_DATA,
                       (short) 0,
                       apdu,
                       hdrLen,
                       hdrPadLen);
        apduLength--;
        apduLength += hdrPadLen;

        // add ssc in front (needed to calculate the mac) so we don't have to
        // modify pointers to locations in the apdu later.
        incrementSSC(ssc);
        Util.arrayCopy(apdu, (short) 0, apdu, (short) ssc.length, apduLength);
        Util.arrayCopy(ssc, (short) 0, apdu, (short) 0, (short) ssc.length);

        apdu_p = (short) (hdrLen + hdrPadLen);
        apdu_p += (short) ssc.length;

        if (apdu[apdu_p] == (byte) 0x87) {
            apdu_p++;
            // do87
            if ((apdu[apdu_p] & 0xff) > 0x80) {
                do87LenBytes = (short) (apdu[apdu_p] & 0x7f);
                apdu_p++;
            } else {
                do87LenBytes = 1;
            }
            if (do87LenBytes > 2) { // sanity check
                ISOException.throwIt((short) 0x6d66);
            }
            for (short i = 0; i < do87LenBytes; i++) {
                do87DataLen += (short) ((apdu[apdu_p + i] & 0xff) << (short) ((do87LenBytes - 1 - i) * 8));
            }
            apdu_p += do87LenBytes;

            if (apdu[apdu_p] != 1) {
                ISOException.throwIt((short) (0x6d66));
            }
            // store pointer to data and defer decrypt to after mac check (do8e)
            do87Data_p = (short) (apdu_p + 1);
            apdu_p += do87DataLen;
            do87DataLen--; // compensate for 0x01 marker
        }

        if (apdu[apdu_p] == (byte) 0x97) {
            // do97
            if (apdu[++apdu_p] != 1)
                ISOException.throwIt((short) (0x6d66));
            le = (short) (apdu[++apdu_p] & 0xff);
            apdu_p++;
        }

        // do8e
        if (apdu[apdu_p] != (byte) 0x8e) {
            ISOException.throwIt((short) (0x6d66));
        }
        if (apdu[++apdu_p] != 8) {
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }

        if (!verifyMac(PassportApplet.MUTUAL_AUTHENTICATED,
                       apdu,
                       (short) 0,
                       (short) (apdu_p - 1),
                       apdu,
                       (short)(apdu_p + 1))) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        // construct unprotected apdu
        // copy back the hdr
        Util.arrayCopy(apdu, (short) ssc.length, apdu, (short) 0, hdrLen);
        apduLength -= 4;

        short plaintextLength = 0;
        short plaintextLc = 0;
        if (do87DataLen != 0) {
            // decrypt data, and leave room for lc
            plaintextLength = decrypt(PassportApplet.MUTUAL_AUTHENTICATED,
                                      apdu,
                                      do87Data_p,
                                      do87DataLen,
                                      apdu,
                                      (short) (hdrLen + 1));

            plaintextLc = PassportUtil.calcLcFromPaddedData(apdu,
                                                            (short) (hdrLen + 1),
                                                            do87DataLen);
            apdu[hdrLen] = (byte) (plaintextLc & 0xff);
        }

        apduLength = (short) (hdrLen + 1 + plaintextLength);

        // empty out the rest
        for (short i = apduLength; i < apdu.length; i++) {
            apdu[i] = 0;
        }

        return le;
    }

    public short wrapResponseAPDU(byte[] ssc, APDU aapdu, short plaintextLen,
            short sw1sw2) {
        byte[] apdu = aapdu.getBuffer();
        short apdu_p = 0;
        // smallest mod 8 strictly larger than plaintextLen, including do87 0x01
        // byte
        short do87DataLen = (short) ((((short) (plaintextLen + 8) / 8) * 8) + 1);
        short do87HeaderLen = (short) (do87DataLen < 0x80 ? 3
                : (4 + do87DataLen / 0xff));
        short do87LenBytes = (short) (1 + do87DataLen / 0xff);

        // insert SSC in front of apdu and reserve space for do87 header
        incrementSSC(ssc);
        short plaintext_p = (short) (ssc.length + do87HeaderLen);
        Util.arrayCopy(apdu, (short) 0, apdu, plaintext_p, plaintextLen);
        Util.arrayCopy(ssc, (short) 0, apdu, apdu_p, (short) ssc.length);
        apdu_p += (short) ssc.length;

        if (plaintextLen > 0) {
            // build do87
            apdu[apdu_p++] = (byte) 0x87;
            if (do87HeaderLen > 3) {
                apdu[apdu_p++] = (byte) (0x80 + do87LenBytes);
            }
            for (short i = 0; i < do87LenBytes; i++) {
                apdu[apdu_p++] = (byte) ((do87DataLen >>> i * 8) & 0xff);
            }

            apdu[apdu_p++] = 0x01;

            // sanity check
            if (plaintext_p != apdu_p)
                ISOException.throwIt((short) 0x6d66);
            short ciphertextLen = encrypt(PassportApplet.MUTUAL_AUTHENTICATED,
                                          PassportCrypto.PAD_INPUT,
                                          apdu,
                                          plaintext_p,
                                          plaintextLen,
                                          apdu,
                                          apdu_p);
            // sanity check
            if ((short) (do87DataLen - 1) != ciphertextLen)
                ISOException.throwIt((short) 0x6d66);
            apdu_p += ciphertextLen;
        }

        // build do99
        apdu[apdu_p++] = (byte) 0x99;
        apdu[apdu_p++] = 0x02;
        Util.setShort(apdu, apdu_p, sw1sw2);
        apdu_p += 2;

        // calculate mac on apdu[0 ... apdu_p]
        createMac(PassportApplet.MUTUAL_AUTHENTICATED,
                  apdu,
                  (short) 0,
                  apdu_p,
                  PassportCrypto.tempSpace_unwrapCommandAPDU,
                  (short) 0);

        // now delete ssc from apdu (shift left apdu by 8 bytes)
        // so we have room to write the do8e
        Util.arrayCopy(apdu,
                       (short) ssc.length,
                       apdu,
                       (short) 0,
                       (short) (apdu_p - ssc.length));
        apdu_p -= (short) ssc.length;

        // write do8e
        apdu[apdu_p++] = (byte) 0x8e;
        apdu[apdu_p++] = 0x08;
        Util.arrayCopy(PassportCrypto.tempSpace_unwrapCommandAPDU,
                       (short) 0,
                       apdu,
                       apdu_p,
                       (short) 8);
        apdu_p += 8;

        return apdu_p;
    }

    /**
     * Derives the ENC or MAC key from the keySeed.
     * 
     * @param keySeed
     *            the key seed.
     * @param mode
     *            either <code>ENC_MODE</code> or <code>MAC_MODE</code>.
     * 
     * @return the key.
     */
    static byte[] c = { 0x00, 0x00, 0x00, 0x00 };

    public static void deriveKey(byte[] buffer, short keySeed_offset, byte mode, short key_offset)
            throws CryptoException {
        // only key_offset is a write pointer
        // sanity checks
        if ((short)buffer.length < (short)(key_offset + 80)) {
            ISOException.throwIt((short)0x6d66);
        }
        if(keySeed_offset > key_offset) {
            ISOException.throwIt((short)0x6d66);
        }

        c[(short)(c.length-1)] = mode;

        // copy seed || c to key_offset
        Util.arrayCopy(buffer, keySeed_offset, buffer, key_offset, PassportApplet.SEED_LENGTH);
        Util.arrayCopy(c, (short) 0, buffer, (short)(key_offset + PassportApplet.SEED_LENGTH), (short)c.length);

        // compute hash on key_offset (+seed len +c len)
        shaDigest.doFinal(buffer, key_offset, (short)(PassportApplet.SEED_LENGTH + c.length), buffer, key_offset);
        shaDigest.reset();

        // parity bits
        for (short i = key_offset; i < (short)(key_offset + PassportApplet.KEY_LENGTH); i++) {
            if (PassportUtil.evenBits(buffer[i]) == 0)
                buffer[i] = (byte) (buffer[i] ^ 1);
        }
    }

    public static void incrementSSC(byte[] ssc) {
        if (ssc == null || ssc.length <= 0)
            return;

        for (short s = (short) (ssc.length - 1); s >= 0; s--) {
            if ((short) ((ssc[s] & 0xff) + 1) > 0xff)
                ssc[s] = 0;
            else {
                ssc[s]++;
                break;
            }
        }
    }

    public static void computeSSC(byte[] rndICC, short rndICC_offset,
            byte[] rndIFD, short rndIFD_offset, byte[] ssc) {
        if (rndICC == null || (short) (rndICC.length - rndICC_offset) < 8
                || rndIFD == null
                || (short) (rndIFD.length - rndIFD_offset) < 8) {
            ISOException.throwIt((short) 0x6d66);
        }

        Util.arrayCopy(rndICC,
                       (short) (rndICC_offset + 4),
                       ssc,
                       (short) 0,
                       (short) 4);
        Util.arrayCopy(rndIFD,
                       (short) (rndIFD_offset + 4),
                       ssc,
                       (short) 4,
                       (short) 4);
    }

    public static void createHash(byte[] msg, short msg_offset, short length,
            byte[] dest, short dest_offset) throws CryptoException {
        if ((dest.length < (short) (dest_offset + length))
                || (msg.length < (short) (msg_offset + length)))
            ISOException.throwIt((short) 0x6d66);

        try {
            shaDigest.doFinal(msg, msg_offset, length, dest, dest_offset);
        } finally {
            shaDigest.reset();
        }

    }
}
