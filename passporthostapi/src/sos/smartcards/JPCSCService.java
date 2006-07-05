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
 * $Id$
 */

package sos.smartcards;

import com.linuxnet.jpcsc.Card;
import com.linuxnet.jpcsc.Context;
import com.linuxnet.jpcsc.PCSC;

/**
 * Simple card service implementation for sending APDUs to a terminal.
 * This is somewhat similar to OCF's PassthruCardService (but using JPCSC).
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 1.10 $
 */
public class JPCSCService extends AbstractCardService
{
   private static final String PREFERRED_READER_NAME = "OMNIKEY";

   private Context context;
   private Card card;

   public JPCSCService() {
      super();
      context = new Context();
      context.EstablishContext(PCSC.SCOPE_GLOBAL, null, null);
      card = null;
   }
   
   public String[] getTerminals() {
      context = new Context();
      context.EstablishContext(PCSC.SCOPE_GLOBAL, null, null);
      card = null;
      return context.ListReaders();
   }
   
   /**
    * Opens a session with the card.
    * Selects a reader. Connects to the card.
    */
   public void open() {
      context = new Context();
      context.EstablishContext(PCSC.SCOPE_GLOBAL, null, null);
      card = null;
      String[] readers = context.ListReaders();
      if (readers.length == 1) {
         /* Only one reader, take it. */
         card = context.Connect();
      } else {
         /* Multiple readers, find the first one with our name. */
         for (int i = 0; i < readers.length; i++) {
            String reader = readers[i];
            if (reader.startsWith(PREFERRED_READER_NAME)) {
               card = context.Connect(reader);
               if (card != null) {
                  break;
               }
            } else if (i + 1 == readers.length) {
               /* Multiple readers, none with our name, take the last one. */
               card = context.Connect(reader);
            }
         }
      }
      if (card == null) {
         throw new IllegalStateException("No readers found!");
      }
      card.BeginTransaction();
      state = SESSION_STARTED_STATE;
      notifyStartedAPDUSession();
   }

   public void open(String reader) {
      context = new Context();
      context.EstablishContext(PCSC.SCOPE_GLOBAL, null, null);
      card = context.Connect(reader);
      if (card == null) {
         throw new IllegalStateException("Reader \"" + reader + "\" not found!");
      }
      card.BeginTransaction();
      state = SESSION_STARTED_STATE;
      notifyStartedAPDUSession();
   }

   /**
    * Sends and apdu to the card.
    * Notifies any interested listeners.
    *
    * @param apdu initially contains the command apdu to send,
    *             afterwards contains the response apdu sent by the card.
    *
    * @return the response from the card, including the status word.
    */
   /*@ requires state == SESSION_STARTED_STATE;
     @ ensures state == SESSION_STARTED_STATE;
    */
   public byte[] sendAPDU(Apdu apdu) {
      byte[] capdu = apdu.getCommandApduBuffer();
      byte[] rapdu = card.Transmit(capdu, 0, capdu.length);
      apdu.setResponseApduBuffer(rapdu);   
      notifyExchangedAPDU(apdu);
      return rapdu;
   }

   /**
    * Closes the session with the card.
    * Disconnects from the card and reader.
    * Notifies any interested listeners.
    */
   /*@ requires state == SESSION_STARTED_STATE;
     @ ensures state == SESSION_STOPPED_STATE;
    */
   public void close() {
      try {
         card.EndTransaction(PCSC.RESET_CARD);
         card.Disconnect();
         context.ReleaseContext();
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         state = SESSION_STOPPED_STATE;
         notifyStoppedAPDUSession();
      }
   }
}

