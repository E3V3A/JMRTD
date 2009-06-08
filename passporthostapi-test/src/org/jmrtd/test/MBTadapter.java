package org.jmrtd.test;

import java.security.Security;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import org.jmrtd.PassportService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;

/*********************************************************************/

import java.net.*;
import java.io.*;

/** ****************************************************************** */

/*******************************************************************************
 * Adapter between TorXakis and Passport Communication with TorXakis via
 * stream-mode socket Communication with Passport via Java Passport API Command
 * line argument: <port number of Server socket in this process>
 ******************************************************************************/

public class MBTadapter

{
	/** service to talk to the passport */
	private static PassportTestService service;

	/* Data format for dates */
	// private static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
	/***************************************************************************
	 * 
	 * setup connection with the card reader
	 */
	public static void setupCard() {
		try {
			TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null);
			CardTerminals terminals = tf.terminals();
			for (CardTerminal terminal : terminals
					.list(CardTerminals.State.CARD_PRESENT)) {
				service = new PassportTestService(new TerminalCardService(
						terminal));
				if (service != null) {
					service.open();
					break;
				}
			}
			if (service == null) {
				System.exit(-23);
			}
			service.setMRZ("XX1234587", "760803", "140507");
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

	/** ****************************************************************** */

	public static void main(String[] args) throws Exception

	{
		Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		if (args.length != 1) {
			System.out.println("own port number required");
		} else {
			try {
				int portNo = Integer.parseInt(args[0]);

				// instantiate a socket for accepting a connection
				ServerSocket servsock = new ServerSocket(portNo);

				// wait to accept a connection request
				// then a data socket is created
				Socket sock = servsock.accept();

				// get an input stream for reading from the data socket
				InputStream inStream = sock.getInputStream();
				// create a BufferedReader object for text line input
				BufferedReader sockin = new BufferedReader(
						new InputStreamReader(inStream));

				// get an output stream for writing to the data socket
				OutputStream outStream = sock.getOutputStream();
				// create a PrinterWriter object for character-mode output
				PrintWriter sockout = new PrintWriter(new OutputStreamWriter(
						outStream));

				// initialize card reader
				setupCard();
				System.out.println("eac setup: "+service.setupEAC());
				//System.out.println("aa setup: "+service.setupAA());
				resetCard();

				while (true) { // read a line from the data stream
					String inAction = sockin.readLine();

					if (inAction.equals("Reset")) {
						resetCard();
					}
					if (inAction.equals("BAC_Call")) {
						if (service.doBAC()) {
							sockout.println("BAC_OK");
						} else {
							sockout.println("BAC_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("GetChallenge_noSM_Call")) {
						int stw = service.sendGetChallengeAndStore(false);
						if (stw == 0x9000) {
							sockout.println("GetChallenge_noSM_OK");
						} else {
							sockout.println("GetChallenge_noSM_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("GetChallenge_Call")) {
						int stw = service.sendGetChallengeAndStore(true);
						if (stw == 0x9000) {
							sockout.println("GetChallenge_OK");
						} else {
							sockout.println("GetChallenge_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("CompleteBAC_Call")) {
						int res = service.sendMutualAuthenticateToCompleteBAC();
						if (res == 0x9000) {
							sockout.println("CompleteBAC_OK");
						} else {
							sockout.println("CompleteBAC_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("FailBAC_Call")) {
						if (service.failBAC()) {
							sockout.println("FailBAC_OK");
						} else {
							sockout.println("FailBAC_NOK");
						}
						sockout.flush();
					}					
					if (inAction.startsWith("ReadFile_Call")) {
						String par = inAction.substring(13).trim();
						short fd = (short) Integer.parseInt(par);
						if (service.canReadFile (fd, true)) {
							sockout.println("ReadFile_OK");
						} else {
							sockout.println("ReadFile_NOK");
						}
						sockout.flush();
					}				
					if (inAction.startsWith("ReadFile_Call_noSM")) {
						String par = inAction.substring(18).trim();
						short fd = (short) Integer.parseInt(par);
						if (service.canReadFile (fd, false)) {
							sockout.println("ReadFile_noSM_OK");
						} else {
							sockout.println("ReadFile_noSM_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("AA_Call")) {
						if (service.doAA()) {
							sockout.println("AA_OK");
						} else {
							sockout.println("AA_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("EAC_Call")) {
						if (service.doEAC()) {
							sockout.println("EAC_OK");
						} else {
							sockout.println("EAC_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("CA_Call")) {
						if (service.doCA()) {
							sockout.println("CA_OK");
						} else {
							sockout.println("CA_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("TA_Call")) {
						if (service.doTA()) {
							sockout.println("TA_OK");
						} else {
							sockout.println("TA_NOK");
						}
						sockout.flush();
					}
					if (inAction.equals("FailEAC_Call")) {
						if (service.failEAC()) {
							sockout.println("FailEAC_OK");
						} else {
							sockout.println("FailEAC_NOK");
						}
						sockout.flush();
					}					
		
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

/** ****************************************************************** */

