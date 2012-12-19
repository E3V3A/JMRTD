package org.jmrtd.test;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.smartcards.CardServiceException;

import org.jmrtd.PassportService;
import org.jmrtd.cert.CVCAuthorizationTemplate;
import org.jmrtd.cert.CVCAuthorizationTemplate.Permission;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CVCertificateBuilder;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.CVCAFile;

public class PassportEACTester extends PassportTesterBase {

    public PassportEACTester(String name) {
        super(name);
//        traceApdu = true;
    }

    private static final File testDVDcert = new File("certs/certDVD_orig.cvcert");

    private static final File testIScert = new File("certs/certIS_orig.cvcert");

    private static final File testISkey = new File("certs/keyIS_orig.der");

    private static final File testCVCAcert = new File("certs/certCVCA_orig.cvcert");

    private static final File testCVCAkey = new File("certs/keyCVCA_orig.der");

    /**
     * Tests the normal EAC behavior: sends the original certificates to the
     * passport and tests reading out DG3.
     * 
     */
    public void testEAC1() {
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testISkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertTrue(service.doTA(new CardVerifiableCertificate[] { c1, c2 }, k));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
    }
    /**
     * Tests the normal EAC behavior: sends the original certificates to the
     * passport and tests reading out DG3.
     * 
     */

