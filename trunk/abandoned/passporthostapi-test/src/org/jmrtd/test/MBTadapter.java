
package org.jmrtd.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Provider;
import java.security.Security;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;

import org.jmrtd.JMRTDSecurityProvider;

/** ****************************************************************** */

/*******************************************************************************
 * Adapter between TorXakis and Passport Communication with TorXakis via
 * stream-mode socket Communication with Passport via Java Passport API Command
 * line argument: <port number of Server socket in this process>
 ******************************************************************************/

public class MBTadapter

{
	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();
	
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
				service = new PassportTestService(CardService.getInstance(terminal));
				if (service != null) {
					service.open();
					break;
				}
			}
			if (service == null) {
				System.exit(-23);
			}
			service.setMRZ("XX1234587", "760803", "140507"); /* Passport MRZ*/
//			service.setMRZ("IZP3R8132", "391109", "140406"); /* ID card MRZ*/
//			service.setMRZ("IU4559DL3", "620827", "070507"); /* ID card MRZ*/
			service.setupAA();
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
		Security.addProvider(BC_PROVIDER);

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
					if (inAction.startsWith("NoSM_ReadFile_Call")) {
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
					if (inAction.startsWith("RandomInstrSM_Call")) {
						String par = inAction.substring(18).trim();
						byte instr = (byte) Byte.parseByte(par);
						int res = service.sendAnyInstruction(instr,true);
						if (res == 0x9000) {
							sockout.println("RandomInstrSM_OK");
						} 
						else if (res == 0x6D00) {
							sockout.println("SW_INS_NOT_SUPPORTED");
						}
						else if (res == 0x6988) {
							sockout.println("SW_SM_DATA_OBJECTS_INCORRECT");
						}
						else if (res == 0x6982) {
							sockout.println("SW_SECURITY_STATUS_NOT_SATISFIED");
						}
						else if (res == 0x6A86) {
							sockout.println("SW_INCORRECT_P1P2");
						}
						else {
							sockout.println("RandomInstrSM_NOK");
						}
						sockout.flush();
					}
					if (inAction.startsWith("RandomInstrnoSM_Call")) {
						String par = inAction.substring(20).trim();
						byte instr = (byte) Byte.parseByte(par);
						int res = service.sendAnyInstruction(instr,false);
						if (res == 0x9000) {
							sockout.println("RandomInstnorSM_OK");
						} 
						else if (res == 0x6D00) {
							sockout.println("SW_INS_NOT_SUPPORTED");
						}
						else if (res == 0x6988) {
							sockout.println("SW_SM_DATA_OBJECTS_INCORRECT");
						}
						else if (res == 0x6982) {
							sockout.println("SW_SECURITY_STATUS_NOT_SATISFIED");
						}
						else if (res == 0x6A86) {
							sockout.println("SW_INCORRECT_P1P2");
						}
						else {
							sockout.println("RandomInstrnoSM_NOK");
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
