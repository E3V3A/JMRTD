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
 * $Id: JCOPEmulatorService.java 258 2007-10-17 13:16:23Z martijno $
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
 * @version $Revision: 258 $
 */
public class JCOPEmulatorService extends AbstractCardService
{
   private RemoteJCTerminal terminal;

   private final String[] terminals = new String[1];
   
   public void open() throws CardServiceException {
      open("localhost:8050");
    }

   public void open(String id) throws CardServiceException {
      try {
         terminal = new RemoteJCTerminal();
         terminal.init(id);
         terminal.open();
         terminal.waitForCard(1000);
         terminals[0] = "JCOP emulator at " + id;
         state = SESSION_STARTED_STATE;
         notifyStartedAPDUSession();
      } catch (JCException jce) {
         throw new CardServiceException(jce.toString());
      }
   }

   public String[] getTerminals() {
      return terminals;
   }
   
   public boolean isOpen() {
      return (state != SESSION_STOPPED_STATE);
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
