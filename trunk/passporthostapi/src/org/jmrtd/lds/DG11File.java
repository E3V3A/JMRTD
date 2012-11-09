/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sourceforge.scuba.tlv.TLVInputStream;
import net.sourceforge.scuba.tlv.TLVOutputStream;
import net.sourceforge.scuba.util.Hex;

/**
 * File structure for the EF_DG11 file.
 * Datagroup 11 contains additional personal detail(s).
 * 
 * All fields are optional. See Section 16 of LDS-TR.
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
 * @version $Revision$
 */
public class DG11File extends DataGroup {

	private static final long serialVersionUID = 8566312538928662937L;

	public static final int TAG_LIST_TAG = 0x5C,
			FULL_NAME_TAG = 0x5F0E,
			OTHER_NAME_TAG = 0x5F0F,
			PERSONAL_NUMBER_TAG = 0x5F10,
			FULL_DATE_OF_BIRTH_TAG = 0x5F2B, // In 'CCYYMMDD' format.
			PLACE_OF_BIRTH_TAG = 0x5F11, // Fields separated by '<'
			PERMANENT_ADDRESS_TAG = 0x5F42, // Fields separated by '<'
			TELEPHONE_TAG = 0x5F12,
			PROFESSION_TAG = 0x5F13,
			TITLE_TAG = 0x5F14,
			PERSONAL_SUMMARY_TAG = 0x5F15,
			PROOF_OF_CITIZENSHIP_TAG = 0x5F16, // Compressed image per ISO/IEC 10918 
			OTHER_VALID_TD_NUMBERS_TAG = 0x5F17, // Separated by '<'
			CUSTODY_INFORMATION_TAG = 0x5F18,
			CONTENT_SPECIFIC_CONSTRUCTED_TAG = 0xA0, // 5F0F is always used inside A0 constructed object
			COUNT_TAG = 0x02; // Used in A0 constructed object to indicate single byte count of simple objects

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private String fullNamePrimaryIdentifier;
	private List<String> fullNameSecondaryIdentifiers;
	private List<String> otherNames;
	private String personalNumber;
	private Date fullDateOfBirth;
	private List<String> placeOfBirth;
	private List<String> permanentAddress;
	private String telephone;
	private String profession;
	private String title;
	private String personalSummary;
	private byte[] proofOfCitizenship;
	private List<String> otherValidTDNumbers;
	private String custodyInformation;

	private List<Integer> tagPresenceList;

	/**
	 * Constructs a new file. Use <code>null</code> if data element is not present.
	 * Use <code>'<'</code> as separator.
	 *
	 * @param fullNamePrimaryIdentifier data element
	 * @param fullNamesecondaryIdentifiers data element
	 * @param otherNames data element
	 * @param personalNumber data element
	 * @param fullDateOfBirth data element
	 * @param placeOfBirth data element
	 * @param permanentAddress data element
	 * @param telephone data element
	 * @param profession data element
	 * @param title data element
	 * @param personalSummary data element
	 * @param proofOfCitizenship data element
	 * @param otherValidTDNumbers data element
	 * @param custodyInformation data element
	 */
	public DG11File(String fullNamePrimaryIdentifier,
			List<String> fullNamesecondaryIdentifiers,
			List<String> otherNames, String personalNumber,
			Date fullDateOfBirth, List<String> placeOfBirth, List<String> permanentAddress,
			String telephone, String profession, String title,
			String personalSummary, byte[] proofOfCitizenship,
			List<String> otherValidTDNumbers, String custodyInformation) {
		super(EF_DG11_TAG);
		this.fullNamePrimaryIdentifier = fullNamePrimaryIdentifier;
		this.fullNameSecondaryIdentifiers = fullNameSecondaryIdentifiers == null ? new ArrayList<String>() : new ArrayList<String>(fullNamesecondaryIdentifiers);
		this.otherNames = otherNames == null ? new ArrayList<String>() : new ArrayList<String>(otherNames);
		this.personalNumber = personalNumber;
		this.fullDateOfBirth = fullDateOfBirth;
		this.placeOfBirth = placeOfBirth == null ? new ArrayList<String>() : new ArrayList<String>(placeOfBirth);
		this.permanentAddress = permanentAddress;
		this.telephone = telephone;
		this.profession = profession;
		this.title = title;
		this.personalSummary = personalSummary;
		this.proofOfCitizenship = proofOfCitizenship; // FIXME: deep copy
		this.otherValidTDNumbers = otherValidTDNumbers == null ? new ArrayList<String>() : new ArrayList<String>(otherValidTDNumbers);
		this.custodyInformation = custodyInformation;
	}

