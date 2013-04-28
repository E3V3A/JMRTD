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
 * $Id: PassportFile.java 1320 2011-04-25 19:53:43Z martijno $
 */

package org.jmrtd.lds;

/**
 * LDS element at file level.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
public interface LDSFile extends LDSElement {

	/**
	 * ICAO specific datagroup tag.
	 * In EAC documents there is also the CVCA file that has no tag!
	 */
	public static final int
	EF_COM_TAG = 0x60,
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
	
	int getLength();
}
