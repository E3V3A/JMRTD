/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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
 * $Id$
 */

package sos.smartcards;

import java.util.ArrayList;
import java.util.Collection;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Default abstract service. Provides some functionality for observing apdu
 * events.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @version $Revision$
 */
public abstract class AbstractCardService implements CardService
{
   static protected final int SESSION_STOPPED_STATE = 0;

   static protected final int SESSION_STARTED_STATE = 1;

   /** The apduListeners. */
   private Collection<APDUListener> apduListeners;
   
   private Collection<SessionListener> sessionListeners;

   /*
    * @ invariant state == SESSION_STOPPED_STATE || state ==
    * SESSION_STARTED_STATE;
    */
   protected int state;

   /**
    * Creates a new service.
    */
   public AbstractCardService() {
      apduListeners = new ArrayList<APDUListener>();
      sessionListeners = new ArrayList<SessionListener>();
      state = SESSION_STOPPED_STATE;
   }

   /**
    * Adds a listener.
    * 
    * @param l the listener to add
    */
   public void addAPDUListener(APDUListener l) {
      apduListeners.add(l);
   }
   
   public void addSessionListener(SessionListener l) {
      sessionListeners.add(l);
   }

   /**
    * Removes the listener <code>l</code>, if present.
    * 
    * @param l the listener to remove
    */
   public void removeAPDUListener(APDUListener l) {
      apduListeners.remove(l);
   }

   /**
    * Opens a session with the card. Selects a reader. Connects to the card.
    * Notifies any interested apduListeners.
    */
   /*
    * @ requires state == SESSION_STOPPED_STATE; @ ensures state ==
    * SESSION_STARTED_STATE;
    */
   public abstract void open();

   /**
    * Sends and apdu to the card. Notifies any interested apduListeners.
    * 
    * @param apdu the command apdu to send.
    * @return the response from the card, including the status word.
    */
   /*
    * @ requires state == SESSION_STARTED_STATE; @ ensures state ==
    * SESSION_STARTED_STATE;
    */
   public abstract ResponseAPDU transmit(CommandAPDU apdu);

   /**
    * Closes the session with the card. Disconnects from the card and reader.
    * Notifies any interested apduListeners.
    */
   /*
    * @ requires state == SESSION_STARTED_STATE; @ ensures state ==
    * SESSION_STOPPED_STATE;
    */
   public abstract void close();

   /**
    * Notifes listeners about initialization of APDU session.
    */
   protected void notifyStartedAPDUSession() {
      for (SessionListener listener: sessionListeners) {
         listener.sessionStarted(this);
      }
   }

   /**
    * Notifies listeners about APDU event.
    * 
    * @param capdu APDU event
    */
   protected void notifyExchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
      for (APDUListener listener: apduListeners) {
         listener.exchangedAPDU(capdu, rapdu);
      }
   }

   /**
    * Notifes listeners about termination of APDU session.
    */
   protected void notifyStoppedAPDUSession() {
      for (SessionListener listener: sessionListeners) {
         listener.sessionStopped(this);
      }
   }
}
