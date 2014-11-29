/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2014  The JMRTD team
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

import net.sf.scuba.smartcards.CardServiceException;

/**
 * An exception to signal errors during execution of the PACE protocol.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class PACEException extends CardServiceException {

	private static final long serialVersionUID = 8383980807753919040L;

	/**
	 * Creates a PACEException.
	 * 
	 * @param msg a message
	 */
	public PACEException(String msg) {
		super(msg);
	}

	/**
	 * Creates a PACEException with a specific status word.
	 * 
	 * @param msg a message
	 * @param sw the status word that caused this CardServiceException
	 */
	public PACEException(String msg, int sw) {
		super(msg, sw);
	}
}
