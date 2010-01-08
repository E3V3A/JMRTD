package org.jmrtd;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import net.sourceforge.scuba.data.Country;

public class CSCAStore
{
	private URL cscaDir;
	
	public CSCAStore(URL folder) {
		cscaDir = folder;
	}
	
	public X509Certificate getCertificate(Country c) throws IOException {
		try {
			X509Certificate countrySigningCert = null;
			/* TODO: also check .pem, .der formats? */
			URL cscaFile = new URL(cscaDir + "/" + c.toString().toLowerCase() + ".cer");
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			InputStream cscaIn = cscaFile.openStream();
			if (cscaIn == null) {
				throw new IOException("Could not read certificate for " + c);
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
}
