package sos.smartcards;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
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
 * instances are created.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class CardManager
{
	private static final CardManager INSTANCE = new CardManager();
	private static final int POLL_INTERVAL = 200;

	private Map<CardTerminal, CardService> terminalServices;
	private Collection<CardTerminal> terminals;
	private Collection<CardTerminalListener> listeners;

	private CardManager() {	   
		try {
			listeners = new ArrayList<CardTerminalListener>();
			terminals = new HashSet<CardTerminal>();
			terminalServices = new Hashtable<CardTerminal, CardService>();
			addTerminals();
		} catch (Exception ex) {
			System.err.println("WARNING: exception while adding terminals");
			ex.printStackTrace();
		}
		(new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						poll();
						Thread.sleep(POLL_INTERVAL);
					}
				} catch (InterruptedException ie) {
					// NOTE: interrupted during blocked state, so quit running
				}
			}
		})).start();
	}

	private synchronized void poll() throws InterruptedException {
		while (hasNoListeners()) {
			wait();
		}
		try {
			for (CardTerminal terminal: terminals) {
				boolean wasCardPresent = false;
				boolean isCardPresent = false;
				CardService service = null;
				if (terminalServices.containsKey(terminal)) {
					service = terminalServices.get(terminal);
					wasCardPresent = true;
				}
				if (service == null) {
					try {
						if (terminal.isCardPresent()) {
							service = new PCSCCardService(terminal);
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
					terminalServices.remove(terminal);
				} else if (!wasCardPresent && isCardPresent) {
					if (service != null) {
						terminalServices.put(terminal, service);
						notifyCardInserted(service);
					}
				}
			}
		} catch (CardException ce) {
			// NOTE: remain in same state?!?
			ce.printStackTrace();
		}
	}

	private void addTerminals() {
		try {
			/* Default factory will contain connected PCSC terminals. */
			addTerminals(TerminalFactory.getDefault());

			/* Our own factories for 'special' terminals. */
			addTerminals(TerminalFactory.getInstance("CREF", "localhost:9025", new sos.smartcards.CardTerminalProvider()));
//			addTerminals(TerminalFactory.getInstance("JCOP", "localhost:8050", new sos.smartcards.CardTerminalProvider()));
		} catch (NoSuchAlgorithmException nsae) {
			/* Listing other readers failed. */
			nsae.printStackTrace();
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

	public synchronized void addCardTerminalListener(CardTerminalListener l) {
		listeners.add(l);
		notifyAll();
	}

	public synchronized void removeCardTerminalListener(CardTerminalListener l) {
		listeners.remove(l);
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
}
