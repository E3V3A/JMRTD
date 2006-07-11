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

import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.DESKey;
import javacard.security.KeyBuilder;
import javacardx.crypto.Cipher;

/**
 * This class is hack. It actually implements 
 * => encrypt/decrypt of ALG_DES_CBC_NOPAD using ALG_DES_CBC_ISO9797_M2
 * for use in
 * => sign/verify ALG_DES_MAC8_ISO9797_1_M2_ALG3
 * 
 * This is because ALG_DES_CBC_NOPAD and ALG_DES_MAC8_ISO9797_1_M2_ALG3 do not 
 * exist on CREF.
 * 
 * @author Cees-Bart Breunesse <ceesb@cs.ru.nl>
 * @author Ronny Wichers Schreur <ronny@cs.ru.nl>
 * 
 * @version $Revision$
 */
public class CREFPassportCrypto extends PassportCrypto implements ISO7816 {
    private static DESKey ma_kMac_a, ma_kMac_b;
    private static DESKey sm_kMac_a, sm_kMac_b;
    private static DESKey sm_kEnc, ma_kEnc;
    
    public static byte[] PAD_DATA = { (byte) 0x80, 0, 0, 0, 0, 0, 0, 0 };

    private static byte[] tempSpace_verifyMac;

    private static Cipher ciph;
    
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
        tempSpace_createMac = JCSystem.makeTransientByteArray((short) 24,
                                                              JCSystem.CLEAR_ON_RESET);
        tempSpace_decryptDES = JCSystem.makeTransientByteArray((short) 16,
                                                               JCSystem.CLEAR_ON_RESET);
        tempSpace_verifyMac = JCSystem.makeTransientByteArray((short) 8,
                                                              JCSystem.CLEAR_ON_RESET);
}

    public  boolean verifyMac(DESKey kMac_a, DESKey kMac_b, byte[] msg,
            short msg_offset, short msg_len, byte[] mac, short mac_offset) {
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

    private short decryptDESusingDESCBCM2(DESKey key, byte[] in,
            short in_offset, byte[] out, short out_offset, short length) {
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

        ciph.init(key, Cipher.MODE_DECRYPT);
        short written = ciph.update(in, in_offset, length, out, out_offset);
        written += ciph.doFinal(tempSpace_decryptDES,
                         (short) 0,
                         (short) (16),
                         out,
                         (short) (out_offset + written));
        
        return (short)(written - 8); // FIXME: hack, compensate for padding
    }

    private static byte[] tempSpace_createMac;
    private static byte[] tempSpace_decryptDES;
    private static final byte[] ZERO = { 0, 0, 0, 0, 0, 0, 0, 0 };

    // FIXME: remove all references to ciph
    public void createMac(DESKey kMac_a, DESKey kMac_b, byte[] msg,
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

    public void setMacSessionKey(byte[] macKey) {
        sm_kMac_a.setKey(macKey, (short) 0);
        sm_kMac_b.setKey(macKey, (short) 8);        
    }
    
    public void setMacMutualAuthKey(byte[] macKey) {
        ma_kMac_a.setKey(macKey, (short) 0);
        ma_kMac_b.setKey(macKey, (short) 8);        
    }
    
    public void setMutualAuthKeys(byte[] macKey, byte[] encKey) {
        setMacMutualAuthKey(macKey);
        ma_kEnc.setKey(encKey, (short)0);
    }
    
    public void setSessionKeys(byte[] macKey, byte[] encKey) {
        setMacSessionKey(macKey);
        sm_kEnc.setKey(encKey, (short)0);
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

    public short decrypt(byte state, byte[] ctext, short ctext_offset,
            short ctext_len, byte[] ptext, short ptext_offset) {
        DESKey k=null;
        
        if (PassportUtil.hasBitMask(state, PassportApplet.MUTUAL_AUTHENTICATED)) {
            k = sm_kEnc;
        }
        else if(PassportUtil.hasBitMask(state, PassportApplet.CHALLENGED)) {
            k = ma_kEnc;            
        }
        
        return decryptDESusingDESCBCM2(k, ctext, ctext_offset, ptext, ptext_offset, ctext_len);
    }

    public short encrypt(byte state, byte padding,  byte[] ptext, short ptext_offset,
            short ptext_len, byte[] ctext, short ctext_offset) {
        DESKey k=null;
        
        if (PassportUtil.hasBitMask(state, PassportApplet.MUTUAL_AUTHENTICATED)) {
            k = sm_kEnc;
        }
        else if(PassportUtil.hasBitMask(state, PassportApplet.CHALLENGED)) {
            k = ma_kEnc;
        }

        ciph.init(k, Cipher.MODE_ENCRYPT);
        short len = ciph.doFinal(ptext, ptext_offset, ptext_len, ctext, ctext_offset);

        if(padding == PAD_INPUT) {
            // ALG_DES_CBC_ISO9797_M2 does padding
            return len;
        }
        else if (padding == DONT_PAD_INPUT) {
            return (short)(len - 8); // FIXME: hack
        }
        return 0;
    }
}
