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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import sos.smartcards.CardEvent;
import sos.smartcards.CardManager;
import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.smartcards.CardTerminalListener;

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
		CardManager cm = CardManager.getInstance();
		cm.addCardTerminalListener(new CardTerminalListener() {

			public void cardInserted(CardEvent ce) {
				CardService service = ce.getService();

				try {
					PassportService passportService = new PassportService(service);
					passportService.open(); /* Selects applet... */
					cardTypes.put(service, CardType.PASSPORT);
					passportServices.put(service, passportService);
					notifyPassportInserted(passportService);
				} catch (CardServiceException cse) {
					cardTypes.put(service, CardType.OTHER_CARD);
				}
			}

			public void cardRemoved(CardEvent ce) {
				CardService service = ce.getService();
				CardType cardType = cardTypes.remove(service);
				if (cardType != null && cardType == CardType.PASSPORT) {
					PassportService passportService = passportServices.get(service);
					notifyPassportRemoved(passportService);
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

	private void notifyPassportInserted(final PassportService passportService) {
		final PassportEvent pe = new PassportEvent(PassportEvent.INSERTED, passportService);
		for (final PassportListener l : listeners) { 
			(new Thread(new Runnable() {
				public void run() {
					l.passportInserted(pe);
				}
			})).start();
		}
	}

	private void notifyPassportRemoved(final PassportService passportService) {
		final PassportEvent pe = new PassportEvent(PassportEvent.REMOVED, passportService);
		for (final PassportListener l : listeners) {
			(new Thread(new Runnable() {
				public void run() {
					l.passportRemoved(pe);
				}
			})).start();
		}
	}
}
