package org.jmrtd.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import org.ejbca.cvc.PublicKeyEC;
import org.jmrtd.PassportService;

public class PassportEACTester extends PassportTesterBase {

    public PassportEACTester(String name) {
        super(name);
    }

    private static final File testDVDcert = new File(
            "certs/certDVD_orig.cvcert");

    private static final File testIScert = new File("certs/certIS_orig.cvcert");

    private static final File testISkey = new File("certs/keyIS_orig.der");

    private static final File testCVCAkey = new File("certs/keyCVCA_orig.der");

    /**
     * Tests the normal EAC behavior: sends the original certificates to the
     * passport and tests reading out DG3.
     * 
     */
    public void testEAC1() {
        traceApdu = true;
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testISkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertTrue(service.doTA(new CVCertificate[] { c1, c2 }, k));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
        traceApdu = false;
    }

    /**
     * This test tries to authenticate with CVCA private key and then
     * immediately read DG3. That is, the certificate chain down to IS is not
     * presented to the passport, but a valid challenge response (last stage of
     * TA) is attempted. This alone should fail. Since there are not
     * certificates in the chain we have to help the TA method with providing
     * the signature algorithm for challenge response.
     * 
     */
    public void testEAC2a() {
        traceApdu = true;
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(new CVCertificate[] {}, k, "SHA224WITHECDSA"));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        traceApdu = false;
    }

    /**
     * This test tries to authenticate with DVD private key and then immediately
     * read DG3. That is, the certificate chain down to IS is not presented to
     * the passport, only the DV certificate and then a valid challenge response
     * is attempted. This alone should fail.
     * 
     */
    public void testEAC2b() {
        traceApdu = true;
        // We do not have a private key for the DV certificate, thus we have
        // to make our own, temporary DV certificate, here we try the domestic
        // one, in the next test we try the foreign one
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CVCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        assertNotNull(dvCert);
        CertsKeyPair ck = createNewCertificate(dvCert, k, 0);
        for(CVCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        // We don't get pass this point:
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        traceApdu = false;
    }

    /**
     * This test tries to authenticate with DVD private key and then immediately
     * read DG3. That is, the certificate chain down to IS is not presented to
     * the passport, only the DV certificate and then a valid challenge response
     * is attempted. This alone should fail.
     * 
     */
    public void testEAC2c() {
        traceApdu = true;
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CVCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        assertNotNull(dvCert);
        CertsKeyPair ck = createNewCertificate(dvCert, k, 1);
        for(CVCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        // We don't get pass this point:
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        traceApdu = false;
    }

    /**
     * This test tries to authenticate with the IS certificate immediately
     * (signed by CVCA). That is, there is no DV certificate sent.
     * 
     */
    public void testEAC3() {
        traceApdu = true;
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CVCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificate(isCert, k, 2);
        for(CVCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        traceApdu = false;
    }

    /**
     * This test authenticates to the passport using a short chain of freshly
     * created certificates. The new certificates are copies of the old one, but
     * freshly signed and with new terminal private key.
     * 
     */
    public void testEAC4a() {
        traceApdu = true;
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CVCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        CVCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(dvCert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificates(new CVCertificate[] { dvCert,
                isCert }, k, "d", 1);
        for(CVCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertTrue(service.doTA(ck.certs, ck.key));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
        traceApdu = false;
    }

    /**
     * This test authenticates to the passport using a short chain of freshly
     * created certificates. The new certificates are copies of the old one, but
     * freshly signed and with new terminal private key. The domestic
     * certificate in the chain is changed to foreign one.
     * 
     */
    public void testEAC4b() {
        traceApdu = true;
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CVCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        CVCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(dvCert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificates(new CVCertificate[] { dvCert,
                isCert }, k, "f", 1);
        for(CVCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertTrue(service.doTA(ck.certs, ck.key));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
        traceApdu = false;
    }

    /**
     * This test authenticates to the passport using a longer chain of freshly
     * created certificates. The DV and IS are changed from single instances to
     * multiples, like
     * 
     * Orig: CVCA -> DV(D/F) -> IS
     * New: CVCA -> [DV(D/F) -> DV(D/F)]1+ -> [IS -> IS]1+
     * 
     * The new certificates are based on the old ones. 
     * 
     * This test fails on the test passport. The passport apparently expects
     * only one DV certificate and one IS certificate. 
     * 
     * An attempt to send an "unexpected", but valid type of certificate
     * results in FILE INVALID 6983. 
     * 
     * Here we try 2 DV certificates and one IS certificate.
     */
    public void testEAC5a() {
        traceApdu = true;
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CVCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        CVCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(dvCert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificates(new CVCertificate[] { dvCert,
                isCert }, k, "dd", 1);
        for(CVCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);

        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));

        traceApdu = false;
    }

    /** A variation of the previous test with two IS certificates */
    public void testEAC5b() {
        traceApdu = true;
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CVCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        CVCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(dvCert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificates(new CVCertificate[] { dvCert,
                isCert }, k, "d", 2);
        for(CVCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);

        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));

        traceApdu = false;
    }

    /**
     * TODO: This is not a description for this method! This creates a new root
     * certificate for the passport with the following features: - if the given
     * public key is not null it is used (here you have to remember to save the
     * corresponding private key!!!) - if there is no public key given, the old
     * one (stored in the certificate) is used - the new certificate starts to
     * be valid when the old one expired and is valid for 3 years - the holder
     * reference counter is increased by 1
     */
    public static CertsKeyPair createNewCertificate(CVCertificate oldCert,
            PrivateKey privateKey, int type) {
        try {
            CVCertificateBody body = oldCert.getCertificateBody();

            CAReferenceField caRef = body.getAuthorityReference();
            HolderReferenceField holderRef = body.getHolderReference();
            Date validFrom = body.getValidFrom();
            Date validTo = body.getValidTo();
            AuthorizationRoleEnum role = body.getAuthorizationTemplate()
                    .getAuthorizationField().getRole();
            if (type == 0) {
                role = AuthorizationRoleEnum.DV_D;
            } else if (type == 1) {
                role = AuthorizationRoleEnum.DV_F;
            } else if (type == 2) {
                role = AuthorizationRoleEnum.IS;
            }
            AccessRightEnum rights = body.getAuthorizationTemplate()
                    .getAuthorizationField().getAccessRight();
            CVCPublicKey publicKey = body.getPublicKey();
            String algName = AlgorithmUtil.getAlgorithmName(publicKey
                    .getObjectIdentifier());

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA",
                    "BC");
            keyGen.initialize(((ECPrivateKey) privateKey).getParams(),
                    new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            CVCertificate newCert = CertificateGenerator.createCertificate(
                    keyPair.getPublic(), privateKey, algName, caRef, holderRef,
                    role, rights, validFrom, validTo, "BC");
            return new CertsKeyPair(newCert, keyPair.getPrivate());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Resigns the chain of certificates.
     * 
     * @param oldCerts
     *            the certificates to be resigned
     * @param privateKey
     *            the root certificate private key
     * @param dvPattern the string specifying
     *               the DV certificates (domestic or foregin)
     * @param chainLengthDV the chain length for DV certifcates
     * @param chainLengthIS the chain length for IS certifcates
     *            
     * @return CertsKeyPair structure with new certificates and new private key
     */
    public static CertsKeyPair createNewCertificates(CVCertificate[] oldCerts,
            PrivateKey privateKey, String dvPattern, int chainLengthIS) {
        try {
            List<CVCertificate> newCerts = new ArrayList<CVCertificate>();
            CAReferenceField caRef = null;
            CVCertificate prevCert = null;
            PublicKey prevKey = null;
            for (CVCertificate oldCert : oldCerts) {
                int chainLength = oldCert.getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole() == AuthorizationRoleEnum.IS ? chainLengthIS : dvPattern.length();
                for (int i = 0; i < chainLength; i++) {
                    CVCertificateBody body = oldCert.getCertificateBody();
                    if (caRef == null) {
                        caRef = body.getAuthorityReference();
                    }
                    HolderReferenceField holderRef = body.getHolderReference();
                    if (holderRef.getConcatenated().equals(
                            caRef.getConcatenated())) {
                        holderRef = increase(holderRef);
                    }
                    Date validFrom = body.getValidFrom();
                    Date validTo = body.getValidTo();
                    AuthorizationRoleEnum role = body
                            .getAuthorizationTemplate().getAuthorizationField()
                            .getRole();
                    
                    if (role == AuthorizationRoleEnum.DV_D || role == AuthorizationRoleEnum.DV_F) {
                        char dvChar = dvPattern.charAt(i);
                        switch(dvChar) {
                        case 'd' :
                            role = AuthorizationRoleEnum.DV_D;
                            break;
                        case 'f' :
                            role = AuthorizationRoleEnum.DV_F;
                            break;
                        default:
                        }
                    }
                    AccessRightEnum rights = body.getAuthorizationTemplate()
                            .getAuthorizationField().getAccessRight();
                    CVCPublicKey publicKey = body.getPublicKey();
                    String algName = AlgorithmUtil.getAlgorithmName(publicKey
                            .getObjectIdentifier());

                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(
                            "ECDSA", "BC");
                    keyGen.initialize(((ECPrivateKey) privateKey).getParams(),
                            new SecureRandom());
                    KeyPair keyPair = keyGen.generateKeyPair();

                    CVCertificate newCert = CertificateGenerator
                            .createCertificate(keyPair.getPublic(), privateKey,
                                    algName, caRef, holderRef, role, rights,
                                    validFrom, validTo, "BC");
                    newCerts.add(newCert);
                    privateKey = keyPair.getPrivate();
                    caRef = new CAReferenceField(holderRef.getCountry(),
                            holderRef.getMnemonic(), holderRef.getSequence());
                    oldCert = newCert;
                    // We want to make sure that the chain that we created is correct:
                    if(prevCert != null) {
                        newCert.verify(prevKey, "BC");
                    }
                    prevCert = newCert;
                    prevKey = keyPair.getPublic();
                }
            }
            return new CertsKeyPair(newCerts.toArray(oldCerts),
                    privateKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CAReferenceField increase(CAReferenceField f) {
        String c = f.getCountry();
        String m = f.getMnemonic();
        String s = f.getSequence();
        s = increase(s);
        return new CAReferenceField(c, m, s);
    }

    private static HolderReferenceField increase(HolderReferenceField f) {
        String c = f.getCountry();
        String m = f.getMnemonic();
        String s = f.getSequence();
        s = increase(s);
        return new HolderReferenceField(c, m, s);
    }

    private static String increase(String c) {
        int index = 0;
        while(Character.isLetter(c.charAt(index++)));
        String prefix = c.substring(0,--index);
        String count = c.substring(index);
        int s = Integer.parseInt(count) + 1;
        c = "" + s;
        while (c.length() != count.length()) {
            c = "0" + c;
        }
        return prefix+c;
    }

    private static class CertsKeyPair {
        CVCertificate[] certs;

        PrivateKey key;

        CertsKeyPair(CVCertificate cert, PrivateKey key) {
            this.certs = new CVCertificate[] { cert };
            this.key = key;
        }

        CertsKeyPair(CVCertificate[] certs, PrivateKey key) {
            this.certs = certs;
            this.key = key;
        }

    }

}
