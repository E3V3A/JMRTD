package org.jmrtd.test;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

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
 * Note that junit reports exceptions as test errors.
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

	private String documentNumber = "XX1234587";
	private Date dateOfBirth = getDate("19760803");
	private Date dateOfExpiry = getDate("20140507");

	public PassportBACTester(String name) {
		super(name);
	}

	

	/**
	 * After BAC we can read MRZ (DG1) and photo (DG2)
	 */
	public void testBACIsWorking() throws CardServiceException {
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		assertTrue(canSelectFile(PassportService.EF_DG1));
		assertTrue(canReadFile(PassportService.EF_DG1));
		assertTrue(canSelectFile(PassportService.EF_DG2));
		assertTrue(canReadFile(PassportService.EF_DG2));
	}

	/**
	 * After BAC we cannot read fingerprint (DF3) or iris (DG4)
	 * 
	 * @throws CardServiceException
	 */
	public void testAC_fingerprint() throws CardServiceException {
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		traceApdu = true;
		// We should now be able to read MRZ, photo and public key for AA
		assertTrue(canSelectFile(PassportService.EF_DG1));
		assertTrue(canSelectFile(PassportService.EF_DG2));
		assertTrue(canSelectFile(PassportService.EF_DG15));
		// but not fingerprint or iris
		assertTrue(canSelectFile(PassportService.EF_DG3));
		assertFalse(canReadFile(PassportService.EF_DG3));
		assertFalse(canSelectFile(PassportService.EF_DG4));
		assertFalse(canReadFile(PassportService.EF_DG4));
	}

	/**
	 * BAC is working and
	 * 
	 * @throws CardServiceException
	 */
	public void testAccessControlAfterBAC() throws CardServiceException {
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		traceApdu = true;
		// We should now be able to read MRZ, photo and public key for AA
		assertTrue(canSelectFile(PassportService.EF_DG1));
		assertTrue(canSelectFile(PassportService.EF_DG2));
		assertTrue(canSelectFile(PassportService.EF_DG15));
		// but not fingerprint or iris
		assertTrue(canSelectFile(PassportService.EF_DG3));
		assertFalse(canReadFile(PassportService.EF_DG3));
		assertFalse(canSelectFile(PassportService.EF_DG4));
		assertFalse(canReadFile(PassportService.EF_DG4));
		// Datagroups that are RFU should not be selectable
		assertFalse(canSelectFile(PassportService.EF_DG6));
		assertTrue(canSelectFile(PassportService.EF_DG14));
		// not so sure about other datagroups
		assertFalse(canSelectFile(PassportService.EF_DG5)); // ?
		assertFalse(canSelectFile(PassportService.EF_DG7)); // ?
		// assertTrue (canSelectFile(PassportService.EF_DG8)); // ?
		// assertTrue (canSelectFile(PassportService.EF_DG9)); // ?
		// assertTrue (canSelectFile(PassportService.EF_DG10)); // ?
		// assertTrue (canSelectFile(PassportService.EF_DG11)); // ?
		// assertTrue (canSelectFile(PassportService.EF_DG12)); // ?
		// assertTrue (canSelectFile(PassportService.EF_DG13)); // ?
		// assertTrue (canSelectFile(PassportService.EF_DG15)); // ?
		// assertTrue (canSelectFile(PassportService.EF_DG16)); // ?
		// Anything without SM should not work, eg
		// assert (!canSelectFileWithoutSM(PassportService.EF_DG1));
		// This should result in an SM error and aborting the SM session
		// assertTrue(getLastSW() == 0x6987 || getLastSW() == 0x6988);
		// and nothing should no be selectable; note that in the calls below
		// will use SM wrapping with the old keys
		// assert (!canSelectFile(PassportService.EF_DG1));
		// assert (!canSelectFile(PassportService.EF_DG2));

		assertNotNull(service);
	}

	/**
	 * Prints supported instructions, skipping instruction byte 112 because this
	 * generates an exception.
	 * 
	 * Any CardServiceException is simply passed on.
	 */
	private void printSupportedInstructions() throws CardServiceException {
		System.out.print("Supported instruction: ");
		for (int ins = 0; ins < 256; ins++) {
			if (ins == 112) {
				continue; // results in strange error
			}
			;
			CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816,
					(byte) ins, (byte) 0x00, (byte) 0x00);
			ResponseAPDU rapdu = service.transmit(capdu);
			if (rapdu.getSW() != 0x6D00) // " instruction not supported"
			{
				System.out.printf(" %X ", ins);
				System.out
						.print("C: " + Hex.bytesToHexString(capdu.getBytes()));
				System.out.println("R: "
						+ Hex.bytesToHexString(rapdu.getBytes()));
			} else {
				// System.out.println("Instruction " + ins + " not supported");
			}
		}
	}

	/**
	 * Print which instructions the applet supports, pre- and post-BAC. This
	 * doesn't really test anything (ie should never fail) but reports useful
	 * info to develop other tests.
	 */
	public void testSupportedInstructions() throws CardServiceException {
		traceApdu = false;
		System.out.println("** Checking supported instructions before BAC **");
		printSupportedInstructions();
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		System.out.println("** Checking supported instructions after BAC **");
		printSupportedInstructions();
	}

	/**
	 * Prints selectable and readable files;
	 */
	private void printSelectableFiles() {
		HashSet<Short> c = new HashSet<Short>();
		System.out.print("Selectable files: ");
		for (short fid = 0x0100; fid <= 0x1FF; fid++) {
			if (canSelectFile(fid)) {
				System.out.printf(" %X ", fid);
				c.add (new Short(fid));
			}
		}
		System.out.println();
		if (!c.isEmpty()){
			Iterator<Short> iter = c.iterator();
			System.out.print("Readable files: ");
			while (iter.hasNext()){
				short fid = (iter.next()).shortValue();
				if (canReadFile(fid)) {
					System.out.printf(" %X ", fid);
				}
			}
			System.out.println();
		}
	}
	

	/**
	 * Print which files are selectable, pre- and post-BAC. This test doesn't test
	 * anything (ie should never fail), but reports useful info to develop other
	 * tests. In particular: which of the non-standard files are present.
	 */
	public void testSelectableFiles() throws CardServiceException {
		traceApdu = false;
		System.out.println("** Checking selectable files before BAC **");
		printSelectableFiles();
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		System.out.println("** Checking selectable files after BAC **");
		printSelectableFiles();
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
	public void todo_FileSelectedAfterRestart() throws CardServiceException {
		traceApdu = true;
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
