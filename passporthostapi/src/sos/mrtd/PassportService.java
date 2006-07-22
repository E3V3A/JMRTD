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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

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

import sos.smartcards.APDUListener;
import sos.smartcards.Apdu;
import sos.smartcards.CardService;
import sos.util.ASN1Utils;

/**
 * High level card service for using the passport.
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
public class PassportService implements CardService
{
   private PassportASN1Service service;
   private KeyFactory keyFactory;
   private CertificateFactory certFactory;
   
   /**
    * Creates a new passport service for accessing the passport.
    * 
    * @param service another service which will deal with sending
    *        the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public PassportService(CardService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      if (service instanceof PassportService) {
         this.service = ((PassportService)service).service;
      } else {
         this.service = new PassportASN1Service(service);
      }
      keyFactory = KeyFactory.getInstance("RSA");
      certFactory = CertificateFactory.getInstance("X.509");
   }
   
   /**
    * Hack to construct a passport service from a service that is already open.
    * This should be removed some day.
    * 
    * @param service underlying service
    * @param wrapper encapsulates secure messaging state
    */
   public PassportService(CardService service, SecureMessagingWrapper wrapper)
   throws GeneralSecurityException, UnsupportedEncodingException {
      this(service);
      this.service.setWrapper(wrapper);   }

   /**
    * Opens a session. This is done by connecting to the card, selecting the
    * passport applet.
    */
   public void open() {
      service.open();
   }
   
   public String[] getTerminals() {
      return service.getTerminals();
   }

   public void open(String id) {
      service.open(id);
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
      service.doBAC(docNr, dateOfBirth, dateOfExpiry);
   }
   
   public boolean doAA(PublicKey pubkey) throws GeneralSecurityException {
      return service.doAA(pubkey);
   }
   
   public byte[] sendAPDU(Apdu capdu) {
      return service.sendAPDU(capdu);
   }

   public void close() {
      service.close();
   }

   public void addAPDUListener(APDUListener l) {
      service.addAPDUListener(l);
   }

   public void removeAPDUListener(APDUListener l) {
      service.removeAPDUListener(l);
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
      byte[] tagList = service.readObject(tags);
      short[] files = new short[tagList.length];
      for (int i = 0; i < files.length; i++) {
         files[i] = PassportASN1Service.lookupFIDByTag(tagList[i]);
      }
      return files;
   }

   /**
    * Reads the type of document.
    * The ICAO documentation gives "P<" as an example.
    * 
    * @return a string of length 2 containing the document type
    * @throws IOException if something goes wrong
    */
   public String readDocumentType() throws IOException {
      byte[] fileData = readMRZ();
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
      byte[] result = new byte[2];
      in.readFully(result);
      return new String(result);
   }

   /**
    * Reads the issuing state.
    * 
    * @return a string of length 3 containing an abbreviation
    *         of the issuing state or organization
    *         
    * @throws IOException if something goes wrong
    */
   public String readIssuingState() throws IOException {
      byte[] fileData = readMRZ();
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
      in.skip(2);
      byte[] data = new byte[3];
      in.readFully(data);
      return new String(data);
   }

   /**
    * Reads the passport holder's name.
    * 
    * @return a string containing last name and first names seperated by spaces
    * 
    * @throws IOException is something goes wrong
    */
   public String readName() throws IOException {
      byte[] fileData = readMRZ();
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
      in.skip(2);
      in.skip(3);
      byte[] data = new byte[39]; // FIXME: check if we have ID3 type document (otherwise 30 or 31 instead of 39)
      in.readFully(data);
      for (int i = 0; i < data.length; i++) {
         if (data[i] == '<') {
            data[i] = ' ';
         }
      }
      String name = new String(data).trim();
      return name;
   }

   /**
    * Reads the document number.
    * 
    * @return the document number
    * 
    * @throws IOException if something goes wrong
    */
   public String readDocumentNumber() throws IOException {
      byte[] fileData = readMRZ();
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
      in.skip(2);
      in.skip(3);
      in.skip(39);
      byte[] data = new byte[9];
      in.readFully(data);
      return new String(data).trim();
   }

   /**
    * Reads the nationality of the passport holder.
    * 
    * @return a string of length 3 containing the nationality of the passport holder
    * @throws IOException if something goes wrong
    */
   public String readNationality() throws IOException {
      byte[] fileData = readMRZ();
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
      in.skip(2);
      in.skip(3);
      in.skip(39);
      in.skip(9);
      in.skip(1);
      byte[] data = new byte[3];
      in.readFully(data);
      return new String(data).trim();
   }

   /**
    * Reads the date of birth of the passport holder
    * 
    * @return the date of birth
    * 
    * @throws IOException if something goes wrong
    * @throws NumberFormatException if something goes wrong
    */
   public Date readDateOfBirth() throws IOException, NumberFormatException {
      byte[] fileData = readMRZ();
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
      in.skip(2);
      in.skip(3);
      in.skip(39);
      in.skip(9);
      in.skip(1);
      in.skip(3);
      byte[] data = new byte[6];
      in.readFully(data);
      String dateString = new String(data).trim();
      return makeDate(1900, dateString);
   }

   /**
    * Reads the date of expiry of this document
    * 
    * @return the date of expiry
    * 
    * @throws IOException if something goes wrong
    */
   public Date readDateOfExpiry() throws IOException {
      byte[] fileData = readMRZ();
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(fileData));
      in.skip(2);
      in.skip(3);
      in.skip(39);
      in.skip(9);
      in.skip(1);
      in.skip(3);
      in.skip(6);
      in.skip(1);
      in.skip(1);
      byte[] data = new byte[6];
      in.readFully(data);
      return makeDate(2000, new String(data).trim());
   }
   
   private Date makeDate(int baseYear, String dateString) throws NumberFormatException {
      if (dateString.length() != 6) {
         throw new NumberFormatException("Wrong date format!");
      }
      int year = baseYear + Integer.parseInt(dateString.substring(0, 2));
      int month = Integer.parseInt(dateString.substring(2, 4));
      int day = Integer.parseInt(dateString.substring(4, 6));
      GregorianCalendar cal = new GregorianCalendar(year, month - 1, day);
      return cal.getTime();
   }

   /**
    * Reads the face of the passport holder.
    * 
    * @return the first face found on the passport
    * 
    * @throws IOException if something goes wrong
    */
   public BufferedImage readFace() throws IOException {
      int[] tags = { PassportASN1Service.EF_DG2_TAG, 0x5F2E }; 
      byte[] facialRecordData = service.readObject(tags);
      if (facialRecordData == null) {
         System.out.println("DEBUG: facialRecordData == null");
      }
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(facialRecordData));

      /* Facial Record Header (14) */
      in.skip(4); // 'F', 'A', 'C', 0
      in.skip(4); // version in ascii (e.g. "010")
      long length = in.readInt() & 0x000000FFFFFFFFL;
      int faceCount = in.readUnsignedShort();

      for (int face = 0; face < faceCount; face++) {

         /* Facial Information (20) */
         long faceImageBlockLength = in.readInt() & 0x00000000FFFFFFFFL;
         int featurePointCount = in.readUnsignedShort();
         byte gender = in.readByte();
         byte eyeColor = in.readByte();
         byte hairColor = in.readByte();
         long featureMask = in.readUnsignedByte();
         featureMask = (featureMask << 16) | in.readUnsignedShort();
         short expression = in.readShort();
         long poseAngle = in.readUnsignedByte();
         poseAngle = (poseAngle << 16) | in.readUnsignedShort();
         long poseAngleUncertainty = in.readUnsignedByte();
         poseAngleUncertainty = (poseAngleUncertainty << 16) | in.readUnsignedShort();

         /* Feature Point(s) (optional) (8 * featurePointCount) */
         for (int i = 0; i < featurePointCount; i++) {
            byte featureType = in.readByte();
            byte featurePoint = in.readByte();
            int x = in.readUnsignedShort();
            int y = in.readUnsignedShort();
            in.skip(2); // 2 bytes reserved
         }

         /* Image Information */
         byte faceImageType = in.readByte();
         byte imageDataType = in.readByte();
         int width = in.readUnsignedShort();
         int height = in.readUnsignedShort();
         byte imageColorSpace = in.readByte();
         int sourceType = in.readUnsignedByte();
         int deviceType = in.readUnsignedShort();
         int quality = in.readUnsignedShort();

         /* Read JPEG2000 data */
         ImageInputStream iis = ImageIO.createImageInputStream(in);
         Iterator readers = ImageIO.getImageReadersByMIMEType("image/jpeg2000");
         if (!readers.hasNext()) {
            throw new IllegalStateException("No jpeg 2000 readers");
         }
         ImageReader reader = (ImageReader)readers.next();
         reader.setInput(iis);
         ImageReadParam pm = reader.getDefaultReadParam();
         pm.setSourceRegion(new Rectangle(0, 0, width, height));
         BufferedImage image = reader.read(0, pm);
         return image; // FIXME: return all images, instead of only the first one :)
      }
      return null;
   }

   private byte[] readMRZ() throws IOException {
      int[] tags = { PassportASN1Service.EF_DG1_TAG, 0x5F1F };
      return service.readObject(tags);
   }

   /**
    * Reads the <i>Active Authentication</i> public key from the passport.
    *
    * @return the public key to be used for AA
    */
   public PublicKey readAAPublicKey() throws IOException, GeneralSecurityException {
      int[] tags = { PassportASN1Service.EF_DG15_TAG };
      X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(service.readObject(tags));
      return keyFactory.generatePublic(pubKeySpec);
   }
     
   public KeyPair generateAAKeyPair() 
   throws GeneralSecurityException, NoSuchAlgorithmException {
       String preferredProvider = "BC";
       Provider provider = Security.getProvider(preferredProvider);
       if(provider == null) {
           return null;    
       }
       KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
       generator.initialize(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4)); 
       KeyPair keyPair = generator.generateKeyPair();
       return keyPair;
   }
      
   public static byte[] publicKey2DG15(PublicKey key) 
   throws IOException {
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       
       byte[] keyBytes = key.getEncoded();
       
       out.write(0x6f);
       out.write(ASN1Utils.lengthId(keyBytes.length));
       out.write(keyBytes);

       return out.toByteArray();
   }
   
   private SignedData readSignedData() throws IOException, Exception {
	   int[] tags = { PassportASN1Service.EF_SOD_TAG };
	   byte[] sd = service.readObject(tags);
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
   
   /**
    * @deprecated hack
    * @param wrapper
    */
   public void setWrapper(SecureMessagingWrapper wrapper) {
      service.setWrapper(wrapper);
   }
}
