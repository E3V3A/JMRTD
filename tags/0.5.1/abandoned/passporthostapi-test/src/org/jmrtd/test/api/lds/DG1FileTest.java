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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.MRZInfo;

public class DG1FileTest extends TestCase {

	public DG1FileTest(String name) {
		super(name);
	}

	public void testToString() {
		DG1File dg1File = createTestObject();
		String expectedResult = "DG1File P<NLDMEULENDIJK<<LOES<ALBERTINE<<<<<<<<<<<<<XX00000000NLD7110195F1108280123456782<<<<<<2";
		assertEquals(dg1File.toString(), expectedResult);
	}

	public void testReflexive() {
		testReflexive(createTestObject());
	}

	public void testReflexive(DG1File dg1File) {
		try {
			byte[] encoded = dg1File.getEncoded();
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			DG1File copy = new DG1File(in);
			assertEquals(dg1File, copy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	private static final String SMITH_SAMPLE = "P<ATASMITH<<JOHN<T<<<<<<<<<<<<<<<<<<<<<<<<<<123456789<HMD7406222M10123130121<<<<<<<<<<54";
	private static final String LOES_SAMPLE = "P<NLDMEULENDIJK<<LOES<ALBERTINE<<<<<<<<<<<<<XA00277324NLD7110195F0610010123456782<<<<<08";

	public void testSpecSample() {
		try {
			DG1File file = getSpecSampleObject(SMITH_SAMPLE);
			assertEquals(file.getMRZInfo().toString().replace("\n", "").trim(), SMITH_SAMPLE);

			file = getSpecSampleObject(LOES_SAMPLE);
			assertEquals(file.getMRZInfo().toString().replace("\n", "").trim(), LOES_SAMPLE);			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	public void testLength() {
		Collection<DG1File> dg1s = getTestObjects();
		for (DG1File dg1: dg1s) {
			testLength(dg1);
		}
	}
	
	public void testLength(DG1File dg1) {
		byte[] encoded = dg1.getEncoded();
		assertNotNull(encoded);
		
		int length = dg1.getLength();
		if (length <= 0) {
			System.out.println("DEBUG: O_o: length = " + length);
		}
		assertTrue(length > 0);
		
		assertTrue(length <= encoded.length);
	}

	public Collection<DG1File> getTestObjects() {
		List<DG1File> testObjects = new ArrayList<DG1File>();
		try { testObjects.add(createTestObject()); } catch (Exception e) { e.printStackTrace(); }
		try { testObjects.add(getSpecSampleObject(SMITH_SAMPLE)); } catch (Exception e) { e.printStackTrace(); }
		try { testObjects.add(getSpecSampleObject(LOES_SAMPLE)); } catch (Exception e) { e.printStackTrace(); }
		return testObjects;
	}

	public DG1File getSpecSampleObject(String str) {
		byte[] header = { 0x61, 0x5B, 0x5F, 0x1F, 0x58 };
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(header);
			out.write(str.getBytes("UTF-8"));
			out.flush();
			byte[] bytes = out.toByteArray();
			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
			return new DG1File(in);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static DG1File createTestObject() {
		MRZInfo mrzInfo = MRZInfoTest.createTestObject();
		return new DG1File(mrzInfo);
	}

	public void testFile(InputStream in) {
		try {
			testReflexive(new DG1File(in));
		} catch (Exception e) {
			fail(e.toString());
		}
	}
}
