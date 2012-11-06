/*
 *  JMRTD Tests.
 *
 *  Copyright (C) 2010  The JMRTD team
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

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.DG3File;
import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;

public class DG3FileTest extends TestCase {

	private static final //			String testFile = "/home/martijno/paspoort/woj-dg3-top-secret-0103.bin";
	String TEST_FILE = "t:/paspoort/test/woj-dg3-top-secret-0103.bin";

	public DG3FileTest(String name) {
		super(name);
	}

	public void testConstruct() {
		try {
			DG3File dg3 = new DG3File(Arrays.asList(new FingerInfo[] { }));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testFile() {
		try {
			DG3File dg3 = getTestObject();
			List<FingerInfo> recordInfos = dg3.getFingerInfos();
			int recordCount = recordInfos.size();
			int recordNumber = 1;
			System.out.println("DEBUG: Number of finger records = " + recordCount);
			for (FingerInfo record: recordInfos) {
				List<FingerImageInfo> imageInfos = record.getFingerImageInfos();
				int imageInfoCount = imageInfos.size();
				System.out.println("DEBUG: Number of images in record " + recordNumber + " is " + imageInfoCount);
				int imageInfoNumber = 1;
				for (FingerImageInfo imageInfo: imageInfos) {
					RenderedImage image = ImageUtil.read(imageInfo.getImageInputStream(), imageInfo.getImageLength(), imageInfo.getMimeType());
					System.out.println("DEBUG: fingerprint " + imageInfoNumber + "/" + imageInfoCount + " in record " + recordNumber + "/" + recordCount + " has " + image.getWidth() + " x " + image.getHeight());
				}
				recordNumber ++;
			}
		} catch (AccessControlException ace) {
			System.out.println("DEBUG: *************** could not get access to DG3 *********");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testDecodeEncode() {
		try {
			FileInputStream in = new FileInputStream(TEST_FILE);
			testDecodeEncode(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testDecodeEncode(InputStream in) {
		try {
			DG3File dg3 = new DG3File(in);

			byte[] encoded = dg3.getEncoded();

			FileOutputStream origOut = new FileOutputStream("t:/dg3orig.bin");			
			origOut.write(encoded);
			origOut.flush();
			origOut.close();

			assertNotNull(encoded);

			DG3File copy = new DG3File(new ByteArrayInputStream(encoded));

			byte[] encodedCopy = copy.getEncoded();

			FileOutputStream copyOut = new FileOutputStream("t:/dg3copy.bin");			
			copyOut.write(encodedCopy);
			copyOut.flush();
			copyOut.close();


			assertNotNull(encodedCopy);
			List<FingerInfo> fingerInfos = dg3.getFingerInfos();
			int fingerInfoCount = fingerInfos.size();

			List<FingerInfo> fingerInfos1 = copy.getFingerInfos();
			int fingerInfoCount1 = fingerInfos1.size();

			assertEquals(fingerInfoCount, fingerInfoCount1);

			int fingerInfoIndex = 0;
			for (FingerInfo fingerInfo: fingerInfos) {
				List<FingerImageInfo> fingerImageInfos = fingerInfo.getFingerImageInfos();
				int fingerImageInfoCount = fingerImageInfos.size();
				FingerInfo fingerInfo1 = fingerInfos1.get(fingerInfoIndex);
				List<FingerImageInfo> fingerImageInfos1 = fingerInfo1.getFingerImageInfos();
				int fingerImageInfoCount1 = fingerImageInfos1.size();
				assertEquals(fingerImageInfoCount, fingerImageInfoCount1);
				int fingerImageInfoIndex = 0;
				for (FingerImageInfo fingerImageInfo: fingerImageInfos) {
					FingerImageInfo fingerImageInfo1 = fingerImageInfos1.get(fingerImageInfoIndex);
					RenderedImage image = ImageUtil.read(fingerImageInfo.getImageInputStream(), fingerImageInfo.getImageLength(), fingerImageInfo.getMimeType());
					RenderedImage image1 = ImageUtil.read(fingerImageInfo1.getImageInputStream(), fingerImageInfo1.getImageLength(), fingerImageInfo1.getMimeType());
					assertEquals(image.getHeight(), image1.getHeight());
					assertEquals(image.getWidth(), image1.getWidth());
					fingerImageInfoIndex ++;
				}
				fingerInfoIndex ++;
			}
		} catch (AccessControlException ace) {
			System.out.println("DEBUG: could not access DG3, ignoring this DG3 file");
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testElements() {
		testElements(getTestObject());
	}
	
	public void testElements(DG3File dg3File) {
		FingerInfoTest fingerInfoTest = new FingerInfoTest("DG3FileTest#testElements");
		List<FingerInfo> records = dg3File.getFingerInfos();
		for (FingerInfo fingerInfo: records) {
			fingerInfoTest.testEncodeDecode(fingerInfo);
			fingerInfoTest.testFieldsReasonable(fingerInfo);
			fingerInfoTest.testFieldsSameAfterReconstruct(fingerInfo);
			fingerInfoTest.testReflexiveReconstruct(fingerInfo);
			fingerInfoTest.testMandatorySBHFields(fingerInfo);
			fingerInfoTest.testOptionalSBHFields(fingerInfo);
			fingerInfoTest.testBiometricSubType(fingerInfo);
			fingerInfoTest.testElements(fingerInfo);
		}
	}
	
	public void testFile(InputStream in) {
		try {
			testDecodeEncode(in);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	public void testEncodeDecode() {
		try {
		DG3File dg3 = getTestObject();
		byte[] dg3Bytes = dg3.getEncoded();
		assertNotNull(dg3Bytes);
		
		DG3File copy = new DG3File(new ByteArrayInputStream(dg3Bytes));
		byte[] copyBytes = copy.getEncoded();
		assertNotNull(copyBytes);
		
		assertTrue(Arrays.equals(dg3Bytes, copyBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testZeroInstanceTestObjectNotEquals() {
		try {
		DG3File dg3 = new DG3File(new LinkedList<FingerInfo>());
		byte[] dg3Bytes = dg3.getEncoded();
		assertNotNull(dg3Bytes);
		
		DG3File anotherDG3 = new DG3File(new LinkedList<FingerInfo>());
		byte[] anotherDG3Bytes = anotherDG3.getEncoded();
		assertNotNull(anotherDG3Bytes);
		
		assertFalse(Arrays.equals(dg3Bytes, anotherDG3Bytes));
		
		DG3File copy = new DG3File(new ByteArrayInputStream(dg3Bytes));
		byte[] copyBytes = copy.getEncoded();
		assertNotNull(copyBytes);
		
		assertFalse(Arrays.equals(dg3Bytes, copyBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testCreate() {
		DG3File dg3 = createTestObject();
		byte[] header = new byte[256];
		System.arraycopy(dg3.getEncoded(), 0, header, 0, header.length);
		System.out.println(Hex.bytesToPrettyString(header));
	}
	
	private DG3File createTestObject() {
		try {
			FingerInfo fingerInfo1 = FingerInfoTest.createSingleRightIndexFingerTestObject();
//			FingerInfo fingerInfo2 = FingerInfoTest.createTestObject();
			List<FingerInfo> fingerInfos = Arrays.asList(new FingerInfo[] { fingerInfo1, /* fingerInfo2 */ });
			DG3File dg3 = new DG3File(fingerInfos);
			return dg3;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			return null;
		}
	}
	
	private DG3File getTestObject() {
		try {
			FileInputStream in = new FileInputStream(TEST_FILE);
			DG3File dg3 = new DG3File(in);
			return dg3;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