    /**
     *  Test EAC multiple times to see whether there any abnormalities in 
     *  ECC implementation. With 1024 iterations takes almost an hour to
     *  finish.
     */
    public void testEACMultiple() {
        List<Integer> failed = new ArrayList<Integer>();
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testISkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        int totalTests = 1024;
        int taFailures = 0;
        for(int t = 0;t<totalTests;t++) {
          try {
            System.out.print("Test "+t+" ");
            resetCard();
          } catch (CardServiceException e) {
            assertTrue(false);
          }
          service.doBAC();
          assertTrue(service.doCA());
          boolean resultTa = service.doTA(new CardVerifiableCertificate[] { c1, c2 }, k);
          if(!resultTa) {
              System.out.println("Failed attempt #"+t);
              taFailures++;
              failed.add(t);
          }
        }
        System.out.println("Out of "+totalTests+" tries TA failed "+taFailures+" times.");
        System.out.println("Failed test #-s "+failed);
        assertTrue(failed.size() == 0);
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
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(new CardVerifiableCertificate[] {}, k, "SHA224WITHECDSA"));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * This test tries to authenticate with DVD private key and then immediately
     * read DG3. That is, the certificate chain down to IS is not presented to
     * the passport, only the DV certificate and then a valid challenge response
     * is attempted. This alone should fail.
     * 
     */
    public void testEAC2b() {
        // We do not have a private key for the DV certificate, thus we have
        // to make our own, temporary DV certificate, here we try the domestic
        // one, in the next test we try the foreign one
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CardVerifiableCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        assertNotNull(dvCert);
        CertsKeyPair ck = createNewCertificate(dvCert, k, 0);
        for(CardVerifiableCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        // We don't get pass this point:
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * This test tries to authenticate with DVD private key and then immediately
     * read DG3. That is, the certificate chain down to IS is not presented to
     * the passport, only the DV certificate and then a valid challenge response
     * is attempted. This alone should fail.
     * 
     */
    public void testEAC2c() {
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CardVerifiableCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        assertNotNull(dvCert);
        CertsKeyPair ck = createNewCertificate(dvCert, k, 1);
        for(CardVerifiableCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        // We don't get pass this point:
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * This test tries to authenticate with the IS certificate immediately
     * (signed by CVCA). That is, there is no DV certificate sent.
     * 
     */
    public void testEAC3() {
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CardVerifiableCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificate(isCert, k, 2);
        for(CardVerifiableCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * This test authenticates to the passport using a short chain of freshly
     * created certificates. The new certificates are copies of the old one, but
     * freshly signed and with new terminal private key.
     * 
     */
    public void testEAC4a() {
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CardVerifiableCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(dvCert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificates(new CardVerifiableCertificate[] { dvCert,
                isCert }, k, "d", 1);
        for(CardVerifiableCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertTrue(service.doTA(ck.certs, ck.key));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * This test authenticates to the passport using a short chain of freshly
     * created certificates. The new certificates are copies of the old one, but
     * freshly signed and with new terminal private key. The domestic
     * certificate in the chain is changed to foreign one.
     * 
     */
    public void testEAC4b() {
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CardVerifiableCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(dvCert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificates(new CardVerifiableCertificate[] { dvCert,
                isCert }, k, "f", 1);
        for(CardVerifiableCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertTrue(service.doTA(ck.certs, ck.key));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
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
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CardVerifiableCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(dvCert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificates(new CardVerifiableCertificate[] { dvCert,
                isCert }, k, "dd", 1);
        for(CardVerifiableCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);

        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
    }

    /** A variation of the previous test with two IS certificates */
    public void testEAC5b() {
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(k);
        CardVerifiableCertificate dvCert = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate isCert = readCVCertificateFromFile(testIScert);
        assertNotNull(dvCert);
        assertNotNull(isCert);
        CertsKeyPair ck = createNewCertificates(new CardVerifiableCertificate[] { dvCert,
                isCert }, k, "d", 2);
        for(CardVerifiableCertificate c : ck.certs) {
            assertNotNull(c);
        }
        assertNotNull(ck.key);

        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(ck.certs, ck.key));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * Test if we can resend the IS certificate if the one we sent first
     * was wrong (by inducing a wrong signature).
     * 
     * The  test says we cannot do it, we have to send the whole chain again,
     * this is covered in the next test.
     * 
     */
    public void testEAC6() {
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testISkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        try {
          c2.getSignature()[20] = (byte)0xFF;
        }catch(Exception e) {
        }

        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(new CardVerifiableCertificate[] { c1, c2 }, k));
        c2 = readCVCertificateFromFile(testIScert);
        assertNotNull(c2);
        assertFalse(service.doTA(new CardVerifiableCertificate[] { c2 }, k));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * See previous test. See if we can reattempt the TA process, with the
     * whole certificate chain, without repeating CA. 
     * 
     */
    public void testEAC6a() {
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testISkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        try {
          c2.getSignature()[20] = (byte)0xFF;
        }catch(Exception e) {
        }

        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertFalse(service.doTA(new CardVerifiableCertificate[] { c1, c2 }, k));
        c2 = readCVCertificateFromFile(testIScert);
        assertNotNull(c2);
        assertTrue(service.doTA(new CardVerifiableCertificate[] { c1, c2 }, k));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
    }

    
    /** Test that an expired IS Domestic certificate is not accepted.
     * This test takes a while. */     
    public void testEAC7a() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c2.getNotBefore();
          to = addDay(currentDate, -1);
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, false);
        assertFalse(verifyCerts(ck));
    }

    /** Test that en expired IS Foreign certificate is not accepted.
     * This test takes a while. */     
    public void testEAC7b() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c2.getNotBefore();
          to = addDay(currentDate, -1);
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, true);
        assertFalse(verifyCerts(ck));
    }

    
    /** Test that a IS Foreign certificate does not change the passport date.
     * This test takes a while (even longer than the previous one). */     
    public void testEAC7c() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = addDay(currentDate, 1);
          to = c2.getNotAfter();
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, true);
        assertTrue(verifyCerts(ck));
        Date newDate = findPassportDate(false);
        System.out.println("Current new date is: "+PassportService.SDF.format(newDate));
        assertTrue(checkEqualDates(currentDate, newDate));
    }

    /** Test that a IS Domestic certificate changes the passport date.
     * This test takes a while (about same as the previous one). */     
    public void testEAC7d() {
        Date currentDate = findPassportDate(true);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = addDay(currentDate, 1);
          to = c2.getNotAfter();
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, false);
        assertTrue(verifyCerts(ck));
        Date newDate = findPassportDate(false);
        System.out.println("Current new date is: "+PassportService.SDF.format(newDate));
        assertTrue(checkEqualDates(addDay(currentDate, 1), newDate));
    }


    
    
    /** Test that an expired DV Domestic certificate is not accepted.
     * This test takes a while. */     
    public void testEAC7e() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c1.getNotBefore();
          to = addDay(currentDate, -1);
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, false, true);
        ck = new CertsKeyPair(new CardVerifiableCertificate[]{ck.certs[0]}, null);
        assertFalse(verifyCerts(ck));
    }

    /** Test that en expired DV Foreign certificate is not accepted.
     * This test takes a while. */     
    public void testEAC7f() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c1.getNotBefore();
          to = addDay(currentDate, -1);
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, true, true);
        ck = new CertsKeyPair(new CardVerifiableCertificate[]{ck.certs[0]}, null);
        assertFalse(verifyCerts(ck));
    }

    
    /** Test that a DV Foreign certificate changes the passport date.
     * This test takes a while. */     
    public void testEAC7g() {
        Date currentDate = findPassportDate(true);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = addDay(currentDate, 1);
          to = c2.getNotAfter();
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, true, true);
        ck = new CertsKeyPair(new CardVerifiableCertificate[]{ck.certs[0]}, null);
        assertTrue(verifyCerts(ck));
        Date newDate = findPassportDate(false);
        System.out.println("Current new date is: "+PassportService.SDF.format(newDate));
        assertTrue(checkEqualDates(addDay(currentDate, 1), newDate));
    }

