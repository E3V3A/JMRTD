/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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

package sos.mrtd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.RSAKeyGenParameterSpec;

import sos.smartcards.CommandAPDU;
import sos.smartcards.BERTLVObject;
import sos.smartcards.CardService;
import sos.smartcards.ISO7816;
import sos.smartcards.ResponseAPDU;
import sos.util.ASN1Utils;

/**
 * Service for initializing blank passport reference applets.
 * 
 * Following MO's design, this class must be split in two: - PassportInitService -
 * PassportInitApduService
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 */
public class PassportInitService extends PassportApduService {
    public static final byte INS_SET_DOCNR_DOB_DOE = (byte) 0x10;
    public static final short AAPRIVKEY_FID = 0x0001;
    private static final byte INS_PUT_DATA = (byte) 0xda;;
    
    private static final byte PRIVMODULUS_TAG = 0x60;
    private static final byte PRIVEXPONENT_TAG = 0x61;
    private static final byte MRZ_TAG = 0x62;

    public PassportInitService(CardService service)
            throws GeneralSecurityException {
        super(service);
    }

    /**
     * Generates an RSA keypair fit for Active Authentication.
     * 
     * @return a KeyPair
     * @throws GeneralSecurityException
     * @throws NoSuchAlgorithmException when BouncyCastle provider cannot be found. 
     */
    public static KeyPair generateAAKeyPair() 
    throws GeneralSecurityException, NoSuchAlgorithmException {
        String preferredProvider = "BC";
        Provider provider = Security.getProvider(preferredProvider);
        if (provider == null) {
            return null;
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
                                                                  provider);
        generator.initialize(new RSAKeyGenParameterSpec(1024,
                                                        RSAKeyGenParameterSpec.F4));
        KeyPair keyPair = generator.generateKeyPair();
        return keyPair;
    }

    /***
     * Creates a PUT_DATA APDU.
     * 
     * @param p1 byte of APDU
     * @param p2 byte of APDU
     * @param data bytes of APDU
     * @return a PUT_DATA apdu.
     */
    public CommandAPDU createPutDataApdu(byte p1, byte p2, byte[] data) {
        byte cla = 0;
        byte ins = INS_PUT_DATA;

        return new CommandAPDU(cla, ins, p1, p2, data);
    }

    /***
     * Sends a PUT_DATA APDU to the card.
     * 
     * @param wrapper for secure mesaging
     * @param p1 byte of APDU
     * @param p2 byte of APDU
     * @param data of APDU
     */
    public byte[] putData(SecureMessagingWrapper wrapper, byte p1, byte p2,
            byte[] data) {
        CommandAPDU capdu = createPutDataApdu(p1, p2, data);

        if (wrapper != null) {
            capdu = wrapper.wrap(capdu);
        }
        ResponseAPDU rapdu = transmit(capdu);
        if (wrapper != null) {
            rapdu = wrapper.unwrap(rapdu, rapdu.getBuffer().length);
        }
        return rapdu.getData();
    }

    /***
     * Sends a PUT_DATA command to the card to set the private keys used for Active Authentication.
     * 
     * @param wrapper for secure mesaging.
     * @param key holding the private key data.
     * @throws IOException on error.
     */
    public void putPrivateKey(SecureMessagingWrapper wrapper, PrivateKey key)
            throws IOException {
        byte[] encodedPriv = key.getEncoded();
        BERTLVObject encodedPrivObject = BERTLVObject.getInstance(new ByteArrayInputStream(encodedPriv));
        byte[] privKeyData = (byte[])encodedPrivObject.getChildByIndex(2)
                                              .getValue();
        BERTLVObject privKeyDataObject = BERTLVObject.getInstance(new ByteArrayInputStream(privKeyData));
        byte[] privModulus = (byte[])privKeyDataObject.getChildByIndex(1)
                                              .getValue();
        byte[] privExponent = (byte[])privKeyDataObject.getChildByIndex(3)
                                               .getValue();
        
        putPrivateKey(wrapper, privModulus, privExponent);
    }

    private void putPrivateKey(SecureMessagingWrapper wrapper,
            byte[] privModulus, byte[] privExponent) throws IOException {

        BERTLVObject privModulusObject = 
                new BERTLVObject(PRIVMODULUS_TAG,
                                 new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG,
                                                  privModulus));
        
        putData(wrapper, (byte) 0, PRIVMODULUS_TAG, privModulusObject.getEncoded());

