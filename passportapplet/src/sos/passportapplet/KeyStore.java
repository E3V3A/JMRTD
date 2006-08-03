package sos.passportapplet;

import javacard.security.DESKey;
import javacard.security.KeyBuilder;

/**
 * @author ceesb
 *
 */
public class KeyStore {
    public static final byte KEY_A = 0;
    public static final byte KEY_B = 1;
    
    private DESKey sm_kMac_a, sm_kMac_b, sm_kMac;
    private DESKey ma_kMac_a, ma_kMac_b, ma_kMac;
    private DESKey ma_kEnc, sm_kEnc;
    private byte mode;
    
    KeyStore(byte mode) {
        this.mode = mode;
        sm_kEnc = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                               KeyBuilder.LENGTH_DES3_2KEY,
                                               false);
        ma_kEnc = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                               KeyBuilder.LENGTH_DES3_2KEY,
                                               false);
        
        switch(mode) {
        case PassportCrypto.PERFECTWORLD_MODE: 
            sm_kMac = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                                   KeyBuilder.LENGTH_DES3_2KEY,
                                                   false);
            ma_kMac = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
                                                   KeyBuilder.LENGTH_DES3_2KEY,
                                                   false);
            break;
        case PassportCrypto.CREF_MODE:
        case PassportCrypto.JCOP41_MODE:
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
            break;
        }
    }
    

    public DESKey getMacKey() {
        if(PassportUtil.hasBitMask(PassportApplet.state, PassportApplet.MUTUAL_AUTHENTICATED)) {
            return sm_kMac;
        }
        else {
            return ma_kMac;
        }
    }
    
    public DESKey getMacKey(byte aOrb) {
        if(PassportUtil.hasBitMask(PassportApplet.state, PassportApplet.MUTUAL_AUTHENTICATED)) {
            if(aOrb == KEY_A) {
                return sm_kMac_a;
            }
            else {
                return sm_kMac_b;
            }
        }
        else {
            if(aOrb == KEY_A) {
                return ma_kMac_a;
            }
            else {
                return ma_kMac_b;
            }
        }      
    }
    
    public DESKey getEncKey() {
        if(PassportUtil.hasBitMask(PassportApplet.state, PassportApplet.MUTUAL_AUTHENTICATED)) {
            return sm_kEnc;
        }
        else {
            return ma_kEnc;
        }
    }
    
    public void setMutualAuthenticationKeys(byte[] kMac, short kMac_offset, byte[] kEnc, short kEnc_offset) {
        ma_kEnc.setKey(kEnc, kEnc_offset);
        switch(mode) {
        case PassportCrypto.PERFECTWORLD_MODE:
            ma_kMac.setKey(kMac, kMac_offset);
            break;
        case PassportCrypto.CREF_MODE:
        case PassportCrypto.JCOP41_MODE:
            ma_kMac_a.setKey(kMac, kMac_offset);
            ma_kMac_b.setKey(kMac, (short)(kMac_offset + 8));
            break;
        }
    }

    public void setSecureMessagingKeys(byte[] kMac, short kMac_offset, byte[] kEnc, short kEnc_offset) {
        sm_kEnc.setKey(kEnc, kEnc_offset);
        switch(mode) {
        case PassportCrypto.PERFECTWORLD_MODE:
            sm_kMac.setKey(kMac, kMac_offset);
            break;
        case PassportCrypto.CREF_MODE:
        case PassportCrypto.JCOP41_MODE:
            sm_kMac_a.setKey(kMac, kMac_offset);
            sm_kMac_b.setKey(kMac, (short)(kMac_offset + 8));
            break;
        }
    }
}

