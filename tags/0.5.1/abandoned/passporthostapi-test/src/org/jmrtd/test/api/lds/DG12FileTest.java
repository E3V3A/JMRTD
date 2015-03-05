/*
 *  JMRTD Tests.
 *
 *  Copyright (C) 2009  The JMRTD team
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  $Id: $
 */

package org.jmrtd.test.api.lds;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.DG12File;

public class DG12FileTest extends TestCase {

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

	public DG12FileTest(String name) {
		super(name);
	}

	public void testToString() {
		DG12File dg12File = createTestObject();
		String expectedResult = "DG12File [, 19711019, , , , , , 19711019, ]";
		assertEquals(dg12File.toString(), expectedResult);
	}

	public void testReflexive() {
		testReflexive(createTestObject());
	}

	public void testReflexive(DG12File dg12File) {
		try {
			if (dg12File == null) { fail("Input file is null"); }
			byte[] encoded = dg12File.getEncoded();
			assertNotNull(encoded);
//			System.out.println("DEBUG: encoded =\n" + Hex.bytesToPrettyString(encoded));

			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			DG12File copy = new DG12File(in);

			assertNotNull(copy);
			assertEquals(dg12File, copy);

			byte[] encodedCopy = copy.getEncoded();
			assertNotNull(encodedCopy);
//			System.out.println("DEBUG: encoded =\n" + Hex.bytesToPrettyString(encodedCopy));

			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testEarlySpecSample() {
		byte[] bytes = {

				0x6C, /* L */ 0x40,
				0x5C, /* L = 6, i.e. 3 tags */ 0x06,
				0x5F, 0x19, 0x5F, 0x26, 0x5F, 0x1A,
				0x5F, 0x19, /* L */ 0x18,
				'U', 'N', 'I', 'T', 'E', 'D', ' ', 'S', 'T', 'A', 'T', 'E', 'S', ' ', 'O', 'F', ' ', 'A', 'M', 'E', 'R', 'I', 'C', 'A',
				0x5F, 0x26, /* L */ 0x08,
				'2', '0', '0', '2', '0', '5', '3', '1', /* NOTE: see R7-p1_v2_sIII_0058 */
				0x5F, 0x1A, /* L */ 0x0F,
				'S', 'M', 'I', 'T', 'H', '<', '<', 'B', 'R', 'E', 'N', 'D', 'A', '<', 'P'
		};

		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		try {
			DG12File file = new DG12File(in);
			assertEquals(file.getIssuingAuthority(), "UNITED STATES OF AMERICA");
			assertEquals(file.getDateOfIssue(), SDF.parse("20020531"));
			assertEquals(file.getNamesOfOtherPersons(), Arrays.asList(new String[] { "SMITH<<BRENDA<P" }));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testNewSpecSample() {
		byte[] bytes = {

				0x6C, /* L */ 0x45,
				0x5C, /* L = 6, i.e. 3 tags */ 0x06,
				0x5F, 0x19, 0x5F, 0x26, 0x5F, 0x1A,
				0x5F, 0x19, /* L */ 0x18,
				'U', 'N', 'I', 'T', 'E', 'D', ' ', 'S', 'T', 'A', 'T', 'E', 'S', ' ', 'O', 'F', ' ', 'A', 'M', 'E', 'R', 'I', 'C', 'A',
				0x5F, 0x26, /* L */ 0x08,
				'2', '0', '0', '2', '0', '5', '3', '1', /* NOTE: see R7-p1_v2_sIII_0058 */
				(byte)0xA0, /* L */ 0x15,
					0x02, 0x01, 0x01,
					0x5F, 0x1A, /* L */ 0x0F,
					'S', 'M', 'I', 'T', 'H', '<', '<', 'B', 'R', 'E', 'N', 'D', 'A', '<', 'P'
		};

		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		try {
			DG12File file = new DG12File(in);
			assertEquals(file.getIssuingAuthority(), "UNITED STATES OF AMERICA");
			assertEquals(file.getDateOfIssue(), SDF.parse("20020531"));
			assertEquals(file.getNamesOfOtherPersons(), Arrays.asList(new String[] { "SMITH<<BRENDA<P" }));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	public void testComplex() {
		try {
			DG12File dg12 = createComplexTestObject();
			assertEquals(dg12.getIssuingAuthority(), "UTOPIA");
			assertEquals(SDF.format(dg12.getDateOfIssue()), "19711019");
			assertNotNull(dg12.getNamesOfOtherPersons());
			assert(dg12.getNamesOfOtherPersons().size() > 0);
			
			byte[] encoded = dg12.getEncoded();
			// System.out.println("DEBUG: encoded = \n" + Hex.bytesToPrettyString(encoded));
			DG12File copy = new DG12File(new ByteArrayInputStream(encoded));
			
			assertEquals(copy.getIssuingAuthority(), "UTOPIA");
			assertEquals(SDF.format(copy.getDateOfIssue()), "19711019");
			assertNotNull(copy.getNamesOfOtherPersons());
			assert(dg12.getNamesOfOtherPersons().size() > 0);
			
			assertEquals(dg12, copy);
			assert(Arrays.equals(dg12.getEncoded(), copy.getEncoded()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static DG12File createComplexTestObject() {
		String issuingAuthority = "UTOPIA";
		Calendar cal = Calendar.getInstance();
		cal.set(1971, 10 - 1, 19); // 1971/10/19
		Date dateOfIssue = cal.getTime();
		List<String> namesOfOtherPersons = Arrays.asList(new String[] { "OTHER PERSON 1", "OTHER PERSON 2" } );
		String endorseMentsAndObservations = "";
		String taxOrExitRequirements = "";
		byte[] imageOfFront = null;
		byte[] imageOfRear = null;
		Date dateAndTimeOfPersonalization = cal.getTime();
		String personalizationSystemSerialNumber = "";

		return new DG12File(issuingAuthority, dateOfIssue, namesOfOtherPersons,
				endorseMentsAndObservations, taxOrExitRequirements, imageOfFront,
				imageOfRear,  dateAndTimeOfPersonalization,
				personalizationSystemSerialNumber);
	}

	public static DG12File createTestObject() {
		String issuingAuthority = "";
		Calendar cal = Calendar.getInstance();
		cal.set(1971, 10 - 1, 19);
		Date dateOfIssue = cal.getTime();
		List<String> namesOfOtherPersons = null;
		String endorseMentsAndObservations = "";
		String taxOrExitRequirements = "";
		byte[] imageOfFront = null;
		byte[] imageOfRear = null;
		Date dateAndTimeOfPersonalization = cal.getTime();
		String personalizationSystemSerialNumber = "";

		return new DG12File(issuingAuthority, dateOfIssue, namesOfOtherPersons,
				endorseMentsAndObservations, taxOrExitRequirements, imageOfFront,
				imageOfRear,  dateAndTimeOfPersonalization,
				personalizationSystemSerialNumber);
	}

	public void testFile(InputStream in) {
		try {
			testReflexive(new DG12File(in));
		} catch (IOException ioe) {
			fail(ioe.toString());
		}
	}
}
