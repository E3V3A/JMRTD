package org.jmrtd.test;

import java.io.File;
import java.security.Security;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import junit.framework.TestCase;
import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.*;

public abstract class PassportTesterBase extends TestCase implements
		APDUListener {

	static {
		Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	protected PassportTestService service = null;

	/** The last response APDU received (SM wrapped when SM is active?) */
	protected ResponseAPDU last_rapdu = null;

	public PassportTesterBase(String name) {
		super(name);
		try {
			TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null);
			CardTerminals terminals = tf.terminals();
			for (CardTerminal terminal : terminals
					.list(CardTerminals.State.CARD_PRESENT)) {
				service = new PassportTestService(new TerminalCardService(
						terminal));
				service.addAPDUListener(this);
				if (service != null) {
					service.open();
					break;
				}
			}
			if (service == null) {
				fail("No card found.");
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

	protected Date getDate(String s) {
		try {
			return SDF.parse(s);
		} catch (ParseException pe) {
			return null;
		}
	}

	protected void resetCard() throws CardServiceException {
		// This actually properly resets the card.
		service.resetCard();
	}

	/**
	 * Reset the card before every test; note that any resulting Exception is
	 * reported as test error
	 * 
	 * @throws CardServiceException
	 *             if card cannot be reset
	 * @throws ParseException
	 *             if someone was stupid enough to provide ill-formed MRZ data
	 *             in the implementation of this method
	 */
	public void setUp() throws CardServiceException, ParseException {
		resetCard();
		service.setMRZ("XX1234587", "760803", "140507");
		System.out.println("Setup EAC: " + service.setupEAC());
	}

	protected boolean traceApdu = false;

	public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
		last_rapdu = rapdu;
		if (traceApdu) {
			System.out.println("C: " + Hex.bytesToHexString(capdu.getBytes()));
			System.out.println("R: " + Hex.bytesToHexString(rapdu.getBytes()));
		}
	}

	/**
	 * Returns last status word received
	 */
	public int getLastSW() {
		return last_rapdu.getSW();
	}

}
