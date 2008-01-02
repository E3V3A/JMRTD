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
 * $Id: CREFService.java 258 2007-10-17 13:16:23Z martijno $
 */

package sos.smartcards;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadDevice;
import com.sun.javacard.apduio.CadTransportException;

/**
 * Implements same interface as JPCSCService, but for library Apduio which is
 * part of Sun's JavaCard SDK.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @version $Revision: 258 $
 */
public class CREFEmulatorService extends AbstractCardService
{
   private CadClientInterface cad;
   private Socket sock;
   private String host;
   private int port;

   public CREFEmulatorService(String host, int port) throws CardServiceException {
      try {
         this.host = host;
         this.port = port;
         this.sock = new Socket(host, port);
      } catch (IOException ioe) {
         throw new CardServiceException(ioe.toString());
      }
   }

   public String[] getTerminals() {
      String[] terminals = { "CREF emulator at " + host + ":" + port };
      return terminals;
   }

   public void open(String id) throws CardServiceException {
      open();
   }

   public void open() throws CardServiceException {
      if (isOpen()) { return; }
      try {
         InputStream is = sock.getInputStream();
         OutputStream os = sock.getOutputStream();
         cad = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);
         cad.powerUp();
         state = SESSION_STARTED_STATE;
      } catch (IOException ioe) {
         if (sock != null) { try { sock.close(); } catch (IOException ioex) {} }
         throw new CardServiceException(ioe.toString());
      } catch (CadTransportException e) {
         if (sock != null) { try { sock.close(); } catch (IOException ioex) {} }
         throw new CardServiceException(e.toString());
      }
   }

   public boolean isOpen() {
      return (state != SESSION_STOPPED_STATE);            
   }

   /**
    * Sends and apdu to the card. Notifies any interested listeners.
    * 
    * @param ourCommandAPDU the command apdu to send.
    * @return the response from the card, including the status word.
    */
   public ResponseAPDU transmit(CommandAPDU ourCommandAPDU) throws CardServiceException {
      try {
         com.sun.javacard.apduio.Apdu theirApdu = new com.sun.javacard.apduio.Apdu();
         setCommand(ourCommandAPDU, theirApdu);
         cad.exchangeApdu(theirApdu);
         ResponseAPDU ourResponseAPDU = getResponse(theirApdu);
         notifyExchangedAPDU(ourCommandAPDU, ourResponseAPDU);
         return ourResponseAPDU;
      } catch (IOException e) {
         throw new CardServiceException(e.toString());
      } catch (CadTransportException e) {
         throw new CardServiceException(e.toString());
      } catch (Exception e) {
         throw new CardServiceException(e.toString());
      }
   }

   /**
    * Closes the session with the card. Disconnects from the card and reader.
    * Notifies any interested listeners.
    */
   public void close() {
      try {
         if (cad != null) {
            cad.powerDown();
            sock.close();

            // cad.powerDown(false);
            // powers down, but doesn't kill the socket.
         }
      } catch (Exception e) {
      } finally {
         state = SESSION_STOPPED_STATE;
      }
   }

   private void setCommand(CommandAPDU ourApdu,
         com.sun.javacard.apduio.Apdu theirApdu) {
      byte[] buffer = ourApdu.getBytes();
      int len = buffer.length;

      /* Set lc and le... */
      int lc = 0;
      int le = buffer[len - 1] & 0x000000FF;
      if (len == 4) {
         lc = 0;
         le = 0; // FIXME: maybe this should be -1?
      } else if (len == 5) {
         /* No cdata, byte at index 5 is le. */
         lc = 0;
      } else if (len > 5) {
         /* Byte at index 5 is not le, so it must be lc. */
         lc = buffer[ISO7816.OFFSET_LC] & 0x000000FF;
      }
      if (4 + lc >= len) {
         /* Value of lc covers rest of apdu length, there is no le. */
         le = 0; // FIXME: maybe this should be -1?
      }
      theirApdu.setLc(lc);
      theirApdu.setLe(le);

      /* Set header */
      theirApdu.command = new byte[4];
      System.arraycopy(buffer, 0, theirApdu.command, 0, 4);

      /* Set data */
      theirApdu.dataIn = new byte[lc];
      System.arraycopy(buffer, ISO7816.OFFSET_CDATA, theirApdu.dataIn, 0, lc);
   }

   private ResponseAPDU getResponse(com.sun.javacard.apduio.Apdu theirApdu) {
      int len = theirApdu.getDataOut().length;
      byte[] rapdu = new byte[len + 2];
      System.arraycopy(theirApdu.getDataOut(), 0, rapdu, 0, len);
      System.arraycopy(theirApdu.getSw1Sw2(), 0, rapdu, len, 2);
      return new ResponseAPDU(rapdu);
   }
}