	/**
	 * Constructs a file from binary representation.
	 * 
	 * @param in an input stream
	 * 
	 * @throws IOException if reading fails
	 */
	public DG11File(InputStream in) throws IOException {
		super(EF_DG11_TAG, in);
	}

	protected void readContent(InputStream in) throws IOException {
		TLVInputStream tlvIn = in instanceof TLVInputStream ? (TLVInputStream)in : new TLVInputStream(in);
		int tag = tlvIn.readTag();
		if (tag != TAG_LIST_TAG) { throw new IllegalArgumentException("Expected tag list in DG11"); }
		int length = tlvIn.readLength();
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

	private void readField(int fieldTag, TLVInputStream tlvIn) throws IOException {
		int tag = tlvIn.readTag();
		if (tag == CONTENT_SPECIFIC_CONSTRUCTED_TAG) {
			/* int contentSpecificLength = */ tlvIn.readLength();
			int countTag = tlvIn.readTag();
			if (countTag != COUNT_TAG) { throw new IllegalArgumentException("Expected " + Integer.toHexString(COUNT_TAG) + ", found " + Integer.toHexString(countTag)); }
			int countLength = tlvIn.readLength();
			if (countLength != 1) { throw new IllegalArgumentException("Expected length 1 count length, found " + countLength); }
			byte[] countValue = tlvIn.readValue();
			if (countValue == null || countValue.length != 1) { throw new IllegalArgumentException("Number of content specific fields should be encoded in single byte, found " + countValue); }
			int count = countValue[0] & 0xFF;
			for (int i = 0; i < count; i++) {
				tag = tlvIn.readTag();
				if (tag != OTHER_NAME_TAG) { throw new IllegalArgumentException("Expected " + Integer.toHexString(OTHER_NAME_TAG) + ", found " + Integer.toHexString(tag)); }
				/* int otherNameLength = */ tlvIn.readLength();
				byte[] value = tlvIn.readValue();
				parseOtherName(new String(value));
			}
		} else {
			if (tag != fieldTag) { throw new IllegalArgumentException("Expected " + Integer.toHexString(fieldTag) + ", but found " + Integer.toHexString(tag)); }
			tlvIn.readLength();
			byte[] value = tlvIn.readValue();
			switch (tag) {
			case FULL_NAME_TAG: parseFullName(new String(value, "UTF-8")); break;
			case OTHER_NAME_TAG: parseOtherName(new String(value, "UTF-8")); break;
			case PERSONAL_NUMBER_TAG: parsePersonalNumber(new String(value, "UTF-8")); break;
			case FULL_DATE_OF_BIRTH_TAG:
				if (value.length == 8) {
					/* NOTE: Belgian encoding */
					parseFullDateOfBirth(new String(value, "UTF-8")); break;
				} else {
					/* NOTE: French encoding */
					parseFullDateOfBirth(Hex.bytesToHexString(value));
				}
				break;
			case PLACE_OF_BIRTH_TAG: parsePlaceOfBirth(new String(value, "UTF-8")); break;
			case PERMANENT_ADDRESS_TAG:  parsePermanentAddress(new String(value, "UTF-8")); break;
			case TELEPHONE_TAG: parseTelephone(new String(value, "UTF-8")); break;
			case PROFESSION_TAG: parseProfession(new String(value, "UTF-8")); break;
			case TITLE_TAG: parseTitle(new String(value, "UTF-8")); break;
			case PERSONAL_SUMMARY_TAG: parsePersonalSummary(new String(value, "UTF-8")); break;
			case PROOF_OF_CITIZENSHIP_TAG: parseProofOfCitizenShip(value); break;
			case OTHER_VALID_TD_NUMBERS_TAG: parseOtherValidTDNumbers(new String(value, "UTF-8")); break;
			case CUSTODY_INFORMATION_TAG: parseCustodyInformation(new String(value, "UTF-8")); break;
			default: throw new IllegalArgumentException("Unknown field tag in DG11: " + Integer.toHexString(tag));
			}
		}
	}

	/* Field parsing and interpretation below. */

	private void parseCustodyInformation(String in) {
		//		custodyInformation = in.replace("<", " ").trim();
		custodyInformation = in.trim();
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
		proofOfCitizenship = value;
	}

	private void parsePersonalSummary(String in) {
		//		personalSummary = in.replace("<", " ").trim();
		personalSummary = in.trim();
	}

	private void parseTitle(String in) {
		//		title = in.replace("<", " ").trim();
		title = in.trim();
	}

	private void parseProfession(String in) {
		//		profession = in.replace("<", " ").trim();
		profession = in.trim();
	}

	private void parseTelephone(String in) {
		//		telephone = in.replace("<", " ").trim();
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
			// in = in.replace("<", " ").trim();
			fullDateOfBirth = SDF.parse(in);
		} catch (ParseException pe) {
			throw new IllegalArgumentException(pe.toString());
		}

	}

