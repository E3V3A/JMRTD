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

package sos.mrtd.test;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;

import sos.data.Country;
import sos.data.Gender;
import sos.mrtd.MRZInfo;
import sos.util.Hex;
import junit.framework.TestCase;

public class MRZInfoTest extends TestCase
{
	public MRZInfoTest(String name) {
		super(name);
	}
	
	public void testToString() {
		MRZInfo mrzInfo = createTestObject();
		String expectedResult = "P<NLDMEULENDIJK<<LOES<ALBERTINE<<<<<<<<<<<<<\nXX00000000NLD7110195F1108280123456782<<<<<02\n";
		assertEquals(mrzInfo.toString(), expectedResult);
	}
	
	public void testReflexive() {
		MRZInfo mrzInfo = createTestObject();
		byte[] encoded = mrzInfo.getEncoded();
		ByteArrayInputStream in = new ByteArrayInputStream(encoded);
		MRZInfo copy = new MRZInfo(in);
		assertEquals(mrzInfo, copy);
		assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copy.getEncoded()));
	}
	
	public static MRZInfo createTestObject() {
		int documentType = MRZInfo.DOC_TYPE_ID3;
		Country issuingState = Country.NL;
		String primaryIdentifier = "MEULENDIJK";
		String[] secondaryIdentifiers = { "LOES", "ALBERTINE" };
		String documentNumber = "XX0000000";
		Country nationality = Country.NL;
		Calendar cal = Calendar.getInstance();
		cal.set(1971, 10 - 1, 19);
		Date dateOfBirth = cal.getTime();
		Gender gender = Gender.FEMALE;
		cal.set(2011, 8 - 1, 28);
		Date dateOfExpiry = cal.getTime();
		String personalNumber = "123456782";
		return new MRZInfo(documentType, issuingState, primaryIdentifier, secondaryIdentifiers, documentNumber, nationality, dateOfBirth, gender, dateOfExpiry, personalNumber);
	}
}
