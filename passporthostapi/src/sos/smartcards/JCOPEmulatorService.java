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

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.ibm.jc.JCException;
import com.ibm.jc.terminal.RemoteJCTerminal;

/**
 * JCOPEmulatorService
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @version $Revision$
 */
public class JCOPEmulatorService extends AbstractCardService
{

   private static final String TERMINAL_NAME = "JCOP emulator";
   private static final String[] TERMINALS = { TERMINAL_NAME };
   private RemoteJCTerminal terminal;

   public void open() {
      try {
         terminal = new RemoteJCTerminal();
         terminal.init("localhost:8050");
         terminal.open();
         terminal.waitForCard(1000);
      } catch (JCException jce) {
         throw new IllegalStateException(
               "Couldn't establish connection to the emulator: "
                     + terminal.getErrorMessage());
      }
      state = SESSION_STARTED_STATE;
      notifyStartedAPDUSession();
   }

   public String[] getTerminals() {
      return TERMINALS;
   }

   public void open(String id) {
      if (!id.equals(TERMINAL_NAME)) { throw new IllegalArgumentException(
            "Unknown terminal " + id); }
      open();
   }

   public ResponseAPDU transmit(CommandAPDU ourCommandAPDU) {
      if (terminal == null) { throw new IllegalStateException(
            "Terminal session seems not to be opened."); }
      try {
         byte[] capdu = ourCommandAPDU.getBytes();
         byte[] rapdu = terminal.send(0, capdu, 0, capdu.length);
         ResponseAPDU ourResponseAPDU = new ResponseAPDU(rapdu);
         notifyExchangedAPDU(ourCommandAPDU, ourResponseAPDU);
         return ourResponseAPDU;
      } catch (JCException jce) {
         throw new IllegalStateException("Send APDU failed: "
               + terminal.getErrorMessage());
      }
   }

   public void close() {
      try {
         terminal.close();
         terminal = null;
      } catch (JCException jce) {
         throw new IllegalStateException(
               "Couldn't establish connection to the emulator: "
                     + terminal.getErrorMessage());
      }
      state = SESSION_STOPPED_STATE;
      notifyStoppedAPDUSession();
   }

}
