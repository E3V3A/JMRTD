package org.jmrtd.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.PassportService;

/**
 * Testing BAC, incl. checks on access control to data groups at various stages
 * of the protocol.
 * 
 * The parent class PassportTesterBase is responsible for provide MRZ data
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

	/** All files that are selectable */
	private final byte[] files = { (byte) 0x01, // MRZ
			(byte) 0x02, // face
			(byte) 0x03, // finger
			(byte) 0x0E, // RFU
			(byte) 0x0F, // AA public key
			(byte) 0x1C, // ??
			(byte) 0x1D, // ??
			(byte) 0x1E }; // ??

	private final byte[] commands = { (byte) 0x00, // ??
			(byte) 0x2A, // INS_MSE
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
			assertFalse(service.canSelectFile(fid));
		}
	}

	/**
	 * After BAC we can read MRZ (DG1) and photo (DG2)
	 */
	public void testBACIsWorking() throws CardServiceException {
		service.doBAC();
		assertTrue(service.canSelectFile(PassportService.EF_DG1));
		assertTrue(service.canReadFile(PassportService.EF_DG1));
		assertTrue(service.canSelectFile(PassportService.EF_DG2));
		assertTrue(service.canReadFile(PassportService.EF_DG2));
	}

	/**
	 * After BAC we cannot read fingerprint (DF3) or iris (DG4)
	 * 
	 * @throws CardServiceException
	 */
	public void testAccessControlOnFingerprintAfterBAC()
			throws CardServiceException {
		service.doBAC();
		traceApdu = true;
		// We should now be able to read MRZ, photo and public key for AA
		assertTrue(service.canSelectFile(PassportService.EF_DG1));
		assertTrue(service.canSelectFile(PassportService.EF_DG2));
		assertTrue(service.canSelectFile(PassportService.EF_DG15));
		// but not fingerprint or iris
		assertFalse(service.canReadFile(PassportService.EF_DG3));
		assertFalse(service.canReadFile(PassportService.EF_DG4));
	}

	/**
	 * Checking all known datagroups (ie datagroups in the standard)
	 * 
	 * @throws CardServiceException
	 */
	public void testAccessControlOnAllFilesAfterBAC()
			throws CardServiceException {
		service.doBAC();
		traceApdu = true;
		// Datagroups that are RFU should not be selectable, probably?
		assertFalse(service.canSelectFile(PassportService.EF_DG6));
		assertTrue(service.canSelectFile(PassportService.EF_DG14)); // ??
		// not so sure about other datagroups
		assertFalse(service.canSelectFile(PassportService.EF_DG5));
		assertFalse(service.canSelectFile(PassportService.EF_DG7));
		assertFalse(service.canSelectFile(PassportService.EF_DG8));
		assertFalse(service.canSelectFile(PassportService.EF_DG9));
		assertFalse(service.canSelectFile(PassportService.EF_DG10));
		assertFalse(service.canSelectFile(PassportService.EF_DG11));
		assertFalse(service.canSelectFile(PassportService.EF_DG12));
		assertFalse(service.canSelectFile(PassportService.EF_DG13));
		assertTrue(service.canSelectFile(PassportService.EF_DG15)); // public
		// key
		// for AA
		assertFalse(service.canSelectFile(PassportService.EF_DG16)); // persons
		// to
		assertTrue(service.canSelectFile(PassportService.EF_COM));
		assertTrue(service.canSelectFile(PassportService.EF_SOD));
	}

	/**
	 * After BAC, we should refuse all non-SM communication
	 */
	public void testSMAbortedCorrectlyOnNonSMSelectFile()
			throws CardServiceException {
		service.doBAC();
		traceApdu = true;
		assertFalse(service.canSelectFileWithoutSM(PassportService.EF_DG1));
		// This should result in an SM error and aborting the SM session
		assertTrue(getLastSW() == 0x6987 || getLastSW() == 0x6988);
	}

	/**
	 * After aborting the SM session we cannot resume it
	 */
	public void testAfterAbortSM() throws CardServiceException {
		service.doBAC();
		traceApdu = true;
		assertFalse(service.canSelectFileWithoutSM(PassportService.EF_DG1));
		// This should result in an SM error and aborting the SM session
		assertTrue(getLastSW() == 0x6987 || getLastSW() == 0x6988);
		// and nothing should be selectable; note that the call below
		// will use SM wrapping with the old keys
		try {
			boolean b = service.canSelectFile(PassportService.EF_DG1);
			assertFalse(b);
		} catch (Exception e) {
		}
		// If we get an exception, this is probably because unwrapping
		// the response APDU causes some error, which is not strange,
		// as the response should not be SM encrypted
	}

	/**
	 * Prints supported instructions. Takes P1=P2=0 annd doesn't supply any data
	 * or Le. Skips instruction byte 112 (0x70, MANAGE CHANNEL) because this
	 * generates an exception.
	 * 
	 * Any CardServiceException is simply passed on.
	 */
	private void printSupportedInstructions() throws CardServiceException {
		System.out.println("Supported instruction: ");
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
				System.out.print(" C: "
						+ Hex.bytesToHexString(capdu.getBytes()));
				System.out.println(" R: "
						+ Hex.bytesToHexString(rapdu.getBytes()));
			} else {
				// System.out.println("Instruction " + ins + " not supported");
			}
		}
	}
	

	/**
	 * Print which instructions the applet supports, pre- and post-BAC. This
	 * doesn't really test anything (ie should never fail) but reports useful
	 * info.
	 */
	public void testSupportedInstructions() throws CardServiceException {
		traceApdu = false;
		System.out.println("** Checking supported instructions before BAC **");
		printSupportedInstructions();
		service.doBAC();
		System.out.println("** Checking supported instructions after BAC **");
		printSupportedInstructions();
	}

	/**
	 * Prints selectable and readable files;
	 */
	private void printSelectableFiles() {
		List<Short> c = new ArrayList<Short>();
		System.out.print("Selectable files: ");
		for (short fid = 0x0100; fid <= 0x01FF; fid++) {
			// note that fid starts with 0x01
			if (service.canSelectFile(fid)) {
				System.out.printf(" %X ", fid);
				c.add(new Short(fid));
			}
		}
		System.out.println();
		if (!c.isEmpty()) {
			Iterator<Short> iter = c.iterator();
			System.out.print("Readable files: ");
			for (Short fid_object : c) {
				short fid = fid_object.shortValue();
				System.out.printf(" %X:", fid);
				if (service.canReadFile(fid)) {
					System.out.printf("ok;  ");
				} else {
					System.out.printf("not ok;  ");
				}
			}
			System.out.println();
		}
	}

	/**
	 * Print which files are selectable, pre- and post-BAC. This test doesn't
	 * test anything (ie should never fail), but reports useful info.
	 */
	public void tes_tSelectableFiles() throws CardServiceException {
		traceApdu = false;
		System.out.println("** Checking selectable files before BAC **");
		printSelectableFiles();
		service.doBAC();
		System.out.println("** Checking selectable files after BAC **");
		printSelectableFiles();
	}

	/** Dump file to console */
	public void print(short fid) throws CardServiceException {
		System.out.printf("Checking out file 0x0%X :", fid);
		CardFileInputStream in = service.readFile(fid);
		byte[] contents = in.toByteArray();
		System.out.println(Hex.bytesToHexString(contents));
	}

	/**
	 * Check the content of these unexpected files
	 */
	public void testWeirdFiles() throws CardServiceException {
		service.doBAC();
		print((short) 0x010E);
		print((short) 0x011C);
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

	/**
	 * TODO: test if we can do anything with instruction 0xB1 that we're not
	 * supposed to, eg read DG3.
	 * 
	 * @throws CardServiceException
	 */
	public void test_READ_BINARY2() throws CardServiceException {
		service.doBAC();
		traceApdu = true;
		service.canSelectFile((short) PassportService.EF_DG3);
		// Apparently we can select DG3, or we'd have an exception

		// Let's try CLA B1 P1 P2 Lc Data Le
		// P1 and P2 should be 00, data is the Offset TLV encoded
		// As data we take FF FF, which TLV-encoded is 54 02 FF FF
		byte[] offset = { (byte) 0x54, (byte) 0x02, (byte) 0xFF, (byte) 0xFF };
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816,
				ISO7816.INS_READ_BINARY2, (byte) 0x00, (byte) 0x00, offset,
				(byte) 0x01);
		capdu = service.getWrapper().wrap(capdu);
		ResponseAPDU rapdu = service.transmit(capdu);
		assertTrue(rapdu.getSW() == 0x6987);
		// 0x69xx = Command not allowed
		// 87 = Expected SM data objects missing
		try {
			rapdu = service.getWrapper().unwrap(rapdu, rapdu.getBytes().length);
		} catch (Exception e) {
			// something went wrong unwrapping
		}
	}
}
