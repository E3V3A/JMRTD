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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

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

import sos.smartcards.BERTLVObject;

/**
 * File structure for the EF_SOD file.
 * This file contains the security object.
 * 
 * TODO: implement this...
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class SODFile extends PassportFile
{
   private SignedData signedData;
	
   public SODFile(SignedData signedData) {
      this.signedData = signedData;
   }
   
   SODFile(InputStream in) throws IOException {
      this(BERTLVObject.getInstance(in));
   }
      
   SODFile(BERTLVObject object) throws IOException {
      byte[] sd = object.getEncoded();
      ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(sd));
      DERSequence seq = (DERSequence)in.readObject();
      // DERObjectIdentifier objId = (DERObjectIdentifier)seq.getObjectAt(0);
      DERSequence s2 = (DERSequence)((DERTaggedObject)seq.getObjectAt(1)).getObject();
      signedData = new SignedData(s2);
      Object nextObject = in.readObject();
      if (nextObject != null) {
         System.out.println("DEBUG: WARNING: extra object found after SignedData...");
      }
   }

   @Override
   public byte[] getEncoded() {
      // TODO Auto-generated method stub
      return null;
   }
   
   private SignerInfo getSignerInfo() throws IOException {
      ASN1Set signerInfos = signedData.getSignerInfos();
      if (signerInfos.size() > 1) {
         System.out.println("DEBUG: WARNING: found " + signerInfos.size() + " signerInfos");
      }
      for (int i = 0; i < signerInfos.size(); i++) {
         SignerInfo info = new SignerInfo((DERSequence)signerInfos.getObjectAt(i));
         return info;
      }
      return null;
   }
   
   /**
    * Reads the security object (containing the hashes
    * of the data groups) found in the SOd on the passport.
    * 
    * @return the security object
    * 
    * @throws IOException
    */
   public LDSSecurityObject getSecurityObject() throws IOException, Exception {
      ContentInfo contentInfo = signedData.getEncapContentInfo();
      byte[] content = ((DEROctetString)contentInfo.getContent()).getOctets();
      ASN1InputStream in =
         new ASN1InputStream(new ByteArrayInputStream(content)); 
      LDSSecurityObject sod =
         new LDSSecurityObject((DERSequence)in.readObject());
      Object nextObject = in.readObject();
      if (nextObject != null) {
         System.out.println("DEBUG: WARNING: extra object found after LDSSecurityObject...");
      }
      return sod;
   }
   
   /**
    * Reads the document signing certificate.
    *
    * @return the document signing certificate
    */
   public Certificate getDocSigningCertificate()
   throws IOException, Exception {
      X509Certificate cert = null;
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
    * @throws IOException when something goes wrong
    */
   private byte[] getEncryptedDigest() throws IOException {
      SignerInfo signerInfo = getSignerInfo();
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
   private byte[] getSecurityObjectContent() throws IOException {
      SignerInfo signerInfo = getSignerInfo();
      ASN1Set signedAttributes = signerInfo.getAuthenticatedAttributes();
      if (signedAttributes.size() == 0) {
         /* Signed attributes absent, digest the contents... */
         ContentInfo contentInfo = signedData.getEncapContentInfo();
         return ((DEROctetString)contentInfo.getContent()).getOctets();
      } else {
         /* Signed attributes present, digest the attributes... */
         return signedAttributes.getDEREncoded();
      }
   }
   /**
    * Verifies the signature over the contents of the security object.
    * 
    * See RFC 3369, Cryptographic Message Syntax, August 2002,
    * Section 5.4 for details.
    * 
    * @param docSigningCert the certificate to use
    *        (should be X509 certificate)
    * 
    * @return status of the verification
    * 
    * @throws GeneralSecurityException if something goes wrong
    * @throws IOException if something goes wrong
    */
   public boolean checkDocSignature(Certificate docSigningCert)
   throws GeneralSecurityException, IOException {
      String sigAlg = null;
      if (docSigningCert instanceof X509Certificate) {
         sigAlg = ((X509Certificate)docSigningCert).getSigAlgName();
      } else {
         sigAlg = "SHA256";
      }
      Signature sig = Signature.getInstance(sigAlg);
      sig.initVerify(docSigningCert);
      sig.update(getSecurityObjectContent());
      return sig.verify(getEncryptedDigest());
   }
}
