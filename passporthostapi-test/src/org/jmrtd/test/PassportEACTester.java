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

import net.sourceforge.scuba.smartcards.CardServiceException;

import org.ejbca.cvc.AccessRightEnum;
import org.ejbca.cvc.AlgorithmUtil;
import org.ejbca.cvc.AuthorizationRoleEnum;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.CVCPublicKey;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CVCertificateBody;
import org.ejbca.cvc.CertificateGenerator;
import org.ejbca.cvc.HolderReferenceField;
import org.jmrtd.CVCAFile;
import org.jmrtd.PassportService;

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
        assertFalse(service.doTA(new CVCertificate[] {}, k, "SHA224WITHECDSA"));
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
    }

    /**
     * This test tries to authenticate with the IS certificate immediately
     * (signed by CVCA). That is, there is no DV certificate sent.
     * 
     */
    public void testEAC3() {
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
    }

    /** A variation of the previous test with two IS certificates */
    public void testEAC5b() {
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
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
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
        assertFalse(service.doTA(new CVCertificate[] { c1, c2 }, k));
        c2 = readCVCertificateFromFile(testIScert);
        assertNotNull(c2);
        assertFalse(service.doTA(new CVCertificate[] { c2 }, k));
        assertFalse(service.canReadFile(PassportService.EF_DG3, true));
    }

    /**
     * See previous test. See if we can reattempt the TA process, with the
     * whole certificate chain, without repeating CA. 
     * 
     */
    public void testEAC6a() {
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
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
        assertFalse(service.doTA(new CVCertificate[] { c1, c2 }, k));
        c2 = readCVCertificateFromFile(testIScert);
        assertNotNull(c2);
        assertTrue(service.doTA(new CVCertificate[] { c1, c2 }, k));
        assertTrue(service.canReadFile(PassportService.EF_DG3, true));
    }

    
    /** Test that an expired IS Domestic certificate is not accepted.
     * This test takes a while. */     
    public void testEAC7a() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c2.getCertificateBody().getValidFrom();
          to = addDay(currentDate, -1);
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, from, to, false);
        assertFalse(verifyCerts(ck));
    }

    /** Test that en expired IS Foreign certificate is not accepted.
     * This test takes a while. */     
    public void testEAC7b() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c2.getCertificateBody().getValidFrom();
          to = addDay(currentDate, -1);
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, from, to, true);
        assertFalse(verifyCerts(ck));
    }

    
    /** Test that a IS Foreign certificate does not change the passport date.
     * This test takes a while (even longer than the previous one). */     
    public void testEAC7c() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = addDay(currentDate, 1);
          to = c2.getCertificateBody().getValidTo();
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, from, to, true);
        assertTrue(verifyCerts(ck));
        Date newDate = findPassportDate(false);
        System.out.println("Current new date is: "+PassportService.SDF.format(newDate));
        assertTrue(checkEqualDates(currentDate, newDate));
    }

    /** Test that a IS Domestic certificate changes the passport date.
     * This test takes a while (about same as the previous one). */     
    public void testEAC7d() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = addDay(currentDate, 1);
          to = c2.getCertificateBody().getValidTo();
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, from, to, false);
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
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c1.getCertificateBody().getValidFrom();
          to = addDay(currentDate, -1);
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, from, to, false, true);
        ck = new CertsKeyPair(new CVCertificate[]{ck.certs[0]}, null);
        assertFalse(verifyCerts(ck));
    }

    /** Test that en expired DV Foreign certificate is not accepted.
     * This test takes a while. */     
    public void testEAC7f() {
        Date currentDate = findPassportDate(false);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentDate));
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c1.getCertificateBody().getValidFrom();
          to = addDay(currentDate, -1);
        }catch(Exception e) {
            fail();
        }
        System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
        CertsKeyPair ck = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, from, to, true, true);
        ck = new CertsKeyPair(new CVCertificate[]{ck.certs[0]}, null);
        assertFalse(verifyCerts(ck));
    }


    /**
     * This test puts a new trust point on the passport. If there is one already, no action is taken.
     * If there isn't any we put it on, we check that the date has not changed after the operation,
     * then check that the new trust point is reported. 
     *
     */
    public void testEAC8() {
        Date currentPassportDate = findPassportDate(true);
        System.out.println("Current passport date is: "+PassportService.SDF.format(currentPassportDate));
        checkNewRootCertificatesExist();
        CVCAFile cvcaFile = service.getCVCAFile();
        assertNotNull(cvcaFile);
        System.out.println("cvca: "+cvcaFile);
        CVCertificate origCVCA = readCVCertificateFromFile(testCVCAcert);
        CVCertificate newCVCA = readCVCertificateFromFile(newCVCAcert);
        assertNotNull(origCVCA);
        assertNotNull(newCVCA);
        boolean putNewTrustPoint = true;
        try {
          if(cvcaFile.getCAReference().equals(newCVCA.getCertificateBody().getAuthorityReference().getConcatenated())) {
            putNewTrustPoint = false;
            assertEquals(cvcaFile.getAltCAReference(), origCVCA.getCertificateBody().getAuthorityReference().getConcatenated());                        
          }else{
            assertNull(cvcaFile.getAltCAReference());
            assertEquals(cvcaFile.getCAReference(), origCVCA.getCertificateBody().getAuthorityReference().getConcatenated());            
          }
        }catch(Exception e) {
            fail();
        }

        if(putNewTrustPoint) {
          assertTrue(service.doBAC());
          assertTrue(service.doCA());
          traceApdu = true;
          assertTrue(service.doTA(new CVCertificate[]{newCVCA}, null));
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
            assertEquals(cvcaFile.getCAReference(), newCVCA.getCertificateBody().getAuthorityReference().getConcatenated());
            assertEquals(cvcaFile.getAltCAReference(), origCVCA.getCertificateBody().getAuthorityReference().getConcatenated());
        }catch(Exception e) {
            fail();  
        }
        Date dateAfterUpdate = findPassportDate(true);
        assertTrue(checkEqualDates(currentPassportDate, dateAfterUpdate));
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
        CVCertificate c1 = readCVCertificateFromFile(testDVDcert);
        CVCertificate c2 = readCVCertificateFromFile(testIScert);
        PrivateKey k = readKeyFromFile(testCVCAkey);
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(k);
        Date from = null; Date to = null;
        try {
          from = c2.getCertificateBody().getValidFrom();
          to = c2.getCertificateBody().getValidTo();
        }catch(Exception e) {
            fail();
        }

        boolean finished = false;
        Date middleDate = null;
        while(!finished) {
            middleDate = new Date((long)(from.getTime() + (long)((long)(to.getTime()-from.getTime())/2)));
            CertsKeyPair ck1 = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, from, middleDate, true);
            CertsKeyPair ck2 = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, middleDate, to, true);
            
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
              from = c1.getCertificateBody().getValidFrom();
              to = addDay(middleDate, -1);
            }catch(Exception e) {
                fail();
            }
            System.out.println("Test dates: "+PassportService.SDF.format(from)+ " "+PassportService.SDF.format(to));
            CertsKeyPair ck = createCertificatesToSearchChangeDate(new CVCertificate[] { c1, c2}, k, from, to, true, true);
            ck = new CertsKeyPair(new CVCertificate[]{ck.certs[0]}, null);
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


    public static CertsKeyPair createCertificatesToSearchChangeDate(
            CVCertificate[] oldCerts,
            PrivateKey privateKey, 
            Date from, Date to, boolean makeForeign) {
        return createCertificatesToSearchChangeDate(oldCerts, privateKey, from, to, makeForeign, false);
    }
    
    public static CertsKeyPair createCertificatesToSearchChangeDate(
            CVCertificate[] oldCerts,
            PrivateKey privateKey, 
            Date from, Date to, boolean makeForeign, boolean applyDatesDomestic) {
        try {
            List<CVCertificate> newCerts = new ArrayList<CVCertificate>();
            CVCertificate prevCert = null;
            PublicKey prevKey = null;
            for (CVCertificate oldCert : oldCerts) {
                    CVCertificateBody body = oldCert.getCertificateBody();
                    CAReferenceField caRef = body.getAuthorityReference();
                    HolderReferenceField holderRef = body.getHolderReference();
                    AuthorizationRoleEnum role = body
                            .getAuthorizationTemplate().getAuthorizationField()
                            .getRole();
                    Date validFrom = body.getValidFrom();
                    Date validTo = body.getValidTo();                    
                    if (role == AuthorizationRoleEnum.DV_D && makeForeign) {
                        role = AuthorizationRoleEnum.DV_F;
                    }
                    if (role == AuthorizationRoleEnum.IS || applyDatesDomestic){
                        validFrom = from;
                        validTo = to;
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
 

    private static void createNewRootCertificateChain(CVCertificate oldRootCert, 
            PrivateKey oldRootPrivateKey, long timeShift, CVCertificate oldDVDCert,
            CVCertificate oldISCert) {
        try {
        CVCertificateBody body = oldRootCert.getCertificateBody();
        CVCPublicKey publicKey = body.getPublicKey();
        String country = body.getAuthorityReference().getCountry();
        String mnemonic = body.getAuthorityReference().getMnemonic();
        String sequence = body.getAuthorityReference().getSequence();
        CAReferenceField caRef = new CAReferenceField(country,mnemonic,sequence);
        sequence = increase(sequence);
        HolderReferenceField holderRef = new HolderReferenceField(country,mnemonic,sequence);
        Date validFrom = body.getValidFrom();
        Date validTo = body.getValidTo();
        validFrom.setTime((long)(validFrom.getTime() + timeShift));
        validTo.setTime((long)(validTo.getTime() + timeShift));
        AuthorizationRoleEnum role = body.getAuthorizationTemplate().getAuthorizationField().getRole();
        AccessRightEnum rights = body.getAuthorizationTemplate().getAuthorizationField().getAccessRight();
        String algName = AlgorithmUtil.getAlgorithmName(publicKey.getObjectIdentifier());

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(((ECPrivateKey) oldRootPrivateKey).getParams(), new SecureRandom());
        KeyPair newRootKeyPair = keyGen.generateKeyPair();
        
        CVCertificate newRootCertificate = CertificateGenerator.createCertificate(
                newRootKeyPair.getPublic(), oldRootPrivateKey,
                algName, caRef, holderRef, role, rights, validFrom,
                validTo, "BC");
        System.out.println("newRoot: "+newRootCertificate);
        System.out.println("newRootKey: "+newRootKeyPair.getPrivate());
        
        // do not overwrite!
        if(!newCVCAcert.exists()) {
          writeFile(newCVCAcert, newRootCertificate.getDEREncoded());
        }
        if(!newCVCAkey.exists()) {
          writeFile(newCVCAkey, newRootKeyPair.getPrivate().getEncoded());
        }
        
        
        body = oldDVDCert.getCertificateBody();
        publicKey = body.getPublicKey();
        caRef = new CAReferenceField(country, mnemonic, sequence);
        holderRef = body.getHolderReference();
        validFrom = body.getValidFrom();
        validTo = body.getValidTo();
        role = body.getAuthorizationTemplate().getAuthorizationField().getRole();
        rights = body.getAuthorizationTemplate().getAuthorizationField().getAccessRight();
        algName = AlgorithmUtil.getAlgorithmName(publicKey.getObjectIdentifier());

        keyGen.initialize(((ECPrivateKey) oldRootPrivateKey).getParams(), new SecureRandom());
        KeyPair newDVDKeyPair = keyGen.generateKeyPair();
        
        CVCertificate newDVDCertificate = CertificateGenerator.createCertificate(
                newDVDKeyPair.getPublic(), newRootKeyPair.getPrivate(),
                algName, caRef, holderRef, role, rights, validFrom,
                validTo, "BC");
        if(!newDVDcert.exists()) {
            writeFile(newDVDcert, newDVDCertificate.getDEREncoded());
          }

        System.out.println("newDVD: "+newDVDCertificate);
        

        body = oldISCert.getCertificateBody();
        publicKey = body.getPublicKey();
        caRef = body.getAuthorityReference();
        holderRef = body.getHolderReference();
        validFrom = body.getValidFrom();
        validTo = body.getValidTo();
        role = body.getAuthorizationTemplate().getAuthorizationField().getRole();
        rights = body.getAuthorizationTemplate().getAuthorizationField().getAccessRight();
        algName = AlgorithmUtil.getAlgorithmName(publicKey.getObjectIdentifier());

        keyGen.initialize(((ECPrivateKey) oldRootPrivateKey).getParams(), new SecureRandom());
        KeyPair newISKeyPair = keyGen.generateKeyPair();
        
        CVCertificate newISCertificate = CertificateGenerator.createCertificate(
                newISKeyPair.getPublic(), newDVDKeyPair.getPrivate(),
                algName, caRef, holderRef, role, rights, validFrom,
                validTo, "BC");
        if(!newIScert.exists()) {
            writeFile(newIScert, newISCertificate.getDEREncoded());
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
        CVCertificate rootCert = readCVCertificateFromFile(testCVCAcert);
        CVCertificate dvdCert = readCVCertificateFromFile(testDVDcert);
        CVCertificate isCert = readCVCertificateFromFile(testIScert);
        long from = 0;
        long now = 0;
        try {
          from = rootCert.getCertificateBody().getValidFrom().getTime();
          now = currentPassportDate.getTime();
        }catch(Exception e) {
          fail();  
        }
        createNewRootCertificateChain(rootCert, rootKey, (long)((long)(now - from)/2), dvdCert, isCert);
        assertTrue(newCVCAcert.exists() && newDVDcert.exists() && newIScert.exists() && newISkey.exists() &&
        newCVCAkey.exists());
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
