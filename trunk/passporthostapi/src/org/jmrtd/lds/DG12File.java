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
 * $Id: DG12File.java 1481 2012-11-09 21:55:31Z martijno $
 */

package org.jmrtd.lds;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import net.sourceforge.scuba.tlv.TLVInputStream;
import net.sourceforge.scuba.tlv.TLVOutputStream;
import net.sourceforge.scuba.util.Hex;

/**
 * File structure for the EF_DG12 file.
 * Datagroup 12 contains additional document detail(s).
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 1481 $
 */
public class DG12File extends DataGroup {

	private static final long serialVersionUID = -1979367459379125674L;

	private static final int TAG_LIST_TAG = 0x5C;

	public static final int ISSUING_AUTHORITY_TAG = 0x5F19,
			DATE_OF_ISSUE_TAG = 0x5F26,  // yyyymmdd
			NAME_OF_OTHER_PERSON_TAG = 0x5F1A, // formatted per ICAO 9303 rules 
			ENDORSEMENTS_AND_OBSERVATIONS_TAG = 0x5F1B,
			TAX_OR_EXIT_REQUIREMENTS_TAG = 0x5F1C,
			IMAGE_OF_FRONT_TAG = 0x5F1D, // Image per ISO/IEC 10918
			IMAGE_OF_REAR_TAG = 0x5F1E, // Image per ISO/IEC 10918
			DATE_AND_TIME_OF_PERSONALIZATION = 0x5F55, // yyyymmddhhmmss
			PERSONALIZATION_SYSTEM_SERIAL_NUMBER_TAG = 0x5F56,
			CONTENT_SPECIFIC_CONSTRUCTED_TAG = 0xA0, // 5F1A is always used inside A0 constructed object
			COUNT_TAG = 0x02; // Used in A0 constructed object to indicate single byte count of simple objects

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat SDTF = new SimpleDateFormat("yyyyMMddhhmmss");

	private String issuingAuthority;
	private Date dateOfIssue;
	private List<String> namesOfOtherPersons;
	private String endorseMentsAndObservations;
	private String taxOrExitRequirements;
	private byte[] imageOfFront;
	private byte[] imageOfRear;
	private Date dateAndTimeOfPersonalization;
	private String personalizationSystemSerialNumber;

	private List<Integer> tagPresenceList;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	/**
	 * Constructs a new file.
	 *
	 * @param issuingAuthority
	 * @param dateOfIssue
	 * @param namesOfOtherPersons
	 * @param endorseMentsAndObservations
	 * @param taxOrExitRequirements
	 * @param imageOfFront
	 * @param imageOfRear
	 * @param dateAndTimeOfPersonalization
	 * @param personalizationSystemSerialNumber
	 */
	public DG12File(String issuingAuthority, Date dateOfIssue,
			List<String> namesOfOtherPersons, String endorseMentsAndObservations,
			String taxOrExitRequirements, byte[] imageOfFront,
			byte[] imageOfRear, Date dateAndTimeOfPersonalization,
			String personalizationSystemSerialNumber) {
		super(EF_DG12_TAG);
		this.issuingAuthority = issuingAuthority;
		this.dateOfIssue = dateOfIssue;
		this.namesOfOtherPersons = namesOfOtherPersons == null ? new ArrayList<String>() : new ArrayList<String>(namesOfOtherPersons);
		this.endorseMentsAndObservations = endorseMentsAndObservations;
		this.taxOrExitRequirements = taxOrExitRequirements;
		this.imageOfFront = imageOfFront;
		this.imageOfRear = imageOfRear;
		this.dateAndTimeOfPersonalization = dateAndTimeOfPersonalization;
		this.personalizationSystemSerialNumber = personalizationSystemSerialNumber;
	}

