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

import java.security.PublicKey;
import java.util.Arrays;
import java.util.EventObject;

/**
 * Event to indicate AA protocol was executed.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
public class AAEvent extends EventObject {

	private static final long serialVersionUID = 3597146485237004531L;

	private PublicKey pubkey;
	private byte[] m1;
	private byte[] m2;
	private boolean success;

	/**
	 * Constructs a new event.
	 * 
	 * @param src event source
	 * @param pubkey public key
	 * @param m1 recoverable part
	 * @param m2 nonce sent by host
	 * @param success resulting status of authentication protocol
	 */
	public AAEvent(Object src, PublicKey pubkey, byte[] m1, byte[] m2, boolean success) {
		super(src);
		this.pubkey = pubkey;
		this.m1 = m1;
		this.m2 = m2;
		this.success = success;
	}

	/**
	 * Gets the public key used in the protocol.
	 * 
	 * @return a public key
	 */
	public PublicKey getPubkey() {
		return pubkey;
	}

	/**
	 * Gets m1.
	 * 
	 * @return m1
	 */
	public byte[] getM1() {
		return m1;
	}

	/**
	 * Gets m2.
	 * 
	 * @return m2
	 */
	public byte[] getM2() {
		return m2;
	}

	/**
	 * Indicates whether the authentication protocol
	 * was successfully executed.
	 * 
	 * @return status of the protocol
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Gets a textual representation of this event.
	 * 
	 * @return a textual representation of this event
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("AAEvent [");
		result.append("m1 = " + Arrays.toString(m1) + ", ");
		result.append("m2 = " + Arrays.toString(m2) + ", ");
		result.append(success);
		result.append("]");
		return result.toString();
	}
}
