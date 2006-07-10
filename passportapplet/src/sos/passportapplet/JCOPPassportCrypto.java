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
import javacard.security.DESKey;
import javacard.security.Signature;

public class JCOPPassportCrypto extends PassportCrypto {
    private static Signature sig;
    private static DESKey ma_kMac, ma_kEnc, sm_kMac, sm_kEnc;

    JCOPPassportCrypto() {
        super();

        sig = Signature.getInstance(Signature.ALG_DES_MAC8_ISO9797_1_M2_ALG3,
                                    false);
    }

    public void createMac(byte state, byte[] msg, short msg_offset,
            short msg_len, byte[] mac, short mac_offset) {
        DESKey k = null;

        if ((state & PassportApplet.MUTUAL_AUTHENTICATED) == PassportApplet.MUTUAL_AUTHENTICATED) {
            k = sm_kMac;
        } else if ((state & PassportApplet.CHALLENGED) == PassportApplet.CHALLENGED) {
            k = ma_kMac;
        }

        sig.init(k, Signature.MODE_SIGN);
        sig.sign(msg, msg_offset, msg_len, mac, mac_offset);
    }

    public void decrypt(byte state, byte[] ctext, short ctext_offset,
            short ctext_len, byte[] ptext, short ptext_offset) {
        // TODO Auto-generated method stub

    }

    public void encrypt(byte state, byte[] ptext, short ptext_offset,
            short ptext_len, byte[] ctext, short ctext_offset) {
        // TODO Auto-generated method stub

    }

    public void setMutualAuthKeys(byte[] macKey, byte[] encKey) {
        // TODO Auto-generated method stub

    }

    public void setSessionKeys(byte[] macKey, byte[] encKey) {
        // TODO Auto-generated method stub

    }

    public short unwrapCommandAPDU(byte[] ssc, APDU apdu) {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean verifyMac(byte state, byte[] msg, short msg_offset,
            short msg_len, byte[] mac, short mac_offset) {
        DESKey k = null;

        if ((state & PassportApplet.MUTUAL_AUTHENTICATED) == PassportApplet.MUTUAL_AUTHENTICATED) {
            k = sm_kMac;
        } else if ((state & PassportApplet.CHALLENGED) == PassportApplet.CHALLENGED) {
            k = ma_kMac;
        }

        sig.init(k, Signature.MODE_VERIFY);
        return sig.verify(msg, msg_offset, msg_len, mac, mac_offset, (short) 8);
    }

    public short wrapResponseAPDU(byte[] ssc, APDU apdu, short len, short sw1sw2) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void createTempSpace() {
        // TODO Auto-generated method stub

    }

}
