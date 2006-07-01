/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, Radboud University
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
 * $Id: AbstractCardService.java,v 1.12 2006/06/09 10:43:37 martijno Exp $
 */

package sos.smartcards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Default abstract service.
 * Provides some functionality for observing apdu events.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: 1.12 $
 */
public abstract class AbstractCardService implements CardService {
    static protected final int SESSION_STOPPED_STATE = 0;

    static protected final int SESSION_STARTED_STATE = 1;

    /** The listeners. */
    private Collection listeners;

    /*@ invariant state == SESSION_STOPPED_STATE || state == SESSION_STARTED_STATE;
     */
    protected int state;


    /**
     * Creates a new service.
     */
    public AbstractCardService() {
        listeners = new ArrayList();
        state = SESSION_STOPPED_STATE;
    }

    /**
     * Adds a listener.
     * 
     * @param l
     *            the listener to add
     */
    public void addAPDUListener(APDUListener l) {
        listeners.add(l);
    }

    /**
     * Removes the listener <code>l</code>, if present.
     * 
     * @param l
     *            the listener to remove
     */
    public void removeAPDUListener(APDUListener l) {
        listeners.remove(l);
    }

    /**
     * Opens a session with the card. Selects a reader. Connects to the card.
     * Notifies any interested listeners.
     */
    /*
     *@ requires state == SESSION_STOPPED_STATE;
     *@ ensures state == SESSION_STARTED_STATE;
     */
    public abstract void open();

    /**
     * Sends and apdu to the card. Notifies any interested listeners.
     * 
     * @param apdu
     *            the command apdu to send.
     * 
     * @return the response from the card, including the status word.
     */
    /*@ requires state == SESSION_STARTED_STATE;
     *@ ensures state == SESSION_STARTED_STATE;
     */
    public abstract byte[] sendAPDU(Apdu apdu);

    /**
     * Closes the session with the card. Disconnects from the card and reader.
     * Notifies any interested listeners.
     */
    /*@ requires state == SESSION_STARTED_STATE;
     *@ ensures state == SESSION_STOPPED_STATE;
     */
    public abstract void close();

    protected void notifyStartedAPDUSession() {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            APDUListener listener = (APDUListener) it.next();
            listener.startedAPDUSession();
        }
    }

    protected void notifyExchangedAPDU(Apdu apdu) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            APDUListener listener = (APDUListener) it.next();
            listener.exchangedAPDU(apdu, apdu.getResponseApduBuffer());
        }
    }

    protected void notifyStoppedAPDUSession() {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            APDUListener listener = (APDUListener) it.next();
            listener.stoppedAPDUSession();
        }
    }
}
