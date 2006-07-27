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
import java.io.DataInputStream;
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

import sos.smartcards.Apdu;
import sos.smartcards.BERTLVObject;
import sos.smartcards.CardService;
import sos.smartcards.ISO7816;
import sos.util.ASN1Utils;

/**
 * Service for initializing blank passport reference applets.
 * 
 * Following MO's design, this class must be split in two:
 *  - PassportInitService
 *  - PassportInitApduService
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 */
public class PassportInitService extends PassportApduService {
    public static final byte INS_SET_DOCNR_DOB_DOE = (byte) 0x10;
    public static final short AAPRIVKEY_FID = 0x0001;
    private static final byte INS_PUT_DATA = (byte)0xda;;
    private static final byte PRIVKEY_TAG = 0x41;
    private static final byte MRZ_TAG = 0x42;
    
    public PassportInitService(CardService service)
            throws GeneralSecurityException {
        super(service);
    }

    public KeyPair generateAAKeyPair() throws GeneralSecurityException,
            NoSuchAlgorithmException {
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

    public Apdu createPutDataApdu(byte p1, byte p2, byte[] data) {
        byte cla = 0;
        byte ins = INS_PUT_DATA;
        
        return new Apdu(cla, ins, p1, p2, data);
    }
    
    public void putData(SecureMessagingWrapper wrapper, byte p1, byte p2, byte[] data) {
        Apdu apdu = createPutDataApdu(p1, p2, data);
        
        if(wrapper != null) {
            apdu.wrapWith(wrapper);
        }
        sendAPDU(apdu);
    }

    public void putPrivateKey(SecureMessagingWrapper wrapper, PrivateKey key) 
    throws IOException {
        byte[] encodedPriv = key.getEncoded();
        BERTLVObject encodedPrivObject = BERTLVObject.getInstance(new ByteArrayInputStream(encodedPriv));
        byte[] privKeyData = encodedPrivObject.getChildByIndex(2).getValueAsBytes();
        BERTLVObject privKeyDataObject = BERTLVObject.getInstance(new ByteArrayInputStream(privKeyData));        
        byte[] privModulus =  privKeyDataObject.getChildByIndex(1).getValueAsBytes();
        byte[] privExponent =  privKeyDataObject.getChildByIndex(3).getValueAsBytes();

        putPrivateKey(wrapper, privModulus, privExponent);
    }
    
    private void putPrivateKey(SecureMessagingWrapper wrapper, 
             byte[] privModulus, byte[]  privExponent) 
    throws IOException {
        if(privExponent.length != 128 || privExponent.length != 128) {
            return;
        }
        
        BERTLVObject object = new BERTLVObject(PRIVKEY_TAG);
        object.addSubObject(new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG, 
                                             privModulus));
        object.addSubObject(new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG, 
                                             privExponent));
        
        putData(wrapper, (byte)0, PRIVKEY_TAG, object.getEncoded());           
    }

    public void createFile(SecureMessagingWrapper wrapper, short fid,
            short len) {
        
        sendCreateFile(wrapper, fid, len);
    }

    // FIXME
    public Apdu createCreateFileAPDU(short fid, short len) {
        byte p1 = (byte) 0x00;
        byte p2 = (byte) 0x00;
        int le = 0;
        byte[] data = { 0x63, 4, (byte)((len >>> 8) & 0xff), 
                                 (byte)(len & 0xff), 
                                 (byte)((fid >>> 8) & 0xff), 
                                 (byte)(fid & 0xff) };
        Apdu apdu = new Apdu(ISO7816.CLA_ISO7816,
                             ISO7816.INS_CREATE_FILE,
                             p1,
                             p2,
                             data,
                             le);
        return apdu;
    }

    public byte[] sendCreateFile(SecureMessagingWrapper wrapper, short fid,
            short len) {
        Apdu capdu = createCreateFileAPDU(fid, len);
        if (wrapper != null) {
            capdu.wrapWith(wrapper);
        }
        byte[] rapdu = sendAPDU(capdu);
        if (wrapper != null) {
            rapdu = wrapper.unwrap(rapdu, rapdu.length);
        }
        byte[] result = new byte[rapdu.length - 2];
        System.arraycopy(rapdu, 0, result, 0, rapdu.length - 2);
        return result;
    }

    public Apdu createUpdateBinaryAPDU(short offset, int data_len, byte[] data) {
        byte p1 = (byte) ((offset & 0x0000FF00) >> 8);
        byte p2 = (byte) (offset & 0x000000FF);
        Apdu apdu = new Apdu(ISO7816.CLA_ISO7816,
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
        Apdu capdu = createUpdateBinaryAPDU(offset, data_len, data);
        if (wrapper != null) {
            capdu.wrapWith(wrapper);
        }
        byte[] rapdu = sendAPDU(capdu);
        if (wrapper != null) {
            rapdu = wrapper.unwrap(rapdu, rapdu.length);
        }
        byte[] result = new byte[rapdu.length - 2];
        System.arraycopy(rapdu, 0, result, 0, rapdu.length - 2);
        return result;

    }

    public void writeFile(SecureMessagingWrapper wrapper, short fid,
            InputStream i) throws IOException {
        byte[] data = new byte[0xdf]; // FIXME: magic

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

    public Apdu createMRZApdu(byte[] docNr, byte[] dob, byte[] doe) {
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

        return new Apdu(cla, ins, p1, p2, data);
    }

    public void writeMRZ(byte[] docNr, byte[] dob, byte[] doe) {
        Apdu capdu = createMRZApdu(docNr, dob, doe);
        sendAPDU(capdu);
    }

    public InputStream createDG15(PublicKey key) 
    throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
        
      byte[] keyBytes = key.getEncoded();
      
      out.write(0x6f);
      out.write(ASN1Utils.lengthId(keyBytes.length));
      out.write(keyBytes);
      
      return new ByteArrayInputStream(out.toByteArray());
    }
}
