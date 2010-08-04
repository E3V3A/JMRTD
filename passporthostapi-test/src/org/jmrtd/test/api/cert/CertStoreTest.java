package org.jmrtd.test.api.cert;

import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.LDAPCertStoreParameters;
import java.security.cert.X509CertSelector;
import java.util.Collection;

import junit.framework.TestCase;


public class CertStoreTest extends TestCase
{
	public void testLDAPCertStore() {
//		connectLDAPCertStore();
	}
	
	private void connectLDAPCertStore() {
		try {
			CertStoreParameters params = new LDAPCertStoreParameters("motest");
			CertStore cs = CertStore.getInstance("LDAP", params);
			X509CertSelector selector = new X509CertSelector();

			selector.setSubject("CN=Document Signing Key 15,OU=London,O=ukps,C=gb");
			selector.setIssuer("CN=Country Signing Authority,O=UKKPA,C=GB");
			
//			subject:
//				   CN=Document Signing Key 15
//				   OU=london
//				   O=ukps
//				   C=gb
//
//				issuer:
//				   CN=Country Signing Authority
//				   O=UKKPA
//				   C=gb

			Collection<? extends Certificate> certificates = cs.getCertificates(selector);
			System.out.println("DEBUG: certificates.size() == " + certificates.size());
			for (Certificate certificate: certificates) {
				System.out.println(certificate);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
