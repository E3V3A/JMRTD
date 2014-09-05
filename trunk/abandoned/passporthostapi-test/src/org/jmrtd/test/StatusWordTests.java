package org.jmrtd.test;

import junit.framework.TestCase;
import net.sourceforge.scuba.smartcards.ISO7816;

/**
 * Some tests on short and int representations of 2 byte status words.
 * 
 *  Conclusion:	cast shorts to int using "& 0xFFFF", then compare in int domain
 *  or:			cast all ints to short, then compare in short domain
 *
 */
public class StatusWordTests extends TestCase {

	public void testEqualityShorts() {
		int sw = 0x9000;
		short swAsShort = (short)sw;
		
		assertEquals(swAsShort, ISO7816.SW_NO_ERROR);
		assertTrue(swAsShort == ISO7816.SW_NO_ERROR);
	}
	
	/*
	 * The latter two tests fail!
	 */
	public void testEqualityInts() {
		int sw = 0x9000;

		assertEquals(sw, ISO7816.SW_NO_ERROR & 0xFFFF);
		assertTrue(sw == (ISO7816.SW_NO_ERROR & 0xFFFF));

//		assertEquals(sw, ISO7816.SW_NO_ERROR);
//		assertTrue(sw == ISO7816.SW_NO_ERROR);
	}
	
	/**
	 * But these work fine as sign bit is 0.
	 */
	public void testEqualityInts2() {
		int sw = 0x6982;
		short swAsShort = (short)sw;

		assertEquals(swAsShort, ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		assertTrue(swAsShort == ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		
		assertEquals(sw, ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED & 0xFFFF);
		assertTrue(sw == (ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED & 0xFFFF));

		assertEquals(sw, ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		assertTrue(sw == ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
	}

}
