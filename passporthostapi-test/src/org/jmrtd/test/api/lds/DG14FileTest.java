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
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey1, 1));
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey2, 2));	
			securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_DH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM, 1));
			securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM, 2));
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
		DG14File dg14 = getSampleObject();
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
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey1, 1));
			securityInfos.add(new ChipAuthenticationPublicKeyInfo(publicKey2, 2));	
			securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_DH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM, 1));
			securityInfos.add(new ChipAuthenticationInfo(ChipAuthenticationInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, ChipAuthenticationInfo.VERSION_NUM, 2));
			securityInfos.add(new TerminalAuthenticationInfo());
			DG14File dg14 = new DG14File(securityInfos);
			return dg14;
		} catch(Exception e) {
			fail(e.getMessage());
			return null;
		}
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
