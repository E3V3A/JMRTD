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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.jmrtd.cbeff.ISO781611;
import org.jmrtd.cbeff.StandardBiometricHeader;
import org.jmrtd.lds.IrisBiometricSubtypeInfo;
import org.jmrtd.lds.IrisImageInfo;
import org.jmrtd.lds.IrisInfo;

public class IrisInfoTest extends TestCase {

	public IrisInfoTest(String name) {
		super(name);
	}

	public void testToString() {
		try {
			IrisInfo info = createTestObject();
			assertNotNull(info);
			String asString = info.toString();
			assertNotNull(asString);
			assertTrue(asString.startsWith("IrisInfo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	public void testSBHFields() {
		IrisInfo irisInfo = createTestObject();
		testMandatorySBHFields(irisInfo);
		testOptionalSBHFields(irisInfo);
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
	public void testMandatorySBHFields(IrisInfo irisInfo) {
		StandardBiometricHeader sbh = irisInfo.getStandardBiometricHeader();
		Set<Integer> tags = sbh.getElements().keySet();
		assertTrue(tags.contains(0x81)); assertTrue(tags.contains(ISO781611.BIOMETRIC_TYPE_TAG));
		assertTrue(tags.contains(0x82)); assertTrue(tags.contains(ISO781611.BIOMETRIC_SUBTYPE_TAG));
		assertTrue(tags.contains(0x87)); assertTrue(tags.contains(ISO781611.FORMAT_OWNER_TAG));
		assertTrue(tags.contains(0x88)); assertTrue(tags.contains(ISO781611.FORMAT_TYPE_TAG));
	}

	public void testOptionalSBHFields(IrisInfo irisInfo) {
		Integer[] possibleTagsArray = { 0x81, 0x82, 0x83, /* 0x84, */ 0x85, 0x86, 0x87, 0x88 };
		Set<Integer> possibleTags = new HashSet<Integer>(Arrays.asList(possibleTagsArray));
		StandardBiometricHeader sbh = irisInfo.getStandardBiometricHeader();
		Set<Integer> tags = sbh.getElements().keySet();
		for (int tag: tags) {
			assertTrue(possibleTags.contains(tag));
		}
	}

	public static IrisInfo createTestObject() {
		IrisBiometricSubtypeInfo irisFeatureInfo = IrisBiometricSubtypeInfoTest.createTestObject();
		int captureDeviceId = IrisInfo.CAPTURE_DEVICE_UNDEF;
		int horizontalOrientation = IrisInfo.ORIENTATION_BASE;
		int verticalOrientation = IrisInfo.ORIENTATION_BASE;
		int scanType = IrisInfo.SCAN_TYPE_UNDEF;
		int irisOcclusion = IrisInfo.IROCC_UNDEF;
		int occlusionFilling = IrisInfo.IROCC_ZEROFILL;
		int boundaryExtraction = IrisInfo.IRBNDY_UNDEF;
		int irisDiameter = 200;
		int imageFormat = irisFeatureInfo.getImageFormat();
		List<IrisImageInfo> imageInfos = irisFeatureInfo.getIrisImageInfos();
		assertNotNull(imageInfos);
		assertEquals(imageInfos.size(), 1);
		IrisImageInfo imageInfo = imageInfos.get(0);
		int rawImageWidth = imageInfo.getWidth();
		int rawImageHeight = imageInfo.getHeight();
		int intensityDepth = 24;
		int imageTransformation = IrisInfo.TRANS_UNDEF;
		byte[] deviceUniqueId = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		
		List<IrisBiometricSubtypeInfo> irisFeatureInfos = Arrays.asList(new IrisBiometricSubtypeInfo[] { irisFeatureInfo });
		IrisInfo irisInfo = new IrisInfo(captureDeviceId, horizontalOrientation, verticalOrientation,
				scanType, irisOcclusion, occlusionFilling, boundaryExtraction, irisDiameter, imageFormat,
				rawImageWidth, rawImageHeight, intensityDepth, imageTransformation, deviceUniqueId, irisFeatureInfos);
		return irisInfo;
	}
}
