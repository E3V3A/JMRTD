package org.jmrtd;

import java.math.BigInteger;
import java.net.URI;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

public abstract class TrustStore
{
	public abstract URI getLocation();

	/**
	 * Gets all certificates in this store.
	 * 
	 * @return all certificates in this store
	 */
	public abstract Collection<Certificate> getCertificates();
	
	public abstract Collection<Key> getKeys();
	
	/**
	 * Gets the certification chain for the argument certificate.
	 * Returns a list of length 1 is the argument certificate itself occurs
	 * @param docCertificate
	 * @return
	 */
	public List<Certificate> getCertificateChain(Certificate docCertificate) {
		Collection<Certificate> certificates = getCertificates();
		List<Certificate> chain = new LinkedList<Certificate>();
		if (certificates.contains(docCertificate)) {
			chain.add(docCertificate);
		}
		if (!(docCertificate instanceof X509Certificate)) {
			return chain;
		}
		X509Certificate x509DocCertificate = (X509Certificate)docCertificate;
		if (isSelfSigned(x509DocCertificate)) {	
			return chain;
		}
		X509Certificate issuerCertificate = getSignerCertificate(x509DocCertificate);
		if (issuerCertificate == null) {
			return chain;
		}
		chain.add(docCertificate);
		chain.addAll(getCertificateChain(issuerCertificate)); /* NOTE: recursion! */
		return chain;
	}

	public List<Certificate> getCertificateChain(X500Principal issuer, BigInteger serialNumber) {
		X509Certificate issuerCertificate = (X509Certificate)getSignerCertificate(issuer, serialNumber);
		List<Certificate> chain = getCertificateChain(issuerCertificate);
		return chain;
	}

	/**
	 * Gets issuer certificate given a subject certificate's issuer principal and serial number.
	 * 
	 * @param docIssuer
	 * @return
	 */
	private Certificate getSignerCertificate(X500Principal docIssuer, BigInteger serialNumber) {
		Collection<Certificate> certificates = getCertificates();
		for (Certificate certificate: certificates) {
			if (certificate instanceof X509Certificate) {
				X509Certificate x509Certificate = (X509Certificate)certificate;
				X500Principal certSubject = x509Certificate.getSubjectX500Principal();
				BigInteger certSerialNumber = x509Certificate.getSerialNumber();
				if (docIssuer.equals(certSubject) && serialNumber.equals(certSerialNumber)) {
					return certificate;
				}
			}
		}
		return null;
	}

	private X509Certificate getSignerCertificate(X509Certificate docSignerCertificate) {
		Collection<Certificate> certificates = getCertificates();
		X500Principal docIssuer = docSignerCertificate.getIssuerX500Principal();
		for (Certificate certificate: certificates) {
			if (certificate instanceof X509Certificate) {
				X509Certificate x509Certificate = (X509Certificate)certificate;
				X500Principal certSubject = x509Certificate.getSubjectX500Principal();
				if (docIssuer.equals(certSubject)) {
					return x509Certificate;
				}
			}
		}
		return null;
	}

	private boolean isSelfSigned(X509Certificate certificate) {
		if (certificate == null) { return false; }
		X500Principal docCertificateIssuer = certificate.getIssuerX500Principal();
		X500Principal docCertificateSubject = certificate.getSubjectX500Principal();
		return docCertificateIssuer.equals(docCertificateSubject);
	}
}
