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
import javacard.framework.Util;
import javacard.security.DESKey;
import javacard.security.KeyBuilder;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacardx.crypto.Cipher;

/**
 * Passport utilities for generating keys, based on Util from mrtd.
 * 
 * Wrap and unwrap for secure messaging based on SecureMessageWrapper from mrtd
 * 
 * Note that the key derivation methods in this class aren't really needed,
 * because all keys can be computed off-card.
 * 
 * @author Cees-Bart Breunesse <ceesb@cs.ru.nl>
 * @author Engelbert Hubbers <hubbers@cs.ru.nl>
 * @author Martijn Oostdijk <martijno@cs.ru.nl>
 * @author Ronny Wichers Schreur <ronny@cs.ru.nl>
 * 
 * @version $Revision$
 */
public class CREFPassportCrypto extends PassportCrypto implements ISO7816 {
    private static DESKey ma_kMac_a, ma_kMac_b, ma_kEnc;
    private static DESKey sm_kMac_a, sm_kMac_b, sm_kEnc;
    private static Cipher ciph;

    public static byte[] PAD_DATA = { (byte) 0x80, 0, 0, 0, 0, 0, 0, 0 };

    private static byte[] tempSpace_verifyMac;

    CREFPassportCrypto() {
        super();
        ciph = Cipher.getInstance(Cipher.ALG_DES_CBC_ISO9797_M2, false);
        sm_kMac_a = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                                 KeyBuilder.LENGTH_DES,
                                                 false);
        sm_kMac_b = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                                 KeyBuilder.LENGTH_DES,
                                                 false);
        ma_kMac_a = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                                 KeyBuilder.LENGTH_DES,
                                                 false);
        ma_kMac_b = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                                 KeyBuilder.LENGTH_DES,
                                                 false);
        sm_kEnc = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                               KeyBuilder.LENGTH_DES3_2KEY,
                                               false);
        ma_kEnc = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                               KeyBuilder.LENGTH_DES3_2KEY,
                                               false);

    }

    public static boolean verifyMac(DESKey kMac_a, DESKey kMac_b, byte[] msg,
            short msg_offset, short msg_len, byte[] mac, short mac_offset) {
        // byte[] cmac = new byte[8]; // FIXME: transient
        createMac(kMac_a,
                  kMac_b,
                  msg,
                  msg_offset,
                  msg_len,
                  tempSpace_verifyMac,
                  (short) 0);
        return Util.arrayCompare(tempSpace_verifyMac,
                                 (short) 0,
                                 mac,
                                 mac_offset,
                                 (short) 8) == 0;
    }

    // no more way to go for Signature.ALG_DES_MAC8_ISO9797_M2
    // public static void OLD_createMac(Cipher ciph, byte[] kMac, byte[] msg,
    // short msg_offset, short msg_len, byte[] mac, short mac_offset) {
    // if (kMac.length != 16 || msg_len != 32 || ciph == null
    // || (ciph.getAlgorithm() != Cipher.ALG_DES_CBC_ISO9797_M2))
    // ISOException.throwIt((short) 0x6d67);
    //
    // short keylen = (short) (kMac.length / 2);
    // short mac_len = 8;
    // DESKey kMac_a_key = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
    // KeyBuilder.LENGTH_DES, false);
    // DESKey kMac_b_key = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
    // KeyBuilder.LENGTH_DES, false);
    // kMac_a_key.setKey(kMac, (short) 0);
    // kMac_b_key.setKey(kMac, (short) keylen);
    //
    // // FIXME transient
    // byte[] enc_a_msg_pad = new byte[(short) (msg_len + PAD_DATA.length)];
    //
    // ciph.init(kMac_a_key, Cipher.MODE_ENCRYPT);
    // ciph.doFinal(msg, (short) 0, (short) msg_len, enc_a_msg_pad, (short) 0);
    //
    // // get the last 8 from enc_a_msg_pad, pad it, and use same buffer as
    // // dest
    // Util.arrayCopy(enc_a_msg_pad, (short) (enc_a_msg_pad.length - mac_len),
    // enc_a_msg_pad, (short) 0, (short) mac_len);
    // Util.arrayCopy(PAD_DATA, (short) 0, enc_a_msg_pad, (short) mac_len,
    // (short) PAD_DATA.length);
    //
    // // FIXME transient
    // byte[] dec_b_enc_a_msg_pad = new byte[mac_len];
    //
    // ciph.init(kMac_b_key, Cipher.MODE_DECRYPT);
    // try {
    // ciph.doFinal(enc_a_msg_pad, (short) 0, (short) 16,
    // dec_b_enc_a_msg_pad, (short) 0);
    // } catch (CryptoException ce) {
    // // BUG: a CryptoException 'illegal use' is thrown here by the cref
    // // implementation,
    // // the decryption is successful anyway ..
    // }
    //
    // // FIXME transient
    // byte[] enc_a_dec_b_enc_a_msg_pad = new byte[16];
    //
    // ciph.init(kMac_a_key, Cipher.MODE_ENCRYPT);
    // ciph.doFinal(dec_b_enc_a_msg_pad, (short) 0, (short) 8,
    // enc_a_dec_b_enc_a_msg_pad, (short) 0);
    //
    // Util.arrayCopy(enc_a_dec_b_enc_a_msg_pad, (short)0, mac, mac_offset,
    // (short)8);
    // //return enc_a_dec_b_enc_a_msg_pad;
    // }

    public static void decryptDESusingDESCBCM2(DESKey key, byte[] in,
            short in_offset, byte[] out, short out_offset, short length) {
        // assert ZERO.length == 8;
        if ((ciph.getAlgorithm() != Cipher.ALG_DES_CBC_ISO9797_M2)
                || ((short) (length + out_offset + 16) > (short) (out.length))
                || ((short) (length + in_offset) > (short) in.length))
            ISOException.throwIt((short) 0x6d69);

        ciph.init(key, Cipher.MODE_ENCRYPT);
        ciph.doFinal(ZERO,
                     (short) 0,
                     (short) 8,
                     tempSpace_decryptDES,
                     (short) 0);

        // Util.arrayCopy(dummy, (short)0, out, (short)0, (short)16);
        // Util.arrayCopy(in, in_offset, out, (short)16, in_length);

        // byte[] craparray = JCSystem.makeTransientByteArray((short)(length +
        // 16), JCSystem.CLEAR_ON_RESET); //new byte[(short)(length + 16)];
        // Util.arrayCopy(in, (short)0, craparray, (short)0, length);
        // Util.arrayCopy(tempSpace_decryptDES, (short)0, craparray, length,
        // (short)16);

        ciph.init(key, Cipher.MODE_DECRYPT);
        short written = ciph.update(in, in_offset, length, out, out_offset);
        try {
            ciph.doFinal(tempSpace_decryptDES,
                         (short) 0,
                         (short) (16),
                         out,
                         (short) (out_offset + written));
        } catch (Exception e) {
            // for(short i=0; i<(short)(length + 16); i++) {
            // short temp;
            // try {
            // temp = out[i];
            // temp = craparray[i];
            // } catch(ArrayIndexOutOfBoundsException ee) {
            // PassportUtil.throwShort((short)0x6d66);
            // }
            // }
            // if(JCSystem.isTransient(out) == JCSystem.CLEAR_ON_RESET
            // && JCSystem.isTransient(craparray) == JCSystem.CLEAR_ON_RESET)
            // PassportUtil.throwShort((short)0x6d68);
            // else
            PassportUtil.throwShort((short) 0x6d70);
        }
        // Util.arrayCopy(dummy, (short)0, out, (short)0, (short)8);
    }

    private static byte[] tempSpace_createMac;
    private static byte[] tempSpace_decryptDES;
    private static byte[] tempSpace_unwrapCommandAPDU;
    private static final byte[] ZERO = { 0, 0, 0, 0, 0, 0, 0, 0 };

    public static void initCryptoTempSpace() {
    }

    private static void createMac(DESKey kMac_a, DESKey kMac_b, byte[] msg,
            short msg_offset, short msg_len, byte[] mac, short mac_offset) {
        short blocksize = (short) 8;
        short blocks = (short) 3;
        if ((short) (blocksize * blocks) > (short) tempSpace_createMac.length)
            if (kMac_a == null || kMac_b == null || msg == null
                    || msg_len != 32 || ciph == null
                    || (ciph.getAlgorithm() != Cipher.ALG_DES_CBC_ISO9797_M2))
                if (msg.length < (short) (msg_offset + msg_len))
                    ISOException.throwIt((short) 0x6d67);

        ciph.init(kMac_a, Cipher.MODE_ENCRYPT);

        short off = msg_offset;
        short aligned = (short) (((short) msg_len) / blocksize);
        short rest = (short) (msg_len % blocksize);
        for (short i = 0; i < aligned; i++) {
            ciph.update(msg, off, blocksize, tempSpace_createMac, (short) 0);
            off += blocksize;
        }
        short last = ciph.doFinal(msg,
                                  off,
                                  rest,
                                  tempSpace_createMac,
                                  (short) 0);

        Util.arrayCopy(tempSpace_createMac,
                       (short) (last - 8),
                       mac,
                       (short) 0,
                       (short) 8);
        decryptDESusingDESCBCM2(kMac_b,
                                mac,
                                (short) 0,
                                tempSpace_createMac,
                                (short) 0,
                                (short) 8);
        Util.arrayCopy(tempSpace_createMac,
                       (short) 0,
                       mac,
                       (short) 0,
                       (short) 8);

        ciph.init(kMac_a, Cipher.MODE_ENCRYPT);
        ciph.doFinal(mac, (short) 0, (short) 8, tempSpace_createMac, (short) 0);

        Util.arrayCopy(tempSpace_createMac,
                       (short) 0,
                       mac,
                       (short) 0,
                       (short) 8);
    }

    public static short wrapResponseAPDU(byte[] ssc, DESKey kMac_a,
            DESKey kMac_b, DESKey kEnc, Cipher ciph, APDU aapdu,
            short plaintextLen, short sw1sw2) {
        byte[] apdu = aapdu.getBuffer();
        short apdu_p = 0;
        // smallest mod 8 strictly larger than plaintextLen, including do87 0x01 byte
        short do87DataLen = (short)((((plaintextLen + 8) / 8) * 8) + 1); 
        short do87HeaderLen = (short)(do87DataLen < 0x80 ? 3 : (4 + do87DataLen/0xff));
        short do87LenBytes = (short)(1 + do87DataLen/0xff);

        // insert SSC in front of apdu and reserve space for do87 header
        PassportCrypto.incrementSSC(ssc);
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
            
            ciph.init(kEnc, Cipher.MODE_ENCRYPT);
            if(plaintext_p != apdu_p) // sanity check
                ISOException.throwIt((short)0x6d66);
            short ciphertextLen = ciph.doFinal(apdu, plaintext_p, plaintextLen, apdu, apdu_p);
            if((do87DataLen - 1) != ciphertextLen) // sanity check
                ISOException.throwIt((short)0x6d66);
            apdu_p += ciphertextLen;
        }
        
        // build do99
        apdu[apdu_p++] = (byte) 0x99;
        apdu[apdu_p++] = 0x02;
        Util.setShort(apdu, apdu_p, sw1sw2);
        apdu_p += 2;
        
        // calculate mac on apdu[0 ... apdu_p]
        createMac(kMac_a, kMac_b, apdu, (short) 0, apdu_p,
                  tempSpace_unwrapCommandAPDU, (short) 0);

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
        Util.arrayCopy(tempSpace_unwrapCommandAPDU,
                       (short) 0,
                       apdu,
                       apdu_p,
                       (short) 8);
        apdu_p += 8;

        return apdu_p;
    }

    public static short unwrapCommandAPDU(byte[] ssc, DESKey ksMac_a,
            DESKey ksMac_b, DESKey ksEnc, Cipher ciph, APDU aapdu) {
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

        // we need the whole apdu in one buffer (and some room to spare),
        // otherwise
        // decrypting becomes a bitch.
        if (apdu.length < (short) (hdr_len + hdr_pad_len + lc + ssc.length))
            PassportUtil.throwShort((short) (hdr_len + hdr_pad_len + lc + ssc.length));

        if (apdu[count] == (byte) 0x87) {
            // do87
            do87_L = apdu[++count];
            if (apdu[++count] != 1) {
                ISOException.throwIt((short) (0x6d51));
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
                ISOException.throwIt((short) (0x6d52));
            le = (short) (apdu[++count] & 0xff);
            count++;
        }

        // do8e

        if (apdu[count] != (byte) 0x8e)
            ISOException.throwIt((short) (0x6d62));
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
        Util.arrayCopy(PAD_DATA, (short) 0, apdu, hdr_len, hdr_pad_len);
        count += hdr_pad_len;
        // put ssc in front to compute the mac
        PassportCrypto.incrementSSC(ssc);
        Util.arrayCopy(apdu, (short) 0, apdu, (short) ssc.length, count);
        Util.arrayCopy(ssc, (short) 0, apdu, (short) 0, (short) ssc.length);
        count += ssc.length;
        // verify the mac over all data except do8e
        mac_offset += (short) (ssc.length + hdr_pad_len);
        count -= (short) (do8e_len + zero_term);

        createMac(ksMac_a,
                  ksMac_b,
                  apdu,
                  (short) 0,
                  count,
                  tempSpace_unwrapCommandAPDU,
                  (short) 0);

        if (Util.arrayCompare(apdu,
                              mac_offset,
                              tempSpace_unwrapCommandAPDU,
                              (short) 0,
                              (short) 8) != 0) {

            PassportUtil.returnBuffer(apdu, (short) 0, count, aapdu);
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            return 0;
        }
        //   

        // PassportUtil.returnBuffer(apdu, (short)(ssc.length + hdr_len +
        // hdr_pad_len + do87_bytes), (short)(do87_L & 0xff), aapdu);

        // construct unprotected apdu
        // copy back the hdr
        Util.arrayCopy(apdu, (short) ssc.length, apdu, (short) 0, hdr_len);

        if (do87_L != 0) {
            // decrypt data, and leave room for lc
            decryptDESusingDESCBCM2(ksEnc,
                                    apdu,
                                    (short) (ssc.length + hdr_len + hdr_pad_len + do87_bytes),
                                    apdu,
                                    (short) (hdr_len + 1),
                                    (short) (do87_L & 0xff));
            apdu[hdr_len] = PassportUtil.calcLcFromPaddedData(apdu,
                                                              (short) (hdr_len + 1),
                                                              (short) (do87_L & 0xff));
        }
        // set le FIXME: remove
        if(do87_L == 0)
            apdu[hdr_len] = (byte)(le & 0xff);
        else
            apdu[(short) ((short) (hdr_len + 1) + (do87_L & 0xff))] = (byte) (le & 0xff);

        // empty out the rest
        short offset = (short)(hdr_len + 1 + (do87_L & 0xff));
        for(short i=offset; i<apdu.length; i++) {
            apdu[i] = 0;
        }
        
        // ISOException.throwIt((short)0x6d68);
        // PassportUtil.returnBuffer(apdu, (short)0,
        // (short)((short)((short)(do87_L & 0xff) + hdr_len) + 1), aapdu);

        return le;
    }

    public void createTempSpace() {
        tempSpace_createMac = JCSystem.makeTransientByteArray((short) 24,
                                                              JCSystem.CLEAR_ON_RESET);
        tempSpace_decryptDES = JCSystem.makeTransientByteArray((short) 16,
                                                               JCSystem.CLEAR_ON_RESET);
        tempSpace_verifyMac = JCSystem.makeTransientByteArray((short) 8,
                                                              JCSystem.CLEAR_ON_RESET);
        tempSpace_unwrapCommandAPDU = JCSystem.makeTransientByteArray((short) 8,
                                                                      JCSystem.CLEAR_ON_RESET);
    }

    public void setSessionKeys(byte[] macKey, byte[] encKey) {
        sm_kMac_a.setKey(macKey, (short) 0);
        sm_kMac_b.setKey(macKey, (short) 8);
        sm_kEnc.setKey(encKey, (short) 0);
    }
    
    public void setMutualAuthKeys(byte[] macKey, byte[] encKey) {
        ma_kMac_a.setKey(macKey, (short) 0);
        ma_kMac_b.setKey(macKey, (short) 8);
        ma_kEnc.setKey(encKey, (short) 0);
    }

    public boolean verifyMac(byte state, byte[] msg, short msg_offset,
            short msg_len, byte[] mac, short mac_offset) {
        if (state == PassportApplet.MUTUAL_AUTHENTICATED) {
            return verifyMac(sm_kMac_a,
                             sm_kMac_b,
                             msg,
                             msg_offset,
                             msg_len,
                             mac,
                             mac_offset);
        } else if (state == PassportApplet.CHALLENGED) {
            return verifyMac(ma_kMac_a,
                             ma_kMac_b,
                             msg,
                             msg_offset,
                             msg_len,
                             mac,
                             mac_offset);
        }

        return false;
    }

    public void createMac(byte state, byte[] msg, short msg_offset,
            short msg_len, byte[] mac, short mac_offset) {
        if ((state & PassportApplet.MUTUAL_AUTHENTICATED) ==  PassportApplet.MUTUAL_AUTHENTICATED) {
            createMac(sm_kMac_a,
                      sm_kMac_b,
                      msg,
                      msg_offset,
                      msg_len,
                      mac,
                      mac_offset);
        } else if ((state & PassportApplet.CHALLENGED) == PassportApplet.CHALLENGED) {
            createMac(ma_kMac_a,
                      ma_kMac_b,
                      msg,
                      msg_offset,
                      msg_len,
                      mac,
                      mac_offset);
        }
    }

    public short  unwrapCommandAPDU(byte[] ssc, APDU apdu) {
        return unwrapCommandAPDU(ssc, sm_kMac_a,
                          sm_kMac_b, sm_kEnc, ciph, apdu);

    }

    public short wrapResponseAPDU(byte[] ssc, APDU apdu, short len, short sw1sw2) {
        return wrapResponseAPDU(ssc, sm_kMac_a, sm_kMac_b, sm_kEnc, ciph, apdu,
                         len, sw1sw2);
               

    }

    public void decrypt(byte state, byte[] ctext, short ctext_offset,
            short ctext_len, byte[] ptext, short ptext_offset) {
        if ((state & PassportApplet.MUTUAL_AUTHENTICATED) == PassportApplet.MUTUAL_AUTHENTICATED) {
            decryptDESusingDESCBCM2(sm_kEnc,
                                    ctext,
                                    ctext_offset,
                                    ptext,
                                    ptext_offset,
                                    ctext_len);
        }
        else if((state & PassportApplet.CHALLENGED) == PassportApplet.CHALLENGED) {
            decryptDESusingDESCBCM2(ma_kEnc,
                                    ctext,
                                    ctext_offset,
                                    ptext,
                                    ptext_offset,
                                    ctext_len);
            
        }
    }

    public void encrypt(byte state, byte[] ptext, short ptext_offset,
            short ptext_len, byte[] ctext, short ctext_offset) {
        DESKey k=null;
        
        if ((state & PassportApplet.MUTUAL_AUTHENTICATED) == PassportApplet.MUTUAL_AUTHENTICATED) {
            k = sm_kEnc;
        }
        else if((state & PassportApplet.CHALLENGED) == PassportApplet.CHALLENGED) {
            k = ma_kEnc;            
        }

        ciph.init(k, Cipher.MODE_ENCRYPT);
        ciph.doFinal(ptext, ptext_offset, ptext_len, ctext, ctext_offset);
    }
}
