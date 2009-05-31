package org.jmrtd.test;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.smartcards.TerminalCardService;

import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;
import org.jmrtd.Util;

/**
 * Card service specifically designed for testing. It aims to provide both
 * high-level functionality (like PassportService does) and lower-level
 * functionality (like PassportApduService does). It tries to do this in a
 * consistent way:
 * <ol>
 * <li>High-level commands (e.g. doAA(), doBAC(), doEAC(), doCA(), doTA(),
 * canSelectFile()) return a boolean indicating success or failure.</li>
 * <li>Low-level commands (e.g. sendGetChallenge(), sendSelectFile(),
 * sendAnyInstruction()), that correspond to a single command APDU return the
 * status word of the response APDU. For these method you can choose to send
 * them with or without Secure Messaging</li>
 * </ol>
 * Low-level details about construction the actual payloads of APDUs is hidden
 * as much as possible.
 * 
 * Usage
 * 
 * <pre>
 *       PassportTestService service = PassportTestService.createPassportTestService();
 *       service.setMRZ;
 *       ...
 *       service.canSelectFile(...);
 *       service.doBAC(); 
 *       service.canSelectFile(...);
 *       ...
 *       // start a new test 
 *       service.resetCard();
 *       ...
 * </pre>
 * 
 * Any Exceptions escaping from these method signal some low-level failures that
 * the testing framework should not try to interpret. These are not the TOE's
 * fault, but problems with the test infrastructure (incl. the card reader).
 * <p>
 * Attempting to send anything with Secure Messaging before Secure Messaging
 * keys have been established (i.e. if BAC has not been performed successfully
 * completed since the last resetCard()) will crash the test framework.
 * <p>
 * The only reason to extend PassportService is to access protected fields
 * there. It would be cleaner to decouple it.
 * <p>
 * I'm not sure what is the easiest way to feed in the data needed for BAC and
 * EAC into here.
 * <p>
 * I'm not sure that it is a workable option to provide methods here that do all
 * the individual protocol steps.
 * <p>
 * 
 * @author erikpoll
 */
public class PassportTestService extends PassportService {

	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

	/** Data needed for BAC **/

	private String documentNumber;
	private Date dateOfBirth;
	private Date dateOfExpiry;

	/** Data needed for AA **/

	private PublicKey publicKey;

	/** Data needed for EAC **/

	// private int keyId;
	// private PublicKey key;
	// private String caReference;
	// private List<CVCertificate> terminalCertificates;
	/** The last challenge received, if any **/
	private byte[] lastChallenge;

	/**
	 * Create a PassportTestService to talk to. Returns null or throws an
	 * exception if card reader cannot be made to work.
	 * 
	 * @throws CardException
	 * @throws CardServiceException
	 * @throws Exception
	 */

