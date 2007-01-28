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
      return readObjectAsObject(tagPath).getValueAsBytes();
   }
   
   BERTLVObject readObjectAsObject(int[] tagPath) throws IOException {
      if (tagPath == null || tagPath.length < 1) {
         throw new IllegalArgumentException("Tag path too short");
      }
      byte[] file = fileService.readFile(PassportFile.lookupFIDByTag(tagPath[0]));
      BERTLVObject object =
         BERTLVObject.getInstance(new ByteArrayInputStream(file));
      object = object.getSubObject(tagPath, 0, tagPath.length);
      return object;
   }
}

