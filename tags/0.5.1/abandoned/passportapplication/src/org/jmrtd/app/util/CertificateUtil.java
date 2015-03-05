package org.jmrtd.app.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;

public class CertificateUtil {

	public static X509Certificate createSelfSignedCertificate(String issuer, String subject, Date dateOfIssuing, Date dateOfExpiry,
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
