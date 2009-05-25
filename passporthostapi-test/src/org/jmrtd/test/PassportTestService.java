package org.jmrtd.test;

import java.security.PublicKey;
import java.util.Date;
import java.util.List;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.ISO7816;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.FileSystemStructured;
import net.sourceforge.scuba.tlv.BERTLVInputStream;
import net.sourceforge.scuba.util.Hex;

import org.ejbca.cvc.AlgorithmUtil;
import org.ejbca.cvc.CVCertificate;
import org.jmrtd.BACEvent;
import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;
import org.jmrtd.Util;

/**
 * Card service specifically designed for testing. It aims to provide both
 * high-level functionality (as PassportService does) and lower-level
 * functionality (as PasspotApduService does). It tries to do this in a
 * consistent way:
 * <ol>
 * <li>High-level commands doAA(), doBAC(), doEAC(), doCA() and doTA() return a
 * boolean indicating success or failure.</li>
 * <li>Low-level commands that correspond to a single command return the status
 * word of the response APDU.</li>
 * </ol>
 * while hiding low-level details about the actual payloads of APDUs as much as
 * possible.
 * 
 * Any (CardService)Exceptions escaping from these method signal some low-level
 * failures that the testing framework should not try to interpret.
 * 
 * The only reason to extend PassportService is to access protected fields
 * there. It would be cleaner to decouple it.
 * 
 * I'm not sure what is the easiest way to feed in the data needed for BAC and
 * EAC into here.
 * 
 * I'm not sure that it is a workable option to provide methods here that
 * do all the individual protocol steps.
 * 
 * @author erikpoll
 * 
 */
public class PassportTestService extends PassportService {

	/** Data needed for BAC **/

	private String documentNumber;
	private Date dateOfBirth;
	private Date dateOfExpiry;

	/** Data needed for AA **/

	private PublicKey publicKey;

	/** Data needed for EAC **/

	private int keyId;
	private PublicKey key;
	private String caReference;
	private List<CVCertificate> terminalCertificates;

	/** The last challenge received, if any **/
	byte[] lastChallenge;

	public PassportTestService(CardService service) throws CardServiceException {
		super(service);
	}

	/**
	 * Reset the card
	 * 
	 * @throws CardServiceException
	 */
	public void resetCard() throws CardServiceException {
		// This actually properly resets the card.
		if (isOpen()) {
			close();
		}
		open();
	}

	/**
	 * Perform BAC, with the currently stored MRZ-data
	 * 
	 * @return
	 */
	public boolean doBAC() {
		try {
			super.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
			return true;
		} catch (CardServiceException e) {
			return false;
		}
	}

	public boolean doAA() {
		super.doAA(publicKey);
	}

	public boolean doEAC() {
		super.doEAC(keyId, key, caReference, terminalCertificates, terminalKey,
				documentNumber);
	}

	/**
	 * Sent a GET CHALLENGE command.
	 * 
	 * @param useSM
	 *            use Secure Messaging;
	 * @return status word.
	 * @throws NullpointerException
	 *             if useSM is true but there is no SM session active
	 */
	public int getSendGetChallenge(boolean useSM) {
		CommandAPDU capdu = createGetChallengeAPDU();
		if (useSM & getWrapper() != null) {
			capdu = getWrapper().wrap(capdu);
		}
		ResponseAPDU rapdu = transmit(capdu);
		if (getWrapper() != null) {
			rapdu = getWrapper().unwrap(rapdu, rapdu.getBytes().length);
		}
		return rapdu.getSW();
	}

	/**
	 * Send a SELECT FILE command.
	 * 
	 * @param fid
	 *            the file to select
	 * @param useSM
	 * @return status word
	 * @throws NullpointerException
	 *             if useSM is true but there is no SM session active
	 */
	public int selectFile(short fid, boolean useSM) {
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
	 * Send arbitrary instruction ins, with P1=0 and P2=0, just to see if it is
	 * supported.
	 * 
	 * @param ins
	 *            the instruction to try
	 * @param useSM
	 * @return status word
	 * @throws NullpointerException
	 *             if useSM is true but there is no SM session active
	 */
	public int tryInstruction(byte ins, boolean useSM) {
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
	 * Send a GET CHALLENGE; the challenge received will be stored and used
	 * 
	 * @return status word
	 * @throws NullpointerException
	 *             if useSM is true but there is no SM session active
	 */
	public int getChallenge(boolean useSM) {
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
	 * Send a Mutual Authenticate in an attempt to successfully complete BAC,
	 * using the last challenge received.
	 * 
	 * Ugly to have to copy & paste this from PassportService, with only minor
	 * change.
	 * 
	 * @return status word
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
	 * Send a Mutual Authenticate in an attempt to fail complete BAC,
	 * using the last challenge received.
	 * 
	 * @return status word
	 */
	public int sendMutualAuthenticateToFailBAC() {
	  //TODO complete
	}

}
