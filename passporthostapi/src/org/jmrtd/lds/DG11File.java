/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

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
public class DG11File extends DataGroup
{
	private static final int TAG_LIST_TAG = 0x5C;
	private static final int FULL_NAME_TAG = 0x5F0E;
	private static final int PERSONAL_NUMBER_TAG = 0x5F10;
	private static final int FULL_DATE_OF_BIRTH_TAG = 0x5F2B; // In 'CCYYMMDD' format.
	private static final int PLACE_OF_BIRTH_TAG = 0x5F11; // Fields separated by '<'
	private static final int PERMANENT_ADDRESS_TAG = 0x5F42; // Fields separated by '<'
	private static final int TELEPHONE_TAG = 0x5F12;
	private static final int PROFESSION_TAG = 0x5F13;
	private static final int TITLE_TAG = 0x5F14;
	private static final int PERSONAL_SUMMARY_TAG = 0x5F15;
	private static final int PROOF_OF_CITIZENSHIP_TAG = 0x5F16; // Compressed image per ISO/IEC 10918 
	private static final int OTHER_VALID_TD_NUMBERS_TAG = 0x5F17; // Separated by '<'
	private static final int CUSTODY_INFORMATION_TAG = 0x5F18;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

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

	private List<Integer> tagPresenceList;

	/**
	 * Constructs a new file. Use <code>null</code> if data element is not present.
	 * Use <code>'<'</code> as separator.
	 *
	 * @param fullNamePrimaryIdentifier data element
	 * @param fullNamesecondaryIdentifiers data element
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
		super(in, EF_DG11_TAG);
	}

	protected void readContent(TLVInputStream tlvIn) throws IOException {
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
		if (tag != fieldTag) { throw new IllegalArgumentException("Expected " + Integer.toHexString(fieldTag) + ", but found " + Integer.toHexString(tag)); }
		tlvIn.readLength();
		byte[] value = tlvIn.readValue();
		switch (tag) {
		case FULL_NAME_TAG: parseFullName(new String(value, "UTF-8")); break;
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
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(value);
			proofOfCitizenship =  ImageIO.read(in);
		} catch (IOException ioe) {
			throw new IllegalArgumentException(ioe.getMessage());
		}
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
	 * Gets list of tags present.
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
	 * @return the fullNamePrimaryIdentifier
	 */
	public String getFullNamePrimaryIdentifier() {
		return fullNamePrimaryIdentifier;
	}

