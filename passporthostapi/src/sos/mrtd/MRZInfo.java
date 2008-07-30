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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import sos.data.Country;
import sos.data.Gender;
import sos.tlv.BERTLVObject;

/**
 * Data structure for storing the MRZ information
 * as found in DG1. Based on ICAO Doc 9303 part 1 and 3.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class MRZInfo
{
	/** Unspecified document type (do not use, choose ID1 or ID3). */
	public static final int DOC_TYPE_UNSPECIFIED = 0;
	/** ID1 document type for credit card sized national identity cards. */
	public static final int DOC_TYPE_ID1 = 1;
	/** ID2 document type. */
	public static final int DOC_TYPE_ID2 = 2;
	/** ID3 document type for passport booklets. */
	public static final int DOC_TYPE_ID3 = 3;                           

	private static final String MRZ_CHARS = "<0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private static final SimpleDateFormat SDF =
		new SimpleDateFormat("yyMMdd");

	private int documentType;
	private Country issuingState;
	private String primaryIdentifier;
	private String[] secondaryIdentifiers;
	private Country nationality;
	private String documentNumber;
	private String personalNumber;
	private Date dateOfBirth;
	private Gender gender;
	private Date dateOfExpiry;
	private char documentNumberCheckDigit;
	private char dateOfBirthCheckDigit;
	private char dateOfExpiryCheckDigit;
	private char personalNumberCheckDigit;
	private char compositeCheckDigit;
	private String optionalData2; // FIXME: Last field on line 2 of ID3 MRZ.

	/**
	 * Constructs a new MRZ.
	 * 
	 * @param documentType document type
	 * @param issuingState issuing state
	 * @param primaryIdentifier card holder name
	 * @param secondaryIdentifiers card holder name
	 * @param documentNumber document number
	 * @param nationality nationality
	 * @param dateOfBirth date of birth
	 * @param gender gender
	 * @param dateOfExpiry date of expiry
	 * @param personalNumber personal number
	 */
	public MRZInfo(int documentType, Country issuingState,
			String primaryIdentifier, String[] secondaryIdentifiers,
			String documentNumber, Country nationality, Date dateOfBirth,
			Gender gender, Date dateOfExpiry, String personalNumber) {
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
		if (documentType == DOC_TYPE_ID1) {
			this.optionalData2 = "<<<<<<<<<<<";
		}
		checkDigit();
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
			if (documentType == DOC_TYPE_ID1) {
				this.issuingState = readIssuingState(dataIn);
				this.documentNumber = readDocumentNumber(dataIn, 9);
				this.documentNumberCheckDigit = (char)dataIn.readUnsignedByte();
				this.personalNumber = trimFillerChars(readPersonalNumber(dataIn, 15));
				/* FIXME is the last char always '<'? is it part of personal nr? or is this indeed a checkbit? */
				this.personalNumberCheckDigit = checkDigit(this.personalNumber);
				this.dateOfBirth = readDateOfBirth(dataIn);
				this.dateOfBirthCheckDigit = (char)dataIn.readUnsignedByte();
				this.gender = readGender(dataIn);
				this.dateOfExpiry = readDateOfExpiry(dataIn);
				this.dateOfExpiryCheckDigit = (char)dataIn.readUnsignedByte();
				this.nationality = readNationality(dataIn);
				byte[] optionalData2Bytes = new byte[11];
				dataIn.readFully(optionalData2Bytes);
				this.optionalData2 = new String(optionalData2Bytes);
				this.compositeCheckDigit = (char)dataIn.readUnsignedByte();
				String name = readName(dataIn, 30);
				processNameIdentifiers(name);
			} else {
				/* Assume it's a ID3 document */
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
				this.personalNumber = trimFillerChars(readPersonalNumber(dataIn, 14));
				this.personalNumberCheckDigit = (char)dataIn.readUnsignedByte();
				this.compositeCheckDigit = (char)dataIn.readUnsignedByte();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalArgumentException("Invalid MRZ input source");
		}
	}

	private void processNameIdentifiers(String mrzNameString) {
		int delimIndex = mrzNameString.indexOf("<<");
		if (delimIndex < 0) {
			throw new IllegalArgumentException("Input does not contain primary identifier!");
		}
		primaryIdentifier = mrzNameString.substring(0, delimIndex);
		String rest = mrzNameString.substring(mrzNameString.indexOf("<<") + 2);
		processSecondaryIdentifiers(rest);
	}

	private void processSecondaryIdentifiers(String secondaryIdentifiersString) {
		StringTokenizer st = new StringTokenizer(secondaryIdentifiersString, "<");
		Collection<String> result = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String identifier = st.nextToken();
			if (identifier != null && identifier.length() > 0) {
				result.add(identifier);
			}
		}
		secondaryIdentifiers = (String[])result.toArray(new String[result.size()]);
	}

	private String trimFillerChars(String str) {
		byte[] chars = str.trim().getBytes();
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '<') { chars[i] = ' '; }
		}
		return (new String(chars)).trim();
	}

	public byte[] getEncoded() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(out);
			writeDocumentType(dataOut);
			if (documentType == DOC_TYPE_ID1) {
				/* Assume it's an ID1 document */
				writeIssuingState(dataOut);
				writeDocumentNumber(dataOut, 9); /* FIXME: max size of field */
				dataOut.write(documentNumberCheckDigit);
				writePersonalNumber(dataOut, 14); /* FIXME: max size of field */
				dataOut.write('<'); /* FIXME: personal number may be length 15 in ID1? */
				// dataOut.write(personalNumberCheckDigit);
				writeDateOfBirth(dataOut);
				dataOut.write(dateOfBirthCheckDigit);
				writeGender(dataOut);
				writeDateOfExpiry(dataOut);
				dataOut.write(dateOfExpiryCheckDigit);
				writeNationality(dataOut);
				dataOut.write(optionalData2.getBytes("UTF-8")); // TODO: Understand this...
				dataOut.write(compositeCheckDigit);
				writeName(dataOut, 30);
			} else {
				/* Assume it's a ID3 document */
				writeIssuingState(dataOut);
				writeName(dataOut, 39);
				writeDocumentNumber(dataOut, 9);
				dataOut.write(documentNumberCheckDigit);
				writeNationality(dataOut);
				writeDateOfBirth(dataOut);
				dataOut.write(dateOfBirthCheckDigit);
				writeGender(dataOut);
				writeDateOfExpiry(dataOut);
				dataOut.write(dateOfExpiryCheckDigit);
				writePersonalNumber(dataOut, 14); /* FIXME: max size of field */
				dataOut.write(personalNumberCheckDigit);
				dataOut.write(compositeCheckDigit);
			}
			byte[] result = out.toByteArray();
			dataOut.close();
			return result;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	private void writeIssuingState(DataOutputStream dataOut) throws IOException {
		dataOut.write(issuingState.toAlpha3Code().getBytes("UTF-8"));
	}

	private void writePersonalNumber(DataOutputStream dataOut, int width) throws IOException {
		dataOut.write(mrzFormat(personalNumber, width).getBytes("UTF-8"));
	}

	private void writeDateOfExpiry(DataOutputStream dataOut) throws IOException {
		dataOut.write(SDF.format(dateOfExpiry).getBytes("UTF-8"));
	}

	private void writeGender(DataOutputStream dataOut) throws IOException {
		dataOut.write(genderToString().getBytes("UTF-8"));
	}

	private void writeDateOfBirth(DataOutputStream dataOut) throws IOException {
		dataOut.write(SDF.format(dateOfBirth).getBytes("UTF-8"));
	}

	private void writeNationality(DataOutputStream dataOut) throws IOException {
		dataOut.write(nationality.toAlpha3Code().getBytes("UTF-8"));
	}

	private void writeDocumentNumber(DataOutputStream dataOut, int width) throws IOException {
		dataOut.write(mrzFormat(documentNumber, width).getBytes("UTF-8"));
	}

	private void writeName(DataOutputStream dataOut, int width) throws IOException {
		dataOut.write(nameToString(width).getBytes("UTF-8"));
	}

	private void writeDocumentType(DataOutputStream dataOut) throws IOException {
		dataOut.write(documentTypeToString().getBytes("UTF-8"));
	}

	private String documentTypeToString() {
		switch (documentType) {
		case DOC_TYPE_ID1: return "I<";
		case DOC_TYPE_ID2: return "P<";
		case DOC_TYPE_ID3: return "P<";
		default: return "P<";
		}
	}

	private String genderToString() {
		switch (gender) {
		case MALE: return "M";
		case FEMALE: return "F";
		default: return "<";
		}
	}

	private String nameToString(int width) {
		StringBuffer name = new StringBuffer();
		name.append(primaryIdentifier);
		name.append("<");
		for (int i = 0; i < secondaryIdentifiers.length; i++) {
			name.append("<");
			name.append(secondaryIdentifiers[i]);
		}
		return mrzFormat(name.toString(), width);
	}


	/**
	 * Reads the type of document.
	 * ICAO Doc 9303 part 1 gives "P<" as an example.
	 * 
	 * @return a string of length 2 containing the document type
	 * @throws IOException if something goes wrong
	 */
	private int readDocumentType(DataInputStream in) throws IOException {
		byte[] docTypeBytes = new byte[2];
		in.readFully(docTypeBytes);
		String docTypeStr = new String(docTypeBytes);
		if (docTypeStr.startsWith("A") || docTypeStr.startsWith("C") || docTypeStr.startsWith("I")) {
			return DOC_TYPE_ID1;
		} else if (docTypeStr.startsWith("P")) {
			return DOC_TYPE_ID3;
		}
		return DOC_TYPE_UNSPECIFIED;
	}

	/**
	 * Reads the issuing state as a three letter string.
	 * 
	 * @return a string of length 3 containing an abbreviation
	 *         of the issuing state or organization
	 *         
	 * @throws IOException if something goes wrong
	 */
	private Country readIssuingState(DataInputStream in) throws IOException {
		byte[] data = new byte[3];
		in.readFully(data);
		return Country.getInstance(new String(data));
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
//		for (int i = 0; i < data.length; i++) {
//		if (data[i] == '<') {
//		data[i] = ' ';
//		}
//		}
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
		return trimFillerChars(new String(data));
	}

	/**
	 * Reads the nationality of the passport holder.
	 * 
	 * @return a string of length 3 containing the nationality of the passport holder
	 * 
	 * @throws IOException if something goes wrong
	 */
	private Country readNationality(DataInputStream in) throws IOException {
		byte[] data = new byte[3];
		in.readFully(data);
		return Country.getInstance(new String(data).trim());
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
	private Gender readGender(DataInputStream in) throws IOException {
		byte[] data = new byte[1];
		in.readFully(data);
		String genderStr = new String(data).trim();
		if (genderStr.equalsIgnoreCase("M")) {
			return Gender.MALE;
		}
		if (genderStr.equalsIgnoreCase("F")) {
			return Gender.FEMALE;
		}
		return Gender.UNKNOWN;
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
	 * Base year is 2000.
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

	private static Date parseDate(int baseYear, String dateString) throws NumberFormatException {
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

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
		checkDigit();
	}

	/**
	 * Gets the date of expiry
	 * 
	 * @return date of expiry (with 2000 as base year)
	 */
	public Date getDateOfExpiry() {
		return dateOfExpiry;
	}

	public void setDateOfExpiry(Date dateOfExpiry) {
		this.dateOfExpiry = dateOfExpiry;
		checkDigit();
	}

	/**
	 * Gets the document number.
	 * 
	 * @return document number
	 */
	public String getDocumentNumber() {
		return documentNumber;
	}


	public void setDocumentNumber(String documentNumber) {
		this.documentNumber = documentNumber.trim();
		checkDigit();
	}

	/**
	 * Gets the document type.
	 * 
	 * @return document type
	 */
	public int getDocumentType() {
		return documentType;
	}

	/**
	 * Gets the issuing state
	 * 
	 * @return issuing state
	 */
	public Country getIssuingState() {
		return issuingState;
	}

	public void setIssuingState(Country issuingState) {
		this.issuingState = issuingState;
		checkDigit();
	}

	/**
	 * Gets the passport holder's last name.
	 * 
	 * @return name
	 */
	public String getPrimaryIdentifier() {
		return primaryIdentifier;
	}

	public void setPrimaryIdentifier(String primaryIdentifier) {
		this.primaryIdentifier = primaryIdentifier.trim();
		checkDigit();
	}

	/**
	 * Gets the passport holder's first names.
	 * 
	 * @return first names
	 */
	public String[] getSecondaryIdentifiers() {
		return secondaryIdentifiers;
	}

	public void setSecondaryIdentifiers(String[] secondaryIdentifiers) {
		if (secondaryIdentifiers == null) {
			this.secondaryIdentifiers = null;
		} else {
			this.secondaryIdentifiers = new String[secondaryIdentifiers.length];
			System.arraycopy(secondaryIdentifiers, 0, this.secondaryIdentifiers, 0, secondaryIdentifiers.length);
		}
		checkDigit();
	}

	public void setSecondaryIdentifiers(String secondaryIdentifiers) {
		processSecondaryIdentifiers(secondaryIdentifiers.trim());
		checkDigit();
	}

	/**
	 * Gets the passport holder's nationality.
	 * 
	 * @return a country
	 */
	public Country getNationality() {
		return nationality;
	}

	public void setNationality(Country nationality) {
		this.nationality = nationality;
		checkDigit();
	}

	/**
	 * Gets the personal number.
	 * 
	 * @return personal number
	 */
	public String getPersonalNumber() {
		return personalNumber;
	}

	public void setPersonalNumber(String personalNumber) {
		this.personalNumber = trimFillerChars(personalNumber);
		checkDigit();
	}

	/**
	 * Gets the passport holder's gender.
	 * 
	 * @return gender
	 */
	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
		checkDigit();
	}

	/**
	 * Creates a textual representation of this MRZ.
	 * This is the 2 or 3 line representation
	 * (depending on the document type) as it
	 * appears in the document. All lines end in
	 * a newline char.
	 * 
	 * @return the MRZ as text
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer out = new StringBuffer();
		if (documentType == DOC_TYPE_ID1) {
			/* 
			 * FIXME: some composite check digit
			 *        should go into this one as well...
			 */
			out.append(documentTypeToString());
			out.append(issuingState.toAlpha3Code());
			out.append(documentNumber);
			out.append(documentNumberCheckDigit);
			out.append(mrzFormat(personalNumber, 15));
			// out.append(personalNumberCheckDigit);
			out.append("\n");
			out.append(SDF.format(dateOfBirth));
			out.append(dateOfBirthCheckDigit);
			out.append(genderToString());
			out.append(SDF.format(dateOfExpiry));
			out.append(dateOfExpiryCheckDigit);
			out.append(nationality.toAlpha3Code());
			out.append(optionalData2);
			out.append(compositeCheckDigit); // should be: upper + middle line?
			out.append("\n");
			out.append(nameToString(30));
			out.append("\n");
		} else {
			out.append(documentTypeToString());
			out.append(issuingState.toAlpha3Code());
			out.append(nameToString(39));
			out.append("\n");
			out.append(documentNumber);
			out.append(documentNumberCheckDigit);
			out.append(nationality.toAlpha3Code());
			out.append(SDF.format(dateOfBirth));
			out.append(dateOfBirthCheckDigit);
			out.append(genderToString());
			out.append(SDF.format(dateOfExpiry));
			out.append(dateOfExpiryCheckDigit);
			out.append(mrzFormat(personalNumber, 14));
			out.append(personalNumberCheckDigit);
			out.append(compositeCheckDigit);
			out.append("\n");
		}
		return out.toString();
	}

	public boolean equals(Object obj) {
		if (obj == this) { return true; }
		if (obj == null) { return false; }
		if (!(obj.getClass().equals(this.getClass()))) { return false; }
		MRZInfo other = (MRZInfo)obj;
		if (documentType != other.documentType) { return false; }
		if (!issuingState.equals(other.issuingState)) { return false; }
		if (!primaryIdentifier.equals(other.primaryIdentifier)) { return false; }
		if (!Arrays.equals(secondaryIdentifiers, other.secondaryIdentifiers)) { return false; }
		if (!nationality.equals(other.nationality)) { return false; }
		if (!documentNumber.equals(other.documentNumber)) { return false; }
		if (!personalNumber.equals(other.personalNumber)) { return false; }
		if (!dateOfBirth.equals(other.dateOfBirth)) { return false; }
		if (gender != other.gender) { return false; }
		if (!dateOfExpiry.equals(other.dateOfExpiry)) { return false; }
		return true;
	}
	
	private void checkDigit() {
		this.documentNumberCheckDigit = checkDigit(documentNumber);
		this.dateOfBirthCheckDigit = checkDigit(SDF.format(dateOfBirth));
		this.dateOfExpiryCheckDigit = checkDigit(SDF.format(dateOfExpiry));
		this.personalNumberCheckDigit = checkDigit(mrzFormat(personalNumber, 14));
		StringBuffer composite = new StringBuffer();
		if (documentType == DOC_TYPE_ID1) {
			// TODO: Include: 6-30 (upper line), 1-7,9-15,19-29 (middle line)
			// composite.append(documentTypeToString());
			// composite.append(issuingState);
			composite.append(documentNumber);
			composite.append(documentNumberCheckDigit);
			composite.append(mrzFormat(personalNumber, 15));
			composite.append(SDF.format(dateOfBirth));
			composite.append(dateOfBirthCheckDigit);
			composite.append(SDF.format(dateOfExpiry));
			composite.append(dateOfExpiryCheckDigit);
			composite.append(optionalData2);
		} else {
			composite.append(documentNumber);
			composite.append(documentNumberCheckDigit);
			composite.append(SDF.format(dateOfBirth));
			composite.append(dateOfBirthCheckDigit);
			composite.append(SDF.format(dateOfExpiry));
			composite.append(dateOfExpiryCheckDigit);
			composite.append(mrzFormat(personalNumber, 14));
			composite.append(personalNumberCheckDigit);
		}
		this.compositeCheckDigit = checkDigit(composite.toString());
	}

	/**
	 * Reformats the input string such that it
	 * only contains 'A'-'Z' and '<' characters.
	 * 
	 * @param str the input string
	 * @param width the (minimal) width of the result
	 * @return the reformatted string
	 */
	private static String mrzFormat(String str, int width) {
		str = str.toUpperCase().trim();
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (MRZ_CHARS.indexOf(c) == -1) {
				result.append('<');
			} else {
				result.append(c);
			}  
		}
		while (result.length() < width) {
			result.append("<");
		}
		return result.toString();
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
					+ ch + " ('" + Character.toString((char)ch) + "')");
		}
	}

	public static void main(String[] arg) {
		try {
			//FileInputStream fileIn = new FileInputStream(arg[0]);
			String[] secundaries = { "JAN", "PETER" };

//			int[] mrzpath = { PassportASN1Service.EF_DG1_TAG, 0x5F1F };

//			BERTLVObject ef0101 = BERTLVObject.getInstance(new FileInputStream(arg[0]));
//			System.out.println(ef0101.getSubObject(mrzpath, 0, mrzpath.length));

//			MRZInfo bla = new MRZInfo(new ByteArrayInputStream(ef0101.getSubObject(mrzpath, 0, mrzpath.length).getValueAsBytes()));
//			System.out.println(bla);

//			if(arg.length > 0)
//			return;

			MRZInfo mrzInfo =
				new MRZInfo(3, Country.getInstance("NLD"),
						"Balkenende", secundaries,
						"PPNUMMER0", Country.getInstance("NLD"), parseDate(1900, "560507"), Gender.MALE, 
						parseDate(2000, "100101"),  "876543210<<<<<");

			BERTLVObject ef0101 = new BERTLVObject(PassportFile.EF_DG1_TAG, new BERTLVObject(0x5f1f, mrzInfo.getEncoded()));        
			System.out.println(ef0101);
			FileOutputStream out = new FileOutputStream(arg[0]);
			out.write(ef0101.getEncoded());
			out.close();

//			System.out.println("primaryIdentifier = " + mrzInfo.getPrimaryIdentifier());
//			String[] secondaryIdentifiers = mrzInfo.getSecondaryIdentifiers();
//			for (int i = 0; i < secondaryIdentifiers.length; i++) {
//			System.out.println("secondaryIdentifiers[" + i + "] = " + secondaryIdentifiers[i]);
//			}

//			ByteArrayInputStream mrzIn = new ByteArrayInputStream(mrzInfo.getEncoded());
//			MRZInfo mrzInfo2 = new MRZInfo(mrzIn);
//			System.out.println(mrzInfo2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
