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

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.x500.X500Principal;

import org.jmrtd.cert.KeyStoreCertStoreParameters;
import org.jmrtd.cert.PKDCertStoreParameters;
import org.jmrtd.cert.PKDMasterListCertStoreParameters;

/**
 * Provides lookup for certificates, keys, CRLs used in
 * document validation and access control for data groups.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class MRTDTrustStore {

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private static final CertSelector SELF_SIGNED_X509_CERT_SELECTOR = new X509CertSelector() {
		public boolean match(Certificate cert) {
			if (!(cert instanceof X509Certificate)) { return false; }
			X509Certificate x509Cert = (X509Certificate)cert;
			X500Principal issuer = x509Cert.getIssuerX500Principal();
			X500Principal subject = x509Cert.getSubjectX500Principal();
			return (issuer == null && subject == null) || subject.equals(issuer);
		}

		public Object clone() { return this; }		
	};

	private Set<TrustAnchor> cscaAnchors;
	private List<CertStore> cscaStores;
	private List<KeyStore> cvcaStores;

	/**
	 * Constructs an instance.
	 */
	public MRTDTrustStore() {
		this(new HashSet<TrustAnchor>(), new ArrayList<CertStore>(), new ArrayList<KeyStore>());
	}

	/**
	 * Constructs an instance.
	 * 
	 * @param cscaAnchors the root certificates for document validation
	 * @param cscaStores the certificates used in document validation
	 * @param cvcaStores the certificates used for access to EAC protected data groups
	 */
	public MRTDTrustStore(Set<TrustAnchor> cscaAnchors, List<CertStore> cscaStores, List<KeyStore> cvcaStores) {
		super();
		this.cscaAnchors = cscaAnchors;
		this.cscaStores = cscaStores;
		this.cvcaStores = cvcaStores;
	}

	/**
	 * Gets the root certificates for document validation.
	 * 
	 * @return the cscaAnchors
	 */
	public Set<TrustAnchor> getCSCAAnchors() {
		return cscaAnchors;
	}
	/**
	 * Gets the certificates used in document validation.
	 * 
	 * @return the cscaStores
	 */
	public List<CertStore> getCSCAStores() {
		return cscaStores;
	}
	/**
	 * Gets the certificates used for access to EAC protected data groups.
	 * 
	 * @return the cvcaStores
	 */
	public List<KeyStore> getCVCAStores() {
		return cvcaStores;
	}

	/**
	 * Adds a root certificate for document validation.
	 * 
	 * @param trustAnchor a trustAnchor
	 */
	public void addCSCAAnchor(TrustAnchor trustAnchor) {
		cscaAnchors.add(trustAnchor);
	}

	/**
	 * Adds root certificates for document validation.
	 * 
	 * @param trustAnchors a collection of trustAnchors
	 */
	public void addCSCAAnchors(Collection<TrustAnchor> trustAnchors) {
		cscaAnchors.addAll(trustAnchors);
	}

	public void addCSCAStore(URI uri) {
		if (uri == null) { LOGGER.severe("uri == null"); return; }
		String scheme = uri.getScheme();
		if (scheme == null) { LOGGER.severe("scheme == null, location = " + uri); return; }
		try {
			if (scheme != null && scheme.equals("ldap")) {
				String server = uri.getHost();
				int port = uri.getPort();
				CertStoreParameters params = port < 0 ? new PKDCertStoreParameters(server) : new PKDCertStoreParameters(server, port);
				CertStoreParameters cscaParams = port < 0 ? new PKDMasterListCertStoreParameters(server) : new PKDMasterListCertStoreParameters(server, port);
				CertStore certStore = CertStore.getInstance("PKD", params);
				if (certStore != null) { addCSCAStore(certStore); }
				CertStore cscaStore = CertStore.getInstance("PKD", cscaParams);
				if (cscaStore != null) { addCSCAStore(cscaStore); }
				Collection<? extends Certificate> rootCerts = cscaStore.getCertificates(SELF_SIGNED_X509_CERT_SELECTOR);
				addCSCAAnchors(getAsAnchors(rootCerts));
			} else {
				/* TODO: Should we check that scheme is "file" or "http"? */
				try {
					CertStoreParameters params = new KeyStoreCertStoreParameters(uri, "JKS");
					CertStore certStore = CertStore.getInstance("JKS", params);
					addCSCAStore(certStore);
					Collection<? extends Certificate> rootCerts = certStore.getCertificates(SELF_SIGNED_X509_CERT_SELECTOR);
					addCSCAAnchors(getAsAnchors(rootCerts));
				} catch (KeyStoreException kse) {
					kse.printStackTrace();
				}
			}
		} catch (GeneralSecurityException gse) {
			gse.printStackTrace();
		}
	}

	public void addCSCAStores(List<URI> uris) {
		if (uris == null) { LOGGER.severe("uris == null"); return; }
		for (URI uri: uris) {
			addCSCAStore(uri);
		}
	}

	public void addCVCAStore(URI uri) {
		// We have to try both store types, only Bouncy Castle Store (BKS) 
		// knows about unnamed EC keys
		String[] storeTypes = new String[] {"JKS", "BKS" }; 
		for(String storeType : storeTypes) {
			try {
				KeyStore cvcaStore = KeyStore.getInstance(storeType);
				URLConnection uc = uri.toURL().openConnection();
				InputStream in = uc.getInputStream();
				cvcaStore.load(in, "".toCharArray());
				addCVCAStore(cvcaStore);
			} catch (Exception e) {
				LOGGER.warning("Could not initialize CVCA: " + e.getMessage());
			}
		}
	}

	public void addCVCAStores(List<URI> uris) {
		for (URI uri: uris) {
			addCVCAStore(uri);
		}
	}

	public void addCSCAStore(CertStore certStore) {
		cscaStores.add(certStore);
	}

	public void addCVCAStore(KeyStore keyStore) {
		cvcaStores.add(keyStore);
	}

	public void removeCSCAAnchor(TrustAnchor trustAnchor) {
		cscaAnchors.remove(trustAnchor);
	}

	public void removeCSCAStore(CertStore certStore) {
		cscaStores.remove(certStore);
	}

	public void removeCVCAStore(KeyStore keyStore) {
		cvcaStores.remove(keyStore);
	}

	/**
	 * Returns a set of trust anchors based on the X509 certificates in <code>certificates</code>.
	 * 
	 * @param certificates a collection of X509 certificates
	 * 
	 * @return a set of trust anchors
	 */
	private Set<TrustAnchor> getAsAnchors(Collection<? extends Certificate> certificates) {
		Set<TrustAnchor> anchors = new HashSet<TrustAnchor>(certificates.size());
		for (Certificate certificate: certificates) {
			if (certificate instanceof X509Certificate) {
				anchors.add(new TrustAnchor((X509Certificate)certificate, null));
			}
		}
		return anchors;
	}
}
