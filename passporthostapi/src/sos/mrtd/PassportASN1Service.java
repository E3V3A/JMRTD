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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import sos.smartcards.APDUListener;
import sos.smartcards.Apdu;
import sos.smartcards.BERTLVObject;
import sos.smartcards.CardService;

/**
 * DER TLV level card fileService for using the passport.
 * Defines access to the information on the passport
 * through selection of tags.
 * 
 * Based on ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt;
 *       doBAC(...) ==&gt; readObject(...) ==&gt; close()
 *    </pre> 
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class PassportASN1Service extends PassportAuthService
{
   public static final int EF_COM_TAG = 0x60;
   public static final int EF_DG1_TAG = 0x61;
   public static final int EF_DG2_TAG = 0x75;
   public static final int EF_DG3_TAG = 0x63;
   public static final int EF_DG4_TAG = 0x76;
   public static final int EF_DG5_TAG = 0x65;
   public static final int EF_DG6_TAG = 0x66;
   public static final int EF_DG7_TAG = 0x67;
   public static final int EF_DG8_TAG = 0x68;
   public static final int EF_DG9_TAG = 0x69;
   public static final int EF_DG10_TAG = 0x6A;
   public static final int EF_DG11_TAG = 0x6B;
   public static final int EF_DG12_TAG = 0x6C;
   public static final int EF_DG13_TAG = 0x6D;
   public static final int EF_DG14_TAG = 0x6E;
   public static final int EF_DG15_TAG = 0x6F;
   public static final int EF_DG16_TAG = 0x70;
   public static final int EF_SOD_TAG = 0x77;

   private PassportFileService fileService;
   
   /**
    * Creates a new passport fileService for accessing the passport.
    * 
    * @param service another service which will deal with sending
    *        the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public PassportASN1Service(CardService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      super(service);
      this.fileService = new PassportFileService(service);
      addAuthenticationListener(fileService);
   }

   /**
    * Reads the contents of object indicated by tags in <code>tagPath</code>.
    * First component of the tag path is the file tag (one of
    * <code>EF_COM_TAG</code> - <code>EF_SOD_TAG</code>). Last component of
    * the tag path indicates the object who's content is returned.
    * 
    * @param tagPath sequence of tags to search for
    * 
    * @return contents of object
    * 
    * @throws IOException when something goes wrong.
    */
   public byte[] readObject(int[] tagPath) throws IOException {
      if (tagPath == null || tagPath.length < 1) {
         throw new IllegalArgumentException("Tag path too short");
      }
      byte[] file = fileService.readFile(lookupFIDByTag(tagPath[0]));
      BERTLVObject object =
         BERTLVObject.getInstance(new ByteArrayInputStream(file));
      object = object.getSubObject(tagPath, 0, tagPath.length);
      return object.getValueAsBytes();
   }
 
   /**
    * Corresponds to Table A1 in ICAO-TR-LDS_1.7_2004-05-18.
    *
    * @param tag the first byte of the EF.
    *
    * @return the file identifier.
    */
   static short lookupFIDByTag(int tag) {
      switch(tag) {
         case EF_COM_TAG: return PassportFileService.EF_COM;
         case EF_DG1_TAG: return PassportFileService.EF_DG1;
         case EF_DG2_TAG: return PassportFileService.EF_DG2;
         case EF_DG3_TAG: return PassportFileService.EF_DG3;
         case EF_DG4_TAG: return PassportFileService.EF_DG4;
         case EF_DG5_TAG: return PassportFileService.EF_DG5;
         case EF_DG6_TAG: return PassportFileService.EF_DG6;
         case EF_DG7_TAG: return PassportFileService.EF_DG7;
         case EF_DG8_TAG: return PassportFileService.EF_DG8;
         case EF_DG9_TAG: return PassportFileService.EF_DG9;
         case EF_DG10_TAG: return PassportFileService.EF_DG10;
         case EF_DG11_TAG: return PassportFileService.EF_DG11;
         case EF_DG12_TAG: return PassportFileService.EF_DG12;
         case EF_DG13_TAG: return PassportFileService.EF_DG13;
         case EF_DG14_TAG: return PassportFileService.EF_DG14;
         case EF_DG15_TAG: return PassportFileService.EF_DG15;
         case EF_DG16_TAG: return PassportFileService.EF_DG16;
         case EF_SOD_TAG: return PassportFileService.EF_SOD;
         default:
            throw new NumberFormatException("Unknown tag "
                                            + Integer.toHexString(tag));
      }
   }
}

