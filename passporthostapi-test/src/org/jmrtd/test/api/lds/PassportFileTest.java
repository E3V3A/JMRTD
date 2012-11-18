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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestCase;
import net.sourceforge.scuba.tlv.TLVInputStream;

import org.jmrtd.lds.LDSFile;

/**
 * You can throw files containing MRTD content at this test case and
 * it will call the relevant tests.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class PassportFileTest extends TestCase {

	/**
	 * Files containing individual LDS files (such as COM, DG1, ..., SOd) or
	 * zipped collections of these.
	 */
	private static final File[] TEST_FILES = new File("/t:/paspoort/test").listFiles(new FileFilter() {

		@Override
		public boolean accept(File pathname) {
			return pathname.getName().endsWith(".zip") || pathname.getName().endsWith(".ZIP");
		}
		
	});

	private COMFileTest comFileTest;
	private DG1FileTest dg1FileTest;
	private DG2FileTest dg2FileTest;
	private DG3FileTest dg3FileTest;
	private DG7FileTest dg7FileTest;
	private DG11FileTest dg11FileTest;
	private DG15FileTest dg15FileTest;
	private SODFileTest sodFileTest;

	public PassportFileTest(String name) {
		super(name);
		comFileTest = new COMFileTest(name);
		dg1FileTest = new DG1FileTest(name);
		dg2FileTest = new DG2FileTest(name);
		dg3FileTest = new DG3FileTest(name);
		dg7FileTest = new DG7FileTest(name);
		dg11FileTest = new DG11FileTest(name);
		dg15FileTest = new DG15FileTest(name);
		sodFileTest = new SODFileTest(name);
	}

	public void testFiles() {
		for (File file: TEST_FILES) {
			try {
				testFile(file);
			} catch (Exception e) {
				fail("For file " + file + ": " + e.toString());
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
		BufferedInputStream bufferedIn = new BufferedInputStream(in, 8);
		TLVInputStream tlvIn = new TLVInputStream(bufferedIn);
		bufferedIn.mark(5);
		int tag = tlvIn.readTag();
		bufferedIn.reset(); /* NOTE: Unread the tag... */
		switch (tag) {
		case LDSFile.EF_COM_TAG: log(name + " -> COM"); comFileTest.testFile(bufferedIn); break;
		case LDSFile.EF_DG1_TAG: log(name + " -> DG1"); dg1FileTest.testFile(bufferedIn); break;
		case LDSFile.EF_DG2_TAG: log(name + " -> DG2"); dg2FileTest.testFile(bufferedIn); break;
		case LDSFile.EF_DG3_TAG: log(name + " -> DG3"); dg3FileTest.testFile(bufferedIn); break;
		case LDSFile.EF_DG4_TAG: break;
		case LDSFile.EF_DG5_TAG: break;
		case LDSFile.EF_DG6_TAG: break;
		case LDSFile.EF_DG7_TAG: log(name + " -> DG7"); dg7FileTest.testFile(bufferedIn); break;
		case LDSFile.EF_DG8_TAG: break;
		case LDSFile.EF_DG9_TAG: break;
		case LDSFile.EF_DG10_TAG: break;
		case LDSFile.EF_DG11_TAG: log(name + " -> DG11"); dg11FileTest.testFile(bufferedIn); break;
		case LDSFile.EF_DG12_TAG: break;
		case LDSFile.EF_DG13_TAG: break;
		case LDSFile.EF_DG14_TAG: break;
		case LDSFile.EF_DG15_TAG: log(name + " -> DG15"); dg15FileTest.testFile(bufferedIn); break;
		case LDSFile.EF_DG16_TAG: break;
		case LDSFile.EF_SOD_TAG: log(name + " -> SOD"); sodFileTest.testFile(bufferedIn); break;
		}
	}

	private void log(String txt) {
		System.out.println("DEBUG: Testing: " + txt);
	}
}
