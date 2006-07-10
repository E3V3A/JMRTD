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
 * $Id: $
 */

package sos.mrtd;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import sos.smartcards.APDUListener;
import sos.smartcards.Apdu;
import sos.smartcards.CardService;

/**
 * Card service for using the BAC and AA protocols on the passport.
 * Defines basic access control, active authentication.
 * 
 * Based on ICAO-TR-PKI and ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt; doBAC(...) ==&gt; doAA() ==&gt; close()
 *    </pre> 
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public class PassportAuthService implements CardService
{
   private static final int SESSION_STOPPED_STATE = 0;
   private static final int SESSION_STARTED_STATE = 1;
   private static final int BAC_AUTHENTICATED_STATE = 2;
   private static final int AA_AUTHENTICATED_STATE = 3;
   private int state;
   
   private Collection authListeners;

   private PassportApduService service;
   private SecureMessagingWrapper wrapper;
   private Signature aaSignature;
   private MessageDigest aaDigest = MessageDigest.getInstance("SHA1");
   private Cipher aaCipher = Cipher.getInstance("RSA");

   private PassportAuthService() throws GeneralSecurityException {
      aaSignature = Signature.getInstance("SHA1WithRSA/ISO9796-2"); /* FIXME: SHA1WithRSA also works? */
      aaDigest = MessageDigest.getInstance("SHA1");
      aaCipher = Cipher.getInstance("RSA");
      authListeners = new ArrayList();
   }
   
   /**
    * Creates a new passport service for accessing the passport.
    * 
    * @param service another service which will deal with sending
    *        the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public PassportAuthService(CardService service)
   throws GeneralSecurityException {
      this();
      if (service instanceof PassportAuthService) {
         this.service = ((PassportAuthService)service).service;
      } else if (service instanceof PassportApduService) {
         this.service = (PassportApduService)service;
      } else {
         this.service = new PassportApduService(service);
      }   
      state = SESSION_STOPPED_STATE;
   }
   
   /**
    * Hack to construct a passport service from a service that is already open.
    * This should be removed some day.
    * 
    * @param service underlying service
    * @param wrapper encapsulates secure messaging state
    */
   public PassportAuthService(CardService service, SecureMessagingWrapper wrapper)
   throws GeneralSecurityException {
      this(service);
      this.wrapper = wrapper;
      if (state < BAC_AUTHENTICATED_STATE) {
         state = BAC_AUTHENTICATED_STATE;
      }
   }

   /**
    * Opens a session. This is done by connecting to the card, selecting the
    * passport applet.
    */
   public void open() {
      if (state == SESSION_STARTED_STATE) {
         return;
      }
      service.open();
      state = SESSION_STARTED_STATE;
   }
   
   public String[] getTerminals() {
      return service.getTerminals();
   }

   public void open(String id) {
      if (state == SESSION_STARTED_STATE) {
         return;
      }
      service.open(id);
      state = SESSION_STARTED_STATE;
   }
   
   /**
    * Performs the <i>Basic Access Control</i> protocol.
    *
    * @param docNr the document number
    * @param dateOfBirth card holder's birth date
    * @param dateOfExpiry document's expiry date
    */
   public void doBAC(String docNr, String dateOfBirth, String dateOfExpiry)
         throws GeneralSecurityException, UnsupportedEncodingException {
      byte[] keySeed = Util.computeKeySeed(docNr, dateOfBirth, dateOfExpiry);
      SecretKey kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
      SecretKey kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
      byte[] rndICC = service.sendGetChallenge();
      byte[] rndIFD = new byte[8]; /* random */
      byte[] kIFD = new byte[16]; /* random */
      byte[] response = service.sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
      byte[] kICC = new byte[16];
      System.arraycopy(response, 16, kICC, 0, 16);
      keySeed = new byte[16];
      for (int i = 0; i < 16; i++) {
         keySeed[i] = (byte) ((kIFD[i] & 0x000000FF) ^ (kICC[i] & 0x000000FF));
      }
      SecretKey ksEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
      SecretKey ksMac = Util.deriveKey(keySeed, Util.MAC_MODE);
      long ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
      wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);
      notifyBACPerformed(wrapper);
      state = BAC_AUTHENTICATED_STATE;
   }
   
   public void addAuthenticationListener(AuthListener l) {
      authListeners.add(l);
   }
   
   public void removeAuthenticationListener(AuthListener l) {
      authListeners.remove(l);
   }
   
   protected void notifyBACPerformed(SecureMessagingWrapper wrapper) {
      Iterator it = authListeners.iterator();
      while (it.hasNext()) {
          AuthListener listener = (AuthListener)it.next();
          listener.performedBAC(wrapper);
      }
  }

   /**
    * Performs the <i>Active Authentication</i> protocol.
    * 
    * @param pubkey the public key to use (usually read from the card)
    * 
    * @return a boolean indicating whether the card was authenticated
    * 
    * @throws GeneralSecurityException if something goes wrong
    */
   public boolean doAA(PublicKey pubkey) throws GeneralSecurityException {
      aaCipher.init(Cipher.ENCRYPT_MODE, pubkey);
      aaSignature.initVerify(pubkey);
      byte[] m2 = new byte[8]; /* random rndIFD */
      byte[] response = service.sendInternalAuthenticate(wrapper, m2);
      int digestLength = aaDigest.getDigestLength(); /* should always be 20 */
      byte[] m1 = Util.getAARecoveredMessage(digestLength, aaCipher.doFinal(response));
      aaSignature.update(m1);
      aaSignature.update(m2);
      boolean success = aaSignature.verify(response);
      notifyAAPerformed(pubkey, success);
      if (success) {
         state = AA_AUTHENTICATED_STATE;
      }
      return success;
   }
   
   protected void notifyAAPerformed(PublicKey pubkey, boolean success) {
      Iterator it = authListeners.iterator();
      while (it.hasNext()) {
          AuthListener listener = (AuthListener)it.next();
          listener.performedAA(pubkey, success);
      }
  }
   
   public byte[] sendAPDU(Apdu capdu) {
      return service.sendAPDU(capdu);
   }

   public void close() {
      try {
         wrapper = null;
         service.close();
      } finally {
         state = SESSION_STOPPED_STATE;
      }
   }

   public void addAPDUListener(APDUListener l) {
      service.addAPDUListener(l);
   }

   public void removeAPDUListener(APDUListener l) {
      service.removeAPDUListener(l);
   }
   
   public SecureMessagingWrapper getWrapper() {
      return wrapper;
   }
   
   /**
    * @deprecated hack
    * @param wrapper wrapper
    */
   public void setWrapper(SecureMessagingWrapper wrapper) {
      this.wrapper = wrapper;
      notifyBACPerformed(wrapper);
   }
}
