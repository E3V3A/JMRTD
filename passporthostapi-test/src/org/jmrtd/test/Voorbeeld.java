package org.jmrtd.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import org.jmrtd.PassportService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;

public class Voorbeeld {

	/** service to talk to the passport */
	private static PassportService service;

	/* Data format for dates */
	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

	/**
	 * setup connection with the card reader
	 */
	public static void setup() {
		try {
			TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null);
			CardTerminals terminals = tf.terminals();
			for (CardTerminal terminal : terminals
					.list(CardTerminals.State.CARD_PRESENT)) {
				service = new PassportService(new TerminalCardService(terminal));
				if (service != null) {
					service.open();
					break;
				}
			}
			if (service == null) {
				System.exit(-23);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * reset connection to the card reader
	 * 
	 * @throws CardServiceException
	 *             if card reader is not working
	 */
	protected static void resetCard() throws CardServiceException {
		// This actually properly resets the card.
		if (service.isOpen()) {
			service.close();
		}
		service.open();
	}

	/**
	 * Sample piece of code to try and do BAC, with hardcoded MRZ
	 * 
	 * @throws ParseException
	 *             if dates are mistyped
	 */
	public static boolean doBAC() throws ParseException {
		try {
			Date birthDate = SDF.parse("19560507");
			Date expiryDate = SDF.parse("20100101");
			String number = "PPNUMMER0";
			service.doBAC(number, birthDate, expiryDate);
			return true; // BAC succeeded
		} catch (CardServiceException e) {
			return false; // BAC failed
		}
	}

	/**
	 * Try selecting a file, with or without Secure Messaging
	 */
	public static boolean canSelectFile(short fid, boolean useSM) {
		try {
			if (useSM) {
				service.sendSelectFile(service.getWrapper(), fid);
			} else {
				service.sendSelectFile(null, fid);
			}
			return true;
		} catch (CardServiceException e) {
			return false;
		}
	}

	/**
	 * Try out BAC and accessing the passport photo
	 */
	public static void main(String[] s) throws Exception {
		setup();
		resetCard();
		// try to read photo, should return false
		boolean a = canSelectFile((short) 0x0101, false);
		// try to do BAC photo, should return true, assuming MRZ is correct
		boolean b = doBAC();
		// try to read photo, should return b
		boolean c = canSelectFile((short) 0x0101, true);
	}
}
