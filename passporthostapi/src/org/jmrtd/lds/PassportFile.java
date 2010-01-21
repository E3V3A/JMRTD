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

package org.jmrtd.lds;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.scuba.tlv.BERTLVInputStream;

import org.jmrtd.PassportService;


/**
 * Super class for passport files (EF_COM, EF_SOD, and data groups).
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public abstract class PassportFile
{
	/** ICAO specific datagroup tag. There is also the CVCA file that has no tag! */
	public static final int EF_COM_TAG = 0x60,
	EF_DG1_TAG = 0x61,
	EF_DG2_TAG = 0x75,
	EF_DG3_TAG = 0x63,
	EF_DG4_TAG = 0x76,
	EF_DG5_TAG = 0x65,
	EF_DG6_TAG = 0x66,
	EF_DG7_TAG = 0x67,
	EF_DG8_TAG = 0x68,
	EF_DG9_TAG = 0x69,
	EF_DG10_TAG = 0x6A,
	EF_DG11_TAG = 0x6B,
	EF_DG12_TAG = 0x6C,
	EF_DG13_TAG = 0x6D,
	EF_DG14_TAG = 0x6E,
	EF_DG15_TAG = 0x6F,
	EF_DG16_TAG = 0x70,
	EF_SOD_TAG = 0x77;

	/* 
	 * We're using a dual representation with a "dirty-bit": When the DG is
	 * read from a passport we need to store the binary information as-is
	 * since our constructed getEncoded() method might not result in exactly
	 * the same byte[] (messing up any cryptographic hash computations needed
	 * to validate the security object). -- MO
	 */
	//   BERTLVObject sourceObject;
	byte[] sourceObject; /* FIXME: always a byte[]? */
	boolean isSourceConsistent;

	/**
	 * Constructor only visible to the other
	 * classes in this package.
	 */
	PassportFile() {
	}

	/**
	 * Gets the contents of this file as byte array,
	 * includes the ICAO tag and length.
	 * 
	 * @return a byte array containing the file
	 */
	/*@ ensures
	 *@    isSourceConsistent ==> \result.equals(sourceObject);
	 */
	public abstract byte[] getEncoded();

	/**
	 * Factory method for creating passport files for a given input stream.
	 * 
	 * @param in a given input stream
	 * 
	 * @return a specific file
	 * 
	 * @throws IOException on reading error from the input stream
	 */
	public PassportFile createPassportFile(InputStream in) throws IOException {
		BERTLVInputStream tlvIn = new BERTLVInputStream(new BufferedInputStream(in, 8));
		tlvIn.mark(5);
		int tag = tlvIn.readTag();
		tlvIn.reset();
		switch (tag) {
		case EF_COM_TAG: return new COMFile(tlvIn);
		case EF_DG1_TAG: return new DG1File(tlvIn);
		case EF_DG2_TAG: return new DG2File(tlvIn);
		case EF_DG3_TAG: return new DG3File(tlvIn);
		case EF_DG4_TAG: return new DG4File(tlvIn);
		case EF_DG5_TAG: return new DG5File(tlvIn);
		case EF_DG6_TAG: return new DG6File(tlvIn);
		case EF_DG7_TAG: return new DG7File(tlvIn);
		case EF_DG8_TAG: throw new IllegalArgumentException("DG8 files are not yet supported");
		case EF_DG9_TAG: throw new IllegalArgumentException("DG9 files are not yet supported");
		case EF_DG10_TAG: throw new IllegalArgumentException("DG10 files are not yet supported");
		case EF_DG11_TAG: return new DG11File(tlvIn);
		case EF_DG12_TAG: return new DG12File(tlvIn);
		case EF_DG13_TAG: throw new IllegalArgumentException("DG13 files are not yet supported");
		case EF_DG14_TAG: return new DG14File(tlvIn);
		case EF_DG15_TAG: return new DG15File(tlvIn);
		case EF_DG16_TAG: throw new IllegalArgumentException("DG16 files are not yet supported");
		default:
			throw new NumberFormatException("Unknown tag " + Integer.toHexString(tag));   
		}
	}
	
	/**
	 * Finds a file identifier for an ICAO tag.
	 *
	 * Corresponds to Table A1 in ICAO-TR-LDS_1.7_2004-05-18.
	 *
	 * @param tag an ICAO tag (the first byte of the EF)
	 *
	 * @return a file identifier.
	 */
	public static short lookupFIDByTag(int tag) {
		switch(tag) {
		case EF_COM_TAG: return PassportService.EF_COM;
		case EF_DG1_TAG: return PassportService.EF_DG1;
		case EF_DG2_TAG: return PassportService.EF_DG2;
		case EF_DG3_TAG: return PassportService.EF_DG3;
		case EF_DG4_TAG: return PassportService.EF_DG4;
		case EF_DG5_TAG: return PassportService.EF_DG5;
		case EF_DG6_TAG: return PassportService.EF_DG6;
		case EF_DG7_TAG: return PassportService.EF_DG7;
		case EF_DG8_TAG: return PassportService.EF_DG8;
		case EF_DG9_TAG: return PassportService.EF_DG9;
		case EF_DG10_TAG: return PassportService.EF_DG10;
		case EF_DG11_TAG: return PassportService.EF_DG11;
		case EF_DG12_TAG: return PassportService.EF_DG12;
		case EF_DG13_TAG: return PassportService.EF_DG13;
		case EF_DG14_TAG: return PassportService.EF_DG14;
		case EF_DG15_TAG: return PassportService.EF_DG15;
		case EF_DG16_TAG: return PassportService.EF_DG16;
		case EF_SOD_TAG: return PassportService.EF_SOD;
		default:
			throw new NumberFormatException("Unknown tag " + Integer.toHexString(tag));
		}
	}

	/**
	 * Finds a data group number for an ICAO tag.
	 * 
	 * @param tag an ICAO tag (the first byte of the EF)
	 * 
	 * @return a data group number (1-16)
	 */
	public static int lookupDataGroupNumberByTag(int tag) {
		switch (tag) {
		case EF_DG1_TAG: return 1;
		case EF_DG2_TAG: return 2;
		case EF_DG3_TAG: return 3;
		case EF_DG4_TAG: return 4;
		case EF_DG5_TAG: return 5;
		case EF_DG6_TAG: return 6;
		case EF_DG7_TAG: return 7;
		case EF_DG8_TAG: return 8;
		case EF_DG9_TAG: return 9;
		case EF_DG10_TAG: return 10;
		case EF_DG11_TAG: return 11;
		case EF_DG12_TAG: return 12;
		case EF_DG13_TAG: return 13;
		case EF_DG14_TAG: return 14;
		case EF_DG15_TAG: return 15;
		case EF_DG16_TAG: return 16;
		default:
			throw new NumberFormatException("Unknown tag " + Integer.toHexString(tag));   
		}
	}

	/**
	 * Finds an ICAO tag for a data group number.
	 * 
	 * 
	 * @param number a data group number (1-16)
	 *
	 * @return an ICAO tag (the first byte of the EF)
	 */
	public static int lookupTagByDataGroupNumber(int number) {
		switch (number) {
		case 1: return EF_DG1_TAG;
		case 2: return EF_DG2_TAG;
		case 3: return EF_DG3_TAG;
		case 4: return EF_DG4_TAG;
		case 5: return EF_DG5_TAG;
		case 6: return EF_DG6_TAG;
		case 7: return EF_DG7_TAG;
		case 8: return EF_DG8_TAG;
		case 9: return EF_DG9_TAG;
		case 10: return EF_DG10_TAG;
		case 11: return EF_DG11_TAG;
		case 12: return EF_DG12_TAG;
		case 13: return EF_DG13_TAG;
		case 14: return EF_DG14_TAG;
		case 15: return EF_DG15_TAG;
		case 16: return EF_DG16_TAG;
		default:
			throw new NumberFormatException("Unknown number " + number);   
		}
	}

	/**
	 * Finds an ICAO tag for a data group number.
	 * 
	 * 
	 * @param number a data group number (1-16)
	 *
	 * @return a file identifier
	 */
	public static short lookupFIDByDataGroupNumber(int number) {
		switch (number) {
		case 1: return PassportService.EF_DG1;
		case 2: return PassportService.EF_DG2;
		case 3: return PassportService.EF_DG3;
		case 4: return PassportService.EF_DG4;
		case 5: return PassportService.EF_DG5;
		case 6: return PassportService.EF_DG6;
		case 7: return PassportService.EF_DG7;
		case 8: return PassportService.EF_DG8;
		case 9: return PassportService.EF_DG9;
		case 10: return PassportService.EF_DG10;
		case 11: return PassportService.EF_DG11;
		case 12: return PassportService.EF_DG12;
		case 13: return PassportService.EF_DG13;
		case 14: return PassportService.EF_DG14;
		case 15: return PassportService.EF_DG15;
		case 16: return PassportService.EF_DG16;
		default:
			throw new NumberFormatException("Unknown number " + number);   
		}
	}

	/**
	 * Finds an ICAO tag for a file identifier.
	 *
	 * Corresponds to Table A1 in ICAO-TR-LDS_1.7_2004-05-18.
	 *
	 * @param fid a file identifier
	 *
	 * @return a an ICAO tag (first byte of EF)
	 */
	public static short lookupTagByFID(short fid) {
		switch(fid) {
		case PassportService.EF_COM: return EF_COM_TAG;
		case PassportService.EF_DG1: return EF_DG1_TAG;
		case PassportService.EF_DG2: return EF_DG2_TAG;
		case PassportService.EF_DG3: return EF_DG3_TAG;
		case PassportService.EF_DG4: return EF_DG4_TAG;
		case PassportService.EF_DG5: return EF_DG5_TAG;
		case PassportService.EF_DG6: return EF_DG6_TAG;
		case PassportService.EF_DG7: return EF_DG7_TAG;
		case PassportService.EF_DG8: return EF_DG8_TAG;
		case PassportService.EF_DG9: return EF_DG9_TAG;
		case PassportService.EF_DG10: return EF_DG10_TAG;
		case PassportService.EF_DG11: return EF_DG11_TAG;
		case PassportService.EF_DG12: return EF_DG12_TAG;
		case PassportService.EF_DG13: return EF_DG13_TAG;
		case PassportService.EF_DG14: return EF_DG14_TAG;
		case PassportService.EF_DG15: return EF_DG15_TAG;
		case PassportService.EF_DG16: return EF_DG16_TAG;
		case PassportService.EF_SOD: return EF_SOD_TAG;
		default:
			throw new NumberFormatException("Unknown fid " + Integer.toHexString(fid));
		}
	}

	/**
	 * Returns a mnemonic name corresponding to the file represented by the
	 * given ICAO tag, such as "EF_COM", "EF_SOD", or "EF_DG1".
	 * 
	 * @param tag an ICAO tag (the first byte of the EF)
	 * 
	 * @return a mnemonic name corresponding to the file represented by the given ICAO tag
	 */
	public static String toString(int tag) {
		switch (tag) {
		case EF_COM_TAG: return "EF_COM";
		case EF_DG1_TAG: return "EF_DG1";
		case EF_DG2_TAG: return "EF_DG2";
		case EF_DG3_TAG: return "EF_DG3";
		case EF_DG4_TAG: return "EF_DG4";
		case EF_DG5_TAG: return "EF_DG5";
		case EF_DG6_TAG: return "EF_DG6";
		case EF_DG7_TAG: return "EF_DG7";
		case EF_DG8_TAG: return "EF_DG8";
		case EF_DG9_TAG: return "EF_DG9";
		case EF_DG10_TAG: return "EF_DG10";
		case EF_DG11_TAG: return "EF_DG11";
		case EF_DG12_TAG: return "EF_DG12";
		case EF_DG13_TAG: return "EF_DG13";
		case EF_DG14_TAG: return "EF_DG14";
		case EF_DG15_TAG: return "EF_DG15";
		case EF_DG16_TAG: return "EF_DG16";
		case EF_SOD_TAG: return "EF_SOD";
		default:
			throw new NumberFormatException("Unknown tag " + Integer.toHexString(tag));   
		}
	}
}
