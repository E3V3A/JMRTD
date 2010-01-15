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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.DG15File;

public class DG15FileTest extends TestCase
{
	public DG15FileTest(String name) {
		super(name);
	}

	public void testReflexive() {
		testReflexive(createTestObject());
	}

	public void testReflexive(DG15File dg15File) {
		byte[] encoded = dg15File.getEncoded();
		ByteArrayInputStream in = new ByteArrayInputStream(encoded);
		DG15File copy = new DG15File(in);
		assertEquals(dg15File, copy);
		assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
	}

	public void testGetPublic() {
		try {
			KeyPair keyPair = createTestKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			DG15File dg15File = new DG15File(publicKey);
			PublicKey otherPublicKey = dg15File.getPublicKey();
			assertEquals(publicKey, otherPublicKey);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	public static DG15File createTestObject() {
		try {
			KeyPair keyPair = createTestKeyPair();
			return new DG15File(keyPair.getPublic());
		} catch (NoSuchAlgorithmException nsae) {
			return null;
		}
	}

	private static KeyPair createTestKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		return keyPairGenerator.generateKeyPair();
	}

	public void testFile(InputStream in) {
		testReflexive(new DG15File(in));
	}
}
