package org.jmrtd.test;

import java.util.Date;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.PassportService;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.ISO7816;

/**
 * Testing BAC, incl. checks on access control to data groups at various stages
 * of the protocol.
 * 
 * Still untested; a lot is just bits and pieces to explore ways of using the
 * existing APIs, esp. of PassportService and PassportApduService.
 * 
 * @author Erik Poll (erikpoll@cs.ru.nl)
 */
public class PassportBACTester extends PassportTesterBase {

	/*
	 * Some general observations:
	 * 
	 * The abstraction provided by PassportService and PassportApduService, such
	 * as - automatically adding/removing SM to APDUs - translating some (all?)
	 * status words other than 9000 into a CardServiceException can be annoying
	 * when testing. Sometimes you want more low level control over APDUs sent.
	 * It would be nice if CardServiceExceptions stored the SW that caused them.
	 * 
	 * The PassportService won't reset the wrapper to null after an SM failure.
	 * So after SM has been aborted, it will still try to SM-wrap.
	 * 
	 * We don't try to handle any CardServiceExceptions, and leave that up to
	 * junit, UNLESS we expect a CardServiceException.
	 * 
	 * Maybe some of the functionality in this class and in PassportTesterBase
	 * (eg testSupportedInstructions() or getLastSW()) should be refactored to
	 * some PassportTestService extends PassportService, so that it could also
	 * be used in tests that don't use junit.
	 */

	private String documentNumber = "PPNUMMER0";
	private Date dateOfBirth = getDate("19560507");
	private Date dateOfExpiry = getDate("20100101");

	public PassportBACTester(String name) {
		super(name);
	}

	public void testDataGroup1() throws CardServiceException {
		traceApdu = true;
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		// We should now be able to read MRZ, photo and public key for AA
		assert (canSelectFile(PassportService.EF_DG1));
		assert (canSelectFile(PassportService.EF_DG2));
		assert (canSelectFile(PassportService.EF_DG15));
		// but not fingerprint or iris
		assert (!canSelectFile(PassportService.EF_DG3));
		assert (!canSelectFile(PassportService.EF_DG4));
		// Datagroups that are RFU should not be selectable
		assert (!canSelectFile(PassportService.EF_DG6));
		assert (!canSelectFile(PassportService.EF_DG14));
		// not so sure about other datagroups
		assert (canSelectFile(PassportService.EF_DG5)); // ?
		assert (canSelectFile(PassportService.EF_DG7)); // ?
		assert (canSelectFile(PassportService.EF_DG8)); // ?
		assert (canSelectFile(PassportService.EF_DG9)); // ?
		assert (canSelectFile(PassportService.EF_DG10)); // ?
		assert (canSelectFile(PassportService.EF_DG11)); // ?
		assert (canSelectFile(PassportService.EF_DG12)); // ?
		assert (canSelectFile(PassportService.EF_DG13)); // ?
		assert (canSelectFile(PassportService.EF_DG15)); // ?
		assert (canSelectFile(PassportService.EF_DG16)); // ?
		// Anything without SM should not work, eg
		assert (!canSelectFileWithoutSM(PassportService.EF_DG1));
		// This should result in an SM error and aborting the SM session
		assert getLastSW() == 0x6987 || getLastSW() == 0x6988;
		// and nothing should no be selectable; note that in the calls below
		// will use SM wrapping with the old keys
		assert (!canSelectFile(PassportService.EF_DG1));
		assert (!canSelectFile(PassportService.EF_DG2));

		assertNotNull(service);
	}

	/**
	 * Prints supported instructions.
	 * 
	 * Any CardServiceException is simply passed on.
	 */
	private void printSupportedInstructions() throws CardServiceException {
		for (int ins = 0; ins < 256; ins++) {
			CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816,
					(byte) ins, (byte) 0x00, (byte) 0x00);
			ResponseAPDU rapdu = service.transmit(capdu);
			if (rapdu.getSW() != 0x6D00) // "instruction not supported"
			{
				System.out.print("Instruction " + ins + " supported");
				System.out.println("C: "
						+ Hex.bytesToHexString(capdu.getBytes()));
				System.out.println("R: "
						+ Hex.bytesToHexString(rapdu.getBytes()));
			}
		}
	}

	/**
	 * Prints selectable files.
	 * 
	 */
	private void printSelectableFiles() {
		for (short fid = 0; fid < 256; fid++) {
			if (canSelectFile(fid)) {
				System.out.print("File " + fid + " selectable");
			}
		}
	}

	/**
	 * Print which instructions the applet supports, pre- and post-BAC.
	 * 
	 */
	public void testSupportedInstructions() throws CardServiceException {
		traceApdu = false;
		System.out.println("*** Checking supported instructions ***");
		resetCard();
		printSupportedInstructions();
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		printSupportedInstructions();
	}

	/**
	 * If access control is done when processing SELECT FILE, rather than when
	 * processing READ BINARY, there is a chance that we can read a file that
	 * happens to still be selected. Granted, it's a bit of a long-shot...
	 * 
	 * Would it be interesting to test for the possibility to do a readBinary
	 * after a failed EAC, in the hope of having a pointer to a file holding
	 * some certificate?
	 */
	public void testFileSelectedAfterRestart() throws CardServiceException {
		traceApdu = true;
		resetCard();
		try {
			byte[] b = service.sendReadBinary(null, (short) 0, 10);
			int sw = getLastSW();
			if (sw == ISO7816.SW_NO_ERROR || sw != ISO7816.SW_END_OF_FILE
					|| sw != ISO7816.SW_LESS_DATA_RESPONDED_THAN_REQUESTED) {
				fail("Weird: we can read a file before selecting one");
			}
		} catch (CardServiceException e) {

		}
	}
}
