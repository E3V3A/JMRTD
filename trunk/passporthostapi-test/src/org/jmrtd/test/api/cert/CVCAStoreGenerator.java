package org.jmrtd.test.api.cert;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.ejbca.cvc.AccessRightEnum;
import org.ejbca.cvc.AuthorizationRoleEnum;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CVCertificateBody;
import org.ejbca.cvc.CertificateGenerator;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.cvc.HolderReferenceField;
import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.cert.CVCAuthorizationTemplate;
import org.jmrtd.cert.CVCAuthorizationTemplate.Permission;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;

public class CVCAStoreGenerator extends TestCase {

	private static final Calendar CALENDAR = Calendar.getInstance(); 

	private static final String
	RFC_3369_SIGNED_DATA_OID = "1.2.840.113549.1.7.2",		/* id-signedData OBJECT IDENTIFIER ::= { iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs7(7) 2 } */
	RFC_3369_CONTENT_TYPE_OID = "1.2.840.113549.1.9.3",
	RFC_3369_MESSAGE_DIGEST_OID = "1.2.840.113549.1.9.4",
	RSA_SA_PSS_OID = "1.2.840.113549.1.1.10",			
	PKCS1_RSA_OID = "1.2.840.113549.1.1.1",
	PKCS1_MD2_WITH_RSA_OID = "1.2.840.113549.1.1.2",
	PKCS1_MD4_WITH_RSA_OID = "1.2.840.113549.1.1.3",
	PKCS1_MD5_WITH_RSA_OID = "1.2.840.113549.1.1.4",
	PKCS1_SHA1_WITH_RSA_OID = "1.2.840.113549.1.1.5",
	PKCS1_SHA256_WITH_RSA_OID = "1.2.840.113549.1.1.11",
	PKCS1_SHA384_WITH_RSA_OID = "1.2.840.113549.1.1.12",
	PKCS1_SHA512_WITH_RSA_OID = "1.2.840.113549.1.1.13",
	PKCS1_SHA224_WITH_RSA_OID = "1.2.840.113549.1.1.14",
	X9_SHA1_WITH_ECDSA_OID = "1.2.840.10045.4.1",
	X9_SHA224_WITH_ECDSA_OID = "1.2.840.10045.4.3.1",
	X9_SHA256_WITH_ECDSA_OID = "1.2.840.10045.4.3.2",
	IEEE_P1363_SHA1_OID = "1.3.14.3.2.26",
	BSI_EAC_TA_SHA224_WITH_ECDSA_OID = "0.4.0.127.0.7.2.2.2.2.2",
	BSI_EAC_TA_SHA256_WITH_ECDSA_OID = "0.4.0.127.0.7.2.2.2.2.3",
	BSI_EAC_TA_SHA384_WITH_ECDSA_OID= "0.4.0.127.0.7.2.2.2.2.4",
	BSI_EAC_TA_SHA512_WITH_ECDSA_OID= "0.4.0.127.0.7.2.2.2.2.5";

	private static final Provider
	BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider(),
	JMRTD_PROVIDER = JMRTDSecurityProvider.getInstance();

	private static CertificateFactory cvCertificateFactory;
	private static KeyFactory rsaKeyFactory;
	private static KeyFactory ecKeyFactory;

	static {
		Security.addProvider(BC_PROVIDER);
	}

	public CVCAStoreGenerator() throws GeneralSecurityException {
		getCVCCertificateFactory();
		getRSAKeyFactory();
	}

	private static CertificateFactory getCVCCertificateFactory() throws CertificateException {
		if (cvCertificateFactory != null) { return cvCertificateFactory; }
		cvCertificateFactory = CertificateFactory.getInstance("CVC", JMRTD_PROVIDER);
		return cvCertificateFactory;
	}

	private static KeyFactory getRSAKeyFactory() throws GeneralSecurityException {
		if (rsaKeyFactory != null) { return rsaKeyFactory; }
		rsaKeyFactory = KeyFactory.getInstance("RSA");
		return rsaKeyFactory;
	}

	private static KeyFactory getECKeyFactory() throws GeneralSecurityException {
		if (ecKeyFactory != null) { return ecKeyFactory; }
		ecKeyFactory = KeyFactory.getInstance("EC", "BC");
		return ecKeyFactory;
	}

	private static final String
	STORE_PASSWORD = "",
	KEY_ENTRY_PASSWORD = "";

	private static final String
	//	WOJ_ROOT = "file:/home/sos/woj",
	WOJ_ROOT = "file:/t:/ca/cvca/woj",
	WOJ_DIR = WOJ_ROOT + "/terminals/nltest",
	WOJ_KS = WOJ_ROOT + "/terminals/nltest.ks";

	//	public static final String filenameCA = "/c:/cacert.cvcert";
	//
	//	public static final String filenameTerminal = "/c:/terminalcert.cvcert";
	//
	//	public static final String filenameKey = "/c:/terminalkey.der";

	private static final String TEST_CV_CERT_DIR = "/t:/ca/cvcert";

	public static final String filenameCA = "cacert.cvcert";
	public static final String filenameTerminal = "terminalcert.cvcert";
	public static final String filenameKey = "terminalkey.der";
	private static final String TEST_CV_KEY_STORE = "cvca.ks";

