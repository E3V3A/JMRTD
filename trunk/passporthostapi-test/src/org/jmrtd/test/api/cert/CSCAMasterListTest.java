package org.jmrtd.test.api.cert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.DatatypeConverter;

import junit.framework.TestCase;

import net.sourceforge.scuba.data.Country;

import org.jmrtd.cert.CSCAMasterList;

public class CSCAMasterListTest extends TestCase {

	private static final String GERMAN_ML_FILE = "t:/ca/icao/csca/german_csca_masterlist/20130614GermanMasterlist.ml";
	private static final String OUTPUT_DIR = "t:/ca/icao/csca/german_csca_masterlist/out";
	
	public void testGermanCSCAMasterList() {
		try {
			File germanMLFile = new File(GERMAN_ML_FILE);
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

				String outName = country.toAlpha2Code().toLowerCase() + "_" + (isSelfSigned ? "root_" : "link_") + "from_german_ml_" + (++i) + ".cer";
				File outFile = new File(OUTPUT_DIR, outName);
				DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(outFile));
				dataOut.write(x509Certificate.getEncoded());
				System.out.println(outName);
				dataOut.close();
			}
			System.out.println("DEBUG: cscaCertificates.size() = " + cscaCertificates.size());
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
