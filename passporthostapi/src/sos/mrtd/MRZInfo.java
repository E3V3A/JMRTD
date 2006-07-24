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
import java.io.InputStream;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Data structure for storing the MRZ information in DG1.
 * Based on ICAO Doc 9303 part 1.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class MRZInfo
{
   private String documentType;
   private String issuingState;
   private String name;
   private String nationality;
   private String documentNumber;
   private String personalNumber;
   private Date dateOfBirth;
   private String sex;
   private Date dateOfExpiry;
   
   public MRZInfo(String documentType, String issuingState, String name,
         String nationality, String docNumber, String personalNumber, Date dateOfBirth,
         String sex, Date dateOfExpiry) {
      this.documentType = documentType;
      this.issuingState = issuingState;
      this.name = name;
      this.nationality = nationality;
      this.documentNumber = docNumber;
      this.personalNumber = personalNumber;
      this.dateOfBirth = dateOfBirth;
      this.sex = sex;
      this.dateOfExpiry = dateOfExpiry;
   }
   
   public MRZInfo(InputStream in) {
      try {
         DataInputStream dataIn = new DataInputStream(in);
         this.documentType = readDocumentType(dataIn);
         if (documentType.startsWith("I")) {
            /* Assume it's an I< document */
            this.issuingState = readIssuingState(dataIn);
            this.documentNumber = readDocumentNumber(dataIn, 9);
            dataIn.skip(1); // check digit
            this.personalNumber = readPersonalNumber(dataIn, 14);
            dataIn.skip(1); // check digit
            this.dateOfBirth = readDateOfBirth(dataIn);
            dataIn.skip(1); // check digit
            this.sex = readSex(dataIn);
            this.dateOfExpiry = readDateOfExpiry(dataIn);
            dataIn.skip(1); // check digit
            this.nationality = readNationality(dataIn);
            dataIn.skip(12);
            this.name = readName(dataIn, 30);
         } else {
            /* Assume it's a P< document */
            this.issuingState = readIssuingState(dataIn);
            this.name = readName(dataIn, 39);
            this.documentNumber = readDocumentNumber(dataIn, 9);
            dataIn.skip(1); // check digit
            this.nationality = readNationality(dataIn);
            this.dateOfBirth = readDateOfBirth(dataIn);
            dataIn.skip(1); // check digit
            this.sex = readSex(dataIn);
            this.dateOfExpiry = readDateOfExpiry(dataIn);
            dataIn.skip(1); // check digit
            this.personalNumber = readPersonalNumber(dataIn, 14);
            dataIn.skip(1); // check digit
         }
      } catch (IOException ioe) {
         throw new IllegalArgumentException("Invalid MRZ input source");
      }
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
      byte[] data = new byte[le]; // FIXME: check if we have ID3 type document (otherwise 30 or 31 instead of 39)
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
   private String readSex(DataInputStream in) throws IOException {
      byte[] data = new byte[1];
      in.readFully(data);
      return new String(data).trim();
   }
   
   /**
    * Reads the date of birth of the passport holder
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
      return makeDate(1900, dateString);
   }

   /**
    * Reads the date of expiry of this document
    * 
    * @return the date of expiry
    * 
    * @throws IOException if something goes wrong
    * @throws NumberFormatException if a date could not be constructed
    */
   private Date readDateOfExpiry(DataInputStream in) throws IOException, NumberFormatException {
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

   public Date getDateOfBirth() {
      return dateOfBirth;
   }

   public Date getDateOfExpiry() {
      return dateOfExpiry;
   }

   public String getDocumentNumber() {
      return documentNumber;
   }

   public String getDocumentType() {
      return documentType;
   }

   public String getIssuingState() {
      return issuingState;
   }

   public String getName() {
      return name;
   }

   public String getNationality() {
      return nationality;
   }

   public String getPersonalNumber() {
      return personalNumber;
   }

   public String getSex() {
      return sex;
   }
   
   public String toString() {
      return
         "type: " + documentType
         + "\nissuing state: " + issuingState
         + "\nname: " + name
         + "\ndoc number: " + documentNumber
         + "\nnationality: " + nationality
         + "\ndate of birth: " + dateOfBirth
         + "\ndate of expiry: " + dateOfExpiry
         + "\npersonal number: " + personalNumber;
   }
}
