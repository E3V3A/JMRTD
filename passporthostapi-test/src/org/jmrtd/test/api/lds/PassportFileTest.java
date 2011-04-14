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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestCase;
import net.sourceforge.scuba.tlv.TLVInputStream;

import org.jmrtd.lds.PassportFile;

/**
 * You can throw files containing MRTD content at this test case and
 * it will call the relevant tests.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class PassportFileTest extends TestCase {

	/**
	 * Files containing individual MRTD files (such as COM, DG1, ..., SOd) or
	 * zipped collections of these.
	 */
	private static final String[] TEST_FILES = {
		// "/t:/paspoort/test/"
	};

	private COMFileTest comFileTest;
	private DG1FileTest dg1FileTest;
	private DG2FileTest dg2FileTest;
	private DG11FileTest dg11FileTest;
	private DG15FileTest dg15FileTest;
	private SODFileTest sodFileTest;

	public PassportFileTest(String name) {
		super(name);
		comFileTest = new COMFileTest(name);
		dg1FileTest = new DG1FileTest(name);
		dg2FileTest = new DG2FileTest(name);
		dg11FileTest = new DG11FileTest(name);
		dg15FileTest = new DG15FileTest(name);
		sodFileTest = new SODFileTest(name);
	}

	public void testFiles() {
		for (String fileName: TEST_FILES) {
			try {
				File file = new File(fileName);
				testFile(file);
			} catch (Exception e) {
				fail(e.toString());
			}
		}
	}

	public void testFile(File file) {
		String name = file.getName();
		ZipFile zipFile = null;

		try {
			zipFile = new ZipFile(file);
		} catch (Exception e) {
			/* So, it's not a zip file. */
			zipFile = null;
		}

		if (zipFile == null) {
			try {
				testInputStream(name, new FileInputStream(file));
				return;
			} catch (Exception e) {
				fail(e.toString());
			}
		} else {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry == null) { break; } // FIXME: fail in this case?
				try {
					testInputStream(name, zipFile.getInputStream(entry));
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.toString());
				}
			}
		}
	}

	private void testInputStream(String name, InputStream in) throws IOException {
		TLVInputStream tlvIn = new TLVInputStream(new BufferedInputStream(in, 256));
		tlvIn.mark(128);
		int tag = tlvIn.readTag();
		tlvIn.reset(); /* NOTE: Unread the tag... */
		switch (tag) {
		case PassportFile.EF_COM_TAG: log(name + " -> COM"); comFileTest.testFile(tlvIn); break;
		case PassportFile.EF_DG1_TAG: log(name + " -> DG1"); dg1FileTest.testFile(tlvIn); break;
		case PassportFile.EF_DG2_TAG: log(name + " -> DG2"); dg2FileTest.testFile(tlvIn); break;
		case PassportFile.EF_DG3_TAG: break;
		case PassportFile.EF_DG4_TAG: break;
		case PassportFile.EF_DG5_TAG: break;
		case PassportFile.EF_DG6_TAG: break;
		case PassportFile.EF_DG7_TAG: break;
		case PassportFile.EF_DG8_TAG: break;
		case PassportFile.EF_DG9_TAG: break;
		case PassportFile.EF_DG10_TAG: break;
		case PassportFile.EF_DG11_TAG: log(name + " -> DG11"); dg11FileTest.testFile(tlvIn); break;
		case PassportFile.EF_DG12_TAG: break;
		case PassportFile.EF_DG13_TAG: break;
		case PassportFile.EF_DG14_TAG: break;
		case PassportFile.EF_DG15_TAG: log(name + " -> DG15"); dg15FileTest.testFile(tlvIn); break;
		case PassportFile.EF_DG16_TAG: break;
		case PassportFile.EF_SOD_TAG: log(name + " -> SOD"); sodFileTest.testFile(tlvIn); break;
		}
	}

	private void log(String txt) {
		System.out.println("DEBUG: Testing: " + txt);
	}
}
