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

package sos.mrtd;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import sos.smartcards.APDUListener;
import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.smartcards.ISO7816;

/**
 * Low level card service for sending apdus to the passport.
 * This service is not responsible for maintaining information about the
 * state of the authentication or secure messaging protocols. It merely
 * offers the basic functionality for sending passport specific apdus to
 * the passport.
 *
 * Based on ICAO-TR-PKI. Defines the following commands:
 * <ul>
 *    <li><code>GET CHALLENGE</code></li>
 *    <li><code>EXTERNAL AUTHENTICATE</code></li>
 *    <li><code>INTERNAL AUTHENTICATE</code> (using secure messaging)</li>
 *    <li><code>SELECT FILE</code> (using secure messaging)</li>
 *    <li><code>READ BINARY</code> (using secure messaging)</li>
 * </ul>
 *
 * @see PassportApduService
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class PassportApduService implements CardService
{
   /** The applet we select when we start a session. */
   private static final byte[] APPLET_AID = { 
      (byte) 0xA0, 0x00, 0x00, 0x02, 0x47, 0x10, 0x01 };

   /** Initialization vector used by the cipher below. */
   private static final IvParameterSpec ZERO_IV_PARAM_SPEC = new IvParameterSpec(
         new byte[8]);

   /** The service we decorate. */
   private CardService service;

   /** DESede encryption/decryption cipher. */
   private Cipher cipher;

   /** ISO9797Alg3Mac. */
   private Mac mac;

   /**
    * Creates a new passport apdu sending service.
    *
    * @param service another service which will deal with sending
    *        the apdus to the card
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives:
    *         <ul>
    *            <li>Cipher: "DESede/CBC/Nopadding"</li>
    *            <li>Mac: "ISO9797Alg3Mac"</li>
    *         </ul>
    */
   public PassportApduService(CardService service)
   throws GeneralSecurityException {
      if (service instanceof PassportApduService) {
         this.service = ((PassportApduService)service).service;
      } else {
         this.service = service;
      }
      cipher = Cipher.getInstance("DESede/CBC/NoPadding");
      mac = Mac.getInstance("ISO9797Alg3Mac");
   }

   /**
    * Opens a session by connecting to the card and
    * selecting the passport applet.
    */
   public void open() throws CardServiceException {
      if(!service.isOpen()) {
         service.open();
      }
      sendSelectApplet();
   }

   public void open(String id) throws CardServiceException {
      if(!service.isOpen()) {
         service.open(id);
      }
      sendSelectApplet();
   }

   public boolean isOpen() {
      return service.isOpen();
   }

   public String[] getTerminals() {
      return service.getTerminals();
   }

   private void sendSelectApplet() throws CardServiceException {
      int sw = sendSelectApplet(APPLET_AID);
      if (sw != 0x00009000) {
         throw new CardServiceException("Could not select passport");
      }
   }

   public ResponseAPDU transmit(CommandAPDU capdu) throws CardServiceException {
      return service.transmit(capdu);
   }

   public void close() {
      if (service != null) {
         service.close();
      }
   }

   public void setService(CardService service) {
      this.service = service;
   }

   public void addAPDUListener(APDUListener l) {
      service.addAPDUListener(l);
   }

   public void removeAPDUListener(APDUListener l) {
      service.removeAPDUListener(l);
   }

   CommandAPDU createSelectAppletAPDU(byte[] aid) {
      byte[] data = aid;
      CommandAPDU apdu = new CommandAPDU(
            ISO7816.CLA_ISO7816, ISO7816.INS_SELECT_FILE,
            (byte) 0x04, (byte) 0x00, data, (byte)0x01);
      return apdu;
   }

   CommandAPDU createSelectFileAPDU(short fid) {
      byte[] fiddle = { (byte) ((fid >> 8) & 0x000000FF),
            (byte) (fid & 0x000000FF) };
      return createSelectFileAPDU(fiddle);
   }

   CommandAPDU createSelectFileAPDU(byte[] fid) {
      CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_SELECT_FILE,
            (byte) 0x02, (byte) 0x0c, fid, 256);
      return apdu;
   }

   CommandAPDU createReadBinaryAPDU(short offset, int le) {
      byte p1 = (byte) ((offset & 0x0000FF00) >> 8);
      byte p2 = (byte) (offset & 0x000000FF);
      CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_READ_BINARY, p1,
            p2, le);
      return apdu;
   }

   CommandAPDU createGetChallengeAPDU() {
      byte p1 = (byte) 0x00;
      byte p2 = (byte) 0x00;
      int le = 8;
      CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_GET_CHALLENGE, p1,
            p2, le);
      return apdu;
   }

   CommandAPDU createInternalAuthenticateAPDU(byte[] rndIFD) {
      if (rndIFD == null || rndIFD.length != 8) {
         throw new IllegalArgumentException("rndIFD wrong length");
      }
      byte p1 = (byte)0x00;
      byte p2 = (byte)0x00;
      byte[] data = rndIFD;
      int le = 255; /* whatever... */
      CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_INTERNAL_AUTHENTICATE,
            p1, p2, data, le);
      return apdu;
   }

   /**
    * Creates an <code>EXTERNAL AUTHENTICATE</code> command.
    *
    * @param rndIFD our challenge
    * @param rndICC their challenge
    * @param kIFD our key material
    * @param kEnc the static encryption key
    * @param kMac the static mac key
    *
    * @return the apdu to be sent to the card.
    */
   CommandAPDU createMutualAuthAPDU(byte[] rndIFD, byte[] rndICC, byte[] kIFD,
         SecretKey kEnc, SecretKey kMac) throws GeneralSecurityException {
      if (rndIFD == null || rndIFD.length != 8) {
         throw new IllegalArgumentException("rndIFD wrong length");
      }
      if (rndICC == null || rndICC.length != 8) {
         // throw new IllegalArgumentException("rndICC wrong length");
         rndICC = new byte[8];
      }
      if (kIFD == null || kIFD.length != 16) {
         throw new IllegalArgumentException("kIFD wrong length");
      }
      if (kEnc == null) {
         throw new IllegalArgumentException("kEnc == null");
      }
      if (kMac == null) {
         throw new IllegalArgumentException("kMac == null");
      }

      cipher.init(Cipher.ENCRYPT_MODE, kEnc, ZERO_IV_PARAM_SPEC);
      /*
       cipher.update(rndIFD);
       cipher.update(rndICC);
       cipher.update(kIFD);
       // This doesn't work, apparently we need to create plaintext array.
       // Probably has something to do with ZERO_IV_PARAM_SPEC.
       */
      byte[] plaintext = new byte[32];
      System.arraycopy(rndIFD, 0, plaintext, 0, 8);
      System.arraycopy(rndICC, 0, plaintext, 8, 8);
      System.arraycopy(kIFD, 0, plaintext, 16, 16);
      byte[] ciphertext = cipher.doFinal(plaintext);
      if (ciphertext.length != 32) {
         throw new IllegalStateException("Cryptogram wrong length "
               + ciphertext.length);
      }

      mac.init(kMac);
      byte[] mactext = mac.doFinal(Util.pad(ciphertext));
      if (mactext.length != 8) {
         throw new IllegalStateException("MAC wrong length");
      }

      byte p1 = (byte) 0x00;
      byte p2 = (byte) 0x00;

      byte[] data = new byte[32 + 8];
      System.arraycopy(ciphertext, 0, data, 0, 32);
      System.arraycopy(mactext, 0, data, 32, 8);

      int le = 40;
      CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816,
            ISO7816.INS_EXTERNAL_AUTHENTICATE, p1, p2, data, le);
      return apdu;
   }

   /**
    * Sends a <code>SELECT APPLET</code> command to the card.
    *
    * @param aid the applet to select
    * 
    * @return status word
    */
   public int sendSelectApplet(byte[] aid) throws CardServiceException {
      return transmit(createSelectAppletAPDU(aid)).getSW();
   }

   /**
    * Sends a <code>SELECT FILE</code> command to the passport.
    *
    * @param fid the file to select
    */
   public void sendSelectFile(short fid) throws CardServiceException {
      sendSelectFile(null, fid);
   }

   /**
    * Sends a <code>SELECT FILE</code> command to the passport.
    * Secure messaging will be applied to the command and response
    * apdu.
    *
    * @param wrapper the secure messaging wrapper to use
    * @param fid the file to select
    */
   public void sendSelectFile(SecureMessagingWrapper wrapper, short fid)
   throws CardServiceException {
      CommandAPDU capdu = createSelectFileAPDU(fid);
      if (wrapper != null) {
         capdu = wrapper.wrap(capdu);
      }
      ResponseAPDU rapdu = transmit(capdu);
      if (wrapper != null) {
         rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
      }
   }

   void sendSelectFile(SecureMessagingWrapper wrapper, byte[] fid)
   throws CardServiceException {
      CommandAPDU capdu = createSelectFileAPDU(fid);
      if (wrapper != null) {
         capdu = wrapper.wrap(capdu);
      }
      ResponseAPDU rapdu = transmit(capdu);
      if (wrapper != null) {
         rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
      }
   }

   /**
    * Sends a <code>READ BINARY</code> command to the passport.
    *
    * @param offset offset into the file
    * @param le the expected length of the file to read
    *
    * @return a byte array of length <code>le</code> with
    *         (the specified part of) the contents of the
    *         currently selected file
    */
   public byte[] sendReadBinary(short offset, int le) throws CardServiceException {
      return sendReadBinary(null, offset, le);
   }

   /**
    * Sends a <code>READ BINARY</code> command to the passport.
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
   public byte[] sendReadBinary(SecureMessagingWrapper wrapper, short offset,
         int le) throws CardServiceException {
      boolean repeatOnEOF = false;
      ResponseAPDU rapdu = null;
      do {
         repeatOnEOF = false;
         // In case the data ended right on the block boundary
         if(le == 0) {
            return null;
         }
         CommandAPDU capdu = createReadBinaryAPDU(offset, le);
         if (wrapper != null) {
            capdu = wrapper.wrap(capdu);
         }
         rapdu = transmit(capdu);
         if (wrapper != null) {
            rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
         }
         if(rapdu.getSW() == ISO7816.SW_END_OF_FILE) {
            le--;
            repeatOnEOF = true;
         }
      }while(repeatOnEOF);
      return rapdu.getData();
   }

   /**
    * Sends a <code>GET CHALLENGE</code> command to the passport.
    *
    * @return a byte array of length 8 containing the challenge
    */
   public byte[] sendGetChallenge() throws CardServiceException {
      ResponseAPDU rapdu = transmit(createGetChallengeAPDU());
      return rapdu.getData();
   }

   /**
    * Sends an <code>INTERNAL AUTHENTICATE</code> command to the passport.
    *
    * @param rndIFD the challenge to send
    * 
    * @return the response from the passport (status word removed)
    */
   public byte[] sendInternalAuthenticate(byte[] rndIFD) throws CardServiceException {
      return sendInternalAuthenticate(null, rndIFD);
   }

   /**
    * Sends an <code>INTERNAL AUTHENTICATE</code> command to the passport.
    * 
    * @param wrapper secure messaging wrapper
    * @param rndIFD the challenge to send
    * 
    * @return the response from the passport (status word removed)
    */
   public byte[] sendInternalAuthenticate(SecureMessagingWrapper wrapper, byte[] rndIFD)
   throws CardServiceException {
      CommandAPDU capdu = createInternalAuthenticateAPDU(rndIFD);
      if (wrapper != null) {
         capdu = wrapper.wrap(capdu);
      }
      ResponseAPDU rapdu = transmit(capdu);
      if (wrapper != null) {
         rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
      }
      return rapdu.getData();
   }

   /**
    * Sends an <code>EXTERNAL AUTHENTICATE</code> command to the passport.
    * The resulting byte array has length 32 and contains <code>rndICC</code> 
    * (first 8 bytes), <code>rndIFD</code> (next 8 bytes), their key
    * material "<code>kICC</code>" (last 16 bytes).
    *
    * @param rndIFD our challenge
    * @param rndICC their challenge
    * @param kIFD our key material
    * @param kEnc the static encryption key
    * @param kMac the static mac key
    *
    * @return a byte array of length 32 containing the response that was
    *         sent by the passport, decrypted (using <code>kEnc</code>)
    *         and verified (using <code>kMac</code>)
    */
   public byte[] sendMutualAuth(byte[] rndIFD, byte[] rndICC, byte[] kIFD,
         SecretKey kEnc, SecretKey kMac) throws CardServiceException {
      try {
         byte[] rapdu = transmit(createMutualAuthAPDU(rndIFD, rndICC, kIFD, kEnc,
               kMac)).getBytes();

         if (rapdu.length != 42) {
            throw new IllegalStateException("Response wrong length: "
                  + rapdu.length + "!=" + 42);
         }

         /*
       byte[] eICC = new byte[32];
       System.arraycopy(rapdu, 0, eICC, 0, 32);

       byte[] mICC = new byte[8];
       System.arraycopy(rapdu, 32, mICC, 0, 8);
          */

         /* Decrypt the response. */
         cipher.init(Cipher.DECRYPT_MODE, kEnc, ZERO_IV_PARAM_SPEC);
         byte[] result = cipher.doFinal(rapdu, 0, rapdu.length - 8 - 2);
         if (result.length != 32) {
            throw new IllegalStateException("Cryptogram wrong length "
                  + result.length);
         }
         return result;
      } catch (GeneralSecurityException gse) {
         throw new CardServiceException(gse.toString());
      }
   }
}
