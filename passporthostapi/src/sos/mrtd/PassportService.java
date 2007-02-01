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
         int[] path = { tag };
		 return
		    PassportFile.getInstance(passportASN1Service.readObject(path));
	   }

	   
   /**
    * Lists the data groups present on this passport.
    * 
    * @return a list of file identifiers
    * 
    * @throws IOException if something goes wrong
    */
   public short[] readDataGroupList() throws IOException {
	   /*
      int[] tags = { PassportFile.EF_COM_TAG, 0x5C };
      byte[] tagList = passportASN1Service.readObject(tags);
      */
	  COMFile comfile = getCOMFile();
	  byte[] tagList = comfile.getTagList();
      short[] files = new short[tagList.length];
      for (int i = 0; i < files.length; i++) {
         files[i] = PassportFile.lookupFIDByTag(tagList[i]);
      }
      return files;
	   
   }
   

   
   /**
    * Reads the MRZ found in DG1 on the passport.
    * 
    * @return the MRZ
    * 
    * @throws IOException if something goes wrong
    */
   public MRZInfo readMRZ() throws IOException {
	   /*
      int[] tags = { PassportFile.EF_DG1_TAG, 0x5F1F };
      return new MRZInfo(new ByteArrayInputStream(passportASN1Service.readObject(tags)));
      */
	   DG1File dg1 = (DG1File)getDataGroup(PassportFile.EF_DG1_TAG);
	   return dg1.getMRZInfo();
   }
   
   /**
    * Reads the face of the passport holder.
    * 
    * @return the faces found in DG2 on the passport
    * 
    * @throws IOException if something goes wrong
    */
   public FaceInfo[] readFace() throws IOException {
	   /*
      int[] tags = { PassportFile.EF_DG2_TAG, 0x5F2E }; 
      byte[] facialRecordData = passportASN1Service.readObject(tags);
      if (facialRecordData == null) {
         System.out.println("DEBUG: facialRecordData == null");
      }
      DataInputStream in =
         new DataInputStream(new ByteArrayInputStream(facialRecordData));
/*

      /* Facial Record Header (14) */
	   /*
      in.skip(4); // 'F', 'A', 'C', 0
      in.skip(4); // version in ascii (e.g. "010")
      long length = in.readInt() & 0x000000FFFFFFFFL;
      int faceCount = in.readUnsignedShort();

      FaceInfo[] result = new FaceInfo[faceCount];
      for (int i = 0; i < faceCount; i++) {
         result[i] = new FaceInfo(in);
      }
      return result;
      */
	   DG2File dg2 = (DG2File)getDataGroup(PassportFile.EF_DG2_TAG);
	   List<FaceInfo> faces = dg2.getFaces();
	   FaceInfo[] result = new FaceInfo[faces.size()];
	   faces.toArray(result);
	   return result;
   }

   /**
    * Reads the <i>Active Authentication</i> public key found in
    * DG15 on the passport.
    *
    * @return the public key to be used for AA
    */
   public PublicKey readAAPublicKey() throws IOException, GeneralSecurityException {
      int[] tags = { PassportFile.EF_DG15_TAG };
      X509EncodedKeySpec pubKeySpec =
         new X509EncodedKeySpec(passportASN1Service.readObject(tags));
      return keyFactory.generatePublic(pubKeySpec);
   }
     
   private SignedData readSignedData() throws IOException {
      int[] tags = { PassportFile.EF_SOD_TAG };
      byte[] sd = passportASN1Service.readObject(tags);
      ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(sd));
      DERSequence seq = (DERSequence)in.readObject();
      // DERObjectIdentifier objId = (DERObjectIdentifier)seq.getObjectAt(0);
      DERSequence s2 = (DERSequence)((DERTaggedObject)seq.getObjectAt(1)).getObject();
      SignedData signedData = new SignedData(s2);
      Object nextObject = in.readObject();
      if (nextObject != null) {
         System.out.println("DEBUG: WARNING: extra object found after SignedData...");
      }
      return signedData;
   }
   
   private SignerInfo readSignerInfo() throws IOException {
      SignedData signedData = readSignedData();
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
   public LDSSecurityObject readSecurityObject() throws IOException, Exception {
      SignedData signedData = readSignedData();
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
   public Certificate readDocSigningCertificate()
   throws IOException, Exception {
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
    * @throws IOException when something goes wrong
    */
   private byte[] readEncryptedDigest() throws IOException {
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
   private byte[] readSecurityObjectContent() throws IOException {
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
      sig.update(readSecurityObjectContent());
      return sig.verify(readEncryptedDigest());
   }
}
