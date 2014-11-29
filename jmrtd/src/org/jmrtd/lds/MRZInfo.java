/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2014  The JMRTD team
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

package org.jmrtd.lds;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import net.sf.scuba.data.Gender;

/**
 * Data structure for storing the MRZ information
 * as found in DG1. Based on ICAO Doc 9303 part 1 and 3.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
public class MRZInfo extends AbstractLDSInfo {

	private static final long serialVersionUID = 7054965914471297804L;

	/** Unspecified document type (do not use, choose ID1 or ID3). */
	public static final int DOC_TYPE_UNSPECIFIED = 0;

	/** ID1 document type for credit card sized identity cards. Specifies a 3-line MRZ, 30 characters wide. */
	public static final int DOC_TYPE_ID1 = 1;

	/** ID2 document type. Specifies a 2-line MRZ, 36 characters wide. */
	public static final int DOC_TYPE_ID2 = 2;

	/** ID3 document type for passport booklets. Specifies a 2-line MRZ, 44 characters wide. */
	public static final int DOC_TYPE_ID3 = 3;

	/** All valid characters in MRZ. */
	private static final String MRZ_CHARS = "<0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/** @deprecated to be replaced with documentCode */
	private int documentType;

	private String documentCode;
	private String issuingState;
	private String primaryIdentifier;
	private String secondaryIdentifier;
	private String nationality;
	private String documentNumber;
	private String dateOfBirth;
	private Gender gender;
	private String dateOfExpiry;
	private char documentNumberCheckDigit;
	private char dateOfBirthCheckDigit;
	private char dateOfExpiryCheckDigit;
	private char compositeCheckDigit;
	private String optionalData1; /* NOTE: holds personal number for some issuing states (e.g. NL), but is used to hold (part of) document number for others. */
	private String optionalData2;

	/**
	 * Creates a new 2-line MRZ compliant with ICAO Doc 9303 part 1 vol 1.
	 *
	 * @param documentCode document code (1 or 2 digit, has to start with "P" or "V")
	 * @param issuingState issuing state as 3 digit string
	 * @param primaryIdentifier card holder last name
	 * @param secondaryIdentifier card holder first name(s)
	 * @param documentNumber document number
	 * @param nationality nationality as 3 digit string
	 * @param dateOfBirth date of birth
	 * @param gender gender
	 * @param dateOfExpiry date of expiry
	 * @param personalNumber either empty, or a personal number of maximum length 14, or other optional data of exact length 15
	 */
	public MRZInfo(String documentCode, String issuingState,
			String primaryIdentifier, String secondaryIdentifier,
			String documentNumber, String nationality, String dateOfBirth,
			Gender gender, String dateOfExpiry, String personalNumber) {
		if (documentCode == null || documentCode.length() < 1 || documentCode.length() > 2
				|| !(documentCode.startsWith("P") || documentCode.startsWith("V"))) {
			throw new IllegalArgumentException("Wrong document code: " + documentCode);
		}
		this.documentType = getDocumentTypeFromDocumentCode(documentCode);
		this.documentCode = trimFillerChars(documentCode);
		this.issuingState = issuingState;
		this.primaryIdentifier = primaryIdentifier;
		this.secondaryIdentifier = secondaryIdentifier;
		this.documentNumber = trimFillerChars(documentNumber);
		this.nationality = nationality;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.dateOfExpiry = dateOfExpiry;
		if (personalNumber == null || equalsModuloFillerChars(personalNumber, "")) {
			/* optional data field is not used */
			this.optionalData1 = "";
		} else if (personalNumber.length() == 15) {
			/* it's either a personalNumber with check digit included, or some other optional data */
			this.optionalData1 = personalNumber;
		} else if (personalNumber.length() <= 14) {
			/* we'll assume it's a personalNumber without check digit, and we add the check digit ourselves */
			this.optionalData1 = mrzFormat(personalNumber, 14) + checkDigit(personalNumber, true);
		} else {
			throw new IllegalArgumentException("Wrong personal number: " + personalNumber);
		}
		checkDigit();
	}

	/**
	 * Creates a new 3-line MRZ compliant with ICAO Doc 9303 part 3 vol 1.
	 *
	 * @param documentCode document code (1 or 2 digit, has to start with "I", "C", or "A")
	 * @param issuingState issuing state as 3 digit string
	 * @param primaryIdentifier card holder last name
	 * @param secondaryIdentifier card holder first name(s)
	 * @param documentNumber document number
	 * @param nationality nationality as 3 digit string
	 * @param dateOfBirth date of birth in YYMMDD format
	 * @param gender gender
	 * @param dateOfExpiry date of expiry in YYMMDD format
	 * @param optionalData1 optional data in line 1 of maximum length 15
	 * @param optionalData2 optional data in line 2 of maximum length 11
	 */
	public MRZInfo(String documentCode,
			String issuingState,
			String documentNumber,
			String optionalData1,
			String dateOfBirth,
			Gender gender,
			String dateOfExpiry,
			String nationality,
			String optionalData2,
			String primaryIdentifier,
			String secondaryIdentifier
			) {
		if (documentCode == null || documentCode.length() < 1 || documentCode.length() > 2
				|| !(documentCode.startsWith("C") || documentCode.startsWith("I") || documentCode.startsWith("A"))) {
			throw new IllegalArgumentException("Wrong document code: " + documentCode);
		}

		this.documentType = getDocumentTypeFromDocumentCode(documentCode);
		this.documentCode = trimFillerChars(documentCode);
		this.issuingState = issuingState;
		this.primaryIdentifier = primaryIdentifier;
		this.secondaryIdentifier = secondaryIdentifier;
		this.documentNumber = trimFillerChars(documentNumber);
		this.nationality = nationality;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.dateOfExpiry = dateOfExpiry;
		if (optionalData1 == null || optionalData1.length() > 15) { throw new IllegalArgumentException("Wrong optional data 1: " + (optionalData1 == null ? "null" : "\"" + optionalData1 + "\"")); }
		this.optionalData1 = optionalData1;
		this.optionalData2 = optionalData2;
		checkDigit();
	}

	/**
	 * Creates a new MRZ based on an input stream.
	 *
	 * @param inputStream contains the contents (value) of DG1 (without the tag and length)
	 * @param length the length of the MRZInfo structure
	 */
	public MRZInfo(InputStream inputStream, int length) {
		try {
			readObject(inputStream, length);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalArgumentException(ioe.getMessage());
		}
	}

	/**
	 * Creates a new MRZ based on the text input.
	 * The text input may contain newlines, which will be ignored.
	 * 
	 * @param str input text
	 */
	public MRZInfo(String str) {
		if (str == null) { throw new IllegalArgumentException("Null string"); }
		str = str.trim().replace("\n", "");
		try {
			readObject(new ByteArrayInputStream(str.getBytes("UTF-8")), str.length());
		} catch (UnsupportedEncodingException uee) {
			/* NOTE: never happens, UTF-8 is supported. */
			uee.printStackTrace();
			throw new IllegalStateException(uee.getMessage());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalArgumentException(ioe.getMessage());
		}
	}

	private void readObject(InputStream inputStream, int length) throws IOException {
		DataInputStream dataIn = new DataInputStream(inputStream);

		/* line 1, pos 1 to 2, Document code */
		this.documentCode = readStringWithFillers(dataIn, 2);
		this.documentType = getDocumentTypeFromDocumentCode(this.documentCode);
		switch (length) {
		case 88: this.documentType = DOC_TYPE_ID3; break;
		case 90: this.documentType = DOC_TYPE_ID1; break;
		default: this.documentType = getDocumentTypeFromDocumentCode(this.documentCode); break;
		}
		if (this.documentType == DOC_TYPE_ID1) {
			/* line 1, pos 3 to 5 Issuing State or organization */
			this.issuingState = readCountry(dataIn);

			/* line 1, pos 6 to 14 Document number */
			this.documentNumber = readString(dataIn, 9);

			/* line 1, pos 15 Check digit */
			this.documentNumberCheckDigit = (char)dataIn.readUnsignedByte();

			/* line 1, pos 16 to 30, Optional data elements */
			this.optionalData1 = readStringWithFillers(dataIn, 15);

			if (documentNumberCheckDigit == '<') {
				/* Interpret personal number as part of document number, see note j. */
				this.documentNumber += optionalData1.substring(0, optionalData1.length() - 1);
				this.documentNumberCheckDigit = optionalData1.charAt(optionalData1.length() - 1);
				this.optionalData1 = null;
			}
			this.documentNumber = trimFillerChars(this.documentNumber);

			/* line 2, pos 1 to 6, Date of birth */
			this.dateOfBirth = readDateOfBirth(dataIn);

			/* line 2, pos 7, Check digit */
			this.dateOfBirthCheckDigit = (char)dataIn.readUnsignedByte();

			/* line 2, pos 8, Sex */
			this.gender = readGender(dataIn);

			/* line 2, Pos 9 to 14, Date of expiry */
			this.dateOfExpiry = readDateOfExpiry(dataIn);

			/* line 2, pos 15, Check digit */
			this.dateOfExpiryCheckDigit = (char)dataIn.readUnsignedByte();

			/* line 2, pos 16 to 18, Nationality */
			this.nationality = readCountry(dataIn);

			/* line 2, pos 19 to 29, Optional data elements */
			this.optionalData2 = readString(dataIn, 11);

			/* line 2, pos 30, Overall check digit */
			this.compositeCheckDigit = (char)dataIn.readUnsignedByte();

			/* line 3 */
			readNameIdentifiers(readString(dataIn, 30));
		} else {
			/* Assume it's a ID3 document, i.e. 2-line MRZ. */

			/* line 1, pos 3 to 5 */
			this.issuingState = readCountry(dataIn);

			/* line 1, pos 6 to 44 */
			readNameIdentifiers(readString(dataIn, 39));

			/* line 2 */
			this.documentNumber = trimFillerChars(readString(dataIn, 9));
			this.documentNumberCheckDigit = (char)dataIn.readUnsignedByte();
			this.nationality = readCountry(dataIn);
			this.dateOfBirth = readDateOfBirth(dataIn);
			this.dateOfBirthCheckDigit = (char)dataIn.readUnsignedByte();
			this.gender = readGender(dataIn);
			this.dateOfExpiry = readDateOfExpiry(dataIn);
			this.dateOfExpiryCheckDigit = (char)dataIn.readUnsignedByte();
			String personalNumber = readStringWithFillers(dataIn, 14);
			char personalNumberCheckDigit = (char)dataIn.readUnsignedByte();
			this.optionalData1 = mrzFormat(personalNumber, 14) + personalNumberCheckDigit;
			this.compositeCheckDigit = (char)dataIn.readUnsignedByte();
		}
	}

	/**
	 * Writes the MRZ to an output stream.
	 * This just outputs the MRZ characters, and does not add newlines.
	 * 
	 * @param outputStream the output stream to write to
	 */
	public void writeObject(OutputStream outputStream) throws IOException {
		DataOutputStream dataOut = new DataOutputStream(outputStream);
		writeDocumentType(dataOut);
		if (documentType == DOC_TYPE_ID1) {
			/* Assume it's an ID1 document */

			/* top line */
			writeIssuingState(dataOut);
			if (documentNumber.length() > 9 && equalsModuloFillerChars(optionalData1, "")) {
				/*
				 * If document number has more than 9 character, the 9 principal
				 * character shall be shown in the MRZ in character positions 1 to 9.
				 * They shall be followed by a filler character instead of a check
				 * digit to indicate a truncated number. The remaining character of
				 * the document number shall be shown at the beginning of the field
				 * reserved of optional data element (character position 29 to 35 of
				 * the lower machine readable line) followed by a check digit and a
				 * filler character.
				 * 
				 * Corresponds to Doc 9303 pt 3 vol 1 page V-10 (note j) (FIXED by Paulo Assumcao)
				 * 
				 * Also see R3-p1_v2_sIV_0041 in Supplement to Doc 9303, release 11.
				 */
				writeString(documentNumber.substring(0, 9), dataOut, 9);
				dataOut.write('<'); /* NOTE: instead of check digit */
				writeString(documentNumber.substring(9, documentNumber.length()) + documentNumberCheckDigit + "<", dataOut, 15);
			} else {
				writeString(documentNumber, dataOut, 9); /* FIXME: max size of field */
				dataOut.write(documentNumberCheckDigit);
				writeString(optionalData1, dataOut, 15); /* FIXME: max size of field */
			}

			/* middle line */
			writeDateOfBirth(dataOut);
			dataOut.write(dateOfBirthCheckDigit);
			writeGender(dataOut);
			writeDateOfExpiry(dataOut);
			dataOut.write(dateOfExpiryCheckDigit);
			writeNationality(dataOut);
			writeString(optionalData2, dataOut, 11);
			dataOut.write(compositeCheckDigit);

			/* bottom line */
			writeName(dataOut, 30);
		} else {
			/* Assume it's a ID3 document */

			/* top line */
			writeIssuingState(dataOut);
			writeName(dataOut, 39);

			/* bottom line */
			writeString(documentNumber, dataOut, 9);
			dataOut.write(documentNumberCheckDigit);
			writeNationality(dataOut);
			writeDateOfBirth(dataOut);
			dataOut.write(dateOfBirthCheckDigit);
			writeGender(dataOut);
			writeDateOfExpiry(dataOut);
			dataOut.write(dateOfExpiryCheckDigit);
			writeString(optionalData1, dataOut, 15); /* NOTE: already includes check digit */
			dataOut.write(compositeCheckDigit);
		}
	}
	
	/**
	 * Gets the date of birth of the passport holder.
	 *
	 * @return date of birth
	 */
	public String getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * Sets the date of birth.
	 *
	 * @param dateOfBirth new date of birth
	 */
	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
		checkDigit();
	}

	/**
	 * Gets the date of expiry
	 *
	 * @return date of expiry
	 */
	public String getDateOfExpiry() {
		return dateOfExpiry;
	}

	/**
	 * Sets the date of expiry.
	 *
	 * @param dateOfExpiry new date of expiry
	 */
	public void setDateOfExpiry(String dateOfExpiry) {
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

	/**
	 * Sets the document number.
	 *
	 * @param documentNumber new document number
	 */
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
	 * Gets the document type.
	 *
	 * @return document type
	 */
	public String getDocumentCode() {
		return documentCode;
	}

	public void setDocumentCode(String documentCode) {
		this.documentCode = documentCode;
		this.documentType = getDocumentTypeFromDocumentCode(documentCode);
		if (documentType == DOC_TYPE_ID1 && optionalData2 == null) {
			optionalData2 = "";
		}
		/* FIXME: need to adjust some other lengths if we go from ID1 to ID3 or back... */
	}

	/**
	 * Gets the issuing state as a 3 letter code
	 *
	 * @return issuing state
	 */
	public String getIssuingState() {
		return issuingState;
	}

	/**
	 * Sets the issuing state.
	 *
	 * @param issuingState new issuing state
	 */
	public void setIssuingState(String issuingState) {
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

	/**
	 * Sets the passport holder's last name.
	 *
	 * @param primaryIdentifier new primary identifier
	 */
	public void setPrimaryIdentifier(String primaryIdentifier) {
		this.primaryIdentifier = primaryIdentifier.trim();
		checkDigit();
	}

	/**
	 * Gets the document holder's first names.
	 * 
	 * @return the secondary identifier
	 */
	public String getSecondaryIdentifier() {
		return secondaryIdentifier;
	}

	/**
	 * Gets the document holder's first names.
	 *
	 * @return first names
	 */
	public String[] getSecondaryIdentifierComponents() {
		return secondaryIdentifier.split(" |<");
	}

	/**
	 * Sets the passport holder's first names.
	 *
	 * @param secondaryIdentifiers new secondary identifiers
	 */
	public void setSecondaryIdentifierComponents(String[] secondaryIdentifiers) {
		if (secondaryIdentifiers == null) {
			this.secondaryIdentifier = null;
		} else {
			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; i < secondaryIdentifiers.length; i++) {
				stringBuffer.append(secondaryIdentifiers[i]);
				if (i < secondaryIdentifiers.length - 1) {
					stringBuffer.append('<');
				}
			}
		}
		checkDigit();
	}

	/**
	 * Sets the passport holder's first names.
	 *
	 * @param secondaryIdentifiers new secondary identifiers
	 */
	public void setSecondaryIdentifiers(String secondaryIdentifiers) {
		readSecondaryIdentifiers(secondaryIdentifiers.trim());
		checkDigit();
	}

	/**
	 * Gets the passport holder's nationality as a 3 digit code.
	 *
	 * @return a country
	 */
	public String getNationality() {
		return nationality;
	}

	/**
	 * Sets the passport holder's nationality.
	 *
	 * @param nationality new nationality
	 */
	public void setNationality(String nationality) {
		this.nationality = nationality;
		checkDigit();
	}

	/**
	 * Gets the personal number (if a personal number is encoded in optional data 1).
	 *
	 * @return personal number
	 */
	public String getPersonalNumber() {
		if (optionalData1.length() > 14) {
			return trimFillerChars(optionalData1.substring(0, 14));
		} else {
			return trimFillerChars(optionalData1);
		}
	}

	/**
	 * Sets the personal number.
	 *
	 * @param personalNumber new personal number
	 */
	public void setPersonalNumber(String personalNumber) {
		if (personalNumber == null || personalNumber.length() > 14) { throw new IllegalArgumentException("Wrong personal number"); }
		this.optionalData1 = mrzFormat(personalNumber, 14) + checkDigit(personalNumber, true);
	}

	/**
	 * Gets the contents of the first optional data field for ID-1 and ID-3 style MRZs.
	 * 
	 * @return optional data 1
	 */
	public String getOptionalData1() {
		return optionalData1;
	}

	/**
	 * Gets the contents of the second optional data field for ID-1 style MRZs.
	 * 
	 * @return optional data 2
	 */
	public String getOptionalData2() {
		return optionalData2;
	}

	/**
	 * Sets the contents for the second optional data field for ID-1 style MRZs.
	 * 
	 * @param optionalData2 optional data 2
	 */
	public void setOptionalData2(String optionalData2) {
		this.optionalData2 = trimFillerChars(optionalData2);
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

	/**
	 * Sets the gender.
	 *
	 * @param gender new gender
	 */
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
		try {
			String str = new String(getEncoded(), "UTF-8");
			switch(str.length()) {
			case 90: /* ID1 */
				return str.substring(0, 30) + "\n"
				+ str.substring(30, 60) + "\n"
				+ str.substring(60, 90) + "\n";
			case 88: /* ID3 */
				return str.substring(0, 44) + "\n"
				+ str.substring(44, 88) + "\n";
			default:
				/* TODO: consider throwing an exception in this case. */
				return str;
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			throw new IllegalStateException(uee.getMessage());
		}
	}

	/**
	 * Gets a hash code for this MRZ info.
	 *
	 * @return a hash code
	 */
	public int hashCode() {
		return 2 * toString().hashCode() + 53;
	}

	/**
	 * Whether this MRZ info is identical to the other one.
	 *
	 * @return a boolean
	 */
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (!(obj.getClass().equals(this.getClass()))) { return false; }
		MRZInfo other = (MRZInfo)obj;

		return
		((documentCode == null && other.documentCode == null) || documentCode !=  null && documentCode.equals(other.documentCode))
		&& ((issuingState == null && other.issuingState == null) || issuingState != null && issuingState.equals(other.issuingState))
		&& ((primaryIdentifier == null && other.primaryIdentifier == null) || primaryIdentifier != null && primaryIdentifier.equals(other.primaryIdentifier))
		&& ((secondaryIdentifier == null && other.secondaryIdentifier == null) || equalsModuloFillerChars(secondaryIdentifier, other.secondaryIdentifier))
		&& ((nationality == null && other.nationality == null) || nationality != null && nationality.equals(other.nationality))
		&& ((documentNumber == null && other.documentNumber == null) || documentNumber != null && documentNumber.equals(other.documentNumber))
		&& ((optionalData1 == null && other.optionalData1 == null) || optionalData1 != null && optionalData1.equals(other.optionalData1) || getPersonalNumber().equals(other.getPersonalNumber()))
		&& ((dateOfBirth == null && other.dateOfBirth == null) || dateOfBirth != null && dateOfBirth.equals(other.dateOfBirth))
		&& ((gender == null && other.gender == null) || gender != null && gender.equals(other.gender))
		&& ((dateOfExpiry == null && other.dateOfExpiry == null) || dateOfExpiry != null && dateOfExpiry.equals(other.dateOfExpiry))
		&& ((optionalData2 == null && other.optionalData2 == null) || optionalData2 != null && equalsModuloFillerChars(optionalData2, other.optionalData2))
		;
	}

	/**
	 * Computes the 7-3-1 check digit for part of the MRZ.
	 *
	 * @param str a part of the MRZ.
	 *
	 * @return the resulting check digit (in '0' - '9')
	 */
	public static char checkDigit(String str) {
		return checkDigit(str, false);
	}
	
	/* ONLY PRIVATE METHODS BELOW */

	private void readNameIdentifiers(String mrzNameString) {
		int delimIndex = mrzNameString.indexOf("<<");
		if (delimIndex < 0) {
			throw new IllegalArgumentException("Input does not contain primary identifier!");
		}
		primaryIdentifier = trimFillerChars(mrzNameString.substring(0, delimIndex));
		String rest = mrzNameString.substring(mrzNameString.indexOf("<<") + 2);
		readSecondaryIdentifiers(rest);
	}

	private void readSecondaryIdentifiers(String secondaryIdentifier) {
		this.secondaryIdentifier = secondaryIdentifier;
	}

	private void writeString(String string, DataOutputStream dataOut, int width) throws IOException {
		dataOut.write(mrzFormat(string, width).getBytes("UTF-8"));
	}

	private void writeIssuingState(DataOutputStream dataOut) throws IOException {
		dataOut.write(issuingState.getBytes("UTF-8"));
	}

	private void writeDateOfExpiry(DataOutputStream dataOut) throws IOException {
		dataOut.write(dateOfExpiry.getBytes("UTF-8"));
	}

	private void writeGender(DataOutputStream dataOut) throws IOException {
		dataOut.write(genderToString().getBytes("UTF-8"));
	}

	private void writeDateOfBirth(DataOutputStream dataOut) throws IOException {
		dataOut.write(dateOfBirth.getBytes("UTF-8"));
	}

	private void writeNationality(DataOutputStream dataOut) throws IOException {
		dataOut.write(nationality.getBytes("UTF-8"));
	}

	private void writeName(DataOutputStream dataOut, int width) throws IOException {
		dataOut.write(nameToString(width).getBytes("UTF-8"));
	}

	private void writeDocumentType(DataOutputStream dataOut) throws IOException {
		writeString(documentCode, dataOut, 2);
	}

	private String genderToString() {
		switch (gender) {
		case MALE: return "M";
		case FEMALE: return "F";
		default: return "<";
		}
	}

	private String nameToString(int width) {
		String[] primaryComponents = primaryIdentifier.split(" |<");
		String[] secondaryComponents = secondaryIdentifier.split(" |<");

		StringBuffer name = new StringBuffer();
		for (int i = 0; i < primaryComponents.length; i++) {
			String component = primaryComponents[i];
			name.append(component);
			name.append('<');
		}
		for (int i = 0; i < secondaryComponents.length; i++) {
			name.append('<');
			name.append(secondaryComponents[i]);
		}
		return mrzFormat(name.toString(), width);
	}

	private String readString(DataInputStream in, int count) throws IOException {
		byte[] data = new byte[count];
		in.readFully(data);
		return new String(data).trim();
	}

	private String readStringWithFillers(DataInputStream in, int count) throws IOException {
		return trimFillerChars(readString(in, count));
	}

	/**
	 * Reads the issuing state as a three letter string.
	 *
	 * @return a string of length 3 containing an abbreviation
	 *         of the issuing state or organization
	 *
	 * @throws IOException if something goes wrong
	 */
	private String readCountry(DataInputStream in) throws IOException {
		String dataString = readString(in, 3);
		return dataString;
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
		String genderStr = readString(in, 1);
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
	 * As only the rightmost two digits are stored,
	 * the assumption that this is a date in the recent
	 * past is made.
	 *
	 * @return the date of birth
	 *
	 * @throws IOException if something goes wrong
	 * @throws NumberFormatException if a data could not be constructed
	 */
	private String readDateOfBirth(DataInputStream in) throws IOException, NumberFormatException {
		return readString(in, 6);
	}

	/**
	 * Reads the date of expiry of this document.
	 * As only the rightmost two digits are stored,
	 * the assumption that this is a date in the near
	 * future is made.
	 *
	 * @return the date of expiry
	 *
	 * @throws IOException if something goes wrong
	 * @throws NumberFormatException if a date could not be constructed
	 */
	private String readDateOfExpiry(DataInputStream in) throws IOException, NumberFormatException {
		return readString(in, 6);
	}

	/**
	 * Reformats the input string such that it
	 * only contains ['A'-'Z'], ['0'-'9'], '<' characters
	 * by replacing other characters with '<'.
	 * Also extends to the given length by adding '<' to the right.
	 *
	 * @param str the input string
	 * @param width the (minimal) width of the result
	 *
	 * @return the reformatted string
	 */
	private static String mrzFormat(String str, int width) {
		if (str == null) { throw new IllegalArgumentException("Attempting to MRZ format null"); }
		if (str.length() > width) { throw new IllegalArgumentException("Argument too wide (" + str.length() + " > " + width + ")"); }
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
	 * Tests equality of two MRZ string while ignoring extra filler characters.
	 * 
	 * @param str1 an MRZ string
	 * @param str2 another MRZ string
	 * 
	 * @return a boolean indicating whether the strings are equal modulo filler characters
	 */
	public static boolean equalsModuloFillerChars(String str1, String str2) {
		if (str1 == str2) { return true; }
		if (str1 == null) { str1 = ""; }
		if (str2 == null) { str2 = ""; }
		int length = Math.max(str1.length(), str2.length());
		return mrzFormat(str1, length).equals(mrzFormat(str2, length));
	}

	/**
	 * Determines the document type based on the document code (the first two characters of the MRZ).
	 * 
	 * ICAO Doc 9303 part 3 vol 1 defines MRTDs with 3-line MRZs,
	 * in this case the document code starts with "A", "C", or "I"
	 * according to note j to Section 6.6 (page V-9).
	 * 
	 * ICAO Doc 9303 part 2 defines MRVs with 2-line MRZs,
	 * in this case the document code starts with "V". 
	 * 
	 * ICAO Doc 9303 part 1 vol 1 defines MRPs with 2-line MRZs,
	 * in this case the document code starts with "P"
	 * according to Section 9.6 (page IV-15).
	 * 
	 * @param documentCode a two letter code
	 *
	 * @return a document type, one of {@link #DOC_TYPE_ID1}, {@link #DOC_TYPE_ID2},
	 * 			{@link #DOC_TYPE_ID3}, or {@link #DOC_TYPE_UNSPECIFIED}
	 */
	private static int getDocumentTypeFromDocumentCode(String documentCode) {
		if (documentCode == null || documentCode.length() < 1 || documentCode.length() > 2) {
			throw new IllegalArgumentException("Was expecting 1 or 2 digit document code, got " + documentCode);
		}
		if (documentCode.startsWith("A")
				|| documentCode.startsWith("C")
				|| documentCode.startsWith("I")) {
			/* MRTD according to ICAO Doc 9303 part 3 vol 1 */
			return DOC_TYPE_ID1;
		} else if (documentCode.startsWith("V")) {
			/* MRV according to ICAO Doc 9303 part 2 */
			return DOC_TYPE_ID1;
		} else if (documentCode.startsWith("P")) {
			/* MRP according to ICAO Doc 9303 part 1 vol 1 */
			return DOC_TYPE_ID3;
		}
		return DOC_TYPE_UNSPECIFIED;
	}

	/**
	 * Replaces '<' with ' ' and trims leading and trailing whitespace.
	 *
	 * @param str
	 * @return trimmed string
	 */
	private static String trimFillerChars(String str) {
		byte[] chars = str.trim().getBytes();
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '<') { chars[i] = ' '; }
		}
		return (new String(chars)).trim();
	}

	/**
	 * Updates the check digit fields for document number,
	 * date of birth, date of expiry, and personal number.
	 */
	private void checkDigit() {
		this.documentNumberCheckDigit = checkDigit(documentNumber);
		this.dateOfBirthCheckDigit = checkDigit(dateOfBirth);
		this.dateOfExpiryCheckDigit = checkDigit(dateOfExpiry);
		if (optionalData1.length() < 15) {
			String personalNumber = mrzFormat(optionalData1, 14);
			char personalNumberCheckDigit = checkDigit(mrzFormat(optionalData1, 14), true); /* FIXME: Uses '<' over '0'. Where specified? */
			optionalData1 = personalNumber + personalNumberCheckDigit;
		}
		StringBuffer composite = new StringBuffer();
		if (documentType == DOC_TYPE_ID1) {
			/*
			 * Based on 6.6 in Part V of Doc 9303 Part 3 Vol 1.
			 * Composite check digit in position 30 is computed over:
			 * 
			 * Upper line:
			 * 6-30, i.e., documentNumber, documentNumberCheckDigit, personalNumber(15)
			 * 
			 * Middle line:
			 * 1-7, i.e., dateOfBirth, dateOfBirthCheckDigit
			 * 9-15, i.e., dateOfExpiry, dateOfExpiryCheckDigit
			 * 19-29, i.e., optionalData2(11)
			 */
			composite.append(documentNumber);
			composite.append(documentNumberCheckDigit);
			composite.append(mrzFormat(optionalData1, 15));
			composite.append(dateOfBirth);
			composite.append(dateOfBirthCheckDigit);
			composite.append(dateOfExpiry);
			composite.append(dateOfExpiryCheckDigit);
			composite.append(mrzFormat(optionalData2, 11));
		} else {
			composite.append(documentNumber);
			composite.append(documentNumberCheckDigit);
			composite.append(dateOfBirth);
			composite.append(dateOfBirthCheckDigit);
			composite.append(dateOfExpiry);
			composite.append(dateOfExpiryCheckDigit);
			composite.append(mrzFormat(optionalData1, 15));
		}
		this.compositeCheckDigit = checkDigit(composite.toString()); /* FIXME: Uses '0' over '<'. Where specified? */
	}

	/**
	 * Computes the 7-3-1 check digit for part of the MRZ.
	 * If <code>preferFillerOverZero</code> is <code>true</code> then '<' will be
	 * returned on check digit 0.
	 *
	 * @param str a part of the MRZ.
	 *
	 * @return the resulting check digit (in '0' - '9', '<')
	 */
	private static char checkDigit(String str, boolean preferFillerOverZero) {
		try {
			byte[] chars = str == null ? new byte[]{ } : str.getBytes("UTF-8");
			int[] weights = { 7, 3, 1 };
			int result = 0;
			for (int i = 0; i < chars.length; i++) {
				result = (result + weights[i % 3] * decodeMRZDigit(chars[i])) % 10;
			}
			String checkDigitString = Integer.toString(result);
			if (checkDigitString.length() != 1) { throw new IllegalStateException("Error in computing check digit."); /* NOTE: Never happens. */ }
			char checkDigit = (char)checkDigitString.getBytes("UTF-8")[0];
			if (preferFillerOverZero && checkDigit == '0') { checkDigit = '<'; }
			return checkDigit;
		} catch (NumberFormatException nfe) {
			/* NOTE: never happens. */
			nfe.printStackTrace();
			throw new IllegalStateException("Error in computing check digit.");
		} catch (UnsupportedEncodingException usee) {
			/* NOTE: never happens. */
			usee.printStackTrace();
			throw new IllegalStateException("Error in computing check digit.");
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
}
