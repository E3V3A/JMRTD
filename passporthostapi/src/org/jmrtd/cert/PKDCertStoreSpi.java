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

import java.io.ByteArrayInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.Provider;
import java.security.cert.CRL;
import java.security.cert.CRLException;
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
import net.sourceforge.scuba.util.Hex;

public class PKDCertStoreSpi extends CertStoreSpi
{
	/** We may need this provider... */
	private static final Provider PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();

	//	private static final String DEFAULT_PKD_SERVER = "ldap://motest:389/";

	private static final String COUNTRY_ATTRIBUTE_NAME = "c";
	private static final String CERTIFICATE_ATTRIBUTE_NAME = "userCertificate";
	private static final String CRL_ATTRIBUTE_NAME = "certificateRevocationList";

	private DirContext context;
	private String server;
	private int port;
	private String baseDN;

	private CertificateFactory factory;
	
	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private List<Certificate> certificates;
	private List<CRL> crls;

	public PKDCertStoreSpi(CertStoreParameters params) throws InvalidAlgorithmParameterException {
		super(params);
		if (params == null) { throw new InvalidAlgorithmParameterException("Input was null."); }
		if (!(params instanceof PKDCertStoreParameters)) { throw new InvalidAlgorithmParameterException("Expected PKDCertStoreParameters, found " + params.getClass().getCanonicalName()); }
		this.server = ((PKDCertStoreParameters)params).getServerName();
		this.port = ((PKDCertStoreParameters)params).getPort();
		this.baseDN = ((PKDCertStoreParameters)params).getBaseDN();
		try {
			factory = CertificateFactory.getInstance("X509");
		} catch (CertificateException ce) {
			throw new IllegalStateException("Could not create an X.509 certificate factory\n" + ce.toString());
		}
	}

	private void start() {
		certificates = new ArrayList<Certificate>();
		crls = new ArrayList<CRL>();
		try {
			connect();
			List<Country> countries = searchCountries();
			loadCSCACertificates();
			loadCertificates(countries);
			loadCRLs(countries);
		} catch (CommunicationException ce) {
			ce.printStackTrace();
			return;
		}
	}

	public Collection<? extends Certificate> engineGetCertificates(CertSelector selector) {
		if (certificates == null) {
			start();
		}
		List<Certificate> result = new ArrayList<Certificate>();
		for (Certificate certificate: certificates) {
			if (selector.match(certificate)) {
				result.add(certificate);
			}
		}
		return result;
	}

	public Collection<? extends CRL> engineGetCRLs(CRLSelector selector)
	throws CertStoreException {
		if (crls == null) {
			start();
		}
		List<CRL> result = new ArrayList<CRL>();
		for (CRL crl: crls) {
			if (selector.match(crl)) {
				result.add(crl);
			}
		}
		return result;
	}

	public String getBaseDN() {
		return baseDN;
	}

	/* ONLY PRIVATE METHODS BELOW */

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

	private synchronized void loadCertificates(List<Country> countries) {
		for (Country country: countries) {
			List<Certificate> countryCertificates = searchCertificates(country);
			certificates.addAll(countryCertificates);
		}
	}

	private synchronized void loadCSCACertificates() {
		List<Certificate> cscaCertificates = searchCSCACertificates();
		certificates.addAll(cscaCertificates);
	}
	
	private synchronized void loadCRLs(List<Country> countries) {
		for (Country country: countries) {
			List<CRL> countryCRLs = searchCRLs(country);
			crls.addAll(countryCRLs);
		}
	}	

	private synchronized List<Country> searchCountries() {
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
				LOGGER.warning("No matches found while searching for countries!");
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
						LOGGER.warning("Search result contains attribute \"" + attributeName + ", was expecting \"" + COUNTRY_ATTRIBUTE_NAME + "\"");
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
						LOGGER.warning("Search found object with more than 1 country value");
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return countries;
	}

	private List<Certificate> searchCertificates(Country country) {
		String countrySpecificDN = "o=" + "Certificates" + ",c=" + country.toAlpha2Code().toUpperCase() + "," + baseDN;
		List<byte[]> binaries = searchAttributes(countrySpecificDN, CERTIFICATE_ATTRIBUTE_NAME);
		if (binaries == null) { return null; }
		List<Certificate> result = new ArrayList<Certificate>(binaries.size());
		for (byte[] valueBytes: binaries) {
			Certificate certificate = null;
			try {
				certificate = factory.generateCertificate(new ByteArrayInputStream(valueBytes));
			} catch (Exception e) {
				try {
					factory = CertificateFactory.getInstance("X509", PROVIDER);
					certificate = factory.generateCertificate(new ByteArrayInputStream(valueBytes));
				}  catch (CertificateException ce) {
					ce.printStackTrace();
					certificate = null;
				}
			}
			result.add(certificate);
		}		
		return result;
	}
	
	private List<Certificate> searchCSCACertificates() {
		String pkdMLDN = "dc=CSCAMasterList,dc=pkdDownload";
		LOGGER.info("DEBUG: pkdMLDN = " + pkdMLDN);
		List<byte[]> binaries = searchAttributes(pkdMLDN, "CscaMasterListData");
		for (byte[] binary: binaries) {
			LOGGER.info("DEBUG: found CscaMasterListData"); // FIXME: WORK IN PROGRESS!
			LOGGER.info(Hex.bytesToASCIIString(binary));
		}
		return new ArrayList<Certificate>(0); // FIXME
	}

	private List<CRL> searchCRLs(Country country) {
		String countrySpecificDN = "o="+ "CRLs" + ",c=" + country.toAlpha2Code().toUpperCase() + "," + baseDN;
		
		List<byte[]> binaries = searchAttributes(countrySpecificDN, CRL_ATTRIBUTE_NAME);
		if (binaries == null) { return null; }
		List<CRL> result = new ArrayList<CRL>(binaries.size());
		for (byte[] valueBytes: binaries) {
			CRL crl = null;
			try {
				crl = factory.generateCRL(new ByteArrayInputStream(valueBytes));
			} catch (Exception e) {
				try {
					factory = CertificateFactory.getInstance("X509", PROVIDER);
					crl = factory.generateCRL(new ByteArrayInputStream(valueBytes));
				}  catch (CRLException crle) {
					crle.printStackTrace();
					crl = null;
				}  catch (CertificateException ce) {
					ce.printStackTrace();
					crl = null;
				}
			}
			result.add(crl);
		}
		return result;
	}

	private List<byte[]> searchAttributes(String specificDN, String attributeName) {
		List<byte[]> result = new ArrayList<byte[]>();
		try {

			Attributes matchAttrs = new BasicAttributes(true); /* Ignore attribute name case. */
			String[] attrIDs = { attributeName };

			matchAttrs.put(new BasicAttribute(attributeName));
			if (!attributeName.endsWith(";binary")) {
				String certificateAttributeNameBinary = attributeName + ";binary";
				matchAttrs.put(new BasicAttribute(certificateAttributeNameBinary));
				attrIDs = new String[]{ attributeName, certificateAttributeNameBinary };
			}

			/* Search for objects that have those matching attributes. */
			NamingEnumeration<?> answer = null;
			try {
				answer = context.search(specificDN, matchAttrs, attrIDs);
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
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return result;
	}
}
