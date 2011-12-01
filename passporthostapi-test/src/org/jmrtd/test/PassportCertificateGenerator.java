package org.jmrtd.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

import org.ejbca.cvc.AccessRightEnum;
import org.ejbca.cvc.AlgorithmUtil;
import org.ejbca.cvc.AuthorizationRoleEnum;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CVCPublicKey;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CVCertificateBody;
import org.ejbca.cvc.CertificateGenerator;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.cvc.HolderReferenceField;
import org.jmrtd.JMRTDSecurityProvider;

public class PassportCertificateGenerator {

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();
	
    public static final String rootCertFile = "/home/sos/woj/passportcert/rootcert.cvcert";
    public static final String newRootCertFile = "/home/sos/woj/passportcert/rootcert.cvcert";
    public static final String rootKeyFile = "/home/sos/woj/passportcert/rootkey.der";

//    public static final String filenameTerminal = "/home/sos/woj/passportcert/terminalcert.cvcert";
//    public static final String filenameKey = "/home/sos/woj/passportcert/terminalkey.der";
   


    /** This creates a new root certificate for the passport with the following features:
     * 
     *   - if the given public key is not null it is used (here you have to remember to save the 
     *     corresponding private key!!!)
     *   - if there is no public key given, the old one (stored in the certificate) is used
     *   - the new certificate starts to be valid when the old one expired and is valid for 3 years
     *   - the holder reference counter is increased by 1 
     */
    public static CVCertificate createNewRootCertificate(CVCertificate oldCert, CVCPublicKey publicKey, PrivateKey privateKey) {
        try {
        CVCertificateBody body = oldCert.getCertificateBody();
        if(publicKey == null) { publicKey = body.getPublicKey(); };

        String country = body.getAuthorityReference().getCountry();
        String mnemonic = body.getAuthorityReference().getMnemonic();
        String sequence = body.getAuthorityReference().getSequence();
        CAReferenceField caRef = new CAReferenceField(country,mnemonic,sequence);
        sequence = increase(sequence);
        HolderReferenceField holderRef = new HolderReferenceField(country,mnemonic,sequence);
        Date validFrom = body.getValidFrom();
        Date validTo = body.getValidTo();
        validFrom = validTo;
        Calendar cal = Calendar.getInstance();
        cal.setTime(validFrom);
        cal.add(Calendar.YEAR, 3);
        validTo = cal.getTime();
        AuthorizationRoleEnum role = body.getAuthorizationTemplate().getAuthorizationField().getRole();
        AccessRightEnum rights = body.getAuthorizationTemplate().getAuthorizationField().getAccessRight();
        String algName = AlgorithmUtil.getAlgorithmName(publicKey.getObjectIdentifier());
        
        CVCertificate newRoot = CertificateGenerator.createCertificate(
                publicKey, privateKey,
                algName, caRef, holderRef, role, rights, validFrom,
                validTo, "BC");
        return newRoot;
        }catch(Exception e) {
            return null;
        }
    }
    
    private static String increase(String c) {
        int s = Integer.parseInt(c) + 1;
        c = ""+s;
        while(c.length() != 5) {
            c = "0" + c;
        }
        return c;
    }
    
    public static void main(String[] args) {
        try {
            Security.addProvider(BC_PROVIDER);

            CVCertificate rootCert = readCVCertificateFromFile(new File(rootCertFile));
            String keyAlg = "ECDSA";
            PrivateKey rootPrivateKey = readKeyFromFile(new File(rootKeyFile), keyAlg);

            System.out.println("Root cert: "+rootCert);
            System.out.println("Root private key: "+rootPrivateKey);
            

/*
            Calendar cal1 = Calendar.getInstance();
            Date validFrom = cal1.getTime();

            Calendar cal2 = Calendar.getInstance();
            cal2.add(Calendar.MONTH, 3);
            Date validTo = cal2.getTime();
  */          
            CVCertificate newRoot = createNewRootCertificate(rootCert, null, rootPrivateKey);
            System.out.println("New root cert: "+newRoot);            
            
            
            System.exit(1);
            
            // Get the current time, and +3 months

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
            SecureRandom random = new SecureRandom();
            
            
            keyGen.initialize(1024, random);

            // Create a new key pair for the self signed CA certificate
            KeyPair caKeyPair = keyGen.generateKeyPair();

            // Create a new key pair for the terminal certificate (signed by CA)
            keyGen.initialize(1024, random);
            KeyPair terminalKeyPair = keyGen.generateKeyPair();

//            CAReferenceField caRef = new CAReferenceField("NL", "MYDL-CVCA",
//                    "00001");
//            HolderReferenceField holderRef = new HolderReferenceField(caRef
//                    .getCountry(), caRef.getMnemonic(), caRef.getSequence());

            // Create the CA certificate
//            CVCertificate caCvc = CertificateGenerator.createCertificate(
//                    caKeyPair.getPublic(), caKeyPair.getPrivate(),
//                    "SHA1WithRSA", caRef, holderRef,AuthorizationRoleEnum.CVCA, AccessRightEnum.READ_ACCESS_DG3_AND_DG4, validFrom,
//                    validTo, "BC");

            // Create the terminal certificate
            HolderReferenceField terminalHolderRef = new HolderReferenceField(
                    "NL", "RUDL-CVCT", "00001");

//            CVCertificate terminalCvc = CertificateGenerator.createCertificate(
//                    terminalKeyPair.getPublic(), caKeyPair.getPrivate(),
//                    "SHA1WithRSA", caRef, terminalHolderRef,
//                    AuthorizationRoleEnum.IS, AccessRightEnum.READ_ACCESS_DG3_AND_DG4,
//                    validFrom, validTo, "BC");

            // Get the raw data from certificates and write to default files.
            // Overwrites the files without question!!!
            //byte[] caCertData = caCvc.getDEREncoded();
            //byte[] terminalCertData = terminalCvc.getDEREncoded();
            //byte[] terminalPrivateKey = terminalKeyPair.getPrivate()
              //      .getEncoded();

//            writeFile(new File(filenameCA), caCertData);
//            writeFile(new File(filenameTerminal), terminalCertData);
//            writeFile(new File(filenameKey), terminalPrivateKey);

            // Test - read the filew again and parse its contents,
            // spit out the certificates

//            CVCertificate c = readCVCertificateFromFile(new File(
//                    filenameCA));
//            System.out.println(c.getCertificateBody().getAsText());

//            c = readCVCertificateFromFile(new File(filenameTerminal));
//            System.out.println(c.getCertificateBody().getAsText());

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

    private static PrivateKey readKeyFromFile(File f, String algName) {
        try {
            byte[] data = loadFile(f);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
            KeyFactory gen = KeyFactory.getInstance(algName);
            return gen.generatePrivate(spec);
        } catch (Exception e) {
            return null;
        }

    }

}
