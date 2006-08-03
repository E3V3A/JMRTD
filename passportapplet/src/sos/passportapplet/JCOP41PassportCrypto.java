package sos.passportapplet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.DESKey;
import javacard.security.KeyBuilder;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

public class JCOP41PassportCrypto extends JCOPPassportCrypto {
    private DESKey ma_kMac_a, sm_kMac_a;
    private Cipher macCiphEBC;
    private DESKey sm_kMac_b;
    private DESKey ma_kMac_b;
    private byte[] tempSpace_verifyMac;
    byte[] DEBUGmsg = { (byte)0x72, (byte)0xC2, (byte)0x9C, (byte)0x23, (byte)0x71, (byte)0xCC, (byte)0x9B, (byte)0xDB, 
            (byte)0x65, (byte)0xB7, (byte)0x79, (byte)0xB8, (byte)0xE8, (byte)0xD3, (byte)0x7B, (byte)0x29, 
            (byte)0xEC, (byte)0xC1, (byte)0x54, (byte)0xAA, (byte)0x56, (byte)0xA8, (byte)0x79, (byte)0x9F, 
            (byte)0xAE, (byte)0x2F, (byte)0x49, (byte)0x8F, (byte)0x76, (byte)0xED, (byte)0x92, (byte)0xF2};

    byte[] DEBUGkeyA = { (byte)0x79, (byte)0x62, (byte)0xD9, (byte)0xEC, (byte)0xE0, (byte)0x3D, (byte)0x1A, (byte)0xCD};
    byte[] DEBUGkeyB = { (byte)0x4C, (byte)0x76, (byte)0x08, (byte)0x9D, (byte)0xCE, (byte)0x13, (byte)0x15, (byte)0x43};

    
    protected void makeSignatureInstance() {
        sig = Signature.getInstance(Signature.ALG_DES_MAC8_ISO9797_M2,
                                    false);
        
        macCiphEBC = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
 
        tempSpace_verifyMac = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
        
        
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

    }
    
    public void createMac(byte state, byte[] msg, short msg_offset,
            short msg_len, byte[] mac, short mac_offset) {
        DESKey kA=null, kB = null;

        if ((state & PassportApplet.MUTUAL_AUTHENTICATED) == PassportApplet.MUTUAL_AUTHENTICATED) {
            kA = sm_kMac_a;
            kB = sm_kMac_b;
        } else if ((state & PassportApplet.CHALLENGED) == PassportApplet.CHALLENGED) {
            kA = ma_kMac_a;
            kB = ma_kMac_b;
        }

        sig.init(kA, Signature.MODE_SIGN);
        sig.sign(msg, msg_offset, msg_len, mac, mac_offset);
//        sig.sign(DEBUGmsg, (short)0, (short)DEBUGmsg.length, mac, mac_offset);
        
        macCiphEBC.init(kB, Cipher.MODE_DECRYPT);
        macCiphEBC.doFinal(mac, mac_offset, (short)8, mac, mac_offset);
        
        macCiphEBC.init(kA, Cipher.MODE_ENCRYPT);
        macCiphEBC.doFinal(mac, mac_offset, (short)8, mac, mac_offset);
    }

    public boolean verifyMac(byte state, byte[] msg, short msg_offset,
            short msg_len, byte[] mac, short mac_offset) {
      
        createMac(state, msg, msg_offset, msg_len, tempSpace_verifyMac, (short)0);
               
        if(Util.arrayCompare(mac, mac_offset, tempSpace_verifyMac, (short)0, (short)8) == 0) {
            return true;
        }
        return false;
    }
    
    public void setMutualAuthKeys(byte[] keys, short macKey_p, short encKey_p) {
//        ma_kMac_a.setKey(DEBUGkeyA, (short)0);
//        ma_kMac_b.setKey(DEBUGkeyB, (short)0);        
        ma_kMac_a.setKey(keys, macKey_p);
        ma_kMac_b.setKey(keys, (short)(macKey_p + 8));
        ma_kEnc.setKey(keys, encKey_p);
    }

    public void setSessionKeys(byte[] keys, short macKey_p, short encKey_p) {
        sm_kMac_a.setKey(keys, macKey_p);
        sm_kMac_b.setKey(keys, (short)(macKey_p + 8));
        sm_kEnc.setKey(keys, encKey_p);
    }

}
