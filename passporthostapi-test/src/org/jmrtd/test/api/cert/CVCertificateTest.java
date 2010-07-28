package org.jmrtd.test.api.cert;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import junit.framework.TestCase;

import org.jmrtd.JMRTDSecurityProvider;

public class CVCertificateTest extends TestCase
{
//	private static final int TAG_CVCERTIFICATE_SIGNATURE = 0x5F37;

	public static final String filenameCA = "/c:/cacert.cvcert";

	public static final String filenameTerminal = "/c:/terminalcert.cvcert";

	public static final String filenameKey = "/c:/terminalkey.der";

	public CVCertificateTest() {
		Security.insertProviderAt(new JMRTDSecurityProvider(), 3);
	}

	private InputStream readTestFile() throws FileNotFoundException  {
		FileInputStream fileIn = new FileInputStream(filenameTerminal);
		return fileIn;
	}

	/**
	 * Can we construct a CVCertificate using a java.security.cert.CertificateFactory?
	 */
	public void testCertificateFactory() {
		try {
			CertificateFactory factory = CertificateFactory.getInstance("CVC");
			Certificate certificate = factory.generateCertificate(readTestFile());
		} catch(Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Can we store a CVCertificate in a java.security.KeyStore?
	 */
	public void testKeyStore() {
		try {
			CertificateFactory factory = CertificateFactory.getInstance("CVC");
			Certificate c1 = factory.generateCertificate(readTestFile());
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null);
			ks.setCertificateEntry("bla", c1);
			Certificate c2 = ks.getCertificate("bla");
			assertEquals(c1, c2);
			System.out.println("DEBUG: ks\n" + ks.size());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Some arbitrary tests.
	 */
	public void testTest() {
		try {
			CertificateFactory factory = CertificateFactory.getInstance("CVC");
			Certificate certificate = factory.generateCertificate(readTestFile());
			System.out.println("DEBUG: " + certificate);


			//			byte[] sig1 = ((CVCertificate)certificate).getCertSignatureData();
			//			byte[] sig2 = ((CVCertificate)certificate).getSignature();
			//			byte[] sig3 = (new BERTLVObject(TAG_CVCERTIFICATE_SIGNATURE, sig2)).getEncoded();
			//
			//			assertTrue(Arrays.equals(sig1, sig3));

			PublicKey pubKey = certificate.getPublicKey();
			System.out.println("DEBUG: pubKey.getAlgorithm() = " + pubKey.getAlgorithm());

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	// TODO: this is a nasty hack, but unfortunately the cv-cert lib does not
	// provide a proper API
	// call for this
	//
	// Woj, this used to live in PassportService. Moved it here... -- MO
	//
	// Using getSignature() in PassportService now, and simply prefixing that with 0x5F37 tag there.
	public byte[] getCertSignatureData(org.ejbca.cvc.CVCertificate cvCertificate) throws IOException,
	CertificateException {
		try {
			byte[] data = cvCertificate.getDEREncoded();
			byte[] body = cvCertificate.getCertificateBody().getDEREncoded();
			int index = 0;
			byte b1 = body[0];
			byte b2 = body[1];
			while (index < data.length) {
				if (data[index] == b1 && data[index + 1] == b2) {
					break;
				}
				index++;
			}
			index += body.length;
			if (index < data.length) {
				byte[] result = new byte[data.length - index];
				System.arraycopy(data, index, result, 0, result.length);
				// Sanity check:
				assert result[0] == (byte) 0x5F && result[1] == (byte) 0x37;
				return result;
			}
			return null;
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}
}
