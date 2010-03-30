/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id: $
 */

package org.jmrtd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.StringTokenizer;

import javax.security.auth.x500.X500Principal;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.util.Files;

/**
 * Certificate store. For storing and retrieving CSCA certificates.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class CSCAStore
{
	private static final File
		JMRTD_USER_DIR = Files.getApplicationDataDir("jmrtd"),
		DEFAULT_CSCA_DIR = new File(JMRTD_USER_DIR, "csca"); // FIXME: copy from the dist dir to this one, if this doesn't exist already
	
	private URL location;
	
	public CSCAStore() {
		setLocation(getDefaultCSCADir());
	}

	public X509Certificate getCertificate(X500Principal issuer) throws IOException {
		/* FIXME: still assumes one cert per country. */
		Country c = getCountry(issuer);
		return getCertificate(c.toString().toLowerCase());
	}

	public URL getLocation() {
		return location;
	}
	
	public void setLocation(URL location) {
		this.location = location;
	}
	
	private X509Certificate getCertificate(String alias) throws IOException {
		try {
			X509Certificate countrySigningCert = null;
			/* TODO: also check .pem, .der formats? */
			URL cscaFile = new URL(location + "/" + alias + ".cer");
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
	
	private Country getCountry(X500Principal issuer) {
		String issuerName = issuer.getName(X500Principal.RFC2253);
		StringTokenizer st = new StringTokenizer(issuerName, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.toUpperCase().startsWith("C=")) {
				String countryString = token.substring(token.indexOf('=') + 1, token.length());
				return Country.getInstance(countryString);
			}
		}
		return null;
	}
}
