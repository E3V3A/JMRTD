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
 * $Id: $
 */

package org.jmrtd.cbeff;

import java.io.Serializable;

/**
 * Biometric data block.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 * 
 * @since 0.4.7
 */
public interface BiometricDataBlock extends Serializable {

	/**
	 * Gets the standard biometric header of this biometric data block
	 * 
	 * @return the standard biometric header
	 */
	public StandardBiometricHeader getStandardBiometricHeader();	
}
