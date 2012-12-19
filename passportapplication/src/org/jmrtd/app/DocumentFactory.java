/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id: $
 */

package org.jmrtd.app;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.Gender;
import net.sourceforge.scuba.data.TestCountry;

import org.jmrtd.MRTDTrustStore;
import org.jmrtd.Passport;
import org.jmrtd.app.util.CertificateUtil;
import org.jmrtd.app.util.ImageUtil;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.DataGroup;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceImageInfo.EyeColor;
import org.jmrtd.lds.FaceImageInfo.FeaturePoint;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.LDS;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.SODFile;

/**
 * Utility class for creating MRTDs.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Id: $
 * 
 * @since 0.4.7
 */
public class DocumentFactory {

	private static final Calendar CALENDAR = Calendar.getInstance(); 

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	/**
	 * Creates an MRTD from scratch.
	 * 
	 * @param docType either <code>MRZInfo.DOC_TYPE_ID1</code> or <code>MRZInfo.DOC_TYPE_ID3</code>
	 * 
	 * @throws GeneralSecurityException if something wrong
	 */
	public static Passport createEmptyMRTD(String docType, MRTDTrustStore trustManager) throws GeneralSecurityException {

		/* EF.COM */
		int[] tagList = { LDSFile.EF_DG1_TAG, LDSFile.EF_DG2_TAG };
		COMFile comFile = new COMFile("1.7", "4.0.0", tagList);

		/* EF.DG1 */
		Date today = CALENDAR.getTime();
		String todayString = SDF.format(today);
		String primaryIdentifier = "TRAVELER";
		String secondaryIdentifiers = "HAPPY";
		String documentNumber = "123456789";
		Country country = TestCountry.UT;
		Gender gender = Gender.FEMALE;
		String optionalData = "";
		MRZInfo mrzInfo = new MRZInfo(docType, country.toAlpha3Code(), primaryIdentifier, secondaryIdentifiers, documentNumber, country.toAlpha3Code(), todayString, gender, todayString, optionalData);
		DG1File dg1 = new DG1File(mrzInfo);

		/* EF.DG2 */
		FaceImageInfo faceImageInfo = createFaceImageInfo();
		FaceInfo faceInfo = new FaceInfo(Arrays.asList(new FaceImageInfo[] { faceImageInfo }));
		DG2File dg2 = new DG2File(Arrays.asList(new FaceInfo[] { faceInfo }));

		/* EF.SOD */
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		Date dateOfIssuing = today;
		Date dateOfExpiry = today;
		String digestAlgorithm = "SHA256";
		String signatureAlgorithm = "SHA256withRSA";
		String issuer = "C=UT, O=JMRTD, OU=DSCA, CN=jmrtd.org";
		String subject = "C=UT, O=JMRTD, OU=DSCA, CN=jmrtd.org";
		X509Certificate docSigningCert = CertificateUtil.createSelfSignedCertificate(issuer, subject, dateOfIssuing, dateOfExpiry, publicKey, privateKey, signatureAlgorithm);
		PrivateKey docSigningPrivateKey = privateKey;
		Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
		MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
		hashes.put(1, digest.digest(dg1.getEncoded()));
		hashes.put(2, digest.digest(dg2.getEncoded()));
		SODFile sodFile = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, privateKey, docSigningCert);
		return new Passport(new LDS(comFile, Arrays.asList(new DataGroup[] { dg1, dg2 }), sodFile), docSigningPrivateKey, trustManager);
	}

	private static FaceImageInfo createFaceImageInfo() {
		try {
			int width = 449, height = 599;
			byte[] jpegImageBytes = ImageUtil.createTrivialJPEGBytes(width, height);
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
			FeaturePoint[] featurePoints = { };
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
					new ByteArrayInputStream(jpegImageBytes), jpegImageBytes.length, imageDataType);
			return imageInfo;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
