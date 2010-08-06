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
		return this;
	}

	private static KeyStore readKeyStore(URI location, String keyStoreType, char[] password) throws KeyStoreException {
		try {
			URLConnection uc = location.toURL().openConnection();
			InputStream in = uc.getInputStream();
			KeyStore ks = null;
			try {
				ks = KeyStore.getInstance(keyStoreType, BC_PROVIDER);
				System.out.println("DEBUG: using BC provider to create keystore of type " + keyStoreType);
			} catch (Exception e1) {
				try {
					ks = KeyStore.getInstance(keyStoreType);
					System.out.println("DEBUG: using whatever provider to create keystore of type " + keyStoreType);
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
