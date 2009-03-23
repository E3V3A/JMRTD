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
 * $Id$
 */

package sos.mrtd;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sourceforge.scuba.tlv.BERTLVInputStream;
import net.sourceforge.scuba.util.Hex;

/**
 * File structure for the EF_DG12 file.
 * Datagroup 12 contains additional document detail(s).
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG12File extends DataGroup
{
	private static final int TAG_LIST_TAG = 0x5C;

	private static final int ISSUING_AUTHORITY_TAG = 0x5F19; 
	private static final int DATE_OF_ISSUE_TAG = 0x5F26;  // yyyymmdd
	private static final int NAME_OF_OTHER_PERSON_TAG = 0x5F1A; // formatted per ICAO 9303 rules 
	private static final int ENDORSEMENTS_AND_OBSERVATIONS_TAG = 0x5F1B;
	private static final int TAX_OR_EXIT_REQUIREMENTS_TAG = 0x5F1C;
	private static final int IMAGE_OF_FRONT_TAG = 0x5F1D; // Image per ISO/IEC 10918
	private static final int IMAGE_OF_REAR_TAG = 0x5F1E; // Image per ISO/IEC 10918
	private static final int DATE_AND_TIME_OF_PERSONALIZATION = 0x5F85; // yyyymmddhhmmss
	private static final int PERSONALIZATION_SYSTEM_SERIAL_NUMBER_TAG = 0x5F86;
	private static final int OTHER_PEOPLE_OBJECT_TAG = 0xA0; // FIXME: alternative to 5F1A?
	private static final int OTHER_PEOPLE_COUNT_TAG = 0x02;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat SDTF = new SimpleDateFormat("yyyyMMddhhmmss");

	private String issuingAuthority;
	private Date dateOfIssue;
	private String nameOfOtherPerson;
	private String endorseMentsAndObservations;
	private String taxOrExitRequirements;
	private BufferedImage imageOfFront;
	private BufferedImage imageOfRear;
	private Date dateAndTimeOfPersonalization;
	private String personalizationSystemSerialNumber;

	/**
	 * Constructs a new file.
	 *
	 * @param issuingAuthority
	 * @param dateOfIssue
	 * @param nameOfOtherPerson
	 * @param endorseMentsAndObservations
	 * @param taxOrExitRequirements
	 * @param imageOfFront
	 * @param imageOfRear
	 * @param dateAndTimeOfPersonalization
	 * @param personalizationSystemSerialNumber
	 */
	public DG12File(String issuingAuthority, Date dateOfIssue,
			String nameOfOtherPerson, String endorseMentsAndObservations,
			String taxOrExitRequirements, BufferedImage imageOfFront,
			BufferedImage imageOfRear, Date dateAndTimeOfPersonalization,
			String personalizationSystemSerialNumber) {
		super();
		this.issuingAuthority = issuingAuthority;
		this.dateOfIssue = dateOfIssue;
		this.nameOfOtherPerson = nameOfOtherPerson;
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
	 * @param in
	 * @throws IOException
	 */
	public DG12File(InputStream in) throws IOException {
		BERTLVInputStream tlvIn = new BERTLVInputStream(in);
		int tag = tlvIn.readTag();
		if (tag != PassportFile.EF_DG12_TAG) { throw new IllegalArgumentException("Expected EF_DG12_TAG"); }
		int length = tlvIn.readLength();
		tag = tlvIn.readTag();
		if (tag != TAG_LIST_TAG) { throw new IllegalArgumentException("Expected tag list in DG12"); }
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
		case ISSUING_AUTHORITY_TAG: parseIssuingAuthority(new String(value)); break;
		case DATE_OF_ISSUE_TAG: parseDateOfIssue(Hex.bytesToHexString(value)); break;
		case NAME_OF_OTHER_PERSON_TAG: parseNameOfOtherPerson(new String(value)); break; 
		case ENDORSEMENTS_AND_OBSERVATIONS_TAG: parseEndorsementsAndObservations(new String(value)); break;
		case TAX_OR_EXIT_REQUIREMENTS_TAG: parseTaxOrExitRequirements(new String(value)); break;
		case IMAGE_OF_FRONT_TAG: parseImageOfFront(value); break;
		case IMAGE_OF_REAR_TAG: parseImageOfRear(value); break;
		case DATE_AND_TIME_OF_PERSONALIZATION: parseDateAndTimeOfPersonalization(Hex.bytesToHexString(value)); break;
		case PERSONALIZATION_SYSTEM_SERIAL_NUMBER_TAG: parsePersonalizationSystemSerialNumber(new String(value)); break;
		// case CUSTODY_INFORMATION_TAG: parseCustodyInformation(new String(value)); break;
		default: throw new IllegalArgumentException("Unknown field tag in DG11: " + Integer.toHexString(tag));
		}
	}

	/* Field parsing below. */

	private void parsePersonalizationSystemSerialNumber(String in) {
		personalizationSystemSerialNumber = in.replace("<", " ").trim();
	}

	private void parseDateAndTimeOfPersonalization(String in) {
		try {
			dateAndTimeOfPersonalization = SDTF.parse(in.replace("<", " ").trim());
		} catch (ParseException pe) {
			throw new IllegalArgumentException(pe.toString());
		}
	}

	private void parseImageOfRear(byte[] in) {
		// TODO: parse jpeg
	}

	private void parseImageOfFront(byte[] in) {
		// TODO parse jpeg

	}

	private void parseTaxOrExitRequirements(String in) {
		taxOrExitRequirements = in.replaceAll("<", " ").trim();
	}

	private void parseEndorsementsAndObservations(String in) {
		endorseMentsAndObservations = in.replaceAll("<", " ").trim();
	}

	private void parseNameOfOtherPerson(String in) {
		nameOfOtherPerson = in.replaceAll("<", " ").trim();

	}

	private void parseDateOfIssue(String in) {
		try {
			dateOfIssue = SDF.parse(in.replace("<", " ").trim());
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.toString());
		}

	}

	private void parseIssuingAuthority(String in) {
		issuingAuthority = in.replace("<", " ").trim();
	}

	/* Accessors below. */

	/**
	 * @return the issuingAuthority
	 */
	public String getIssuingAuthority() {
		return issuingAuthority;
	}

	/**
	 * @return the dateOfIssue
	 */
	public Date getDateOfIssue() {
		return dateOfIssue;
	}

	/**
	 * @return the nameOfOtherPerson
	 */
	public String getNameOfOtherPerson() {
		return nameOfOtherPerson;
	}

	/**
	 * @return the endorseMentsAndObservations
	 */
	public String getEndorseMentsAndObservations() {
		return endorseMentsAndObservations;
	}

	/**
	 * @return the taxOrExitRequirements
	 */
	public String getTaxOrExitRequirements() {
		return taxOrExitRequirements;
	}

	/**
	 * @return the imageOfFront
	 */
	public BufferedImage getImageOfFront() {
		return imageOfFront;
	}

	/**
	 * @return the imageOfRear
	 */
	public BufferedImage getImageOfRear() {
		return imageOfRear;
	}

	/**
	 * @return the dateAndTimeOfPersonalization
	 */
	public Date getDateAndTimeOfPersonalization() {
		return dateAndTimeOfPersonalization;
	}

	/**
	 * @return the personalizationSystemSerialNumber
	 */
	public String getPersonalizationSystemSerialNumber() {
		return personalizationSystemSerialNumber;
	}

	public int getTag() {
		return EF_DG11_TAG;
	}

	public String toString() {
		return "DG11File";
	}

	/**
	 * TODO: in progress.
	 */
	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject;
		}
		return null;
	}
}
