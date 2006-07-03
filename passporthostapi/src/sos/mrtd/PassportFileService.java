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
 * $Id: PassportFileService.java,v 1.3 2006/06/15 00:34:09 martijno Exp $
 */

package sos.mrtd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import sos.smartcards.APDUListener;
import sos.smartcards.Apdu;
import sos.smartcards.CardService;


/**
 * Card service for using the filesystem on the passport.
 * Defines basic access control, active authentication,
 * and reading of complete files.
 * 
 * Based on ICAO-TR-PKI and ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt;
 *       doBAC(...) ==&gt; readFile(fid) ==&gt; close()
 *    </pre> 
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 1.3 $
 */
public class PassportFileService implements CardService
{
   /** Data group 1 contains the MRZ. */
   public static final short EF_DG1 = 0x0101;
   /** Data group 2 contains face image data. */
   public static final short EF_DG2 = 0x0102;
   /** Data group 3 contains finger print data. */
   public static final short EF_DG3 = 0x0103;
   /** Data group 4 contains iris data. */
   public static final short EF_DG4 = 0x0104;
   /** Data group 5 contains displayed portrait. */
   public static final short EF_DG5 = 0x0105;
   /** Data group 6 is RFU. */
   public static final short EF_DG6 = 0x0106;
   /** Data group 7 contains displayed signature. */
   public static final short EF_DG7 = 0x0107;
   /** Data group 8 contains data features. */
   public static final short EF_DG8 = 0x0108;
   /** Data group 9 contains structure features. */
   public static final short EF_DG9 = 0x0109;
   /** Data group 10 contains substance features. */
   public static final short EF_DG10 = 0x010A;
   /** Data group 11 contains additional personal details. */
   public static final short EF_DG11 = 0x010B;
   /** Data group 12 contains additional document details. */
   public static final short EF_DG12 = 0x010C;
   /** Data group 13 contains optional details. */
   public static final short EF_DG13 = 0x010D;
   /** Data group 14 is RFU. */
   public static final short EF_DG14 = 0x010E;
   /** Data group 15 contains the public key used for Active Authentication. */
   public static final short EF_DG15 = 0x010F;
   /** Data group 16 contains person(s) to notify. */
   public static final short EF_DG16 = 0x0110;
   /** The security document. */
   public static final short EF_SOD = 0x011D;
   /** File indicating which data groups are present. */
   public static final short EF_COM = 0x011E;

   private static final int SESSION_STOPPED_STATE = 0;
   private static final int SESSION_STARTED_STATE = 1;
   private static final int AUTHENTICATED_STATE = 2;
   private int state;

   private PassportApduService service;
   private SecretKey kEnc, kMac;
   private SecureMessagingWrapper wrapper;
   private Signature aaSignature;

   /** Files read during this session. */
   private Map files;

   /**
    * Creates a new passport service for accessing the passport.
    * 
    * @param service another service which will deal with sending
    *        the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public PassportFileService(CardService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      if (service instanceof PassportFileService) {
         this.service = ((PassportFileService)service).service;
         if (((PassportFileService)service).files != null) {
            files = ((PassportFileService)service).files;
         } else {
            files = new HashMap();
         }
      } else if (service instanceof PassportApduService) {
         this.service = (PassportApduService)service;
         files = new HashMap();
      } else {
         this.service = new PassportApduService(service);
         files = new HashMap();
      }
      aaSignature = Signature.getInstance("RSA");
      state = SESSION_STOPPED_STATE;
   }
   
   /**
    * Hack to construct a passport service from a service that is already open.
    * This should be removed some day.
    * 
    * @param service underlying service
    * @param wrapper encapsulates secure messaging state
    */
   public PassportFileService(CardService service, SecureMessagingWrapper wrapper)
   throws GeneralSecurityException, UnsupportedEncodingException {
      this(service);
      this.wrapper = wrapper;
      files = new HashMap();
      aaSignature = Signature.getInstance("RSA");
      state = AUTHENTICATED_STATE;
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
      files = new HashMap();
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
      files = new HashMap();
      state = SESSION_STARTED_STATE;
   }
   
   /**
    * Performs the Basic Access Control protocol.
    *
    * @param docNr the document number
    * @param dateOfBirth card holder's birth date
    * @param dateOfExpiry document's expiry date
    */
   public void doBAC(String docNr, String dateOfBirth, String dateOfExpiry)
         throws GeneralSecurityException, UnsupportedEncodingException {
      byte[] keySeed = Util.computeKeySeed(docNr, dateOfBirth, dateOfExpiry);
      kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
      kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
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
      state = AUTHENTICATED_STATE;
   }
   
   /**
    * FIXME: implement active authentication.
    * FIXME: maybe move to lower level service?
    */
   public boolean doAA(PublicKey pubkey) throws GeneralSecurityException {
      byte[] rndIFD = new byte[8]; /* Random */
      byte[] response = service.sendInternalAuthenticate(wrapper, rndIFD);
      aaSignature.initVerify(pubkey);
      return aaSignature.verify(response);
   }
   
   public byte[] sendAPDU(Apdu capdu) {
      return service.sendAPDU(capdu);
   }

   public void close() {
      try {
         wrapper = null;
         files = null;
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

   /**
    * Reads the file with id <code>fid</code>.
    *
    * @param fid the file to read.
    *
    * @return the contents of the file.
    */
   public byte[] readFile(short fid) throws IOException {
      /* Was this file read previously? */
      Short fidKey = new Short(fid);
      if (files.containsKey(fidKey)) {
         return (byte[])files.get(fidKey);
      }
      /* No? Read it from document... */
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      service.sendSelectFile(wrapper, fid);
      int offset = 0;
      while (true) {
         byte[] data = service.sendReadBinary(wrapper, (short)offset, 255);
         if (data.length == 0) {
            // TODO: also break out of loop if SW indicates EOF
            break;
         }
         out.write(data, 0, data.length);
         offset += data.length;
      }
      byte[] file = out.toByteArray();
      files.put(fidKey, file);
      return file;
   }

   void setSecureMessagingWrapper(SecureMessagingWrapper wrapper) {
      this.wrapper = wrapper;
   }
}
