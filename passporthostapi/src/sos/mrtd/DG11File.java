/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import sos.tlv.BERTLVInputStream;

/**
 * File structure for the EF_DG11 file.
 * Datagroup 11 contains additional personal detail(s).
 * 
 * All fields are optional.
 * <ol>
 * <li>Name of Holder (Primary and Secondary Identifiers, in full)</li>
 * <li>Other Name(s)</li>
 * <li>Personal Number</li>
 * <li>Place of Birth</li>
 * <li>Date of Birth (in full)</li>
 * <li>Address</li>
 * <li>Telephone Number(s)</li>
 * <li>Profession</li>
 * <li>Title</li>
 * <li>Personal Summary</li>
 * <li>Proof of Citizenship [see 14.5.1]</li>
 * <li>Number of Other Valid Travel Documents</li>
 * <li>Other Travel Document Numbers</li>
 * <li>Custody Information</li>
 * </ol>
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: $
 */
public class DG11File extends DataGroup
{
	private static final int TAG_LIST_TAG = 0x5C;
	private static final int FULL_NAME_TAG = 0x5F0E;
	private static final int PERSONAL_NUMBER_TAG = 0x5F10;
	private static final int FULL_DATE_OF_BIRTH_TAG = 0x5F2B;
	private static final int PLACE_OF_BIRTH_TAG = 0x5F11; // Fields separated by ‘<’
	private static final int PERMANENT_ADDRESS_TAG = 0x5F42; // Fields separated by ‘<’
	private static final int TELEPHONE_TAG = 0x5F12;
	private static final int PROFESSION_TAG = 0x5F13;
	private static final int TITLE_TAG = 0x5F14;
	private static final int PERSONAL_SUMMARY_TAG = 0x5F15;
	private static final int PROOF_OF_CITIZENSHIP_TAG = 0x5F16; // Compressed image per ISO/IEC 10918 
	private static final int OTHER_VALID_TD_NUMBERS_TAG = 0x5F17; // Separated by ‘<’
	private static final int CUSTODY_INFORMATION_TAG = 0x5F18;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private String fullNamePrimaryIdentifier;
	private List<String> fullNameSecondaryIdentifiers;
	private String personalNumber;
	private Date fullDateOfBirth;
	private List<String> placeOfBirth;
	private List<String> permanentAddress;
	private String telephone;
	private String profession;
	private String title;
	private String personalSummary;
	private BufferedImage proofOfCitizenship;
	private List<String> otherValidTDNumbers;
	private String custodyInformation;

	/**
	 * Constructs a new file.
	 *
	 * @param fullNamePrimaryIdentifier
	 * @param fullNamesecondaryIdentifiers
	 * @param personalNumber
	 * @param fullDateOfBirth
	 * @param placeOfBirth
	 * @param permanentAddress
	 * @param telephone
	 * @param profession
	 * @param title
	 * @param personalSummary
	 * @param proofOfCitizenship
	 * @param otherValidTDNumbers
	 * @param custodyInformation
	 */
	public DG11File(String fullNamePrimaryIdentifier,
			List<String> fullNamesecondaryIdentifiers, String personalNumber,
			Date fullDateOfBirth, List<String> placeOfBirth, List<String> permanentAddress,
			String telephone, String profession, String title,
			String personalSummary, BufferedImage proofOfCitizenship,
			List<String> otherValidTDNumbers, String custodyInformation) {
		super();
		this.fullNamePrimaryIdentifier = fullNamePrimaryIdentifier;
		this.fullNameSecondaryIdentifiers = fullNamesecondaryIdentifiers;
		this.personalNumber = personalNumber;
		this.fullDateOfBirth = fullDateOfBirth;
		this.placeOfBirth = placeOfBirth;
		this.permanentAddress = permanentAddress;
		this.telephone = telephone;
		this.profession = profession;
		this.title = title;
		this.personalSummary = personalSummary;
		this.proofOfCitizenship = proofOfCitizenship;
		this.otherValidTDNumbers = otherValidTDNumbers;
		this.custodyInformation = custodyInformation;
	}

	public DG11File(InputStream in) throws IOException {
		BERTLVInputStream tlvIn = new BERTLVInputStream(in);
		int tag = tlvIn.readTag();
		if (tag != PassportFile.EF_DG11_TAG) { throw new IllegalArgumentException("Expected EF_DG11_TAG"); }
		int length = tlvIn.readLength();
		tag = tlvIn.readTag();
		if (tag != TAG_LIST_TAG) { throw new IllegalArgumentException("Expected tag list in DG11"); }
		length = tlvIn.readLength();
		int tagCount = length / 2;
		int[] tagList = new int[tagCount];
		for (int i = 0; i < tagCount; i++) {
			int hi = tlvIn.read();
			int lo = tlvIn.read();
			tag = ((hi & 0xFF) << 8) | (lo & 0xFF);
			tagList[i] = tag;
		}
		for (int i = 0; i < tagCount; i++) {
			readField(tagList[i], tlvIn);
		}
	}

