/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2011  The JMRTD team
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

package org.jmrtd.cbeff;

/**
 * Constants interface representing ISO7816-11.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public interface ISO781611 {

	static final int BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG = 0x7F61;
	static final int BIOMETRIC_INFORMATION_TEMPLATE_TAG = 0x7F60;

	static final int BIOMETRIC_INFO_COUNT_TAG = 0x02;
	static final int BIOMETRIC_HEADER_TEMPLATE_BASE_TAG = (byte) 0xA1;
	static final int BIOMETRIC_DATA_BLOCK_TAG = 0x5F2E;
	static final int BIOMETRIC_DATA_BLOCK_CONSTRUCTED_TAG = 0x7F2E;
	
	static final int DISCRETIONARY_DATA_FOR_PAYLOAD_TAG = 0x53;
	static final int DISCRETIONARY_DATA_FOR_PAYLOAD_CONSTRUCTED_TAG = 0x73;
	
	/*
	 * FIXME: For 7F2E check ISO7816-11, Table 3: a 7F2E structure appears to include a 5F2E structure?
	 * Difference between primitive/constructed.
	 */

	/**
	 * From ISO7816-11: Secure Messaging Template tags.
	 */
	static final int
	SMT_TAG = 0x7D,
	SMT_DO_PV = 0x81,
	SMT_DO_CG = 0x85,
	SMT_DO_CC = 0x8E,
	SMT_DO_DS = 0x9E;
	
	/**
	 * Biometric Type tag, ISO7816-11.
	 */
	static final int BIOMETRIC_TYPE_TAG = 0x81;
	
	/**
	 * Biometric Subtype tag, ISO7816-11.
	 */
	static final int BIOMETRIC_SUBTYPE_TAG = 0x82;
	
	static final int CREATION_DATA_AND_TIME_TAG = 0x83;
	
	static final int VALIDITY_PERIOD_TAG = 0x85;
	
	static final int CREATOR_OF_BIOMETRIC_REFERENCE_DATA = 0x86;
	
	/**
	 * ID of the Group or Vendor which defined the BDB, specified in NISTIR-6529A and/or ISO7817-11.
	 */
	static final int FORMAT_OWNER_TAG = 0x87;

	/**
	 * BDB Format Type as specified by the Format Owner, specified in NISTIR-6529A and/or ISO7816-11.
	 */
	static final int FORMAT_TYPE_TAG = 0x88;
}