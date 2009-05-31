package org.jmrtd.test;

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
				service = new PassportTestService(new TerminalCardService(terminal));
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
//		if (service.isOpen()) {
//			service.close();
//		}
//		service.open();
	}

	/**
	 * Reset the card before every test; note that any resulting Exception is
	 * reported as test error
	 * 
	 * @throws CardServiceException
	 */
	public void setUp() throws CardServiceException {
		resetCard();
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
	 * Return true if datagroup can be selected; SM is not used even if it is
	 * active. Note that this may (should!) reset the passport application, if
	 * it is expecting SM.
	 */
     protected boolean canSelectFileWithoutSM(short fid) {
    	return service.canSelectFileWithoutSM(fid);
     }

	/**
	 * Return true if datagroup can be selected; SM is used if it is active.
	 * Uses P2 = 0x02, P3 = 0x0c, and Le = 256 (see PassportAPDUService)
	 */
	protected boolean canSelectFile(short fid) {
		return service.canSelectFile(fid);
	}

	/**
	 * Return true if datagroup can be read; SM is used if it is active.
	 */
	protected boolean canReadFile(short fid) {
		return service.canReadFile(fid);
	}

	/**
	 * Returns last status word received
	 */
	protected int getLastSW() {
		return last_rapdu.getSW();
	}

}
