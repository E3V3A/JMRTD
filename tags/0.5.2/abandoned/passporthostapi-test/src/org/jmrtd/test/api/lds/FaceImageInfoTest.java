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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import junit.framework.TestCase;
import net.sourceforge.scuba.data.Gender;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceImageInfo.EyeColor;
import org.jmrtd.lds.FaceImageInfo.FeaturePoint;
import org.jmrtd.lds.ImageInfo;

public class FaceImageInfoTest extends TestCase {

	public FaceImageInfoTest(String name) {
		super(name);
	}

	public void testToString() {
		FaceImageInfo imageInfo = createNonEmptyTestObject();
		try {
			assertNotNull(imageInfo);
			String asString = imageInfo.toString();
			assertNotNull(asString);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testNonNullEncoded() {
		FaceImageInfo imageInfo = createNonEmptyTestObject();
		assertNotNull(imageInfo);
		byte[] encoded = imageInfo.getEncoded();
		assertNotNull(encoded);
	}

	public void testEncodeDecode() {
		testEncodeDecode(createNonEmptyTestObject());
	}

	public void testEncodeDecode(FaceImageInfo original) {
		try {
			byte[] encoded = original.getEncoded();
			assertNotNull(encoded);
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			FaceImageInfo copy = new FaceImageInfo(in);
			assertEquals(original, copy);
			byte[] encodedCopy = copy.getEncoded();
			assertNotNull(encodedCopy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(encodedCopy));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testNumExtractImageOnce() {
		FaceImageInfo imageInfo = createNonEmptyTestObject(50, 50);
		testExtractImage(imageInfo, 50, 50);
	}
	
	public void testNumExtractImage() {
		for (int width = 100; width < 1000; width += 200) {
			for (int height = 100; height < 1000; height += 200) {
				FaceImageInfo imageInfo = createNonEmptyTestObject(width, height);
				testExtractImage(imageInfo, width, height);
			}
		}
	}

	public void testExtractImage(FaceImageInfo imageInfo, int expectedWidth, int expectedHeight) {
		try {
			InputStream imageInputStream = imageInfo.getImageInputStream();
			int imageLength = imageInfo.getImageLength();
			String imageMimeType = imageInfo.getMimeType();
			BufferedImage image = ImageUtil.read(imageInputStream, imageLength, imageMimeType);
			assertNotNull(image);
			assertEquals(image.getWidth(), expectedWidth);
			assertEquals(image.getHeight(), expectedHeight);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testValidType() {
		FaceImageInfo portraitInfo = createTestObject();
		testValidType(portraitInfo);
	}

	public void testValidType(FaceImageInfo imageInfo) {
		int type = imageInfo.getType();
		assertEquals(type, ImageInfo.TYPE_PORTRAIT);
	}

	public void testLength() {
		FaceImageInfo faceImageInfo = createTestObject();
		int imageLength = faceImageInfo.getImageLength();
		int recordLength = (int)faceImageInfo.getRecordLength();
		assertTrue(imageLength < recordLength);
	}

	public static FaceImageInfo createTestObject() {
		return createNonEmptyTestObject(300, 400);
	}

	public static FaceImageInfo createNonEmptyTestObject() {
		return createNonEmptyTestObject(1, 1);
	}


	public static FaceImageInfo createNonEmptyTestObject(int width, int height) {
		try {
			byte[] imageBytes = createTrivialJPEGBytes(width, height);
			Gender gender = Gender.UNSPECIFIED;
			EyeColor eyeColor = EyeColor.UNSPECIFIED;
			int hairColor = FaceImageInfo.HAIR_COLOR_UNSPECIFIED;
			int featureMask = 0;
			short expression = FaceImageInfo.EXPRESSION_UNSPECIFIED;
			int[] poseAngle = { 0, 0, 0 };
			int[] poseAngleUncertainty = { 0, 0, 0 };
			int faceImageType = FaceImageInfo.FACE_IMAGE_TYPE_FULL_FRONTAL;
			int colorSpace = 0x00;
			int sourceType = FaceImageInfo.SOURCE_TYPE_UNSPECIFIED;
			int deviceType = 0x0000;
			int quality = 0x0000;
			int imageDataType = FaceImageInfo.IMAGE_DATA_TYPE_JPEG;	
			FeaturePoint[] featurePoints = new FeaturePoint[0];
			FaceImageInfo imageInfo = new FaceImageInfo(
					gender,  eyeColor, hairColor,
					featureMask,
					expression,
					poseAngle, poseAngleUncertainty,
					faceImageType,
					colorSpace,
					sourceType,
					deviceType,
					quality,
					featurePoints,
					width, height,
					new ByteArrayInputStream(imageBytes), imageBytes.length, imageDataType);
			return imageInfo;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static byte[] createTrivialJPEGBytes(int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", out);
			out.flush();
			byte[] bytes = out.toByteArray();
			return bytes;
		} catch (Exception e) {
			fail(e.toString());
			return null;
		}
	}
}
