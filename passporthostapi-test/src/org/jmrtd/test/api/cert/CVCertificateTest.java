package org.jmrtd.test.api.cert;

import java.io.FileInputStream;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import junit.framework.TestCase;

import org.jmrtd.cvc.JMRTDCVCProvider;

public class CVCertificateTest extends TestCase
{
	public static final String filenameCA = "/c:/cacert.cvcert";

	public static final String filenameTerminal = "/c:/terminalcert.cvcert";

	public static final String filenameKey = "/c:/terminalkey.der";

	public void testCVC() {
		try {
			Security.insertProviderAt(new JMRTDCVCProvider(), 3);
			FileInputStream fileIn = new FileInputStream(filenameCA);
			CertificateFactory factory = CertificateFactory.getInstance("CVC");
			Certificate certificate = factory.generateCertificate(fileIn);
			System.out.println("DEBUG: " + certificate);
			
			System.out.println("DEBUG: ");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
