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
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import junit.framework.TestCase;

import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.lds.ChipAuthenticationInfo;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.DG14File;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.TerminalAuthenticationInfo;

public class DG14FileTest extends TestCase {

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();
	static {
		Security.addProvider(BC_PROVIDER);
	}

	public void testConstruct() {
		try {
			Map<Integer, PublicKey> keys = new TreeMap<Integer, PublicKey>();

			/* Using BC here, since SunJCE doesn't support EC. */
			KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance("EC", BC_PROVIDER);
			keyGen1.initialize(192);
			KeyPair keyPair1 = keyGen1.generateKeyPair();
			assertNotNull(keyPair1);

			/* Using SunJCE here, since BC sometimes hangs?!?! Bug in BC?
			 *
			 * FIXME: This happened to MO on WinXP, Eclipse 3.4, Sun JDK1.6.0_15,
			 * not tested on other platforms... replace "SunJCE" with "BC" and see
			 * if this test hangs forever.
			 */
			KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance("DH", "SunJCE");

			System.out.println("DEBUG: DG14FileTest: Generating key pair 2");
			KeyPair keyPair2 = keyGen2.generateKeyPair();
			assertNotNull(keyPair2);

			PublicKey publicKey1 = keyPair1.getPublic();
			assertNotNull(publicKey1);
			PublicKey publicKey2 = keyPair2.getPublic();
			assertNotNull(publicKey2);

			keys.put(1, publicKey1);
			keys.put(2, publicKey2);

			System.out.println("DEBUG: publicKey1.getAlgorithm() = " + publicKey1.getAlgorithm());
			System.out.println("DEBUG: publicKey2.getAlgorithm() = " + publicKey2.getAlgorithm());

			Map<Integer, String> algs = new TreeMap<Integer, String>();
			algs.put(1, SecurityInfo.ID_CA_DH_3DES_CBC_CBC_OID);
			algs.put(2, SecurityInfo.ID_CA_ECDH_3DES_CBC_CBC_OID);

			List<SecurityInfo> securityInfos = new ArrayList<SecurityInfo>();
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey1, BigInteger.valueOf(1)));
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey2, BigInteger.valueOf(2)));
			securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_DH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM, BigInteger.valueOf(1)));
			securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM, BigInteger.valueOf(2)));
			securityInfos.add(new TerminalAuthenticationInfo());
			DG14File dg14File2 = new DG14File(securityInfos);
			assertNotNull(dg14File2.getChipAuthenticationInfos());
			assertNotNull(dg14File2.getChipAuthenticationPublicKeyInfos());
			assertNotNull(dg14File2.getSecurityInfos());
			assertNotNull(dg14File2.getCVCAFileIds());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public void testEncodeDecode() {
		try {
			DG14File dg14 = getSampleObject();
			byte[] encoded = dg14.getEncoded();
			assertNotNull(encoded);

			DG14File copy = new DG14File(new ByteArrayInputStream(encoded));
			assertEquals(dg14, copy);

			byte[] copyEncoded = dg14.getEncoded();
			assertNotNull(copyEncoded);

			DG14File copyOfCopy = new DG14File(new ByteArrayInputStream(copyEncoded));
			assertEquals(dg14, copyOfCopy);
			assertEquals(copyOfCopy, copy);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testDecodeEncode() {
		DG14File dg14 = getSampleObject();
		Collection<SecurityInfo> securityInfos = dg14.getSecurityInfos();
		assertNotNull(securityInfos);
		DG14File copy = new DG14File(securityInfos);
		assertEquals(dg14, copy);

		byte[] encoded = dg14.getEncoded();
		assertNotNull(encoded);
		byte[] copyEncoded = copy.getEncoded();
		assertNotNull(copyEncoded);
		assertTrue(Arrays.equals(encoded, copyEncoded));
	}

	public void testDecodeEncode1() {
		try {
			DG14File dg14 = getSampleObject();
			byte[] encoded = dg14.getEncoded();
			DG14File copy = new DG14File(new ByteArrayInputStream(encoded));
			byte[] copyEncoded = copy.getEncoded();
			assert(Arrays.equals(encoded, copyEncoded));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSpecSample() {
		try {
			byte[] specSample = getSpecSampleDG14File();
			DG14File dg14 = new DG14File(new ByteArrayInputStream(specSample));
			byte[] encoded = dg14.getEncoded();
			assert(Arrays.equals(specSample, encoded));
			DG14File copy = new DG14File(new ByteArrayInputStream(encoded));
			assertEquals(dg14, copy);

			Collection<SecurityInfo> securityInfos = dg14.getSecurityInfos();
			for (SecurityInfo securityInfo: securityInfos) {
				//				System.out.println("DEBUG: securityInfo " + securityInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testBenali() {
		byte[] dg14bytes = new byte[] { (byte)0x6E, (byte)0x82, (byte)0x01, (byte)0x91, (byte)0x31, (byte)0x82, (byte)0x01, (byte)0x8D, (byte)0x30, (byte)0x82, (byte)0x01, (byte)0x57, (byte)0x06, (byte)0x09, (byte)0x04, (byte)0x00,
				(byte)0x7F, (byte)0x00, (byte)0x07, (byte)0x02, (byte)0x02, (byte)0x01, (byte)0x02, (byte)0x30, (byte)0x82, (byte)0x01, (byte)0x36, (byte)0x30, (byte)0x81, (byte)0xEF, (byte)0x06, (byte)0x07,
				(byte)0x2A, (byte)0x86, (byte)0x48, (byte)0xCE, (byte)0x3D, (byte)0x02, (byte)0x01, (byte)0x30, (byte)0x81, (byte)0xE3, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x30, (byte)0x2C, (byte)0x06,
				(byte)0x07, (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0xCE, (byte)0x3D, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x21, (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
				(byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xFF,
				(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x30, (byte)0x44, (byte)0x04, (byte)0x20, (byte)0xFF,
				(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
				(byte)0x00, (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFC, (byte)0x04,
				(byte)0x20, (byte)0x5A, (byte)0xC6, (byte)0x35, (byte)0xD8, (byte)0xAA, (byte)0x3A, (byte)0x93, (byte)0xE7, (byte)0xB3, (byte)0xEB, (byte)0xBD, (byte)0x55, (byte)0x76, (byte)0x98, (byte)0x86,
				(byte)0xBC, (byte)0x65, (byte)0x1D, (byte)0x06, (byte)0xB0, (byte)0xCC, (byte)0x53, (byte)0xB0, (byte)0xF6, (byte)0x3B, (byte)0xCE, (byte)0x3C, (byte)0x3E, (byte)0x27, (byte)0xD2, (byte)0x60,
				(byte)0x4B, (byte)0x04, (byte)0x41, (byte)0x04, (byte)0x6B, (byte)0x17, (byte)0xD1, (byte)0xF2, (byte)0xE1, (byte)0x2C, (byte)0x42, (byte)0x47, (byte)0xF8, (byte)0xBC, (byte)0xE6, (byte)0xE5,
				(byte)0x63, (byte)0xA4, (byte)0x40, (byte)0xF2, (byte)0x77, (byte)0x03, (byte)0x7D, (byte)0x81, (byte)0x2D, (byte)0xEB, (byte)0x33, (byte)0xA0, (byte)0xF4, (byte)0xA1, (byte)0x39, (byte)0x45,
				(byte)0xD8, (byte)0x98, (byte)0xC2, (byte)0x96, (byte)0x4F, (byte)0xE3, (byte)0x42, (byte)0xE2, (byte)0xFE, (byte)0x1A, (byte)0x7F, (byte)0x9B, (byte)0x8E, (byte)0xE7, (byte)0xEB, (byte)0x4A,
				(byte)0x7C, (byte)0x0F, (byte)0x9E, (byte)0x16, (byte)0x2B, (byte)0xCE, (byte)0x33, (byte)0x57, (byte)0x6B, (byte)0x31, (byte)0x5E, (byte)0xCE, (byte)0xCB, (byte)0xB6, (byte)0x40,
				(byte)0x68, (byte)0x37, (byte)0xBF, (byte)0x51, (byte)0xF5, (byte)0x02, (byte)0x21, (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
				(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xBC, (byte)0xE6, (byte)0xFA, (byte)0xAD, (byte)0xA7, (byte)0x17, (byte)0x9E, (byte)0x84,
				(byte)0xF3, (byte)0xB9, (byte)0xCA, (byte)0xC2, (byte)0xFC, (byte)0x63, (byte)0x25, (byte)0x51, (byte)0x02, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x03, (byte)0x42,
				(byte)0x00, (byte)0x04, (byte)0xD9, (byte)0x5B, (byte)0x52, (byte)0x56, (byte)0x11, (byte)0x6E, (byte)0x04, (byte)0xD9, (byte)0xC3, (byte)0x76, (byte)0xC6, (byte)0xB5, (byte)0x9D, (byte)0x07,
				(byte)0x6F, (byte)0x4A, (byte)0x2A, (byte)0x93, (byte)0x2B, (byte)0xC3, (byte)0x60, (byte)0x65, (byte)0x41, (byte)0xC1, (byte)0x93, (byte)0x1E, (byte)0x39, (byte)0x7F, (byte)0xF6, (byte)0xE5,
				(byte)0xE7, (byte)0x1B, (byte)0x24, (byte)0x1C, (byte)0x26, (byte)0x31, (byte)0x69, (byte)0x07, (byte)0x09, (byte)0xB6, (byte)0x6F, (byte)0x31, (byte)0xE9, (byte)0xBC, (byte)0x09, (byte)0xEF,
				(byte)0x7E, (byte)0xEB, (byte)0x74, (byte)0x4A, (byte)0x10, (byte)0x18, (byte)0x5C, (byte)0xBF, (byte)0x46, (byte)0x29, (byte)0xEF, (byte)0x45, (byte)0xE8, (byte)0xA1, (byte)0x44, (byte)0xC1,
				(byte)0xC9, (byte)0xDD, (byte)0x02, (byte)0x10, (byte)0x41, (byte)0x6C, (byte)0x67, (byte)0x65, (byte)0x72, (byte)0x69, (byte)0x61, (byte)0x43, (byte)0x41, (byte)0x4B, (byte)0x65, (byte)0x79,
				(byte)0x4E, (byte)0x61, (byte)0x6D, (byte)0x65, (byte)0x30, (byte)0x21, (byte)0x06, (byte)0x0A, (byte)0x04, (byte)0x00, (byte)0x7F, (byte)0x00, (byte)0x07, (byte)0x02, (byte)0x02, (byte)0x03,
				(byte)0x02, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x10, (byte)0x41, (byte)0x6C, (byte)0x67, (byte)0x65, (byte)0x72, (byte)0x69, (byte)0x61, (byte)0x43, (byte)0x41,
				(byte)0x4B, (byte)0x65, (byte)0x79, (byte)0x4E, (byte)0x61, (byte)0x6D, (byte)0x65, (byte)0x30, (byte)0x0D, (byte)0x06, (byte)0x08, (byte)0x04, (byte)0x00, (byte)0x7F, (byte)0x00, (byte)0x07,
				(byte)0x02, (byte)0x02, (byte)0x02, (byte)0x02, (byte)0x01, (byte)0x01 };

		try {
//			FileOutputStream dg14Out = new FileOutputStream("c:/dg14out.bin");
//			dg14Out.write(dg14bytes);
//			dg14Out.flush();
//			dg14Out.close();
			
			DG14File dg14 = new DG14File(new ByteArrayInputStream(dg14bytes));
			for (SecurityInfo securityInfo: dg14.getSecurityInfos()) {
				System.out.println("DEBUG: securityInfo = " + securityInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public DG14File getSampleObject() {
		try {
			/* Using BC here, since SunJCE doesn't support EC. */
			KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance("EC", BC_PROVIDER);
			keyGen1.initialize(192);
			KeyPair keyPair1 = keyGen1.generateKeyPair();

			/* Using SunJCE here, since BC sometimes hangs?!?! Bug in BC?
			 *
			 * FIXME: This happened to MO on WinXP, Eclipse 3.4, Sun JDK1.6.0_15,
			 * not tested on other platforms... replace "SunJCE" with "BC" and see
			 * if this test halts forever.
			 */
			KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance("DH", "SunJCE");

			KeyPair keyPair2 = keyGen2.generateKeyPair();

			PublicKey publicKey1 = keyPair1.getPublic();
			PublicKey publicKey2 = keyPair2.getPublic();

			List<SecurityInfo> securityInfos = new ArrayList<SecurityInfo>();
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey1, BigInteger.valueOf(1)));
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey2, BigInteger.valueOf(2)));	
			securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_DH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM, BigInteger.valueOf(1)));
			securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM, BigInteger.valueOf(2)));
			securityInfos.add(new TerminalAuthenticationInfo());
			DG14File dg14 = new DG14File(securityInfos);
			return dg14;
		} catch(Exception e) {
			fail(e.getMessage());
			return null;
		}
	}

	/**
	 * Figure D.3 from TR 03110 v1.11, specifies DG14 (DH).
	 * 
	 * @return
	 */
	public static byte[] getSpecSampleDG14File() {
		return new byte[] {
				/*0000:*/ 0x6E, (byte)0x82, 0x01, (byte)0xDC, 0x31, (byte)0x82, 0x01, (byte)0xD8, 0x30, (byte)0x82, 0x01, (byte)0xB4, 0x06, 0x09, 0x04, 0x00, 0x7F, 0x00, 0x07, 0x02, 0x02, 0x01, 0x01, 0x30, (byte)0x82, 0x01, (byte)0xA5, 0x30, (byte)0x82, 0x01, 0x1A, 0x06,
				/*0020:*/ (byte)0x09, 0x2A, (byte)0x86, 0x48, (byte)0x86, (byte)0xF7, 0x0D, 0x01, 0x03, 0x01, 0x30, (byte)0x82, 0x01, 0x0B, 0x02, (byte)0x81, (byte)0x81, 0x00, (byte)0xDC, (byte)0xB5, 0x54, (byte)0xDF, (byte)0x8C, 0x69, 0x31, (byte)0xE8, 0x65, (byte)0xC1, (byte)0xB5, (byte)0x88, 0x27, 0x3D,
				/*0040:*/ (byte)0x80, (byte)0xA2, (byte)0xD8, 0x7A, (byte)0xB5, 0x39, (byte)0xC5, (byte)0xE4, (byte)0xA0, 0x74, (byte)0xB4, 0x02, 0x49, (byte)0xFF, 0x65, 0x5A, (byte)0x9A, (byte)0xB8, 0x30, 0x63, 0x3B, 0x45, 0x7C, 0x4C, (byte)0xF8, (byte)0x85, (byte)0xE3, 0x1C, (byte)0xD7, (byte)0x9F, (byte)0x81, 0x14,
				/*0060:*/ (byte)0x8C, (byte)0x8A, 0x68, (byte)0xD1, (byte)0xDB, (byte)0xFC, 0x2F, 0x7B, 0x70, (byte)0xED, 0x55, (byte)0xC0, 0x38, 0x7C, 0x23, (byte)0xA0, 0x47, (byte)0x9A, (byte)0x95, 0x72, (byte)0xE8, (byte)0xA6, 0x71, 0x4F, 0x41, (byte)0x8A, 0x6B, (byte)0xF9, (byte)0xB0, 0x0E, (byte)0xC5, (byte)0xBC,
				/*0080:*/ (byte)0x4D, (byte)0xEF, 0x25, 0x5A, (byte)0x94, (byte)0x85, 0x05, (byte)0x8A, 0x42, 0x71, 0x00, (byte)0x8B, (byte)0xA6, (byte)0x94, (byte)0xAA, 0x62, (byte)0xCC, 0x18, 0x38, 0x5E, (byte)0xF9, (byte)0xD7, (byte)0xB6, (byte)0xE8, 0x33, (byte)0xA7, 0x08, (byte)0x8A, (byte)0xC8, 0x17, (byte)0xAA, 0x1F,
				/*00A0:*/ (byte)0x9B, (byte)0x93, (byte)0xA8, 0x6B, (byte)0x98, 0x3E, (byte)0xAB, 0x73, (byte)0xC1, 0x58, (byte)0x84, (byte)0xE7, 0x33, 0x66, 0x56, 0x59, (byte)0xCA, 0x7D, 0x02, (byte)0x81, (byte)0x80, 0x2E, 0x69, (byte)0xFE, (byte)0x94, (byte)0xD3, (byte)0xC0, (byte)0xA4, 0x37, (byte)0x8C, (byte)0x8A, 0x47,
				/*00C0:*/ (byte)0x9D, (byte)0x83, 0x09, 0x1A, (byte)0xED, 0x41, (byte)0x92, 0x34, 0x25, (byte)0xC1, 0x03, 0x00, (byte)0x8C, 0x6A, (byte)0xB3, (byte)0xF6, (byte)0xE8, 0x3E, 0x20, (byte)0xCB, 0x16, (byte)0xC4, (byte)0xAE, 0x0B, 0x0E, 0x28, (byte)0xED, (byte)0x9B, (byte)0xC7, (byte)0x9C, (byte)0xD7, (byte)0xD7,
				/*00E0:*/ (byte)0xE9, (byte)0xDF, (byte)0xD3, (byte)0x9D, (byte)0xD0, (byte)0xA3, (byte)0x91, 0x41, (byte)0xF2, (byte)0xDD, 0x57, 0x14, (byte)0x9A, (byte)0xB6, (byte)0x88, (byte)0xDB, (byte)0xAD, 0x17, 0x7C, 0x68, 0x6F, 0x77, 0x18, 0x28, (byte)0xE5, (byte)0xA0, 0x44, 0x08, 0x51, 0x2F, 0x15, 0x64,
				/*0100:*/ 0x74, (byte)0xB0, (byte)0xBF, (byte)0xD4, 0x30, (byte)0xCB, (byte)0xBF, (byte)0x91, (byte)0xC0, 0x15, (byte)0x89, (byte)0xE7, 0x21, (byte)0xDD, (byte)0xDF, (byte)0xFC, (byte)0xDF, 0x45, 0x00, 0x43, (byte)0xEB, 0x77, 0x1E, 0x61, 0x08, 0x4C, 0x59, 0x7F, 0x7A, (byte)0xEA, (byte)0x90, 0x48,
				/*0120:*/ 0x42, 0x0A, 0x21, (byte)0x80, (byte)0xEB, (byte)0xFE, (byte)0xC1, (byte)0xB3, (byte)0xB9, 0x3C, 0x1A, 0x6C, (byte)0xB1, (byte)0xAD, 0x38, (byte)0xB3, (byte)0x98, 0x4F, (byte)0xF0, 0x52, 0x10, 0x02, 0x02, 0x03, (byte)0xF9, 0x03, (byte)0x81, (byte)0x84, 0x00, 0x02, (byte)0x81, (byte)0x80,
				/*0140:*/ 0x55, 0x3C, (byte)0xE7, 0x35, (byte)0xEC, (byte)0xF5, (byte)0xCB, (byte)0xF2, 0x02, (byte)0x9D, 0x30, (byte)0xFA, (byte)0xA4, (byte)0xF9, 0x73, 0x35, (byte)0xDF, 0x40, 0x40, 0x47, (byte)0xE4, (byte)0xF8, 0x58, 0x6D, 0x76, (byte)0xA7, (byte)0xD2, 0x21, (byte)0xA0, (byte)0x9E, 0x7F, 0x55,
				/*0160:*/ (byte)0xBB, (byte)0xE2, 0x55, (byte)0xC6, 0x58, 0x7B, (byte)0xF2, (byte)0x88, 0x5D, 0x41, (byte)0xB7, (byte)0x86, (byte)0xBC, (byte)0xEF, 0x21, 0x77, (byte)0xD5, 0x2B, (byte)0xF3, (byte)0xCD, (byte)0xBA, 0x78, 0x5D, 0x37, (byte)0xD7, 0x0B, (byte)0x88, (byte)0xD6, (byte)0xAB, 0x4E, 0x1C, (byte)0xA6,
				/*0180:*/ 0x6A, 0x63, (byte)0xB6, 0x01, 0x13, 0x76, (byte)0xED, 0x44, 0x44, 0x4A, 0x66, 0x2B, (byte)0xD0, (byte)0xDC, (byte)0x95, 0x24, 0x17, 0x6E, (byte)0x97, 0x12, (byte)0x87, (byte)0xAD, 0x41, (byte)0xD2, (byte)0x9B, (byte)0xED, 0x3D, 0x35, (byte)0xEA, (byte)0xC7, (byte)0xD3, (byte)0x9C,
				/*01A0:*/ (byte)0xA7, 0x3E, (byte)0xCB, 0x2A, 0x3B, 0x4D, 0x39, 0x67, 0x1C, (byte)0xE4, 0x12, 0x5C, (byte)0x92, 0x65, (byte)0x8C, 0x5B, (byte)0xF3, (byte)0xDE, (byte)0xDA, (byte)0x91, 0x5E, (byte)0xD7, 0x1B, (byte)0x88, (byte)0xFC, 0x03, 0x1B, (byte)0xAB, (byte)0x88, 0x72, 0x48, (byte)0xA1,
				/*01C0:*/ 0x30, 0x0F, 0x06, 0x0A, 0x04, 0x00, 0x7F, 0x00, 0x07, 0x02, 0x02, 0x03, 0x01, 0x01, 0x02, 0x01, 0x01, 0x30, 0x0D, 0x06, 0x08, 0x04, 0x00, 0x7F, 0x00, 0x07, 0x02, 0x02, 0x02, 0x02, 0x01, 0x01
		};
	}

	public boolean equalsDHPublicKey(DHPublicKey thisPublicKey, Object other) {
		if (!(other instanceof DHPublicKey)) { return false; }
		DHPublicKey otherPublicKey = (DHPublicKey)other;
		DHParameterSpec params1 = thisPublicKey.getParams();
		DHParameterSpec params2 = otherPublicKey.getParams();
		return thisPublicKey.getAlgorithm().equals(otherPublicKey.getAlgorithm())
				&& params1.getL()== params2.getL()
				&& params1.getG().equals(params2.getG())
				&& params1.getP().equals(params2.getP())
				&& thisPublicKey.getY().equals(otherPublicKey.getY());
	}
}
