package org.jmrtd.test.api.cert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;
import net.sourceforge.scuba.data.Country;

import org.jmrtd.cert.CSCAMasterList;

public class CSCAMasterListTest extends TestCase {

	private static final String TEST_CERT_DIR = "t:/ca/icao/csca";

	private static final String DE_ML_FILE =
			TEST_CERT_DIR
			// + "/german_csca_masterlist/20130614GermanMasterlist.ml";
			// + "/german_csca_masterlist/20130717GermanMasterlist.ml";
			// + "/german_csca_masterlist/20130815GermanMasterlist.ml";
			//			+ "/german_csca_masterlist/20131217_GermanMasterlist.ml";
			+ "/german_csca_masterlist/20140319_GermanMasterList.ml";

	private static final String DE_PREFIX = "from_de_ml_";

	private static final String CH_PREFIX = "from_ch_ml_";

	private static final String CH_ML_FILE =
			TEST_CERT_DIR + "/swiss_csca_masterlist/chMasterlist.ml";

	private static final String DE_CERTS_OUTPUT_DIR = TEST_CERT_DIR + "/german_csca_masterlist";

	private static final String CH_CERTS_OUTPUT_DIR = TEST_CERT_DIR + "/swiss_csca_masterlist";

	public void testGermanCSCAMasterList() {
		testCSCAMasterList(DE_ML_FILE, DE_PREFIX, DE_CERTS_OUTPUT_DIR);
	}

	public void testSwissMasterList() {
		testCSCAMasterList(CH_ML_FILE, CH_PREFIX, CH_CERTS_OUTPUT_DIR);
	}

	public void testCSCAMasterList(String fileName, String prefix, String outputDir) {
		try {
			File germanMLFile = new File(fileName);
			int length = (int)germanMLFile.length();
			byte[] bytes = new byte[length];
			DataInputStream dataIn = new DataInputStream(new FileInputStream(germanMLFile));
			dataIn.readFully(bytes);
			dataIn.close();
			CSCAMasterList cscaMasterList = new CSCAMasterList(bytes);
			List<Certificate> cscaCertificates = cscaMasterList.getCertificates();
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
				File outFile = new File(outputDir, outName);
				DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(outFile));
				dataOut.write(x509Certificate.getEncoded());
				System.out.println(outName);
				dataOut.close();
			}
			System.out.println("DEBUG: cscaCertificates.size() = " + cscaCertificates.size());

			(new CSCAStoreGenerator()).testImportX509Certificates(outputDir, outputDir + "/csca.ks");
		} catch (Exception e) {
			fail(e.getMessage());
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
