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
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceInfo;

public class DG2FileTest extends TestCase {

	private static final String TEST_FILE = "/t:/paspoort/test/0102.bin";

	public DG2FileTest(String name) {
		super(name);
	}

	public void testConstruct() {
		try {
			DG2File dg2 = new DG2File(Arrays.asList(new FaceInfo[] { }));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReflexive() {
		try {
			DG2File dg2File = getTestObject();
			testReflexive(dg2File);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testReflexive(DG2File dg2File) {
		try {
			byte[] encoded = dg2File.getEncoded();
			assertNotNull(encoded);
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			DG2File copy = new DG2File(in);

			assertEquals(dg2File, copy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testWriteObject() {
		try {
			DG2File dg2File = getTestObject();
			testDecodeEncode(dg2File, 2);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	/**
	 * Tests if we can decode and then encode.
	 * 
	 * @param dg2File
	 * @param n number of times
	 */
	public void testDecodeEncode(DG2File dg2File, int n) {
		try {
			byte[] encoded = null;

			List<FaceInfo> records = dg2File.getFaceInfos();
			int faceCount = records.size();
			FaceInfo record = faceCount == 0 ? null : records.get(0);
			List<FaceImageInfo> images = record.getFaceImageInfos();
			int faceImageCount = images.size();
			FaceImageInfo faceImage = faceImageCount == 0 ? null : images.get(0);
			int width = faceImageCount == 0 ? -1 : faceImage.getWidth(), height = faceImageCount == 0 ? -1 : faceImage.getHeight();

			for (int i = 0; i < n; i++) {
				encoded = dg2File.getEncoded();
				dg2File = new DG2File(new ByteArrayInputStream(encoded));
			}

			List<FaceInfo> records1 = dg2File.getFaceInfos();
			int faceCount1 = records1.size();
			FaceInfo record1 = faceCount1 == 0 ? null : records1.get(0);
			List<FaceImageInfo> images1 = record1.getFaceImageInfos();
			int faceImageCount1 = images1.size();
			FaceImageInfo faceImage1 = faceImageCount1 == 0 ? null : images1.get(0);
			int width1 = faceImageCount1 == 0 ? -1 : faceImage1.getWidth(), height1 = faceImageCount1 == 0 ? -1 : faceImage1.getHeight();

			System.out.println("width = " + width);

			System.out.println("width1 = " + width1);

			assertEquals(width, width1);
			assertEquals(height, height1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testElements() {
		try {
			testElements(getTestObject());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testElements(DG2File dg2File) {
		testDecodeEncode(dg2File, 2);

		FaceInfoTest faceInfoTest = new FaceInfoTest("DG2FileTest");
		List<FaceInfo> faceInfos = dg2File.getFaceInfos();
		System.out.println("DEBUG: faceInfos: " + faceInfos.size());
		for (FaceInfo faceInfo: faceInfos) {
			faceInfoTest.testMandatorySBHFields(faceInfo);
			faceInfoTest.testOptionalSBHFields(faceInfo);
			faceInfoTest.testElements(faceInfo);
		}
	}

	public void testCreate() {
		try {
			DG2File dg2 = createTestObject();
			byte[] header = new byte[256];
			System.arraycopy(dg2.getEncoded(), 0, header, 0, header.length);
			System.out.println(Hex.bytesToPrettyString(header));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static DG2File createTestObject() {
		try {
			FaceInfo faceInfo = FaceInfoTest.createTestObject();
			DG2File dg2 = new DG2File(Arrays.asList(new FaceInfo[] { faceInfo }));
			return dg2;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			return null;
		}
	}

	public static DG2File getTestObject() throws IOException {
		return new DG2File(new FileInputStream(TEST_FILE));
	}

	public void testFile(InputStream in) {
		try {
			testDecodeEncode(new DG2File(in), 3);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
