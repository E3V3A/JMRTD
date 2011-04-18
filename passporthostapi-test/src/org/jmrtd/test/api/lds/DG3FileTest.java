/*
 *  JMRTD Tests.
 *
 *  Copyright (C) 2010  The JMRTD team
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;

import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.DG3File;
import org.jmrtd.lds.FingerInfo;

public class DG3FileTest extends TestCase
{
	private static final //			String testFile = "/home/martijno/paspoort/woj-dg3-top-secret-0103.bin";
	String TEST_FILE = "t:/paspoort/test/woj-dg3-top-secret-0103.bin";

	public DG3FileTest(String name) {
		super(name);
	}

	public void testFile() {
		try {
			FileInputStream in = new FileInputStream(TEST_FILE);
			DG3File dg3 = new DG3File(in);
			for (FingerInfo fingerPrint: dg3.getFingerInfos()) {
				BufferedImage image = fingerPrint.getImage();
				System.out.println("DEBUG: fingerprint " + image.getWidth() + " x " + image.getHeight());
			}
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			fail(fnfe.getMessage());
		}
	}

	public void testReflexive() {
		try {
			DG3File dg3 = new DG3File(new FileInputStream(TEST_FILE));
			byte[] encoded = dg3.getEncoded();
			assertNotNull(encoded);
			
			System.out.println("DEBUG: encoded =\n" + Hex.bytesToPrettyString(encoded));
			
			DG3File copy = new DG3File(new ByteArrayInputStream(encoded));
			byte[] encodedCopy = copy.getEncoded();
			
			System.out.println("DEBUG: encoded =\n" + Hex.bytesToPrettyString(encodedCopy));

			assertNotNull(encodedCopy);
			assertEquals(dg3, copy);
			assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(encodedCopy));
		} catch(IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
