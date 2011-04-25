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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.FaceInfo;

public class DG2FileTest extends TestCase
{
	private static final String TEST_FILE = "/t:/paspoort/test/0102.bin";
	
	public DG2FileTest(String name) {
		super(name);
	}

	public void testReflexive() {
		try {
			DG2File dg2File = createTestObject();
			testReflexive(dg2File);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	public void testWriteObject() {
		try {
			DG2File dg2File = createTestObject();
			testWriteObject(dg2File, 2);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	public void testReflexive(DG2File dg2File) {
		try {
			byte[] encoded = dg2File.getEncoded();

			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			DG2File copy = new DG2File(in);

			// assertEquals(dg2File, copy);
			// assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/**
	 * Tests if we can decode and then encode using DG2File.writeObject.
	 * 
	 * @param dg2File
	 * @param n number of times
	 */
	public void testWriteObject(DG2File dg2File, int n) {
		try {
			byte[] encoded = null;
			int faceCount = dg2File.getBiometricTemplates().size();
			FaceInfo faceInfo = faceCount == 0 ? null : dg2File.getBiometricTemplates().get(0);
			int width = faceCount == 0 ? -1 : faceInfo.getWidth(), height = faceCount == 0 ? -1 : faceInfo.getHeight();
			
			for (int i = 0; i < n; i++) {
				encoded = dg2File.getEncoded();
				dg2File = new DG2File(new ByteArrayInputStream(encoded));
				// assertEquals(dg2File, copy);
				// assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
			}
			
			// System.out.println("DEBUG: final copy =\n" + Hex.bytesToPrettyString(encoded));
			
			int copyFaceCount = dg2File.getBiometricTemplates().size();
			faceInfo = faceCount == 0 ? null : dg2File.getBiometricTemplates().get(0);
			int copyWidth = faceCount == 0 ? -1 : faceInfo.getWidth(), copyHeight = faceCount == 0 ? -1 : faceInfo.getHeight();
			assertEquals(faceCount, copyFaceCount);
			assertEquals(width, copyWidth);
			assertEquals(height, copyHeight);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public static DG2File createTestObject() throws IOException {
		return new DG2File(new FileInputStream(TEST_FILE));
	}

	public void testFile(InputStream in) {
		testWriteObject(new DG2File(in), 3);
	}
}
