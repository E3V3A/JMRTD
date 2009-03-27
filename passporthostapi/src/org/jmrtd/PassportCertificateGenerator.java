/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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
 * $Id: PassportApduService.java 894 2009-03-23 15:50:46Z martijno $
 */

package org.jmrtd;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ejbca.cvc.AccessRightEnum;
import org.ejbca.cvc.AuthorizationRoleEnum;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CertificateGenerator;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.cvc.HolderReferenceField;

public class PassportCertificateGenerator {

    public static final String filenameCA = "/home/sos/woj/terminals/cacert.cvcert";

    public static final String filenameTerminal = "/home/sos/woj/terminals/terminalcert.cvcert";

    public static final String filenameKey = "/home/sos/woj/terminals/terminalkey.der";

    public static void main(String[] args) {
        try {
            // Install BC as security provider
            Security.addProvider(new BouncyCastleProvider());

            // Get the current time, and +3 months
            Calendar cal1 = Calendar.getInstance();
            Date validFrom = cal1.getTime();

            Calendar cal2 = Calendar.getInstance();
            cal2.add(Calendar.MONTH, 3);
            Date validTo = cal2.getTime();

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
            SecureRandom random = new SecureRandom();
            keyGen.initialize(1024, random);

            // Create a new key pair for the self signed CA certificate
            KeyPair caKeyPair = keyGen.generateKeyPair();

            // Create a new key pair for the terminal certificate (signed by CA)
            keyGen.initialize(1024, random);
            KeyPair terminalKeyPair = keyGen.generateKeyPair();

            CAReferenceField caRef = new CAReferenceField("NL", "MYDL-CVCA",
                    "00001");
            HolderReferenceField holderRef = new HolderReferenceField(caRef
                    .getCountry(), caRef.getMnemonic(), caRef.getSequence());

            // Create the CA certificate
            CVCertificate caCvc = CertificateGenerator.createCertificate(
                    caKeyPair.getPublic(), caKeyPair.getPrivate(),
                    "SHA1WithRSA", caRef, holderRef,AuthorizationRoleEnum.CVCA, AccessRightEnum.READ_ACCESS_DG3_AND_DG4, validFrom,
                    validTo, "BC");

            // Create the terminal certificate
            HolderReferenceField terminalHolderRef = new HolderReferenceField(
                    "NL", "RUDL-CVCT", "00001");

            CVCertificate terminalCvc = CertificateGenerator.createCertificate(
                    terminalKeyPair.getPublic(), caKeyPair.getPrivate(),
                    "SHA1WithRSA", caRef, terminalHolderRef,
                    AuthorizationRoleEnum.IS, AccessRightEnum.READ_ACCESS_DG3_AND_DG4,
                    validFrom, validTo, "BC");

            // Get the raw data from certificates and write to default files.
            // Overwrites the files without question!!!
            byte[] caCertData = caCvc.getDEREncoded();
            byte[] terminalCertData = terminalCvc.getDEREncoded();
            byte[] terminalPrivateKey = terminalKeyPair.getPrivate()
                    .getEncoded();

            writeFile(new File(filenameCA), caCertData);
            writeFile(new File(filenameTerminal), terminalCertData);
            writeFile(new File(filenameKey), terminalPrivateKey);

            // Test - read the filew again and parse its contents,
            // spit out the certificates

            CVCertificate c = readCVCertificateFromFile(new File(
                    filenameCA));
            System.out.println(c.getCertificateBody().getAsText());

            c = readCVCertificateFromFile(new File(filenameTerminal));
            System.out.println(c.getCertificateBody().getAsText());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static CVCertificate readCVCertificateFromFile(File f) {
        try {
            byte[] data = loadFile(f);
            CVCObject parsedObject = CertificateParser.parseCertificate(data);
            CVCertificate c = (CVCertificate) parsedObject;
            return c;
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Reads the byte data from a file.
     * 
     * @param path
     *            the path to the file
     * @return the raw contents of the file
     * @throws IOException
     *             if there are problems
     */
    public static byte[] loadFile(String path) throws IOException {
        return loadFile(new File(path));
    }

    /**
     * Reads the byte data from a file.
     * 
     * @param file
     *            the file object to read data from
     * @return the raw contents of the file
     * @throws IOException
     *             if there are problems
     */
    public static byte[] loadFile(File file) throws IOException {
        byte[] dataBuffer = null;
        FileInputStream inStream = null;
        try {
            // Simple file loader...
            int length = (int) file.length();
            dataBuffer = new byte[length];
            inStream = new FileInputStream(file);

            int offset = 0;
            int readBytes = 0;
            boolean readMore = true;
            while (readMore) {
                readBytes = inStream.read(dataBuffer, offset, length - offset);
                offset += readBytes;
                readMore = readBytes > 0 && offset != length;
            }
        } finally {
            try {
                if (inStream != null)
                    inStream.close();
            } catch (IOException e1) {
                System.out.println("loadFile - error when closing: " + e1);
            }
        }
        return dataBuffer;
    }

    /**
     * Writes raw data to a file.
     * 
     * @param path
     *            path to the file to be written (no overwrite checks!)
     * @param data
     *            raw data to be written
     * @throws IOException
     *             if something goes wrong
     */
    public static void writeFile(String path, byte[] data) throws IOException {
        writeFile(new File(path), data);
    }

    /**
     * Writes raw data to a file.
     * 
     * @param file
     *            the file object to be written (no overwrite checks!)
     * @param data
     *            raw data to be written
     * @throws IOException
     *             if something goes wrong
     */
    public static void writeFile(File file, byte[] data) throws IOException {
        FileOutputStream outStream = null;
        BufferedOutputStream bout = null;
        try {
            outStream = new FileOutputStream(file);
            bout = new BufferedOutputStream(outStream, 1000);
            bout.write(data);
        } finally {
            if (bout != null)
                bout.close();
        }
    }

}