	/**
	 * @return the fullNamesecondaryIdentifiers
	 */
	public List<String> getFullNameSecondaryIdentifiers() {
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

	/**
	 * Gets a textual representation of this file.
	 * 
	 * @return a textual representation of this file
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("DG11File [");
		result.append(fullNamePrimaryIdentifier); result.append(", ");
		result.append(fullNameSecondaryIdentifiers == null || fullNameSecondaryIdentifiers.size() == 0 ? "[]" : fullNameSecondaryIdentifiers.toString()); result.append(", ");
		result.append(personalNumber); result.append(", ");
		result.append(SDF.format(fullDateOfBirth)); result.append(", ");
		result.append(placeOfBirth == null || placeOfBirth.size() == 0 ? "[]" : placeOfBirth.toString()); result.append(", ");
		result.append(permanentAddress == null || permanentAddress.size() == 0 ? "[]" : permanentAddress.toString()); result.append(", ");
		result.append(telephone); result.append(", ");
		result.append(profession); result.append(", ");
		result.append(title); result.append(", ");
		result.append(personalSummary); result.append(", ");
		result.append(proofOfCitizenship == null ? "" : proofOfCitizenship.getWidth() + "x" + proofOfCitizenship.getHeight()); result.append(", ");
		result.append(otherValidTDNumbers == null || otherValidTDNumbers.size() == 0 ? "[]" : otherValidTDNumbers.toString()); result.append(", ");
		result.append(custodyInformation);
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

	protected void writeContent(TLVOutputStream tlvOut) throws IOException {
		tlvOut.writeTag(TAG_LIST_TAG);
		DataOutputStream dataOut = new DataOutputStream(tlvOut);
		List<Integer> tags = getTagPresenceList();
		for (int tag: tags) {
			dataOut.writeShort(tag);
		}
		dataOut.flush();
		tlvOut.writeValueEnd(); /* TAG_LIST_TAG */
		for (int tag: tags) {
			tlvOut.writeTag(tag);
			switch (tag) {
			case FULL_NAME_TAG:
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
			case PERSONAL_NUMBER_TAG:
				tlvOut.writeValue(personalNumber.trim().getBytes("UTF-8"));
				break;
			case FULL_DATE_OF_BIRTH_TAG:
				String fullDateOfBirthString = SDF.format(fullDateOfBirth);
				// byte[] fullDateOfBirthBytes = fullDateOfBirthString.getBytes("UTF-8");
				byte[] fullDateOfBirthBytes = Hex.hexStringToBytes(fullDateOfBirthString);
				tlvOut.writeValue(fullDateOfBirthBytes);		
				break;
			case PLACE_OF_BIRTH_TAG:
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
				tlvOut.writeValue(telephone.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			case PROFESSION_TAG:
				tlvOut.writeValue(profession.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			case TITLE_TAG:
				tlvOut.writeValue(title.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			case PERSONAL_SUMMARY_TAG:
				tlvOut.writeValue(personalSummary.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			case PROOF_OF_CITIZENSHIP_TAG:
				ImageIO.write(proofOfCitizenship, "image/jpeg", tlvOut);
				tlvOut.writeValueEnd(); /* PROOF_OF_CITIZENSHIP_TAG */
				break;
			case OTHER_VALID_TD_NUMBERS_TAG:
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
				tlvOut.writeValue(custodyInformation.trim().replace(' ', '<').getBytes("UTF-8"));
				break;
			default: throw new IllegalStateException("Unknown tag in DG11: " + Integer.toHexString(tag));
			}
		}
	}

	/**
	 * Gets this file encoded as bytes, including ICAO tag.
	 * 
	 * @return this file as byte array.
	 */
	//	public byte[] getEncoded() {
	//		if (isSourceConsistent) {
	//			return sourceObject;
	//		}
	//		try {
	//			List<BERTLVObject> dataElements = new LinkedList<BERTLVObject>();
	//
	//			if (fullNamePrimaryIdentifier != null || fullNameSecondaryIdentifiers != null) {
	//				ByteArrayOutputStream out = new ByteArrayOutputStream();
	//				if (fullNamePrimaryIdentifier !=  null) {
	//					//					out.write(fullNamePrimaryIdentifier.trim().replace(' ', '<').getBytes("UTF-8"));
	//					out.write(fullNamePrimaryIdentifier.trim().getBytes("UTF-8"));
	//				}
	//				out.write("<<".getBytes("UTF-8"));
	//				boolean isFirstOne = true;
	//				if (fullNameSecondaryIdentifiers != null) {
	//					for (String secondaryPrimaryIdentifier: fullNameSecondaryIdentifiers) {
	//						if (secondaryPrimaryIdentifier != null) {
	//							if (isFirstOne) { isFirstOne = false; } else { out.write('<'); }
	//							//							out.write(secondaryPrimaryIdentifier.trim().replace(' ', '<').getBytes("UTF-8"));
	//							out.write(secondaryPrimaryIdentifier.trim().getBytes("UTF-8"));
	//						}
	//					}
	//				}
	//				out.flush();
	//				dataElements.add(new BERTLVObject(FULL_NAME_TAG, out.toByteArray()));
	//			}
	//			if (personalNumber != null) {
	//				//				dataElements.add(new BERTLVObject(PERSONAL_NUMBER_TAG, personalNumber.trim().replace(' ', '<').getBytes("UTF-8")));
	//				dataElements.add(new BERTLVObject(PERSONAL_NUMBER_TAG, personalNumber.trim().getBytes("UTF-8")));
	//			}
	//			if (fullDateOfBirth != null) {
	//				String fullDateOfBirthString = SDF.format(fullDateOfBirth);
	//				//				byte[] fullDateOfBirthBytes = fullDateOfBirthString.getBytes("UTF-8");
	//				byte[] fullDateOfBirthBytes = Hex.hexStringToBytes(fullDateOfBirthString);
	//				dataElements.add(new BERTLVObject(FULL_DATE_OF_BIRTH_TAG, fullDateOfBirthBytes));
	//			}
	//			if (placeOfBirth != null) {
	//				boolean isFirstOne = true;
	//				ByteArrayOutputStream out = new ByteArrayOutputStream();
	//				for (String detail: placeOfBirth) {
	//					if (detail != null) {
	//						if (isFirstOne) { isFirstOne = false; } else { out.write('<'); }
	//						//						out.write(detail.trim().replace(' ', '<').getBytes("UTF-8"));
	//						out.write(detail.trim().getBytes("UTF-8"));
	//
	//					}
	//				}
	//				out.flush();
	//				dataElements.add(new BERTLVObject(PLACE_OF_BIRTH_TAG, out.toByteArray()));
	//			}
	//			if (permanentAddress != null) {
	//				boolean isFirstOne = true;
	//				ByteArrayOutputStream out = new ByteArrayOutputStream();
	//				for (String detail: permanentAddress) {
	//					if (detail != null) {
	//						if (isFirstOne) { isFirstOne = false; } else { out.write('<'); }
	//						//						out.write(detail.trim().replace(' ', '<').getBytes("UTF-8"));
	//						out.write(detail.trim().getBytes("UTF-8"));
	//					}
	//				}
	//				out.flush();
	//				dataElements.add(new BERTLVObject(PERMANENT_ADDRESS_TAG, out.toByteArray()));
	//			}
	//			if (telephone != null) {
	//				dataElements.add(new BERTLVObject(TELEPHONE_TAG, telephone.trim().replace(' ', '<').getBytes("UTF-8")));
	//			}
	//			if (profession != null) {
	//				dataElements.add(new BERTLVObject(PROFESSION_TAG, profession.trim().replace(' ', '<').getBytes("UTF-8")));
	//			}
	//			if (title != null) {
	//				dataElements.add(new BERTLVObject(TITLE_TAG, title.trim().replace(' ', '<').getBytes("UTF-8")));
	//			}
	//			if (personalSummary != null) {
	//				dataElements.add(new BERTLVObject(PERSONAL_SUMMARY_TAG, personalSummary.trim().replace(' ', '<').getBytes("UTF-8")));
	//			}
	//			if (proofOfCitizenship != null) {
	//				ByteArrayOutputStream out = new ByteArrayOutputStream();
	//				ImageIO.write(proofOfCitizenship, "image/jpeg", out);
	//				dataElements.add(new BERTLVObject(PROOF_OF_CITIZENSHIP_TAG, out.toByteArray()));
	//			}
	//			if (otherValidTDNumbers != null) {
	//				boolean isFirstOne = true;
	//				ByteArrayOutputStream out = new ByteArrayOutputStream();
	//				for (String detail: otherValidTDNumbers) {
	//					if (detail != null) {
	//						if (isFirstOne) { isFirstOne = false; } else { out.write('<'); }
	//						out.write(detail.trim().replace(' ', '<').getBytes("UTF-8"));
	//					}
	//				}
	//				out.flush();
	//				dataElements.add(new BERTLVObject(OTHER_VALID_TD_NUMBERS_TAG, out.toByteArray()));
	//			}
	//			if (custodyInformation != null) {
	//				dataElements.add(new BERTLVObject(CUSTODY_INFORMATION_TAG, custodyInformation.trim().replace(' ', '<').getBytes("UTF-8")));
	//			}
	//
	//			/* Create tag list */
	//			ByteArrayOutputStream out = new ByteArrayOutputStream();
	//			DataOutputStream dataOut = new DataOutputStream(out);
	//			for (BERTLVObject dataElement: dataElements) {
	//				try {
	//					dataOut.writeShort(dataElement.getTag());
	//				} catch (IOException ioe) {
	//					/* NOTE: skip tag on error... */
	//				}
	//			}
	//			BERTLVObject tagList = new BERTLVObject(TAG_LIST_TAG, out.toByteArray());
	//			BERTLVObject dg11Object = new BERTLVObject(PassportFile.EF_DG11_TAG, tagList);
	//			for (BERTLVObject dataElement: dataElements) {
	//				dg11Object.addSubObject(dataElement);
	//			}
	//			byte[] result = dg11Object.getEncoded();
	//			return result;
	//		} catch (UnsupportedEncodingException uee) {
	//			/* NOTE: UTF-8 always supported... */
	//			uee.printStackTrace();
	//			return null;
	//		} catch (IOException ioe) {
	//			/* NOTE: write or flush on ByteArrayOutputStream never fails... */ 
	//			ioe.printStackTrace();
	//			return null;
	//		}
	//	}
}
