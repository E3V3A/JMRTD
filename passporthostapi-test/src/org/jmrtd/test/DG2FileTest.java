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

package org.jmrtd.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;
import sos.mrtd.DG2File;

public class DG2FileTest extends TestCase
{
	public DG2FileTest(String name) {
		super(name);
	}

	public void testReflexive() {
		testReflexive(createTestObject());
	}
	
	public void testReflexive(DG2File dg2File) {
		try {
			byte[] encoded = dg2File.getEncoded();
			ByteArrayInputStream in = new ByteArrayInputStream(encoded);
			DG2File copy = new DG2File(in);
			assertEquals(dg2File, copy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	public static DG2File createTestObject() {
		DG2File result = new DG2File();
		return result;
	}

	public void testFile(InputStream in) {
		testReflexive(new DG2File(in));
	}
}
