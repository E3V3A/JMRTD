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
          service.doBAC("PPNUMMER0", getDate("19560507"), getDate("20100101"));
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
