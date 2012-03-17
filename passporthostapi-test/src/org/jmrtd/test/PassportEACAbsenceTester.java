package org.jmrtd.test;

import java.io.IOException;
import java.text.ParseException;
import java.util.Set;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.PassportService;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.SODFile;

/**
 * This class is supposed to test the absence of EAC implementation/features on
 * the passport.
 * 
 * @author woj
 * 
 */
public class PassportEACAbsenceTester extends PassportTesterBase {

    public PassportEACAbsenceTester(String name) {
        super(name);
    }

    /** Setup for this test. */
    public void setUp() throws CardServiceException, ParseException {
        resetCard();
        service.setMRZ("IZF0D4B51", "391109", "140406");
    }

    /**
     * The first part is to check that EAC specific files are not selectable.
     * This includes CVCA file (trust points), and DG14 (Chip Authentication
     * public key).
     * 
     */
    public void test1() {
        assertTrue(service.doBAC());
        assertFalse(service.canSelectFile(PassportService.EF_CVCA));
        assertFalse(service.canSelectFile(PassportService.EF_DG14));
    }

    /**
     * Test that EAC related data groups are not listed in EF.COM. Note that
     * CVCA file is never listed in EF.COM. It has either the default file id
     * (=PassportService.EF_CVCA), or it is listed in DG14. So here we check
     * only for the presence of DG14 in EF.COM.
     */
    public void test2() {
        assertTrue(service.doBAC());
        short fid = PassportService.EF_COM;
        COMFile com = null;
        try {
            service.sendSelectFile(service.getWrapper(), fid);
            CardFileInputStream in = service.getInputStream(fid);
            com = new COMFile(in);
        } catch (CardServiceException cse) {
            fail();
        } catch (IOException ioe) {
            fail();
        }
        int[] tags = com.getTagList();
        for (int tag : tags) {
            assertTrue(tag != LDSFile.EF_DG14_TAG);
        }
    }

    /**
     * Test that EAC related data groups are not mentioned in EF.SOD (the
     * datagroup hashes table). Note that CVCA file is not listed in EF.SOD. It
     * has either the default file id (=PassportService.EF_CVCA), or it is
     * listed in DG14. So here we check only for the presence of DG14 in EF.SOD
     * data group hash table.
     */
    public void test3() {
        assertTrue(service.doBAC());
        short fid = PassportService.EF_SOD;
        SODFile sod = null;
        try {
            service.sendSelectFile(service.getWrapper(), fid);
            CardFileInputStream in = service.getInputStream(fid);
            sod = new SODFile(in);
        } catch (CardServiceException cse) {
            fail();
        } catch (IOException ioe) {
            fail();
        }
        Set<Integer> dgs = sod.getDataGroupHashes().keySet();
        for (int dg : dgs) {
            assertTrue(dg != 14);
        }
    }

    /**
     * Test that none of the EAC related INS bytes are not supported. Since this
     * fails (for READ_BINART_2 and MSE), we split this test into three parts to
     * see what fails how.
     * 
     * Comment on READ_BINARY_2: the id card with the EAC cut out fully supports
     * READ_BINARY_2 (tested by running the host application with READ_BINARY_2
     * only). This by itself does not offer any EAC functionality, but it is (a)
     * not necessary (there won't be files on this card larger than 32K), (b)
     * leaks the information that this card implementation is probably based on
     * EAC supported implementation. Not sure how big of an information this is,
     * but it is there. The last comment also goes for MSE instruction.
     */
    public void test4a() {
        assertTrue(service.doBAC());
        byte ins = ISO7816.INS_READ_BINARY2;
        short sw = 0;
        try {
            sw = (short) service.sendAnyInstruction(ins, true);
            System.out.println("INS: " + Hex.byteToHexString(ins) + ", SW: "
                    + Hex.shortToHexString(sw));
            assertTrue(sw == ISO7816.SW_INS_NOT_SUPPORTED);
        } catch (CardServiceException cse) {
            fail();
        }
    }

    public void test4b() {
        assertTrue(service.doBAC());
        byte ins = ISO7816.INS_MSE;
        short sw = 0;
        try {
            sw = (short) service.sendAnyInstruction(ins, true);
            System.out.println("INS: " + Hex.byteToHexString(ins) + ", SW: "
                    + Hex.shortToHexString(sw));
            assertTrue(sw == ISO7816.SW_INS_NOT_SUPPORTED);
        } catch (CardServiceException cse) {
            fail();
        }
    }

    public void test4c() {
        assertTrue(service.doBAC());
        byte ins = ISO7816.INS_PSO;
        short sw = 0;
        try {
            sw = (short) service.sendAnyInstruction(ins, true);
            System.out.println("INS: " + Hex.byteToHexString(ins) + ", SW: "
                    + Hex.shortToHexString(sw));
            assertTrue(sw == ISO7816.SW_INS_NOT_SUPPORTED);
        } catch (CardServiceException cse) {
            fail();
        }
    }

    /** Do the same as above, but without BAC. */
    public void test5a() {
        byte ins = ISO7816.INS_READ_BINARY2;
        short sw = 0;
        try {
            sw = (short) service.sendAnyInstruction(ins, false);
            System.out.println("INS: " + Hex.byteToHexString(ins) + ", SW: "
                    + Hex.shortToHexString(sw));
            assertTrue(sw == ISO7816.SW_INS_NOT_SUPPORTED);
        } catch (CardServiceException cse) {
            fail();
        }
    }

    public void test5b() {
        byte ins = ISO7816.INS_MSE;
        short sw = 0;
        try {
            sw = (short) service.sendAnyInstruction(ins, false);
            System.out.println("INS: " + Hex.byteToHexString(ins) + ", SW: "
                    + Hex.shortToHexString(sw));
            assertTrue(sw == ISO7816.SW_INS_NOT_SUPPORTED);
        } catch (CardServiceException cse) {
            fail();
        }
    }

    public void test5c() {
        byte ins = ISO7816.INS_PSO;
        short sw = 0;
        try {
            sw = (short) service.sendAnyInstruction(ins, false);
            System.out.println("INS: " + Hex.byteToHexString(ins) + ", SW: "
                    + Hex.shortToHexString(sw));
            assertTrue(sw == ISO7816.SW_INS_NOT_SUPPORTED);
        } catch (CardServiceException cse) {
            fail();
        }
    }

}
