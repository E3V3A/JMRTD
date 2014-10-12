/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.scuba.tlv.TLVInputStream;
import net.sf.scuba.tlv.TLVOutputStream;
import net.sf.scuba.tlv.TLVUtil;
import net.sf.scuba.util.Hex;

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
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 */
public class DG11File extends DataGroup {

	private static final long serialVersionUID = 8566312538928662937L;

	public static final int TAG_LIST_TAG = 0x5C;

	public static final int
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
	CUSTODY_INFORMATION_TAG = 0x5F18;

	public static final int
	CONTENT_SPECIFIC_CONSTRUCTED_TAG = 0xA0, // 5F0F is always used inside A0 constructed object
	COUNT_TAG = 0x02; // Used in A0 constructed object to indicate single byte count of simple objects

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd.lds");

	private String nameOfHolder;
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
	 * @param nameOfHolder data element
	 * @param fullNameSecondaryIdentifiers data element
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
	public DG11File(String nameOfHolder,
			List<String> otherNames, String personalNumber,
			Date fullDateOfBirth, List<String> placeOfBirth, List<String> permanentAddress,
			String telephone, String profession, String title,
			String personalSummary, byte[] proofOfCitizenship,
			List<String> otherValidTDNumbers, String custodyInformation) {
		super(EF_DG11_TAG);
		this.nameOfHolder = nameOfHolder;
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

	protected void readContent(InputStream inputStream) throws IOException {
		TLVInputStream tlvInputStream = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		int tagListTag = tlvInputStream.readTag();
		if (tagListTag != TAG_LIST_TAG) { throw new IllegalArgumentException("Expected tag list in DG11"); }

		int tagListLength = tlvInputStream.readLength();
		int tagListBytesRead = 0;

		int expectedTagCount = tagListLength / 2;

		ByteArrayInputStream tagListBytesInputStream = new ByteArrayInputStream(tlvInputStream.readValue());

		/* Find out which tags are present. */
		List<Integer> tagList = new ArrayList<Integer>(expectedTagCount + 1);
		while (tagListBytesRead < tagListLength) {
			/* We're using another TLV inputstream everytime to read each tag. */
			TLVInputStream anotherTLVInputStream = new TLVInputStream(tagListBytesInputStream);
			int tag = anotherTLVInputStream.readTag();
			tagListBytesRead += TLVUtil.getTagLength(tag);
			tagList.add(tag);
		}

		/* Now read the fields in order. */
		for (int t: tagList) {
			readField(t, tlvInputStream);
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
			if (countValue == null || countValue.length != 1) { throw new IllegalArgumentException("Number of content specific fields should be encoded in single byte, found " + Arrays.toString(countValue)); }
			int count = countValue[0] & 0xFF;
			for (int i = 0; i < count; i++) {
				tag = tlvIn.readTag();
				if (tag != OTHER_NAME_TAG) { throw new IllegalArgumentException("Expected " + Integer.toHexString(OTHER_NAME_TAG) + ", found " + Integer.toHexString(tag)); }
				/* int otherNameLength = */ tlvIn.readLength();
				byte[] value = tlvIn.readValue();
				parseOtherName(value);
			}
		} else {
			if (tag != fieldTag) { throw new IllegalArgumentException("Expected " + Integer.toHexString(fieldTag) + ", but found " + Integer.toHexString(tag)); }
			tlvIn.readLength();
			byte[] value = tlvIn.readValue();
			switch (tag) {
			case FULL_NAME_TAG: parseNameOfHolder(value); break;
			case OTHER_NAME_TAG: parseOtherName(value); break;
			case PERSONAL_NUMBER_TAG: parsePersonalNumber(value); break;
			case FULL_DATE_OF_BIRTH_TAG: parseFullDateOfBirth(value); break;
			case PLACE_OF_BIRTH_TAG: parsePlaceOfBirth(value); break;
			case PERMANENT_ADDRESS_TAG:  parsePermanentAddress(value); break;
			case TELEPHONE_TAG: parseTelephone(value); break;
			case PROFESSION_TAG: parseProfession(value); break;
			case TITLE_TAG: parseTitle(value); break;
			case PERSONAL_SUMMARY_TAG: parsePersonalSummary(value); break;
			case PROOF_OF_CITIZENSHIP_TAG: parseProofOfCitizenShip(value); break;
			case OTHER_VALID_TD_NUMBERS_TAG: parseOtherValidTDNumbers(value); break;
			case CUSTODY_INFORMATION_TAG: parseCustodyInformation(value); break;
			default: throw new IllegalArgumentException("Unknown field tag in DG11: " + Integer.toHexString(tag));
			}
		}
	}

	/* Field parsing and interpretation below. */

	private void parseCustodyInformation(byte[] value) {
		try {
			String field = new String(value, "UTF-8");
			//		custodyInformation = in.replace("<", " ").trim();
			custodyInformation = field.trim();
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			custodyInformation = new String(value).trim();
		}
	}

	private void parseOtherValidTDNumbers(byte[] value) {
		String field = new String(value).trim();
		try {
			field = new String(value, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}
		otherValidTDNumbers = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(field, "<");
		while (st.hasMoreTokens()) {
			String number = st.nextToken().trim();
			otherValidTDNumbers.add(number);
		}
	}

	private void parseProofOfCitizenShip(byte[] value) {
		proofOfCitizenship = value;
	}

	private void parsePersonalSummary(byte[] value) {
		try {
			String field = new String(value, "UTF-8");
			//		personalSummary = in.replace("<", " ").trim();
			personalSummary = field.trim();
		} catch (UnsupportedEncodingException usee) {
			usee.printStackTrace();
			personalSummary = new String(value).trim();
		}
	}

	private void parseTitle(byte[] value) {
		try {
			String field = new String(value, "UTF-8");
			//		title = in.replace("<", " ").trim();
			title = field.trim();
		} catch (UnsupportedEncodingException usee) {
			usee.printStackTrace();
			title = new String(value).trim();
		}
	}

	private void parseProfession(byte[] value) {
		String field = new String(value);
		try {
			field = new String(value, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}
		//		profession = in.replace("<", " ").trim();
		profession = field.trim();
	}

	private void parseTelephone(byte[] value) {
		String field = new String(value);
		try {
			field = new String(value, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}
		//		telephone = in.replace("<", " ").trim();
		telephone = field.replace("<", " ").trim();
	}

	private void parsePermanentAddress(byte[] value) {
		String field = new String(value);
		try {
			field = new String(value, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}
		StringTokenizer st = new StringTokenizer(field, "<");
		permanentAddress = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String line = st.nextToken().trim();
			permanentAddress.add(line);
		}
	}

	private void parsePlaceOfBirth(byte[] value) {
		String field = new String(value);
		try {
			field = new String(value, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}
		StringTokenizer st = new StringTokenizer(field, "<");
		placeOfBirth = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String line = st.nextToken().trim();
			placeOfBirth.add(line);
		}
	}

	private void parseFullDateOfBirth(byte[] value) {
		try {
			String field = null;
			if (value.length == 4) {
				/* Either France or Belgium uses this encoding for dates. */
				field = Hex.bytesToHexString(value);
			} else {
				field = new String(value);
				try {
					field = new String(value, "UTF-8");
				} catch (UnsupportedEncodingException usee) {
					usee.printStackTrace();
				}
			}
			// in = in.replace("<", " ").trim();
			fullDateOfBirth = SDF.parse(field);
		} catch (ParseException pe) {
			throw new IllegalArgumentException(pe.toString());
		}
	}

	private synchronized void parseOtherName(byte[] value) {
		if (otherNames == null) { otherNames = new ArrayList<String>(); }
		try {
			String field = new String(value, "UTF-8");
			otherNames.add(field.trim());
		} catch (UnsupportedEncodingException usee) {
			usee.printStackTrace();
			otherNames.add(new String(value).trim());
		}
	}

	private void parsePersonalNumber(byte[] value) {
		String field = new String(value);
		try {
			field = new String(value, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}
		personalNumber = field.trim();
	}

	private void parseNameOfHolder(byte[] value) {
		String field = new String(value);
		try {
			field = new String(value, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}
		nameOfHolder = field.trim();
	}
	
//	private void parseFullName(byte[] value) {
//		String field = new String(value);
//		try {
//			field = new String(value, "UTF-8");
//		} catch (UnsupportedEncodingException uee) {
//			uee.printStackTrace();
//		}
//		String delim = "<<";
//		int delimIndex = field.indexOf(delim);
//		if (delimIndex < 0) {
//			LOGGER.warning("Input does not contain primary identifier delimited by \"<<\", input was \"" + field + "\"");
//			LOGGER.warning("Trying \"<\" (the Austrian way)");
//			delim = "<";
//			delimIndex = field.indexOf(delim);
//			if(delimIndex < 0) {
//				LOGGER.warning("Didn't work either. Trying spaces.");
//				delim = " "; /* NOTE: Some passports (Belgian 1st generation) uses space?!? */
//				delimIndex = field.indexOf(delim);
//				if(delimIndex < 0) {
//					LOGGER.warning("Giving up. Putting everything into the primary identifier.");
//					fullNamePrimaryIdentifier = field; 
//					return;
//				}
//			}
//		}
//		fullNamePrimaryIdentifier = field.substring(0, delimIndex);
//		StringTokenizer st = new StringTokenizer(field.substring(field.indexOf(delim) + delim.length()), "<");
//		fullNameSecondaryIdentifiers = new ArrayList<String>();
//		while (st.hasMoreTokens()) {
//			String secondaryIdentifier = st.nextToken().trim();
//			fullNameSecondaryIdentifiers.add(secondaryIdentifier);
//		}
//	}

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
		if (nameOfHolder != null) {
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
	 * Gets the full name of the holder (primary and secondary identifiers).
	 * 
	 * @return the name of holder
	 */
	public String getNameOfHolder() {
		return nameOfHolder;
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
	 * @return the personal number
	 */
	public String getPersonalNumber() {
		return personalNumber;
	}

	/**
	 * Gets the full date of birth.
	 * 
	 * @return the full date of birth
	 */
	public Date getFullDateOfBirth() {
		return fullDateOfBirth;
	}

	/**
	 * Gets the place of birth.
	 * 
	 * @return the place of birth
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
		result.append(nameOfHolder == null ? "" : nameOfHolder); result.append(", ");
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
				tlvOut.writeValue(nameOfHolder.trim().getBytes("UTF-8"));
				break;
			case OTHER_NAME_TAG:
				if (otherNames == null) { otherNames = new ArrayList<String>(); }
				tlvOut.writeTag(CONTENT_SPECIFIC_CONSTRUCTED_TAG);
				tlvOut.writeTag(COUNT_TAG);
				tlvOut.write(otherNames.size());
				tlvOut.writeValueEnd(); /* COUNT_TAG */
				for (String otherName: otherNames) {
					tlvOut.writeTag(OTHER_NAME_TAG);
					tlvOut.writeValue(otherName.trim().getBytes("UTF-8"));
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
				byte[] fullDateOfBirthBytes = Hex.hexStringToBytes(fullDateOfBirthString);
				tlvOut.writeValue(fullDateOfBirthBytes);		
				break;
			case PLACE_OF_BIRTH_TAG:
				tlvOut.writeTag(tag);
				boolean isFirstOne = true;
				for (String detail: placeOfBirth) {
					if (detail != null) {
						if (isFirstOne) { isFirstOne = false; } else { tlvOut.write('<'); }
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
