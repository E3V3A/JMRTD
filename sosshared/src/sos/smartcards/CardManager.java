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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private static final Comparator<CardTerminal> TERMINAL_COMPARATOR = new Comparator<CardTerminal>() {
		public int compare(CardTerminal o1, CardTerminal o2) {
			return ((CardTerminal)o1).getName().compareToIgnoreCase(((CardTerminal)o2).getName());
		}
	};

	private Map<CardTerminal, TerminalPoller> terminals;
	private Collection<CardTerminalListener> listeners;

	private CardManager() {	   
		try {
			listeners = new ArrayList<CardTerminalListener>();
			terminals = new HashMap<CardTerminal, TerminalPoller>();
			addTerminals();
		} catch (Exception ex) {
			System.err.println("WARNING: exception while adding terminals");
			ex.printStackTrace();
		}
	}
	
	/**
	 * @deprecated Use {@link #startPolling(CardTerminal)}.
	 */
	public synchronized void start() {
		for (CardTerminal terminal: terminals.keySet()) {
			startPolling(terminal);
		}
	}
	
	/**
	 * @deprecated Use {@link #stopPolling(CardTerminal)}.
	 */
	public synchronized void stop() {
		for (CardTerminal terminal: terminals.keySet()) {
			stopPolling(terminal);
		}
	}
	
	/**
	 * Starts polling <code>terminal</code> (if not already doing so).
	 * 
	 * @param terminal a card terminal
	 */
	public synchronized void startPolling(CardTerminal terminal) {
		TerminalPoller poller = terminals.get(terminal);
		if (poller == null) { poller = new TerminalPoller(terminal); }
		poller.startPolling();
		notifyAll();
	}
	
	/**
	 * Stops polling <code>terminal</code>.
	 * 
	 * @param terminal a card terminal
	 */
	public synchronized void stopPolling(CardTerminal terminal) {
		TerminalPoller poller = terminals.get(terminal);
		if (poller == null) { return; }
		try {
			poller.stopPolling();
		} catch (InterruptedException ie) {
			/* NOTE: if thread interrupted we just quit. */
		}
		notifyAll();
	}
	
	/**
	 * Whether we are polling <code>terminal</code>.
	 *
	 * @param terminal a card terminal
	 *
	 * @return a boolean
	 */
	public synchronized boolean isPolling(CardTerminal terminal) {
		TerminalPoller poller = terminals.get(terminal);
		if (poller == null) { return false; }
		return poller.isPolling();
	}

	/**
	 * Gets the service associated with <code>terminal</code> (or <code>null</code> if
	 * we are not polling <code>terminal</code>).
	 *
	 * @param terminal a card terminal
	 *
	 * @return a card service or <code>null</code>
	 */
	public synchronized CardService getService(CardTerminal terminal) {
		TerminalPoller poller = terminals.get(terminal);
		if (poller == null) { return null; }
		CardService service = poller.getService();
		return service;
	}

	/**
	 * Whether the card manager is running.
	 * 
	 * @return a boolean indicating whether the card manager is running.
	 * 
	 * @deprecated Use {@link #isPolling(CardTerminal)}.
	 */
	public boolean isPolling() {
		boolean isPolling = false;
		for (CardTerminal terminal: terminals.keySet()) {
			TerminalPoller poller = terminals.get(terminal);
			isPolling ^= poller.isPolling();
		}
		return isPolling;
	}
	
	private void addTerminals() {
		addTerminals(TerminalFactory.getDefault(), true);
	}

	/**
	 * Adds the terminals produced by <code>factory</code>.
	 *
	 * @param factory
	 * @return the number of terminals added
	 */
	public synchronized int addTerminals(TerminalFactory factory, boolean isPolling) {
		try {
			CardTerminals additionalTerminals = factory.terminals();
			if (additionalTerminals == null) { return 0; }
			List<CardTerminal> additionalTerminalsList = additionalTerminals.list();
			if (additionalTerminalsList == null) { return 0; }
			List<CardTerminal> terminalsList = new ArrayList<CardTerminal>();
			terminalsList.addAll(additionalTerminalsList);
			for (CardTerminal terminal: terminalsList) {
				addTerminal(terminal, isPolling);
			}
			return additionalTerminalsList.size();
		} catch (CardException cde) {
			/* NOTE: Listing of readers failed. Don't add anything. */
		}
		return 0;
	}

	/**
	 * Adds a terminal.
	 *
	 * @param terminal the card terminal to add
	 * @param isPolling whether we should immediately start polling this terminal
	 */
	public synchronized void addTerminal(CardTerminal terminal, boolean isPolling) {
		TerminalPoller poller = terminals.get(terminal);
		if (poller == null) {
			poller = new TerminalPoller(terminal);
			terminals.put(terminal, poller);
		}
		if (isPolling && !isPolling(terminal)) {
			startPolling(terminal);
		}
		if (!isPolling && isPolling(terminal)) {
			stopPolling(terminal);
		}
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
		List<CardTerminal> result = new ArrayList<CardTerminal>();
		result.addAll(terminals.keySet());
		Collections.sort(result, TERMINAL_COMPARATOR);
		return result;
	}

	/**
	 * Gets the card manager.
	 * By default only PC/SC terminals are added,
	 * use {@link #addTerminals(TerminalFactory,boolean)} to add additional terminals.
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
		private boolean isPolling;
		private Thread myThread;

		public TerminalPoller(CardTerminal terminal) {
			this.terminal = terminal;
			this.service = null;
			this.isPolling = false;
		}
		
		public boolean isPolling() {
			return isPolling;
		}
		
		public synchronized void startPolling() {
			if (isPolling()) { return; }
			isPolling = true;
			if (myThread != null && myThread.isAlive()) { return; }
			myThread = new Thread(this);
			myThread.start();
		}
		
		public synchronized void stopPolling() throws InterruptedException {
			if (!isPolling()) { return; }
			isPolling = false;
			wait();
		}
		
		public CardService getService() {
			return service;
		}
		
		public String toString() {
			return "Poller for " + terminal.getName() + (isPolling ? " (polling)" : " (not polling)");
		}

		public void run() {
			try {
				while (isPolling()) {
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
						//		if (isCardPresent) {
						//			terminal.waitForCardAbsent(POLL_INTERVAL); // or longer..
						//		} else {
						//			terminal.waitForCardPresent(POLL_INTERVAL); // or longer..
						//		}
						// // ... so we'll just sleep for a while as a courtesy to other threads...
						Thread.sleep(POLL_INTERVAL);
					} catch (CardException ce) {
						/* FIXME: what if reader no longer connected, should we remove it from list? */
						// ce.printStackTrace(); // for debugging
					} finally {
						if (!isPolling() && service != null) { service.close(); }

					}
				}
			} catch (InterruptedException ie) {
				/* NOTE: This ends thread when interrupted. */
			}
			notifyAll(); /* NOTE: we just stopped polling, stopPolling may be waiting on us. */
		}
	}
}
