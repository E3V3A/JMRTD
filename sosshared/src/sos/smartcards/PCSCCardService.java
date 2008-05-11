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

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Card service implementation for sending APDUs to a terminal using the
 * <code>javax.smartcardio.*</code> classes in Java SDK 6.0 and higher.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @version $Revision$
 */
public class PCSCCardService extends AbstractCardService
{
   private static ArrayList<String> protocols;

   private CardTerminal terminal;
   private Card card;
   private CardChannel channel;

   /**
    * Constructs a new card service.
    * @param terminal 
    */
   public PCSCCardService(CardTerminal terminal) {
	   this.terminal = terminal;
      if (protocols == null) {
         protocols = new ArrayList<String>();
      }
      if (protocols.size() < 2) {
    	  protocols.add("T=CL");
         protocols.add("T=1");
         protocols.add("T=0");
      }
   }
   
   public void open() throws CardServiceException {
      if (isOpen()) { return; }
      for (String protocol: protocols) {
         try {
            card = terminal.connect(protocol);
            if (protocols.indexOf(protocol) != 0) {
               protocols.remove(protocols.indexOf(protocol));
               protocols.add(0, protocol);
            }
            break;
         } catch (Exception e) {
            continue;
         }
      }
      channel = card.getBasicChannel();
      if (channel == null) { throw new CardServiceException("channel == null"); }
      state = SESSION_STARTED_STATE;
   }
   
   public boolean isOpen() {
      return (state != SESSION_STOPPED_STATE);
   }

   /**
    * Sends an apdu to the card.
    * 
    * @param ourCommandAPDU the command apdu to send.
    * @return the response from the card, including the status word.
    */
   public ResponseAPDU transmit(CommandAPDU ourCommandAPDU) throws CardServiceException {
      try {
         if (channel == null) {
            throw new CardServiceException("channel == null");
         }
         ResponseAPDU ourResponseAPDU = channel.transmit(ourCommandAPDU);
         notifyExchangedAPDU(ourCommandAPDU, ourResponseAPDU);
         return ourResponseAPDU;
      } catch (CardException ce) {
    	  ce.printStackTrace();
         throw new CardServiceException(ce.toString());
      }
   }

   /**
    * Closes the session with the card.
    */
   public void close() {
      try {
         if (card != null) {
            card.disconnect(false);
         }
         state = SESSION_STOPPED_STATE;
      } catch (CardException ce) {
         ce.printStackTrace();
      }
   }
}
