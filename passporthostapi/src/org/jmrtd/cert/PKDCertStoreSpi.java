/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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

package org.jmrtd.cert;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.Provider;
import java.security.cert.CRL;
import java.security.cert.CRLSelector;
import java.security.cert.CertSelector;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertStoreSpi;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.ISOCountry;

public class PKDCertStoreSpi extends CertStoreSpi
{
	/** We may need this provider... */
	private static final Provider PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();

	//	private static final String DEFAULT_PKD_SERVER = "ldap://motest:389/";

	private static final String COUNTRY_ATTRIBUTE_NAME = "c";
	private static final String CERTIFICATE_ATTRIBUTE_NAME = "userCertificate";

	private DirContext context;
	private String server;
	private int port;
	private String baseDN;

	private Logger logger = Logger.getLogger("org.jmrtd");

	private List<Certificate> certificates;

	public PKDCertStoreSpi(CertStoreParameters params) throws InvalidAlgorithmParameterException {
		super(params);
		if (params == null) { throw new InvalidAlgorithmParameterException("Input was null."); }
		if (!(params instanceof PKDCertStoreParameters)) { throw new InvalidAlgorithmParameterException("Expected PKDCertStoreParameters, found " + params.getClass().getCanonicalName()); }
		this.server = ((PKDCertStoreParameters)params).getServerName();
		this.port = ((PKDCertStoreParameters)params).getPort();
		this.baseDN = ((PKDCertStoreParameters)params).getBaseDN();
		certificates = new ArrayList<Certificate>();
		new Thread(new Runnable() {
			public void run() {
				try {
					connect();
					loadCertificates();
				} catch (CommunicationException ce) {
					ce.printStackTrace();
					return;
				}
			}
		}).start();
	}

	public Collection<? extends Certificate> engineGetCertificates(CertSelector selector) {
		List<Certificate> result = new ArrayList<Certificate>();
		for (Certificate certificate: certificates) {
			if (selector.match(certificate)) {
				result.add(certificate);
			}
		}
		return result;
	}


	public String getBaseDN() {
		return baseDN;
	}