	public static PassportTestService createPassportTestService()
			throws CardException, CardServiceException, Exception {
		PassportTestService service = null;
		TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null);
		CardTerminals terminals = tf.terminals();
		for (CardTerminal terminal : terminals
				.list(CardTerminals.State.CARD_PRESENT)) {
			service = new PassportTestService(new TerminalCardService(terminal));
			if (service != null) {
				service.open();
				break;
			}
			if (service == null) {
				throw new Exception("No card found.");
			}
		}
		return service;
	}

	protected PassportTestService(CardService service)
			throws CardServiceException {
		super(service);
	}

	/**
	 * Set MRZ, with dates in "YYYYMMDD" format
	 * 
	 * @throws ParseException
	 *             if format of dates is incorrect
	 */
	public void setMRZ(String documentNumber, String dateOfBirth,
			String expiryDate) throws ParseException {
		this.documentNumber = documentNumber;
		this.dateOfBirth = SDF.parse(dateOfBirth);
		this.dateOfExpiry = SDF.parse(expiryDate);

	}

	/**
	 * Reset the card
	 * 
	 * @throws CardServiceException
	 *             if card reader has a problem
	 */
	public void resetCard() throws CardServiceException {
		// This actually properly resets the card.
		if (isOpen()) {
			close();
		}
		open();
	}
	
	/**
	 * Return true if datagroup can be selected; SM is used if it is active.
	 * Uses P2 = 0x02, P3 = 0x0c, and Le = 256 (see PassportAPDUService)
	 */
	protected boolean canSelectFile(short fid) {
		try {
			sendSelectFile(getWrapper(), fid);
			return true;
		} catch (CardServiceException e) {
			return false;
		}
	}
	
	/**
	 * Return true if datagroup can be selected; SM is not used even if it is
	 * active. Note that this may (should!) reset the passport application, if
	 * it is expecting SM.
	 */
	protected boolean canSelectFileWithoutSM(short fid) {
		try {
			sendSelectFile(null, fid);
			return true;
		} catch (CardServiceException e) {
			return false;
		}
	}

	/**
	 * Try selecting a file, with or without Secure Messaging.
	 * 
	 * @return whether this succeeded
	 * @throws e
	 *             NullpointerException is no Secure Messaging keys are found,
	 *             if BAC hasn't been completed earlier
	 */
	public boolean canSelectFile(short fid, boolean useSM) {
		try {
			if (useSM) {
				sendSelectFile(getWrapper(), fid);
			} else {
				sendSelectFile(null, fid);
			}
			return true;
		} catch (CardServiceException e) {
			return false;
		}
	}
	
	/**
	 * Return true if datagroup can be read; SM is used if it is active.
	 */
	protected boolean canReadFile(short fid) {
		try {
			CardFileInputStream in = readFile(fid);
			return (in != null);
		} catch (CardServiceException e) {
			return false;
		}
	}

	/**
	 * Try Basic Access Control, with the currently stored MRZ-data, returning true if this
	 * succeeded.
	 * 
	 * @return whether this succeeded
	 */
	public boolean doBAC() {
		try {
			super.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
			return true;
		} catch (CardServiceException e) {
			return false;
		}
	}

	/**
	 * Try Active Authentication, with the currently stored AA-data, returning true if this
	 * succeeded.
	 * 
	 * @return whether this succeeded
	 */
	public boolean doAA() {
		try {
			super.doAA(publicKey);
			return true;
		} catch (CardServiceException e) {
			return false;
		}
	}

	/**
	 * Try EAC, with the currently stored EAC-data, returning true if this
	 * succeeded.
	 * 
	 * @return whether this succeeded
	 */
	public boolean doEAC() {
		return false;
		// super.doEAC(keyId, key, caReference, terminalCertificates,
		// terminalKedocumentNumber);
	}
	

	/**
	 * Try Chip Authenticaion with the currently stored EAC-data, returning true if this
	 * succeeded.
	 * 
	 * @return whether this succeeded
	 */
	public boolean doCA() {
		return false;
		// super.doEAC(keyId, key, caReference, terminalCertificates,
		// terminalKedocumentNumber);
	}
	

	/**
	 * Try Terminal Authentication, with the currently stored EAC-data, returning true if this
	 * succeeded.
	 * 
	 * @return whether this succeeded
	 */
	public boolean doTA() {
		return false;
		// super.doEAC(keyId, key, caReference, terminalCertificates,
		// terminalKedocumentNumber);
	}

	/**
	 * Send a GET CHALLENGE; the challenge received will be stored and used.
	 * Returns the resulting Status Word.
	 * 
	 * @return status word
	 * @throws CardServiceException
	 *             if there is a problem with the card reader
	 * @throws NullpointerException
	 *             if useSM is true but there is no SM session active
	 */
	public int sendGetChallenge(boolean useSM) throws CardServiceException {
		CommandAPDU capdu = createGetChallengeAPDU();
		if (useSM) {
			capdu = getWrapper().wrap(capdu);
		}
		ResponseAPDU rapdu = transmit(capdu);
		if (useSM) {
			rapdu = getWrapper().unwrap(rapdu, rapdu.getBytes().length);
		}
		if (rapdu.getSW() == 0x9000) {
			lastChallenge = rapdu.getData();
		}
		return rapdu.getSW();
	}

	/**
	 * Send a SELECT FILE command. Returns the resulting Status Word
	 * 
	 * @param fid
	 *            the file to select
	 * @param useSM
	 * @return status word, i.e. 0x9000 if this went ok
	 * @throws NullpointerException
	 *             if useSM is true but there is no SM session active
	 */
	public int sendSelectFile(short fid, boolean useSM) {
		try {
			if (useSM) {
				sendSelectFile(getWrapper(), fid);
			} else {
				sendSelectFile(null, fid);
			}
			return 0x9000;
		} catch (CardServiceException e) {
			return e.getSW();
		}
	}

	/**
	 * Send instruction ins, with P1=0 and P2=0,. Returns the resulting Status
	 * Word.
	 * 
	 * @param ins
	 *            the instruction to try
	 * @param useSM
	 * @return status word, i.e. 0x9000 if this went ok
	 * @throws NullpointerException
	 *             if useSM is true but there is no SM session active
	 */
	public int sendAnyInstruction(byte ins, boolean useSM) {
		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, ins,
				(byte) 0x00, (byte) 0x00);
		ResponseAPDU rapdu;
		try {
			rapdu = transmit(capdu);
		} catch (CardServiceException e) {
			e.printStackTrace();
			throw new RuntimeException("something low level went wrong");
		}
		return (rapdu.getSW());
	}

	/**
	 * Send a Mutual Authenticate in an attempt to successfully complete BAC,
	 * using the last challenge received. Returns the resulting Status Word
	 * 
	 * Ugly to have to copy & paste this from PassportService::doBAC, with as
	 * only change that instead of sending a GET CHALLENGE we use the last
	 * challenge we received, sort in the field lastChallenge
	 * 
	 * @return status word
	 * @throws CardServiceException
	 *             if something went wrong, but we don't know what.
	 */
	public int sendMutualAuthenticateToCompleteBAC()
			throws CardServiceException {
		try {
			byte[] keySeed = Util.computeKeySeed(documentNumber, SDF
					.format(dateOfBirth), SDF.format(dateOfExpiry));
			SecretKey kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
			SecretKey kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
			byte[] rndICC = lastChallenge; // We use the last challenge
			byte[] rndIFD = new byte[8];
			random.nextBytes(rndIFD);
			byte[] kIFD = new byte[16];
			random.nextBytes(kIFD);
			byte[] response = sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
			byte[] kICC = new byte[16];
			System.arraycopy(response, 16, kICC, 0, 16);
			keySeed = new byte[16];
			for (int i = 0; i < 16; i++) {
				keySeed[i] = (byte) ((kIFD[i] & 0xFF) ^ (kICC[i] & 0xFF));
			}
			SecretKey ksEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
			SecretKey ksMac = Util.deriveKey(keySeed, Util.MAC_MODE);
			long ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
			wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);
			return 0x9000;
		} catch (CardServiceException e) {
			if (e.getSW() == -1) {
				throw e;
			} else {
				return e.getSW();
			}
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		} catch (UnsupportedEncodingException uee) {
			throw new CardServiceException(uee.toString());
		}
	}

	/**
	 * Send a Mutual Authenticate incorrectly, in an attempt to fail BAC.
	 * Returns the resulting Status Word.
	 * 
	 * @return status word
	 */
	public int sendMutualAuthenticateToFailBAC() {
		// TODO complete
		return 0x9000;
	}

}
