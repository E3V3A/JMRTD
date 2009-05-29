package org.jmrtd.test;

import org.jmrtd.PassportService;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;

public class PassportTesterExample extends PassportTesterBase {

    public PassportTesterExample(String name) {
        super(name);
    }

    public void test1() {
        traceApdu = true;
        try {
          resetCard();
          service.doBAC("XX1234587", getDate("19760803"), getDate("20140507"));
          CardFileInputStream in = service.readFile(PassportService.EF_DG1);
          assertNotNull(in);
          in = null;
          resetCard();
          try {
            in = service.readFile(PassportService.EF_DG1);
            fail();
          }catch(CardServiceException e){
            assertNull(in);
          }
        }catch(CardServiceException cse) {
            fail(cse.getMessage());
        }
        assertNotNull(service);
    }

}
