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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.Gender;
import net.sourceforge.scuba.data.TestCountry;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.jmrtd.MRTDTrustStore;
import org.jmrtd.Passport;
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
		X509Certificate docSigningCert = createSelfSignedCertificate(issuer, subject, dateOfIssuing, dateOfExpiry, publicKey, privateKey, signatureAlgorithm);
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
			byte[] jpegImageBytes = createTrivialJPEGBytes(width, height);
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

	private static byte[] createTrivialJPEGBytes(int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", out);
			out.flush();
			byte[] bytes = out.toByteArray();
			return bytes;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static X509Certificate createSelfSignedCertificate(String issuer, String subject, Date dateOfIssuing, Date dateOfExpiry,
			PublicKey publicKey, PrivateKey privateKey, String signatureAlgorithm) throws CertificateEncodingException, InvalidKeyException, IllegalStateException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException {
		//		X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
		//		certGenerator.setSerialNumber(new BigInteger("1"));
		//		certGenerator.setIssuerDN(new X509Name(issuer));
		//		certGenerator.setSubjectDN(new X509Name(subject));
		//		certGenerator.setNotBefore(dateOfIssuing);
		//		certGenerator.setNotAfter(dateOfExpiry);
		//		certGenerator.setPublicKey(publicKey);
		//		certGenerator.setSignatureAlgorithm(signatureAlgorithm);
		//		X509Certificate certificate = 
		// (X509Certificate)certGenerator.generate(privateKey, "BC");

		try {
			X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(new X500Name(issuer), new BigInteger("1"), dateOfIssuing, dateOfExpiry, new X500Name(subject), SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));
			byte[] certBytes = certBuilder.build(new JCESigner(privateKey, signatureAlgorithm)).getEncoded();
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate)certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
			return certificate;
		} catch (Exception  e) {
			e.printStackTrace();
			return null;
		}
	}

	private static class JCESigner implements ContentSigner {

		private static final AlgorithmIdentifier PKCS1_SHA256_WITH_RSA_OID = new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.2.840.113549.1.1.11"));

		private Signature signature;
		private ByteArrayOutputStream outputStream;

		public JCESigner(PrivateKey privateKey, String signatureAlgorithm) {
			if (!"SHA256withRSA".equals(signatureAlgorithm)) {
				throw new IllegalArgumentException("Signature algorithm \"" + signatureAlgorithm + "\" not yet supported");
			}
			try {
				this.outputStream = new ByteArrayOutputStream();
				this.signature = Signature.getInstance(signatureAlgorithm);
				this.signature.initSign(privateKey);
			} catch (GeneralSecurityException gse) {
				throw new IllegalArgumentException(gse.getMessage());
			}
		}

		@Override
		public AlgorithmIdentifier getAlgorithmIdentifier() {
			if (signature.getAlgorithm().equals("SHA256withRSA")) {
				return PKCS1_SHA256_WITH_RSA_OID;
			} else {
				return null;
			}
		}

		@Override
		public OutputStream getOutputStream() {
			return outputStream;
		}

		@Override
		public byte[] getSignature() {
			try {
				signature.update(outputStream.toByteArray());
				return signature.sign();
			} catch (GeneralSecurityException gse) {
				gse.printStackTrace();
				return null;
			}
		}
	}
}
