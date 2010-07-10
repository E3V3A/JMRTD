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

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import junit.framework.TestCase;

import org.jmrtd.lds.DG3File;

public class DG3FileTest extends TestCase
{
	public DG3FileTest(String name) {
		super(name);
	}

	public void testFile() {
		try {
//			String testFile = "/home/martijno/paspoort/woj-dg3-top-secret-0103.bin";
			String testFile = "t:/paspoort/woj-dg3-top-secret-0103.bin";
			FileInputStream in = new FileInputStream(testFile);
			new DG3File(in);
		} catch (FileNotFoundException fnfe) {
			fail(fnfe.getMessage());
		}
	}
}
