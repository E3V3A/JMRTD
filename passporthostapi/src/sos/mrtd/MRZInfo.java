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
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

/**
 * Data structure for storing the MRZ information
 * as found in DG1.
 * Based on ICAO Doc 9303 part 1.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class MRZInfo
{
   private static final SimpleDateFormat SDF =
      new SimpleDateFormat("yyMMdd");
   
   private String documentType;
   private String issuingState;
   private String primaryIdentifier;
   private String[] secondaryIdentifiers;
   private String nationality;
   private String documentNumber;
   private String personalNumber;
   private Date dateOfBirth;
   private String gender;
   private Date dateOfExpiry;
   private char documentNumberCheckDigit;
   private char dateOfBirthCheckDigit;
   private char dateOfExpiryCheckDigit;
   private char personalNumberCheckDigit;
   private char compositeCheckDigit;
   private String unknownMRZField; // FIXME: Last field on line 2 of ID3 MRZ.
   
   /**
    * Constructs a new MRZ.
    * 
    * @param documentType document type
    * @param issuingState issuing state 3 letter abreviation
    * @param name card holder name
    * @param documentNumber document number
    * @param nationality nationality 3 letter abreviation
    * @param dateOfBirth date of birth
    * @param gender gender
    * @param dateOfExpiry date of expiry
    * @param personalNumber personal number
    */
   public MRZInfo(String documentType, String issuingState,
         String primaryIdentifier, String[] secondaryIdentifiers,
         String documentNumber, String nationality, Date dateOfBirth,
         String gender, Date dateOfExpiry, String personalNumber) {
      this.documentType = documentType;
      this.issuingState = issuingState;
      this.primaryIdentifier = primaryIdentifier;
      this.secondaryIdentifiers = secondaryIdentifiers;
      this.documentNumber = documentNumber;
      this.nationality = nationality; 
      this.dateOfBirth = dateOfBirth;
      this.gender = gender;
      this.dateOfExpiry = dateOfExpiry;
      this.personalNumber = personalNumber;
      if (documentType.startsWith("I")) {
         this.unknownMRZField = "<<<<<<<<<<<";
      }
      this.documentNumberCheckDigit = checkDigit(documentNumber);
      this.dateOfBirthCheckDigit = checkDigit(SDF.format(dateOfBirth));
      this.dateOfExpiryCheckDigit = checkDigit(SDF.format(dateOfExpiry));
      this.personalNumberCheckDigit = checkDigit(personalNumber);
      StringBuffer composite = new StringBuffer();
      if (documentType.startsWith("I")) {
         // TODO: just guessing...
         composite.append(documentNumber);
         composite.append(documentNumberCheckDigit);
         composite.append(personalNumber);
         composite.append(personalNumberCheckDigit);
         composite.append(SDF.format(dateOfBirth));
         composite.append(dateOfBirthCheckDigit);
         composite.append(SDF.format(dateOfExpiry));
         composite.append(dateOfExpiryCheckDigit);
      } else {
         composite.append(documentNumber);
         composite.append(documentNumberCheckDigit);
         composite.append(SDF.format(dateOfBirth));
         composite.append(dateOfBirthCheckDigit);
         composite.append(SDF.format(dateOfExpiry));
         composite.append(dateOfExpiryCheckDigit);
         composite.append(personalNumber);
         composite.append(personalNumberCheckDigit);
      }
      this.compositeCheckDigit = checkDigit(composite.toString());
   }
   
   /**
    * Constructs a new MRZ.
    * 
    * @param in contains the contents of DG1 (without the tag and length)
    */
   MRZInfo(InputStream in) {
      try {
         DataInputStream dataIn = new DataInputStream(in);
         this.documentType = readDocumentType(dataIn);
         if (documentType.startsWith("I")) {
            /* Assume it's an I< document */
            this.issuingState = readIssuingState(dataIn);
            this.documentNumber = readDocumentNumber(dataIn, 9);
            this.documentNumberCheckDigit = (char)dataIn.readUnsignedByte();
            this.personalNumber = readPersonalNumber(dataIn, 14);
            this.personalNumberCheckDigit = (char)dataIn.readUnsignedByte();
            this.dateOfBirth = readDateOfBirth(dataIn);
            this.dateOfBirthCheckDigit = (char)dataIn.readUnsignedByte();
            this.gender = readGender(dataIn);
            this.dateOfExpiry = readDateOfExpiry(dataIn);
            this.dateOfExpiryCheckDigit = (char)dataIn.readUnsignedByte();
            this.nationality = readNationality(dataIn);
            byte[] unknownMRZFieldBytes = new byte[11];
            dataIn.readFully(unknownMRZFieldBytes);
            this.unknownMRZField = new String(unknownMRZFieldBytes);
            this.compositeCheckDigit = (char)dataIn.readUnsignedByte();
            String name = readName(dataIn, 30);
            processNameIdentifiers(name);
         } else {
            /* Assume it's a P< document */
            this.issuingState = readIssuingState(dataIn);
            String name = readName(dataIn, 39);
            processNameIdentifiers(name);
            this.documentNumber = readDocumentNumber(dataIn, 9);
            this.documentNumberCheckDigit = (char)dataIn.readUnsignedByte();
            this.nationality = readNationality(dataIn);
            this.dateOfBirth = readDateOfBirth(dataIn);
            this.dateOfBirthCheckDigit = (char)dataIn.readUnsignedByte();
            this.gender = readGender(dataIn);
            this.dateOfExpiry = readDateOfExpiry(dataIn);
            this.dateOfExpiryCheckDigit = (char)dataIn.readUnsignedByte();
            this.personalNumber = readPersonalNumber(dataIn, 14);
            this.personalNumberCheckDigit = (char)dataIn.readUnsignedByte();
            this.compositeCheckDigit = (char)dataIn.readUnsignedByte();
         }
      } catch (IOException ioe) {
         throw new IllegalArgumentException("Invalid MRZ input source");
      }
   }
   
   private void processNameIdentifiers(String mrzNameString) {
      StringTokenizer st = new StringTokenizer(mrzNameString, "<<");
      if (!st.hasMoreTokens()) {
         throw new IllegalArgumentException("Input does not contain primary identifier!");
      }
      primaryIdentifier = st.nextToken();
      String rest = mrzNameString.substring(mrzNameString.indexOf("<<") + 2);
      st = new StringTokenizer(rest, "<");
      Collection result = new ArrayList();
      while (st.hasMoreTokens()) {
         String identifier = st.nextToken();
         if (identifier != null && identifier.length() > 0) {
            result.add(identifier);
         }
      }
      secondaryIdentifiers = (String[])result.toArray(new String[result.size()]);
   }
   
   public byte[] getEncoded() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      DataOutputStream dataOut = new DataOutputStream(out);
      writeDocumentType(dataOut);
      if (documentType.startsWith("I")) {
         /* Assume it's an I< document */
         writeIssuingState(dataOut);
         writeDocumentNumber(dataOut);
         dataOut.write(documentNumberCheckDigit);
         writePersonalNumber(dataOut);
         dataOut.write(personalNumberCheckDigit);
         writeDateOfBirth(dataOut);
         dataOut.write(dateOfBirthCheckDigit);
         writeGender(dataOut);
         writeDateOfExpiry(dataOut);
         dataOut.write(dateOfExpiryCheckDigit);
         writeNationality(dataOut);
         dataOut.write(unknownMRZField.getBytes("UTF-8")); // TODO: Understand this...
         dataOut.write(compositeCheckDigit);
         writeName(dataOut);
      } else {
         /* Assume it's a P< document */
         writeIssuingState(dataOut);
         writeName(dataOut);
         writeDocumentNumber(dataOut);
         dataOut.write(documentNumberCheckDigit);
         writeNationality(dataOut);
         writeDateOfBirth(dataOut);
         dataOut.write(dateOfBirthCheckDigit);
         writeGender(dataOut);
         writeDateOfExpiry(dataOut);
         dataOut.write(dateOfExpiryCheckDigit);
         writePersonalNumber(dataOut);
         dataOut.write(personalNumberCheckDigit);
         dataOut.write(compositeCheckDigit);
      }
      byte[] result = out.toByteArray();
      dataOut.close();
      return result;
   }
   
   private void writeIssuingState(DataOutputStream dataOut) throws IOException {
      dataOut.write(issuingState.getBytes("UTF-8"));
      
   }

   private void writePersonalNumber(DataOutputStream dataOut) throws IOException {
      dataOut.write(personalNumber.getBytes("UTF-8"));
   }

   private void writeDateOfExpiry(DataOutputStream dataOut) throws IOException {
      dataOut.write(SDF.format(dateOfExpiry).getBytes("UTF-8"));
   }

   private void writeGender(DataOutputStream dataOut) throws IOException {
      dataOut.write(gender.getBytes("UTF-8"));
      
   }

   private void writeDateOfBirth(DataOutputStream dataOut) throws IOException {
      dataOut.write(SDF.format(dateOfBirth).getBytes("UTF-8"));
   }

   private void writeNationality(DataOutputStream dataOut) throws IOException {
      dataOut.write(nationality.getBytes("UTF-8"));
   }

   private void writeDocumentNumber(DataOutputStream dataOut) throws IOException {
      dataOut.write(documentNumber.getBytes("UTF-8"));
   }
   
   private String getName() {
      int width = documentType.startsWith("I") ? 30 : 39;
      StringBuffer name = new StringBuffer();
      name.append(primaryIdentifier);
      name.append("<");
      for (int i = 0; i < secondaryIdentifiers.length; i++) {
         name.append("<");
         name.append(secondaryIdentifiers[i]);
      }
      while (name.length() < width) {
         name.append("<");
      }
      return name.toString().toUpperCase();
   }
   
   private void writeName(DataOutputStream dataOut) throws IOException {
      dataOut.write(getName().getBytes("UTF-8"));
   }

   private void writeDocumentType(DataOutputStream dataOut) throws IOException {
      dataOut.write(documentType.getBytes("UTF-8"));
   }

   /**
    * Reads the type of document.
    * ICAO Doc 9303 gives "P<" as an example.
    * 
    * @return a string of length 2 containing the document type
    * @throws IOException if something goes wrong
    */
   private String readDocumentType(DataInputStream in) throws IOException {
      byte[] result = new byte[2];
      in.readFully(result);
      return new String(result);
   }

   /**
    * Reads the issuing state as a three letter string.
    * 
    * @return a string of length 3 containing an abbreviation
    *         of the issuing state or organization
    *         
    * @throws IOException if something goes wrong
    */
   private String readIssuingState(DataInputStream in) throws IOException {
      byte[] data = new byte[3];
      in.readFully(data);
      return new String(data);
   }

   /**
    * Reads the passport holder's name, including &lt; characters.
    * 
    * @return a string containing last name and first names seperated by spaces
    * 
    * @throws IOException is something goes wrong
    */
   private String readName(DataInputStream in, int le) throws IOException {
      byte[] data = new byte[le];
      in.readFully(data);
      for (int i = 0; i < data.length; i++) {
         /*
         if (data[i] == '<') {
            data[i] = ' ';
         } */
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
   private String readDocumentNumber(DataInputStream in, int le) throws IOException {
      byte[] data = new byte[le];
      in.readFully(data);
      return new String(data).trim();
   }
   
   /**
    * Reads the personal number of the passport holder (or other optional data).
    * 
    * @param in input source
    * @param le maximal length
    * 
    * @return the personal number
    * 
    * @throws IOException if something goes wrong
    */
   private String readPersonalNumber(DataInputStream in, int le) throws IOException {
      byte[] data = new byte[le];
      in.readFully(data);
      return new String(data).trim();
   }
   
   /**
    * Reads the nationality of the passport holder.
    * 
    * @return a string of length 3 containing the nationality of the passport holder
    * 
    * @throws IOException if something goes wrong
    */
   private String readNationality(DataInputStream in) throws IOException {
      byte[] data = new byte[3];
      in.readFully(data);
      return new String(data).trim();
   }

   /**
    * Reads the 1 letter gender information.
    * 
    * @param in input source
    * 
    * @return the gender of the passport holder
    * 
    * @throws IOException if something goes wrong
    */
   private String readGender(DataInputStream in) throws IOException {
      byte[] data = new byte[1];
      in.readFully(data);
      return new String(data).trim();
   }
   
   /**
    * Reads the date of birth of the passport holder.
    * Base year is 1900.
    * 
    * @return the date of birth
    * 
    * @throws IOException if something goes wrong
    * @throws NumberFormatException if a data could not be constructed
    */
   private Date readDateOfBirth(DataInputStream in) throws IOException, NumberFormatException {
      byte[] data = new byte[6];
      in.readFully(data);
      String dateString = new String(data).trim();
      return parseDate(1900, dateString);
   }

   /**
    * Reads the date of expiry of this document.
    * Base year = 2000.
    * 
    * @return the date of expiry
    * 
    * @throws IOException if something goes wrong
    * @throws NumberFormatException if a date could not be constructed
    */
   private Date readDateOfExpiry(DataInputStream in) throws IOException, NumberFormatException {
      byte[] data = new byte[6];
      in.readFully(data);
      return parseDate(2000, new String(data).trim());
   }
   
   private Date parseDate(int baseYear, String dateString) throws NumberFormatException {
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
    * Gets the date of birth of the passport holder.
    * 
    * @return date of birth (with 1900 as base year)
    */
   public Date getDateOfBirth() {
      return dateOfBirth;
   }

   /**
    * Gets the date of expiry
    * 
    * @return date of expiry (with 2000 as base year)
    */
   public Date getDateOfExpiry() {
      return dateOfExpiry;
   }

   /**
    * Gets the document number.
    * 
    * @return document number
    */
   public String getDocumentNumber() {
      return documentNumber;
   }

   /**
    * Gets the document type.
    * 
    * @return document type
    */
   public String getDocumentType() {
      return documentType;
   }

   /**
    * Gets the issuing state
    * 
    * @return issuing state
    */
   public String getIssuingState() {
      return issuingState;
   }

   /**
    * Gets the passport holder's last name.
    * 
    * @return name
    */
   public String getPrimaryIdentifier() {
      return primaryIdentifier;
   }
   
   /**
    * Gets the passport holder's first names.
    * 
    * @return first names
    */
   public String[] getSecondaryIdentifiers() {
      return secondaryIdentifiers;
   }

   /**
    * Gets the passport holder's nationality.
    * 
    * @return a 3 letter country code
    */
   public String getNationality() {
      return nationality;
   }

   /**
    * Gets the personal number.
    * 
    * @return personal number
    */
   public String getPersonalNumber() {
      return personalNumber;
   }

   /**
    * Gets the passport holder's gender.
    * 
    * @return gender
    */
   public String getGender() {
      return gender;
   }
   
   /**
    * Creates a textual representation of this MRZ.
    * This is the 2 or 3 line representation
    * (depending on the document type) as it
    * appears in the document. // check digit
    * 
    * @return the MRZ as text
    * 
    * @see java.lang.Object#toString()
    */
   public String toString() {
      StringBuffer out = new StringBuffer();
      if (documentType.startsWith("I")) {
         /* 
          * FIXME: some composite check digit
          *        should go into this one as well...
          */
         out.append(documentType);
         out.append(issuingState);
         out.append(documentNumber);
         out.append(documentNumberCheckDigit);
         out.append(personalNumber);
         out.append(personalNumberCheckDigit);
         out.append("\n");
         out.append(SDF.format(dateOfBirth));
         out.append(dateOfBirthCheckDigit);
         out.append(gender);
         out.append(SDF.format(dateOfExpiry));
         out.append(dateOfExpiryCheckDigit);
         out.append(nationality);
         out.append(unknownMRZField);
         out.append(compositeCheckDigit);
         out.append("\n");
         out.append(getName());
         out.append("\n");
      } else {
         out.append(documentType);
         out.append(issuingState);
         out.append(getName());
         out.append("\n");
         out.append(documentNumber);
         out.append(documentNumberCheckDigit);
         out.append(nationality);
         out.append(SDF.format(dateOfBirth));
         out.append(dateOfBirthCheckDigit);
         out.append(gender);
         out.append(SDF.format(dateOfExpiry));
         out.append(dateOfExpiryCheckDigit);
         out.append(personalNumber);
         out.append(personalNumberCheckDigit);
         out.append(compositeCheckDigit);
         out.append("\n");
      }
      return out.toString();
   }
   
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj.getClass().equals(this.getClass()))) {
         return false;
      }
      MRZInfo other = (MRZInfo)obj;
      if (!documentType.equals(other.documentType)) { return false; }
      if (!issuingState.equals(other.issuingState)) { return false; }
      if (!primaryIdentifier.equals(other.primaryIdentifier)) { return false; }
      if (!Arrays.equals(secondaryIdentifiers, other.secondaryIdentifiers)) { return false; }
      if (!nationality.equals(other.nationality)) { return false; }
      if (!documentNumber.equals(other.documentNumber)) { return false; }
      if (!personalNumber.equals(other.personalNumber)) { return false; }
      if (!dateOfBirth.equals(other.dateOfBirth)) { return false; }
      if (!gender.equals(other.gender)) { return false; }
      if (!dateOfExpiry.equals(other.dateOfExpiry)) { return false; }
      return true;
   }
   
   /**
    * Computes the 7-3-1 check digit for part of the MRZ.
    *
    * @param str a part of the MRZ.
    *
    * @return the resulting check digit.
    */
   public static char checkDigit(String str) {
      try {
         byte[] chars = str.getBytes("UTF-8");
         int[] weights = { 7, 3, 1 };
         int result = 0;
         for (int i = 0; i < chars.length; i++) {
            result = (result + weights[i % 3] * decodeMRZDigit(chars[i])) % 10;
         }
         chars = Integer.toString(result).getBytes("UTF-8");
         return (char)chars[0];
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalArgumentException(e.toString());
      }
   }

   /**
    * Looks up the numerical value for MRZ characters. In order to be able
    * to compute check digits.
    *
    * @param ch a character from the MRZ.
    *
    * @return the numerical value of the character.
    *
    * @throws NumberFormatException if <code>ch</code> is not a valid MRZ
    *                               character.
    */
   private static int decodeMRZDigit(byte ch) throws NumberFormatException {
      switch (ch) {
         case '<':
         case '0': return 0; case '1': return 1; case '2': return 2;
         case '3': return 3; case '4': return 4; case '5': return 5;
         case '6': return 6; case '7': return 7; case '8': return 8;
         case '9': return 9;
         case 'a': case 'A': return 10; case 'b': case 'B': return 11;
         case 'c': case 'C': return 12; case 'd': case 'D': return 13;
         case 'e': case 'E': return 14; case 'f': case 'F': return 15;
         case 'g': case 'G': return 16; case 'h': case 'H': return 17;
         case 'i': case 'I': return 18; case 'j': case 'J': return 19;
         case 'k': case 'K': return 20; case 'l': case 'L': return 21;
         case 'm': case 'M': return 22; case 'n': case 'N': return 23;
         case 'o': case 'O': return 24; case 'p': case 'P': return 25;
         case 'q': case 'Q': return 26; case 'r': case 'R': return 27;
         case 's': case 'S': return 28; case 't': case 'T': return 29;
         case 'u': case 'U': return 30; case 'v': case 'V': return 31;
         case 'w': case 'W': return 32; case 'x': case 'X': return 33;
         case 'y': case 'Y': return 34; case 'z': case 'Z': return 35;
         default:
            throw new NumberFormatException("Could not decode MRZ character "
                                            + ch);
      }
   }
   
   public static void main(String[] arg) {
      try {
         FileInputStream fileIn = new FileInputStream(arg[0]);
         MRZInfo mrzInfo = new MRZInfo(fileIn);
         System.out.println(mrzInfo);
         
         System.out.println("primaryIdentifier = " + mrzInfo.getPrimaryIdentifier());
         String[] secondaryIdentifiers = mrzInfo.getSecondaryIdentifiers();
         for (int i = 0; i < secondaryIdentifiers.length; i++) {
            System.out.println("secondaryIdentifiers[" + i + "] = " + secondaryIdentifiers[i]);
         }
         
         ByteArrayInputStream mrzIn = new ByteArrayInputStream(mrzInfo.getEncoded());
         MRZInfo mrzInfo2 = new MRZInfo(mrzIn);
         System.out.println(mrzInfo2);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
