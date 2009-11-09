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
import java.security.PublicKey;
import java.security.Security;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.DG14File;

public class DG14FileTest extends TestCase {

	public void testReflexive1() throws NoSuchAlgorithmException {
		Security.addProvider(new BouncyCastleProvider());
		KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance("EC");
		keyGen1.initialize(192);
		KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance("DH");
		System.out.println("DEBUG: DG14FileTest: Generating key pair 1");

		KeyPair keyPair1 = null;
		KeyPair keyPair2 = null;
		keyPair1 = keyGen1.generateKeyPair();
		System.out.println("DEBUG: DG14FileTest: Generating key pair 2");
		keyPair2 = keyGen2.generateKeyPair();

		PublicKey publicKey1 = keyPair1.getPublic();
		PublicKey publicKey2 = keyPair2.getPublic();

		Map<Integer, PublicKey> keys = new TreeMap<Integer, PublicKey>();
		keys.put(1, publicKey1);
		keys.put(2, publicKey2);

		System.out.println("DEBUG: publicKey1.getEncoded().length == " + publicKey1.getEncoded().length);
		System.out.println("DEBUG: publicKey2.getEncoded().length == " + publicKey2.getEncoded().length);

		Map<Integer, DERObjectIdentifier> algs = new TreeMap<Integer, DERObjectIdentifier>();
		algs.put(1, EACObjectIdentifiers.id_CA_DH_3DES_CBC_CBC);
		algs.put(2, EACObjectIdentifiers.id_CA_ECDH_3DES_CBC_CBC);

		DG14File f = new DG14File(keys, algs, null, null);

		System.out.println("DEBUG: DG14FileTest: End");

		assertEquals(keys, f.getPublicKeys());
		assertEquals(algs, f.getChipAuthenticationInfos());

	}
}
