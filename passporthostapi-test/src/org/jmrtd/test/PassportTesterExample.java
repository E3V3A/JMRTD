package org.jmrtd.test;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;

import org.jmrtd.BACKey;
import org.jmrtd.PassportService;

public class PassportTesterExample extends PassportTesterBase {

	public PassportTesterExample(String name) {
		super(name);
	}

	public void test1() {
		traceApdu = true;
		try {
			service.doBAC(new BACKey("XX1234587", getDate("19760803"), getDate("20140507")));
			CardFileInputStream in = service.getInputStream(PassportService.EF_DG1);
			assertNotNull(in);
			in = null;
			resetCard();
			try {
				in = service.getInputStream(PassportService.EF_DG1);
				fail();
			} catch (CardServiceException e) {
				assertNull(in);
			}
		} catch (CardServiceException cse) {
			fail(cse.getMessage());
		}
		assertNotNull(service); // NB try-catch and this explicit fail are not
								// really needed if - in addition to making sure
								// we don't have any test failures - we also
								// make sure we don't have any test errors

	}

}