	private void readField(int fieldTag, BERTLVInputStream tlvIn) throws IOException {
		int tag = tlvIn.readTag();
		if (tag != fieldTag) { throw new IllegalArgumentException("Expected " + Integer.toHexString(fieldTag) + ", but found " + Integer.toHexString(tag)); }
		int length = tlvIn.readLength();
		byte[] value = tlvIn.readValue();
		switch (tag) {
		case FULL_NAME_TAG: parseFullName(new String(value)); break;
		case PERSONAL_NUMBER_TAG: parsePersonalNumber(new String(value)); break;
		case FULL_DATE_OF_BIRTH_TAG: parseFullDateOfBirth(new String(value)); break;
		case PLACE_OF_BIRTH_TAG: parsePlaceOfBirth(new String(value)); break;
		case PERMANENT_ADDRESS_TAG: parsePermanentAddress(new String(value)); break;
		case TELEPHONE_TAG: parseTelephone(new String(value)); break;
		case PROFESSION_TAG: parseProfession(new String(value)); break;
		case TITLE_TAG: parseTitle(new String(value)); break;
		case PERSONAL_SUMMARY_TAG: parsePersonalSummary(new String(value)); break;
		case PROOF_OF_CITIZENSHIP_TAG: parseProofOfCitizenShip(value); break;
		case OTHER_VALID_TD_NUMBERS_TAG: parseOtherValidTDNumbers(new String(value)); break;
		case CUSTODY_INFORMATION_TAG: parseCustodyInformation(new String(value)); break;
		default: throw new IllegalArgumentException("Unknown field tag in DG11: " + Integer.toHexString(tag));
		}
	}

	/* Field parsing and interpretation below. */
	
	private void parseCustodyInformation(String in) {
		custodyInformation = in.replace("<", " ").trim();
	}

	private void parseOtherValidTDNumbers(String in) {
		otherValidTDNumbers = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(in, "<");
		while (st.hasMoreTokens()) {
			String number = st.nextToken().trim();
			otherValidTDNumbers.add(number);
		}
	}

	private void parseProofOfCitizenShip(byte[] value) {
		proofOfCitizenship = null;
		// FIXME: parse jpeg.
	}

	private void parsePersonalSummary(String in) {
		personalSummary = in.replace("<", " ").trim();
	}

	private void parseTitle(String in) {
		title = in.replace("<", " ").trim();
	}

	private void parseProfession(String in) {
		profession = in.replace("<", " ").trim();
	}

	private void parseTelephone(String in) {
		telephone = in.replace("<", " ").trim();
	}

	private void parsePermanentAddress(String in) {
		StringTokenizer st = new StringTokenizer(in, "<");
		permanentAddress = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String line = st.nextToken().trim();
			permanentAddress.add(line);
		}
	}

	private void parsePlaceOfBirth(String in) {
		StringTokenizer st = new StringTokenizer(in, "<");
		placeOfBirth = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String line = st.nextToken().trim();
			placeOfBirth.add(line);
		}
	}

	private void parseFullDateOfBirth(String in) {
		try {
			fullDateOfBirth = SDF.parse(in.replace("<", " ").trim());
		} catch (ParseException pe) {
			throw new IllegalArgumentException(pe.toString());
		}

	}

	private void parsePersonalNumber(String in) {
		personalNumber = in.trim();
	}

	private void parseFullName(String in) {
		int delimIndex = in.indexOf("<<");
		if (delimIndex < 0) {
			throw new IllegalArgumentException("Input does not contain primary identifier!");
		}
		fullNamePrimaryIdentifier = in.substring(0, delimIndex).replace("<", " ");
		StringTokenizer st = new StringTokenizer(in.substring(in.indexOf("<<") + 2), "<");
		fullNameSecondaryIdentifiers = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String secondaryIdentifier = st.nextToken().trim();
			fullNameSecondaryIdentifiers.add(secondaryIdentifier);
		}
	}
	
	/* Accessors below. */

	public int getTag() {
		return EF_DG11_TAG;
	}

	/**
	 * @return the fullNamePrimaryIdentifier
	 */
	public String getFullNamePrimaryIdentifier() {
		return fullNamePrimaryIdentifier;
	}

	/**
	 * @return the fullNamesecondaryIdentifiers
	 */
	public List<String> getFullNamesecondaryIdentifiers() {
		return fullNameSecondaryIdentifiers;
	}

	/**
	 * @return the personalNumber
	 */
	public String getPersonalNumber() {
		return personalNumber;
	}

	/**
	 * @return the fullDateOfBirth
	 */
	public Date getFullDateOfBirth() {
		return fullDateOfBirth;
	}

	/**
	 * @return the placeOfBirth
	 */
	public List<String> getPlaceOfBirth() {
		return placeOfBirth;
	}

	/**
	 * @return the permanentAddress
	 */
	public List<String> getPermanentAddress() {
		return permanentAddress;
	}

	/**
	 * @return the telephone
	 */
	public String getTelephone() {
		return telephone;
	}

	/**
	 * @return the profession
	 */
	public String getProfession() {
		return profession;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the personalSummary
	 */
	public String getPersonalSummary() {
		return personalSummary;
	}

	/**
	 * @return the proofOfCitizenship
	 */
	public BufferedImage getProofOfCitizenship() {
		return proofOfCitizenship;
	}

	/**
	 * @return the otherValidTDNumbers
	 */
	public List<String> getOtherValidTDNumbers() {
		return otherValidTDNumbers;
	}

	/**
	 * @return the custodyInformation
	 */
	public String getCustodyInformation() {
		return custodyInformation;
	}

	public String toString() {
		return "DG11File";
	}

	/**
	 * TODO: in progress.
	 */
	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject.getEncoded();
		}
		return null;
	}
}
