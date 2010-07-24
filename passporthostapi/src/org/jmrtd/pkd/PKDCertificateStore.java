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

package org.jmrtd.pkd;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.Key;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

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
import javax.security.auth.x500.X500Principal;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.ISOCountry;

import org.jmrtd.TrustStore;

public class PKDCertificateStore extends TrustStore
{
	/** We may need this provider... */
	private static final Provider PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();

	//	private static final String DEFAULT_PKD_SERVER = "ldap://motest:389/";
	private static final String DEFAULT_BASE_DN = "dc=data,dc=pkdDownload";

	private static final String COUNTRY_ATTRIBUTE_NAME = "c";
	private static final String CERTIFICATE_ATTRIBUTE_NAME = "userCertificate";

	private URI location;
	private DirContext context;
	private String baseDN;

	private List<Certificate> certificates;
	
	public PKDCertificateStore(URI server) {
		this(server, DEFAULT_BASE_DN);
	}

	public PKDCertificateStore(URI server, String baseDN) {
		this.location = server;
		setBaseDN(baseDN);
		certificates = new ArrayList<Certificate>();
		new Thread(new Runnable() {
			public void run() {
				connect();
				loadCertificates();
			}
		}).start();
	}

	public Collection<Certificate> getCertificates() {
		return certificates;
	}
	
	public Collection<Key> getKeys() {
		return null;
	}

	public URI getLocation() {
		return location;
	}
	
	public void setLocation(URI server) {
		this.location = server;
		new Thread(new Runnable() {
			public void run() {
				connect();
				loadCertificates();
			}
		}).start();
	}

	public String getBaseDN() {
		return baseDN;
	}

	public void setBaseDN(String baseDN) {
		this.baseDN = baseDN;
	}

	private synchronized void connect() {
		try {
			context = null;
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, location.toString());
			context = new InitialDirContext(env);
		} catch (NamingException ne) {
			ne.printStackTrace();
			throw new IllegalArgumentException("Could not connect to server \"" + location + "\"");
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
				System.err.println("WARNING: No matches found for while searching for countries!");
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
						System.err.println("WARNING: search result contains attribute \"" + attributeName + ", was expecting \"" + COUNTRY_ATTRIBUTE_NAME + "\"");
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
						System.err.println("WARNING: search found object with more than 1 country value");
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
				System.err.println("WARNING: No matches found for " + country + "!");
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
						System.err.println("WARNING: more than 1 value for \"" + attributeName + "\"");
					}
				}
				if (attributeCount != 1) {
					System.err.println("WARNING: more than 1 attribute found in an object with attribute \"" + CERTIFICATE_ATTRIBUTE_NAME + "\"");
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return result;
	}

	private Country getIssuerCountry(Certificate certificate) {
		if (certificate == null || !(certificate instanceof X509Certificate)) {
			throw new IllegalArgumentException("Was expecting a non-null X509Certificate");
		}
		X500Principal issuer = ((X509Certificate)certificate).getIssuerX500Principal();
		String issuerName = issuer.getName(X500Principal.RFC2253);
		StringTokenizer st = new StringTokenizer(issuerName, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.toUpperCase().startsWith("C=")) {
				String countryString = token.substring(token.indexOf('=') + 1, token.length()).toUpperCase();
				return ISOCountry.getInstance(countryString);
			}
		}
		return null;
	}
}
