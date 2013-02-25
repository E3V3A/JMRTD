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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
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
					int length = imageInfo.getImageLength();
					byte[] bytes = new byte[length];
					DataInputStream dataIn = new DataInputStream(imageInfo.getImageInputStream());
					dataIn.readFully(bytes);

					RenderedImage image = ImageUtil.read(new ByteArrayInputStream(bytes), imageInfo.getImageLength(), imageInfo.getMimeType());

					//					RenderedImage image = ImageUtil.read(imageInfo.getImageInputStream(), imageInfo.getImageLength(), imageInfo.getMimeType());
					//					System.out.println("DEBUG: fingerprint " + imageInfoNumber + "/" + imageInfoCount + " in record " + recordNumber + "/" + recordCount + " has " + image.getWidth() + " x " + image.getHeight());
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

	public void testImageBytes() {
		try {
			DG3File dg3 = getTestObject();
			FingerImageInfo i1 = dg3.getFingerInfos().get(0).getFingerImageInfos().get(0);
			int l1 = i1.getImageLength();
			byte[] b1 = new byte[l1];
			(new DataInputStream(i1.getImageInputStream())).readFully(b1);
			FingerImageInfo i2 = dg3.getFingerInfos().get(1).getFingerImageInfos().get(0);
			int l2 = i2.getImageLength();
			byte[] b2 = new byte[l2];
			(new DataInputStream(i2.getImageInputStream())).readFully(b2);

			RenderedImage image = ImageUtil.read(new ByteArrayInputStream(b2), l2, i2.getMimeType());

			DataOutputStream out1 = new DataOutputStream(new FileOutputStream("t:/img1.wsq"));
			out1.write(b1);
			out1.flush();
			out1.close();

			DataOutputStream out2 = new DataOutputStream(new FileOutputStream("t:/img2.wsq"));
			out2.write(b2);
			out2.flush();
			out2.close();


			System.out.println("DEBUG: " + Hex.bytesToPrettyString(b1));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public void testImageBytes0() {
		try {
			File testFile = new File(TEST_FILE);
			byte[] bytes = new byte[(int)testFile.length()];
			DataInputStream dataIn = new DataInputStream(new FileInputStream(testFile));
			dataIn.readFully(bytes);
			dataIn.close();

			DG3File dg3 = new DG3File(new ByteArrayInputStream(bytes));
			List<FingerInfo> fingerInfos = dg3.getFingerInfos();
			for (FingerInfo fingerInfo: fingerInfos) {
				List<FingerImageInfo> fingerImageInfos = fingerInfo.getFingerImageInfos();
				for (FingerImageInfo fingerImageInfo: fingerImageInfos) {
					DataInputStream dataInputStream = new DataInputStream(fingerImageInfo.getImageInputStream());
					byte[] imageBytes = new byte[64]; // FIXME: first check 64 < fingerImageInfo.getImageLength()
					dataInputStream.readFully(imageBytes);
					System.out.println("DEBUG:\n" + Hex.bytesToPrettyString(imageBytes));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testImageBytes2() {
		try {
			/* Fetch the contents of the binary file. */
			File testFile = new File(TEST_FILE);
			byte[] bytes = new byte[(int)testFile.length()];
			DataInputStream dataIn = new DataInputStream(new FileInputStream(testFile));
			dataIn.readFully(bytes);
			dataIn.close();

			/* Test with byte array input stream as carrier. */

			DG3File dg3 = new DG3File(new ByteArrayInputStream(bytes));
			//			DG3File dg3 = new DG3File(new FileInputStream(testFile));

			List<FingerInfo> recordInfos = dg3.getFingerInfos();
			assertEquals(recordInfos.size(), 2);
			FingerInfo record = recordInfos.get(1);
			List<FingerImageInfo> imageInfos = record.getFingerImageInfos();
			assertEquals(imageInfos.size(), 1);
			FingerImageInfo imageInfo = imageInfos.get(0);
			int imgLength = imageInfo.getImageLength();
			assertEquals(imgLength, 17583);

			byte[] imgBytes = new byte[imgLength];


			DataInputStream imgDataIn = new DataInputStream(imageInfo.getImageInputStream());
			imgDataIn.readFully(imgBytes);
			assertEquals("FFA0FFA8007A4E4953545F434F4D20390A5049585F5749445448203330380A5049585F484549474854203532380A5049585F444550544820380A505049203530", Hex.bytesToHexString(imgBytes, 0, 64));

			System.out.println("DEBUG:\n" + Hex.bytesToHexString(imgBytes, 0, 64));


			RenderedImage image = ImageUtil.read(new ByteArrayInputStream(imgBytes), imageInfo.getImageLength(), imageInfo.getMimeType());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public void testDecodeSecondImage() {
		try {
			/* Fetch the contents of the binary file. */
			File testFile = new File(TEST_FILE);
			byte[] bytes = new byte[(int)testFile.length()];
			DataInputStream dataIn = new DataInputStream(new FileInputStream(testFile));
			dataIn.readFully(bytes);
			dataIn.close();

			DG3File dg3 = new DG3File(new ByteArrayInputStream(bytes));
			
			int img1Length = dg3.getFingerInfos().get(0).getFingerImageInfos().get(0).getImageLength();
			DataInputStream img1In = new DataInputStream(dg3.getFingerInfos().get(0).getFingerImageInfos().get(0).getImageInputStream());
			byte[] img1Bytes = new byte[img1Length];
			img1In.readFully(img1Bytes);

			int img2Length = dg3.getFingerInfos().get(1).getFingerImageInfos().get(0).getImageLength();
			DataInputStream img2In = new DataInputStream(dg3.getFingerInfos().get(1).getFingerImageInfos().get(0).getImageInputStream());
			byte[] img2Bytes = new byte[img2Length];
			img2In.readFully(img2Bytes);

			System.out.println("DEBUG: img1 (" + img1Bytes.length + ")\n" + Hex.bytesToHexString(img1Bytes, 0, 256));
			System.out.println("DEBUG: img2 (" + img2Bytes.length + ")\n" + Hex.bytesToHexString(img2Bytes, 0, 256));
			
			assertEquals(Hex.bytesToHexString(img2Bytes, 0, 32), "FFA0FFA8007A4E4953545F434F4D20390A5049585F5749445448203330380A50");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public void testEncodeDecode1() {
		try {
			/* Fetch the contents of the binary file. */
			File testFile = new File(TEST_FILE);
			byte[] bytes = new byte[(int)testFile.length()];
			DataInputStream dataIn = new DataInputStream(new FileInputStream(testFile));
			dataIn.readFully(bytes);
			dataIn.close();

			/* Test with byte array input stream as carrier. */

			DG3File dg3 = new DG3File(new ByteArrayInputStream(bytes));
			int img1Length = dg3.getFingerInfos().get(0).getFingerImageInfos().get(0).getImageLength();
			DataInputStream img1In = new DataInputStream(dg3.getFingerInfos().get(0).getFingerImageInfos().get(0).getImageInputStream());
			byte[] img1Bytes = new byte[img1Length];
			img1In.readFully(img1Bytes);
			System.out.println("DEBUG: img1\n" + Hex.bytesToHexString(img1Bytes, 0, 32));

			int img2Length = dg3.getFingerInfos().get(1).getFingerImageInfos().get(0).getImageLength();
			DataInputStream img2In = new DataInputStream(dg3.getFingerInfos().get(1).getFingerImageInfos().get(0).getImageInputStream());
			byte[] img2Bytes = new byte[img2Length];
			img2In.readFully(img2Bytes);

			System.out.println("DEBUG: img2\n" + Hex.bytesToHexString(img2Bytes, 0, 32));

			byte[] encodedFromByteArrayStream = dg3.getEncoded();

			assertEquals(bytes.length, encodedFromByteArrayStream.length);

			for (int i = 0; i < encodedFromByteArrayStream.length; i++) {
				if (bytes[i] != encodedFromByteArrayStream[i]) {
					System.out.println("DEBUG: difference at " + i);
					break;
				}
			}

			//			FileOutputStream out1 = new FileOutputStream("bytes.bin");
			//			out1.write(bytes);
			//			out1.flush();
			//			out1.close();
			//			
			//			FileOutputStream out2 = new FileOutputStream("encodedFromByteArrayStream.bin");
			//			out2.write(encodedFromByteArrayStream);
			//			out2.flush();
			//			out2.close();

			assertTrue(Arrays.equals(bytes, encodedFromByteArrayStream));

			/* Same but using file input stream */

			FileInputStream fileInputStream = new FileInputStream(testFile);
			DG3File dg3FromFileStream = new DG3File(fileInputStream);
			byte[] encodedFromFileStream = dg3FromFileStream.getEncoded();
			assertEquals(bytes.length, encodedFromFileStream.length);
			for (int i = 0; i < encodedFromByteArrayStream.length; i++) {
				if (bytes[i] != encodedFromFileStream[i]) {
					System.out.println("DEBUG: difference at " + i);
					break;
				}
			}
			assertTrue(Arrays.equals(bytes, encodedFromFileStream));

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testEncodeDecode2() {
		try {
			/* Fetch the contents of the binary file. */
			File testFile = new File(TEST_FILE);
			byte[] bytes = new byte[(int)testFile.length()];
			DataInputStream dataIn = new DataInputStream(new FileInputStream(testFile));
			dataIn.readFully(bytes);
			dataIn.close();

			/* Test with byte array input stream as carrier. */

			DG3File dg3 = new DG3File(new ByteArrayInputStream(bytes));
			byte[] encodedFromByteArrayStream = dg3.getEncoded();

			DG3File dg3Other = new DG3File(new ByteArrayInputStream(encodedFromByteArrayStream));
			byte[] encodedFromByteArrayStreamOther = dg3Other.getEncoded();

			assertEquals(dg3, dg3Other);

			assertEquals(encodedFromByteArrayStream.length, encodedFromByteArrayStreamOther.length);
			assertTrue(Arrays.equals(encodedFromByteArrayStream, encodedFromByteArrayStreamOther));

		} catch (Exception e) {
			fail(e.getMessage());
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

			FileOutputStream out = new FileOutputStream("dg3Bytes.out");
			out.write(dg3Bytes);
			out.flush();
			out.flush();
			out.close();

			out = new FileOutputStream("copyBytes.out");
			out.write(copyBytes);
			out.flush();
			out.flush();
			out.close();

			assertEquals(dg3Bytes.length, copyBytes.length);

			for (int i = 0; i < dg3Bytes.length; i++) {
				if (dg3Bytes[i] != copyBytes[i]) {
					System.out.println("DEBUG: difference at " + i);
					break;
				}
			}

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

	public void testFromBin() {
		try {
			File testFile = new File(TEST_FILE);			
			FileInputStream fileIn = new FileInputStream(testFile);
			DG3File dg3 = new DG3File(fileIn);						
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
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
			File testFile = new File(TEST_FILE);
			InputStream in = new FileInputStream(testFile);
			DG3File dg3 = new DG3File(in);
			return dg3;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
