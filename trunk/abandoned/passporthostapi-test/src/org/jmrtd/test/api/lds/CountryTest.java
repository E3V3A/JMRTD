package org.jmrtd.test.api.lds;

import junit.framework.TestCase;
import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.ISOCountry;
import net.sourceforge.scuba.data.UnicodeCountry;

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
		Country isoGermany = Country.getInstance("DEU");
		assertNotNull(icaoGermany);
		assertTrue(ISOCountry.DE == isoGermany || UnicodeCountry.DE == isoGermany);
		assertTrue(ISOCountry.DE.equals(isoGermany) || UnicodeCountry.DE.equals(isoGermany));
		assertEquals(ICAOCountry.DE, icaoGermany);
		assertSame(ICAOCountry.DE, icaoGermany);
		assertEquals(isoGermany.toAlpha2Code(), icaoGermany.toAlpha2Code());
	}

	public void testTaiwan() {
		Country icaoCountry = ICAOCountry.getInstance("TWN");
		Country unicodeCountry = Country.getInstance("TWN");
		assertEquals(icaoCountry, unicodeCountry);
		assertFalse(icaoCountry.getName().toLowerCase().contains("china"));
	}
	
	public void testNetherlands() {
		assertTrue(Country.getInstance("NLD") == ISOCountry.NL || Country.getInstance("NLD") == UnicodeCountry.NL);
		assertTrue(ISOCountry.NL.equals(Country.getInstance("NLD")) || UnicodeCountry.NL.equals(Country.getInstance("NLD")));
		assertEquals(ISOCountry.NL.getName(), UnicodeCountry.NL.getName());
	}

	public void testUtopia() {
		Country country = Country.getInstance("UT");
		assertNotNull(country);
		// System.out.println(country);
	}
}
