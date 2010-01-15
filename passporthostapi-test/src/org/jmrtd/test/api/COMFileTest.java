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

package org.jmrtd.test.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.PassportFile;

public class COMFileTest extends TestCase
{
	public COMFileTest(String name) {
		super(name);
	}
	
	public void testToString() {
		COMFile dg1File = createTestObject();
		String expectedResult = "COMFile LDS 01.07, Unicode 04.00.00, [DG1, DG2, DG15]";
		assertEquals(dg1File.toString(), expectedResult);
	}
	
	public void testReflexive() {
		testReflexive(createTestObject());
	}
	
	private void testReflexive(COMFile comFile) {
		try {
			byte[] encoded = comFile.getEncoded();
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			COMFile copy = new COMFile(in);
			assertEquals(comFile, copy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
		} catch (Exception e) {
			fail(e.toString());
		}
	}
	
	public void testSpecSample() {
		byte[] bytes = { 0x60, /* L */ 0x16,
							0x5F, 0x01, /* L */ 0x04,
								'0', '1', '0', '7',
							0x5F, 0x36, /* L */ 0x06,
								'0', '4', '0', '0', '0', '0',
							0x5C, /* L */ 0x04,
								0x61, 0x75, 0x76, 0x6C };
		
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		try {
			COMFile file = new COMFile(in);
			assertEquals(file.getLDSVersion(), "01.07");
			assertEquals(file.getUnicodeVersion(), "04.00.00");
			assertEquals(file.getTagList().size(), 4);
			assertEquals((int)file.getTagList().get(0), COMFile.EF_DG1_TAG);
			assertEquals((int)file.getTagList().get(1), COMFile.EF_DG2_TAG);
			assertEquals((int)file.getTagList().get(2), COMFile.EF_DG4_TAG);
			assertEquals((int)file.getTagList().get(3), COMFile.EF_DG12_TAG);
		} catch (Exception e) {
			fail(e.toString());
		}
	}
	
	public static COMFile createTestObject() {
		List<Integer> tagList = new ArrayList<Integer>();
		tagList.add(PassportFile.EF_DG1_TAG);
		tagList.add(PassportFile.EF_DG2_TAG);
		tagList.add(PassportFile.EF_DG15_TAG);
		return new COMFile("01", "07", "04", "00", "00", tagList);
	}

	public void testFile(InputStream in) {
		try {
			testReflexive(new COMFile(in));
		} catch (Exception e) {
			fail(e.toString());
		}
	}
}
