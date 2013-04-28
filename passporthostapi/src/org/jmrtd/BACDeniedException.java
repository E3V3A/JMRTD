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
 * $Id: $
 */

package org.jmrtd;

import java.util.List;

import net.sourceforge.scuba.smartcards.CardServiceException;

/**
 * Exception for signaling failed BAC.
 * 
 * @author The JMRTD team
 * 
 * @version $Revision: $
 * 
 * @since 0.4.8
 */
public class BACDeniedException extends CardServiceException {

	private static final long serialVersionUID = -7094953658210693249L;
	
	private List<BACKey> triedEntries;
	
	/**
	 * Creates an exception.
	 * 
	 * @param msg the message
	 * @param triedEntries the BAC entries that were tried before BAC failed
	 */
	public BACDeniedException(String msg, List<BACKey> triedEntries, int sw) {
		super(msg, sw);
	}
	
	/**
	 * Gets the BAC entries that were tried before BAC failed
	 * 
	 * @return a list
	 */
	public List<BACKey> getTriedEntries() {
		return triedEntries;
	}
}
