package org.jmrtd.test.api.cert;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

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
import org.jmrtd.cert.CardVerifiableCertificate;

public class CVCAStoreGenerator extends TestCase
{	
	private static final Provider
	BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider(),
	JMRTD_PROVIDER = JMRTDSecurityProvider.getInstance();

	static {
		Security.addProvider(BC_PROVIDER);
	}
	
	//	private static final String TEST_KEY_STORE = "file:/c:/csca.ks";

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
			HolderReferenceField holderRef = new HolderReferenceField(caRef
					.getCountry(), caRef.getMnemonic(), caRef.getSequence());

			// Create the CA certificate
			CVCertificate caCvc = CertificateGenerator.createCertificate(
					caKeyPair.getPublic(), caKeyPair.getPrivate(),
					"SHA1WithRSA", caRef, holderRef,AuthorizationRoleEnum.CVCA, AccessRightEnum.READ_ACCESS_DG3_AND_DG4, validFrom,
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

	public void testCreateKeyStore() {
		try {
			File certsDir = new File(TEST_CV_CERT_DIR);
			if (!certsDir.exists()) { certsDir.mkdirs(); }
			if (!certsDir.isDirectory()) { fail("Certs dir needs to be a directory!"); }
			String[] files = certsDir.list();
			KeyStore outStore = KeyStore.getInstance("JKS");
			outStore.load(null);
			for (String fileName: files) {
				File file = new File(certsDir, fileName);
				if (file.isFile()) {
					if (fileName.endsWith(".cvcert")) {
						System.out.println("DEBUG: reading cert from file " + file.getName() + " size " + file.length());
						Certificate certificate =
							CertificateFactory.getInstance("CVC", JMRTD_PROVIDER).generateCertificate(new FileInputStream(file));
						System.out.println("DEBUG: cert = " + toString(certificate));
						outStore.setCertificateEntry(fileName, certificate);
					} else if (fileName.endsWith(".der")) {
						System.out.println("DEBUG: reading key from file " + file.getName() + " size " + file.length());
						byte[] keyBytes = new byte[(int)file.length()];
						(new DataInputStream(new FileInputStream(file))).readFully(keyBytes);
						PrivateKey key =
							KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
						System.out.println("DEBUG: key = " + key);
						Certificate terminalCertificate = outStore.getCertificate("terminalcert.cvcert");
						Certificate caCertificate = outStore.getCertificate("cacert.cvcert");
						String keyEntryAlias = ((CardVerifiableCertificate)terminalCertificate).getHolderReference().getName();
						outStore.setKeyEntry(keyEntryAlias, key, KEY_ENTRY_PASSWORD.toCharArray(), new Certificate[] { terminalCertificate, caCertificate });
					}
				}
			}
			System.out.println("DEBUG: entries in outStore: " + outStore.size());
			File outFile = new File(certsDir, TEST_CV_KEY_STORE);
			FileOutputStream out = new FileOutputStream(outFile);
			outStore.store(out, STORE_PASSWORD.toCharArray());
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testCreateKeyStore2() {
		try {
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
			KeyStore outStore = KeyStore.getInstance("JKS");
			outStore.load(null);
			String keyAlgName = "RSA";
			for (String fileName: certFiles) {
				File file = new File(certsDir, fileName);
				System.out.println("DEBUG: reading cert from file " + file.getName() + " size " + file.length());
				Certificate certificate =
					CertificateFactory.getInstance("CVC", JMRTD_PROVIDER).generateCertificate(new FileInputStream(file));
				System.out.println("DEBUG: cert = " + toString(certificate));
				outStore.setCertificateEntry(fileName, certificate);
				keyAlgName = certificate.getPublicKey().getAlgorithm();
			}
			for (String fileName: keyFiles) {
				File file = new File(certsDir, fileName);
				System.out.println("DEBUG: reading key from file " + file.getName() + " size " + file.length());
				byte[] keyBytes = new byte[(int)file.length()];
				(new DataInputStream(new FileInputStream(file))).readFully(keyBytes);
				PrivateKey key =
					KeyFactory.getInstance(keyAlgName).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
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
		try {
			testCreateKeyStore();
			
			int jmrtdProvIndex = JMRTDSecurityProvider.beginPreferBouncyCastleProvider();
			Security.addProvider(JMRTD_PROVIDER); // So that KeyStore knows about CVC certs

			File certsDir = new File(TEST_CV_CERT_DIR);
			if (!certsDir.exists()) {
				certsDir.mkdirs();
			}
			File keyStoreFile = new File(certsDir, TEST_CV_KEY_STORE);			
			KeyStore keyStore = KeyStore.getInstance("JKS");
			InputStream in = new FileInputStream(keyStoreFile);
			keyStore.load(in, STORE_PASSWORD.toCharArray());	
			keyStore.getKey("terminalkey.der", KEY_ENTRY_PASSWORD.toCharArray());

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
			byte[] data = loadFile(f);
			CVCObject parsedObject = CertificateParser.parseCertificate(data);
			CVCertificate c = (CVCertificate) parsedObject;
			return c;
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Reads the byte data from a file.
	 * 
	 * @param path
	 *            the path to the file
	 * @return the raw contents of the file
	 * @throws IOException
	 *             if there are problems
	 */
	public static byte[] loadFile(String path) throws IOException {
		return loadFile(new File(path));
	}

	/**
	 * Reads the byte data from a file.
	 * 
	 * @param file
	 *            the file object to read data from
	 * @return the raw contents of the file
	 * @throws IOException
	 *             if there are problems
	 */
	public static byte[] loadFile(File file) throws IOException {
		byte[] dataBuffer = null;
		FileInputStream inStream = null;
		try {
			// Simple file loader... <-- Woj, you call this simple? Check DataInputStream.readFully(byte[]) ;) -- MO
			int length = (int) file.length();
			dataBuffer = new byte[length];
			inStream = new FileInputStream(file);

			int offset = 0;
			int readBytes = 0;
			boolean readMore = true;
			while (readMore) {
				readBytes = inStream.read(dataBuffer, offset, length - offset);
				offset += readBytes;
				readMore = readBytes > 0 && offset != length;
			}
		} finally {
			try {
				if (inStream != null)
					inStream.close();
			} catch (IOException e1) {
				System.out.println("loadFile - error when closing: " + e1);
			}
		}
		return dataBuffer;
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
	public static void writeFile(String path, byte[] data) throws IOException {
		writeFile(new File(path), data);
	}

	/**
	 * Writes raw data to a file.
	 * 
	 * @param file
	 *            the file object to be written (no overwrite checks!)
	 * @param data
	 *            raw data to be written
	 * @throws IOException
	 *             if something goes wrong
	 */
	public static void writeFile(File file, byte[] data) throws IOException {
		FileOutputStream outStream = null;
		BufferedOutputStream bout = null;
		try {
			outStream = new FileOutputStream(file);
			bout = new BufferedOutputStream(outStream, 1000); // Why buffer it? -- MO
			bout.write(data);
		} finally {
			if (bout != null)
				bout.close();
		}
	}
}
