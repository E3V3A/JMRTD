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
 * $Id: $
 */

package sos.mrtd;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import sos.smartcards.BERTLVObject;

/**
 * File structure for the EF_DG15 file.
 * Datagroup 15 contains the public key used in AA.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class DG15File extends PassportFile
{
   private PublicKey publicKey;

   /**
    * Constructs a new file.
    * 
    * @param publicKey the key to store in this file
    */
   public DG15File(PublicKey publicKey) {
      this.publicKey = publicKey;
   }
   
   DG15File(InputStream in) throws IOException {
      this(BERTLVObject.getInstance(in));
   }
   
   DG15File(BERTLVObject object) {   
      try {
         X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(object.getValueAsBytes());
         KeyFactory keyFactory = KeyFactory.getInstance("RSA");
         publicKey = keyFactory.generatePublic(pubKeySpec);
      } catch (Exception e) {
         throw new IllegalArgumentException(e.toString());
      }
   }
   
   public byte[] getEncoded() {
      try {
         BERTLVObject ef010F =
            new BERTLVObject(PassportFile.EF_DG15_TAG,
                  publicKey.getEncoded());
         return ef010F.getEncoded();
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }

   /**
    * Gets the public key stored in this file.
    * 
    * @return the public key
    */
   public PublicKey getPublicKey() {
      return publicKey;
   }
}
