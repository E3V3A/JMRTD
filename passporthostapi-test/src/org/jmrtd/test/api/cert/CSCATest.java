package org.jmrtd.test.api.cert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;
import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.ISOCountry;
import net.sourceforge.scuba.util.Files;

import org.jmrtd.CSCAStore;

public class CSCATest extends TestCase
{
	/** We may need this provider... */
	private static final Provider PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();

	private static final String PATH_TO_KEY_STORE = "d:/bla.ks";
	
	private static final String[] EASY_COUNTRIES =
	{ "AT", "ES", "FI", "FR", "GR", "HU", "MC", "NL1", "NL2", "SI" };

	/** Weird NoSuchAlgorithmException OID 1.2.840.113549.1.1.10 for "keytool -importcert". */
	private static final String[] CZ_COUNTRIES = { "CZ" };

	/** Input not an X.509 certificate for "keytool -importcert". */
	private static final String[] EC_COUNTRIES = { "CH", "DE" };

	private Map<Country, List<Certificate>> cscaMap;

	public CSCATest() {
	}

	public void testCSCAFiles() {
		/* Needed here, unless CSCAStore explicitly uses BC as provider! */
		// Security.insertProviderAt(PROVIDER, 1);

		// writeAllToStore();
		readAllFromStore();

	}

	private void writeAllToStore() {
		CSCAStore cscaStore = new CSCAStore();
		cscaMap = new HashMap<Country, List<Certificate>>();
		for (String c: EASY_COUNTRIES) { readCountryFromCertFile(c, cscaStore); }
		for (String c: CZ_COUNTRIES) { readCountryFromCertFile(c, cscaStore); }
		for (String c: EC_COUNTRIES) { readCountryFromCertFile(c, cscaStore); }

		writeToKeyStore(cscaMap, PATH_TO_KEY_STORE);
	}

	private void readAllFromStore() {
		Map<Country, List<Certificate>> map = readFromKeyStore("d:/bla.ks");
		for (Map.Entry<Country, List<Certificate>> entry: map.entrySet()) {
			Country country = entry.getKey();
			System.out.println("DEBUG: country = " + country);
			List<Certificate> certificates = entry.getValue();
			for (Certificate certificate: certificates) {
				if (certificate instanceof X509Certificate) {
					X509Certificate x509Certificate = (X509Certificate)certificate;
					System.out.println("DEBUG:     certificate (x509)  issuer: " + x509Certificate.getIssuerDN().getName());
					System.out.println("DEBUG:                        subject: " + x509Certificate.getSubjectDN().getName());
					System.out.println("DEBUG:                         serial: " + x509Certificate.getSerialNumber());
					System.out.println("DEBUG:                        version: " + x509Certificate.getVersion());
				} else {
					System.out.println("DEBUG:      certificate (non x509) " + certificate);
				}
			}
		}
	}

	public void readCountryFromCertFile(String alias, CSCAStore cscaStore) {
		try {
			System.err.println("DEBUG: testing certificate for " + alias);
			Certificate certificate = getCertificate(alias);
			if (certificate == null) { fail("Could not load certificate for " + alias); }
			// Country country = ISOCountry.getInstance(alias);
			Country country = getCountry(((X509Certificate)certificate).getIssuerX500Principal());
			List<Certificate> certificates = cscaMap.get(country);
			if (certificates == null) { certificates = new ArrayList<Certificate>(); }
			certificates.add(certificate);
			cscaMap.put(country, certificates);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	public Certificate getCertificate(String alias) throws IOException {
		try {
			Certificate countrySigningCert = null;
			/* TODO: also check .pem, .der formats? */
			
			URL cscaFile = new URL("file:///d/csca/"+ alias + ".cer");
			CertificateFactory certFactory = null;
			try {
				certFactory = CertificateFactory.getInstance("X.509", PROVIDER);
			} catch (Exception e) {
				certFactory = CertificateFactory.getInstance("X.509");		
			}
			InputStream cscaIn = cscaFile.openStream();
			if (cscaIn == null) {
				throw new IOException("Could not read certificate for " + alias);
			}
			countrySigningCert = certFactory.generateCertificate(cscaIn);
			return countrySigningCert;
		} catch (MalformedURLException mfue) {
			mfue.printStackTrace();
			throw new IOException(mfue.toString());
		} catch (CertificateException ce) {
			ce.printStackTrace();
			throw new IOException(ce.toString());
		}
	}

	private Map<Country, List<Certificate>> readFromKeyStore(String keyStoreFileName) {
		Map<Country, List<Certificate>> result = new HashMap<Country, List<Certificate>>();
		try {
			File keyStoreFile = new File(keyStoreFileName);
			FileInputStream in = new FileInputStream(keyStoreFile);
			KeyStore ks = KeyStore.getInstance("PKCS12", PROVIDER); // or whatever type of keystore you have
			char[] pw = "".toCharArray();
			ks.load(in, pw);
			Enumeration<String> aliases = ks.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				if (alias.length() < 3) { throw new IllegalArgumentException("Aliases in CSCA store need to be in XXn format (found: \"" + alias + "\""); }
				Certificate certificate = ks.getCertificate(alias);
				System.out.println("DEBUG: alias = " + alias);
				String countryCode = alias.substring(0, 2).toUpperCase();
				System.out.println("DEBUG: countryCode = " + countryCode);
				Country country = ISOCountry.getInstance(countryCode);
				List<Certificate> certificates = result.get(country);
				if (certificates == null) { certificates = new ArrayList<Certificate>(); }
				certificates.add(certificate);
				result.put(country, certificates);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private void writeToKeyStore(Map<Country, List<Certificate>> cscaMap, String keyStoreFileName) {
		try {
			File keyStoreFile = new File(keyStoreFileName);
			FileOutputStream out = new FileOutputStream(keyStoreFile);
			KeyStore ks = KeyStore.getInstance("PKCS12", PROVIDER); // or whatever type of keystore you have
			ks.load(null); /* NOTE: Initializes an empty keystore. */
			char[] pw = "".toCharArray();
			for (Map.Entry<Country, List<Certificate>> entry: cscaMap.entrySet()) {
				Country country = entry.getKey();
				List<Certificate> certificates = entry.getValue();
				int i = 1;
				for (Certificate certificate: certificates) {
					String alias = country.toAlpha2Code().toLowerCase() + i;
					ks.setCertificateEntry(alias, certificate);
					i++;
				}
			}
			ks.store(out, pw);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Country getCountry(X500Principal issuer) {
		String issuerName = issuer.getName(X500Principal.RFC2253);
		StringTokenizer st = new StringTokenizer(issuerName, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.toUpperCase().startsWith("C=")) {
				String countryString = token.substring(token.indexOf('=') + 1, token.length()).toUpperCase();
				return ISOCountry.getInstance(countryString);
			}
		}
		return null;
	}
}
