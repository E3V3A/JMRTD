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
 * $Id: PassportFileService.java 118 2006-07-28 09:20:17Z martijno $
 */

package sos.mrtd.evil;

import java.security.GeneralSecurityException;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;

import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;

/**
 * Card service for using the filesystem on the passport.
 * Defines reading of complete files.
 * 
 * Based on ICAO-TR-PKI and ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt; doBAC(...) ==&gt; readFile(fid)* ==&gt; close()
 *    </pre> 
 *
 * @author Ronny Wichers Schreur (ronny@cs.ru.nl)
 *
 * @version $Revision: 118 $
 */
public class EvilService extends PassportService
{
   /**
    * Creates a new passport service for accessing the passport.
    * 
    * @param service another service which will deal with sending
    *        the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public EvilService(CardService service) throws CardServiceException {
      super(service);
   }


   private CommandAPDU createEvilAPDU (byte evilInstruction, byte p1, byte p2, int le, byte [] data) {
	  CommandAPDU apdu = new CommandAPDU(EvilInterface.CLA_EVIL,
	         evilInstruction, p1, p2, data, le);
      return apdu;	   
   }

   /**
    * Opens the backdoor to an evil applet.
    *
    * @return interface version number of the evil applet.
    */
   public short openBackDoor() throws CardServiceException {
      SecureMessagingWrapper wrapper = getWrapper();
      short version;
      byte[] result;

      result = sendEvilCommand(wrapper, EvilInterface.INS_OPEN_BACKDOOR, (byte) 0, (byte) 0, 2,
    		  EvilInterface.ACCESS_CODE);
      version = (short) ((result [0] << 8) | result [1]);

      return version;
   }

   /**
    * Closes the backdoor to an evil applet.
    */
   public void closeBackDoor() throws CardServiceException {
      SecureMessagingWrapper wrapper = getWrapper();

      sendEvilCommand(wrapper, EvilInterface.INS_CLOSE_BACKDOOR, (byte) 0, (byte) 0, 0,
    		  null);
   }

   /**
    * Sends a <code>EVIL COMMANDS</code> command to the passport.
    * Secure messaging will be applied to the command and response
    * apdu.
    *
    * @param wrapper the secure messaging wrapper to use
    * @param offset offset into the file
    * @param le the expected length of the file to read
    *
    * @return a byte array of length <code>le</code> with
    *         (the specified part of) the contents of the
    *         currently selected file
    */
   public byte[] sendEvilCommand(SecureMessagingWrapper wrapper, byte ins, byte p1, byte p2, int le, byte[] payload)
   throws CardServiceException {
      CommandAPDU capdu = createEvilAPDU(ins, p1, p2, le, payload);
      if (wrapper != null) {
         capdu = wrapper.wrap(capdu);
      }
      ResponseAPDU rapdu = transmit(capdu);
      if (wrapper != null) {
         rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
      }
      return rapdu.getData();
   }
}
