/*
 * $Id: CREFPassportCrypto.java,v 1.1 2006/07/05 13:49:02 ceesb Exp $
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
 * @version $Revision: 1.1 $
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

    // probably good when Signature.ALG_DES_MAC8_ISO9797_1_M2_ALG3
    // public static void UNUSED_createMac(Signature sig, DESKey kMac, byte[]
    // msg,
    // short msg_offset, short msg_len, byte[] mac, short mac_offset) {
    // sig.init(kMac, Signature.MODE_SIGN);
    // sig.sign(msg, msg_offset, msg_len, mac, mac_offset);
    // }

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

    /**
     * Does the actual encoding of a command apdu. Based on Section E.3 of
     * ICAO-TR-PKI, especially the examples.
     * 
     * @param capdu
     *            buffer containing the apdu data.
     * @param capdu_len
     *            length of the apdu data.
     * 
     * @return a byte array containing the wrapped apdu buffer.
     */
    /*
     * @ requires apdu != null && 4 <= len && len <= apdu.length;
     */
    // private static void wrapCommandAPDU(byte[] ssc, DESKey ksMac_a, DESKey
    // ksMac_b, DESKey ksEnc_key, Cipher ciph, byte[] capdu, short capdu_len,
    // byte[] out, short out_len) {
    // if (capdu == null || capdu.length < 4 || capdu_len < 4) {
    // // maybe copy "IDIOT" in out buffer as well?
    // return;
    // }
    //
    // /* Determine lc and le... */
    // byte lc = 0;
    // byte le = capdu[(short)(capdu_len - 1)];
    // if (capdu_len == 4) {
    // lc = 0;
    // le = 0;
    // } else if (capdu_len == 5) {
    // /* No command data, byte at index 5 is le. */
    // lc = 0;
    // } else if (capdu_len > 5) {
    // /* Byte at index 5 is not le, so it must be lc. */
    // lc = capdu[ISO7816.OFFSET_LC];
    // }
    // if (4 + lc >= capdu_len) {
    // /* Value of lc covers rest of apdu length, there is no le. */
    // le = 0;
    // }
    //
    // short current;
    // short hdr_len = PassportUtil.min((short)4, capdu_len);
    // Util.arrayCopy(capdu, (short)0, out, (short)0, hdr_len);
    // out[ISO7816.OFFSET_CLA] = (byte)0x0C;
    // // we pad the header only for calculating the mac, later
    // Util.arrayCopy(PAD_DATA, (short)0, out, hdr_len, (short)(8 - hdr_len));
    //      
    // current = (short)8;
    //       
    // // write this now, we need it for the mac, overwritten later
    // incrementSSC(ssc);
    // Util.arrayCopy(ssc, (short)0, out, current, (short)ssc.length);
    // current += (short)ssc.length;
    //       
    // if (lc > 0) {
    // // do87, optional
    // short count;
    // ciph.init(ksEnc_key, Cipher.MODE_ENCRYPT);
    // count = ciph.doFinal(capdu, (short)(ISO7816.OFFSET_CDATA & 0xff),
    // (short)(lc & 0xff), out, (short)(current + 3));
    //
    // out[current++] = (byte)0x87;
    // out[current++] = (byte)count;
    // out[current++] = (byte)0x01;
    //           
    // current = (short)(current + count);
    // }
    //
    // if (le > 0) {
    // // do97, optional
    // out[current++] = (byte)0x97;
    // out[current++] = (byte)0x01;
    // out[current++] = le;
    // }
    //       
    // /* Compute cryptographic checksum... */
    //
    // // save the mac for now, note we don't add new padding,
    // // as the des mac iso9797 m2 does it for us
    // byte[] mac = new byte[8];
    // createMac(ciph, ksMac_a, ksMac_b, out, (short)0, current, mac, (short)0);
    //       
    // // kill ssc in out (first 8 bytes)
    // current -= ssc.length;
    // Util.arrayCopy(out, (short)ssc.length, out, (short)0, current);
    //       
    // // kill the padding of the header in out
    // current -= (short)(8 - hdr_len);
    // Util.arrayCopy(out, (short)8, out, hdr_len, current);
    //       
    // // add do8E
    // out[current++] = (byte)0x8E;
    // out[current++] = (byte)8;
    // Util.arrayCopy(mac, (short)0, out, current, (short)8);
    //       
    // // set lc
    // out[4] = (byte)(current - 4);
    //
    // // trailing zero?
    // out[current++] = 0;
    // }
    public static short wrapResponseAPDU(byte[] ssc, DESKey kMac_a,
            DESKey kMac_b, DESKey kEnc, Cipher ciph, APDU aapdu,
            short rapdu_len, short sw1sw2) {
        byte[] apdu = aapdu.getBuffer();
        short count = 0;

        // write this now, we need it for the mac only, overwritten later
        PassportCrypto.incrementSSC(ssc);
        Util.arrayCopy(ssc, (short) 0, apdu, count, (short) ssc.length);
        count += (short) ssc.length;

        if (rapdu_len > 0) {
            // build do87
            apdu[count++] = (byte) 0x87;
            apdu[count++] = (byte) 0xff;
            short do87_L_pos = count;
            apdu[count++] = 0x01;
            ciph.init(kEnc, Cipher.MODE_ENCRYPT);
            short L = ciph.doFinal(apdu, (short) 0, rapdu_len, apdu, count);
            apdu[do87_L_pos] = (byte) (L & 0xff);
        }

        // build do99
        byte sw1 = (byte) 0x90, sw2 = 0;
        if (sw1sw2 != 0) {
            sw1 = (byte) ((sw1sw2 & (short) 0xff00) >>> (short) 8);
            sw2 = (byte) (sw1sw2 & 0xff);
        }

        apdu[count++] = (byte) 0x99;
        apdu[count++] = 0x02;
        apdu[count++] = sw1;
        apdu[count++] = sw2;

        // build do8e
        createMac(kMac_a, kMac_b, apdu, (short) 0, count,
        // apdu, (short)(count + 2)
                  tempSpace_unwrapCommandAPDU, (short) 0);
        apdu[count++] = (byte) 0x8e;
        apdu[count++] = 0x08;
        Util.arrayCopy(tempSpace_unwrapCommandAPDU,
                       (short) 0,
                       apdu,
                       count,
                       (short) 8);
        count += 8; // compensate for mac len

        // now delete ssc from apdu (shift left apdu by 8 bytes)
        Util.arrayCopy(apdu,
                       (short) ssc.length,
                       apdu,
                       (short) 0,
                       (short) (count - ssc.length));
        count -= (short) ssc.length;

        return count;
    }

    public static void unwrapCommandAPDU(byte[] ssc, DESKey ksMac_a,
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

        if ((PassportApplet.getState() & PassportApplet.FILE_SELECTED) != 0) {
            if (apdu[count] != (byte) 0x8e) {
                PassportUtil.returnBuffer(apdu, (short) (count + 1), aapdu);
                PassportUtil.throwArrayIndex(apdu, count);
                return;
            }
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
            return;
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
        // set le
        apdu[(short) ((short) (hdr_len + 1) + apdu[hdr_len])] = (byte) (le & 0xff);

        // ISOException.throwIt((short)0x6d68);
        // PassportUtil.returnBuffer(apdu, (short)0,
        // (short)((short)((short)(do87_L & 0xff) + hdr_len) + 1), aapdu);

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

    public void unwrapCommandAPDU(byte[] ssc, APDU apdu) {
        // TODO Auto-generated method stub

    }

    public short wrapResponseAPDU(byte[] ssc, APDU apdu, short len, short sw1sw2) {
        // TODO Auto-generated method stub
        return (short)0;

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
