package org.jmrtd.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import junit.framework.TestCase;
import net.sourceforge.scuba.smartcards.APDUEvent;
import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.CommandAPDU;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.cert.CardVerifiableCertificate;

public abstract class PassportTesterBase extends TestCase implements
		APDUListener {


	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();
	
	static {
		Security.addProvider(BC_PROVIDER);
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
				service = new PassportTestService(CardService.getInstance(terminal));
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

	public void exchangedAPDU(APDUEvent e) {
		CommandAPDU capdu = e.getCommandAPDU();
		ResponseAPDU rapdu = e.getResponseAPDU();
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

    /** Reads in a CVCertficate object from a file */
	protected static CardVerifiableCertificate readCVCertificateFromFile(File f) {
		try {
			CertificateFactory cf = CertificateFactory.getInstance("CVC", JMRTDSecurityProvider.getInstance());
			return (CardVerifiableCertificate)cf.generateCertificate(new FileInputStream(f));
		} catch (Exception e) {
			return null;
		}
	}

    /** Reads in a key file (ECDSA) */
    protected static PrivateKey readKeyFromFile(File f) {
        try {
            byte[] data = loadFile(f);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
            KeyFactory gen = KeyFactory.getInstance("ECDSA");
            return gen.generatePrivate(spec);
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Reads the byte data from a file.
     */
    protected static byte[] loadFile(File file) throws IOException {
        byte[] dataBuffer = null;
        FileInputStream inStream = null;
        try {
            dataBuffer = new byte[(int) file.length()];
            inStream = new FileInputStream(file);
            inStream.read(dataBuffer);
            inStream.close();
        } catch (IOException e1) {
            if (inStream != null)
                inStream.close();
            System.out.println("loadFile error: " + e1);
        }
        return dataBuffer;
    }

    public static void writeFile(File file, byte[] data) throws IOException {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
            outStream.write(data);
        } finally {
            if (outStream != null)
                outStream.close();
        }
    }

}
