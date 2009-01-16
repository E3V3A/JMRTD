/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id: $
 */

package sos.mrtd;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.smartcardio.TerminalFactory;

import sos.smartcards.CardEvent;
import sos.smartcards.CardManager;
import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.smartcards.CardTerminalListener;

/**
 * Manages passport insertion and removal events.
 *
 * FIXME: Perhaps have this class extend sos.smartcards.CardManager?
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 */
public class PassportManager
{
	private enum CardType { OTHER_CARD, PASSPORT };

	private static final PassportManager INSTANCE = new PassportManager();

	private Map<CardService, CardType> cardTypes;
	private Map<CardService, PassportService> passportServices;
	private Collection<PassportListener> listeners;

	private PassportManager() {
		cardTypes = new Hashtable<CardService, CardType>();
		passportServices = new Hashtable<CardService, PassportService>();
		listeners = new ArrayList<PassportListener>();
		final CardManager cm = CardManager.getInstance();
		try {
			Class<?> acrProviderClass = Class.forName("ds.smartcards.acr122.ACR122Provider");
			Provider acrProvider = (Provider)acrProviderClass.newInstance();
			TerminalFactory acrFactory = TerminalFactory.getInstance("ACR", null, acrProvider);
			cm.addTerminals(acrFactory, true);
		} catch (ClassNotFoundException cnfe) {
			/* Ignore this provider... not installed */ 
		} catch (Exception e) {
			/* Ignore this provider */
		}
		try {
			Class<?> acrProviderClass = Class.forName("ds.smartcards.acr120.ACR120UProvider");
			Provider acrProvider = (Provider)acrProviderClass.newInstance();
			TerminalFactory acrFactory = TerminalFactory.getInstance("ACR120U", null, acrProvider);
			System.out.println("DEBUG: acrFactory = " + acrFactory);
			cm.addTerminals(acrFactory, false);
		} catch (ClassNotFoundException cnfe) {
			/* Ignore this provider... not installed */
			cnfe.printStackTrace();
		} catch (Exception e) {
			/* Ignore this provider */
			e.printStackTrace();
		}
		try {
			Class<?> crefProviderClass = Class.forName("ds.smartcards.CREFTerminalProvider");
			Provider crefProvider = (Provider)crefProviderClass.newInstance();
			TerminalFactory crefFactory = TerminalFactory.getInstance("CREF", "localhost:9025", crefProvider);
			cm.addTerminals(crefFactory, false);
		} catch (ClassNotFoundException cnfe) {
			/* Ignore this provider... not installed */ 
		} catch (Exception e) {
			/* Ignore this provider */
			e.printStackTrace();
		}
		try {
			Class<?> jcopProviderClass = Class.forName("ds.smartcards.JCOPTerminalProvider");
			Provider jcopProvider = (Provider)jcopProviderClass.newInstance();
			TerminalFactory jcopFactory = TerminalFactory.getInstance("JCOP", "localhost:8050", jcopProvider);
			cm.addTerminals(jcopFactory, false);
		} catch (ClassNotFoundException cnfe) {
			/* Ignore this provider... not installed */ 
		} catch (Exception e) {
			/* Ignore this provider */
			e.printStackTrace();
		}
		cm.addCardTerminalListener(new CardTerminalListener() {

			public void cardInserted(CardEvent ce) {
				notifyCardEvent(ce);
				CardService service = ce.getService();
				try {
					PassportService passportService = new PassportService(service);
					passportService.open(); /* Selects applet... */
					cardTypes.put(service, CardType.PASSPORT);
					passportServices.put(service, passportService);
					final PassportEvent pe = new PassportEvent(PassportEvent.INSERTED, passportService);
					notifyPassportEvent(pe);
				} catch (CardServiceException cse) {
					cardTypes.put(service, CardType.OTHER_CARD);
				}
			}

			public void cardRemoved(CardEvent ce) {
				notifyCardEvent(ce);
				CardService service = ce.getService();
				CardType cardType = cardTypes.remove(service);
				if (cardType != null && cardType == CardType.PASSPORT) {
					PassportService passportService = passportServices.get(service);
					final PassportEvent pe = new PassportEvent(PassportEvent.REMOVED, passportService);
					notifyPassportEvent(pe);
				}
			}
		});
	}

	public synchronized void addPassportListener(PassportListener l) {
		listeners.add(l);
	}

	public synchronized void removePassportListener(PassportListener l) {
		listeners.remove(l);
	}

	public static PassportManager getInstance() {
		return INSTANCE;
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

	private void notifyPassportEvent(final PassportEvent pe) {
		for (final PassportListener l : listeners) { 
			(new Thread(new Runnable() {
				public void run() {
					switch (pe.getType()) {
					case PassportEvent.INSERTED: l.passportInserted(pe); break;
					case PassportEvent.REMOVED: l.passportRemoved(pe); break;
					}	
				}
			})).start();
		}
	}
}
