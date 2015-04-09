/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
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

package org.jmrtd.cbeff;

/**
 * CBEFF according to ISO 19785-1 (version 2.0) and NISTIR 6529-A (version 1.1).
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 * 
 * @since 0.4.7
 */
public interface CBEFFInfo {

	/**
	 * Biometric type value, based on
	 * Section 5.2.1.5 and Table 4 in NISTIR-6529A,
	 * Table C.2 in ISO/IEC 7816-11,
	 * Section 6.5.6 of ISO/IEC 19785-1.
	 */
	public static final int
	BIOMETRIC_TYPE_NO_INFORMATION_GIVEN = 0x000000,
	BIOMETRIC_TYPE_MULTIPLE_BIOMETRICS_USED = 0x000001,
	BIOMETRIC_TYPE_FACIAL_FEATURES = 0x000002,
	BIOMETRIC_TYPE_VOICE = 0x000004,
	BIOMETRIC_TYPE_FINGERPRINT = 0x000008,
	BIOMETRIC_TYPE_IRIS = 0x000010,
	BIOMETRIC_TYPE_RETINA = 0x000020,
	BIOMETRIC_TYPE_HAND_GEOMETRY = 0x000040,
	BIOMETRIC_TYPE_SIGNATURE_DYNAMICS = 0x000080,
	BIOMETRIC_TYPE_KEYSTROKE_DYNAMICS = 0x000100,
	BIOMETRIC_TYPE_LIP_MOVEMENT = 0x000200,
	BIOMETRIC_TYPE_THERMAL_FACE_IMAGE = 0x000400,
	BIOMETRIC_TYPE_THERMAL_HAND_IMAGE = 0x000800,
	BIOMETRIC_TYPE_GAIT = 0x001000,
	BIOMETRIC_TYPE_BODY_ODOR = 0x002000,
	BIOMETRIC_TYPE_DNA = 0x004000,
	BIOMETRIC_TYPE_EAR_SHAPE = 0x008000,
	BIOMETRIC_TYPE_FINGER_GEOMETRY = 0x010000,
	BIOMETRIC_TYPE_PALM_PRINT = 0x020000,
	BIOMETRIC_TYPE_VEIN_PATTERN = 0x040000,
	BIOMETRIC_TYPE_FOOT_PRINT = 0x080000;
	
	/**
	 * Biometric subtype, based on
	 * Section 5.1.2.6 and Table 6 in NISTIR-6529A,
	 * Table C.3 in ISO/IEC 7816-11,
	 * Section 6.5.7 of ISO/IEC 19785-1.
	 */
	public static final int
	BIOMETRIC_SUBTYPE_NONE = 0x00,					/* 00000000 */
	BIOMETRIC_SUBTYPE_MASK_RIGHT = 0x01,			/* xxxxxx01 */
	BIOMETRIC_SUBTYPE_MASK_LEFT = 0x02,				/* xxxxxx10 */
	BIOMETRIC_SUBTYPE_MASK_THUMB = 0x04,			/* xxx001xx */
	BIOMETRIC_SUBTYPE_MASK_POINTER_FINGER = 0x08,	/* xxx010xx */
	BIOMETRIC_SUBTYPE_MASK_MIDDLE_FINGER = 0x0C,	/* xxx011xx */
	BIOMETRIC_SUBTYPE_MASK_RING_FINGER = 0x10,		/* xxx100xx */
	BIOMETRIC_SUBTYPE_MASK_LITTLE_FINGER = 0x14;	/* xxx101xx */
}
