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
 * @version $Revision$
 */
public class CREFService extends AbstractCardService
{

   private static final String TERMINAL_NAME = "CREF simulator";
   private static final String[] TERMINALS = { TERMINAL_NAME };

   private CadClientInterface cad;

   public String[] getTerminals() {
      return TERMINALS;
   }

   /**
    * Opens a session with the card. Selects a reader. Connects to the card.
    * Notifies any interested listeners.
    */
   /*
    * @ requires state == SESSION_STOPPED_STATE; @ ensures state ==
    * SESSION_STARTED_STATE;
    */
   public void open() {
      Socket sock;

      try {
         sock = new Socket("localhost", 9025);
         InputStream is = sock.getInputStream();
         OutputStream os = sock.getOutputStream();

         cad = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);
         cad.powerUp();
      } catch (IOException e) {
         System.out.println(e);
      } catch (CadTransportException e) {
         System.out.println(e);
      }
      state = SESSION_STARTED_STATE;
      notifyStartedAPDUSession();
   }

   public void open(String id) {
      if (!id.equals(TERMINAL_NAME)) { throw new IllegalArgumentException(
            "Unknown terminal " + id); }
      open();
   }

   /**
    * Sends and apdu to the card. Notifies any interested listeners.
    * 
    * @param ourCommandAPDU the command apdu to send.
    * @return the response from the card, including the status word.
    */
   /*
    * @ requires state == SESSION_STARTED_STATE; @ ensures state ==
    * SESSION_STARTED_STATE;
    */
   public ResponseAPDU transmit(CommandAPDU ourCommandAPDU) {
      try {
         com.sun.javacard.apduio.Apdu theirApdu = new com.sun.javacard.apduio.Apdu();
         setCommand(ourCommandAPDU, theirApdu);
         cad.exchangeApdu(theirApdu);
         ResponseAPDU ourResponseAPDU = getResponse(theirApdu);
         notifyExchangedAPDU(ourCommandAPDU, ourResponseAPDU);
         return ourResponseAPDU;
      } catch (IOException e) {
         System.out.println(e);
         return null; // TODO: throw CardException here...
      } catch (CadTransportException e) {
         System.out.println(e);
         return null; // TODO: throw CardException here...
      }
   }

   /**
    * Closes the session with the card. Disconnects from the card and reader.
    * Notifies any interested listeners.
    */
   /*
    * @ requires state == SESSION_STARTED_STATE; @ ensures state ==
    * SESSION_STOPPED_STATE;
    */
   public void close() {
      try {
         cad.powerDown(false);
         // powers down, but doesn't kill the socket.
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         state = SESSION_STOPPED_STATE;
         notifyStoppedAPDUSession();
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
