/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2008  The JMRTD team
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

/**
 * Manages all card terminals.
 * This is the source of card insertion and removal events.
 * Ideally this should be the only place where low level CardService
 * instances (such as {@link sos.smartcards.TerminalCardService TerminalCardService})
 * are created.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class CardManager
{
	private static final CardManager INSTANCE = new CardManager();
	private static final int POLL_INTERVAL = 450;

	private List<CardTerminal> terminals;
	private Collection<CardTerminalListener> listeners;
	private boolean isPolling;

	private CardManager() {	   
		try {
			listeners = new ArrayList<CardTerminalListener>();
			terminals = new ArrayList<CardTerminal>();
			addTerminals();
		} catch (Exception ex) {
			System.err.println("WARNING: exception while adding terminals");
			ex.printStackTrace();
		}
	}

	/**
	 * Starts polling.
	 */
	public synchronized void start() {
		/* For each terminal start a polling thread. */
		if (isPolling) { return; }
		isPolling = true;
		for (CardTerminal terminal: terminals) {
			(new Thread(new TerminalPoller(terminal))).start();
		}
	}

	/**
	 * Stops polling.
	 */
	public synchronized void stop() {
		isPolling = false;
		notifyAll();
	}

	/**
	 * Whether the card manager is running.
	 * 
	 * @return a boolean indicating whether the card manager is running.
	 */
	public boolean isPolling() {
		return isPolling;
	}
	
	private void addTerminals() {
		int n = addTerminals(TerminalFactory.getDefault());
		if (n == 0) {
			System.out.println("DEBUG: no PC/SC terminals found!");
		}
	}

	/**
	 * Adds the terminals produced by <code>factory</code>.
	 *
	 * @param factory
	 * @return
	 */
	public synchronized int addTerminals(TerminalFactory factory) {
		try {
			CardTerminals newTerminals = factory.terminals();
			if (newTerminals == null) { return 0; }
			List<CardTerminal> newTerminalsList = newTerminals.list();
			if (newTerminalsList == null) { return 0; }
			terminals.addAll(newTerminalsList);
			return newTerminalsList.size();
		} catch (CardException cde) {
			/* NOTE: Listing of readers failed. Don't add anything. */
		}
		return 0;
	}

	/**
	 * Adds a listener.
	 * 
	 * @param l the listener to add
	 */
	public void addCardTerminalListener(CardTerminalListener l) {
		synchronized(INSTANCE) {
			listeners.add(l);
			notifyAll();
		}
	}

	/**
	 * Removes a listener.
	 * 
	 * @param l the listener to remove
	 */
	public void removeCardTerminalListener(CardTerminalListener l) {
		synchronized(INSTANCE) {
			listeners.remove(l);
		}
	}

	private void notifyCardEvent(final CardEvent ce) {
		for (final CardTerminalListener l : listeners) { 
			(new Thread(new Runnable() {
				public void run() {
					switch (ce.getType()) {
					case CardEvent.INSERTED: l.cardInserted(ce); break;
					case CardEvent.REMOVED: l.cardRemoved(ce); break;
					}	
				}
			})).start();
		}
	}

	private synchronized boolean hasNoListeners() {
		return listeners.isEmpty();
	}

	/**
	 * Gets a list of terminals.
	 * 
	 * @return a list of terminals
	 */
	public List<CardTerminal> getTerminals() {
		return terminals;
	}

	/**
	 * Gets the card manager.
	 * By default only PC/SC terminals are added, use {@link #addTerminals(TerminalFactory)} to add additional terminals.
	 * 
	 * @return the card manager
	 */
	public static CardManager getInstance() {
		return INSTANCE;
	}

	private class TerminalPoller implements Runnable
	{
		private CardTerminal terminal;
		private TerminalCardService service;

		public TerminalPoller(CardTerminal terminal) {
			this.terminal = terminal;
			this.service = null;
		}

		public void run() {
			try {
				while (isPolling) {
					if (hasNoListeners()) {
						/* No listeners, we go to sleep. */
						synchronized(INSTANCE) {
							while (hasNoListeners()) {
								INSTANCE.wait();
							}
						}
					}
					boolean wasCardPresent = false;
					boolean isCardPresent = false;
					long currentTime = System.currentTimeMillis();
					try {
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
						if (service != null && (currentTime - service.getLastActiveTime() < POLL_INTERVAL)) {
							isCardPresent = true;
						} else {
							isCardPresent = terminal.isCardPresent();
						}
						if (wasCardPresent && !isCardPresent) {
							if (service != null) {
								final CardEvent ce = new CardEvent(CardEvent.REMOVED, service);
								notifyCardEvent(ce);
								service.close();
							}
							service = null;
						} else if (!wasCardPresent && isCardPresent) {
							if (service != null) {
								final CardEvent ce = new CardEvent(CardEvent.INSERTED, service);
								notifyCardEvent(ce);
							}
						}


						// // This doesn't seem to work on some variants of Linux + pcsclite. :(
						//						if (isCardPresent) {
						//							terminal.waitForCardAbsent(POLL_INTERVAL);
						//						} else {
						//							terminal.waitForCardPresent(POLL_INTERVAL);
						//						}
						Thread.sleep(POLL_INTERVAL);
					} catch (CardException ce) {
						/* NOTE: remain in same state?!? */
						/* FIXME: what if reader no longer connected? */
						// ce.printStackTrace(); // for debugging
					} finally {
						if (!isPolling && service != null) { service.close(); }

					}
				}
			} catch (InterruptedException ie) {
				/* NOTE: This ends thread when interrupted. */
			}

		}
	}
}
