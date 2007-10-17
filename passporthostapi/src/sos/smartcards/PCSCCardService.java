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
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

/**
 * Card service implementation for sending APDUs to a terminal using the
 * <code>javax.smartcardio.*</code> classes in Java SDK 6.0.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @version $Revision$
 */
public class PCSCCardService extends AbstractCardService
{
   private ArrayList<String> protocols;

   private Card card;
   private CardChannel channel;

   /**
    * Constructs a new card service.
    */
   public PCSCCardService() {
      protocols = new ArrayList<String>();
      protocols.add("T=1");
      protocols.add("T=0");
   }

   /**
    * Gives a list of terminals (card accepting devices) accessible by this
    * service.
    * 
    * @return a list of terminal names
    */
   public String[] getTerminals() {
      try {
         TerminalFactory factory = TerminalFactory.getDefault();
         List<CardTerminal> terminals = factory.terminals().list();
         String[] result = new String[terminals.size()];
         int i = 0;
         for (CardTerminal terminal : terminals) {
            result[i++] = terminal.toString();
         }
         return result;
      } catch (Exception ce) {
         ce.printStackTrace();
         return new String[0];
      }
   }

   /**
    * Opens a session with the card in the default terminal.
    */
   public void open() {
      try {
         TerminalFactory factory = TerminalFactory.getDefault();
         List<CardTerminal> terminals = factory.terminals().list();
         CardTerminal terminal = terminals.get(0);
         open(terminal);
      } catch (CardException ce) {
         ce.printStackTrace();
      }
   }

   /**
    * Opens a session with the card designated by <code>id</code>.
    * 
    * @param id some identifier (typically the name of a card terminal)
    */
   public void open(String id) {
      try {
         System.out.println("DEBUG: OPENING " + id);
         TerminalFactory factory = TerminalFactory.getDefault();
         List<CardTerminal> terminals = factory.terminals().list();
         CardTerminal terminal = null;
         for (CardTerminal t : terminals) {
            if (t.toString().equals(id)) {
               terminal = t;
            }
         }
         if (terminal == null) {
            terminal = terminals.get(0);
         }
         open(terminal);
      } catch (CardException ce) {
         ce.printStackTrace();
      }
   }
   
   void open(CardTerminal terminal) throws CardException {
      for (String protocol: protocols) {
         try {
            System.out.println("Trying " + protocol);
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
      notifyStartedAPDUSession();
      return;
   }

   /**
    * Sends an apdu to the card.
    * 
    * @param capdu the command apdu to send.
    * @return the response from the card, including the status word.
    */
   public ResponseAPDU transmit(CommandAPDU ourCommandAPDU) {
      try {
         if (channel == null) {
            System.err.println("DEBUG: channel == null");
            return null;
         }
         ResponseAPDU ourResponseAPDU = channel.transmit(ourCommandAPDU);
         notifyExchangedAPDU(ourCommandAPDU, ourResponseAPDU);
         return ourResponseAPDU;
      } catch (CardException ce) {
         ce.printStackTrace();
      }
      return null;
   }

   /**
    * Closes the session with the card.
    */
   public void close() {
      try {
         if (card != null) {
            card.disconnect(false);
         }
         notifyStoppedAPDUSession();
      } catch (CardException ce) {
         ce.printStackTrace();
      }
   }
}
