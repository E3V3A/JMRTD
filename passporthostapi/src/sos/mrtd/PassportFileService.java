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
 * $Id$
 */

package sos.mrtd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import sos.smartcards.APDUListener;
import sos.smartcards.Apdu;
import sos.smartcards.CardService;

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
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
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

   private PassportApduService service;
   private PassportAuthService authService;

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
         PassportFileService copy = (PassportFileService)service;
         this.service = copy.service;
         this.files = (copy.files != null) ? copy.files : new HashMap();
      } else if (service instanceof PassportApduService) {
         this.service = (PassportApduService)service;
         files = new HashMap();
      } else {
         this.service = new PassportApduService(service);
         files = new HashMap();
      }
      authService = new PassportAuthService(service);
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
      authService.setWrapper(wrapper);
   }

   /**
    * Opens a session. This is done by connecting to the card, selecting the
    * passport applet.
    */
   public void open() {
      service.open();
      files = new HashMap();
   }
   
   public String[] getTerminals() {
      return service.getTerminals();
   }

   public void open(String id) {
      service.open(id);
      files = new HashMap();
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
      authService.doBAC(docNr, dateOfBirth, dateOfExpiry);
   }
   
   public boolean doAA(PublicKey pubkey) throws GeneralSecurityException {
      return authService.doAA(pubkey);
   }
   
   public byte[] sendAPDU(Apdu capdu) {
      return service.sendAPDU(capdu);
   }

   public void close() {
      files = null;
      authService.close();
      // service.close();
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
      SecureMessagingWrapper wrapper = getWrapper();
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
   
   public SecureMessagingWrapper getWrapper() {
      return authService.getWrapper();
   }
   
   /**
    * @deprecated hack
    * @param wrapper
    */
   public void setWrapper(SecureMessagingWrapper wrapper) {
      authService.setWrapper(wrapper);
   }
}
