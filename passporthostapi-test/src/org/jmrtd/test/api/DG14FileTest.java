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

package org.jmrtd.test.api;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import junit.framework.TestCase;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.lds.DG14File;

public class DG14FileTest extends TestCase {

	Provider BC_PROV = new BouncyCastleProvider();

	public void testReflexive1() throws NoSuchAlgorithmException {
		try {
			Map<Integer, PublicKey> keys = new TreeMap<Integer, PublicKey>();

			Security.addProvider(BC_PROV);

			/* Using BC here, since SunJCE doesn't support EC. */
			KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance("EC", "BC");
			keyGen1.initialize(192);
			KeyPair keyPair1 = keyGen1.generateKeyPair();

			/* Using SunJCE here, since BC sometimes hangs?!?! Bug in BC?
			 *
			 * FIXME: This happened to MO on WinXP, Eclipse 3.4, Sun JDK1.6.0_15,
			 * not tested on other platforms... replace "SunJCE" with "BC" and see
			 * if this test halts forever.
			 */
			KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance("DH", "SunJCE");

			
			System.out.println("DEBUG: DG14FileTest: Generating key pair 2");
			KeyPair keyPair2 = keyGen2.generateKeyPair();
			PublicKey publicKey1 = keyPair1.getPublic();
			PublicKey publicKey2 = keyPair2.getPublic();

			keys.put(1, publicKey1);
			keys.put(2, publicKey2);

			Map<Integer, DERObjectIdentifier> algs = new TreeMap<Integer, DERObjectIdentifier>();
			algs.put(1, EACObjectIdentifiers.id_CA_DH_3DES_CBC_CBC);
			algs.put(2, EACObjectIdentifiers.id_CA_ECDH_3DES_CBC_CBC);

			DG14File dg14File = new DG14File(keys, algs, null, null);

			System.out.println("DEBUG: DG14FileTest: End");

			Map<Integer, PublicKey> dg14PublicKeys = dg14File.getPublicKeys();
			

			/* Oops, assertEquals fails... sometimes (depending on order of
			 * inserted providers) because neither SunJCE nor BC implement equals
			 * methods in their DHPublicKey.
			 * 
			 * 		assertEquals(keys, dg14PublicKeys);
			 * 
			 * Below we compare the parameters ourselves using equalsDHPublicKey(..)
			 * method... not pretty.
			 * 
			 * FIXME: Other solutions?
			 */
			assertEquals(keys.keySet(), dg14PublicKeys.keySet());
			for (int i: keys.keySet()) {
				PublicKey publicKey = (PublicKey)keys.get(i);
				PublicKey dg14PublicKey = (PublicKey)dg14PublicKeys.get(i);
				if (publicKey instanceof DHPublicKey) {
					assertTrue(equalsDHPublicKey((DHPublicKey)publicKey, dg14PublicKey));
				} else {
					assertEquals(publicKey, dg14PublicKey);
				}
			}
			assertEquals(algs, dg14File.getChipAuthenticationInfos());
		} catch (Exception e) {
			fail(e.getMessage());
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
