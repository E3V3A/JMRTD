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

import javacard.security.DESKey;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

/**
 * JCOP implementation of Passport crypto
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class JCOPPassportCrypto extends PassportCrypto {
    protected Signature sig;
    private static Cipher ciph;

    protected void makeSignatureInstance() {
        sig = Signature.getInstance(Signature.ALG_DES_MAC8_ISO9797_1_M2_ALG3,
                                    false);
    }
    
    JCOPPassportCrypto(KeyStore keyStore) {
        super(keyStore);

        makeSignatureInstance();
        ciph = Cipher.getInstance(Cipher.ALG_DES_CBC_NOPAD, false);        
    }

    public void createMacFinal(byte[] msg, short msg_offset, short msg_len,
            byte[] mac, short mac_offset) {
        DESKey k = keyStore.getMacKey();

        sig.init(k, Signature.MODE_SIGN);
        sig.sign(msg, msg_offset, msg_len, mac, mac_offset);
    }

    public boolean verifyMacFinal(byte[] msg, short msg_offset, short msg_len,
            byte[] mac, short mac_offset) {
        DESKey k = keyStore.getMacKey();

        sig.init(k, Signature.MODE_VERIFY);
        return sig.verify(msg, msg_offset, msg_len, mac, mac_offset, (short) 8);
    }

    public short decrypt(byte[] ctext, short ctext_offset, short ctext_len,
            byte[] ptext, short ptext_offset) {
        DESKey k = keyStore.getEncKey();
        
        ciph.init(k, Cipher.MODE_DECRYPT);
        return ciph.doFinal(ctext, ctext_offset, ctext_len, ptext, ptext_offset);
    }

    public short encrypt(byte padding, byte[] ptext,  short ptext_offset, short ptext_len,
            byte[] ctext, short ctext_offset) {
        DESKey k = keyStore.getEncKey();

        if(padding == PAD_INPUT) {
            // pad input
            ptext_len = PassportUtil.pad(ptext, ptext_offset, ptext_len);
        }
 
        ciph.init(k, Cipher.MODE_ENCRYPT);
        short len = ciph.doFinal(ptext, ptext_offset, ptext_len, ctext, ctext_offset);
        
        return len;
    }

    public void updateMac(byte[] msg, short msg_offset, short msg_len) {
        // TODO Auto-generated method stub
        
    }

    public void initMac() {
        // TODO Auto-generated method stub
        
    }
}