	/**
	 * Constructs a new file.
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
	public DG12File(InputStream inputStream) throws IOException {
		super(EF_DG12_TAG, inputStream);
	}

	protected void readContent(InputStream inputStream) throws IOException {	
		TLVInputStream tlvIn = inputStream instanceof TLVInputStream ? (TLVInputStream)inputStream : new TLVInputStream(inputStream);
		int tag = tlvIn.readTag();
		if (tag != TAG_LIST_TAG) { throw new IllegalArgumentException("Expected tag list in DG12"); }
		int length = tlvIn.readLength();
		int tagCount = length / 2;
		int[] tagList = new int[tagCount];
		for (int i = 0; i < tagCount; i++) {
			int hi = tlvIn.read();
			int lo = tlvIn.read();
			tag = ((hi & 0xFF) << 8) | (lo & 0xFF);
			tagList[i] = tag;
		}
		for (int t: tagList) {
			LOGGER.info("DG12 tagList entry " + Integer.toHexString(t));
		}
		for (int i = 0; i < tagCount; i++) {
			readField(tagList[i], tlvIn);
		}
	}

	/**
	 * Gets the tags of fields actually present in this file.
	 * 
	 * @return a list of tags
	 */
	public List<Integer> getTagPresenceList() {
		if (tagPresenceList != null) { return tagPresenceList; }
		tagPresenceList = new ArrayList<Integer>(10);
		if(issuingAuthority != null) { tagPresenceList.add(ISSUING_AUTHORITY_TAG); }
		if(dateOfIssue != null) { tagPresenceList.add(DATE_OF_ISSUE_TAG); }
		if(namesOfOtherPersons != null && namesOfOtherPersons.size() > 0) { tagPresenceList.add(NAME_OF_OTHER_PERSON_TAG); }
		if(endorseMentsAndObservations != null) { tagPresenceList.add(ENDORSEMENTS_AND_OBSERVATIONS_TAG); }
		if(taxOrExitRequirements != null) { tagPresenceList.add(TAX_OR_EXIT_REQUIREMENTS_TAG); }
		if(imageOfFront != null) { tagPresenceList.add(IMAGE_OF_FRONT_TAG); }
		if(imageOfRear != null) { tagPresenceList.add(IMAGE_OF_REAR_TAG); }
		if(dateAndTimeOfPersonalization != null) { tagPresenceList.add(DATE_AND_TIME_OF_PERSONALIZATION); }
		if(personalizationSystemSerialNumber != null) { tagPresenceList.add(PERSONALIZATION_SYSTEM_SERIAL_NUMBER_TAG); }
		return tagPresenceList;
	}