        BERTLVObject privExponentObject = 
            new BERTLVObject(PRIVEXPONENT_TAG,
                             new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG,
                                              privExponent));
        
        putData(wrapper, (byte) 0, PRIVEXPONENT_TAG, privExponentObject.getEncoded());
    }

    /***
     * Sends a CREATE_FILE APDU to the card.
     * 
     * @param wrapper for secure messaging.
     * @param fid (file identifier) of the new file.
     * @param length of the new file.
     */
    public void createFile(SecureMessagingWrapper wrapper, short fid, short length) {
        sendCreateFile(wrapper, fid, length);
    }

    /***
     * Creates a CREATE_FILE APDU.
     * 
     * @param fid (file indentifier) of the new file,
     * @param length of the new file.
     * @return a CREATE_FILE APDU.
     */
    public CommandAPDU createCreateFileAPDU(short fid, short length) {
        byte p1 = (byte) 0x00;
        byte p2 = (byte) 0x00;
        int le = 0;
        byte[] data = { 0x63, 4, (byte) ((length >>> 8) & 0xff),
                (byte) (length & 0xff), (byte) ((fid >>> 8) & 0xff),
                (byte) (fid & 0xff) };
        CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816,
                             ISO7816.INS_CREATE_FILE,
                             p1,
                             p2,
                             data,
                             le);
        return apdu;
    }

    
    /***
     * Sends a CREATE_FILE APDU to the card.
     * 
     * @param wrapper for secure messaging.
     * @param fid of the new file
     * @param length of the new file
     * @return a return APDU with the response from the card.
     */
    public byte[] sendCreateFile(SecureMessagingWrapper wrapper, short fid,
            short length) {
        CommandAPDU capdu = createCreateFileAPDU(fid, length);
        if (wrapper != null) {
            capdu = wrapper.wrap(capdu);
        }
        ResponseAPDU rapdu = transmit(capdu);
        if (wrapper != null) {
            rapdu = wrapper.unwrap(rapdu, rapdu.getBuffer().length);
        }
        return rapdu.getData();
    }

    public CommandAPDU createUpdateBinaryAPDU(short offset, int data_len, byte[] data) {
        byte p1 = (byte) ((offset >>> 8) & 0xff);
        byte p2 = (byte) (offset & 0xff);
        CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816,
                             ISO7816.INS_UPDATE_BINARY,
                             p1,
                             p2,
                             data_len,
                             data,
                             -1);
        return apdu;
    }

    public byte[] sendUpdateBinary(SecureMessagingWrapper wrapper,
            short offset, int data_len, byte[] data) throws IOException {
        CommandAPDU capdu = createUpdateBinaryAPDU(offset, data_len, data);
        if (wrapper != null) {
            capdu = wrapper.wrap(capdu);
        }
        ResponseAPDU rapdu = transmit(capdu);
        if (wrapper != null) {
            rapdu = wrapper.unwrap(rapdu, rapdu.getBuffer().length);
        }
        return rapdu.getData();

    }

    /** 
     * Returns the maximum allowable plaintext data length that causes the 
     * wrapped apdu length to fit in a byte.
     * 
     * @param a wrapper.
     * @return additional length of wrapped APDU.
     */
    private int getPlainDataMaxLength(SecureMessagingWrapper wrapper) {
        int maxWithoutSM = 0xff;
        byte[] dummyData = new byte[maxWithoutSM];
        CommandAPDU dummy = new CommandAPDU((byte)0, (byte)0, (byte)0, (byte)0, dummyData);
        byte[] wrappedApdu = wrapper.wrap(dummy).getBuffer();
        int x = wrappedApdu.length - dummy.getBuffer().length;
        int lowestMod8 = ((maxWithoutSM - x) / 8) * 8; 
        return lowestMod8;
    }


    public void writeFile(SecureMessagingWrapper wrapper, short fid,
            InputStream i) throws IOException {
        int length = 0xff;
        if (wrapper != null) {
            length -= 32;
        }
        byte[] data = new byte[length];

        int r = 0;
        short offset = 0;
        while (true) {
            r = i.read(data, (short) 0, data.length);
            if (r == -1)
                break;
            sendUpdateBinary(wrapper, offset, r, data);
            offset += r;
        }
    }

    public CommandAPDU createMRZApdu(byte[] docNr, byte[] dob, byte[] doe) {
        byte cla = 0;
        byte ins = INS_SET_DOCNR_DOB_DOE;
        byte p1 = 0;
        byte p2 = 0;
        int lc = docNr.length + dob.length + doe.length;
        if (lc != 9 + 6 + 6) // sanity check
            return null;
        byte[] data = new byte[lc];
        byte data_p = 0;
        System.arraycopy(docNr, (short) 0, data, data_p, docNr.length);
        data_p += docNr.length;
        System.arraycopy(dob, (short) 0, data, data_p, dob.length);
        data_p += dob.length;
        System.arraycopy(doe, (short) 0, data, data_p, doe.length);

        return new CommandAPDU(cla, ins, p1, p2, data);
    }

    public void putMRZ(byte[] docNr, byte[] dob, byte[] doe) 
    throws IOException {
        BERTLVObject mrzObject = new BERTLVObject(MRZ_TAG, 
                                                  new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG,
                                                                   docNr));
        mrzObject.addSubObject(new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG, dob));
        mrzObject.addSubObject(new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG, doe));
        
        putData(null, (byte)0, MRZ_TAG, mrzObject.getEncoded());  
    }
    
    public void writeMRZ(byte[] docNr, byte[] dob, byte[] doe) {
        CommandAPDU capdu = createMRZApdu(docNr, dob, doe);
        transmit(capdu);
    }

    public InputStream createDG15(PublicKey key) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] keyBytes = key.getEncoded();

        out.write(0x6f);
        out.write(ASN1Utils.lengthId(keyBytes.length));
        out.write(keyBytes);

        return new ByteArrayInputStream(out.toByteArray());
    }

    public void lockApplet(SecureMessagingWrapper wrapper) {
        putData(wrapper, (byte)0xde, (byte)0xad, null);
    }
    
    public void selectFile(SecureMessagingWrapper wrapper, byte[] fid)
    throws IOException {
       sendSelectFile(wrapper, fid);
    }
    
    public void selectFile(SecureMessagingWrapper wrapper, short fid)
    throws IOException {
       byte[] fiddle = { (byte)((fid >>> 8) & 0xff), (byte)(fid & 0xff) };  
       selectFile(wrapper, fiddle);
    }
}
