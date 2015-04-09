package org.jmrtd.test.api.cert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import junit.framework.TestCase;

import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.cert.KeyStoreCertStoreParameters;

public class CSCAStoreGenerator extends TestCase {

	private static final Provider
	BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider(),
	JMRTD_PROVIDER = JMRTDSecurityProvider.getInstance();

	//	private static final String TEST_KEY_STORE = "file:/d:/csca.ks";

	static {
		Security.addProvider(BC_PROVIDER);
	}

	private static final String
	STORE_PASSWORD = "",
	KEY_ENTRY_PASSWORD = "";

	private static final String TEST_CERT_DIR = "file:/t:/ca/icao/csca";

	private static final String TEST_DEST_KEY_STORE = "file:/t:/ca/icao/csca/csca.ks";

	private static final String TEST_SRC_KEY_STORE = TEST_DEST_KEY_STORE;

	private static final CertSelector ALL_SELECTOR = new CertSelector() {
		public boolean match(Certificate cert) { return true; }
		public Object clone() { return this; }
	};

	public void testImportX509Certificates() {
		testImportX509Certificates(TEST_CERT_DIR, TEST_DEST_KEY_STORE);
	}

	/** Converts a directory with .cer files into a keystore. */
	public void testImportX509Certificates(String certsDirPath, String testDestKeyStore) {
		try {
			int jmrtdProvIndex = JMRTDSecurityProvider.beginPreferBouncyCastleProvider();

			certsDirPath = getPath(certsDirPath);
			File certsDirFile = new File(certsDirPath);
			if (!certsDirFile.exists()) { certsDirFile.mkdirs(); }
			if (!certsDirFile.isDirectory()) { fail("Certs dir needs to be a directory!"); }
			String[] files = certsDirFile.list();
			KeyStore outStore = KeyStore.getInstance("BKS");
			outStore.load(null);
			for (String fileName: files) {
				File file = new File(certsDirFile, fileName);
				if (file.isFile() && fileName.endsWith(".cer")) {
					System.out.println("DEBUG: file " + file.getName() + " size " + file.length());
					X509Certificate certificate =
							(X509Certificate)CertificateFactory.getInstance("X509", BC_PROVIDER).generateCertificate(new FileInputStream(file));
					System.out.println("DEBUG: file " + fileName + ", cert = " + toString(certificate));

					PublicKey publicKey = certificate.getPublicKey();
					String algorithm = publicKey.getAlgorithm();
					outStore.setCertificateEntry(fileName, certificate);
				}
			}
			System.out.println("DEBUG: certs in outStore: " + outStore.size());
			
			testDestKeyStore = getPath(testDestKeyStore);
			
			System.out.println("DEBUG: using testDestKeyStore " + testDestKeyStore);
			
			File outFile = new File(testDestKeyStore);
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

	/**
	 * Reads a JKS certificates file as a cert store using JMRTD security
	 * provider's KeyStoreCertStoreSpi.
	 */
	public void testReadKeyStoreAsX509CertStore() {
		try {
			Security.removeProvider(BC_PROVIDER.getName());
			Security.insertProviderAt(BC_PROVIDER, 1); /* TODO: Can we make KeyStoreCertStoreSpi such that this is not necessary? */
			URI keyStore = new URI(TEST_SRC_KEY_STORE);
			CertStore certStore = CertStore.getInstance("JKS", new KeyStoreCertStoreParameters(keyStore, "JKS", STORE_PASSWORD.toCharArray()), JMRTD_PROVIDER);
			System.out.println("DEBUG: certStore =\n");
			Collection<? extends Certificate> certificates = certStore.getCertificates(ALL_SELECTOR);
			System.out.println("DEBUG: certs in certStore: " + certificates.size());
			for (Certificate certificate: certificates) {
				System.out.println(toString(certificate));
			}
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

	private String getPath(String path) {
		if (path.startsWith("file:")) {
			try {
				URI uriToPath = new URI(path);

				/* It's a URI, get the path, we only support file as scheme. */
				path = uriToPath.getPath();
			} catch (Exception e) {
				/* It's not a URI, try to interpret it as a file path. */
			}
		}
		return path;
	}
}
