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
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERApplicationSpecific;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.icao.DataGroupHash;
import org.bouncycastle.asn1.icao.LDSSecurityObject;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.jce.provider.X509CertificateObject;

import sos.smartcards.CardServiceException;
import sos.tlv.BERTLVInputStream;
import sos.util.Hex;

/**
 * File structure for the EF_SOD file.
 * This file contains the security object.
 * 
 * TODO: implement this without coupling the public interface of
 * this class to the Bouncy Castle classes.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class SODFile extends PassportFile
{
   private static final String SIGNED_DATA_OID = "1.2.840.113549.1.7.2";
   private static final String SHA1_HASH_ALG_OID = "1.3.14.3.2.26";
   private static final String SHA1_WITH_RSA_ENC_OID = "1.2.840.113549.1.1.5";
   private static final String SHA256_HASH_ALG_OID = "2.16.840.1.101.3.4.2.1";
   private static final String ICAO_SOD_OID = "2.23.136.1.1.1";
   private static final String E_CONTENT_TYPE_OID = "1.2.528.1.1006.1.20.1";

   private SignedData signedData;

   public SODFile(SignedData signedData) {
      this.signedData = signedData;
      System.out.println("Here?");
   }

   public SODFile(InputStream in) throws CardServiceException {
      try {
         BERTLVInputStream tlvIn = new BERTLVInputStream(in);
         tlvIn.readTag();
         tlvIn.readLength();
         ASN1InputStream asn1in =
            new ASN1InputStream(in);
         DERSequence seq = (DERSequence)asn1in.readObject();
         //To test DER spitting:
         //System.out.println("Test\n"+Util.printDERObject(seq));
         DERObjectIdentifier objectIdentifier = (DERObjectIdentifier)seq.getObjectAt(0);
         DERSequence s2 = (DERSequence)((DERTaggedObject)seq.getObjectAt(1)).getObject();
         this.signedData = new SignedData(s2);
      } catch (IOException e) {
         throw new CardServiceException(e.toString());
      }
   }

   /**
    * The tag of this file.
    * 
    * @return the tag
    */
   public int getTag() {
      return EF_SOD_TAG;
   }

   /**
    * FIXME: needed for output.
    */
   public byte[] getEncoded() {
      if (isSourceConsistent) {
         return sourceObject.getEncoded();
      }
      return null;
   }

   /*
      try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ASN1OutputStream asn1out = new ASN1OutputStream(out);
      asn1out.writeObject(new DERObjectIdentifier(PKCS7_SIGNED_DATA_OBJ_ID));


      DERSequence s2 = (DERSequence)DERSequence.getInstance(signedData);

      asn1out.writeObject(s2);
      asn1out.close();
      return out.toByteArray();
      } catch (IOException ioe) {

         System.err.println("DEBUG: ");
         ioe.printStackTrace();
         throw new IllegalStateException(ioe.toString());
      }
    */


   /*
      ASN1InputStream asn1in = new ASN1InputStream(in);
      DERSequence seq = (DERSequence)asn1in.readObject();
      DERObjectIdentifier objId = (DERObjectIdentifier)seq.getObjectAt(0);
      DERSequence s2 = (DERSequence)((DERTaggedObject)seq.getObjectAt(1)).getObject();
      signedData = new SignedData(s2);
    */

   private SignerInfo getSignerInfo()  {
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
   private LDSSecurityObject getSecurityObject() throws IOException, Exception {
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

   public DataGroupHash[] getDataGroupHashes() throws Exception {
       return getSecurityObject().getDatagroupHash();
      
   }

   public String getDigestAlgorithmSpec() throws Exception {
       return getHashAlgSpec(getSecurityObject().getDigestAlgorithmIdentifier().getObjectId());      
   }
   
   /**
    * Gets the document signing certificate.
    * Use this certificate to verify that
    * <i>eSignature</i> is a valid signature for
    * <i>eContent</i>. This certificate itself is
    * signed using the country signing certificate.
    * 
    * @see #getEContent()
    * @see #getSignature()
    *
    * @return the document signing certificate
    */
   public Certificate getDocSigningCertificate()
   throws IOException, CertificateException {
      byte[] certSpec = null;
      ASN1Set certs = signedData.getCertificates();
      if (certs.size() != 1) {
         System.out.println("DEBUG: WARNING: found "
               + certs.size() + " certificates");
      }
      for (int i = 0; i < certs.size(); i++) {
         X509CertificateStructure e =
            new X509CertificateStructure((DERSequence)certs.getObjectAt(i));
         certSpec = new X509CertificateObject(e).getEncoded();
      }

      /* NOTE: we could have just returned that X509CertificateObject here,
       * but by reconstructing it we hide the fact that we're using BC here.
       */
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      Certificate cert = factory.generateCertificate(new ByteArrayInputStream(certSpec));
      return cert;
   }

   /**
    * Gets the contents of the security object over which the
    * signature is to be computed. 
    * 
    * See RFC 3369, Cryptographic Message Syntax, August 2002,
    * Section 5.4 for details.
    *
    * @see #getDocSigningCertificate()
    * @see #getSignature()
    * 
    * @return the contents of the security object over which the
    *         signature is to be computed
    */
   private byte[] getEContent() throws IOException {
      SignerInfo signerInfo = getSignerInfo();
      ASN1Set signedAttributes = signerInfo.getAuthenticatedAttributes();

      if (signedAttributes.size() == 0) {
         /* Signed attributes absent, digest the contents... */
         ContentInfo contentInfo = signedData.getEncapContentInfo();
         return ((DEROctetString)contentInfo.getContent()).getOctets();
      } else {
         /* Signed attributes present, digest the attributes... */
         /* This option is taken by ICAO passports. */
         return signedAttributes.getDEREncoded();
      }
   }

   /**
    * Gets the contents of the security object over which the
    * signature is to be computed. 
    * 
    * See RFC 3369, Cryptographic Message Syntax, August 2002,
    * Section 5.4 for details.
    *
    * @see #getDocSigningCertificate()
    * @see #getSignature()
    * 
    * @return the contents of the security object over which the
    *         signature is to be computed
    */
   public byte[] getSignedAttributes() {
      SignerInfo signerInfo = getSignerInfo();
      ASN1Set signedAttributes = signerInfo.getAuthenticatedAttributes();
      return signedAttributes.getDEREncoded();
   }

   /**
    * Gets the stored signature of the security object.
    * 
    * @see #getDocSigningCertificate()
    * 
    * @return the signature
    * @throws IOException when something goes wrong
    */
   public byte[] getSignature() throws IOException {
      SignerInfo signerInfo = getSignerInfo();
      return signerInfo.getEncryptedDigest().getOctets();
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
    * 
    * @deprecated Leave this responsibility to client
    */
   public boolean checkDocSignature(Certificate docSigningCert)
   throws GeneralSecurityException, IOException {
      String sigAlg = "SHA256"; 
            
      byte[] eContent = getEContent();      
      byte[] signature = getSignature();
      
      // 1. Try whatever the certificate says, if anything
      if (docSigningCert instanceof X509Certificate) {
         sigAlg = ((X509Certificate)docSigningCert).getSigAlgName();
      }
      Signature sig = Signature.getInstance(sigAlg);
      sig.initVerify(docSigningCert);
      sig.update(eContent);
      boolean result = false;
      try {
          result = sig.verify(signature);
          if(result) {
              return result;
          }
      }catch(Exception e) {
          
      }
      // 2. Do it manually, decrypt the signature and extract the hashing algorithm

      try {
          Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
          c.init(Cipher.DECRYPT_MODE, docSigningCert);
          c.update(signature);
          byte[] decryptedBytes = c.doFinal();
          String id = getHashId(decryptedBytes);
          byte[] expectedHash = getHashBytes(decryptedBytes);
          AlgorithmIdentifier aId = AlgorithmIdentifier.getInstance(id);
          MessageDigest digest = MessageDigest.getInstance(getHashAlgSpec(aId.getObjectId()));
          digest.update(eContent);
          byte[] digestBytes = digest.digest();
          if(digestBytes.length != expectedHash.length) {
              result = false;
          }else{
              result = true;
              for(int i=0; i<digestBytes.length; i++) {
                  if(digestBytes[i] != expectedHash[i]) {
                      result = false;
                      break;
                  }
              }
          }
      }catch(Exception e) {
          
      }
      if(result) {
          // At this point it also means that the certificate alg. is not consistent with reality
          return result;
      }
      // 3. Finally, simply try SHA1withRSA

      sigAlg = "SHA1withRSA";
      sig = Signature.getInstance(sigAlg);
      sig.initVerify(docSigningCert);
      sig.update(eContent);
      try {
          result = sig.verify(signature);
      }catch(Exception e) {
            
      }      
      
      return result;
   }
   
   private static String getHashId(byte[] derBytes) throws IOException {
       ASN1InputStream asn1in = new ASN1InputStream(derBytes);
       DERSequence seq = (DERSequence)asn1in.readObject();
       return ((DERObjectIdentifier)((DERSequence)seq.getObjectAt(0)).getObjectAt(0)).getId();       
   }

   private static byte[] getHashBytes(byte[] derBytes) throws IOException {
       ASN1InputStream asn1in = new ASN1InputStream(derBytes);
       DERSequence seq = (DERSequence)asn1in.readObject();
       return ((DEROctetString)seq.getObjectAt(1)).getOctets();       
   }

   private static String getHashAlgSpec(DERObjectIdentifier oid) {
       if(oid.equals(X509ObjectIdentifiers.id_SHA1)) {
           return "SHA1"; 
       }
       if(oid.equals(NISTObjectIdentifiers.id_sha224)) {
           return "SHA224"; 
       }
       if(oid.equals(NISTObjectIdentifiers.id_sha256)) {
           return "SHA256"; 
       }
       if(oid.equals(NISTObjectIdentifiers.id_sha384)) {
           return "SHA384"; 
       }
       if(oid.equals(NISTObjectIdentifiers.id_sha512)) {
           return "SHA512"; 
       }
       return "SHA1";
   }
}

