/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2008  Martijn Oostdijk (martijn.oostdijk@gmail.com)
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

package sos.smartcards;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

/**
 * Manages all card terminals.
 * This is the source of card insertion and removal events.
 * Ideally this should be the only place where low level CardService
 * instances are created.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class CardManager
{
	private static final CardManager INSTANCE = new CardManager();
	private static final int POLL_INTERVAL = 200;

	private Collection<CardTerminal> terminals;
	private Collection<CardTerminalListener> listeners;
	private boolean isPolling;

	private CardManager() {	   
		try {
			listeners = new ArrayList<CardTerminalListener>();
			terminals = new HashSet<CardTerminal>();
			addTerminals();
			start();
		} catch (Exception ex) {
			System.err.println("WARNING: exception while adding terminals");
			ex.printStackTrace();
		}
	}

	/**
	 * Starts polling.
	 */
	public void start() {
		/* For each terminal start a polling thread. */
		isPolling = true;
		for (CardTerminal terminal: terminals) {
			(new Thread(new TerminalPoller(terminal))).start();
		}
	}

	/**
	 * Stops polling.
	 */
	public void stop() {
		synchronized(INSTANCE) {
			isPolling = false;
			notifyAll();
		}
	}

	private void addTerminals() {

		/* Default factory will contain connected PCSC terminals. */
		addTerminals(TerminalFactory.getDefault());

		/* Our own factories for 'non-PCSC' terminals. */

		try {
			Class<?> acrProvClass = Class.forName("ds.smartcards.acr122.ACR122Provider");
			Provider acrProv = (Provider)acrProvClass.newInstance();
			addTerminals(TerminalFactory.getInstance("ACR", null, acrProv));          
		} catch (Exception e) {
		}

		/* Factories for simulators. */

		try {
			addTerminals(TerminalFactory.getInstance("CREF", "localhost:9025", new sos.smartcards.CardTerminalProvider()));
		} catch (NoSuchAlgorithmException nsae) {
			/* Listing readers of this factory failed. */
		}

		try {
			addTerminals(TerminalFactory.getInstance("JCOP", "localhost:8050", new sos.smartcards.CardTerminalProvider()));
		} catch (NoSuchAlgorithmException nsae) {
			/* Listing readers of this factory failed. */
		}
	}

	private void addTerminals(TerminalFactory factory) {
		try {
			CardTerminals defaultTerminals = factory.terminals();
			List<CardTerminal> defaultTerminalsList = defaultTerminals.list();
			terminals.addAll(defaultTerminalsList);
		} catch (CardException cde) {
			/* NOTE: Listing of readers failed. Don't add anything. */
		}
	}

	public void addCardTerminalListener(CardTerminalListener l) {
		synchronized(INSTANCE) {
			listeners.add(l);
			notifyAll();
		}
	}

	public void removeCardTerminalListener(CardTerminalListener l) {
		synchronized(INSTANCE) {
			listeners.remove(l);
		}
	}

	private synchronized void notifyCardInserted(CardService service) {
		for (CardTerminalListener l: listeners) {
			l.cardInserted(new CardEvent(CardEvent.INSERTED, service));
		}
	}

	private synchronized void notifyCardRemoved(CardService service) {
		for (CardTerminalListener l: listeners) {
			l.cardRemoved(new CardEvent(CardEvent.REMOVED, service));
		}
	}

	private synchronized boolean hasNoListeners() {
		return listeners.isEmpty();
	}

	public Collection<CardTerminal> getTerminals() {
		return terminals;
	}

	public static CardManager getInstance() {
		return INSTANCE;
	}

	private class TerminalPoller implements Runnable
	{
		private CardTerminal terminal;
		private CardService service;

		public TerminalPoller(CardTerminal terminal) {
			this.terminal = terminal;
			this.service = null;
		}

		public void run() {
			try {
				while (isPolling) {
					if (hasNoListeners()) {
						synchronized(INSTANCE) {
							while (hasNoListeners()) {
								INSTANCE.wait();
							}
						}
					}
					try {
						boolean wasCardPresent = false;
						boolean isCardPresent = false;
						if (service != null) {
							wasCardPresent = true;
						} else {
							try {
								if (terminal.isCardPresent()) {
									service = new TerminalCardService(terminal);
								}
							} catch (Exception e) {
								if (service != null) { service.close(); }
							}
						}
						isCardPresent = terminal.isCardPresent();

						if (wasCardPresent && !isCardPresent) {
							if (service != null) {
								notifyCardRemoved(service);
								service.close();
							}
							service = null;
						} else if (!wasCardPresent && isCardPresent) {
							if (service != null) {
								notifyCardInserted(service);
							}
						}
					} catch (CardException ce) {
						// NOTE: remain in same state?!?
						ce.printStackTrace();
					} finally {
						if (!isPolling && service != null) { service.close(); }
						Thread.sleep(POLL_INTERVAL);
					}
				}
			} catch (InterruptedException ie) {
				/* NOTE: This ends thread when interrupted. */
			}

		}
	}
}
