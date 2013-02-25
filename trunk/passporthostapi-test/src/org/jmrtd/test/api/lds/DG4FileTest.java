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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.jmrtd.lds.DG4File;
import org.jmrtd.lds.IrisBiometricSubtypeInfo;
import org.jmrtd.lds.IrisImageInfo;
import org.jmrtd.lds.IrisInfo;

public class DG4FileTest extends TestCase {

	public static final String TEST_FILE = "t:/paspoort/test/bsi/Datagroup4.bin";

	public DG4FileTest(String name) {
		super(name);
	}

	public void testConstruct() {
		try {
			DG4File dg4 = new DG4File(Arrays.asList(new IrisInfo[] { }));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReflexive() {
		DG4File dg4 = createTestObject();
		testReflexive(dg4);

		dg4 = getTestObject();
		testReflexive(dg4);
	}

	public void testReflexive(DG4File dg4) {
		try {
			byte[] bytes = dg4.getEncoded();
			InputStream inputStream = new ByteArrayInputStream(bytes);
			DG4File copy = new DG4File(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testEncodeDecode() {
		DG4File dg4 = getTestObject();
		testEncodeDecode(dg4);

		dg4 = createTestObject();
		testEncodeDecode(dg4);
	}

	public void testEncodeDecode(DG4File dg4) {
		try {
			byte[] dg4Bytes = dg4.getEncoded();
			assertNotNull(dg4Bytes);

			DG4File copy = new DG4File(new ByteArrayInputStream(dg4Bytes));
			byte[] copyBytes = copy.getEncoded();
			assertNotNull(copyBytes);

			assertTrue(Arrays.equals(dg4Bytes, copyBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testZeroInstanceTestObjectNotEquals() {
		try {
			DG4File dg4 = new DG4File(new LinkedList<IrisInfo>());
			byte[] dg4Bytes = dg4.getEncoded();
			assertNotNull(dg4Bytes);

			DG4File anotherDG4 = new DG4File(new LinkedList<IrisInfo>());
			byte[] anotherDG4Bytes = anotherDG4.getEncoded();
			assertNotNull(anotherDG4Bytes);

			assertFalse(Arrays.equals(dg4Bytes, anotherDG4Bytes));

			DG4File copy = new DG4File(new ByteArrayInputStream(dg4Bytes));
			byte[] copyBytes = copy.getEncoded();
			assertNotNull(copyBytes);

			assertFalse(Arrays.equals(dg4Bytes, copyBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testFileFromLDS() {
		try {
			File zipFile = new File("t:/paspoort/test/bsi.zip");
			FileInputStream inputStream = new FileInputStream(zipFile);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public void testFile() {
		try {
			DG4File dg4 = getTestObject();
			List<IrisInfo> recordInfos = dg4.getIrisInfos();
			int recordCount = recordInfos.size();
			int recordNumber = 1;
			System.out.println("DEBUG: Number of iris records = " + recordCount);
			for (IrisInfo record: recordInfos) {
				List<IrisBiometricSubtypeInfo> subtypeInfos = record.getIrisBiometricSubtypeInfos();
				int subtypeInfoCount = subtypeInfos.size();
				System.out.println("DEBUG: Number of subtypes in iris record " + recordNumber + " is " + subtypeInfoCount);
				int imageInfoNumber = 1;
				for (IrisBiometricSubtypeInfo subtypeInfo: subtypeInfos) {
					List<IrisImageInfo> imageInfos = subtypeInfo.getIrisImageInfos();
					int imageInfoCount = imageInfos.size();
					System.out.println("DEBUG: Number of image infos in iris subtype record " + imageInfoNumber + " is " + imageInfoCount);
					for (IrisImageInfo imageInfo: imageInfos) {
						int length = imageInfo.getImageLength();
						byte[] bytes = new byte[length];
						DataInputStream dataIn = new DataInputStream(imageInfo.getImageInputStream());
						dataIn.readFully(bytes);

						RenderedImage image = ImageUtil.read(new ByteArrayInputStream(bytes), imageInfo.getImageLength(), imageInfo.getMimeType());
						System.out.println("DEBUG: iris " + imageInfoNumber + "/" + imageInfoCount + " in record " + recordNumber + "/" + recordCount + " has " + image.getWidth() + " x " + image.getHeight());
					}
					subtypeInfoCount ++;
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

	public static DG4File createTestObject() {
		try {
			BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
			ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
			ImageUtil.write(image, "image/jpeg", imageOut);
			byte[] imageBytes = imageOut.toByteArray();

			int imageFormat = IrisInfo.IMAGEFORMAT_RGB_JPEG;
			IrisImageInfo irisImageInfo = new IrisImageInfo(0, 300, 200, new ByteArrayInputStream(imageBytes), imageBytes.length, IrisInfo.IMAGEFORMAT_RGB_JPEG);

			int biometricSubtype = IrisBiometricSubtypeInfo.EYE_UNDEF;
			IrisBiometricSubtypeInfo irisBiometricSubtypeInfo = new IrisBiometricSubtypeInfo(biometricSubtype, imageFormat, Arrays.asList(new IrisImageInfo[] { irisImageInfo }));

			int captureDeviceId = IrisInfo.CAPTURE_DEVICE_UNDEF;
			int horizontalOrientation = IrisInfo.ORIENTATION_UNDEF;
			int verticalOrientation = IrisInfo.ORIENTATION_UNDEF;
			int scanType = IrisInfo.SCAN_TYPE_UNDEF;
			int irisOcclusion = IrisInfo.IROCC_UNDEF;
			int occlusionFilling = IrisInfo.IROCC_UNDEF;
			int boundaryExtraction = IrisInfo.IRBNDY_UNDEF;
			int irisDiameter = 167;
			int rawImageWidth = 300;
			int rawImageHeight = 200;
			int intensityDepth = 8;
			int imageTransformation = IrisInfo.TRANS_UNDEF;
			byte[] deviceUniqueId = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
			IrisInfo irisInfo = new IrisInfo(
					captureDeviceId,
					horizontalOrientation,
					verticalOrientation,
					scanType,
					irisOcclusion,
					occlusionFilling,
					boundaryExtraction,
					irisDiameter,
					imageFormat,
					rawImageWidth,
					rawImageHeight,
					intensityDepth,
					imageTransformation,
					deviceUniqueId,
					Arrays.asList(new IrisBiometricSubtypeInfo[] { irisBiometricSubtypeInfo }));
			DG4File dg4 = new DG4File(Arrays.asList(new IrisInfo[] { irisInfo }));
			dg4.addIrisInfo(IrisInfoTest.createTestObject());
			return dg4;
		} catch (IOException ioe) {
			fail(ioe.getMessage());
			return null;
		}
	}

	public static DG4File getTestObject() {
		try {
			FileInputStream in = new FileInputStream(TEST_FILE);
			DG4File dg4 = new DG4File(in);
			return dg4;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
