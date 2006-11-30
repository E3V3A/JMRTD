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
 * $Id: $
 */

package sos.smartcards;

import java.util.*;
import javax.smartcardio.*;

/**
 * Card service implementation for sending APDUs to a terminal
 * using the <code>javax.smartcardio.*</code> classes in Java
 * SDK 6.0 (aka "Mustang").
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public class MustangCardService extends AbstractCardService
{
   
   private CardTerminal terminal;
   private Card card;
   private CardChannel channel;

   public MustangCardService() {
   }
   
   @Override
   public void close() {
      try {
         card.disconnect(false);
         notifyStoppedAPDUSession();
      } catch (CardException ce) {
         ce.printStackTrace();
      }
   }

   @Override
   public void open() {
      try {
         TerminalFactory factory = TerminalFactory.getDefault();
         List<CardTerminal> terminals = factory.terminals().list();
         terminal = terminals.get(0);
         card = terminal.connect("T=0");
         channel = card.getBasicChannel();
         notifyStartedAPDUSession();
      } catch (CardException ce) {
         ce.printStackTrace();
      }
   }

   @Override
   public byte[] sendAPDU(Apdu apdu) {
      try {
         CommandAPDU capdu = new CommandAPDU(apdu.getCommandApduBuffer());
         ResponseAPDU rapdu = channel.transmit(capdu);
         apdu.setResponseApduBuffer(rapdu.getBytes()); 
         notifyExchangedAPDU(apdu);
         return rapdu.getBytes();
      } catch (CardException ce) {
         ce.printStackTrace();
      }
      return null;
   }

   public String[] getTerminals() {
      try {
         TerminalFactory factory = TerminalFactory.getDefault();
         List<CardTerminal> terminals = factory.terminals().list();
         String[] result = new String[terminals.size()];
         int i = 0;
         for (CardTerminal terminal: terminals) {
            result[i] = terminal.toString();
         }
         return result;
      } catch (CardException ce) {
         ce.printStackTrace();
         return new String[0];
      }
   }

   public void open(String id) {
      try {
         TerminalFactory factory = TerminalFactory.getDefault();
         List<CardTerminal> terminals = factory.terminals().list();
         terminal = null;
         for (CardTerminal t: terminals) {
            if (t.toString().equals(id)) {
               this.terminal = t;
            }

         }
         if (terminal == null) {
            terminal = terminals.get(0);
         }
         card = terminal.connect("T=0");
         channel = card.getBasicChannel();
         notifyStartedAPDUSession();
      } catch (CardException ce) {
         ce.printStackTrace();
      }
   }
}