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
 * $Id: PassportFile.java 1320 2011-04-25 19:53:43Z martijno $
 */

package org.jmrtd.cbeff;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A Standard Biometric Header preceeds a Biometric Data Block.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 * 
 * @since 0.4.7
 */
public class StandardBiometricHeader implements Serializable {

	private static final long serialVersionUID = 4113147521594478513L;

	private SortedMap<Integer, byte[]> elements;
	
	/**
	 * Constructs a standard biometric header.
	 * 
	 * @param elements the elements, consisting of a tag and value
	 */
	public StandardBiometricHeader(Map<Integer, byte[]> elements) {
		this.elements = new TreeMap<Integer, byte[]>(elements);
	}

	/**
	 * Gets the elements of this standard biometric header.
	 * 
	 * @return the elements, each consisting of a tag and value
	 */
	public SortedMap<Integer, byte[]> getElements() {
		return new TreeMap<Integer, byte[]>(elements);
	}
}