	private synchronized void connect() throws CommunicationException {
		try {
			context = null;
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, "ldap://" + server + ":" + port);
			context = new InitialDirContext(env);
		} catch (NamingException ne) {
			ne.printStackTrace();
			throw new IllegalArgumentException("Could not connect to server \"" + server + "\"");
		}
	}

	private synchronized void loadCertificates() {
		List<Country> countries = searchCountries();
		for (Country country: countries) {
			List<Certificate> countryCertificates = searchCertificates(country);
			certificates.addAll(countryCertificates);
		}
	}

	private List<Country> searchCountries() {
		List<Country> countries = new ArrayList<Country>();
		try {
			String filter = "(&(objectClass=country)(entryDN=*))";
			String[] attrIDs = { COUNTRY_ATTRIBUTE_NAME };
			SearchControls controls = new SearchControls();
			controls.setReturningAttributes(attrIDs);

			// Search for objects using the filter
			NamingEnumeration<?> answer = null;
			try {
				answer = context.search(baseDN, filter, controls);
			} catch (NamingException ne) {
				logger.warning("No matches found for while searching for countries!");
			}
			int resultCount = 0;
			for (; answer != null && answer.hasMore(); resultCount++) {
				SearchResult searchResult = (SearchResult)answer.next();

				int attributeCount = 0;
				Attributes attributes = searchResult.getAttributes();
				for (NamingEnumeration<?> ae = attributes.getAll(); ae.hasMore(); attributeCount++) {
					Attribute attribute = (Attribute)ae.next();

					/* Name */
					String attributeName = attribute.getID();
					if (!attributeName.equalsIgnoreCase(COUNTRY_ATTRIBUTE_NAME)) {
						logger.warning("Search result contains attribute \"" + attributeName + ", was expecting \"" + COUNTRY_ATTRIBUTE_NAME + "\"");
					}

					/* Values */
					int attributeValueCount = 0;
					for (NamingEnumeration<?> attrValueEnum = attribute.getAll(); attrValueEnum.hasMore(); attributeValueCount++) {
						Object value = attrValueEnum.next();
						if (value instanceof String && value != null) {
							String countryCode = ((String)value).trim().toUpperCase();
							countries.add(ISOCountry.getInstance(countryCode));
						}
					}
					if (attributeValueCount != 1) {
						logger.warning("Search found object with more than 1 country value");
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return countries;
	}

	private List<Certificate> searchCertificates(Country country) {
		List<Certificate> result = new ArrayList<Certificate>();
		try {
			String countryCertificatesDN = "o=Certificates,c="
				+ country.toAlpha2Code().toUpperCase() + "," + baseDN;

			Attributes matchAttrs = new BasicAttributes(true); /* Ignore attribute name case. */
			String[] attrIDs = { CERTIFICATE_ATTRIBUTE_NAME };

			matchAttrs.put(new BasicAttribute(CERTIFICATE_ATTRIBUTE_NAME));
			if (!CERTIFICATE_ATTRIBUTE_NAME.endsWith(";binary")) {
				String certificateAttributeNameBinary = CERTIFICATE_ATTRIBUTE_NAME + ";binary";
				matchAttrs.put(new BasicAttribute(certificateAttributeNameBinary));
				attrIDs = new String[]{ CERTIFICATE_ATTRIBUTE_NAME, certificateAttributeNameBinary };
			}

			/* Search for objects that have those matching attributes. */
			NamingEnumeration<?> answer = null;
			try {
				answer = context.search(countryCertificatesDN, matchAttrs, attrIDs);
			} catch (NameNotFoundException nnfe) {
				/* NOTE: No certificates found for this country. Maybe they just publish CRL through PKD. Fine. */
			}

			int resultCount = 0;
			for (; answer != null && answer.hasMore(); resultCount++) {
				SearchResult searchResult = (SearchResult)answer.next();

				int attributeCount = 0;
				Attributes attributes = searchResult.getAttributes();
				for (NamingEnumeration<?> ae = attributes.getAll(); ae.hasMore(); attributeCount++) {
					Attribute attribute = (Attribute)ae.next();

					/* Name */
					String attributeName = attribute.getID();
					if (!attributeName.startsWith(CERTIFICATE_ATTRIBUTE_NAME)) {
						System.err.println("WARNING: search found \"" + attributeName + "\", was expecting \"" + CERTIFICATE_ATTRIBUTE_NAME + "\"");
					}

					/* Values */
					int attributeValueCount = 0;
					for (NamingEnumeration<?> attrValueEnum = attribute.getAll(); attrValueEnum.hasMore(); attributeValueCount++) {
						Object value = attrValueEnum.next();						
						if (value instanceof byte[]) {
							byte[] valueBytes = (byte[])value;
							Certificate certificate = null;
							try {
								CertificateFactory factory = CertificateFactory.getInstance("X509");
								certificate = factory.generateCertificate(new ByteArrayInputStream(valueBytes));
							} catch (Exception e) {
								try {
									CertificateFactory factory = CertificateFactory.getInstance("X509", PROVIDER);
									certificate = factory.generateCertificate(new ByteArrayInputStream(valueBytes));
								}  catch (CertificateException ce) {
									ce.printStackTrace();
									certificate = null;
								}
							}
							result.add(certificate);
						}
					}
					if (attributeValueCount != 1) {
						logger.warning("More than 1 value for \"" + attributeName + "\"");
					}
				}
				if (attributeCount != 1) {
					logger.warning("More than 1 attribute found in an object with attribute \"" + CERTIFICATE_ATTRIBUTE_NAME + "\"");
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return result;
	}

	public Collection<? extends CRL> engineGetCRLs(CRLSelector selector)
	throws CertStoreException {
		// TODO Auto-generated method stub
		return null;
	}
}