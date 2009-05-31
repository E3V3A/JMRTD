package org.jmrtd.test;

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
	 * junit, unless we EXCEPT a CardServiceException.
	 * 
	 * Maybe some of the functionality in this class and in PassportTesterBase
	 * (eg testSupportedInstructions() or getLastSW()) should be refactored to
	 * PassportTestService, so that it could also be used by tests that don't
	 * use junit.
	 */

	private String documentNumber = "XX1234587";
	private Date dateOfBirth = getDate("19760803");
	private Date dateOfExpiry = getDate("20140507");

	/** All files that are selectable */
	private final byte[] files = { (byte) 0x01, // MRZ
			(byte) 0x02, // face
			(byte) 0x03, // finger
			(byte) 0x0E, // RFU
			(byte) 0x0F, // AA public key
			(byte) 0x1C, // ??
			(byte) 0x1D, // ??
			(byte) 0x1E }; // ??

	private final byte[] commands = { (byte) 0x2A, // INS_MSE
			(byte) 0x2A, // INS_PSO
			(byte) 0x82, // INS_EXTERNAL_AUTHENTICATE
			(byte) 0x84, // INS_GET_CHALLENGE
			(byte) 0x88, // INS_INTERNAL_AUTHENTICATE
			(byte) 0xA4, // INS_SELECT
			(byte) 0xB0, // INS_READ_BINARY
			(byte) 0xB1 }; // INS_READ_BINARY2

	public PassportBACTester(String name) {
		super(name);
	}

	/**
	 * Before BAC we cannot select anything
	 */
	public void testBeforeBAC() throws CardServiceException {
		for (short fid = 0x0100; fid <= 0x1FF; fid++) {
			assertFalse(canSelectFile(fid));
		}
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
		assertFalse(canReadFile(PassportService.EF_DG3));
		assertFalse(canReadFile(PassportService.EF_DG4));
	}

	/**
	 * Checking all datagroups
	 * 
	 * @throws CardServiceException
	 */
	public void testAccessControlAfterBAC() throws CardServiceException {
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		traceApdu = true;
		// Datagroups that are RFU should not be selectable, probably?
		assertFalse(canSelectFile(PassportService.EF_DG6));
		assertTrue(canSelectFile(PassportService.EF_DG14)); // ??
		// not so sure about other datagroups
		assertFalse(canSelectFile(PassportService.EF_DG5));
		assertFalse(canSelectFile(PassportService.EF_DG7));
		assertFalse(canSelectFile(PassportService.EF_DG8));
		assertFalse(canSelectFile(PassportService.EF_DG9));
		assertFalse(canSelectFile(PassportService.EF_DG10));
		assertFalse(canSelectFile(PassportService.EF_DG11));
		assertFalse(canSelectFile(PassportService.EF_DG12));
		assertFalse(canSelectFile(PassportService.EF_DG13));
		assertTrue(canSelectFile(PassportService.EF_DG15)); // public key
		// for AA
		assertFalse(canSelectFile(PassportService.EF_DG16));
	}

	/**
	 * After BAC, we should refuse all non-SM communication
	 */
	public void testAbortSM() throws CardServiceException {
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		traceApdu = true;
		assertFalse(canSelectFileWithoutSM(PassportService.EF_DG1));
		// This should result in an SM error and aborting the SM session
		assertTrue(getLastSW() == 0x6987 || getLastSW() == 0x6988);
	}

	/**
	 * After BAC, we should refuse all non-SM communication, and after aborting
	 * the SM sesion we can resume it
	 */
	public void testAfterAbortSM() throws CardServiceException {
		service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
		traceApdu = true;
		assertFalse(canSelectFileWithoutSM(PassportService.EF_DG1));
		// This should result in an SM error and aborting the SM session
		assertTrue(getLastSW() == 0x6987 || getLastSW() == 0x6988);
		// and nothing should be selectable; note that in the call below
		// will use SM wrapping with the old keys
		assertFalse(canSelectFile(PassportService.EF_DG1));
		// ?? somehow, this crashes things ??
	}

	/**
	 * Prints supported instructions, skipping instruction byte 112 (0x70,
	 * MANAGE CHANNEL) because this generates an exception.
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
	 * info to develop other tests. 22 2A 82 84 88 A4 B0 B1
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
		for (short fid = 0x0100; fid <= 0x01FF; fid++) {
			// arguments note that fid shart with 0x01
			if (canSelectFile(fid)) {
				System.out.printf(" %X ", fid);
				c.add(new Short(fid));
			}
		}
		System.out.println();
		if (!c.isEmpty()) {
			Iterator<Short> iter = c.iterator();
			System.out.print("Readable files: ");
			while (iter.hasNext()) {
				short fid = (iter.next()).shortValue();
				if (canReadFile(fid)) {
					System.out.printf(" %X ", fid);
				}
			}
			System.out.println();
		}
	}

	/**
	 * Print which files are selectable, pre- and post-BAC. This test doesn't
	 * test anything (ie should never fail), but reports useful info to develop
	 * other tests. In particular: which of the non-standard files are present.
	 * 
	 * 101 102 103 10E 10F 11C 11D 11E
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