	private void readField(int expectedFieldTag, TLVInputStream tlvIn) throws IOException {
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
				if (tag != NAME_OF_OTHER_PERSON_TAG) { throw new IllegalArgumentException("Expected " + Integer.toHexString(NAME_OF_OTHER_PERSON_TAG) + ", found " + Integer.toHexString(tag)); }
				/* int otherPersonLength = */ tlvIn.readLength();
				byte[] value = tlvIn.readValue();
				parseNameOfOtherPerson(new String(value));
			}
		} else {
			if (tag != expectedFieldTag) { throw new IllegalArgumentException("Expected " + Integer.toHexString(expectedFieldTag) + ", but found " + Integer.toHexString(tag)); }
			/* int length = */ tlvIn.readLength();
			byte[] value = tlvIn.readValue();
			switch (tag) {
			case ISSUING_AUTHORITY_TAG: parseIssuingAuthority(new String(value)); break;
			case DATE_OF_ISSUE_TAG: parseDateOfIssue(new String(value)); break;
			case NAME_OF_OTHER_PERSON_TAG: parseNameOfOtherPerson(new String(value)); break;
			case ENDORSEMENTS_AND_OBSERVATIONS_TAG: parseEndorsementsAndObservations(new String(value)); break;
			case TAX_OR_EXIT_REQUIREMENTS_TAG: parseTaxOrExitRequirements(new String(value)); break;
			case IMAGE_OF_FRONT_TAG: parseImageOfFront(value); break;
			case IMAGE_OF_REAR_TAG: parseImageOfRear(value); break;
			case DATE_AND_TIME_OF_PERSONALIZATION: parseDateAndTimeOfPersonalization(Hex.bytesToHexString(value)); break;
			case PERSONALIZATION_SYSTEM_SERIAL_NUMBER_TAG: parsePersonalizationSystemSerialNumber(new String(value)); break;
			default: throw new IllegalArgumentException("Unknown field tag in DG12: " + Integer.toHexString(tag));
			}
		}
	}

	/* Field parsing below. */

	private void parsePersonalizationSystemSerialNumber(String in) {
		personalizationSystemSerialNumber = in.trim();
	}

	private void parseDateAndTimeOfPersonalization(String in) {
		try {
			dateAndTimeOfPersonalization = SDTF.parse(in.trim());
		} catch (ParseException pe) {
			throw new IllegalArgumentException(pe.toString());
		}
	}

	private void parseImageOfFront(byte[] value) {
		imageOfFront =  value;
	}

	private void parseImageOfRear(byte[] value) {
		imageOfRear =  value;
	}

	private void parseTaxOrExitRequirements(String in) {
		taxOrExitRequirements = in.trim();
	}

	private void parseEndorsementsAndObservations(String in) {
		endorseMentsAndObservations = in.trim();
	}

	private synchronized void parseNameOfOtherPerson(String in) {
		if (namesOfOtherPersons == null) { namesOfOtherPersons = new ArrayList<String>(); }
		namesOfOtherPersons.add(in.trim());
	}

	private void parseDateOfIssue(String in) {
		try {
			if (in == null || in.length() != 8) { throw new IllegalArgumentException("Wrong date format: " + in); }
			dateOfIssue = SDF.parse(in.trim());
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.toString());
		}

	}

	private void parseIssuingAuthority(String in) {
		issuingAuthority = in.trim();
	}

	/* Accessors below. */

	/**
	 * Gets the issuing authority.
	 * 
	 * @return the issuingAuthority
	 */
	public String getIssuingAuthority() {
		return issuingAuthority;
	}

	/**
	 * Gets the date of issuance.
	 * 
	 * @return the dateOfIssue
	 */
	public Date getDateOfIssue() {
		return dateOfIssue;
	}

	/**
	 * Gets name of other person.
	 * 
	 * @return the nameOfOtherPerson
	 */
	public List<String> getNamesOfOtherPersons() {
		return namesOfOtherPersons;
	}

	/**
	 * Gets endorsements and observations.
	 * 
	 * @return the endorsementsAndObservations
	 */
	public String getEndorsementsAndObservations() {
		return endorseMentsAndObservations;
	}

	/**
	 * Gets tax or exit requirements.
	 * 
	 * @return the taxOrExitRequirements
	 */
	public String getTaxOrExitRequirements() {
		return taxOrExitRequirements;
	}

	/**
	 * Gets image of front.
	 * 
	 * @return the imageOfFront
	 */
	public byte[] getImageOfFront() {
		return imageOfFront;
	}

	/**
	 * Gets image of rear.
	 * 
	 * @return the imageOfRear
	 */
	public byte[] getImageOfRear() {
		return imageOfRear;
	}

	/**
	 * Gets date and time of personalization.
	 * 
	 * @return the dateAndTimeOfPersonalization
	 */
	public Date getDateAndTimeOfPersonalization() {
		return dateAndTimeOfPersonalization;
	}

	/**
	 * Gets the personalization system serial number.
	 * 
	 * @return the personalizationSystemSerialNumber
	 */
	public String getPersonalizationSystemSerialNumber() {
		return personalizationSystemSerialNumber;
	}

	public int getTag() {
		return EF_DG12_TAG;
	}

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("DG12File [");
		result.append(issuingAuthority == null ? "" : issuingAuthority); result.append(", ");
		result.append(dateOfIssue == null ? "" : SDF.format(dateOfIssue)); result.append(", ");
		result.append(namesOfOtherPersons == null || namesOfOtherPersons.size() == 0 ? "" : namesOfOtherPersons); result.append(", ");
		result.append(endorseMentsAndObservations == null ? "" : endorseMentsAndObservations); result.append(", ");
		result.append(taxOrExitRequirements == null ? "" : taxOrExitRequirements); result.append(", ");
		result.append(imageOfFront == null ? "" : "image (" + imageOfFront.length + ")"); result.append(", ");
		result.append(imageOfRear == null ? "" : "image (" + imageOfRear.length + ")"); result.append(", ");
		result.append(dateAndTimeOfPersonalization == null ? "" : SDF.format(dateAndTimeOfPersonalization)); result.append(", ");
		result.append(personalizationSystemSerialNumber== null ? "" : personalizationSystemSerialNumber);
		result.append("]");
		return result.toString();
	}

	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (!obj.getClass().equals(this.getClass())) { return false; }
		DG12File other = (DG12File)obj;
		return this.toString().equals(other.toString());
	}

	public int hashCode() {
		return 13 * toString().hashCode() + 112;
	}
	
	protected void writeContent(OutputStream outputStream) throws IOException {
		TLVOutputStream tlvOut = outputStream instanceof TLVOutputStream ? (TLVOutputStream)outputStream : new TLVOutputStream(outputStream);
		tlvOut.writeTag(TAG_LIST_TAG);
		List<Integer> tags = getTagPresenceList();
		DataOutputStream dataOut = new DataOutputStream(tlvOut);
		for (int tag: tags) {
			dataOut.writeShort(tag);
		}
		dataOut.flush();
		tlvOut.writeValueEnd(); /* TAG_LIST_TAG */
		for (int tag: tags) {
			switch (tag) {
			case ISSUING_AUTHORITY_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(issuingAuthority.trim().getBytes("UTF-8"));
				break;
			case DATE_OF_ISSUE_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(new String(SDF.format(dateOfIssue)).getBytes("UTF-8"));
				break;
			case NAME_OF_OTHER_PERSON_TAG:
				if (namesOfOtherPersons == null) { namesOfOtherPersons = new ArrayList<String>(); }
				tlvOut.writeTag(CONTENT_SPECIFIC_CONSTRUCTED_TAG);
				tlvOut.writeTag(COUNT_TAG);
				tlvOut.write(namesOfOtherPersons.size());
				tlvOut.writeValueEnd(); /* COUNT_TAG */
				for (String nameOfOtherPerson: namesOfOtherPersons) {
					tlvOut.writeTag(NAME_OF_OTHER_PERSON_TAG);
					tlvOut.writeValue(nameOfOtherPerson.trim().getBytes("UTF-8"));
				}
				tlvOut.writeValueEnd(); /* CONTENT_SPECIFIC_CONSTRUCTED_TAG */
				break; 
			case ENDORSEMENTS_AND_OBSERVATIONS_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(endorseMentsAndObservations.trim().getBytes("UTF-8"));
				break;
			case TAX_OR_EXIT_REQUIREMENTS_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(taxOrExitRequirements.trim().getBytes("UTF-8"));
				break;
			case IMAGE_OF_FRONT_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(imageOfFront);
				break;
			case IMAGE_OF_REAR_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(imageOfRear);
				break;
			case DATE_AND_TIME_OF_PERSONALIZATION:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(Hex.hexStringToBytes(SDTF.format(dateAndTimeOfPersonalization)));
				break;
			case PERSONALIZATION_SYSTEM_SERIAL_NUMBER_TAG:
				tlvOut.writeTag(tag);
				tlvOut.writeValue(personalizationSystemSerialNumber.trim().getBytes("UTF-8"));
				break;
			default: throw new IllegalArgumentException("Unknown field tag in DG12: " + Integer.toHexString(tag));
			}
		}
	}
}