	public void testGenerateDERFiles() {
		try {
			int jmrtdProviderIndex = JMRTDSecurityProvider.beginPreferBouncyCastleProvider();

			// Get the current time, and +3 months
			Calendar cal1 = Calendar.getInstance();
			Date validFrom = cal1.getTime();

			Calendar cal2 = Calendar.getInstance();
			cal2.add(Calendar.MONTH, 3);
			Date validTo = cal2.getTime();

			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", BC_PROVIDER);
			SecureRandom random = new SecureRandom();
			keyGen.initialize(1024, random);

			// Create a new key pair for the self signed CA certificate
			KeyPair caKeyPair = keyGen.generateKeyPair();

			// Create a new key pair for the terminal certificate (signed by CA)
			keyGen.initialize(1024, random);
			KeyPair terminalKeyPair = keyGen.generateKeyPair();

			CAReferenceField caRef = new CAReferenceField("NL", "CVCA0", "00002");
			HolderReferenceField holderRef = new HolderReferenceField(caRef.getCountry(), caRef.getMnemonic(), caRef.getSequence());

			// Create the CA certificate
			CVCertificate caCvc = CertificateGenerator.createCertificate(
					caKeyPair.getPublic(), caKeyPair.getPrivate(),
					"SHA1WithRSA", caRef, holderRef, AuthorizationRoleEnum.CVCA, AccessRightEnum.READ_ACCESS_DG3_AND_DG4, validFrom,
					validTo, "BC");

			// Create the terminal certificate
			HolderReferenceField terminalHolderRef = new HolderReferenceField("NL", "RUDL-CVCT", "00001");

			CVCertificate terminalCvc = CertificateGenerator.createCertificate(
					terminalKeyPair.getPublic(), caKeyPair.getPrivate(),
					"SHA1WithRSA", caRef, terminalHolderRef,
					AuthorizationRoleEnum.IS, AccessRightEnum.READ_ACCESS_DG3_AND_DG4,
					validFrom, validTo, "BC");

			// Get the raw data from certificates and write to default files.
			// Overwrites the files without question!!!
			byte[] caCertData = caCvc.getDEREncoded();
			byte[] terminalCertData = terminalCvc.getDEREncoded();
			byte[] terminalPrivateKey = terminalKeyPair.getPrivate().getEncoded();

			File certsDir = new File(TEST_CV_CERT_DIR);
			if (!certsDir.exists()) { certsDir.mkdirs(); }
			writeFile(new File(certsDir, filenameCA), caCertData);
			writeFile(new File(certsDir, filenameTerminal), terminalCertData);
			writeFile(new File(certsDir, filenameKey), terminalPrivateKey);

			// Test - read the files again and parse its contents,
			// spit out the certificates

			CVCertificate c = readCVCertificateFromFile(new File(certsDir, filenameCA));
			if (c == null) {
				fail("could not read filenameCA as CVCertificate");
			}
			CVCertificateBody body = c.getCertificateBody();
			if (body == null) {
				fail("c's body is null");
			}
			String bodyText = body.getAsText();
			if (bodyText == null) {
				fail("c's body as text is null");
			}

			System.out.println("DEBUG: " + bodyText);

			c = readCVCertificateFromFile(new File(certsDir, filenameTerminal));
			assertNotNull(c);
			System.out.println(c.getCertificateBody().getAsText());

			JMRTDSecurityProvider.endPreferBouncyCastleProvider(jmrtdProviderIndex);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private CredentialsDir origNLSamples() throws Exception {
		String dir = "certs/orig";
		String[] fileNames = {
				"certCVCA_orig.cvcert",
				"certDVD_orig.cvcert",
				"certIS_orig.cvcert",
		"keyIS_orig.der" };
		return new CredentialsDir(dir, fileNames);
	}

	/**
	 * Creates a new keystore based on the original samples.
	 */
	public void testCreateOrigNLSamplesKeyStore() {
		try {
			testCreateKeyStore("BKS", origNLSamples(), new File("certs/orig/cvca.ks"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * And read them back.
	 */
	public void testReadFromKeyStoreOrig() {
		try {
			File certsDir = new File("certs/orig");
			File keyStoreFile = new File(certsDir, "cvca.ks");
			testReadFromKeyStore(keyStoreFile);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testCreateKeyStore(String storeType, CredentialsDir credentialsDir, File outFile) {
		try {
			/* Create empty keystore. */
			KeyStore outStore = null;
			if ("JDK".equals(storeType)) {
				outStore = KeyStore.getInstance(storeType);
			} else {
				outStore = KeyStore.getInstance(storeType, "BC");
			}
			outStore.load(null);

			Map<String, Key> keys = credentialsDir.getKeys();
			Map<String, Certificate> certificates = credentialsDir.getCertificates();

			/* DEBUG */
			for (Map.Entry<String, Certificate> certEntry: certificates.entrySet()) {
				String id = certEntry.getKey();
				Certificate cert = certEntry.getValue();
				CardVerifiableCertificate cvcCert = (CardVerifiableCertificate)cert;
				System.out.println("DEBUG: authref = " + cvcCert.getAuthorityReference());
				System.out.println("DEBUG: holderref = " + cvcCert.getHolderReference());

				System.out.println("pubkey.alg = " + cvcCert.getPublicKey().getAlgorithm());
				System.out.println("DEBUG: access in " + id + " = " + cvcCert.getAuthorizationTemplate().getAccessRight());
				byte[] signature = cvcCert.getSignature();
				System.out.println("DEBUG: signature (" + signature.length + ") = \n" + Hex.bytesToPrettyString(signature));
			}
			/* END DEBUG */

			for (Map.Entry<String, Key> entry: keys.entrySet()) {
				String keyAlias = entry.getKey();
				Key key = entry.getValue();
				if (key instanceof ECPrivateKey) {
					System.out.println("DEBUG: KEY " + keyAlias + ": " + key.getAlgorithm() + ", " + key.getClass().getCanonicalName());
					ECParameterSpec params = ((ECPrivateKey)key).getParams();
					BigInteger s = ((ECPrivateKey) key).getS();
					KeyFactory keyFactory = getECKeyFactory();
					// key = keyFactory.generatePrivate(new ECPrivateKeySpec(s, toNamedCurveSpec(params)));
				}
				System.out.println("DEBUG: KEY " + keyAlias + ": " + key.getAlgorithm() + ", " + key.getClass().getCanonicalName());

				Collection<Certificate> certValues = certificates.values();
				System.out.println("DEBUG: certValues = " + certValues);

				List<Certificate> chainList = asChainDebug(certValues);
				Certificate[] chain = new Certificate[chainList.size()];
				chainList.toArray(chain);
				
				System.out.println("DEBUG: chain for " + keyAlias + " = (" + chain.length + ") " +  Arrays.toString(chain));
				outStore.setKeyEntry(keyAlias, key, KEY_ENTRY_PASSWORD.toCharArray(), chain);
			}

			OutputStream outputStream = new FileOutputStream(outFile);
			outStore.store(outputStream, STORE_PASSWORD.toCharArray());
			outputStream.flush();
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//		ECParameterSpec params = null;
	//		for (String alias: aliases) {
	//			Certificate certificate = outStore.getCertificate(alias);
	//			PublicKey publicKey = certificate.getPublicKey();
	//			if (publicKey instanceof RSAPublicKey) {
	//			} else if (publicKey instanceof ECPublicKey) {
	//				ECParameterSpec publicKeyParams = ((ECPublicKey) publicKey).getParams();
	//				if (params == null) {
	//					params = publicKeyParams;
	//				} else {
	//					System.out.println("Found two sets of params in certs, hope they're equal");
	//				}
	//			}
	//		}
	//
	//		if (params == null) {
	//			throw new IllegalStateException("No params");
	//		}
	//
	//		for (String alias: aliases) {
	//			Certificate certificate = outStore.getCertificate(alias);
	//			if (!(certificate instanceof CardVerifiableCertificate)) {
	//				throw new IllegalStateException("Was expecting a CVC cert");
	//			}


	private CardVerifiableCertificate fixParams(CardVerifiableCertificate cvcCertificate, ECParameterSpec params) throws Exception {
		PublicKey publicKey = cvcCertificate.getPublicKey();
		if (!(publicKey instanceof ECPublicKey)) {
			throw new IllegalStateException("Was expecting ECPublicKey, found " + publicKey.getClass().getCanonicalName());
		}

		ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
		ECParameterSpec publicKeyParams = ecPublicKey.getParams();
		if (publicKeyParams != null) { return cvcCertificate; }
		ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(ecPublicKey.getW(), params);
		publicKey = KeyFactory.getInstance(publicKey.getAlgorithm()).generatePublic(ecPublicKeySpec); // is this always "EC"?

		/* Put pubkey in certificate */
		return new CardVerifiableCertificate(
				cvcCertificate.getAuthorityReference(),
				cvcCertificate.getHolderReference(),
				publicKey,
				publicKey.getAlgorithm(), // ???
				cvcCertificate.getNotBefore(),
				cvcCertificate.getNotAfter(),
				cvcCertificate.getAuthorizationTemplate().getRole(),
				cvcCertificate.getAuthorizationTemplate().getAccessRight(),
				cvcCertificate.getSignature());

	}

	private List<Certificate> asChainDebug(Collection<Certificate> certificates) {
		try {
			Certificate[] result = new Certificate[3];
			for (Certificate cert: certificates) {
				if (cert instanceof CardVerifiableCertificate) {
					CardVerifiableCertificate cvcCert = (CardVerifiableCertificate)cert;
					if ("CVCAA".equals(cvcCert.getHolderReference().getMnemonic())) {
						result[0] = cvcCert;
					} else if ("DVAAA".equals(cvcCert.getHolderReference().getMnemonic())) {
						result[1] = cvcCert;
					} else if ("IS7202000".equals(cvcCert.getHolderReference().getMnemonic())) {
						result[2] = cvcCert;
					} else {
						throw new IllegalStateException("Unexpected cert: " + cvcCert.getHolderReference().getMnemonic());
					}
				}
			}
			List<Certificate> resultList = Arrays.asList(result);
			System.out.println("DEBUG: resultList = " + resultList);
			return resultList;
		} catch (CertificateException ce) {
			ce.printStackTrace();
			throw new IllegalStateException(ce.getMessage());
		}
	}


	private List<Certificate> asChain(Collection<Certificate> certificates) {
		Certificate rootOfChain = null;
		for (Certificate certificate: certificates) {
			rootOfChain = certificate;
			break;
		}

		/* Not found, return empty chain. */
		if (rootOfChain == null) { return new ArrayList<Certificate>(); }

		/* Build chain, root first. */
		List<Certificate> chain = new ArrayList<Certificate>();
		chain.add(rootOfChain);
		boolean isFinished = false;
		while (!isFinished) {
			boolean addedSomethingNew = false;
			for (Certificate otherCertificate: certificates) {
				if (rootOfChain.equals(otherCertificate)) { continue; }
				if (isIssuerOf(otherCertificate, rootOfChain)) {
					rootOfChain = otherCertificate;
					chain.add(0, rootOfChain);
					addedSomethingNew = true;
					break;
				}
			}
			isFinished = !addedSomethingNew;
		}

		Collections.reverse(chain);

		return chain;
	}

	//	public List<? extends Certificate> testCertPath(Collection<Certificate> certificates) {
	//		try {
	//			Certificate root = null;
	//			Certificate leaf = null;
	//			for (Certificate other: certificates) {
	//				if (root == null || isIssuerOf(other, root)) { root = other; }
	//				if (leaf == null || isIssuerOf(leaf, other)) { leaf = other; }
	//			}
	//
	//			final Certificate theRealLeaf = leaf;
	//			
	//			Collection<Certificate> intermediateCerts = new HashSet<Certificate>(certificates);
	//			intermediateCerts.remove(root);
	//			intermediateCerts.remove(leaf);
	//
	//			CertStore intermediates = CertStore.getInstance("Collection", new CollectionCertStoreParameters(intermediateCerts));
	//
	//			CertSelector selector = new CertSelector() {
	//				public boolean match(Certificate cert) {
	//					return theRealLeaf.equals(cert);
	//				}
	//
	//				public Object clone() {
	//					return this;
	//				}
	//			};
	//
	//			KeyStore anchors = KeyStore.getInstance("JKS");
	//			anchors.load(null, "".toCharArray());
	//			anchors.setCertificateEntry("cvca", root);
	//
	//			CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", BC_PROVIDER);
	//			PKIXBuilderParameters  buildParams = new PKIXBuilderParameters(anchors, selector);
	//			buildParams.addCertStore(intermediates);
	//
	//			buildParams.setRevocationEnabled(false);
	//			PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult)builder.build(buildParams);
	//			assertNotNull(result);
	//			CertPath chain = result.getCertPath();
	//			assertNotNull(chain);
	//			return chain.getCertificates();
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//			fail(e.getMessage());
	//			return null;
	//		}
	//	}

	private boolean isIssuerOf(Certificate issuer, Certificate subject) {
		if (issuer instanceof X509Certificate && subject instanceof X509Certificate) {
			X509Certificate x509Issuer = (X509Certificate)issuer;
			X509Certificate x509Subject = (X509Certificate)subject;
			return x509Subject.getIssuerX500Principal().equals(x509Issuer.getSubjectX500Principal());
		} else if (issuer instanceof CardVerifiableCertificate && subject instanceof CardVerifiableCertificate) {
			try {
				CardVerifiableCertificate cvcIssuer = (CardVerifiableCertificate)issuer;
				CardVerifiableCertificate cvcSubject = (CardVerifiableCertificate)subject;
				return cvcSubject.getAuthorityReference().equals(cvcIssuer.getHolderReference());
			} catch (CertificateException ce) {
				ce.printStackTrace();
				return false;
			}
		} else {
			try {
				subject.verify(issuer.getPublicKey());
				return true;
			} catch (SignatureException se) {
				return false;
			} catch (Exception e) {
				/* FIXME: Hmmm, not supposed to happen? Or should SigException be GenSecException? */
				e.printStackTrace();
				return false;
			}
		}
	}

	public void testRSA() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);

			byte[] plainText = new byte[220];

			byte[] cipherText = cipher.doFinal(plainText);

			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] decryptedCipherText = cipher.doFinal(cipherText);

			assertTrue(Arrays.equals(plainText, decryptedCipherText));

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	private static Key getKey(File file) throws IOException {
		System.out.println("DEBUG: looking at key with name " + file.getName());
		byte[] keyBytes = new byte[(int)file.length()];
		DataInputStream keyInputStream = new DataInputStream(new FileInputStream(file));
		keyInputStream.readFully(keyBytes);
		keyInputStream.close();
		Key key = null;
		try {
			KeyFactory keyFactory = getRSAKeyFactory();
			key = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
			return key;
		} catch (Exception e) {		
		}
		try {
			KeyFactory keyFactory = getECKeyFactory();
			key = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
			ECPrivateKey ecPrivateKey = (ECPrivateKey)key;
			ECParameterSpec ecParamSpec = ecPrivateKey.getParams();
			System.out.println("DEBUG: ecParamSpec: " + ecParamSpec.getClass().getCanonicalName());
			// HIER: convert that to named paramspec.
			// ecParamSpec = toNamedCurveSpec(ecParamSpec);
			//			System.out.println("DEBUG: named ecParamSpec = " + ((ECNamedCurveSpec)ecParamSpec).getName());
			key = keyFactory.generatePrivate(new ECPrivateKeySpec(ecPrivateKey.getS(), ecParamSpec));
			return key;
		} catch (Exception e) {		
			e.printStackTrace();
		}
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("EC");
			key = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
			return key;
		} catch (Exception e) {		
		}
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("ECDH");
			key = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
			return key;
		} catch (Exception e) {	
		}

		return key;
	}

	public void testCreateKeyStore2() {
		try {
			String storeType ="JKS";
			int jmrtdProvIndex = JMRTDSecurityProvider.beginPreferBouncyCastleProvider();

			URI certsDirURI = new URI(WOJ_DIR);
			File certsDir = new File(certsDirURI.getPath());
			if (!certsDir.exists()) {
				certsDir.mkdirs();
			}
			if (!certsDir.isDirectory()) { fail("Certificate directory \"" + certsDir + "\" needs to be a directory!"); }
			String[] certFiles = certsDir.list(new FilenameFilter(){
				public boolean accept(File dir, String name) {
					return name.endsWith(".cvcert") || name.endsWith(".CVCERT"); 
				}

			});
			String[] keyFiles = certsDir.list(new FilenameFilter(){
				public boolean accept(File dir, String name) {
					return name.endsWith(".der") || name.endsWith(".DER"); 
				}

			});
			KeyStore outStore = KeyStore.getInstance(storeType);
			outStore.load(null);
			String keyAlgName = "RSA";
			for (String fileName: certFiles) {
				File file = new File(certsDir, fileName);
				System.out.println("DEBUG: reading cert from file " + file.getName() + " size " + file.length());
				Certificate certificate = CertificateFactory.getInstance("CVC", JMRTD_PROVIDER).generateCertificate(new FileInputStream(file));
				System.out.println("DEBUG: cert = " + toString(certificate));
				outStore.setCertificateEntry(fileName, certificate);
				keyAlgName = certificate.getPublicKey().getAlgorithm();
			}
			for (String fileName: keyFiles) {
				File file = new File(certsDir, fileName);
				System.out.println("DEBUG: reading key from file " + file.getName() + " size " + file.length());
				byte[] keyBytes = new byte[(int)file.length()];
				(new DataInputStream(new FileInputStream(file))).readFully(keyBytes);
				PrivateKey key = KeyFactory.getInstance(keyAlgName).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
				System.out.println("DEBUG: key = " + key);
				Certificate terminalCertificate = outStore.getCertificate("terminalcert1.cvcert");
				Certificate dvCertificate = outStore.getCertificate("terminalcert0.cvcert");
				String keyEntryAlias = ((CardVerifiableCertificate)dvCertificate).getHolderReference().getName();
				System.out.println("DEBUG: alias: "+keyEntryAlias);
				outStore.setKeyEntry(keyEntryAlias, key, KEY_ENTRY_PASSWORD.toCharArray(), new Certificate[] { dvCertificate, terminalCertificate });
			}

			System.out.println("DEBUG: entries in outStore: " + outStore.size());
			File outFile = new File(new URI(WOJ_KS).getPath());
			FileOutputStream out = new FileOutputStream(outFile);
			outStore.store(out, STORE_PASSWORD.toCharArray());
			out.flush();
			out.close();

			JMRTDSecurityProvider.endPreferBouncyCastleProvider(jmrtdProvIndex);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReadFromKeyStore() {
		File certsDir = new File(TEST_CV_CERT_DIR);
		File keyStoreFile = new File(certsDir, TEST_CV_KEY_STORE);	
		testReadFromKeyStore(keyStoreFile);
	}

	public void testReadFromKeyStore(File keyStoreFile) {
		try {
			int jmrtdProvIndex = JMRTDSecurityProvider.beginPreferBouncyCastleProvider();
			Security.insertProviderAt(JMRTD_PROVIDER, 0); // So that KeyStore knows about CVC certs

			KeyStore keyStore = KeyStore.getInstance("BKS");
			InputStream in = new FileInputStream(keyStoreFile);
			keyStore.load(in, STORE_PASSWORD.toCharArray());
			List<String> aliases = Collections.list(keyStore.aliases());
			System.out.println("DEBUG: aliases = " + aliases);
			assertEquals(aliases.size(), 1);
			Key key = keyStore.getKey(aliases.get(0), KEY_ENTRY_PASSWORD.toCharArray());
			System.out.println("DEBUG: key.class = " + key.getClass().getCanonicalName());

			JMRTDSecurityProvider.endPreferBouncyCastleProvider(jmrtdProvIndex);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private String toString(Certificate certificate) {
		if (certificate instanceof X509Certificate) {
			X509Certificate x509Certificate = (X509Certificate)certificate;
			StringBuffer result = new StringBuffer();
			result.append(x509Certificate.getSubjectX500Principal().getName());
			result.append(" ");
			String algorithm = x509Certificate.getPublicKey().getAlgorithm();
			result.append("pubkey.algorithm = " + algorithm);
			return result.toString();
		} else if (certificate instanceof CardVerifiableCertificate) {
			try {
				return ((CardVerifiableCertificate)certificate).getHolderReference().getName();
			} catch (CertificateException ce) {
				ce.printStackTrace();
				return "ERROR: CVCertificate ?!?!" + certificate.toString();
			}
		} else {
			return "Non-X509" + certificate.toString();
		}
	}

	public static CVCertificate readCVCertificateFromFile(File f) {
		try {
			DataInputStream dataIn = new DataInputStream(new FileInputStream(f));
			byte[] data = new byte[(int)f.length()];
			dataIn.readFully(data);
			CVCObject parsedObject = CertificateParser.parseCertificate(data);
			CVCertificate c = (CVCertificate) parsedObject;
			dataIn.close();
			return c;
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Writes raw data to a file.
	 * 
	 * @param path
	 *            path to the file to be written (no overwrite checks!)
	 * @param data
	 *            raw data to be written
	 * @throws IOException
	 *             if something goes wrong
	 */
	public static void writeFile(File file, byte[] data) throws IOException {
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(file);
			outStream.write(data);
		} finally {
			if (outStream != null)
				outStream.close();
		}

	}

	private static String lookupMnemonicByOID(String oid) throws NoSuchAlgorithmException {
		if (oid.equals(X9_SHA1_WITH_ECDSA_OID)) { return "SHA1withECDSA"; }
		if (oid.equals(X9_SHA224_WITH_ECDSA_OID)) { return "SHA224withECDSA"; }
		if (oid.equals(X9_SHA256_WITH_ECDSA_OID)) { return "SHA256withECDSA"; }		
		if (oid.equals(PKCS1_RSA_OID)) { return "RSA"; }
		if (oid.equals(PKCS1_MD2_WITH_RSA_OID)) { return "MD2withRSA"; }
		if (oid.equals(PKCS1_MD4_WITH_RSA_OID)) { return "MD4withRSA"; }
		if (oid.equals(PKCS1_MD5_WITH_RSA_OID)) { return "MD5withRSA"; }
		if (oid.equals(PKCS1_SHA1_WITH_RSA_OID)) { return "SHA1withRSA"; }
		if (oid.equals(PKCS1_SHA256_WITH_RSA_OID)) { return "SHA256withRSA"; }
		if (oid.equals(PKCS1_SHA384_WITH_RSA_OID)) { return "SHA384withRSA"; }
		if (oid.equals(PKCS1_SHA512_WITH_RSA_OID)) { return "SHA512withRSA"; }
		if (oid.equals(PKCS1_SHA224_WITH_RSA_OID)) { return "SHA224withRSA"; }
		if (oid.equals(BSI_EAC_TA_SHA224_WITH_ECDSA_OID)) { return "SHA224withECDSA"; }
		if (oid.equals(BSI_EAC_TA_SHA256_WITH_ECDSA_OID)) { return "SHA256withECDSA"; }
		if (oid.equals(BSI_EAC_TA_SHA384_WITH_ECDSA_OID)) { return "SHA384withECDSA"; }
		if (oid.equals(BSI_EAC_TA_SHA512_WITH_ECDSA_OID)) { return "SHA512withECDSA"; }
		throw new NoSuchAlgorithmException(oid);
	}

	public void testKeyStore() throws Exception{
		try {
			String storeType = "PKCS12";
			String storeName =  "c:/outstore.pkcs12";
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(1024);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();
			Certificate trustCert =  createX509Certificate("CN=CA", "CN=CA", publicKey, privateKey, "SHA1withRSA");
			Certificate[] outChain = { createX509Certificate("CN=Client", "CN=CA", publicKey, privateKey, "SHA1withRSA"), trustCert };

			KeyStore outStore = KeyStore.getInstance(storeType);
			outStore.load(null, "secret".toCharArray());
			outStore.setKeyEntry("mykey", privateKey, "secret".toCharArray(), outChain);
			OutputStream outputStream = new FileOutputStream(storeName);
			outStore.store(outputStream, "secret".toCharArray());
			outputStream.flush();
			outputStream.close();

			KeyStore inStore = KeyStore.getInstance(storeType);
			inStore.load(new FileInputStream(storeName), "secret".toCharArray());
			Key key = outStore.getKey("myKey", "secret".toCharArray());
			assertEquals(privateKey, key);

			Certificate[] inChain = outStore.getCertificateChain("mykey");
			assertNotNull(inChain);
			assertEquals(outChain.length, inChain.length);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError(e.getMessage());
		}
	}

	public void testCreateCert() {
		try {
			ECNamedCurveParameterSpec bcParamSpec = ECNamedCurveTable.getParameterSpec("brainpoolp224r1");
			ECNamedCurveSpec jceParamSpec = new ECNamedCurveSpec(bcParamSpec.getName(), bcParamSpec.getCurve(), bcParamSpec.getG(), bcParamSpec.getN(), bcParamSpec.getH(), bcParamSpec.getSeed());

			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
			keyPairGenerator.initialize(jceParamSpec);

			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();
			PrivateKey dvdSigningKey = keyPair.getPrivate(); // FIXME
			String signatureAlgorithm = "SHA224withECDSA";
			CardVerifiableCertificate cert = createISCertificate("NLIS7202000NL001", "NLDVAAANL001", publicKey, dvdSigningKey, signatureAlgorithm);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private static CardVerifiableCertificate createISCertificate(String subject, String issuer, PublicKey publicKey, PrivateKey dvdSigningKey, String algorithm) {
		try {
			CVCPrincipal authorityReference = new CVCPrincipal(issuer);
			CVCPrincipal holderReference = new CVCPrincipal(subject);
			Date notBefore = CALENDAR.getTime();
			Date notAfter = CALENDAR.getTime();

			Role role = CVCAuthorizationTemplate.Role.IS;
			Permission permission = CVCAuthorizationTemplate.Permission.READ_ACCESS_DG3;

			byte[] signatureData = null;

			CardVerifiableCertificate cert = new CardVerifiableCertificate(authorityReference, holderReference, publicKey, algorithm, notBefore, notAfter, role, permission, signatureData);
			byte[] body = cert.getCertBodyData();
			Signature signature = Signature.getInstance(algorithm);
			signature.initSign(dvdSigningKey);
			signature.update(body);
			signatureData = signature.sign();
			cert = new CardVerifiableCertificate(authorityReference, holderReference, publicKey, algorithm, notBefore, notAfter, role, permission, signatureData);
			return cert;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static X509Certificate[] createX509Chain(int depth, String subject, PublicKey caPublicKey, PrivateKey caPrivateKey, int keyLength, String keyAlg, String signatureAlgorithm) throws Exception {
		X509Certificate[] result = new X509Certificate[depth];

		/* Root certificate */
		String issuer = subject;

		Date dateOfIssuing = CALENDAR.getTime();
		Date dateOfExpiry = CALENDAR.getTime();

		PrivateKey signingKey = null;

		for (int i = 0; i < depth; i++) {
			PublicKey publicKey = null;
			PrivateKey privateKey = null;
			if (i == 0) {
				publicKey = caPublicKey;
				privateKey = caPrivateKey;
			} else {
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyAlg);
				keyPairGenerator.initialize(keyLength);
				KeyPair keyPair = keyPairGenerator.generateKeyPair();
				publicKey = keyPair.getPublic();
				privateKey = keyPair.getPrivate();
			}
			if (signingKey == null) { signingKey = caPrivateKey; } /* Root is self-signed */

			X509Certificate certificate = createX509Certificate(issuer, subject, dateOfIssuing, dateOfExpiry, publicKey, signingKey, signatureAlgorithm);
			result[result.length - i - 1] = certificate;

			/* Next certificate */
			issuer = subject;
			subject = "CN=SubjectOf" + issuer.substring("CN=".length());
			signingKey = privateKey;
		}

		return result;
	}

	//	private static CardVerifiableCertificate createCardVerifiableCertificate(String issuer, String subject, Date dateOfIssuing, Date dateOfExpiry,
	//			PublicKey publicKey, PrivateKey privateKey, String signatureAlgorithm) throws Exception {
	//		CVCPrincipal authorityReference = new CVCPrincipal(issuer);
	//		CVCPrincipal holderReference = new CVCPrincipal(subject);
	//		Role role = Role.IS;
	//		Permission permission = Permission.READ_ACCESS_DG3_AND_DG4;
	//		return new CardVerifiableCertificate(authorityReference, holderReference, publicKey, signatureAlgorithm, dateOfIssuing, dateOfExpiry, role, permission, signatureData);
	//	}

	private static X509Certificate createX509Certificate(String issuer, String subject, Date dateOfIssuing, Date dateOfExpiry,
			PublicKey publicKey, PrivateKey privateKey, String signatureAlgorithm) throws Exception {
		X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
		certGenerator.setSerialNumber(new BigInteger("1"));
		certGenerator.setIssuerDN(new X509Name(issuer));
		certGenerator.setSubjectDN(new X509Name(subject));
		certGenerator.setNotBefore(dateOfIssuing);
		certGenerator.setNotAfter(dateOfExpiry);
		certGenerator.setPublicKey(publicKey);
		certGenerator.setSignatureAlgorithm(signatureAlgorithm);
		X509Certificate certificate = (X509Certificate)certGenerator.generate(privateKey, "BC");
		return certificate;
	}

	public void testStoreAndReadECPrivateKey() {
		try {
			String storeType = "JKS", storePass = "secret", storePath = "c:/keystore.ks";

			ECNamedCurveParameterSpec bcParamSpec = ECNamedCurveTable.getParameterSpec("brainpoolp224r1");
			ECNamedCurveSpec jceParamSpec = new ECNamedCurveSpec(bcParamSpec.getName(), bcParamSpec.getCurve(), bcParamSpec.getG(), bcParamSpec.getN(), bcParamSpec.getH(), bcParamSpec.getSeed());

			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
			keyPairGenerator.initialize(jceParamSpec);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			ECPublicKey publicKey = (ECPublicKey)keyPair.getPublic();
			ECPrivateKey privateKey = (ECPrivateKey)keyPair.getPrivate();		

			Certificate trustCert =  createX509Certificate("CN=CA", "CN=CA", publicKey, privateKey, "SHA224withECDSA");
			Certificate[] chain = { createX509Certificate("CN=Client", "CN=CA", publicKey, privateKey, "SHA224withECDSA"), trustCert };

			KeyStore keyStore = KeyStore.getInstance(storeType);
			keyStore.load(null, storePass.toCharArray());
			keyStore.setKeyEntry("eckey", privateKey, storePass.toCharArray(), chain);

			FileOutputStream outputStream = new FileOutputStream(storePath);
			keyStore.store(outputStream, storePass.toCharArray());
			outputStream.close();

			/* Now read it back */			
			FileInputStream inputStream = new FileInputStream(storePath);
			KeyStore keyStore2 = KeyStore.getInstance(storeType);
			keyStore2.load(inputStream, storePass.toCharArray());

			Key privateKey2 = keyStore2.getKey("eckey", storePass.toCharArray());
			assertEquals(privateKey, privateKey2);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private static X509Certificate createX509Certificate(String dn, String issuer, PublicKey publicKey, PrivateKey privateKey, String sigAlg) throws Exception {
		X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
		certGenerator.setSerialNumber(BigInteger.valueOf(Math.abs(new Random().nextLong())));
		//        certGenerator.setIssuerDN(new X509Name(dn));
		certGenerator.setIssuerDN(new X509Name(issuer)); // Set issuer!
		certGenerator.setSubjectDN(new X509Name(dn));
		certGenerator.setNotBefore(Calendar.getInstance().getTime());
		certGenerator.setNotAfter(Calendar.getInstance().getTime());
		certGenerator.setPublicKey(publicKey);
		certGenerator.setSignatureAlgorithm(sigAlg);
		X509Certificate certificate = (X509Certificate)certGenerator.generate(privateKey, "BC");
		return certificate;
	}

	public void testNamedCurve() {
		try {
			ECNamedCurveParameterSpec secp224r1Spec = ECNamedCurveTable.getParameterSpec("secp224r1");
			assertNotNull(secp224r1Spec);

			ECNamedCurveParameterSpec brainpool224r1Spec = ECNamedCurveTable.getParameterSpec("brainpoolp224r1");
			assertNotNull(brainpool224r1Spec);

			//			assertEquals(secp224r1Spec.getCurve().getA(), brainpool224r1Spec.getCurve().getA());


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ECNamedCurveSpec toNamedCurveSpec(ECParameterSpec ecParamSpec) {
		if (ecParamSpec == null) { return null; }
		if (ecParamSpec instanceof ECNamedCurveSpec) { return (ECNamedCurveSpec)ecParamSpec; }
		List<String> names = (List<String>)Collections.list(ECNamedCurveTable.getNames());
		List<ECNamedCurveSpec> namedSpecs = new ArrayList<ECNamedCurveSpec>();
		for (String name: names) {
			ECNamedCurveParameterSpec namedParamSpec = ECNamedCurveTable.getParameterSpec(name);
			ECCurve curve = namedParamSpec.getCurve();
			ECPoint generator = namedParamSpec.getG();
			BigInteger order = namedParamSpec.getN();
			BigInteger coFactor = namedParamSpec.getH();
			byte[] seed = namedParamSpec.getSeed();

			ECNamedCurveSpec namedSpec = new ECNamedCurveSpec(name, curve, generator, order, coFactor, seed);
			if (namedSpec.getCurve().equals(ecParamSpec.getCurve())
					&& namedSpec.getGenerator().equals(ecParamSpec.getGenerator())
					&& namedSpec.getOrder().equals(ecParamSpec.getOrder())
					&& namedSpec.getCofactor() == ecParamSpec.getCofactor()
					) {
				namedSpecs.add(namedSpec);
			}
		}
		if (namedSpecs.size() == 0) {
			throw new IllegalArgumentException("No named curve found");
		} else if (namedSpecs.size() == 1) {
			return namedSpecs.get(0);
		} else {
			System.out.println("DEBUG: found " + namedSpecs);
			return namedSpecs.get(0);
		}
	}

	private static Certificate getCertificate(File file) throws IOException, CertificateException {
		if (cvCertificateFactory == null) { cvCertificateFactory = getCVCCertificateFactory(); }
		return cvCertificateFactory.generateCertificate(new FileInputStream(file));
	}

	/*
	 * We're assuming those files contain either a cvc-cert or a key,
	 * and that the cvc-certs form a chain, and that the files are named
	 * as
	 * <pre>
	 *    cert[Alias].cvcert
	 *    key[Alias].der
	 * </pre>
	 */
	static class CredentialsDir {
		private Map<String, Certificate> certificates;
		private Map<String, Key> keys;

		public CredentialsDir(String dir, String[] fileNames) throws Exception {
			certificates = new HashMap<String, Certificate>();
			keys = new HashMap<String, Key>();

			for (String fileName: fileNames) {
				fileName = fileName.trim();
				File file = new File(dir, fileName);
				if (!file.isFile()) { continue; /* NOTE: ignore sub-directories */ }
				if (fileName.startsWith("cert") && fileName.endsWith(".cvcert")) {
					String alias = fileName;
					alias = alias.substring(0, alias.length() - ".cvcert".length());
					alias = alias.substring("cert".length());
					System.out.println("DEBUG: cert entry alias = " + alias);
					Certificate certificate = getCertificate(file);
					certificates.put(alias, certificate);
				} else if (fileName.startsWith("key") && fileName.endsWith(".der")) {
					String alias = fileName;
					alias = alias.substring(0, alias.length() - ".der".length());
					alias = alias.substring("key".length());
					System.out.println("DEBUG: key entry alias = " + alias);

					/* Try to interpret it as a key */
					Key key = getKey(file);
					System.out.println("DEBUG: key alg " + key.getAlgorithm());
					keys.put(alias, key);
				}
			}
		}

		public Map<String, Certificate> getCertificates() { return certificates; }

		public Map<String, Key> getKeys() { return keys; }


	}
}
