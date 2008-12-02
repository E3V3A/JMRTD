/*
 * JCOP driver for javax.smartcardio framework.
 * 
 * Copyright (C) 2008  Martijn Oostdijk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: $
 */

package ds.smartcards;

import java.nio.ByteBuffer;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.ibm.jc.JCException;
import com.ibm.jc.JCTerminal;
import com.ibm.jc.terminal.RemoteJCTerminal;

/**
 * CardTerminal implementation for NXP's JCOP emulator. This
 * makes it possible to use JCOP as an ordinary terminal within
 * the <code>javax.smartcardio</code> framework.
 * 
 * @author Cees-Bart Breunesse (ceesb@riscure.com)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @version $Revision: $
 */
public class JCOPEmulatorTerminal extends CardTerminal
{
	private static final long CARD_CHECK_SLEEP_TIME = 150;
	private static final long HEARTBEAT_TIMEOUT = 1200;

	private String hostName;
	private int port;

	private long heartBeat;

	private final Object terminal;
	private boolean wasCardPresent;
	private EmulatedCard card;

	private RemoteJCTerminal jcTerminal;

	/**
	 * Listens for instances of the emulator on the specified host and port.
	 * 
	 * @param hostName the host name, for instance <code>&quot;localhost&quot;"</code>
	 * @param port the port number, for instance <code>9025</code>
	 */
	public JCOPEmulatorTerminal(String hostName, int port) {
		terminal = this;
		this.hostName = hostName;
		this.port = port;
		jcTerminal = new RemoteJCTerminal();
		jcTerminal.init(hostName.trim() + ":" + port);
		heartBeat = System.currentTimeMillis();
	}

	/**
	 * Connects to the emulator.
	 * 
	 * @param protocol is ignored for now
	 */
	public synchronized Card connect(String protocol) throws CardException {
		synchronized (terminal) {
			if (!isCardPresent()) {
				throw new CardException("No card present");
			}
			if (card == null) {
				card = new EmulatedCard();
			}
			return card;
		}
	}

	/**
	 * The name of this terminal.
	 * 
	 * @return the name of this terminal.
	 */
	public String getName() {
		return "JCOP emulator at " + hostName + ":" + port;
	}

	public String toString() {
		return getName();
	}

	/**
	 * Determines whether a card is present.
	 * 
	 * @return whether a card is present.
	 */
	public boolean isCardPresent() {
		synchronized (terminal) {
			if (jcTerminal == null) { return false; }
			if ((System.currentTimeMillis() - heartBeat) < HEARTBEAT_TIMEOUT) { return wasCardPresent; }
			try {
				jcTerminal.open();	
				switch(jcTerminal.getState()) {
				case JCTerminal.CARD_PRESENT: wasCardPresent = true; break;
				case JCTerminal.NOT_CONNECTED: wasCardPresent = false; break;
				case JCTerminal.ERROR: wasCardPresent = false; break;
				default: wasCardPresent = false; break;
				}
				heartBeat = System.currentTimeMillis();
			} catch (Throwable e1) {
				wasCardPresent = false;
//				try { jcTerminal.close(); } catch (Exception e2) { }
			}
		}
		return wasCardPresent;
	}

	/**
	 * Waits for a card to be present. TODO: NEEDS TESTING!
	 * 
	 * @param timeout a timeout
	 * 
	 * @return whether the card became present before the timeout expired
	 */
	public boolean waitForCardAbsent(long timeout) throws CardException {
		long startTime = System.currentTimeMillis();
		if (CARD_CHECK_SLEEP_TIME > timeout) {
			return !isCardPresent();
		}
		try {
			while (isCardPresent()) {
				if (System.currentTimeMillis() - startTime > timeout) {
					break;
				}
				Thread.sleep(CARD_CHECK_SLEEP_TIME);
			}
		} catch (InterruptedException ie) {
			/* NOTE: Exit on interrupt. */
		}
		return !isCardPresent();
	}

	/**
	 * Waits for a card to be absent. TODO: NEEDS TESTING!
	 * 
	 * @param timeout a timeout
	 * 
	 * @return whether the card became absent before the timeout expired
	 */
	public boolean waitForCardPresent(long timeout) throws CardException {
		/* TODO: test this method. */
		long startTime = System.currentTimeMillis();
		if (CARD_CHECK_SLEEP_TIME > timeout) {
			return isCardPresent();
		}
		try {
			while (!isCardPresent()) {
				if (System.currentTimeMillis() - startTime > timeout) {
					break;
				}
				Thread.sleep(CARD_CHECK_SLEEP_TIME);
			}
		} catch (InterruptedException ie) {
			/* NOTE: Exit on interrupt. */
		}
		return isCardPresent();
	}

	/**
	 * This merely wraps channel.
	 */
	private class EmulatedCard extends Card
	{
		private EmulatedCardChannel basicChannel;

		public EmulatedCard() {
			basicChannel = new EmulatedCardChannel(this);
		}

		public void beginExclusive() throws CardException {
		}

		public void disconnect(boolean reset) throws CardException {
			basicChannel.close();
		}

		public void endExclusive() throws CardException {
		}

		public ATR getATR() {
			return basicChannel.getATR();
		}

		public CardChannel getBasicChannel() {
			return basicChannel;
		}

		public String getProtocol() {
			return "T=1";
		}

		public CardChannel openLogicalChannel() throws CardException {
			return null;
		}

		public byte[] transmitControlCommand(int controlCode, byte[] command)
		throws CardException {
			return null;
		}
	}

	/**
	 * Basic card channel to the emulator.
	 */
	private class EmulatedCardChannel extends CardChannel
	{
		private ATR atr;

		private Card card;

		public EmulatedCardChannel(Card card) {
			synchronized (terminal) {
				this.card = card;
				jcTerminal.open();
				byte[] atrBytes = jcTerminal.waitForCard(1000);
				atr = new ATR(atrBytes);
				heartBeat = System.currentTimeMillis();
			}

		}

		public ATR getATR() {
			return atr;
		}

		public void close() throws CardException {
			synchronized (terminal) {
				try {
					jcTerminal.close();
				} catch (JCException jce) {
					throw new CardException(
							"Couldn't establish connection to the emulator: "
							+ jcTerminal.getErrorMessage());
				}
			}
		}

		public Card getCard() {
			return card;
		}

		public int getChannelNumber() {
			return 0;
		}

		/**
		 * Transmits a command to the card.
		 * 
		 * @param command the command to send
		 * 
		 * @return the response from the card.
		 */
		public int transmit(ByteBuffer command, ByteBuffer response) throws CardException {
			synchronized (terminal) {
				ResponseAPDU rapdu = transmit(new CommandAPDU(command));
				byte[] rapduBytes = rapdu.getBytes();
				response.put(rapduBytes);
				return rapduBytes.length;
			}
		}

		/**
		 * Transmits a command to the card.
		 * 
		 * @param command the command to send
		 * 
		 * @return the response from the card.
		 */
		public ResponseAPDU transmit(CommandAPDU command) throws CardException {
			synchronized (terminal) {
				try {
					byte[] capdu = command.getBytes();
					byte[] rapdu = jcTerminal.send(0, capdu, 0, capdu.length);
					ResponseAPDU ourResponseAPDU = new ResponseAPDU(rapdu);
					heartBeat = System.currentTimeMillis();
					return ourResponseAPDU;
				} catch (Exception e) {
					throw (e instanceof CardException) ? (CardException)e : new CardException(e.toString());
				}
			}
		}
	}
}
