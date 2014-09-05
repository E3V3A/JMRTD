package org.jmrtd.test.api.lds;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.jmrtd.lds.IrisImageInfo;
import org.jmrtd.lds.IrisInfo;

public class IrisImageInfoTest extends TestCase {

	public void testImageExtract() {
		try {
			IrisImageInfo imageInfo = createTestObject();
			BufferedImage image = ImageUtil.read(imageInfo.getImageInputStream(), imageInfo.getImageLength(), imageInfo.getMimeType());
			assertNotNull(image);
			assertEquals(image.getWidth(), imageInfo.getWidth());
			assertEquals(image.getHeight(), imageInfo.getHeight());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testToString() {
		try {
			IrisImageInfo info = createTestObject();
			assertNotNull(info);
			String asString = info.toString();
			assertNotNull(asString);
			assertTrue(asString.startsWith("IrisImageInfo"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	public void testLength() {
		IrisImageInfo irisImageInfo = createTestObject();
		int imageLength = irisImageInfo.getImageLength();
		int recordLength = (int)irisImageInfo.getRecordLength();
		assertTrue(imageLength < recordLength);
	}

	public static IrisImageInfo createTestObject() {
		try {
			BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_BYTE_GRAY);
			ByteArrayOutputStream encodedImageOut = new ByteArrayOutputStream();
			ImageUtil.write(image, "image/jpeg", encodedImageOut);
			encodedImageOut.flush();
			byte[] imageBytes = encodedImageOut.toByteArray();
			IrisImageInfo irisImageInfo = new IrisImageInfo(1, image.getWidth(), image.getHeight(), new ByteArrayInputStream(imageBytes), imageBytes.length, IrisInfo.IMAGEFORMAT_MONO_JPEG);
			return irisImageInfo;
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return null;
		}
	}
}
