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
 * $Id: $
 */

package sos.mrtd;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
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
import sos.smartcards.CardService;
import sos.smartcards.ISO7816;
import sos.util.ASN1Utils;

/**
 * Service for initializing blank passport reference applet.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 */
public class PassportInitService extends PassportApduService {
    public static final byte INS_SET_DOCNR_DOB_DOE = (byte) 0x10;
    public static final short AAPRIVKEY_FID = 0x0001;

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

    public static byte[] publicKey2DG15(PublicKey key) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] keyBytes = key.getEncoded();

        out.write(0x6f);
        out.write(ASN1Utils.lengthId(keyBytes.length));
        out.write(keyBytes);

        return out.toByteArray();
    }

    public static void writeAAPublicKey(PublicKey key) {

    }

    public void createFile(SecureMessagingWrapper wrapper, byte[] fid,
            byte[] len) {
        sendCreateFile(wrapper, fid, len);
    }

    // FIXME
    Apdu createCreateFileAPDU(byte[] fid, byte[] len) {
        byte p1 = (byte) 0x00;
        byte p2 = (byte) 0x00;
        int le = 0;
        byte[] data = { 0x63, 4, len[0], len[1], fid[0], fid[1] };
        Apdu apdu = new Apdu(ISO7816.CLA_ISO7816,
                             ISO7816.INS_CREATE_FILE,
                             p1,
                             p2,
                             data,
                             le);
        return apdu;
    }

    public byte[] sendCreateFile(SecureMessagingWrapper wrapper, byte[] fid,
            byte[] len) {
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

    Apdu createUpdateBinaryAPDU(short offset, int data_len, byte[] data) {
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
            FileInputStream i) throws IOException {
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

    public void sendAAPrivateKey(SecureMessagingWrapper wrapper,
            PrivateKey privKey) {
        // secretly, we just create a file, select it, and push the data in
        // we then send a secret "close" operation
    }
}
