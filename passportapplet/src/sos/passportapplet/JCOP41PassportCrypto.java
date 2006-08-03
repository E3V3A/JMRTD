package sos.passportapplet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.DESKey;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

public class JCOP41PassportCrypto extends JCOPPassportCrypto {
    private Cipher macCiphEBC;
    private byte[] tempSpace_verifyMac;

    JCOP41PassportCrypto(KeyStore keyStore) {
        super(keyStore);
                
        macCiphEBC = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
 
        tempSpace_verifyMac = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
    }

//    byte[] DEBUGmsg = { (byte)0x72, (byte)0xC2, (byte)0x9C, (byte)0x23, (byte)0x71, (byte)0xCC, (byte)0x9B, (byte)0xDB, 
//            (byte)0x65, (byte)0xB7, (byte)0x79, (byte)0xB8, (byte)0xE8, (byte)0xD3, (byte)0x7B, (byte)0x29, 
//            (byte)0xEC, (byte)0xC1, (byte)0x54, (byte)0xAA, (byte)0x56, (byte)0xA8, (byte)0x79, (byte)0x9F, 
//            (byte)0xAE, (byte)0x2F, (byte)0x49, (byte)0x8F, (byte)0x76, (byte)0xED, (byte)0x92, (byte)0xF2};
//
//    byte[] DEBUGkeyA = { (byte)0x79, (byte)0x62, (byte)0xD9, (byte)0xEC, (byte)0xE0, (byte)0x3D, (byte)0x1A, (byte)0xCD};
//    byte[] DEBUGkeyB = { (byte)0x4C, (byte)0x76, (byte)0x08, (byte)0x9D, (byte)0xCE, (byte)0x13, (byte)0x15, (byte)0x43};  
    
    protected void makeSignatureInstance() {
        sig = Signature.getInstance(Signature.ALG_DES_MAC8_ISO9797_M2,
                                    false);
    }
 
    public void initMac() {
        DESKey kA = keyStore.getMacKey(KeyStore.KEY_A);
        
        sig.init(kA, Signature.MODE_SIGN);    
    }
    
    public void updateMac(byte[] msg, short msg_offset, short msg_len) {
        sig.update(msg, msg_offset, msg_len);
    }
    
    public void createMacFinal(byte[] msg, short msg_offset, short msg_len,
            byte[] mac, short mac_offset) {
        DESKey kA = keyStore.getMacKey(KeyStore.KEY_A);
        DESKey kB = keyStore.getMacKey(KeyStore.KEY_B);

        updateMac(msg, msg_offset, msg_len);
        sig.sign(null, (short)0, (short)0, mac, mac_offset);
        
        macCiphEBC.init(kB, Cipher.MODE_DECRYPT);
        macCiphEBC.doFinal(mac, mac_offset, (short)8, mac, mac_offset);
        
        macCiphEBC.init(kA, Cipher.MODE_ENCRYPT);
        macCiphEBC.doFinal(mac, mac_offset, (short)8, mac, mac_offset);
    }

    
    public boolean verifyMacFinal(byte[] msg, short msg_offset, short msg_len,
            byte[] mac, short mac_offset) {
      
        createMacFinal(msg, msg_offset, msg_len, tempSpace_verifyMac, (short)0);
               
        if(Util.arrayCompare(mac, mac_offset, tempSpace_verifyMac, (short)0, (short)8) == 0) {
            return true;
        }
        return false;
    }
}
