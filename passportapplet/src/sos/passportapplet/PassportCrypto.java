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
import javacard.framework.ISOException;
import javacard.framework.Util;
import javacard.security.CryptoException;
import javacard.security.MessageDigest;

public abstract class PassportCrypto {
    public static final byte ENC_MODE = 1;
    public static final byte MAC_MODE = 2;
    
    public static final byte CREF_MODE = 0;
    public static final byte JCOP_MODE = 1;
    
    private static MessageDigest shaDigest;
    
    public PassportCrypto() {
        shaDigest = MessageDigest.getInstance(MessageDigest.ALG_SHA, false); 
    };
    
    public abstract void createTempSpace();

    public abstract boolean verifyMac(byte state, byte[] msg, short msg_offset, short msg_len, byte[] mac, short mac_offset);
    
    public abstract void createMac(byte state, byte[] msg, short msg_offset, short msg_len, byte[] mac, short mac_offset);

    public abstract void encrypt(byte state, byte[] ptext, short ptext_offset, short ptext_len, byte[] ctext, short ctext_offset);
 
    public abstract void decrypt(byte state, byte[] ctext, short ctext_offset, short ctext_len, byte[] ptext, short ptext_offset);
    
    public abstract short unwrapCommandAPDU(byte[] ssc, APDU apdu);

    public abstract short wrapResponseAPDU(byte[] ssc, APDU apdu, short len, short sw1sw2);

    public abstract void setSessionKeys(byte[] macKey, byte[] encKey);
    
    public abstract void setMutualAuthKeys(byte[] macKey, byte[] encKey);
    
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
    
//    public static short maxResponseDataLength(byte state, APDU apdu) {
//        short max=(short)apdu.getBuffer().length;
//        
////         max -= 2;  // status words
//        
//        if((state & PassportApplet.MUTUAL_AUTHENTICATED) == PassportApplet.MUTUAL_AUTHENTICATED) {
//            max -= 4;  // do99
//            max -= 10; // do8e
//            max -= 4;  // do87 header FIXME: (might be 3, but i dont care)
//            max -= 8;  // 8 bytes padding 
//        }
//        
//        return max;
//    }

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

    public static void computeSSC(byte[] rndICC, byte[] rndIFD, byte[] ssc) {
        if (rndICC == null || rndICC.length != 8 || rndIFD == null
                || rndIFD.length != 8) {
            ISOException.throwIt((short) 0x6d08);
        }
        Util.arrayCopy(rndICC, (short) 4, ssc, (short) 0, (short) 4);
        Util.arrayCopy(rndIFD, (short) 4, ssc, (short) 4, (short) 4);
    }

    public static void createHash(byte[] msg, short msg_offset, short length, byte[] dest, short dest_offset) 
    throws CryptoException {
        if((dest.length < dest_offset + length) ||
                (msg.length < msg_offset + length))
            ISOException.throwIt((short)0x6d66);
        
        try { 
            shaDigest.doFinal(msg, msg_offset, length, dest, dest_offset);
        } finally {
            shaDigest.reset();
        }
        
    }
}
