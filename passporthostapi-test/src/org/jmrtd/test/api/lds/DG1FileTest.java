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

	public void testSpecSample() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] smithHeader = { 0x61, 0x5B, 0x5F, 0x1F, 0x58 };
			String smithInfo = "P<ATASMITH<<JOHN<T<<<<<<<<<<<<<<<<<<<<<<<<<<123456789<HMD7406222M10123130121<<<<<<<<<<54";
			out.write(smithHeader);
			out.write(smithInfo.getBytes("UTF-8"));
			out.flush();
			byte[] bytes = out.toByteArray();
			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
			DG1File file = new DG1File(in);
			assertEquals(file.getMRZInfo().toString().replace("\n", "").trim(), smithInfo);
			
			out = new ByteArrayOutputStream();
			byte[] loesHeader = { 0x61, 0x5B, 0x5F, 0x1F, 0x58 };
			String loesInfo = "P<NLDMEULENDIJK<<LOES<ALBERTINE<<<<<<<<<<<<<XA00277324NLD7110195F0610010123456782<<<<<08";
			out.write(loesHeader);
			out.write(loesInfo.getBytes("UTF-8"));
			out.flush();
			bytes = out.toByteArray();
			in = new ByteArrayInputStream(bytes);
			file = new DG1File(in);
			assertEquals(file.getMRZInfo().toString().replace("\n", "").trim(), loesInfo);			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
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
