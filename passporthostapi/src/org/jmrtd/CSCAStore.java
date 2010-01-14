package org.jmrtd;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.util.Files;

/**
 * Certificate store. For storing and retrieving CSCA certificates.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class CSCAStore
{
	private URL cscaDir;
	
	public CSCAStore() {
		this(getDefaultCSCADir());
	}
	
	public CSCAStore(URL folder) {
		cscaDir = folder;
	}
	
	public X509Certificate getCertificate(Country c) throws IOException {
		return getCertificate(c.toString().toLowerCase());
	}
	
	private X509Certificate getCertificate(String alias) throws IOException {
		try {
			X509Certificate countrySigningCert = null;
			/* TODO: also check .pem, .der formats? */
			URL cscaFile = new URL(cscaDir + "/" + alias + ".cer");
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			InputStream cscaIn = cscaFile.openStream();
			if (cscaIn == null) {
				throw new IOException("Could not read certificate for " + alias);
			}
			countrySigningCert = (X509Certificate)certFactory.generateCertificate(cscaIn);
			return countrySigningCert;
		} catch (MalformedURLException mfue) {
			mfue.printStackTrace();
			throw new IOException(mfue.toString());
		} catch (CertificateException ce) {
			ce.printStackTrace();
			throw new IOException(ce.toString());
		}
	}

	private static URL getDefaultCSCADir() {
		URL cscaDir = null;
		try {
			cscaDir = new URL(Files.getBaseDir(CSCAStore.class) + "/csca");
		} catch (MalformedURLException mfue) {
			mfue.printStackTrace();
		}
		return cscaDir;
	}
}
