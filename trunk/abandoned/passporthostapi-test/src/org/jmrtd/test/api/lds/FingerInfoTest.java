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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.jmrtd.cbeff.CBEFFInfo;
import org.jmrtd.cbeff.ISO781611;
import org.jmrtd.cbeff.StandardBiometricHeader;
import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;

public class FingerInfoTest extends TestCase {

	public FingerInfoTest(String name) {
		super(name);
	}

	public void testToString() {
		try {
			FingerInfo imageInfo = createSingleRightIndexFingerTestObject();
			assertNotNull(imageInfo);
			String asString = imageInfo.toString();
			assertNotNull(asString);
			assertTrue(asString.startsWith("FingerInfo ["));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testEncodeDecode() {
		FingerInfo fingerInfo = createSingleRightIndexFingerTestObject();
		testEncodeDecode(fingerInfo);
	}

	public void testReflexive() {
		FingerInfo fingerInfo = createSingleRightIndexFingerTestObject();
		testReflexiveReconstruct(fingerInfo);
	}

	public void testElements() {
		FingerInfo fingerInfo = createSingleRightIndexFingerTestObject();
		testElements(fingerInfo);
	}


	public void testFields() {
		FingerInfo fingerInfo = createSingleRightIndexFingerTestObject();
		testFieldsReasonable(fingerInfo);
		testFieldsSameAfterReconstruct(fingerInfo);
	}

	public void testEncodeDecode(FingerInfo fingerInfo) {
		try {
			byte[] encoded = fingerInfo.getEncoded();
			FingerInfo fingerInfo2 = new FingerInfo(null, new ByteArrayInputStream(encoded));
			assertEquals(fingerInfo, fingerInfo2);
			testReflexiveReconstruct(fingerInfo2);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReflexiveReconstruct(FingerInfo fingerInfo) {
		FingerInfo fingerInfo2 = new FingerInfo(fingerInfo.getCaptureDeviceId(), fingerInfo.getAcquisitionLevel(), fingerInfo.getScaleUnits(), fingerInfo.getHorizontalScanningResolution(), fingerInfo.getVerticalScanningResolution(), fingerInfo.getHorizontalImageResolution(), fingerInfo.getVerticalImageResolution(), fingerInfo.getDepth(), fingerInfo.getCompressionAlgorithm(), fingerInfo.getFingerImageInfos());
		assertEquals(fingerInfo, fingerInfo2);
	}

	public void testElements(FingerInfo fingerInfo) {
		List<FingerImageInfo> imageInfos = fingerInfo.getFingerImageInfos();
		for (FingerImageInfo imageInfo: imageInfos) {
			FingerImageInfoTest imageInfoTest = new FingerImageInfoTest("FingerInfoTest_testElements");
			imageInfoTest.testEncodeDecode(imageInfo);
			imageInfoTest.testViewCountAndNumber(imageInfo);
			imageInfoTest.testValidType(imageInfo);
		}
	}

	public void testFieldsReasonable(FingerInfo fingerInfo) {
		int captureDeviceId = fingerInfo.getCaptureDeviceId();
		int acquisitionLevel = fingerInfo.getAcquisitionLevel();
		int scaleUnits = fingerInfo.getScaleUnits();
		int scanResolutionHorizontal = fingerInfo.getHorizontalScanningResolution();
		int scanResolutionVertical = fingerInfo.getVerticalScanningResolution();
		int imageResolutionHorizontal = fingerInfo.getHorizontalImageResolution();
		int imageResolutionVertical = fingerInfo.getVerticalImageResolution();
		int depth = fingerInfo.getDepth();
		int compressionAlgorithm = fingerInfo.getCompressionAlgorithm();

		assertTrue(scaleUnits == FingerInfo.SCALE_UNITS_PPI || scaleUnits == FingerInfo.SCALE_UNITS_PPCM);
		assertTrue(scanResolutionHorizontal >= 0);
		assertTrue(scanResolutionVertical >= 0);
		assertTrue(imageResolutionHorizontal >= 0);
		assertTrue(imageResolutionVertical >= 0);

		/* FIXME: is there some relation between scanRes and imgRes? Is scanRes <= imgRes? */
//		assertTrue("scanResolutionHorizontal == " + scanResolutionHorizontal
//				+ ", imageResolutionHorizontal == " + imageResolutionHorizontal,
//				scanResolutionHorizontal >= imageResolutionHorizontal);
//		assertTrue("scanResolutionVertical == " + scanResolutionVertical
//				+ ", imageResolutionVertical == " + imageResolutionVertical,
//				scanResolutionVertical >= imageResolutionVertical);

		assertTrue(depth >= 0);
		assertTrue(compressionAlgorithm == FingerInfo.COMPRESSION_JPEG
				|| compressionAlgorithm == FingerInfo.COMPRESSION_JPEG2000
				|| compressionAlgorithm == FingerInfo.COMPRESSION_PNG
				|| compressionAlgorithm == FingerInfo.COMPRESSION_UNCOMPRESSED_BIT_PACKED
				|| compressionAlgorithm == FingerInfo.COMPRESSION_UNCOMPRESSED_NO_BIT_PACKING
				|| compressionAlgorithm == FingerInfo.COMPRESSION_WSQ);
	}

	public void testFieldsSameAfterReconstruct(FingerInfo fingerInfo) {
		FingerInfo fingerInfo2 = new FingerInfo(fingerInfo.getCaptureDeviceId(), fingerInfo.getAcquisitionLevel(), fingerInfo.getScaleUnits(), fingerInfo.getHorizontalScanningResolution(), fingerInfo.getVerticalScanningResolution(), fingerInfo.getHorizontalImageResolution(), fingerInfo.getVerticalImageResolution(), fingerInfo.getDepth(), fingerInfo.getCompressionAlgorithm(), fingerInfo.getFingerImageInfos());
		int captureDeviceId = fingerInfo.getCaptureDeviceId();
		int acquisitionLevel = fingerInfo.getAcquisitionLevel();
		int scaleUnits = fingerInfo.getScaleUnits();
		int scanResolutionHorizontal = fingerInfo.getHorizontalScanningResolution();
		int scanResolutionVertical = fingerInfo.getVerticalScanningResolution();
		int imageResolutionHorizontal = fingerInfo.getHorizontalImageResolution();
		int imageResolutionVertical = fingerInfo.getVerticalImageResolution();
		int depth = fingerInfo.getDepth();
		int compressionAlgorithm = fingerInfo.getCompressionAlgorithm();

		int captureDeviceId2 = fingerInfo2.getCaptureDeviceId();
		int acquisitionLevel2 = fingerInfo2.getAcquisitionLevel();
		int scaleUnits2 = fingerInfo2.getScaleUnits();
		int scanResolutionHorizontal2 = fingerInfo2.getHorizontalScanningResolution();
		int scanResolutionVertical2 = fingerInfo2.getVerticalScanningResolution();
		int imageResolutionHorizontal2 = fingerInfo2.getHorizontalImageResolution();
		int imageResolutionVertical2 = fingerInfo2.getVerticalImageResolution();
		int depth2 = fingerInfo2.getDepth();
		int compressionAlgorithm2 = fingerInfo2.getCompressionAlgorithm();

		assertEquals(captureDeviceId, captureDeviceId2);
		assertEquals(acquisitionLevel,acquisitionLevel2);
		assertEquals(scaleUnits, scaleUnits2);
		assertEquals(scanResolutionHorizontal, scanResolutionHorizontal2);
		assertEquals(scanResolutionVertical, scanResolutionVertical2);
		assertEquals(imageResolutionHorizontal, imageResolutionHorizontal2);
		assertEquals(imageResolutionVertical, imageResolutionVertical2);
		assertEquals(depth, depth2);
		assertEquals(compressionAlgorithm, compressionAlgorithm2);

		assertNotNull(fingerInfo.getFingerImageInfos());
		assertNotNull(fingerInfo2.getFingerImageInfos());
		assertEquals(fingerInfo.getFingerImageInfos(), fingerInfo2.getFingerImageInfos());
	}

	public void testSBHFields() {
		FingerInfo fingerInfo = createSingleRightIndexFingerTestObject();
		testMandatorySBHFields(fingerInfo);
		testOptionalSBHFields(fingerInfo);
	}

	/*
	 * Doc 9303 says:
	 * - Biometric type (Optional, but mandatory if subtype specified) // FIXME: is this true?
	 * - Biometric subtype (Optional for DG2, mandatory for DG3, DG4.)
	 * - Creation date and time (Optional)
	 * - Validity period (from through) (Optional)
	 * - Creator of the biometric reference data(PID) (Optional)
	 * - Format owner (Mandatory)
	 * - Format type (Mandatory)
	 * 
	 * In practice this means: 0x81 (bio type), 0x82 (bio subtype),
	 * 0x87 (format owner), 0x88 (format type) will be present.
	 */
	public void testMandatorySBHFields(FingerInfo fingerInfo) {
		StandardBiometricHeader sbh = fingerInfo.getStandardBiometricHeader();
		Set<Integer> tags = sbh.getElements().keySet();
		assertTrue(tags.contains(0x81)); assertTrue(tags.contains(ISO781611.BIOMETRIC_TYPE_TAG));
		assertTrue(tags.contains(0x82)); assertTrue(tags.contains(ISO781611.BIOMETRIC_SUBTYPE_TAG));
		assertTrue(tags.contains(0x87)); assertTrue(tags.contains(ISO781611.FORMAT_OWNER_TAG));
		assertTrue(tags.contains(0x88)); assertTrue(tags.contains(ISO781611.FORMAT_TYPE_TAG));
		
		byte[] bioType = sbh.getElements().get(ISO781611.BIOMETRIC_TYPE_TAG);
		assertNotNull(bioType);
		assertEquals(bioType.length, 1);
		assertEquals(bioType[0], CBEFFInfo.BIOMETRIC_TYPE_FINGERPRINT);
		assertEquals(bioType[0], 8);

		/* FIXME: is bio type really mandatory for DG3 (according to ICAO)? */
		byte[] bioSubType = sbh.getElements().get(ISO781611.BIOMETRIC_SUBTYPE_TAG);
		assertNotNull(bioSubType);
		assertEquals(bioSubType.length, 1);
		
		/* Possible subtypes for finger. */
		assertTrue("Bio sub type = " + bioSubType[0],
				bioSubType[0] == 5
				|| bioSubType[0] == 9
				|| bioSubType[0] == 13
				|| bioSubType[0] == 17
				|| bioSubType[0] == 21
				|| bioSubType[0] == 6
				|| bioSubType[0] == 10
				|| bioSubType[0] == 14
				|| bioSubType[0] == 18
				|| bioSubType[0] == 22);
	}

	public void testOptionalSBHFields(FingerInfo fingerInfo) {
		Integer[] possibleTagsArray = { 0x81, 0x82, 0x83, /* 0x84, */ 0x85, 0x86, 0x87, 0x88 };
		Set<Integer> possibleTags = new HashSet<Integer>(Arrays.asList(possibleTagsArray));
		StandardBiometricHeader sbh = fingerInfo.getStandardBiometricHeader();
		Set<Integer> tags = sbh.getElements().keySet();
		for (int tag: tags) {
			assertTrue(possibleTags.contains(tag));
		}
	}
	
	public void testBiometricSubType() {
		FingerInfo fingerInfo = createSingleRightIndexFingerTestObject();
		testBiometricSubType(fingerInfo);
	}
	
	public void testBiometricSubType(FingerInfo fingerInfo) {
		StandardBiometricHeader sbh = fingerInfo.getStandardBiometricHeader();
		byte[] bioSubType = sbh.getElements().get(ISO781611.BIOMETRIC_SUBTYPE_TAG);
		/* bio sub type is mandatory in DG3 (Doc 9303). */
		assertNotNull(bioSubType);
		assertEquals(bioSubType.length, 1);
		List<FingerImageInfo> fingerImageInfos = fingerInfo.getFingerImageInfos();
		for (FingerImageInfo fingerImageInfo: fingerImageInfos) {
			int position = fingerImageInfo.getPosition();
			switch (bioSubType[0]) {
			case 5: assertEquals(position, 1); break;
			case 9: assertEquals(position, 2); break;
			case 13: assertEquals(position, 3); break;
			case 17: assertEquals(position, 4); break;
			case 21: assertEquals(position, 5); break;
			case 6: assertEquals(position, 6); break;
			case 10: assertEquals(position, 7); break;
			case 14: assertEquals(position, 8); break;
			case 18: assertEquals(position, 9); break;
			case 22: assertEquals(position, 10); break;
			default: fail("Unknown BHT coded bio type: " + bioSubType[0]);
			}
		}
	}
	
	public static FingerInfo createSingleRightIndexFingerTestObject() {
		List<FingerImageInfo> fingerImageInfos = new LinkedList<FingerImageInfo>();
		FingerImageInfo fingerImageInfo = FingerImageInfoTest.createRightIndexFingerTestObject();
		fingerImageInfos.add(fingerImageInfo);
		FingerInfo fingerInfo = new FingerInfo(0,
				31, /* 31 if 500dpi, 41 if 1000dpi */
				FingerInfo.SCALE_UNITS_PPI,
				500,
				500,
				500,
				500,
				8,
				FingerInfo.COMPRESSION_WSQ,
				fingerImageInfos);
		return fingerInfo;
	}
}
