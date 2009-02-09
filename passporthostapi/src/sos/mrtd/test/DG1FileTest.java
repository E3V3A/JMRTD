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

import junit.framework.TestCase;
import sos.mrtd.DG1File;
import sos.mrtd.MRZInfo;
import sos.util.Hex;

public class DG1FileTest extends TestCase
{
	public DG1FileTest(String name) {
		super(name);
	}
	
	public void testToString() {
		DG1File dg1File = createTestObject();
		String expectedResult = "DG1File P<NLDMEULENDIJK<<LOES<ALBERTINE<<<<<<<<<<<<<XX00000000NLD7110195F1108280123456782<<<<<02";
		assertEquals(dg1File.toString(), expectedResult);
	}
	
	public void testReflexive() {
		try {
			DG1File dg1File = createTestObject();
			byte[] encoded = dg1File.getEncoded();
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			DG1File copy = new DG1File(in);
			assertEquals(dg1File, copy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
		} catch (Exception e) {
			fail(e.toString());
		}
	}
	
	public static DG1File createTestObject() {
		MRZInfo mrzInfo = MRZInfoTest.createTestObject();
		return new DG1File(mrzInfo);
	}
}
