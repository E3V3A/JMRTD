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
	private MRZInfo loesPassportMRZInfo;
	
	public MRZInfoTest(String name) {
		super(name);
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
		loesPassportMRZInfo = new MRZInfo(documentType, issuingState, primaryIdentifier, secondaryIdentifiers, documentNumber, nationality, dateOfBirth, gender, dateOfExpiry, personalNumber);
	}
	
	public void testToString() {
		String expectedResult = "P<NLDMEULENDIJK<<LOES<ALBERTINE<<<<<<<<<<<<<\nXX00000000NLD7110195F1108280123456782<<<<<02\n";
		assertEquals(loesPassportMRZInfo.toString(), expectedResult);
	}
	
	public void testReflexive() {
		byte[] encoded = loesPassportMRZInfo.getEncoded();
		ByteArrayInputStream in = new ByteArrayInputStream(encoded);
		MRZInfo copyMRZInfo = new MRZInfo(in);
		assertEquals(loesPassportMRZInfo, copyMRZInfo);
		assertEquals(Hex.bytesToHexString(encoded), Hex.bytesToHexString(copyMRZInfo.getEncoded()));
	}
}
