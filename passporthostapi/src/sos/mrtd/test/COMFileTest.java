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

package sos.mrtd.test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import sos.mrtd.COMFile;
import sos.mrtd.PassportFile;
import sos.util.Hex;

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
		try {
			COMFile comFile = createTestObject();
			byte[] encoded = comFile.getEncoded();
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			COMFile copy = new COMFile(in);
			assertEquals(comFile, copy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
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
}
