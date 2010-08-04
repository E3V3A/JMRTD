package org.jmrtd.test.api.cert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import junit.framework.TestCase;

import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.cert.CVCertificate;
import org.jmrtd.cert.KeyStoreCertStoreParameters;

public class KeyStoreCertStoreTest extends TestCase
{
	private static final Provider
	BC_PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider(),
	JMRTD_PROVIDER = new JMRTDSecurityProvider();

	//	private static final String TEST_KEY_STORE = "file:/d:/csca.ks";

	private static final String TEST_CERT_DIR = "file:/d:/ca/icao/csca";
	
	private static final String TEST_CV_CERT_DIR = "file:/d:/ca/cvcert";
	
	private static final String TEST_CV_DEST_KEY_STORE = "file:/d:/ca/cvcert/cvca.ks";

	private static final String TEST_DEST_KEY_STORE = "file:/d:/ca/icao/csca/csca.ks";

	private static final String TEST_SRC_KEY_STORE = TEST_DEST_KEY_STORE;

	private static final CertSelector ALL_SELECTOR = new CertSelector() {
		public boolean match(Certificate cert) { return true; }
		public Object clone() { return this; }
	};

	public void testImportCVCertificates() {
		try {
			URI certsDirURI = new URI(TEST_CV_CERT_DIR);
			File certsDir = new File(certsDirURI.getPath());
			if (!certsDir.isDirectory()) { fail("Certs dir needs to be a directory!"); }
			String[] files = certsDir.list();
			KeyStore outStore = KeyStore.getInstance("JKS");
			outStore.load(null);
			for (String fileName: files) {
				File file = new File(certsDir, fileName);
				if (file.isFile() && fileName.endsWith(".cvcert")) {
					System.out.println("DEBUG: file " + file.getName() + " size " + file.length());
					Certificate certificate =
						CertificateFactory.getInstance("CVC", JMRTD_PROVIDER).generateCertificate(new FileInputStream(file));
					System.out.println("DEBUG: cert = " + toString(certificate));
					 outStore.setCertificateEntry(fileName, certificate);
				}
			}
			System.out.println("DEBUG: certs in outStore: " + outStore.size());
			File outFile = new File(new URI(TEST_CV_DEST_KEY_STORE).getPath());
			FileOutputStream out = new FileOutputStream(outFile);
			outStore.store(out, "".toCharArray());
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testImportCertificates() {
		try {
			URI certsDirURI = new URI(TEST_CERT_DIR);
			File certsDir = new File(certsDirURI.getPath());
			if (!certsDir.isDirectory()) { fail("Certs dir needs to be a directory!"); }
			String[] files = certsDir.list();
			KeyStore outStore = KeyStore.getInstance("PKCS12", BC_PROVIDER);
			outStore.load(null);
			for (String fileName: files) {
				File file = new File(certsDir, fileName);
				if (file.isFile() && fileName.endsWith(".cer")) {
					System.out.println("DEBUG: file " + file.getName() + " size " + file.length());
					Certificate certificate =
						CertificateFactory.getInstance("X509", BC_PROVIDER).generateCertificate(new FileInputStream(file));
					System.out.println("DEBUG: cert = " + toString(certificate));
					outStore.setCertificateEntry(fileName, certificate);
				}
			}
			System.out.println("DEBUG: certs in outStore: " + outStore.size());
			File outFile = new File(new URI(TEST_DEST_KEY_STORE).getPath());
			FileOutputStream out = new FileOutputStream(outFile);
			outStore.store(out, "".toCharArray());
			out.flush();
			out.close();
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
			URI pkcs12Store = new URI(TEST_SRC_KEY_STORE);
			CertStore certStore = CertStore.getInstance("PKCS12", new KeyStoreCertStoreParameters(pkcs12Store, "PKCS12"), JMRTD_PROVIDER);
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
			return ((X509Certificate)certificate).getSubjectX500Principal().getName();
		} else if (certificate instanceof CVCertificate) {
			try {
				return ((CVCertificate)certificate).getHolderReference().getName();
			} catch (CertificateException ce) {
				ce.printStackTrace();
				return "ERROR: CVCertificate ?!?!" + certificate.toString();
			}
		} else {
			return "Non-X509" + certificate.toString();
		}
	}
}
