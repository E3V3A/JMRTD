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

package org.jmrtd;

/**
 * Trivial implementation of AuthListener.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 */
public class AuthAdapter implements AuthListener {

	/**
	 * Called when an attempt was made to perform the BAC protocol.
	 *
	 * @param be contains the resulting wrapper
	 */
	public void performedBAC(BACEvent be) { }

	/**
	 * Called when an attempt was made to perform the EAC protocol.
	 *
	 * @param ee contains the resulting wrapper and other EAC data
	 */
	public void performedEAC(EACEvent ee) { }

	/**
	 * Called when an attempt was made to perform the AA protocol.
	 *
	 * @param ae contains the used public key and resulting status of the protocol 
	 */
	public void performedAA(AAEvent ae) { }
}