    /** Test that a DV Domestic certificate changes the passport date.
     * This test takes a while. */     
    public void testEAC7h() {
        Date currentDate = findPassportDate(true);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = addDay(currentDate, 1);
          to = c2.getNotAfter();
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, false, true);
        ck = new CertsKeyPair(new CardVerifiableCertificate[]{ck.certs[0]}, null);
        assertTrue(verifyCerts(ck));
        Date newDate = findPassportDate(false);
        System.out.println("Current new date is: "+PassportService.SDF.format(newDate));
        assertTrue(checkEqualDates(addDay(currentDate, 1), newDate));
    }



    /**
     * This test puts a new trust point on the passport. If there is one already, no action is taken.
     * If there isn't any we put it on, we check that the date has not changed after the operation,
     * then check that the new trust point is reported. 
     *
     */
    public void t_estEAC8() {
        Date currentPassportDate = findPassportDate(true);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentPassportDate));
        checkNewRootCertificatesExist();
        CVCAFile cvcaFile = service.getCVCAFile();
        assertNotNull(cvcaFile);
        System.out.println("cvca: "+cvcaFile);
        CardVerifiableCertificate origCVCA = readCVCertificateFromFile(testCVCAcert);
        CardVerifiableCertificate newCVCA = readCVCertificateFromFile(newCVCAcert);
        assertNotNull(origCVCA);
        assertNotNull(newCVCA);
        boolean putNewTrustPoint = true;
        try {
          if(cvcaFile.getCAReference().equals(newCVCA.getHolderReference().getName())) {
            putNewTrustPoint = false;
            assertEquals(cvcaFile.getAltCAReference(), origCVCA.getHolderReference().getName());                        
          }else{
            assertNull(cvcaFile.getAltCAReference());
            assertEquals(cvcaFile.getCAReference(), origCVCA.getHolderReference().getName());            
          }
        }catch(Exception e) {
            fail();
        }

        if(putNewTrustPoint) {
          assertTrue(service.doBAC());
          assertTrue(service.doCA());
          traceApdu = true;
          assertTrue(service.doTA(new CardVerifiableCertificate[]{newCVCA}, null));
          traceApdu = false;
          try {
            resetCard();
          }catch(Exception e) {
            fail();
          }
        }
        cvcaFile = service.getCVCAFile();
        assertNotNull(cvcaFile);
        System.out.println("New CVCA file: "+cvcaFile);
        try {
            assertEquals(cvcaFile.getCAReference(), newCVCA.getHolderReference().getName());
            assertEquals(cvcaFile.getAltCAReference(), origCVCA.getHolderReference().getName());
        }catch(Exception e) {
            fail();  
        }
       Date dateAfterUpdate = findPassportDate(true);
       assertTrue(checkEqualDates(currentPassportDate, dateAfterUpdate));
    }
    
    /**
     * Tests the normal EAC behavior after trust point update:
     * sends the original certificates to the and tests reading out DG3.
     * 
     */
    public void testEAC8a() {
        
        // First check (lightly) that the new trust point is already there
        CVCAFile cvcaFile = service.getCVCAFile();
        assertNotNull(cvcaFile);
        assertNotNull(cvcaFile.getAltCAReference());
        
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testISkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertTrue(service.doTA(new CardVerifiableCertificate[] { c1, c2 }, k));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * Tests the normal EAC behavior after trust point update:
     * sends the new certificates to the and tests reading out DG3.
     * 
     */
    public void testEAC8b() {

        // First check (lightly) that the new trust point is already there
        CVCAFile cvcaFile = service.getCVCAFile();
        assertNotNull(cvcaFile);
        assertNotNull(cvcaFile.getAltCAReference());

        CardVerifiableCertificate c1 = readCVCertificateFromFile(newDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(newIScert);
        PrivateKey k = readKeyFromFile(newISkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        service.doBAC();
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
        assertTrue(service.doCA());
        assertTrue(service.doTA(new CardVerifiableCertificate[] { c1, c2 }, k));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
    }

    
    /**
     * Tests the expiry of the old trust point after the date has been set 
     * after the expiry point of the old trust point.
     * 
     * This test is only runnable once (in terms of permanent change to the  
     * passport), the test checks this!
     * 
     * After this test8a and test8b should fail.
     * 
     * The one run on passport sample #090527-03 passed successfully.
     * This passport now has only the new trust point and the current date
     * some time in 2011 (just after the expiry date of the old root CVCA 
     * certificate, which is 2011-05-19.
     */
    public void t_estEAC9() {
        CVCAFile cvcaFile = service.getCVCAFile();
        System.out.println("pre CVCA file: "+cvcaFile);
        assertNotNull(cvcaFile);
        assertNotNull(cvcaFile.getAltCAReference());

        CardVerifiableCertificate c0 = readCVCertificateFromFile(newCVCAcert);
        CardVerifiableCertificate c1 = readCVCertificateFromFile(newDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(newIScert);        
        PrivateKey k = readKeyFromFile(newCVCAkey);
        assertNotNull(c0);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        CardVerifiableCertificate oldRoot = readCVCertificateFromFile(testCVCAcert);
       
        Date from = null; Date to = null;
        try {
          from = addDay(oldRoot.getNotAfter(), 1);
          to = c0.getNotAfter();
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, false, true);
        assertTrue(verifyCerts(ck));
        
        try {  resetCard(); }catch(Exception e) {fail();}
        cvcaFile = service.getCVCAFile();
        System.out.println("new CVCA file: "+cvcaFile);
        assertNotNull(cvcaFile);
        assertNull(cvcaFile.getAltCAReference());
    }

    /**
     * Test the passport with binary search on certificates to find the current 
     * passport date 
     * 
     * The verify flag should be set to true only for tests that may alter the passport 
     * trust points.
     * 
     */
    private Date findPassportDate(boolean verify) {
        CardVerifiableCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c2.getNotBefore();
          to = c2.getNotAfter();
        }catch(Exception e) {
            fail();
        }

        boolean finished = false;
        Date middleDate = null;
        while(!finished) {
            middleDate = new Date((long)(from.getTime() + (long)((long)(to.getTime()-from.getTime())/2)));
            CertsKeyPair ck1 = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, middleDate, true);
            CertsKeyPair ck2 = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, middleDate, to, true);
            
            boolean result1 = verifyCerts(ck1);
            if(!result1) {
                result1 = verifyCerts(ck1);
            }
            boolean result2 = verifyCerts(ck2);
            if(!result2) {
                result2 = verifyCerts(ck2);
            }
//            System.out.println(PassportService.SDF.format(from)+" "+PassportService.SDF.format(middleDate)+" "+PassportService.SDF.format(to));
//            System.out.println("Verified: "+result1+" "+result2);
            assertTrue(result1 || result2);

            if(result1) {
                to = middleDate;
            }else{
                if(!result1 && checkEqualDates(from, middleDate)) {
                    from = addDay(middleDate, 1);
                }else{
                    from = middleDate;
                }
            }
            if(checkEqualDates(from , to)) {
                finished = true;
                middleDate = from;
            }

        }
        // now we need to see if this is correct, if one of the doTAs failed,
        // then this date may be off...
        if(verify) {
            try {
              from = c1.getNotBefore();
              to = addDay(middleDate, -1);
            }catch(Exception e) {
                fail();
            }
            System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
            CertsKeyPair ck = createCertificatesToSearchChangeDate(new CardVerifiableCertificate[] { c1, c2}, k, from, to, true, true);
            ck = new CertsKeyPair(new CardVerifiableCertificate[]{ck.certs[0]}, null);
            assertFalse(verifyCerts(ck));
        }
        return middleDate;
    }
    
    private boolean verifyCerts(CertsKeyPair ck) {
        try { resetCard(); assertTrue(true);
        }catch(CardServiceException cse) { assertTrue(false); }
        service.doBAC();
        assertTrue(service.doCA());                
        return service.doTA(ck.certs, ck.key);
    }

    private boolean checkEqualDates(Date d1, Date d2) {
        String s1 = PassportService.SDF.format(d1);
        String s2 = PassportService.SDF.format(d2);
        return s1.equals(s2);
    }
    
    private Date addDay(Date from, int num) {
        Calendar c = Calendar.getInstance();
        c.setTime(from);
        c.add(Calendar.DAY_OF_MONTH, num);
        return c.getTime();
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
    public static CertsKeyPair createNewCertificate(CardVerifiableCertificate oldCert,
            PrivateKey privateKey, int type) {
        try {
//            CVCertificateBody body = oldCert.getCertificateBody();

            CVCPrincipal caRef = oldCert.getAuthorityReference();
            CVCPrincipal holderRef = oldCert.getHolderReference();
            Date validFrom = oldCert.getNotBefore();
            Date validTo = oldCert.getNotAfter();
            Role role = oldCert.getAuthorizationTemplate().getRole();
            switch (type) {
            case 0: role = Role.DV_D;
            case 1: role = Role.DV_F;
            case 2: role = Role.IS;
            }
            Permission rights = oldCert.getAuthorizationTemplate().getAccessRight();
            PublicKey publicKey = oldCert.getPublicKey();
            String algName = publicKey.getAlgorithm();

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            keyGen.initialize(((ECPrivateKey) privateKey).getParams(), new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            CardVerifiableCertificate newCert = CVCertificateBuilder.createCertificate(
                    keyPair.getPublic(), privateKey, algName, caRef, holderRef,
                    new CVCAuthorizationTemplate(role, rights), validFrom, validTo, "BC");
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
    public static CertsKeyPair createNewCertificates(CardVerifiableCertificate[] oldCerts,
            PrivateKey privateKey, String dvPattern, int chainLengthIS) {
        try {
            List<CardVerifiableCertificate> newCerts = new ArrayList<CardVerifiableCertificate>();
            CVCPrincipal caRef = null;
            CardVerifiableCertificate prevCert = null;
            PublicKey prevKey = null;
            for (CardVerifiableCertificate oldCert : oldCerts) {
                int chainLength = oldCert.getAuthorizationTemplate().getRole() == Role.IS ? chainLengthIS : dvPattern.length();
                for (int i = 0; i < chainLength; i++) {
                    // CVCertificateBody body = oldCert.getCertificateBody();
                    if (caRef == null) {
                        caRef = oldCert.getAuthorityReference();
                    }
                    CVCPrincipal holderRef = oldCert.getHolderReference();
                    if (holderRef.getName().equals(caRef.getName())) {
                        holderRef = increase(holderRef);
                    }
                    Date validFrom = oldCert.getNotBefore();
                    Date validTo = oldCert.getNotAfter();
                    Role role = oldCert.getAuthorizationTemplate().getRole();
                    
                    if (role == Role.DV_D || role == Role.DV_F) {
                        char dvChar = dvPattern.charAt(i);
                        switch(dvChar) {
                        case 'd' :
                            role = Role.DV_D;
                            break;
                        case 'f' :
                            role = Role.DV_F;
                            break;
                        default:
                        }
                    }
                    Permission rights = oldCert.getAuthorizationTemplate().getAccessRight();
                    PublicKey publicKey = oldCert.getPublicKey();
                    String algName = publicKey.getAlgorithm();

                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
                    keyGen.initialize(((ECPrivateKey) privateKey).getParams(),
                            new SecureRandom());
                    KeyPair keyPair = keyGen.generateKeyPair();

                    CardVerifiableCertificate newCert = CVCertificateBuilder
                            .createCertificate(keyPair.getPublic(), privateKey,
                                    algName, caRef, holderRef, new CVCAuthorizationTemplate(role, rights),
                                    validFrom, validTo, "BC");
                    newCerts.add(newCert);
                    privateKey = keyPair.getPrivate();
                    caRef = new CVCPrincipal(holderRef.getCountry(),
                            holderRef.getMnemonic(), holderRef.getSeqNumber());
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


    public static CertsKeyPair createCertificatesToSearchChangeDate(
            CardVerifiableCertificate[] oldCerts,
            PrivateKey privateKey, 
            Date from, Date to, boolean makeForeign) {
        return createCertificatesToSearchChangeDate(oldCerts, privateKey, from, to, makeForeign, false);
    }
    
    public static CertsKeyPair createCertificatesToSearchChangeDate(
            CardVerifiableCertificate[] oldCerts,
            PrivateKey privateKey, 
            Date from, Date to, boolean makeForeign, boolean applyDatesDomestic) {
        try {
            List<CardVerifiableCertificate> newCerts = new ArrayList<CardVerifiableCertificate>();
            CardVerifiableCertificate prevCert = null;
            PublicKey prevKey = null;
            for (CardVerifiableCertificate oldCert : oldCerts) {
//                    CVCertificateBody body = oldCert.getCertificateBody();
                    CVCPrincipal caRef = oldCert.getAuthorityReference();
                    CVCPrincipal holderRef = oldCert.getHolderReference();
                    Role role = oldCert
                            .getAuthorizationTemplate()
                            .getRole();
                    Date validFrom = oldCert.getNotBefore();
                    Date validTo = oldCert.getNotAfter();                    
                    if (role == Role.DV_D && makeForeign) {
                        role = Role.DV_F;
                    }
                    if (role == Role.IS || applyDatesDomestic){
                        validFrom = from;
                        validTo = to;
                    }
                    Permission rights = oldCert.getAuthorizationTemplate()
                            .getAccessRight();
                    PublicKey publicKey = oldCert.getPublicKey();
                    String algName = publicKey.getAlgorithm();

                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(
                            "ECDSA", "BC");
                    keyGen.initialize(((ECPrivateKey) privateKey).getParams(),
                            new SecureRandom());
                    KeyPair keyPair = keyGen.generateKeyPair();

                    CardVerifiableCertificate newCert = CVCertificateBuilder
                            .createCertificate(keyPair.getPublic(), privateKey,
                                    algName, caRef, holderRef,
                                    new CVCAuthorizationTemplate(role, rights),
                                    validFrom, validTo, "BC");
                    newCerts.add(newCert);
                    privateKey = keyPair.getPrivate();
                    // We want to make sure that the chain that we created is correct:
                    if(prevCert != null) {
                        newCert.verify(prevKey, "BC");
                    }
                    prevCert = newCert;
                    prevKey = keyPair.getPublic();
            }
            return new CertsKeyPair(newCerts.toArray(oldCerts),
                    privateKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final File newCVCAcert = new File("certs/certCVCA_new.cvcert");
    private static final File newDVDcert = new File("certs/certDVD_new.cvcert");
    private static final File newIScert = new File("certs/certIS_new.cvcert");
    private static final File newISkey = new File("certs/keyIS_new.der");
    private static final File newCVCAkey = new File("certs/keyCVCA_new.der");
 

    private static void createNewRootCertificateChain(CardVerifiableCertificate oldRootCert, 
            PrivateKey oldRootPrivateKey, long timeShift, CardVerifiableCertificate oldDVDCert,
            CardVerifiableCertificate oldISCert) {
        try {
//        CVCertificateBody body = oldRootCert.getCertificateBody();
        PublicKey publicKey = oldRootCert.getPublicKey();
        Country country = oldRootCert.getAuthorityReference().getCountry();
        String mnemonic = oldRootCert.getAuthorityReference().getMnemonic();
        String sequence = oldRootCert.getAuthorityReference().getSeqNumber();
        CVCPrincipal caRef = new CVCPrincipal(country, mnemonic, sequence);
        sequence = increase(sequence);
        CVCPrincipal holderRef = new CVCPrincipal(country, mnemonic, sequence);
        Date validFrom = oldRootCert.getNotBefore();
        Date validTo = oldRootCert.getNotAfter();
        validFrom.setTime((long)(validFrom.getTime() + timeShift));
        validTo.setTime((long)(validTo.getTime() + timeShift));
        Role role = oldRootCert.getAuthorizationTemplate().getRole();
        Permission rights = oldRootCert.getAuthorizationTemplate().getAccessRight();
        String algName = publicKey.getAlgorithm();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(((ECPrivateKey) oldRootPrivateKey).getParams(), new SecureRandom());
        KeyPair newRootKeyPair = keyGen.generateKeyPair();
        
        CardVerifiableCertificate newRootCertificate = CVCertificateBuilder.createCertificate(
                newRootKeyPair.getPublic(), oldRootPrivateKey,
                algName, caRef, holderRef,
                new CVCAuthorizationTemplate(role, rights), validFrom,
                validTo, "BC");
        System.out.println("newRoot: "+newRootCertificate);
        System.out.println("newRootKey: "+newRootKeyPair.getPrivate());
        
        // do not overwrite!
        if(!newCVCAcert.exists()) {
          writeFile(newCVCAcert, newRootCertificate.getEncoded());
        }
        if(!newCVCAkey.exists()) {
          writeFile(newCVCAkey, newRootKeyPair.getPrivate().getEncoded());
        }
        
        
//        body = oldDVDCert.getCertificateBody();
        publicKey = oldDVDCert.getPublicKey();
        caRef = new CVCPrincipal(country, mnemonic, sequence);
        holderRef = oldDVDCert.getHolderReference();
        validFrom = oldDVDCert.getNotBefore();
        validTo = oldDVDCert.getNotAfter();
        role = oldDVDCert.getAuthorizationTemplate().getRole();
        rights = oldDVDCert.getAuthorizationTemplate().getAccessRight();
        algName = publicKey.getAlgorithm();

        keyGen.initialize(((ECPrivateKey) oldRootPrivateKey).getParams(), new SecureRandom());
        KeyPair newDVDKeyPair = keyGen.generateKeyPair();
        
        CardVerifiableCertificate newDVDCertificate = CVCertificateBuilder.createCertificate(
                newDVDKeyPair.getPublic(), newRootKeyPair.getPrivate(),
                algName, caRef, holderRef,
                new CVCAuthorizationTemplate(role, rights),
                validFrom, validTo, "BC");
        if(!newDVDcert.exists()) {
            writeFile(newDVDcert, newDVDCertificate.getEncoded());
          }

        System.out.println("newDVD: "+newDVDCertificate);
        

//        body = oldISCert.getCertificateBody();
        publicKey = oldISCert.getPublicKey();
        caRef = oldISCert.getAuthorityReference();
        holderRef = oldISCert.getHolderReference();
        validFrom = oldISCert.getNotBefore();
        validTo = oldISCert.getNotAfter();
        role = oldISCert.getAuthorizationTemplate().getRole();
        rights = oldISCert.getAuthorizationTemplate().getAccessRight();
        algName = publicKey.getAlgorithm();

        keyGen.initialize(((ECPrivateKey) oldRootPrivateKey).getParams(), new SecureRandom());
        KeyPair newISKeyPair = keyGen.generateKeyPair();
        
        CardVerifiableCertificate newISCertificate = CVCertificateBuilder.createCertificate(
                newISKeyPair.getPublic(), newDVDKeyPair.getPrivate(),
                algName, caRef, holderRef,
                new CVCAuthorizationTemplate(role, rights),
                validFrom, validTo, "BC");
        if(!newIScert.exists()) {
            writeFile(newIScert, newISCertificate.getEncoded());
          }
        if(!newISkey.exists()) {
            writeFile(newISkey, newISKeyPair.getPrivate().getEncoded());
          }

        System.out.println("newIS: "+newISCertificate);
        System.out.println("newISKey: "+newISKeyPair.getPrivate());
        
        
        
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    
    private void checkNewRootCertificatesExist() {
        if(newCVCAcert.exists() && newDVDcert.exists() && newIScert.exists() && newISkey.exists() &&
                newCVCAkey.exists()) {
            return;
        }
        Date currentPassportDate = findPassportDate(true);
        PrivateKey rootKey = readKeyFromFile(testCVCAkey);
        CardVerifiableCertificate rootCert = readCVCertificateFromFile(testCVCAcert);
        CardVerifiableCertificate dvdCert = readCVCertificateFromFile(testDVDcert);
        CardVerifiableCertificate isCert = readCVCertificateFromFile(testIScert);
        long from = 0;
        long now = 0;
        try {
          from = rootCert.getNotBefore().getTime();
          now = currentPassportDate.getTime();
        }catch(Exception e) {
          fail();  
        }
        createNewRootCertificateChain(rootCert, rootKey, (long)((long)(now - from)/2), dvdCert, isCert);
        assertTrue(newCVCAcert.exists() && newDVDcert.exists() && newIScert.exists() && newISkey.exists() &&
        newCVCAkey.exists());
    }
    
    private static CVCPrincipal increase(CVCPrincipal f) {
        Country c = f.getCountry();
        String m = f.getMnemonic();
        String s = f.getSeqNumber();
        s = increase(s);
        return new CVCPrincipal(c, m, s);
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
        CardVerifiableCertificate[] certs;

        PrivateKey key;

        CertsKeyPair(CardVerifiableCertificate cert, PrivateKey key) {
            this.certs = new CardVerifiableCertificate[] { cert };
            this.key = key;
        }

        CertsKeyPair(CardVerifiableCertificate[] certs, PrivateKey key) {
            this.certs = certs;
            this.key = key;
        }

    }

}