	private synchronized void parseOtherName(String in) {
		if (otherNames == null) { otherNames = new ArrayList<String>(); }
		otherNames.add(in.trim());
	}

	private void parsePersonalNumber(String in) {
		personalNumber = in.trim();
	}

	private void parseFullName(String in) {
		String delim = "<<";
		int delimIndex = in.indexOf(delim);
		if (delimIndex < 0) {
			LOGGER.warning("Input does not contain primary identifier delimited by \"<<\", input was \"" + in + "\"");
			delim = " "; /* NOTE: Some passports (Belgian 1st generation) uses space?!? */
			delimIndex = in.indexOf(delim);
		}
		fullNamePrimaryIdentifier = in.substring(0, delimIndex);
		StringTokenizer st = new StringTokenizer(in.substring(in.indexOf(delim) + delim.length()), "<");
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
	 * Gets list of tags of fields actually present.
	 * 
	 * @return list of tags
	 */
	public List<Integer> getTagPresenceList() {
		if (tagPresenceList != null) { return tagPresenceList; }
		tagPresenceList = new ArrayList<Integer>(12);
		if (fullNamePrimaryIdentifier != null
				|| (fullNameSecondaryIdentifiers != null && fullNameSecondaryIdentifiers.size() > 0)) {
			tagPresenceList.add(FULL_NAME_TAG);
		}
		if (otherNames != null && otherNames.size() > 0) {
			tagPresenceList.add(OTHER_NAME_TAG);
		}
		if (personalNumber != null) {
			tagPresenceList.add(PERSONAL_NUMBER_TAG);
		}
		if (fullDateOfBirth != null) {
			tagPresenceList.add(FULL_DATE_OF_BIRTH_TAG);
		}
		if (placeOfBirth != null && placeOfBirth.size() > 0) {
			tagPresenceList.add(PLACE_OF_BIRTH_TAG);
		}
		if (permanentAddress != null && permanentAddress.size() > 0) {
			tagPresenceList.add(PERMANENT_ADDRESS_TAG);
		}
		if (telephone != null) {
			tagPresenceList.add(TELEPHONE_TAG);
		}
		if (profession != null) {
			tagPresenceList.add(PROFESSION_TAG);
		}
		if (title != null) {
			tagPresenceList.add(TITLE_TAG);
		}
		if (personalSummary != null) {
			tagPresenceList.add(PERSONAL_SUMMARY_TAG);
		}
		if (proofOfCitizenship != null) {
			tagPresenceList.add(PROOF_OF_CITIZENSHIP_TAG);
		}
		if (otherValidTDNumbers != null && otherValidTDNumbers.size() > 0) {
			tagPresenceList.add(OTHER_VALID_TD_NUMBERS_TAG);
		}
		if (custodyInformation != null) {
			tagPresenceList.add(CUSTODY_INFORMATION_TAG);
		}
		return tagPresenceList;
	}

	/**
	 * Gets the full name primary identifier.
	 * 
	 * @return the fullNamePrimaryIdentifier
	 */
	public String getFullNamePrimaryIdentifier() {
		return fullNamePrimaryIdentifier;
	}

	/**
	 * Gets the full name secondary identifiers.
	 * 
	 * @return the fullNamesecondaryIdentifiers
	 */
	public List<String> getFullNameSecondaryIdentifiers() {
		return fullNameSecondaryIdentifiers;
	}

	/**
	 * Gets the other names.
	 * 
	 * @return the other names, or empty list when not present
	 */
	public List<String> getOtherNames() {
		return otherNames == null ? new ArrayList<String>() : new ArrayList<String>(otherNames);
	}

	/**
	 * Gets the personal number.
	 * 
	 * @return the personalNumber
	 */
	public String getPersonalNumber() {
		return personalNumber;
	}

	/**
	 * Gets the full date of birth.
	 * 
	 * @return the fullDateOfBirth
	 */
	public Date getFullDateOfBirth() {
		return fullDateOfBirth;
	}

	/**
	 * Gets the place of birth.
	 * 
	 * @return the placeOfBirth
	 */
	public List<String> getPlaceOfBirth() {
		return placeOfBirth;
	}

	/**
	 * Gets the permanent address.
	 * 
	 * @return the permanentAddress
	 */
	public List<String> getPermanentAddress() {
		return permanentAddress;
	}

	/**
	 * Gets the telephone number.
	 * 
	 * @return the telephone
	 */
	public String getTelephone() {
		return telephone;
	}

	/**
	 * Gets the profession.
	 * 
	 * @return the profession
	 */
	public String getProfession() {
		return profession;
	}

	/**
	 * Gets the title.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Gets the personal summary.
	 * 
	 * @return the personalSummary
	 */
	public String getPersonalSummary() {
		return personalSummary;
	}

	/**
	 * Gets the proof of citizenship.
	 * 
	 * @return the proofOfCitizenship
	 */
	public byte[] getProofOfCitizenship() {
		return proofOfCitizenship;
	}

	/**
	 * Gets the other valid TD numbers.
	 * 
	 * @return the otherValidTDNumbers
	 */
	public List<String> getOtherValidTDNumbers() {
		return otherValidTDNumbers;
	}

	/**
	 * Gets custody information.
	 * 
	 * @return the custodyInformation
	 */
	public String getCustodyInformation() {
		return custodyInformation;
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("DG11File [");
		result.append(fullNamePrimaryIdentifier == null ? "" : fullNamePrimaryIdentifier); result.append(", ");
		result.append(fullNameSecondaryIdentifiers == null || fullNameSecondaryIdentifiers.size() == 0 ? "[]" : fullNameSecondaryIdentifiers.toString()); result.append(", ");
		result.append(otherNames == null || otherNames.size() == 0 ? "[]" : otherNames); result.append(", ");
		result.append(personalNumber == null ? "" : personalNumber); result.append(", ");
		result.append(fullDateOfBirth == null ? "" : SDF.format(fullDateOfBirth)); result.append(", ");
		result.append(placeOfBirth == null || placeOfBirth.size() == 0 ? "[]" : placeOfBirth.toString()); result.append(", ");
		result.append(permanentAddress == null || permanentAddress.size() == 0 ? "[]" : permanentAddress.toString()); result.append(", ");
		result.append(telephone == null ? "" : telephone); result.append(", ");
		result.append(profession == null ? "" : profession); result.append(", ");
		result.append(title == null ? "" : title); result.append(", ");
		result.append(personalSummary == null ? "" : personalSummary); result.append(", ");
		result.append(proofOfCitizenship == null ? "" : "image (" + proofOfCitizenship.length + ")"); result.append(", ");
		result.append(otherValidTDNumbers == null || otherValidTDNumbers.size() == 0 ? "[]" : otherValidTDNumbers.toString()); result.append(", ");
		result.append(custodyInformation == null ? "" : custodyInformation);
		result.append("]");
		return result.toString();
	}

	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (!obj.getClass().equals(this.getClass())) { return false; }
		DG11File other = (DG11File)obj;
		return this.toString().equals(other.toString());
	}

