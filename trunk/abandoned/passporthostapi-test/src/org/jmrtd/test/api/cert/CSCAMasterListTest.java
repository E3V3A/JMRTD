package org.jmrtd.test.api.cert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;
import net.sourceforge.scuba.data.Country;

import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.cert.CSCAMasterList;

public class CSCAMasterListTest extends TestCase {

	private static final String TEST_CERT_DIR = "tmp/csca";

	private static final String DE_ML_URL = "https://www.bsi.bund.de/SharedDocs/Downloads/EN/BSI/CSCA/GermanMasterList.zip?__blob=publicationFile";

//	private static final String DE_ML_FILE =
//			TEST_CERT_DIR
//			// + "/german_csca_masterlist/20130614GermanMasterlist.ml";
//			// + "/german_csca_masterlist/20130717GermanMasterlist.ml";
//			// + "/german_csca_masterlist/20130815GermanMasterlist.ml";
//			//			+ "/german_csca_masterlist/20131217_GermanMasterlist.ml";
//			+ "/german_csca_masterlist/20140319_GermanMasterList.ml";

	private static final String DE_PREFIX = "from_de_ml_";

	private static final String CH_PREFIX = "from_ch_ml_";

	private static final String CH_ML_FILE =
			TEST_CERT_DIR + "/swiss_csca_masterlist/chMasterlist.ml";

	private static final String DE_CERTS_OUTPUT_DIR = TEST_CERT_DIR + "/german_csca_masterlist";

	private static final String CH_CERTS_OUTPUT_DIR = TEST_CERT_DIR + "/swiss_csca_masterlist";

	/** Expects the German ML in a file. */
//	public void testGermanCSCAMasterList() {
//		try {
//			testCSCAMasterList(DE_ML_FILE, DE_PREFIX, DE_CERTS_OUTPUT_DIR);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}

	public void testSwissMasterList() {
		try {
			testCSCAMasterList(CH_ML_FILE, CH_PREFIX, CH_CERTS_OUTPUT_DIR);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/** Expects the German ML at the BSI site. */
	public void testGermanCSCAMasterListURL() {
		try {
			testGermanCSCAMasterListURL(DE_ML_URL, DE_PREFIX, DE_CERTS_OUTPUT_DIR);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGermanCSCAMasterListURL(String urlString, String prefix, String outputDir) throws Exception {
		try {
			URL url = new URL(urlString);
			ZipInputStream zipInputStream = new ZipInputStream(url.openStream());
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			byte[] bytes = new byte[(int)zipEntry.getSize()];
			DataInputStream dataInputStream = new DataInputStream(zipInputStream);
			dataInputStream.readFully(bytes);
			dataInputStream.close();
			CSCAMasterList cscaMasterList = new CSCAMasterList(bytes);
			testCSCAMasterList(cscaMasterList, prefix, outputDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testCSCAMasterList(String fileName, String prefix, String outputDir) throws Exception {
		File germanMLFile = new File(fileName);
		int length = (int)germanMLFile.length();
		byte[] bytes = new byte[length];
		DataInputStream dataIn = new DataInputStream(new FileInputStream(germanMLFile));
		dataIn.readFully(bytes);
		dataIn.close();
		CSCAMasterList cscaMasterList = new CSCAMasterList(bytes);
		testCSCAMasterList(cscaMasterList, prefix, outputDir);
	}

	public void testCSCAMasterList(CSCAMasterList cscaMasterList, String prefix, String outputDirPath) throws Exception {
		/* Convert to key store. */
		Map<String, X509Certificate> certificates = readCSCAMasterList(cscaMasterList, prefix);
		KeyStore outStore = toKeyStore(certificates);

		/* Prepare output directory. */
		File outputDir = new File(outputDirPath);

		if (!outputDir.exists()) {
			System.out.println("DEBUG: output dir " + outputDirPath + " doesn't exist, creating it.");
			if (!outputDir.mkdirs()) {
				fail("Could not create output dir \"" + outputDirPath + "\"");
				throw new IllegalStateException();
			}			
		}
		
		if (!outputDir.isDirectory()) {
			fail("Output dir is not a directory");
			throw new IllegalArgumentException();
		}
		

		/* Write to keystore. */
		File outFile = new File(outputDir, "csca.ks");
		FileOutputStream out = new FileOutputStream(outFile);
		outStore.store(out, "".toCharArray());
		out.flush();
		out.close();
	}

	public Map<String, X509Certificate> readCSCAMasterList(CSCAMasterList cscaMasterList, String prefix) {
		List<Certificate> cscaCertificates = cscaMasterList.getCertificates();
		Map<String, X509Certificate> result = new TreeMap<String, X509Certificate>();
		int i = 0;
		for (Certificate certificate: cscaCertificates) {
			X509Certificate x509Certificate = (X509Certificate)certificate;
			X500Principal issuer = x509Certificate.getIssuerX500Principal();
			X500Principal subject = x509Certificate.getSubjectX500Principal();
			BigInteger serial = x509Certificate.getSerialNumber();
			//				String outName = subject + " (" + serial.toString(16) + ")";
			//				outName = DatatypeConverter.printBase64Binary(outName.getBytes("UTF-8"));
			//				outName = outName + ".cer";

			Country country = getCountry(issuer);

			boolean isSelfSigned = (issuer == null && subject == null) || subject.equals(issuer);

			String outName = country.toAlpha2Code().toLowerCase() + "_" + (isSelfSigned ? "root_" : "link_") + prefix + (++i) + ".cer";
			result.put(outName, x509Certificate);
		}
		return result;
	}

	private KeyStore toKeyStore(Map<String, X509Certificate> certificates) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		int jmrtdProvIndex = JMRTDSecurityProvider.beginPreferBouncyCastleProvider();

		try {
			KeyStore keyStore = KeyStore.getInstance("BKS");
			keyStore.load(null);
			for (Map.Entry<String, X509Certificate> entry: certificates.entrySet()) {
				String alias = entry.getKey();
				X509Certificate certificate = entry.getValue();
				System.out.println("DEBUG: adding certificate \"" + alias + "\" to key store.");
				keyStore.setCertificateEntry(alias, certificate);
			}
			return keyStore;
		} finally {
			JMRTDSecurityProvider.endPreferBouncyCastleProvider(jmrtdProvIndex);
		}
	}

	private void toCertDir(Map<String, X509Certificate> certificates, String outputDir) throws CertificateEncodingException, IOException {
		for (Map.Entry<String, X509Certificate> entry: certificates.entrySet()) {
			String alias = entry.getKey();
			X509Certificate certificate = entry.getValue();
			File outFile = new File(outputDir, alias);
			DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(outFile));
			dataOut.write(certificate.getEncoded());
			dataOut.close();
		}
	}

	private static Country getCountry(X500Principal principal) {
		String issuerName = principal.getName("RFC1779");
		int startIndex = issuerName.indexOf("C=");
		if (startIndex < 0) { throw new IllegalArgumentException("Could not get country from issuer name, " + issuerName); }
		int endIndex = issuerName.indexOf(",", startIndex);
		if (endIndex < 0) { endIndex = issuerName.length(); }
		final String countryCode = issuerName.substring(startIndex + 2, endIndex).trim().toUpperCase();
		try {			
			return Country.getInstance(countryCode);
		} catch (Exception e) {
			return new Country() {
				public int valueOf() { return -1; }
				public String getName() { return "Unknown country (" + countryCode + ")"; }
				public String getNationality() { return "Unknown nationality (" + countryCode + ")"; }
				public String toAlpha2Code() { return countryCode; }
				public String toAlpha3Code() { return "X" + countryCode; }
			};
		}
	}
}
