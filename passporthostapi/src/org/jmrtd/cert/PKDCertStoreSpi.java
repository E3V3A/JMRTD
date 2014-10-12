/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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
 * $Id$
 */

package org.jmrtd.cert;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.Provider;
import java.security.cert.CRL;
import java.security.cert.CRLSelector;
import java.security.cert.CertSelector;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertStoreSpi;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import net.sf.scuba.util.Hex;

import org.jmrtd.JMRTDSecurityProvider;

/**
 * SPI for PKD backed certificate store.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
public class PKDCertStoreSpi extends CertStoreSpi
{
	/** We may need this provider... */
	private static final Provider JMRTD_PROVIDER = JMRTDSecurityProvider.getInstance();

	private static final long SERVER_TIMEOUT = 5000;

	private static final String CERTIFICATE_ATTRIBUTE_NAME = "userCertificate";
	private static final String CSCA_MASTER_LIST_DATA_ATTRIBUTE_NAME = "CscaMasterListData";
	private static final String CRL_ATTRIBUTE_NAME = "certificateRevocationList";

	private PKDCertStoreParameters params;
	private DirContext context;
	private String server;
	private int port;
	private String baseDN;
	private boolean isMasterListStore;

	private long heartBeat;
	private Collection<CRL> crls;
	private Collection<Certificate> certificates;

	private List<CertificateFactory> factories;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	public PKDCertStoreSpi(CertStoreParameters params) throws InvalidAlgorithmParameterException {
		super(params);
		// LOGGER.setLevel(Level.ALL); /* NOTE: only uncomment for debugging. */
		if (params == null) { throw new InvalidAlgorithmParameterException("Input was null."); }
		if (!(params instanceof PKDCertStoreParameters)) { throw new InvalidAlgorithmParameterException("Expected PKDCertStoreParameters, found " + params.getClass().getCanonicalName()); }
		this.params = (PKDCertStoreParameters)params;
		isMasterListStore = (params instanceof PKDMasterListCertStoreParameters);
		this.server = ((PKDCertStoreParameters)params).getServerName();
		this.port = ((PKDCertStoreParameters)params).getPort();
		this.baseDN = ((PKDCertStoreParameters)params).getBaseDN();
		factories = new ArrayList<CertificateFactory>();
		try {
			factories.add(CertificateFactory.getInstance("X.509"));
		} catch (Exception e) {
			/* NOTE: failed to add that factory */
		}
		try {
			if (JMRTD_PROVIDER != null) {
				factories.add(CertificateFactory.getInstance("X.509", JMRTD_PROVIDER.getName()));
			}
		} catch (Exception e) {
			/* NOTE: failed to add that factory */
		}
	}

	public Collection<? extends Certificate> engineGetCertificates(CertSelector selector) {
		try {
			if (context == null) { connect(); }
			if (isMasterListStore) {
				return searchCSCACertificates(selector);
			} else {
				return searchCertificates(selector);
			}
		} catch (CommunicationException ce) {
			ce.printStackTrace();
		}
		return new HashSet<Certificate>();
	}

	public Collection<? extends CRL> engineGetCRLs(CRLSelector selector) throws CertStoreException {
		try {
			if (context == null) { connect(); }
			return searchCRLs(selector);
		} catch (CommunicationException ce) {
			ce.printStackTrace();
		}
		return new HashSet<CRL>();
	}

	public String getBaseDN() {
		return baseDN;
	}

	/* ONLY PRIVATE METHODS BELOW */

	/**
	 * Connects (binds) to the server.
	 */
	private synchronized void connect() throws CommunicationException {
		try {
			context = null;
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put("java.naming.ldap.attributes.binary", CSCA_MASTER_LIST_DATA_ATTRIBUTE_NAME); /* NOTE: otherwise master list data is returned as text. */
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, "ldap://" + server + ":" + port);
			context = new InitialDirContext(env);
		} catch (NamingException ne) {
			ne.printStackTrace();
			throw new IllegalArgumentException("Could not connect to server \"" + server + "\"");
		}
	}

	private Collection<Certificate> searchCertificates(CertSelector selector) {
		if (certificates != null && System.currentTimeMillis() - heartBeat < SERVER_TIMEOUT) {
			heartBeat = System.currentTimeMillis();
			return certificates;
		}
		String specificDN = params.getBaseDN();
		String filter = "(&(objectclass=inetOrgPerson))";

		if (selector instanceof X509CertSelector) {
			//			X500Principal issuer = ((X509CertSelector)selector).getIssuer();
			BigInteger serialNumber = ((X509CertSelector)selector).getSerialNumber();
			if (serialNumber != null) {
				filter = "(&(objectclass=inetOrgPerson)(sn=" + Hex.bytesToHexString(serialNumber.toByteArray()) + "))";
			}
		}

		Collection<byte[]> binaries = searchAllAttributes(specificDN, CERTIFICATE_ATTRIBUTE_NAME, filter);
		Collection<Certificate> result = new HashSet<Certificate>(binaries.size());
		for (byte[] valueBytes: binaries) {
			Certificate certificate = null;
			for (CertificateFactory factory: factories) {
				try {
					certificate = factory.generateCertificate(new ByteArrayInputStream(valueBytes));
				} catch (Exception e) {
					continue; /* NOTE: try next factory. */
				}
			}

			if (selector.match(certificate)) {
				result.add(certificate);
			}
		}
		certificates = result;
		heartBeat = System.currentTimeMillis();
		return result;
	}

	private Collection<Certificate> searchCSCACertificates(CertSelector selector) {
		if (certificates != null && System.currentTimeMillis() - heartBeat < SERVER_TIMEOUT) {
			heartBeat = System.currentTimeMillis();
			return certificates;
		}
		String pkdMLDN = params.getBaseDN();
		String filter = "(&(objectclass=CscaMasterList))";
		Collection<byte[]> binaries = searchAllAttributes(pkdMLDN, CSCA_MASTER_LIST_DATA_ATTRIBUTE_NAME, filter);

		List<Certificate> result = new ArrayList<Certificate>(binaries.size());

		for (byte[] binary: binaries) {
			CSCAMasterList masterList = new CSCAMasterList(binary, selector);
			result.addAll(masterList.getCertificates());
		}
		certificates = result;
		heartBeat = System.currentTimeMillis();
		return result;
	}

	private Collection<CRL> searchCRLs(CRLSelector selector) {
		if (crls != null && System.currentTimeMillis() - heartBeat < SERVER_TIMEOUT) {
			heartBeat = System.currentTimeMillis();
			return crls;
		}
		String pkdMLDN = params.getBaseDN();
		String filter = "(&(objectclass=cRLDistributionPoint))";
		Collection<byte[]> binaries = searchAllAttributes(pkdMLDN, CRL_ATTRIBUTE_NAME, filter);
		Collection<CRL> result = new HashSet<CRL>(binaries.size());
		for (byte[] valueBytes: binaries) {
			CRL crl = null;
			for (CertificateFactory factory: factories) {
				try {
					crl = factory.generateCRL(new ByteArrayInputStream(valueBytes));
				} catch (Exception e) {
					continue; /* NOTE: try next factory... */
				}
			}
			if (crl != null && selector.match(crl)) {
				result.add(crl);
			}
		}
		heartBeat = System.currentTimeMillis();
		crls = result;
		return result;
	}

	private Collection<byte[]> searchAllAttributes(String specificDN, String attributeName, String filter) {
		SearchControls controls = new SearchControls();
		String[] attrIDs = { attributeName };
		//		if (!attributeName.endsWith(";binary")) {
		//			String attributeNameBinary = attributeName + ";binary";
		//			attrIDs = new String[]{ attributeName, attributeNameBinary };
		//		}
		controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		controls.setReturningAttributes(attrIDs);
		controls.setReturningObjFlag(true);
		Set<byte[]> result = new HashSet<byte[]>();
		try {
			// Search for objects using the filter
			NamingEnumeration<?> answer = context.search(specificDN, filter, controls);
			addToList(answer, attributeName, result);
		} catch (NamingException ne) {
			ne.printStackTrace();
		}
		return result;
	}

	//	private Collection<byte[]> searchAttributes(String specificDN, String attributeName) {
	//		Collection<byte[]> result = new HashSet<byte[]>();
	//		try {
	//			Attributes matchAttrs = new BasicAttributes(true); /* Ignore attribute name case. */
	//			String[] attrIDs = { attributeName };
	//
	//			matchAttrs.put(new BasicAttribute(attributeName));
	//			if (!attributeName.endsWith(";binary")) {
	//				String attributeNameBinary = attributeName + ";binary";
	//				matchAttrs.put(new BasicAttribute(attributeNameBinary));
	//				attrIDs = new String[]{ attributeName, attributeNameBinary };
	//			}
	//
	//			/* Search for objects that have those matching attributes. */
	//			NamingEnumeration<?> answer = null;
	//			try {
	//				answer = context.search(specificDN, matchAttrs, attrIDs);
	//			} catch (NameNotFoundException nnfe) {
	//				/* NOTE: No results found. Fine. */
	//			}
	//
	//			addToList(answer, attributeName, result);
	//		} catch (NamingException e) {
	//			e.printStackTrace();
	//		}
	//		return result;
	//	}

	private void addToList(NamingEnumeration<?> answer, String attributeName, Collection<byte[]> result) throws NamingException {
		int resultCount = 0;
		for (; answer != null && answer.hasMore(); resultCount++) {
			SearchResult searchResult = (SearchResult)answer.next();

			int attributeCount = 0;
			Attributes attributes = searchResult.getAttributes();
			for (NamingEnumeration<?> ae = attributes.getAll(); ae.hasMore(); attributeCount++) {
				Attribute attribute = (Attribute)ae.next();

				/* Name */
				String foundAttributeName = attribute.getID();
				if (!foundAttributeName.startsWith(attributeName)) {
					LOGGER.warning("Search found \"" + foundAttributeName + "\", was expecting \"" + attributeName + "\"");
				}

				/* Values */
				int attributeValueCount = 0;
				for (NamingEnumeration<?> attrValueEnum = attribute.getAll(); attrValueEnum.hasMore(); attributeValueCount++) {
					Object value = attrValueEnum.next();						
					if (value instanceof byte[]) {
						byte[] valueBytes = (byte[])value;
						result.add(valueBytes);
					} else if (value instanceof String) {
						LOGGER.warning("Found String attribute value, was expecting byte[]");
						try {
							byte[] valueBytes = ((String)value).getBytes("UTF-8");
							result.add(valueBytes);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						LOGGER.warning("Found attribute value of type " + value.getClass().getCanonicalName());
					}
				}
				if (attributeValueCount != 1) {
					LOGGER.warning("More than 1 value for \"" + foundAttributeName + "\"");
				}
			}
			if (attributeCount != 1) {
				LOGGER.warning("More than 1 attribute found in an object with attribute \"" + attributeName + "\"");
			}
		}
	}
}
