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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.icao.LDSSecurityObject;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.jce.provider.X509CertificateObject;

import sos.smartcards.CardService;

/**
 * High level card passport service for using the passport.
 * Defines high-level commands to access the information on the passport.
 * Based on ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt;
 *       doBAC(...) ==&gt; readBlah() ==&gt; close()
 *    </pre> 
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class PassportService extends PassportAuthService
{
   private PassportASN1Service passportASN1Service;
   private KeyFactory keyFactory;

   /**
    * Creates a new passport passportASN1Service for accessing the passport.
    * 
    * @param service another passportASN1Service which will deal
    *        with sending the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public PassportService(CardService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      super(service);
      if (service instanceof PassportService) {
         this.passportASN1Service =
            ((PassportService)service).passportASN1Service;
      } else {
         this.passportASN1Service = new PassportASN1Service(service);
      }
      addAuthenticationListener(passportASN1Service);
      keyFactory = KeyFactory.getInstance("RSA");
   }

   public COMFile getCOMFile() throws IOException {
      return (COMFile)getFile(PassportFile.EF_COM_TAG);
   }

   /*
    * TODO: Temporary method. Probably nicer to have getDG1File(), getDG2File(),
    * etc.
    * 
    * @param tag should be a valid ICAO file tag
    * @return the data group file
    * @throws IOException if file cannot be read
    */
   public DataGroup getDataGroup(int tag) throws IOException {
      return (DataGroup)getFile(tag);
   }

   public SODFile getSODFile() throws IOException {
      return (SODFile)getFile(PassportFile.EF_SOD_TAG);
   }

   private PassportFile getFile(int tag) throws IOException {
      return PassportFile.getInstance(passportASN1Service.readFile(tag));
   }
}
