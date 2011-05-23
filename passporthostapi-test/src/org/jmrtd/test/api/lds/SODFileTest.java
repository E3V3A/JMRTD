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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.PassportFile;
import org.jmrtd.lds.SODFile;

public class SODFileTest extends TestCase {

	/** We need this for SHA-256 (and probably more). */
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
	
	public void testSignature() {
		testSignature(createTestObject());
	}
	
	public void testSignature(SODFile sodFile) {
		try {
			X509Certificate certificate = sodFile.getDocSigningCertificate();
			assertTrue(sodFile.checkDocSignature(certificate));
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
//			hashes.put(15, digest.digest(dg15Bytes));
//			byte[] encryptedDigest = new byte[128]; // Arbitrary value. Use a private key to generate a real signature?
			
			SODFile sod = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, privateKey, docSigningCert);

			List<Integer> dgPresenceList = Arrays.asList(new Integer[] { PassportFile.EF_DG1_TAG, PassportFile.EF_DG2_TAG });
			COMFile com = new COMFile("01", "07", "04", "00", "00", dgPresenceList);
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

	public void testFile(InputStream in) {
		try {
			testReflexive(new SODFile(in));
		} catch (Exception e) {
			fail(e.toString());
		}
	}
}
