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
import java.io.IOException;
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
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
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

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.jce.provider.X509CertificateObject;

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
		LOGGER.setLevel(Level.ALL); /* FIXME: only uncomment for debugging. */
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
		if (selector instanceof X509CertSelector) {
			// TODO: use getIssuer and getSerial on selector to limit the set of certs to get from LDAP
		}
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
			env.put("java.naming.ldap.attributes.binary", "CscaMasterListData"); /* NOTE: otherwise master list data is returned as text. */
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
		List<byte[]> binaries = searchAllAttributes(pkdMLDN, "CscaMasterListData");
		List<Certificate> certificates = new ArrayList<Certificate>();

		for (byte[] binary: binaries) {
			try {
				DERSequence derSequence = (DERSequence)DERSequence.getInstance(binary);
				List<SignedData> signedDataList = getSignedDataFromDERObject(derSequence, null);
				for (SignedData signedData: signedDataList) {
					certificates.addAll(getCertificatesFromDERObject(signedData, null));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		LOGGER.info("DEBUG: found " + certificates.size() + " CSCA certificates");
		return certificates;
	}

	private List<SignedData> getSignedDataFromDERObject(Object o, List<SignedData> signedDataList) {

		if (signedDataList == null) { signedDataList = new ArrayList<SignedData>(); }

		try {
			SignedData signedData = SignedData.getInstance(o);
			if (signedData != null) {
				signedDataList.add(signedData);
			}
			return signedDataList;
		} catch (Exception e) {
		}

		if (o instanceof DERTaggedObject) {
			DERObject childObject = ((DERTaggedObject)o).getObject();
			return getSignedDataFromDERObject(childObject, signedDataList);
		} else if (o instanceof DERSequence) {
			Enumeration derObjects = ((DERSequence)o).getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				signedDataList = getSignedDataFromDERObject(nextObject, signedDataList);
			}
			return signedDataList;
		} else if (o instanceof DERSet) {
			Enumeration derObjects = ((DERSet)o).getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				signedDataList = getSignedDataFromDERObject(nextObject, signedDataList);
			}
			return signedDataList;
		} else if (o instanceof DEROctetString) {
			DEROctetString derOctetString = (DEROctetString)o;
			byte[] octets = derOctetString.getOctets();
			ASN1InputStream derInputStream = new ASN1InputStream(new ByteArrayInputStream(octets));
			try {
				while (true) {
					DERObject derObject = derInputStream.readObject();
					if (derObject == null) { break; }
					signedDataList = getSignedDataFromDERObject(derObject, signedDataList);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return signedDataList;
		}
		return signedDataList;
	}

	private List<Certificate> getCertificatesFromDERObject(Object o, List<Certificate> certificates) {
		if (certificates == null) { certificates = new ArrayList<Certificate>(); }

		try {
			X509CertificateStructure cert = X509CertificateStructure.getInstance(o);
			certificates.add(new X509CertificateObject(cert));
			return certificates;
		} catch (Exception e) {
		}

		if (o instanceof DERTaggedObject) {
			DERObject childObject = ((DERTaggedObject)o).getObject();
			return getCertificatesFromDERObject(childObject, certificates);
		} else if (o instanceof DERSequence) {
			Enumeration derObjects = ((DERSequence)o).getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				certificates = getCertificatesFromDERObject(nextObject, certificates);
			}
			return certificates;
		} else if (o instanceof DERSet) {
			Enumeration derObjects = ((DERSet)o).getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				certificates = getCertificatesFromDERObject(nextObject, certificates);
			}
			return certificates;
		} else if (o instanceof DEROctetString) {
			DEROctetString derOctetString = (DEROctetString)o;
			byte[] octets = derOctetString.getOctets();
			ASN1InputStream derInputStream = new ASN1InputStream(new ByteArrayInputStream(octets));
			try {
				while (true) {
					DERObject derObject = derInputStream.readObject();
					if (derObject == null) { break; }
					certificates = getCertificatesFromDERObject(derObject, certificates);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return certificates;
		} else if (o instanceof SignedData) {
			SignedData signedData = (SignedData)o;
			//			ASN1Set certificatesASN1Set = signedData.getCertificates();
			//			Enumeration certificatesEnum = certificatesASN1Set.getObjects();
			//			while (certificatesEnum.hasMoreElements()) {
			//				Object certificateObject = certificatesEnum.nextElement();
			//				explore(indent + 1, certificateObject);
			//			}

			ContentInfo contentInfo = signedData.getContentInfo();
			Object content = contentInfo.getContent();
			return getCertificatesFromDERObject(content, certificates);
		}
		return certificates;
	}


	private void explore(int indent, Object o) {
		if (o instanceof SignedData) {
			SignedData signedData = (SignedData)o;
			LOGGER.info("DEBUG: " + indent(indent) + "signedData");
			//			ASN1Set certificatesASN1Set = signedData.getCertificates();
			//			Enumeration certificatesEnum = certificatesASN1Set.getObjects();
			//			while (certificatesEnum.hasMoreElements()) {
			//				Object certificateObject = certificatesEnum.nextElement();
			//				explore(indent + 1, certificateObject);
			//			}

			ContentInfo contentInfo = signedData.getContentInfo();
			Object content = contentInfo.getContent();
			explore(indent + 1, content);
			return;
		}

		if (o instanceof X509CertificateStructure) {
			X509CertificateStructure cert = (X509CertificateStructure)o;
			LOGGER.info("DEBUG: " + indent(indent) + "X509CertificateStructure " + cert.getSubject());
			return;
		}

		if (tryToParseAsSignedData(indent, o)) {
			explore(indent, SignedData.getInstance(o));
			return;
		}
		if (tryToParseAsX509Certificate(indent, o)) {
			explore(indent, X509CertificateStructure.getInstance(o));
			return;
		}
		if (o instanceof DEROctetString) {
			DEROctetString derOctetString = (DEROctetString)o;
			byte[] octets = derOctetString.getOctets();
			LOGGER.info("DEBUG: " + indent(indent) + "DEROctetString (" + octets.length + "): " + Hex.bytesToHexString(octets, 0, 10) + "...");
			ASN1InputStream derInputStream = new ASN1InputStream(new ByteArrayInputStream(octets));
			try {
				while (true) {
					DERObject derObject = derInputStream.readObject();
					if (derObject == null) { break; }
					explore(indent + 1, derObject);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} else if (o instanceof DERObjectIdentifier) {
			DERObjectIdentifier oid = (DERObjectIdentifier)o;
			LOGGER.info("DEBUG: " + indent(indent) + "DERObjectIdentifier " + oid.getId());
		} else if (o instanceof DERSequence) {
			DERSequence derSequence = (DERSequence)o;
			LOGGER.info("DEBUG: " + indent(indent) + "derSequence (" + derSequence.size() + ")");
			Enumeration derObjects = derSequence.getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				explore(indent + 1, nextObject);
			}
		} else if (o instanceof DERSet) {
			DERSet derSet = (DERSet)o;
			Enumeration derObjects = derSet.getObjects();
			LOGGER.info("DEBUG: " + indent(indent) + "derSeT (" + derSet.size() + ")");
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				explore(indent + 1, nextObject);
			}
		} else if (o instanceof DERTaggedObject) {
			DERTaggedObject taggedObject = (DERTaggedObject)o;
			int tag = taggedObject.getTagNo();
			DERObject childObject = taggedObject.getObject();
			LOGGER.info("DEBUG: " + indent(indent) + "DERTaggedObject with tag " + Integer.toHexString(tag));
			explore(indent + 1, childObject);
		} else {
			LOGGER.info("DEBUG: " + indent(indent) + "unknown of type " + o.getClass().getSimpleName());
		}
	}


	private boolean tryToParseAsSignedData(int indent, Object o) {
		try {
			SignedData signedData = SignedData.getInstance(o);
			LOGGER.warning("DEBUG: " + indent(indent) + "Found signedData at depth " + indent + ": " + signedData);
			return true;
		} catch (Exception e) {
			// LOGGER.warning("DEBUG: " + indent(indent) + "Failed to parse as signedData");
			return false;
		}
	}

	private boolean tryToParseAsX509Certificate(int indent, Object o) {
		try {
			X509CertificateStructure x509CertificateObject = X509CertificateStructure.getInstance(o);
			return true;
		} catch (Exception e) {
			return false;
		}
	}


	private String indent(int level) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < level; i++) {
			result.append(" ");
		}
		return result.toString();
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

	private List<byte[]> searchAllAttributes(String specificDN, String attributeName) {
		SearchControls controls = new SearchControls();
		String[] attrIDs = { "CscaMasterListData" };
		//		if (!attributeName.endsWith(";binary")) {
		//			String attributeNameBinary = attributeName + ";binary";
		//			attrIDs = new String[]{ attributeName, attributeNameBinary };
		//		}
		controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		controls.setReturningAttributes(attrIDs);
		controls.setReturningObjFlag(true);
		String filter = "(&(objectclass=CscaMasterList))";
		List<byte[]> result = new ArrayList<byte[]>();
		try {
			// Search for objects using the filter
			LOGGER.info("context.search(" + specificDN + ", " + filter + ", " + controls);
			NamingEnumeration<?> answer = context.search(specificDN, filter, controls);
			toList(answer, attributeName, result);
		} catch (NamingException ne) {
			ne.printStackTrace();
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
				String attributeNameBinary = attributeName + ";binary";
				matchAttrs.put(new BasicAttribute(attributeNameBinary));
				attrIDs = new String[]{ attributeName, attributeNameBinary };
			}

			/* Search for objects that have those matching attributes. */
			NamingEnumeration<?> answer = null;
			try {
				answer = context.search(specificDN, matchAttrs, attrIDs);
			} catch (NameNotFoundException nnfe) {
				/* NOTE: No results found. Fine. */
			}

			toList(answer, attributeName, result);

		} catch (NamingException e) {
			e.printStackTrace();
		}
		return result;
	}

	private void toList(NamingEnumeration<?> answer, String attributeName, List<byte[]> result) throws NamingException {
		int resultCount = 0;
		for (; answer != null && answer.hasMore(); resultCount++) {
			SearchResult searchResult = (SearchResult)answer.next();

			int attributeCount = 0;
			Attributes attributes = searchResult.getAttributes();
			for (NamingEnumeration<?> ae = attributes.getAll(); ae.hasMore(); attributeCount++) {
				Attribute attribute = (Attribute)ae.next();

				/* Name */
				String foundAttributeName = attribute.getID();
				//				LOGGER.info("DEBUG: found attributeName " + foundAttributeName);
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
			//			LOGGER.info("DEBUG: attributeCount = " + attributeCount);
		}
		//		LOGGER.info("DEBUG: resultCount = " + resultCount);
	}
}
