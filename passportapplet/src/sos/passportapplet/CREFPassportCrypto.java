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
import javacard.security.CryptoException;
import javacard.security.DESKey;
import javacard.security.Key;
import javacardx.crypto.Cipher;

/**
 * This class is a hack. It (probably) implements 
 * => encrypt/decrypt of ALG_DES_CBC_NOPAD using ALG_DES_CBC_ISO9797_M2

 * This is because ALG_DES_CBC_NOPAD and ALG_DES_MAC8_ISO9797_1_M2_ALG3 do not 
 * exist on CREF.
 * 
 * @author Cees-Bart Breunesse <ceesb@cs.ru.nl>
 * @author Ronny Wichers Schreur <ronny@cs.ru.nl>
 * 
 * @version $Revision$
 */
public class CREFPassportCrypto extends JCOP41PassportCrypto implements ISO7816 {
  
    CREFPassportCrypto(KeyStore keyStore) {
        super(keyStore);
        ciph = Cipher.getInstance(Cipher.ALG_DES_CBC_ISO9797_M2, false);

        tempSpace_decryptDES = JCSystem.makeTransientByteArray((short) 16,
                                                               JCSystem.CLEAR_ON_RESET);
    }

    class DESCipher extends Cipher {
        private byte mode; 
        
        public short doFinal(byte[] inBuff, short inOffset, short inLength, byte[] outBuff, short outOffset) throws CryptoException {
            
            // TODO Auto-generated method stub
            return 0;
        }

        public void init(Key theKey, byte theMode, byte[] bArray, short bOff, short bLen) throws CryptoException {
            if(theMode == MODE_ENCRYPT) {
                ciph.init(theKey, theMode, bArray, bOff, bLen);
            }
            else {
                // FIXME: niks?
            }
            mode = theMode;
        }

        public byte getAlgorithm() {
            return ALG_DES_CBC_NOPAD;
        }

        public short update(byte[] inBuff, short inOffset, short inLength, byte[] outBuff, short outOffset) throws CryptoException {
            // TODO Auto-generated method stub
            return 0;
        }

        public void init(Key theKey, byte theMode) throws CryptoException {
            // TODO Auto-generated method stub
            
        }
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

    private static byte[] tempSpace_decryptDES;
    private static final byte[] ZERO = { 0, 0, 0, 0, 0, 0, 0, 0 };

    public short decrypt(byte[] ctext, short ctext_offset, short ctext_len,
            byte[] ptext, short ptext_offset) {
        DESKey k=keyStore.getEncKey();
        
        return decryptDESusingDESCBCM2(k, ctext, ctext_offset, ptext, ptext_offset, ctext_len);
    }

    public short encrypt(byte padding, byte[] ptext,  short ptext_offset, short ptext_len,
            byte[] ctext, short ctext_offset) {
        DESKey k=keyStore.getEncKey();
        
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
