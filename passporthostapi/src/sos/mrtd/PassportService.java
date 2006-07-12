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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.mozilla.jss.pkcs7.SignedData;

import sos.smartcards.APDUListener;
import sos.smartcards.Apdu;
import sos.smartcards.BERTLVObject;
import sos.smartcards.CardService;
import sos.smartcards.JPCSCService;

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
   private static final int SESSION_STOPPED_STATE = 0;
   private static final int SESSION_STARTED_STATE = 1;
   private static final int AUTHENTICATED_STATE = 2;
   private int state;

   private PassportFileService service;
   private KeyFactory factory;
   
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
         this.service = new PassportFileService(service);
      }
      factory = KeyFactory.getInstance("RSA");
      state = SESSION_STOPPED_STATE;
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
      this.service.setWrapper(wrapper);
      state = AUTHENTICATED_STATE;
   }

   /**
    * Opens a session. This is done by connecting to the card, selecting the
    * passport applet.
    */
   public void open() {
      service.open();
      state = SESSION_STARTED_STATE;
   }
   
   public String[] getTerminals() {
      return service.getTerminals();
   }

   public void open(String id) {
      service.open(id);
      state = SESSION_STARTED_STATE;
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
      try {
         service.close();
      } finally {
         state = SESSION_STOPPED_STATE;
      }
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
      byte[] tag = { 0x5C };
      byte[] tagList = readObject(PassportFileService.EF_COM, tag).getValueAsBytes();
      short[] files = new short[tagList.length];
      for (int i = 0; i < files.length; i++) {
         files[i] = Util.lookupFIDbyTag(tagList[i]);
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
      System.out.println("DEBUG: " + dateString);
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
      byte[] tag = { 0x5F, 0x2E }; 
      byte[] facialRecordData = (byte[])readObject(PassportFileService.EF_DG2, tag).getValueAsBytes();
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
      byte[] tag = { 0x5F, 0x1F };
      return readObject(PassportFileService.EF_DG1, tag).getValueAsBytes();
   }
   
   private BERTLVObject readObject(short fid, byte[] tag) throws IOException {
      byte[] file = service.readFile(fid);
      BERTLVObject fileObject = BERTLVObject.getInstance(new ByteArrayInputStream(file));
      BERTLVObject object = fileObject.getChild(tag);
      return object;
   }

   public PublicKey readAAPublicKey() throws IOException, GeneralSecurityException {
      byte[] file = service.readFile(PassportFileService.EF_DG15);
      BERTLVObject fileObj = BERTLVObject.getInstance(new ByteArrayInputStream(file));
      X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(fileObj.getValueAsBytes());
      return factory.generatePublic(pubKeySpec);
   }
   
   /**
    * Reads the security object.
    * 
    * @return the security object data
    * 
    * @throws IOException
    */
   public SignedData readSecurityObject() throws IOException {
      byte[] tag = { 0x77 };
      BERTLVObject object = readObject(PassportFileService.EF_SOD, tag);
      BERTLVObject[] children = (BERTLVObject[])object.getValue();
      System.out.println("children.length = " + children.length);
      return null; // HIER
   }

   private static final Provider PROVIDER =
      new org.bouncycastle.jce.provider.BouncyCastleProvider();
   

   public static void main(String[] arg) {
      try {
         Security.insertProviderAt(PROVIDER, 2);
         PassportService service = new PassportService(new JPCSCService());
         service.open();
         service.doBAC("ZZ0062725", "710121", "091130");
         service.readSecurityObject();
         service.close();
      } catch (Exception e) {
         e.printStackTrace();
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
