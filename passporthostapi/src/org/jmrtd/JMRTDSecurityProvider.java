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

package org.jmrtd;

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Security provider for JMRTD specific implementations.
 * Provides:
 * <ul>
 *    <li>{@link java.security.cert.CertificateFactory} &quot;CVC&quot;
 *    	  (a factory for {@link org.jmrtd.cert.CardVerifiableCertificate} instances)
 *    </li>
 *    <li>{@link java.security.cert.CertStore} &quot;PKD&quot;
 *       (LDAP based <code>CertStore</code>,
 *       where the directory contains CSCA and document signer certificates)
 *    </li>
 *    <li>{@link java.security.cert.CertStore} &quot;JKS&quot;
 *       (<code>KeyStore</code> based <code>CertStore</code>,
 *       where the JKS formatted <code>KeyStore</code> contains CSCA certificates)
 *    </li>
 *    <li>{@link java.security.cert.CertStore} &quot;PKCS12&quot;
 *       (<code>KeyStore</code> based <code>CertStore</code>,
 *       where the PKCS#12 formatted <code>KeyStore</code> contains CSCA certificates)
 *    </li>
 * </ul>
 *
 * @author JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class JMRTDSecurityProvider extends Provider
{
	private static final long serialVersionUID = -2881416441551680704L;

	private static final Provider BC_PROVIDER = createBouncyCastleProviderOrNull();
	private static final Provider SC_PROVIDER = createSpongyCastleProviderOrNull();
	private static final Provider JMRTD_PROVIDER = new JMRTDSecurityProvider();

	static {
		if (BC_PROVIDER != null) { Security.insertProviderAt(BC_PROVIDER, 1); }
		if (SC_PROVIDER != null) { Security.insertProviderAt(SC_PROVIDER, 2); }
		if (JMRTD_PROVIDER != null) { Security.insertProviderAt(JMRTD_PROVIDER, 3); }
	}
	
	private JMRTDSecurityProvider() {
		super("JMRTD", 0.1, "JMRTD Security Provider");
		put("CertificateFactory.CVC", "org.jmrtd.cert.CVCertificateFactorySpi");
		put("CertStore.PKD", "org.jmrtd.cert.PKDCertStoreSpi");
		put("CertStore.JKS", "org.jmrtd.cert.KeyStoreCertStoreSpi");
		put("CertStore.PKCS12", "org.jmrtd.cert.KeyStoreCertStoreSpi");

		if (BC_PROVIDER != null) {
			/* Replicate BC algorithms... */

			/* FIXME: this won't work, our provider is not signed! */
//			replicateFromProvider("Cipher", "DESede/CBC/NoPadding", getBouncyCastleProvider());
//			replicateFromProvider("Cipher", "RSA/ECB/PKCS1Padding", getBouncyCastleProvider());
//			replicateFromProvider("Cipher", "RSA/NONE/NoPadding", getBouncyCastleProvider());
//			replicateFromProvider("KeyFactory", "RSA", getBouncyCastleProvider());
//			replicateFromProvider("KeyFactory", "DH", getBouncyCastleProvider());
//			replicateFromProvider("Mac", "ISO9797ALG3MAC", getBouncyCastleProvider());
//			replicateFromProvider("Mac", "ISO9797ALG3WITHISO7816-4PADDING", getBouncyCastleProvider());
//			replicateFromProvider("SecretKeyFactory", "DESede", getBouncyCastleProvider());

			/* But these work fine. */
			replicateFromProvider("CertificateFactory", "X.509", getBouncyCastleProvider());
			replicateFromProvider("CertStore", "Collection", getBouncyCastleProvider());
			replicateFromProvider("Keystore", "JKS", getBouncyCastleProvider());
			replicateFromProvider("MessageDigest", "SHA1", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA1withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "MD2withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "MD4withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "MD5withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA1withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA1withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA256withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA256withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA384withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA384withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA512withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA512withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA224withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA224withRSA/ISO9796-2", getBouncyCastleProvider());

			put("Alg.Alias.Mac.ISO9797Alg3Mac", "ISO9797ALG3MAC");
			put("Alg.Alias.CertificateFactory.X509", "X.509");
		}
	}

	private void replicateFromProvider(String serviceName, String algorithmName, Provider provider) {
		String name = serviceName + "." + algorithmName;
		Object bouncyService = provider.get(name);
		if (bouncyService != null) {
			put(name, bouncyService);
		}
	}

	public static Provider getInstance() {
		return JMRTD_PROVIDER;
	}

	public static Provider getBouncyCastleProvider() {
		if (BC_PROVIDER != null) { return BC_PROVIDER; }
		if (SC_PROVIDER != null) { return SC_PROVIDER; }
		return null;
	}

	public static Provider getSpongyCastleProvider() {
		if (SC_PROVIDER != null) { return SC_PROVIDER; }
		if (BC_PROVIDER != null) { return BC_PROVIDER; }
		return null;
	}

	private static Provider createSpongyCastleProviderOrNull() {
		try {
			return (Provider)(Class.forName("org.spongycastle.jce.provider.BouncyCastleProvider")).newInstance();
		} catch (IllegalAccessException iae) {
			// iae.printStackTrace();
		} catch (InstantiationException ie) {
			// ie.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			// cnfe.printStackTrace();
		}
		return null;
	}

	private static Provider createBouncyCastleProviderOrNull() {
		try {
			return (Provider)(Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")).newInstance();
		} catch (IllegalAccessException iae) {
			// iae.printStackTrace();
		} catch (InstantiationException ie) {
			// ie.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			// cnfe.printStackTrace();
		}
		return null;
	}

	public static Provider getProvider(String serviceName, String algorithmName) {
		List<Provider> providers = getProviders(serviceName, algorithmName);
		if (providers != null && providers.size() > 0) {
			return providers.get(0);
		}
		return null;
	}
	
	public static List<Provider> getProviders(String serviceName, String algorithmName) {
		if (Security.getAlgorithms(serviceName).contains(algorithmName)) {
			Provider[] providers = Security.getProviders(serviceName + "." + algorithmName);
			return Arrays.asList(providers);
		}
		if (BC_PROVIDER != null && BC_PROVIDER.getService(serviceName, algorithmName) != null) {
			return Collections.singletonList(BC_PROVIDER);
		}
		if (SC_PROVIDER != null && SC_PROVIDER.getService(serviceName, algorithmName) != null) {
			return Collections.singletonList(SC_PROVIDER);
		}
		if (JMRTD_PROVIDER != null && JMRTD_PROVIDER.getService(serviceName, algorithmName) != null) {
			return Collections.singletonList(JMRTD_PROVIDER);
		}
		return null;
	}
}
