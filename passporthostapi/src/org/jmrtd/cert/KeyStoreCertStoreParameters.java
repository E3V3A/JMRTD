/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2011  The JMRTD team
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
 * $Id:  $
 */

package org.jmrtd.cert;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.cert.CertStoreParameters;

public class KeyStoreCertStoreParameters implements Cloneable, CertStoreParameters
{
	private static final Provider BC_PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();

	private static final String DEFAULT_ALGORITHM = "JKS";
	private static final char[] DEFAULT_PASSWORD = "".toCharArray();

	private KeyStore keyStore;

	public KeyStoreCertStoreParameters(URI uri) throws KeyStoreException {
		this(uri, DEFAULT_ALGORITHM, DEFAULT_PASSWORD);
	}

	public KeyStoreCertStoreParameters(URI uri, char[] password) throws KeyStoreException {
		this(uri, DEFAULT_ALGORITHM, password);
	}

	public KeyStoreCertStoreParameters(URI uri, String algorithm) throws KeyStoreException {
		this(uri, algorithm, DEFAULT_PASSWORD);
	}

	public KeyStoreCertStoreParameters(URI uri, String algorithm, char[] password) throws KeyStoreException {
		this(readKeyStore(uri, algorithm, password));
	}

	public KeyStoreCertStoreParameters(KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	public KeyStore getKeyStore() {
		return keyStore;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return this;
		}
	}

	private static KeyStore readKeyStore(URI location, String keyStoreType, char[] password) throws KeyStoreException {
		try {
			URLConnection uc = location.toURL().openConnection();
			InputStream in = uc.getInputStream();
			KeyStore ks = null;
			try {
				ks = KeyStore.getInstance(keyStoreType, BC_PROVIDER);
			} catch (Exception e1) {
				try {
					ks = KeyStore.getInstance(keyStoreType);
				} catch (Exception e2) {
					throw e1;
				}
			}
			ks.load(in, password);
			in.close();
			return ks;
		} catch (Exception e) {
			e.printStackTrace();
			throw new KeyStoreException("Error getting keystore: " + e.getMessage());
		}
	}
}
