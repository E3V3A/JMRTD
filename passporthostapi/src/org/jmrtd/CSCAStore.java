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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.security.auth.x500.X500Principal;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.ISOCountry;
import net.sourceforge.scuba.util.Files;

/**
 * Certificate store. For storing and retrieving CSCA certificates.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class CSCAStore
{
	private static final Provider PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();

	/** It's a PKCS12 keystore file. */
	private static String KEY_STORE_FILE_NAME = "csca.ks";

	private KeyStore keyStore;
	private URL location;

	public CSCAStore() {
		try {
			setLocation(getDefaultCSCAFile());
			keyStore = getKeyStore(location);
		} catch (KeyStoreException kse) {
			kse.printStackTrace();
			// FIXME: What to do? Fatal error?
		}
	}

	/**
	 * Finds a certificate in this store given issuer information.
	 * 
	 * @param issuer identifies the issuer
	 * @return a certificate or null
	 * @throws KeyStoreException on error while reading the underlying keystore
	 */
	public Certificate getCertificate(X500Principal issuer) throws KeyStoreException {
		if (keyStore == null) { keyStore = getKeyStore(location); }
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			if (alias.length() < 3) { System.err.println("WARNING: Aliases in CSCA store not in XXn format (found: \"" + alias + "\""); }
			Certificate certificate = keyStore.getCertificate(alias);
			if (certificate instanceof X509Certificate) {
				X500Principal certIssuer = ((X509Certificate)certificate).getIssuerX500Principal();
				if (certIssuer.equals(issuer)) {
					return certificate;
				} else if (certIssuer.getName().equals(issuer.getName())) {
					System.err.println("WARNING: DEBUG: (in CSCAStore) problem with equals method of X500Principal...");
					return certificate;
				}
			}
		}
		return null;
	}
	
	/**
	 * FIXME: Untested.
	 * 
	 * @param certificate
	 * @throws KeyStoreException
	 */
	public void putCertificate(Certificate certificate) throws KeyStoreException
	{
		Country country = getCountry(certificate);
		String alias = (country == null) ? "unknown" : country.toAlpha2Code().toLowerCase();
		int i = 1;
		if (keyStore == null) { keyStore = getKeyStore(location); }
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String otherAlias = aliases.nextElement();
			if (otherAlias.startsWith(alias)) {
				Certificate otherCertificate = keyStore.getCertificate(otherAlias);
				if (certificate.equals(otherCertificate)) { return; } /* Already present. */
				try {
					String numStr = otherAlias.substring(alias.length());
					System.out.println("DEBUG: numStr = " + numStr);
					int n = Integer.parseInt(numStr);
					i = Math.max(i, n) + 1;
				} catch (NumberFormatException nfe) {
					System.err.println("WARNING: unexpected alias in CSCAStore \"" + otherAlias + "\")");
				}
			}
		}
	}

	public URL getLocation() {
		return location;
	}

	public void setLocation(URL location) {
		this.location = location;
	}
	
	private Country getCountry(Certificate cert) {
		if (cert instanceof X509Certificate) {
			X500Principal issuer = ((X509Certificate)cert).getIssuerX500Principal();
			String issuerName = issuer.getName(X500Principal.RFC2253);
			StringTokenizer st = new StringTokenizer(issuerName, ",");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.toUpperCase().startsWith("C=")) {
					String countryString = token.substring(token.indexOf('=') + 1, token.length()).toUpperCase();
					return ISOCountry.getInstance(countryString);
				}
			}
		}
		return null;
	}

	private static KeyStore getKeyStore(URL location) throws KeyStoreException {
		try {
			URLConnection uc = location.openConnection();
			InputStream in = uc.getInputStream();
			KeyStore ks = KeyStore.getInstance("PKCS12", PROVIDER);
			char[] pw = "".toCharArray();
			ks.load(in, pw);
			in.close();
			return ks;
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new KeyStoreException("Error getting keystore...");
	}

	private static URL getDefaultCSCAFile() {
		URL cscaFileURL = null;
		try {
			cscaFileURL = new URL(Files.getBaseDir(CSCAStore.class) + "/" + KEY_STORE_FILE_NAME);
		} catch (MalformedURLException mfue) {
			mfue.printStackTrace();
		}
		return cscaFileURL;
	}
}
