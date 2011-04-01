package org.jmrtd.test.api.lds;

import net.sourceforge.scuba.data.Country;
import junit.framework.TestCase;

public class CountryTest extends TestCase {

	public void testCountryValues() {
		Country[] values = Country.values();
		for (Country country: values) {
			System.out.println(country);
		}
	}
	
	public void testUtopia() {
		System.out.println(Country.getInstance("UT"));
	}
}