	public int hashCode() {
		return 13 * toString().hashCode() + 111;
	}

	protected void writeContent(OutputStream out) throws IOException {
		TLVOutputStream tlvOut = out instanceof TLVOutputStream ? (TLVOutputStream)out : new TLVOutputStream(out);
		tlvOut.writeTag(TAG_LIST_TAG);
		DataOutputStream dataOut = new DataOutputStream(tlvOut);
		List<Integer> tags = getTagPresenceList();
		for (int tag: tags) {
			dataOut.writeShort(tag);
		}
		dataOut.flush();
		tlvOut.writeValueEnd(); /* TAG_LIST_TAG */
		for (int tag: tags) {
			switch (tag) {
			case FULL_NAME_TAG:
				tlvOut.writeTag(tag);
				if (fullNamePrimaryIdentifier !=  null) {
					//					out.write(fullNamePrimaryIdentifier.trim().replace(' ', '<').getBytes("UTF-8"));
					tlvOut.write(fullNamePrimaryIdentifier.trim().getBytes("UTF-8"));
				}
				tlvOut.write("<<".getBytes("UTF-8"));
				boolean isFirstOne = true;
				if (fullNameSecondaryIdentifiers != null) {
					for (String secondaryPrimaryIdentifier: fullNameSecondaryIdentifiers) {
						if (secondaryPrimaryIdentifier != null) {
							if (isFirstOne) { isFirstOne = false; } else { tlvOut.write('<'); }
							// tlvOut.write(secondaryPrimaryIdentifier.trim().replace(' ', '<').getBytes("UTF-8"));
							tlvOut.write(secondaryPrimaryIdentifier.trim().getBytes("UTF-8"));
						}
					}
				}
				tlvOut.writeValueEnd(); /* FULL_NAME_TAG */
				break;
			case OTHER_NAME_TAG:
				if (otherNames == null) { otherNames = new ArrayList<String>(); }
				tlvOut.writeTag(CONTENT_SPECIFIC_CONSTRUCTED_TAG);
				tlvOut.writeTag(COUNT_TAG);
				tlvOut.write(otherNames.size());
				tlvOut.writeValueEnd(); /* COUNT_TAG */
				for (String otherName: otherNames) {
					tlvOut.writeTag(OTHER_NAME_TAG);
					tlvOut.writeValue(otherName.trim().replace(' ', '<').getBytes("UTF-8"));
				}
				tlvOut.writeValueEnd(); /* CONTENT_SPECIFIC_CONSTRUCTED_TAG */
				break; 	
			case PERSONAL_NUMBER_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(personalNumber.trim().getBytes("UTF-8"));
				break;
			case FULL_DATE_OF_BIRTH_TAG:
				tlvOut.writeTag(tag);
				String fullDateOfBirthString = SDF.format(fullDateOfBirth);
				// byte[] fullDateOfBirthBytes = fullDateOfBirthString.getBytes("UTF-8");
				byte[] fullDateOfBirthBytes = Hex.hexStringToBytes(fullDateOfBirthString);
				tlvOut.writeValue(fullDateOfBirthBytes);		
				break;
			case PLACE_OF_BIRTH_TAG:
				tlvOut.writeTag(tag);
				isFirstOne = true;
				for (String detail: placeOfBirth) {
					if (detail != null) {
						if (isFirstOne) { isFirstOne = false; } else { tlvOut.write('<'); }
						//	tlvOut.write(detail.trim().replace(' ', '<').getBytes("UTF-8"));
						tlvOut.write(detail.trim().getBytes("UTF-8"));
					}
				}
				tlvOut.writeValueEnd(); /* PLACE_OF_BIRTH_TAG */
				break;
			case PERMANENT_ADDRESS_TAG:
				tlvOut.writeTag(tag);
				isFirstOne = true;
				for (String detail: permanentAddress) {
					if (detail != null) {
						if (isFirstOne) { isFirstOne = false; } else { tlvOut.write('<'); }
						//	tlvOut.write(detail.trim().replace(' ', '<').getBytes("UTF-8"));
						tlvOut.write(detail.trim().getBytes("UTF-8"));
					}
				}
				tlvOut.writeValueEnd(); /* PERMANENT_ADDRESS_TAG */
				break;
			case TELEPHONE_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(telephone.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			case PROFESSION_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(profession.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			case TITLE_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(title.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			case PERSONAL_SUMMARY_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(personalSummary.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			case PROOF_OF_CITIZENSHIP_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(proofOfCitizenship);
				break;
			case OTHER_VALID_TD_NUMBERS_TAG:
				tlvOut.writeTag(tag);
				isFirstOne = true;
				for (String detail: otherValidTDNumbers) {
					if (detail != null) {
						if (isFirstOne) { isFirstOne = false; } else { tlvOut.write('<'); }
						tlvOut.write(detail.trim().replace(' ', '<').getBytes("UTF-8"));
					}
				}
				tlvOut.writeValueEnd(); /* OTHER_VALID_TD_NUMBERS_TAG */
				break;
			case CUSTODY_INFORMATION_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(custodyInformation.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			default: throw new IllegalStateException("Unknown tag in DG11: " + Integer.toHexString(tag));
			}
		}
	}
}
