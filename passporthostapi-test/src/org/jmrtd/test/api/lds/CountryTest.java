package org.jmrtd.test.api.lds;

import junit.framework.TestCase;
import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.ISOCountry;

import org.jmrtd.lds.ICAOCountry;

public class CountryTest extends TestCase {

	public void testCountryValues() {
		Country[] values = Country.values();
		assertNotNull(values);
		for (Country country: values) {
			// System.out.println(country);
		}
	}
	
	public void testGermany() {
		Country icaoGermany = ICAOCountry.getInstance("D<<");
		Country isoGermany = ISOCountry.getInstance("DEU");
		assertNotNull(icaoGermany);
		assertEquals(ICAOCountry.DE, icaoGermany);
		assertSame(ICAOCountry.DE, icaoGermany);
		assertEquals(ISOCountry.DE, isoGermany);
		assertSame(ISOCountry.DE, isoGermany);
		assertEquals(isoGermany.toAlpha2Code(), icaoGermany.toAlpha2Code());
	}
	
	public void testNetherlands() {
		assertEquals(ICAOCountry.getInstance("NLD"), ISOCountry.NL);
		assertSame(ICAOCountry.getInstance("NLD"), ISOCountry.NL);
	}
	
	public void testUtopia() {
		Country country = Country.getInstance("UT");
		assertNotNull(country);
		// System.out.println(country);
	}
}
