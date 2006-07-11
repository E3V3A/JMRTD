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
    
    public PassportCrypto() {
       tempSpace_unwrapCommandAPDU = JCSystem.makeTransientByteArray((short) 8, JCSystem.CLEAR_ON_RESET);
       shaDigest = MessageDigest.getInstance(MessageDigest.ALG_SHA, false); 
    }

    public abstract boolean verifyMac(byte state, byte[] msg, short msg_offset, short msg_len, byte[] mac, short mac_offset);
    
    public abstract void createMac(byte state, byte[] msg, short msg_offset, short msg_len, byte[] mac, short mac_offset);
    
    public abstract void setSessionKeys(byte[] macKey, byte[] encKey);
    
    public abstract void setMutualAuthKeys(byte[] macKey, byte[] encKey);
    
    public abstract short decrypt(byte state, byte[] ctext, short ctext_offset,
            short ctext_len, byte[] ptext, short ptext_offset);
    
    public abstract short encrypt(byte state, byte padding,  byte[] ptext, short ptext_offset,
            short ptext_len, byte[] ctext, short ctext_offset);

    public short unwrapCommandAPDU(byte[] ssc, APDU aapdu) {
            byte[] apdu = aapdu.getBuffer();
            short count = (short) (ISO7816.OFFSET_CDATA & 0xff); // len and
            // offset
            short lc = (short) (apdu[ISO7816.OFFSET_LC] & 0xff); // len
            short le = 0; // len
            byte do87_L = 0; // len
            short zero_term = 1; // 0 or 1
            short hdr_len = 4;
            short hdr_pad_len = 4;
            short do87_bytes = 2;
    
            aapdu.setIncomingAndReceive();
    
            // we currently need the whole apdu in one buffer (and some room to spare),
            if (apdu.length < (short) (hdr_len + hdr_pad_len + lc + ssc.length))
                PassportUtil.throwShort((short) (hdr_len + hdr_pad_len + lc + ssc.length));
    
            if (apdu[count] == (byte) 0x87) {
                // do87
                do87_L = apdu[++count];
                if (apdu[++count] != 1) {
                    ISOException.throwIt((short) (0x6d66));
                }
                // defer decrypt to after mac check (do8e)
                count += (short) (do87_L & 0xff);
                // skip leading one byte
                do87_L--;
                do87_bytes++;
            }
    
            if (apdu[count] == (byte) 0x97) {
                // do97
                if (apdu[++count] != 1)
                    // PassportUtil.throwShort(count);
                    ISOException.throwIt((short) (0x6d66));
                le = (short) (apdu[++count] & 0xff);
                count++;
            }
    
            // do8e
    
            if (apdu[count] != (byte) 0x8e)
                ISOException.throwIt((short) (0x6d66));
            if (apdu[++count] != 8)
                ISOException.throwIt(ISO7816.SW_DATA_INVALID);
            short do8e_len = 10; // FIXME
            short mac_offset = count;
            // make count point to position after mac (no more input data after
            // that)
            count += 8;
            count += zero_term;
    
            // to verify the mac, we have to pad/add 4 bytes after the hdr
            // and we toss away the old lc as well
            Util.arrayCopy(apdu,
                           (short) (hdr_len + 1),
                           apdu,
                           (short) (hdr_len + hdr_pad_len),
                           count);
            Util.arrayCopy(CREFPassportCrypto.PAD_DATA, (short) 0, apdu, hdr_len, hdr_pad_len);
            count += hdr_pad_len;
            // put ssc in front to compute the mac
            incrementSSC(ssc);
            Util.arrayCopy(apdu, (short) 0, apdu, (short) ssc.length, count);
            Util.arrayCopy(ssc, (short) 0, apdu, (short) 0, (short) ssc.length);
            count += ssc.length;
            // verify the mac over all data except do8e
            mac_offset += (short) (ssc.length + hdr_pad_len);
            count -= (short) (do8e_len + zero_term);
    
            createMac(PassportApplet.MUTUAL_AUTHENTICATED,
                      apdu,
                      (short) 0,
                      count,
                      PassportCrypto.tempSpace_unwrapCommandAPDU,
                      (short) 0);
    
            if (Util.arrayCompare(apdu,
                                  mac_offset,
                                  PassportCrypto.tempSpace_unwrapCommandAPDU,
                                  (short) 0,
                                  (short) 8) != 0) {
    
                PassportUtil.returnBuffer(apdu, (short) 0, count, aapdu);
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
                return 0;
            }
 
            // construct unprotected apdu
            // copy back the hdr
            Util.arrayCopy(apdu, (short) ssc.length, apdu, (short) 0, hdr_len);
    
            if (do87_L != 0) {
                // decrypt data, and leave room for lc
                decrypt(PassportApplet.MUTUAL_AUTHENTICATED, 
                                     apdu,
                                   (short) (ssc.length + hdr_len + hdr_pad_len + do87_bytes),
                                   (short) (do87_L & 0xff),
                                   apdu,
                                   (short) (hdr_len + 1));
                                     
                apdu[hdr_len] = PassportUtil.calcLcFromPaddedData(apdu,
                                                                  (short) (hdr_len + 1),
                                                                  (short) (do87_L & 0xff));
            }
            // set le FIXME: remove
//            if(do87_L == 0)
//                apdu[hdr_len] = (byte)(le & 0xff);
//            else
//                apdu[(short) ((short) (hdr_len + 1) + (do87_L & 0xff))] = (byte) (le & 0xff);
    
            // empty out the rest
            short offset = (short)(hdr_len + 1 + (do87_L & 0xff));
            for(short i=offset; i<apdu.length; i++) {
                apdu[i] = 0;
            }

            return le;
        }

    public short wrapResponseAPDU(byte[] ssc,  APDU aapdu, short plaintextLen, short sw1sw2) {
            byte[] apdu = aapdu.getBuffer();
            short apdu_p = 0;
            // smallest mod 8 strictly larger than plaintextLen, including do87 0x01 byte
            short do87DataLen = (short)((((short)(plaintextLen + 8) / 8) * 8) + 1); 
            short do87HeaderLen = (short)(do87DataLen < 0x80 ? 3 : (4 + do87DataLen/0xff));
            short do87LenBytes = (short)(1 + do87DataLen/0xff);
    
            // insert SSC in front of apdu and reserve space for do87 header
            incrementSSC(ssc);
            short plaintext_p = (short)(ssc.length + do87HeaderLen);        
            Util.arrayCopy(apdu, (short)0, apdu, plaintext_p, plaintextLen); 
            Util.arrayCopy(ssc, (short) 0, apdu, apdu_p, (short) ssc.length);
            apdu_p += (short) ssc.length;
    
            if (plaintextLen > 0) {
                // build do87
                apdu[apdu_p++] = (byte) 0x87;
                if(do87HeaderLen > 3) {
                    apdu[apdu_p++] = (byte)(0x80 + do87LenBytes);
                }
                for(short i=0; i<do87LenBytes; i++) {
                    apdu[apdu_p++] = (byte)((do87DataLen >>> i) & 0xff);
                }   
                      
                apdu[apdu_p++] = 0x01;
                
                // sanity check
                if(plaintext_p != apdu_p)
                    ISOException.throwIt((short)0x6d66);
                short ciphertextLen = encrypt(PassportApplet.MUTUAL_AUTHENTICATED, PassportCrypto.PAD_INPUT,
                                              apdu, plaintext_p, plaintextLen, apdu, apdu_p);
                // sanity check
                if((short)(do87DataLen - 1) != ciphertextLen)
                    ISOException.throwIt((short)0x6d66);
                apdu_p += ciphertextLen;
            }
            
            // build do99
            apdu[apdu_p++] = (byte) 0x99;
            apdu[apdu_p++] = 0x02;
            Util.setShort(apdu, apdu_p, sw1sw2);
            apdu_p += 2;
            
            // calculate mac on apdu[0 ... apdu_p]
            createMac(PassportApplet.MUTUAL_AUTHENTICATED, apdu, (short) 0, apdu_p,
                      PassportCrypto.tempSpace_unwrapCommandAPDU, (short) 0);
    
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
    public static byte[] deriveKey(byte[] keySeed,
            byte mode) throws CryptoException {
        if (keySeed.length != 16)
            CryptoException.throwIt(CryptoException.ILLEGAL_VALUE);
    
        /*
         * MessageDigest shaDigest = MessageDigest.getInstance(
         * MessageDigest.ALG_SHA, false); // FIXME: false?
         */
    
        byte[] c = { 0x00, 0x00, 0x00, (byte) mode };
        byte[] d = new byte[(short) 20];
        Util.arrayCopy(keySeed, (short) 0, d, (short) 0, (short) 16);
        Util.arrayCopy(c, (short) 0, d, (short) 16, (short) 4);
    
        byte[] sha = new byte[(short) 80];
        shaDigest.doFinal(d, (short) 0, (short) 20, sha, (short) 0);
        shaDigest.reset();
        byte[] keydata = new byte[16];
        Util.arrayCopy(sha, (short) 0, keydata, (short) 0, (short) 16);
        /*
         * DESKey key = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
         * KeyBuilder.LENGTH_DES3_2KEY, false); // FIXME: false?
         */
    
        // adjust parity bits
        for (short i = 0; i < 16; i++) {
            if (PassportUtil.evenBits(keydata[i]) == 0)
                keydata[i] = (byte) (keydata[i] ^ 1);
        }
    
        // key.setKey(keydata, (short) 0);
        return keydata;
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

    public static void computeSSC(byte[] rndICC, short rndICC_offset,  byte[] rndIFD, short rndIFD_offset, byte[] ssc) {
        if (rndICC == null || 
            (short)(rndICC.length - rndICC_offset) < 8 || 
            rndIFD == null || 
            (short)(rndIFD.length - rndIFD_offset) < 8) {
            ISOException.throwIt((short) 0x6d66);
        }
        
        Util.arrayCopy(rndICC, (short)(rndICC_offset + 4), ssc, (short) 0, (short) 4);
        Util.arrayCopy(rndIFD, (short)(rndIFD_offset + 4), ssc, (short) 4, (short) 4);
    }

    public static void createHash(byte[] msg, short msg_offset, short length, byte[] dest, short dest_offset) 
    throws CryptoException {
        if((dest.length < (short)(dest_offset + length)) ||
           (msg.length < (short)(msg_offset + length)))
            ISOException.throwIt((short)0x6d66);
        
        try { 
            shaDigest.doFinal(msg, msg_offset, length, dest, dest_offset);
        } finally {
            shaDigest.reset();
        }
        
    }
}
