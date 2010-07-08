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
import java.io.FileInputStream;
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

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.SODFile;

public class SODFileTest extends TestCase {

	/** We need this for SHA256 (and probably more). */
	private static final Provider PROVIDER =
		new org.bouncycastle.jce.provider.BouncyCastleProvider();

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
			fail(e.toString());
		}
	}

	public static SODFile createTestObject() {
		try {
			Security.insertProviderAt(PROVIDER, 4);

			Date today = Calendar.getInstance().getTime();
			DG1File dg1File = DG1FileTest.createTestObject();
			byte[] dg1Bytes = dg1File.getEncoded();
			DG2File dg2File = DG2FileTest.createTestObject();
			byte[] dg2Bytes = dg2File.getEncoded();			
			DG15File dg15File = DG15FileTest.createTestObject();
			byte[] dg15Bytes = dg15File.getEncoded();

			KeyPair keyPair = createTestKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();
			Date dateOfIssuing = today;
			Date dateOfExpiry = today;
			String digestAlgorithm = "SHA256";
			String signatureAlgorithm = "SHA256withRSA";
			X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
			certGenerator.setSerialNumber(new BigInteger("1"));
			certGenerator.setIssuerDN(new X509Name("C=NL, O=State of the Netherlands, OU=Ministry of the Interior and Kingdom Relations, CN=CSCA NL"));
			certGenerator.setSubjectDN(new X509Name("C=NL, O=State of the Netherlands, OU=Ministry of the Interior and Kingdom Relations, CN=DS-01 NL, OID.2.5.4.5=1"));
			certGenerator.setNotBefore(dateOfIssuing);
			certGenerator.setNotAfter(dateOfExpiry);
			certGenerator.setPublicKey(publicKey);
			certGenerator.setSignatureAlgorithm(signatureAlgorithm);
			X509Certificate docSigningCert = (X509Certificate)certGenerator.generate(privateKey, "BC");
			Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
			MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
			hashes.put(1, digest.digest(dg1Bytes));
			hashes.put(2, digest.digest(dg2Bytes));
			hashes.put(15, digest.digest(dg15Bytes));
			byte[] encryptedDigest = new byte[128]; // Arbitrary value. Use a private key to generate a real signature?
			return new SODFile(digestAlgorithm, signatureAlgorithm, hashes, encryptedDigest, docSigningCert);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static KeyPair createTestKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		return keyPairGenerator.generateKeyPair();
	}

	public void testDavidEid1() {
		try {
			Provider[] providers = 	Security.getProviders();
			for (Provider provider: providers) {
				System.out.println("Security provider: " + provider);
			}

			//			SODFile sodFile = new SODFile(new FileInputStream("/home/martijno/Downloads/SmartCard_EF.SOD.dat"));
			SODFile sodFile = new SODFile(new FileInputStream("t:/paspoort/david_eid_sod.bin"));
			X509Certificate cert = sodFile.getDocSigningCertificate();
			System.out.println(cert.toString());
			System.out.println(cert.getSerialNumber());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public void testDavidEid2() {
		try {
			//			SODFile sodFile = new SODFile(new FileInputStream("/home/martijno/Downloads/SmartCard_EF.SOD.dat"));
			//			SODFile sodFile = new SODFile(new FileInputStream("t:/paspoort/DavidEid_falsified_EF.SOD.bin"));
			SODFile sodFile = new SODFile(new FileInputStream("t:/paspoort/davideid3.bin"));
			X509Certificate sodcert = sodFile.getDocSigningCertificate();
			boolean result = sodFile.checkDocSignature(sodcert);
			System.out.println("DEBUG: result = " + result);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testFile(InputStream in) {
		try {
			testReflexive(new SODFile(in));
		} catch (Exception e) {
			fail(e.toString());
		}
	}
}
