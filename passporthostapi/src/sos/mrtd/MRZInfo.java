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
 * Data structure for storing the DG1. Based on ICAO 9303.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class MRZInfo
{
   private String docType;
   private String issuingState;
   private String firstNames;
   private String lastName;
   private String nationality;
   private String docNumber;
   private String personNumber;
   private Date dateOfBirth;
   private Date dateOfExpiry;
   
   public MRZInfo(String docType, String issuingState, String firstNames, String lastName,
         String nationality, String docNumber, String personNumber, Date dateOfBirth, Date dateOfExpiry) {
      this.docType = docType;
      this.issuingState = issuingState;
      this.firstNames = firstNames;
      this.lastName = lastName;
      this.nationality = nationality;
      this.docNumber = docNumber;
      this.personNumber = personNumber;
      this.dateOfBirth = dateOfBirth;
      this.dateOfExpiry = dateOfExpiry;
   }
   
   public MRZInfo(InputStream in) {
      try {
         DataInputStream dataIn = new DataInputStream(in);
         this.docType = readDocumentType(dataIn);
         if (docType.startsWith("I")) {
            /* Assume it's a I< document */
            this.issuingState = readIssuingState(dataIn);
            this.docNumber = readDocumentNumber(dataIn);
            this.personNumber = readPersonNumber(dataIn);
            this.dateOfBirth = readDateOfBirth(dataIn);
            this.dateOfExpiry = readDateOfExpiry(dataIn);
            this.nationality = readNationality(dataIn);
            String fullName = readName(dataIn);
            // this.firstNames = ...
            // this.lastName = ...
         } else {
            /* Assume it's a P< document */
            this.issuingState = readIssuingState(dataIn);
            String fullName = readName(dataIn);
            // this.firstNames = ...
            // this.lastName = ...
            this.docNumber = readDocumentNumber(dataIn);
            this.nationality = readNationality(dataIn);
            this.dateOfBirth = readDateOfBirth(dataIn);
            this.dateOfExpiry = readDateOfExpiry(dataIn);
            this.personNumber = readPersonNumber(dataIn);
         }
      } catch (IOException ioe) {
         throw new IllegalArgumentException("Invalid MRZ input source");
      }
   }
   
   /**
    * Reads the type of document.
    * The ICAO documentation gives "P<" as an example.
    * 
    * @return a string of length 2 containing the document type
    * @throws IOException if something goes wrong
    */
   public String readDocumentType(DataInputStream in) throws IOException {
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
   public String readIssuingState(DataInputStream in) throws IOException {
      /* in.skip(2); */
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
   public String readName(DataInputStream in) throws IOException {
   /* in.skip(2);
      in.skip(3); */
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
   public String readDocumentNumber(DataInputStream in) throws IOException {
   /* in.skip(2);
      in.skip(3);
      in.skip(39); */
      byte[] data = new byte[9];
      in.readFully(data);
      return new String(data).trim();
   }
   
   public String readPersonNumber(DataInputStream in) throws IOException {
      byte[] data = new byte[15];
      in.readFully(data);
      return new String(data).trim();
   }
   
   /**
    * Reads the nationality of the passport holder.
    * 
    * @return a string of length 3 containing the nationality of the passport holder
    * @throws IOException if something goes wrong
    */
   public String readNationality(DataInputStream in) throws IOException {
   /* in.skip(2);
      in.skip(3);
      in.skip(39);
      in.skip(9);
      in.skip(1); */
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
   public Date readDateOfBirth(DataInputStream in) throws IOException, NumberFormatException {
   /* in.skip(2);
      in.skip(3);
      in.skip(39);
      in.skip(9);
      in.skip(1);
      in.skip(3); */
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
   public Date readDateOfExpiry(DataInputStream in) throws IOException {in.skip(2);
   /* in.skip(3);
      in.skip(39);
      in.skip(9);
      in.skip(1);
      in.skip(3);
      in.skip(6);
      in.skip(1);
      in.skip(1); */
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
}
