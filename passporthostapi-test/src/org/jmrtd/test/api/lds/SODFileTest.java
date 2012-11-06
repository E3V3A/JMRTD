/*
 *  JMRTD Tests.
 *
 *  Copyright (C) 2009  The JMRTD team
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  $Id: $
 */

package org.jmrtd.test.api.lds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.SODFile;

public class SODFileTest extends TestCase {

	/** We need this for SHA-256 (and probably more). */
	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();
	private static final String BC_PROVIDER_NAME = BC_PROVIDER == null ? null : BC_PROVIDER.getName();

	public SODFileTest(String name) {
		super(name);
	}

	public void testReflexive() {
		testReflexive(createTestObject());
	}

	public void testReflexive(SODFile sodFile) {
		try {
			byte[] encoded = sodFile.getEncoded();
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			SODFile copy = new SODFile(in);
			assertEquals(sodFile, copy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testSignature() {
		testSignature(createTestObject());
	}

	public void testSignature(SODFile sodFile) {
		try {
			X509Certificate certificate = sodFile.getDocSigningCertificate();
			assertNotNull(certificate);
			assertTrue(sodFile.checkDocSignature(certificate));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	private static KeyPair createTestKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		return keyPairGenerator.generateKeyPair();
	}

	public void testSODInFile(File file) {
		try {
			Provider[] providers = 	Security.getProviders();
			for (Provider provider: providers) {
				System.out.println("Security provider: " + provider);
			}
			SODFile sodFile = new SODFile(new FileInputStream(file));
			X509Certificate cert = sodFile.getDocSigningCertificate();
			System.out.println(cert.toString());
			System.out.println(cert.getSerialNumber());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return; // inconclusive!
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public void testFields() {
		testFields(createTestObject());
	}

	public void testFields(SODFile sodFile) {
		try {
			String ldsVersion = sodFile.getLDSVersion();
			assertTrue(ldsVersion == null || ldsVersion.length() == "aabb".length());

			String unicodeVersion = sodFile.getUnicodeVersion();
			assertTrue(unicodeVersion == null || unicodeVersion.length() == "aabbcc".length());

			X509Certificate certificate = sodFile.getDocSigningCertificate();
			
			BigInteger serialNumber = sodFile.getSerialNumber();

			if (serialNumber != null && certificate != null) {
				assertTrue("serialNumber = " + serialNumber + ", certificate.getSerialNumber() = " + certificate.getSerialNumber(),
					serialNumber.equals(certificate.getSerialNumber()));
			}

			X500Principal issuer = sodFile.getIssuerX500Principal();
			
			System.out.println("DEBUG: issuer = " + issuer);
			
			String issuerName = issuer.getName(X500Principal.RFC2253);
			assertNotNull(issuerName);
			
			if (issuer != null && certificate != null) {
				X500Principal certIssuer = certificate.getIssuerX500Principal();
				System.out.println("DEBUG: certIssuer = " + certIssuer);
				String certIssuerName = certIssuer.getName(X500Principal.RFC2253);
				assertNotNull(certIssuerName);
//				assertTrue("issuerName = \"" + issuerName + "\", certIssuerName = \"" + certIssuerName + "\"",
//						certIssuerName.equals(issuerName));
			}

			String digestAlgorithm = sodFile.getDigestAlgorithm();
			String digestEncryptionAlgorithm = sodFile.getDigestEncryptionAlgorithm();

			assertNotNull(digestAlgorithm);
			assertNotNull(digestEncryptionAlgorithm);
		} catch (Exception ce) {
			ce.printStackTrace();
			fail(ce.getMessage());
		}
	}

	public void testFile(InputStream in) {
		try {
			SODFile sodFile = new SODFile(in);
			testReflexive(sodFile);
			testFields(sodFile);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	public void testMustermann() {
		testFile(createMustermannSampleInputStream());
	}

	public static SODFile createTestObject() {
		try {
			Security.insertProviderAt(BC_PROVIDER, 4);

			Date today = Calendar.getInstance().getTime();
			DG1File dg1File = DG1FileTest.createTestObject();
			byte[] dg1Bytes = dg1File.getEncoded();
			DG2File dg2File = DG2FileTest.getTestObject();
			byte[] dg2Bytes = dg2File.getEncoded();			
			//			DG15File dg15File = DG15FileTest.createTestObject();
			//			byte[] dg15Bytes = dg15File.getEncoded();

			KeyPair keyPair = createTestKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();
			Date dateOfIssuing = today;
			Date dateOfExpiry = today;
			String digestAlgorithm = "SHA-256";
			String signatureAlgorithm = "SHA256withRSA";
			X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
			certGenerator.setSerialNumber(BigInteger.ONE);
			certGenerator.setIssuerDN(new X509Name("C=NL, O=State of the Netherlands, OU=Ministry of the Interior and Kingdom Relations, CN=CSCA NL"));
			certGenerator.setSubjectDN(new X509Name("C=NL, O=State of the Netherlands, OU=Ministry of the Interior and Kingdom Relations, CN=DS-01 NL, OID.2.5.4.5=1"));
			certGenerator.setNotBefore(dateOfIssuing);
			certGenerator.setNotAfter(dateOfExpiry);
			certGenerator.setPublicKey(publicKey);
			certGenerator.setSignatureAlgorithm(signatureAlgorithm);
			X509Certificate docSigningCert = (X509Certificate)certGenerator.generate(privateKey, BC_PROVIDER_NAME);
			Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
			MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
			hashes.put(1, digest.digest(dg1Bytes));
			hashes.put(2, digest.digest(dg2Bytes));
			//			hashes.put(15, digest.digest(dg15Bytes));
			//			byte[] encryptedDigest = new byte[128]; // Arbitrary value. Use a private key to generate a real signature?

			SODFile sod = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, privateKey, docSigningCert);

			int[] dgPresenceList = { LDSFile.EF_DG1_TAG, LDSFile.EF_DG2_TAG };
			COMFile com = new COMFile("1.7", "4.0.0", dgPresenceList);
			FileOutputStream comOut = new FileOutputStream("t:/EF_COM.bin");			
			comOut.write(com.getEncoded());
			comOut.flush();
			comOut.close();

			FileOutputStream dg1Out = new FileOutputStream("t:/DataGroup1.bin");
			dg1Out.write(dg1File.getEncoded());
			dg1Out.flush();
			dg1Out.close();

			FileOutputStream dg2Out = new FileOutputStream("t:/DataGroup2.bin");
			dg2Out.write(dg2File.getEncoded());
			dg2Out.flush();
			dg2Out.close();

			FileOutputStream sodOut = new FileOutputStream("t:/EF_SOD.bin");
			sodOut.write(sod.getEncoded());
			sodOut.flush();
			sodOut.close();

			return sod;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public InputStream createMustermannSampleInputStream() {
		try {
			return new FileInputStream("t:/paspoort/test/mustermann_EF_SOD.bin");
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
			return null;
		}
	}
}
