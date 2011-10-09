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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;
import net.sourceforge.scuba.util.ImageUtil;

import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;
import org.jmrtd.lds.ImageInfo;

public class FingerImageInfoTest extends TestCase
{
	public FingerImageInfoTest(String name) {
		super(name);
	}

	public void testToString() {
		try {
			FingerImageInfo imageInfo = createRightIndexFingerTestObject();
			assertNotNull(imageInfo);
			String asString = imageInfo.toString();
			assertNotNull(asString);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testNonNullEncoded() {
		FingerImageInfo imageInfo = createRightIndexFingerTestObject();
		assertNotNull(imageInfo);
		byte[] encoded = imageInfo.getEncoded();
		assertNotNull(encoded);
	}

	public void testEncodeDecode() {
		FingerImageInfo testObject = createRightIndexFingerTestObject();
		testEncodeDecode(testObject);
	}

	public void testEncodeDecode(FingerImageInfo original) {
		try {
			String mimeType = original.getMimeType();
			int compressionAlg = 0;

			if ("image/x-wsq".equals(mimeType)) { compressionAlg = FingerInfo.COMPRESSION_WSQ; }
			else if ("image/jpeg".equals(mimeType)) { compressionAlg = FingerInfo.COMPRESSION_JPEG; }
			else if ("image/jpeg2000".equals(mimeType)) { compressionAlg = FingerInfo.COMPRESSION_JPEG2000; }
			else { fail("This test doesn't support this image data type " + mimeType); }
			byte[] encoded = original.getEncoded();
			assertNotNull(encoded);
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			FingerImageInfo copy = new FingerImageInfo(in, compressionAlg);
			assertEquals(original, copy);
			byte[] encodedCopy = copy.getEncoded();
			assertNotNull(encodedCopy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(encodedCopy));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testWidthHeight() {
		try {
			FingerImageInfo imageInfo = createRightIndexFingerTestObject();
			System.out.println("DEBUG: imageInfo.getWidth() = " + imageInfo.getWidth());
			System.out.println("DEBUG: imageInfo.getHeight() = " + imageInfo.getHeight());
			String mimeType = imageInfo.getMimeType();
			assertNotNull(mimeType);
			assertTrue("image/x-wsq".equals(mimeType) || "image/jpeg2000".equals(mimeType) || "image/jpeg".equals(mimeType));
			BufferedImage image = ImageUtil.read(imageInfo.getImageInputStream(), imageInfo.getImageLength(), mimeType);
			assertEquals(imageInfo.getWidth(), image.getWidth());
			assertEquals(imageInfo.getHeight(), image.getHeight());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testExtractImage() {
		FingerImageInfo imageInfo = createRightIndexFingerTestObject();
		testExtractImage(imageInfo, 545, 622);
	}

	public void testExtractImage(FingerImageInfo imageInfo, int expectedWidth, int expectedHeight) {
		try {
			BufferedImage image = ImageUtil.read(imageInfo.getImageInputStream(), imageInfo.getImageLength(), imageInfo.getMimeType());
			assertNotNull(image);
			assertEquals(image.getType(), BufferedImage.TYPE_BYTE_GRAY);
			assertEquals(image.getWidth(), expectedWidth);
			assertEquals(image.getHeight(), expectedHeight);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testViewCountAndNumber() {
		FingerImageInfo fingerImageInfo = createRightIndexFingerTestObject();
		testViewCountAndNumber(fingerImageInfo);
	}

	public void testViewCountAndNumber(FingerImageInfo fingerImageInfo) {
		int viewCount = fingerImageInfo.getViewCount();
		int viewNumber = fingerImageInfo.getViewCount();
		System.out.println("DEBUG: viewCount = " + viewCount);
		System.out.println("DEBUG: viewNumber = " + viewNumber);
		assertTrue(viewCount >= 1);
		assertTrue(viewNumber <= viewCount);
	}

	public void testValidType() {
		FingerImageInfo portraitInfo = createRightIndexFingerTestObject();
		testValidType(portraitInfo);
	}

	public void testValidType(FingerImageInfo imageInfo) {
		int type = imageInfo.getType();
		assertEquals(type, ImageInfo.TYPE_FINGER);
	}
	
	public void testLength() {
		FingerImageInfo fingerImageInfo = createRightIndexFingerTestObject();
		int imageLength = fingerImageInfo.getImageLength();
		int recordLength = (int)fingerImageInfo.getRecordLength();
		System.out.println("DEBUG: imageLength = " + imageLength);
		System.out.println("DEBUG: recordLength = " + recordLength);
		assertTrue(imageLength < recordLength);
	}

	public static FingerImageInfo createNonEmptyTestObject() {
		try {
			byte[] imageBytes = createTrivialWSQBytes(200, 200);
			FingerImageInfo imageInfo = new FingerImageInfo(new ByteArrayInputStream(imageBytes), FingerInfo.COMPRESSION_WSQ);
			return imageInfo;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			fail(ioe.getMessage());
			return null;
		}
	}

	/*
	 * Encoding of a 545 x 622 WSQ image.
	 */
	private static byte[] getSampleWSQBytes() {
		try {
			File file = new File("/t:/paspoort/sample_image.wsq");
			byte[] result = new byte[(int)file.length()];
			FileInputStream fileIn = new FileInputStream(file);
			int bytesRead = 0;
			while (bytesRead < result.length) {
				bytesRead += fileIn.read(result, bytesRead, result.length - bytesRead);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
			return null;
		}
	}

	/**
	 * FIXME: this appears to break j2wsq!
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	private static byte[] createTrivialWSQBytes(int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			ByteArrayOutputStream out = new ByteArrayOutputStream();			
			// Images.writeImage(image, "image/x-wsq", out);
			ImageIO.write(image, "wsq", out);
			out.flush();
			byte[] bytes = out.toByteArray();
			return bytes;
		} catch (Exception e) {
			fail(e.toString());
			return null;
		}
	}

	/*
	 * A finger image object containing a WSQ image with position is right index finger.
	 */
	public static FingerImageInfo createRightIndexFingerTestObject() {
		try {
			byte[] wsqBytes = getSampleWSQBytes();
			BufferedImage wsqImage = ImageUtil.read(new ByteArrayInputStream(wsqBytes), wsqBytes.length, "image/x-wsq");
			int width = wsqImage.getWidth(), height = wsqImage.getHeight();
			int position = FingerImageInfo.POSITION_RIGHT_INDEX_FINGER;
			int viewCount = 1;
			int viewNumber = 1;
			int quality = 69;
			int impressionType = FingerImageInfo.IMPRESSION_TYPE_LIVE_SCAN_PLAIN;
			FingerImageInfo testObject = new FingerImageInfo(
					position,
					viewCount, viewNumber, quality, impressionType, width, height, getSampleWSQBytes(), FingerInfo.COMPRESSION_WSQ);
			return testObject;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			return null;
		}
	}
}
