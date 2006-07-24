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
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObjectIdentifier;
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
 * High level card passportASN1Service for using the passport.
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
   private CertificateFactory certFactory;
   
   /**
    * Creates a new passport passportASN1Service for accessing the passport.
    * 
    * @param passportASN1Service another passportASN1Service which will deal with sending
    *        the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public PassportService(CardService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      super(service);
      if (service instanceof PassportService) {
         this.passportASN1Service = ((PassportService)service).passportASN1Service;
      } else {
         this.passportASN1Service = new PassportASN1Service(service);
      }
      addAuthenticationListener(passportASN1Service);
      keyFactory = KeyFactory.getInstance("RSA");
      certFactory = CertificateFactory.getInstance("X.509");
   }

   /**
    * Lists the data groups present on this passport.
    * 
    * @return a list of file identifiers
    * 
    * @throws IOException if something goes wrong
    */
   public short[] readDataGroupList() throws IOException {
      int[] tags = { PassportASN1Service.EF_COM_TAG, 0x5C };
      byte[] tagList = passportASN1Service.readObject(tags);
      short[] files = new short[tagList.length];
      for (int i = 0; i < files.length; i++) {
         files[i] = PassportASN1Service.lookupFIDByTag(tagList[i]);
      }
      return files;
   }

   /**
    * Reads the face of the passport holder.
    * 
    * @return the first face found on the passport
    * 
    * @throws IOException if something goes wrong
    */
   public FaceInfo[] readFace() throws IOException {
      int[] tags = { PassportASN1Service.EF_DG2_TAG, 0x5F2E }; 
      byte[] facialRecordData = passportASN1Service.readObject(tags);
      if (facialRecordData == null) {
         System.out.println("DEBUG: facialRecordData == null");
      }
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(facialRecordData));

      /* Facial Record Header (14) */
      in.skip(4); // 'F', 'A', 'C', 0
      in.skip(4); // version in ascii (e.g. "010")
      long length = in.readInt() & 0x000000FFFFFFFFL;
      int faceCount = in.readUnsignedShort();

      FaceInfo[] result = new FaceInfo[faceCount];
      for (int i = 0; i < faceCount; i++) {
         result[i] = new FaceInfo(in);
      }
      return result;
   }

   public MRZInfo readMRZ() throws IOException {
      int[] tags = { PassportASN1Service.EF_DG1_TAG, 0x5F1F };
      return new MRZInfo(new ByteArrayInputStream(passportASN1Service.readObject(tags)));
   }

   /**
    * Reads the <i>Active Authentication</i> public key from the passport.
    *
    * @return the public key to be used for AA
    */
   public PublicKey readAAPublicKey() throws IOException, GeneralSecurityException {
      int[] tags = { PassportASN1Service.EF_DG15_TAG };
      X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(passportASN1Service.readObject(tags));
      return keyFactory.generatePublic(pubKeySpec);
   }
     
   private SignedData readSignedData() throws IOException, Exception {
	   int[] tags = { PassportASN1Service.EF_SOD_TAG };
	   byte[] sd = passportASN1Service.readObject(tags);
       ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(sd));
       DERSequence seq = (DERSequence)in.readObject();
       DERObjectIdentifier objId = (DERObjectIdentifier)seq.getObjectAt(0);
       DERSequence s2 = (DERSequence)((DERTaggedObject)seq.getObjectAt(1)).getObject();
	   SignedData signedData = new SignedData(s2);
       Object nextObject = in.readObject();
       if (nextObject != null) {
          System.out.println("DEBUG: WARNING: extra object found after SignedData...");
       }
	   return signedData;
   }
   
   private SignerInfo readSignerInfo() throws Exception {
      SignedData signedData = readSignedData();
      ASN1Set signerInfos = signedData.getSignerInfos();
      if (signerInfos.size() > 1) {
         System.out.println("DEBUG: WARNING: found " + signerInfos.size() + " signerInfos");
      }
      for (int i = 0; i < signerInfos.size(); i++) {
         SignerInfo info = new SignerInfo((DERSequence) signerInfos
               .getObjectAt(i));
         return info;
      }
      return null;
   }
   
   /**
    * Reads the security object.
    * 
    * @return the security object
    * 
    * @throws IOException
    */
   public LDSSecurityObject readSecurityObject() throws IOException, Exception {
      SignedData signedData = readSignedData();
      ContentInfo contentInfo = signedData.getEncapContentInfo();
      byte[] content = ((DEROctetString)contentInfo.getContent()).getOctets();
      ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(content)); 
      LDSSecurityObject sod = new LDSSecurityObject((DERSequence)in.readObject());
      Object nextObject = in.readObject();
      if (nextObject != null) {
         System.out.println("DEBUG: WARNING: extra object found after LDSSecurityObject...");
      }
      return sod;
   }
   
   /**
    * Reads the document signing certificate from the passport.
    *
    * @return the document signing certificate
    */
   public Certificate readDocSigningCertificate() throws IOException, Exception {
      X509Certificate cert = null;
      SignedData signedData = readSignedData();
      ASN1Set certs = signedData.getCertificates();
      for (int i = 0; i < certs.size(); i++) {
         X509CertificateStructure e =
            new X509CertificateStructure((DERSequence)certs.getObjectAt(i));
          cert = new X509CertificateObject(e);
      }
      return cert;
   }
  
   /**
    * Reads the stored signature of the security object.
    * 
    * @return the signature
    * @throws Exception when something goes wrong
    */
   public byte[] readEncryptedDigest() throws Exception {
      SignerInfo signerInfo = readSignerInfo();
      return signerInfo.getEncryptedDigest().getOctets();
   }
   
   /**
    * Reads the contents of the security object over which the
    * signature is to be computed.
    * 
    * See RFC 3369, Cryptographic Message Syntax, August 2002,
    * Section 5.4 for details.
    * 
    * @return the contents of the security object over which the
    *         signature is to be computed
    * @throws Exception when something is wrong
    */
   public byte[] readSecurityObjectContent() throws Exception {
      SignerInfo signerInfo = readSignerInfo();
      ASN1Set signedAttributes = signerInfo.getAuthenticatedAttributes();
      if (signedAttributes.size() == 0) {
         /* Signed attributes absent, digest the contents... */
         SignedData signedData = readSignedData();
         ContentInfo contentInfo = signedData.getEncapContentInfo();
         return ((DEROctetString)contentInfo.getContent()).getOctets();
      } else {
         /* Signed attributes present, digest the attributes... */
         return signedAttributes.getDEREncoded();
      }
   }
}
