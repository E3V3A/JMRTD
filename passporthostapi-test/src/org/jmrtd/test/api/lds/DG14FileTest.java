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
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECField;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
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
import org.jmrtd.lds.PACEInfo;
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
			e.printStackTrace();
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

	public void testSpecSample1() {
		try {
			byte[] encoded0 = getSpecSampleDG14File();
			DG14File file0 = new DG14File(new ByteArrayInputStream(encoded0));
			byte[] encoded1 = file0.getEncoded();
			DG14File file1 = new DG14File(new ByteArrayInputStream(encoded1));
			byte[] encoded2 = file1.getEncoded();
			System.out.println("encoded0: " + encoded0.length);
			System.out.println("encoded1: " + encoded1.length);
			System.out.println("encoded2: " + encoded2.length);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/** Tests a specific sample. */
	public void testGWSample() {
		try {
			DG14File dg14 = getGWSample();
			
			testLDS_E_3(dg14);

//			byte[] bytes = dg14.getEncoded();
//			FileOutputStream fileOutputStream = new FileOutputStream("c:/gw_dg14.bin");
//			fileOutputStream.write(bytes);
//			fileOutputStream.flush();
//			fileOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Tests from TR03105.
	 * See https://www.bsi.bund.de/EN/Publications/TechnicalGuidelines/TR03105/BSITR03105.html.
	 */

	public void testLDS_E_1() {
		try {
			Collection<DG14File> dg14s = getSampleObjects();
			for (DG14File dg14: dg14s) {
				testLDS_E_1(dg14);
			}
		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	public void testLDS_E_1(DG14File dg14) {
		byte[] encoded = dg14.getEncoded();
		assertNotNull(encoded);
		assertTrue(encoded.length > 0);
		assertEquals(encoded[0], 0x6E);

		int length = dg14.getLength();
		//		System.out.println("length = " + length + ", encoded length = " + encoded.length);
		assertTrue(length > 0);
		assertTrue(length <= encoded.length);
	}

	public void testLDS_E_2() {
		try {
			Collection<DG14File> dg14s = getSampleObjects();
			for (DG14File dg14: dg14s) {
				testLDS_E_2(dg14);
			}
		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	public void testLDS_E_2(DG14File dg14) {
		Collection<SecurityInfo> securityInfos = dg14.getSecurityInfos();
		int chipAuthenticationPublicKeyInfoCount = 0;
		for (SecurityInfo securityInfo: securityInfos) {
			if (securityInfo instanceof ChipAuthenticationPublicKeyInfo) {
				ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = (ChipAuthenticationPublicKeyInfo)securityInfo;
				String oid = chipAuthenticationPublicKeyInfo.getObjectIdentifier();
				if ("0.4.0.127.0.7.2.2.1.1".equals(oid) || "0.4.0.127.0.7.2.2.1.2".equals(oid)) {
					chipAuthenticationPublicKeyInfoCount++;
					testLDS_E_3(chipAuthenticationPublicKeyInfo);
				}
			}
		}

		/* For profiles CA_KAT, CA_ATGA:
		 *
		 * The SecurityInfos set MUST contain at least one 
		 * ChipAuthenticationPublicKeyInfo element with one of the protocol OID 
		 * defined in the EAC specification (id-PK-DH or id-PK-ECDH). The test 
		 * LDS_E_3 MUST be performed for each 
		 * ChipAuthenticationPublicKeyInfo element which has such an OID.
		 */
		assertTrue(chipAuthenticationPublicKeyInfoCount > 0);
	}

	public void testLDS_E_3() {
		try {
			Collection<DG14File> dg14s = getSampleObjects();
			for (DG14File dg14: dg14s) {
				testLDS_E_3(dg14);
			}
		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	public void testLDS_E_3(DG14File dg14) {
		try {
			Collection<SecurityInfo> securityInfos = dg14.getSecurityInfos();
			for (SecurityInfo securityInfo: securityInfos) {
				if (securityInfo instanceof ChipAuthenticationPublicKeyInfo) {
					ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = (ChipAuthenticationPublicKeyInfo)securityInfo;
					String oid = chipAuthenticationPublicKeyInfo.getObjectIdentifier();
					if ("0.4.0.127.0.7.2.2.1.1".equals(oid) || "0.4.0.127.0.7.2.2.1.2".equals(oid)) {
						testLDS_E_3(chipAuthenticationPublicKeyInfo);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * 1. The ChipAuthenticationPublicKeyInfo element must follow the ASN.1
	 *    syntax definition in the EAC specification [R2] and [R11].
	 * 2. The presence of the key reference in the
	 *    ChipAuthenticationPublicKeyInfo MUST be coherent with the ICS
	 * 3. The algorithm identifier MUST match to the Key agreement protocol and
	 *    be one of the following:
	 *       - DHKeyAgreement (OID: 1.2.840.113549.1.3.1)
	 *       - ecPublicKey (OID: 1.2.840.10045.2.1)
	 * 4. The parameters MUST follow PKCS #3 (DH) or KAEG specification
	 *    (ECDH).
	 *    For DH verify that
	 *       - 0 < g < p, that is both should be positive and g should be less than p.
	 *       - If private value length l is present, verify that l > 0 and 2l-1 < p.
	 *    In case of ECDH verify that
	 *       - prime p > 2
	 *       - curve parameter 0 <= a < p
	 *       - curve parameter 0 <= b < p
	 *       - 4*a^3 + 27*b^2 != 0
	 *       - base point G is on the curve, with both coordinates in range 0 ... p – 1 
	 *       - Cofactor f > 0 
	 *       - order r of base point r > 0 , r != p 
	 *       - r * f <= 2p 
	 */
	public void testLDS_E_3(ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo) {
		/* Pre-conditions. */
		assertNotNull(chipAuthenticationPublicKeyInfo);
		String oid = chipAuthenticationPublicKeyInfo.getObjectIdentifier();
		if (!"0.4.0.127.0.7.2.2.1.1".equals(oid) && !"0.4.0.127.0.7.2.2.1.2".equals(oid)) {
			/* FIXME: Should we fail if pre-condition is false? */
			fail("Not an appropriate ChipAuthenticationPublicKeyInfo for this test:"
					+ "was expecting 0.4.0.127.0.7.2.2.1.1 (id-PK-DH) or 0.4.0.127.0.7.2.2.1.2 (id-PK-ECDH), "
					+ "found OID " + oid);
		}

		/* 1. By definition (the fact that it is a ChipAuthenticationPublicKeyInfo object). */
		/* 2. */
		BigInteger keyId = chipAuthenticationPublicKeyInfo.getKeyId();
		assertNotNull(keyId);
		assertTrue(keyId.compareTo(BigInteger.valueOf(-1)) == 0 || keyId.compareTo(BigInteger.ZERO) > 0);

		/* 3. */
		PublicKey publicKey = chipAuthenticationPublicKeyInfo.getSubjectPublicKey();
		assertNotNull(publicKey);
		String algorithm  = publicKey.getAlgorithm();
		assertNotNull(algorithm);
		if ("0.4.0.127.0.7.2.2.1.1".equals(oid)) { assertEquals("DH", algorithm); } // id-PK-DH
		if ("0.4.0.127.0.7.2.2.1.2".equals(oid)) { assertTrue("EC".equals(algorithm) || "ECDH".equals(algorithm)); } // id-PK-ECDH

		/* 4. */
		if ("DH".equals(algorithm)) {
			/* DH case. */
			if (!(publicKey instanceof DHPublicKey)) {
				fail("Was expecting a DH public key, found " + publicKey.getClass().getSimpleName());
				DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
				//				BigInteger y = dhPublicKey.getY(); /* public value */
				DHParameterSpec params = dhPublicKey.getParams();
				BigInteger g = params.getG();
				int l = params.getL();
				BigInteger p = params.getP();

				assertTrue(BigInteger.ZERO.compareTo(g) < 0);
				assertTrue(g.compareTo(p) < 0);

				if (l != 0) {
					assertTrue(l > 0);
					assertTrue(BigInteger.valueOf(2 * l - 1).compareTo(p) < 0);
				}
			}
		} else if ("EC".equals(algorithm) || "ECDH".equals(algorithm)) {
			/* ECDH case. */
			if (!(publicKey instanceof ECPublicKey)) {
				fail("Was expecting an EC public key, found " + publicKey.getClass().getSimpleName());
			}
			ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
			ECPoint w = ecPublicKey.getW();

			ECParameterSpec params = ecPublicKey.getParams();
			assertNotNull(params);
			int coFactor = params.getCofactor();
			//			ECPoint generator = params.getGenerator();
			BigInteger order = params.getOrder();

			EllipticCurve curve = params.getCurve();
			assertNotNull(curve);
			BigInteger a = curve.getA();
			assertNotNull(a);
			BigInteger b = curve.getB();
			assertNotNull(b);

			ECField field = curve.getField();
			assertNotNull(field);
			if (!(field instanceof ECFieldFp)) {
				fail("Was expecting a prime field, found " + field.getClass().getSimpleName());
			}

			ECFieldFp primeField = (ECFieldFp)field;
			BigInteger p = primeField.getP();
			assertNotNull(p);
			assertTrue(p.compareTo(BigInteger.valueOf(2)) > 0);

			assertTrue(BigInteger.ZERO.compareTo(a) <= 0);
			assertTrue(a.compareTo(p) < 0);
			assertTrue(BigInteger.ZERO.compareTo(b) <= 0);
			assertTrue(b.compareTo(p) < 0);

			BigInteger sum = BigInteger.valueOf(4).multiply(a.pow(3)).add(BigInteger.valueOf(27).multiply(b.pow(2)));
			assertTrue(sum.compareTo(BigInteger.ZERO) != 0);

			assertTrue(isOnCurve(w, curve));

			assertTrue(coFactor > 0);

			assertTrue(order.compareTo(BigInteger.ZERO) > 0);
			assertTrue(order.compareTo(p) != 0);
			assertTrue(order.multiply(BigInteger.valueOf(coFactor)).compareTo(p.multiply(BigInteger.valueOf(2))) <= 0);
		} else {
			fail("Was expecting DH, EC or ECDH algorithm for public key. Found " + algorithm);
		}
	}

	public Collection<DG14File> getSampleObjects() {
		List<DG14File> dg14s = new ArrayList<DG14File>();
		try { dg14s.add(getSampleObject()); } catch (Exception e) { e.printStackTrace(); }
		try { dg14s.add(getGWSample()); } catch (Exception e) { e.printStackTrace(); }
		try { dg14s.add(new DG14File(new FileInputStream("samples/bsi2008/Datagroup14.bin"))); } catch (Exception e) { e.printStackTrace(); }
		return dg14s;
	}

	/** Elaborate sample. */
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
			securityInfos.add(new PACEInfo(PACEInfo.ID_PACE_DH_GM_3DES_CBC_CMAC_192, 2, PACEInfo.PARAM_ID_ECP_BRAINPOOL_P224_R1));
			DG14File dg14 = new DG14File(securityInfos);
			return dg14;
		} catch(Exception e) {
			fail(e.getMessage());
			return null;
		}
	}


	/** Sanity check. */
	public void showSecurityInfos(DG14File dg14) {
		Collection<SecurityInfo> securityInfos = dg14.getSecurityInfos();
		for (SecurityInfo securityInfo: securityInfos) {
			if (securityInfo instanceof ChipAuthenticationPublicKeyInfo) {
				ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = (ChipAuthenticationPublicKeyInfo)securityInfo;
				System.out.println("DEBUG: ChipAuthenticationPublicKeyInfo oid = " + chipAuthenticationPublicKeyInfo.getObjectIdentifier());
				System.out.println("DEBUG: ChipAuthenticationPublicKeyInfo keyId = "+ chipAuthenticationPublicKeyInfo.getKeyId());
				PublicKey publicKey = chipAuthenticationPublicKeyInfo.getSubjectPublicKey();
				String algorithm = publicKey.getAlgorithm();
				if ("EC".equals(algorithm)) {
					showSubjectPublicKeyInfo(publicKey);
				} else {
					System.out.println("Not testing this public key, algorithm = \"" + algorithm + "\"");
				}
			} else if (securityInfo instanceof ChipAuthenticationInfo) {
				ChipAuthenticationInfo chipAuthenticationInfo = (ChipAuthenticationInfo)securityInfo;
				System.out.println("DEBUG: ChipAuthenticationInfo oid = " + chipAuthenticationInfo.getObjectIdentifier());
				System.out.println("DEBUG: ChipAuthenticationInfo keyId = "+ chipAuthenticationInfo.getKeyId());
			}
		}
	}

	/** Sanity check. */
	private void showSubjectPublicKeyInfo(PublicKey publicKey) {
		ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
		ECPoint w = ecPublicKey.getW();
		System.out.println("DEBUG: w = " + toString(w));
		ECParameterSpec params = ecPublicKey.getParams();
		ECPoint g = params.getGenerator();
		System.out.println("DEBUG: g = " + toString(g));
		BigInteger order = params.getOrder();
		System.out.println("DEBUG: order = " + order); // n
		int coFactor = params.getCofactor(); // h
		System.out.println("DEBUG: coFactor = " + coFactor);
		EllipticCurve curve = params.getCurve();
		BigInteger a = curve.getA();
		System.out.println("DEBUG: A = " + a);
		BigInteger b = curve.getB();
		System.out.println("DEBUG: B = " + b);
		ECField field = curve.getField();
		if (field instanceof ECFieldFp) {
			BigInteger p = ((ECFieldFp)field).getP();
			System.out.println("DEBUG: Field is F_" + p);
		} else if (field instanceof ECFieldF2m) {
			int m = ((ECFieldF2m)field).getM();
			System.out.println("DEBUG: Field is F_2^" + m);
		}
	}
	
	private DG14File getGWSample() throws GeneralSecurityException {
		Collection<SecurityInfo> securityInfos = new ArrayList<SecurityInfo>();
		securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM));
		securityInfos.add(getGWSampleChipAuthenticationPublicKeyInfo());
		DG14File dg14 = new DG14File(securityInfos);
		return dg14;
	}
	
	private ChipAuthenticationPublicKeyInfo getGWSampleChipAuthenticationPublicKeyInfo() throws GeneralSecurityException {
		PublicKey publicKey = getGWSamplePublicKey();
		ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = new ChipAuthenticationPublicKeyInfo(publicKey);
		return chipAuthenticationPublicKeyInfo;
	}

	private PublicKey getGWSamplePublicKey() throws GeneralSecurityException {
		ECPoint w = new ECPoint(new BigInteger("104351772082740350592218973609013984525419572253879843433078472109506934675868"), new BigInteger("14166132649477039766003768534081831647667842724916480786968553069450191944115"));
		ECPoint g = new ECPoint(new BigInteger("48439561293906451759052585252797914202762949526041747995844080717082404635286"), new BigInteger("36134250956749795798585127919587881956611106672985015071877198253568414405109"));
		BigInteger n = new BigInteger("115792089210356248762697446949407573529996955224135760342422259061068512044369"); // order
		int h = 1; // co-factor
		BigInteger a = new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853948");
		BigInteger b = new BigInteger("41058363725152142129326129780047268409114441015993725554835256314039467401291");
		BigInteger p = new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853951");
		ECField field = new ECFieldFp(p);
		EllipticCurve curve = new EllipticCurve(field, a, b);
		ECParameterSpec params = new ECParameterSpec(curve, g, n, h);
		ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(w, params);
		return KeyFactory.getInstance("EC", "BC").generatePublic(publicKeySpec);
	}

	public static String toString(ECPoint ecPoint) {
		return "(" + ecPoint.getAffineX() + ", " + ecPoint.getAffineY() + ")";
	}
	
	public DG14File getSimpleObject() {
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
			securityInfos.add(new PACEInfo(PACEInfo.ID_PACE_DH_GM_3DES_CBC_CMAC_192, 2, PACEInfo.PARAM_ID_ECP_BRAINPOOL_P224_R1));
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

	private boolean isOnCurve(ECPoint point, EllipticCurve curve) {
		/* Adapted from http://stackoverflow.com/a/6664005/27190. */

		BigInteger a = curve.getA();
		BigInteger b = curve.getB();
		ECField field = curve.getField();
		BigInteger modulus = null;
		if (field instanceof ECFieldFp) {
			modulus = ((ECFieldFp)field).getP();
		} else if (field instanceof ECFieldF2m) {
			modulus = BigInteger.valueOf(2).pow(((ECFieldF2m)field).getM());
		} else {
			throw new IllegalStateException("Unexpected field type: " + field.getClass().getSimpleName());
		}

		BigInteger x = point.getAffineX();
		BigInteger y = point.getAffineY();

		/* FIXME: more efficient equals modulo possible? Use BC ECFieldElement for this? */
		BigInteger lhs = y.multiply(y);
		BigInteger rhs = x.multiply(x).multiply(x).add(a.multiply(x)).add(b);

		return lhs.mod(modulus).equals(rhs.mod(modulus));
	}
}
