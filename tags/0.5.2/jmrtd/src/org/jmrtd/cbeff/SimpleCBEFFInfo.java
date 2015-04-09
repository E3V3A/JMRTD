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
 * Simple CBEFF BIR. 
 * Specified in ISO 19785-1 (version 2.0) and NISTIR 6529-A (version 1.1).
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 * 
 * @param <B> the embedded BDB
 * 
 * @since 0.4.7
 */
public class SimpleCBEFFInfo<B extends BiometricDataBlock> implements CBEFFInfo {

	private B bdb;
	
	/**
	 * Constructs a simple CBEFF info from the given BDB.
	 * 
	 * @param bdb a biometric data block
	 */
	public SimpleCBEFFInfo(B bdb) {
		this.bdb = bdb;
	}
	
	/**
	 * Gets the biometric data block from this simple CBEFF info.
	 * 
	 * @return a biometric data block
	 */
	public B getBiometricDataBlock() {
		return bdb;
	}
}
