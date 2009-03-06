package sos.passportapplet;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.KeyBuilder;
import javacard.security.RSAPublicKey;
import javacard.security.Signature;

/**
 * Encapsulation class for a card verifiable certificates according to EAC 1.11.
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class CVCertificate {

    private static final byte ROLE_CVCA = (byte)0xC0;
    private static final byte ROLE_DV_DOMESTIC = (byte)0x80;
    private static final byte ROLE_DV_FOREIGN = (byte)0x40;
    private static final byte ROLE_IS = (byte)0x00;
    
    /**
     * Offsets to where the particular data (offsets & lengths) of the current
     * certificate is (temporarily) stored in the data array
     */
    static final short OFFSET_PUB_KEY_MODULUS_OFFSET = 0;
    static final short OFFSET_PUB_KEY_MODULUS_LENGTH = 1;
    static final short OFFSET_PUB_KEY_EXPONENT_OFFSET = 2;
    static final short OFFSET_PUB_KEY_EXPONENT_LENGTH = 3;
    static final short OFFSET_SUB_ID_OFFSET = 4;
    static final short OFFSET_SUB_ID_LENGTH = 5;
    static final short OFFSET_AUTHORIZATION_OFFSET = 6;
    static final short OFFSET_EFF_DATE_OFFSET = 7;
    static final short OFFSET_EXP_DATE_OFFSET = 8;
    static final short OFFSET_SIGNATURE_OFFSET = 9;
    static final short OFFSET_SIGNATURE_LENGTH = 10;
    static final short OFFSET_BODY_LENGTH = 11;

    /** Different tags to parse */
    private static final short TAG_CERT_BODY = 0x7F4E;
    private static final short TAG_CERT_VERSION = 0x5F29;
    private static final short TAG_AUTH_ID = 0x42;
    private static final short TAG_PUB_KEY = 0x7F49;
    private static final short TAG_OID = 0x06;
    private static final short TAG_MODULUS = 0x81;
    private static final short TAG_EXPONENT = 0x82;
    private static final short TAG_SUBJECT_ID = 0x5F20;
    private static final short TAG_SUBJECT_AUTH = 0x7F4C;
    private static final short TAG_AUTHORIZATION = 0x53;
    private static final short TAG_EFF_DATE = 0x5F25;
    private static final short TAG_EXP_DATE = 0x5F24;
    private static final short TAG_SIGNATURE = 0x5F37;

    /** The ASN1 OID of the only algorithm our certificates support */
    private static final byte[] RSA_SHA1_OID = { 0x04, 0x00,
            0x7F, 0x00, 0x07, 0x02, 0x02, 0x02, 0x01, 0x02 };

    /** The EAC OID, see EAC 1.11, D.2.1.3 */
    private static final byte[] EAC_OID = { 0x04, 0x00,
            0x7F, 0x00, 0x07, 0x03, 0x01, 0x02, 0x01 };

    short[] data;
    Object[] source;

    RSAPublicKey currentCertPublicKey; // init
    byte[] currentCertSubjectId; // init
    byte[] effectiveCertAuthorization; // init
    byte[] currentCertEffDate; // init
    byte[] currentCertExpDate; // init
    byte[] accessFlag;

    byte[] currentDate = { 0x00, 0x09, 0x00, 0x01, 0x00, 0x01 }; // 2009-01-01

    Signature signature;

    byte[] currentCertNum;
    byte[] cert1HolderReference;
    byte[] cert1PublicKeyData;
    byte cert1Authorization;
    byte[] cert1EffDate;
    byte[] cert1ExpDate;

    byte[] cert2HolderReference;
    byte[] cert2PublicKeyData;
    byte cert2Authorization;
    byte[] cert2EffDate;
    byte[] cert2ExpDate;
    
    byte[] cvcaFileReference;

    CVCertificate() {
        data = JCSystem.makeTransientShortArray(
                (short) (OFFSET_BODY_LENGTH + 1), JCSystem.CLEAR_ON_DESELECT);
        effectiveCertAuthorization = JCSystem.makeTransientByteArray((short)1,
                JCSystem.CLEAR_ON_DESELECT);
        currentCertNum = JCSystem.makeTransientByteArray((short)1,
                JCSystem.CLEAR_ON_DESELECT);
        accessFlag = JCSystem.makeTransientByteArray((short)1,
                JCSystem.CLEAR_ON_DESELECT);
        currentCertEffDate = JCSystem.makeTransientByteArray((short)6,
                JCSystem.CLEAR_ON_DESELECT);
        currentCertExpDate = JCSystem.makeTransientByteArray((short)6,
                JCSystem.CLEAR_ON_DESELECT);
        source = JCSystem.makeTransientObjectArray((short) 1,
                JCSystem.CLEAR_ON_DESELECT);
        currentCertSubjectId = JCSystem.makeTransientByteArray((short) 17,
                JCSystem.CLEAR_ON_DESELECT);
        currentCertPublicKey = (RSAPublicKey) KeyBuilder.buildKey(
                KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
        signature = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
    }

    /**
     * Checks if the provided subject id matches the current one, if any, or selects one of the root ones.
     * 
     * @param data
     *            byte[] with the subject id
     * @param offset
     *            offset to data
     * @param length
     *            length of the data
     * @return true if subjects match
     */
    boolean selectSubjectId(byte[] data, short offset, short length) {
        if(currentCertSubjectId[0] == 0) {
            if(cert1HolderReference != null && cert1HolderReference.length == length) {
                if(Util.arrayCompare(cert1HolderReference, (short)0, data, offset, length) == 0) {
                    setupCurrentKey(cert1HolderReference, cert1PublicKeyData, cert1Authorization, cert1EffDate, cert1ExpDate);
                    currentCertNum[0] = 1;
                    return true;
                }
            }else if(cert2HolderReference != null && cert2HolderReference.length == length) {
                setupCurrentKey(cert2HolderReference, cert2PublicKeyData, cert2Authorization, cert2EffDate, cert2ExpDate);
                currentCertNum[0] = 2;
                return true;
            }
            return false;
        }
        return length == currentCertSubjectId[0] && Util.arrayCompare(currentCertSubjectId, (short) 1, data, offset,
                length) == 0;
    }
    
    private void setupCurrentKey(byte[] certHolderReference, byte[] certPublicKeyData, byte certAuthorization, byte[] certEffDate, byte[] certExpDate) {
        Util.arrayCopyNonAtomic(certHolderReference, (short)0, currentCertSubjectId, (short)0, (short)certHolderReference.length);
        currentCertPublicKey.setExponent(certPublicKeyData, (short)0, (short)3);
        currentCertPublicKey.setModulus(certPublicKeyData, (short)3, (short)128);
        effectiveCertAuthorization[0] = certAuthorization;
        Util.arrayCopyNonAtomic(certEffDate, (short)0, currentCertEffDate, (short)0, (short)6);
        Util.arrayCopyNonAtomic(certExpDate, (short)0, currentCertExpDate, (short)0, (short)6);
    }

    private void setupCurrentKeyFromCurrentCertificate() {
        byte[] certData = (byte[])source[0];
        short certHolderReferenceOffset = data[OFFSET_SUB_ID_OFFSET];
        short certHolderReferenceLength = data[OFFSET_SUB_ID_LENGTH];
        short pubKeyExpOffset = data[OFFSET_PUB_KEY_EXPONENT_OFFSET];
        short pubKeyExpLength = data[OFFSET_PUB_KEY_EXPONENT_LENGTH];
        short pubKeyModOffset = data[OFFSET_PUB_KEY_MODULUS_OFFSET];
        short pubKeyModLength = data[OFFSET_PUB_KEY_MODULUS_LENGTH];
        short authorizationOffset = data[OFFSET_AUTHORIZATION_OFFSET];
        short effDateOffset = data[OFFSET_EFF_DATE_OFFSET];
        short expDateOffset = data[OFFSET_EXP_DATE_OFFSET];
        Util.arrayCopyNonAtomic(certData, certHolderReferenceOffset, currentCertSubjectId, (short)1, certHolderReferenceLength);
        currentCertSubjectId[0] = (byte)certHolderReferenceLength;
        currentCertPublicKey.setExponent(certData, pubKeyExpOffset, pubKeyExpLength);
        currentCertPublicKey.setModulus(certData, pubKeyModOffset, pubKeyModLength);
        effectiveCertAuthorization[0] &= certData[authorizationOffset];
        Util.arrayCopyNonAtomic(certData, effDateOffset, currentCertEffDate, (short)0, (short)6);
        Util.arrayCopyNonAtomic(certData, expDateOffset, currentCertExpDate, (short)0, (short)6);
    }
    /**
     * Cleans up the current certificate information.
     * 
     */
    void clear() {
        for (short i = 0; i < data.length; i++) {
            data[i] = 0;
        }
        Util.arrayFillNonAtomic(effectiveCertAuthorization, (short) 0, (short) 1,
                (byte) 0);
        Util.arrayFillNonAtomic(currentCertSubjectId, (short) 0, (short) 17,
                (byte) 0);
        Util.arrayFillNonAtomic(currentCertEffDate, (short) 0, (short) 6,
                (byte) 0);
        Util.arrayFillNonAtomic(currentCertExpDate, (short) 0, (short) 6,
                (byte) 0);
        Util.arrayFillNonAtomic(currentCertNum, (short) 0, (short) 1,
                (byte) 0);
        Util.arrayFillNonAtomic(accessFlag, (short) 0, (short) 1,
                (byte) 0);
        currentCertPublicKey.clearKey();
        source[0] = null;
    }


    /**
     * Verify the current certificate (ie. the data in source) using the current
     * state of certificate verification data (publicKey, subject id, etc.) The
     * verification procedure is described in ISO18013-3 Section C.4.4.2.
     * 
     * @return true if certificate verification succeeds
     */
    boolean verify() {

        byte[] certData = (byte[])source[0];
        short bodyLength =data[OFFSET_BODY_LENGTH]; 
        short sigOffset =data[OFFSET_SIGNATURE_OFFSET]; 
        short sigLength =data[OFFSET_SIGNATURE_LENGTH]; 
        
        // check the actual signature
        signature.init(currentCertPublicKey, Signature.MODE_VERIFY);
        signature.update(certData, (short) 0,
                bodyLength);
        boolean result = signature.verify(certData, bodyLength,
                (short) 0, certData, sigOffset, sigLength);

        // check dates
        result = (compareDate((byte[]) source[0], data[OFFSET_EXP_DATE_OFFSET],
                currentDate, (short) 0) > 0)
                && result;

        // If succeeded make the just verified certificate the current one,
        // otherwise reset.
        short subjectIdOffset = data[OFFSET_SUB_ID_OFFSET];
        short subjectIdLength = data[OFFSET_SUB_ID_LENGTH];
        if((cert1HolderReference != null && (byte)subjectIdLength == cert1HolderReference[0] && 
                Util.arrayCompare(cert1HolderReference, (short)1, certData, subjectIdOffset, subjectIdLength) == 0)
                ||
                (cert2HolderReference != null && (byte)subjectIdLength == cert2HolderReference[0] && 
                        Util.arrayCompare(cert2HolderReference, (short)1, certData, subjectIdOffset, subjectIdLength) == 0)){
                    result = false;
                }
        if (result) {
            setupCurrentKeyFromCurrentCertificate();
            // check if permanent import, update CVCA file, dates, keys, etc.
            // also clear current selection
            
        } else {
            clear();
        }
        return result;
    }

    /**
     * Sets the root certificate data
     * 
     * @param num certificate number, 1 or 2.
     */
    void setRootCertificate(short num) {
        if((num == 1 && cert1HolderReference != null) || (num == 2 && cert2HolderReference != null) || (num != 1 && num != 2)) {
            return;
        }
        byte[] certData = (byte[])source[0];
        short certHolderReferenceOffset = data[OFFSET_SUB_ID_OFFSET];
        short certHolderReferenceLength = data[OFFSET_SUB_ID_LENGTH];
        short pubKeyExpOffset = data[OFFSET_PUB_KEY_EXPONENT_OFFSET];
        short pubKeyExpLength = data[OFFSET_PUB_KEY_EXPONENT_LENGTH];
        short pubKeyModOffset = data[OFFSET_PUB_KEY_MODULUS_OFFSET];
        short pubKeyModLength = data[OFFSET_PUB_KEY_MODULUS_LENGTH];
        short authorizationOffset = data[OFFSET_AUTHORIZATION_OFFSET];
        short effDateOffset = data[OFFSET_EFF_DATE_OFFSET];
        short expDateOffset = data[OFFSET_EXP_DATE_OFFSET];
        byte[] holderReference = new byte[17];
        Util.arrayCopyNonAtomic(certData, certHolderReferenceOffset, holderReference, (short)1, certHolderReferenceLength);
        holderReference[0] = (byte)certHolderReferenceLength;
        byte[] certPubKeyData = new byte[(short)(pubKeyExpLength + pubKeyModLength)];
        Util.arrayCopyNonAtomic(certData, pubKeyExpOffset, certPubKeyData, (short)0, pubKeyExpLength);
        Util.arrayCopyNonAtomic(certData, pubKeyModOffset, certPubKeyData, pubKeyExpLength, pubKeyModLength);
        byte certAuthorization = certData[authorizationOffset];
        byte[] certEffDate = new byte[6];
        Util.arrayCopyNonAtomic(certData, effDateOffset, certEffDate, (short)0, (short)6);
        byte[] certExpDate = new byte[6];
        Util.arrayCopyNonAtomic(certData, expDateOffset, certExpDate, (short)0, (short)6);
        if(num == 1) {
            cert1HolderReference = holderReference;
            cert1PublicKeyData = certPubKeyData;
            cert1Authorization = certAuthorization;
            cert1EffDate = certEffDate;
            cert1ExpDate = certExpDate;
        }else {
            cert2HolderReference = holderReference;
            cert2PublicKeyData = certPubKeyData;
            cert2Authorization = certAuthorization;
            cert2EffDate = certEffDate;
            cert2ExpDate = certExpDate;
        }
    }

    /**
     * Parse the current certificate. The data in source/in is analyzed and
     * offsets and lengths of particular elements of the certificate are stored
     * in the data array. For the root certificate (root == true) we do not
     * parse the signature (we have chosen not to provide it). The format of the
     * certificate is described in ISO18013-3, Section C.2.
     * 
     * @param in
     *            the array with the certificate to be parsed
     * @param offset
     *            offset to in
     * @param length
     *            length of the data
     * @param root
     *            whether we are parsing a root certificate (no signature)
     */
    void parseCertificate(byte[] in, short offset, short length, boolean root) {

        try {
            offset = BERTLVScanner.readTag(in, offset);
            if (BERTLVScanner.tag != TAG_CERT_BODY) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.readLength(in, offset);

            offset = BERTLVScanner.readTag(in, offset);
            offset = BERTLVScanner.readLength(in, offset);
            if (BERTLVScanner.tag != TAG_CERT_VERSION
                    || BERTLVScanner.valueLength != (short) 1
                    || in[offset] != (byte) 0x00) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.skipValue();

            offset = BERTLVScanner.readTag(in, offset);
            if (BERTLVScanner.tag != TAG_AUTH_ID) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            BERTLVScanner.readLength(in, offset);
            offset = BERTLVScanner.skipValue();

            offset = BERTLVScanner.readTag(in, offset);
            if (BERTLVScanner.tag != TAG_PUB_KEY) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.readLength(in, offset);

            offset = BERTLVScanner.readTag(in, offset);
            offset = BERTLVScanner.readLength(in, offset);
            if (BERTLVScanner.tag != TAG_OID
                    || BERTLVScanner.valueLength != (short) RSA_SHA1_OID.length
                    || Util.arrayCompare(in, offset, RSA_SHA1_OID, (short) 0,
                            (short) RSA_SHA1_OID.length) != 0) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.skipValue();

            offset = BERTLVScanner.readTag(in, offset);
            if (BERTLVScanner.tag != TAG_MODULUS) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            data[OFFSET_PUB_KEY_MODULUS_OFFSET] = BERTLVScanner.readLength(in,
                    offset);
            data[OFFSET_PUB_KEY_MODULUS_LENGTH] = BERTLVScanner.valueLength;
            offset = BERTLVScanner.skipValue();
            if (in[data[OFFSET_PUB_KEY_MODULUS_OFFSET]] == (byte) 0x00) {
                data[OFFSET_PUB_KEY_MODULUS_OFFSET]++;
                data[OFFSET_PUB_KEY_MODULUS_LENGTH]--;
            }

            offset = BERTLVScanner.readTag(in, offset);
            if (BERTLVScanner.tag != TAG_EXPONENT) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            data[OFFSET_PUB_KEY_EXPONENT_OFFSET] = BERTLVScanner.readLength(in,
                    offset);
            data[OFFSET_PUB_KEY_EXPONENT_LENGTH] = BERTLVScanner.valueLength;
            offset = BERTLVScanner.skipValue();
            if (in[data[OFFSET_PUB_KEY_EXPONENT_OFFSET]] == (byte) 0x00) {
                data[OFFSET_PUB_KEY_EXPONENT_OFFSET]++;
                data[OFFSET_PUB_KEY_EXPONENT_LENGTH]--;
            }

            offset = BERTLVScanner.readTag(in, offset);
            if (BERTLVScanner.tag != TAG_SUBJECT_ID) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            data[OFFSET_SUB_ID_OFFSET] = BERTLVScanner.readLength(in, offset);
            data[OFFSET_SUB_ID_LENGTH] = BERTLVScanner.valueLength;
            offset = BERTLVScanner.skipValue();

            offset = BERTLVScanner.readTag(in, offset);
            offset = BERTLVScanner.readLength(in, offset);
            if (BERTLVScanner.tag != TAG_SUBJECT_AUTH
                    || BERTLVScanner.valueLength != (short) 15) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.readTag(in, offset);
            offset = BERTLVScanner.readLength(in, offset);
            if (BERTLVScanner.tag != TAG_OID
                    || BERTLVScanner.valueLength != (short)EAC_OID.length
                    || Util.arrayCompare(in, offset, EAC_OID,
                            (short) 0, (short)EAC_OID.length) != 0) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.skipValue();

            offset = BERTLVScanner.readTag(in, offset);
            data[OFFSET_AUTHORIZATION_OFFSET] = BERTLVScanner.readLength(in,
                    offset);
            if (BERTLVScanner.tag != TAG_AUTHORIZATION
                    || BERTLVScanner.valueLength != (short) 4) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.skipValue();

            offset = BERTLVScanner.readTag(in, offset);
            data[OFFSET_EFF_DATE_OFFSET] = BERTLVScanner.readLength(in, offset);
            if (BERTLVScanner.tag != TAG_EFF_DATE
                    || BERTLVScanner.valueLength != (short) 6) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.skipValue();

            offset = BERTLVScanner.readTag(in, offset);
            data[OFFSET_EXP_DATE_OFFSET] = BERTLVScanner.readLength(in, offset);
            if (BERTLVScanner.tag != TAG_EXP_DATE
                    || BERTLVScanner.valueLength != (short) 6) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            offset = BERTLVScanner.skipValue();
            data[OFFSET_BODY_LENGTH] = offset;
            if (!root) {
                offset = BERTLVScanner.readTag(in, offset);
                if (BERTLVScanner.tag != TAG_SIGNATURE) {
                    ISOException.throwIt(ISO7816.SW_WRONG_DATA);
                }
                data[OFFSET_SIGNATURE_OFFSET] = BERTLVScanner.readLength(in,
                        offset);
                data[OFFSET_SIGNATURE_LENGTH] = BERTLVScanner.valueLength;
            }
            source[0] = in;
        } catch (Exception e) {
            clear();
            ISOException.throwIt((short) (ISO7816.SW_WRONG_DATA));
        }

    }

    /**
     * Compares two dates.
     * 
     * @param date1
     *            the first date
     * @param offset1
     *            offset to the first date
     * @param date2
     *            the second date
     * @param offset2
     *            offset to the second date
     * @return -1 if the first date is before the second, 1 if it is after, 0 if
     *         the same
     */
    private byte compareDate(byte[] date1, short offset1, byte[] date2,
            short offset2) {
        short year1 = (short) ((short) (date1[offset1] * 10) + date1[(short) (offset1 + 1)]);
        short year2 = (short) ((short) (date2[offset2] * 10) + date2[(short) (offset2 + 1)]);
        short month1 = (short) ((short) (date1[(short) (offset1 + 2)] * 10) + date1[(short) (offset1 + 3)]);
        short month2 = (short) ((short) (date2[(short) (offset2 + 2)] * 10) + date2[(short) (offset2 + 3)]);
        short day1 = (short) ((short) (date1[(short) (offset1 + 4)] * 10) + date1[(short) (offset1 + 5)]);
        short day2 = (short) ((short) (date2[(short) (offset2 + 4)] * 10) + date2[(short) (offset2 + 5)]);
        if (year1 < year2) {
            return -1;
        } else if (year1 > year2) {
            return 1;
        }
        if (month1 < month2) {
            return -1;
        } else if (month1 > month2) {
            return 1;
        }
        if (day1 < day2) {
            return -1;
        } else if (day1 > day2) {
            return 1;
        }
        return 0;
    }
}
