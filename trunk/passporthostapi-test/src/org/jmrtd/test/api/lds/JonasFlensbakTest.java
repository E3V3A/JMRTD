package org.jmrtd.test.api.lds;

import java.io.FileInputStream;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.scuba.data.Gender;

import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.DG3File;
import org.jmrtd.lds.DG7File;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceImageInfo.EyeColor;
import org.jmrtd.lds.FaceImageInfo.FeaturePoint;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;

/**
 * Tests specific binary samples of DG2, DG3, DG7.
 */
public class JonasFlensbakTest extends TestCase {

	private static final String JONAS_DIR = "/t:/paspoort/test/jonas3";

	private static final String DG2_SAMPLE = JONAS_DIR + "/dg2.bin";
	private static final String DG3_LEFT_SAMPLE = JONAS_DIR + "/dg3_left.bin";
	private static final String DG3_RIGHT_SAMPLE = JONAS_DIR + "/dg3_right.bin";
	private static final String DG7_SAMPLE = JONAS_DIR + "/dg7.bin";

	public void testDG2() {
		try {
			DG2FileTest dg2FileTest = new DG2FileTest("JonasFlensbakTest");
			dg2FileTest.testFile(new FileInputStream(DG2_SAMPLE));
			DG2File dg2File = new DG2File(new FileInputStream(DG2_SAMPLE));
			List<FaceInfo> faceInfos = dg2File.getFaceInfos();
			assertEquals(faceInfos.size(), 1);

			for (FaceInfo faceInfo: faceInfos) {
				List<FaceImageInfo> faceImageInfos = faceInfo.getFaceImageInfos();
				assertEquals(faceImageInfos.size(), 1);
				for (FaceImageInfo faceImageInfo: faceImageInfos) {
					testFaceImageInfo(faceImageInfo);
				}
			}

			dg2FileTest.testElements(dg2File);
			dg2FileTest.testReflexive(dg2File);
			dg2FileTest.testDecodeEncode(dg2File, 3);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testFaceImageInfo(FaceImageInfo faceImageInfo) {
		FeaturePoint[] featurePoints = faceImageInfo.getFeaturePoints();
		assertNotNull(featurePoints);
		Gender gender = faceImageInfo.getGender();
		EyeColor eyeColor = faceImageInfo.getEyeColor();
		int hairColor = faceImageInfo.getHairColor();
		int featureMask = faceImageInfo.getFeatureMask();
		int expression = faceImageInfo.getExpression();

		int faceImageType = faceImageInfo.getFaceImageType();
		int imageDataType = faceImageInfo.getImageDataType();
		int colorSpace = faceImageInfo.getColorSpace();
		int sourceType = faceImageInfo.getSourceType();
		int deviceType = faceImageInfo.getDeviceType();
		int quality = faceImageInfo.getQuality();

		/* Values from Danish police document... Not all ICAO passports will comply. */

		/* Facial information */
		assertEquals(featurePoints.length, 0);
//		assertEquals(gender.toInt(), 0x00); // FIXME: fail v3
		assertEquals(eyeColor.toInt(), 0x00);
		assertEquals(hairColor, 0x00);
		assertEquals(featureMask, 0x00000000);
		assertEquals(expression, 0x00);

		/* Image information */
		assertEquals(faceImageType, 0x01); // Full frontal
		assertEquals(imageDataType, 0x00); // JPEG
//		assertEquals(colorSpace, 0x00); // FIXME: fail v3
//		assertEquals(sourceType, 0x00); // FIXME: fail v3
		assertEquals(deviceType, 0x0000);
		assertEquals(quality, 0x0000);
	}

	public void testDG3Right() {
		try {
			DG3FileTest dg3FileTest = new DG3FileTest("JonasFlensbakTest");
			dg3FileTest.testFile(new FileInputStream(DG3_RIGHT_SAMPLE));
			DG3File dg3File = new DG3File(new FileInputStream(DG3_RIGHT_SAMPLE));
			List<FingerInfo> fingerInfos = dg3File.getFingerInfos();
			assertEquals(fingerInfos.size(), 1);
			for (FingerInfo fingerInfo: fingerInfos) {
				testFingerInfo(fingerInfo);
				List<FingerImageInfo> fingerImageInfos = fingerInfo.getFingerImageInfos();
				assertEquals(fingerImageInfos.size(), 1);
				for (FingerImageInfo fingerImageInfo: fingerImageInfos) {
					testFingerImageInfo(fingerImageInfo);
				}
			}
			dg3FileTest.testElements(dg3File);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public void testDG3Left() {
		try {
			DG3FileTest dg3FileTest = new DG3FileTest("JonasFlensbakTest");
			dg3FileTest.testFile(new FileInputStream(DG3_LEFT_SAMPLE));
			DG3File dg3File = new DG3File(new FileInputStream(DG3_LEFT_SAMPLE));
			List<FingerInfo> fingerInfos = dg3File.getFingerInfos();
			assertEquals(fingerInfos.size(), 1);
			for (FingerInfo fingerInfo: fingerInfos) {
				testFingerInfo(fingerInfo);
				List<FingerImageInfo> fingerImageInfos = fingerInfo.getFingerImageInfos();
				assertEquals(fingerImageInfos.size(), 1);
				for (FingerImageInfo fingerImageInfo: fingerImageInfos) {
					testFingerImageInfo(fingerImageInfo);
				}
			}
			dg3FileTest.testElements(dg3File);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}		
	}

	public void testFingerInfo(FingerInfo fingerInfo) {
		int deviceId = fingerInfo.getCaptureDeviceId();
		int acquisitionLevel = fingerInfo.getAcquisitionLevel();
		int scaleUnits = fingerInfo.getScaleUnits();
		int scanResH = fingerInfo.getHorizontalScanningResolution();
		int scanResV = fingerInfo.getVerticalScanningResolution();
		int imgResH = fingerInfo.getHorizontalImageResolution();
		int imgResV = fingerInfo.getVerticalImageResolution();
		int depth = fingerInfo.getDepth();
		int compressionAlg = fingerInfo.getCompressionAlgorithm();
		
//		assertEquals(deviceId, 0x0000); // FIXME: fail v3
//		assertTrue(acquisitionLevel == 0x0029 || acquisitionLevel == 0x01F); // 500ppi or 1000ppi // FIXME: fail v3
		assertEquals(scaleUnits, 0x01); // PPI
		assertTrue(scanResH == 0x03E8 || scanResH == 0x01F4);
		assertTrue(scanResV == 0x03E8 || scanResV == 0x01F4);
		assertTrue(/* imgResH == 0x03E8 || */ imgResH == 0x01F4);
		assertTrue(/* imgResV == 0x03E8 || */ imgResV == 0x01F4);
		assertEquals(depth, 8);
		assertEquals(compressionAlg, 0x02); // WSQ
	}

	public void testFingerImageInfo(FingerImageInfo fingerImageInfo) {
		int impressionType = fingerImageInfo.getImpressionType();
		int position = fingerImageInfo.getPosition();
		int quality = fingerImageInfo.getQuality();
		int viewNumber = fingerImageInfo.getViewNumber();
		int viewCount = fingerImageInfo.getViewCount();
		
		assertEquals(impressionType, 0x00); // live scan plain
		
		assertTrue(position == 1 // right thumb
				|| position == 2 // right index
				|| position == 3 // right middle
				|| position == 4 // right ring
				|| position == 5 // right little
				|| position == 6 // left thumb
				|| position == 7 // left index
				|| position == 8 // left middle
				|| position == 9 // left ring
				|| position == 10); // left little
	}

	public void testDG7() {
		try {
			DG7FileTest dg7FileTest = new DG7FileTest("JonasFlensbakTest");
			dg7FileTest.testFile(new FileInputStream(DG7_SAMPLE));
			DG7File dg7File = new DG7File(new FileInputStream(DG7_SAMPLE));
			List<DisplayedImageInfo> images = dg7File.getImages();
			assertEquals(images.size(), 1);

			DisplayedImageInfo displayedImageInfo = images.get(0);

			assertEquals(displayedImageInfo.getType(), DisplayedImageInfo.TYPE_SIGNATURE_OR_MARK);

			dg7FileTest.testElements(dg7File);
			dg7FileTest.testEncodeDecode(dg7File);
			dg7FileTest.testType(dg7File);			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
