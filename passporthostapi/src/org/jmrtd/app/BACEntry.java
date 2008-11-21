/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

package org.jmrtd.app;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A BAC key entry.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class BACEntry
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private String documentNumber;
	private Date dateOfBirth;
	private Date dateOfExpiry;

	public BACEntry(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		this.documentNumber = documentNumber.trim();
		this.dateOfBirth = dateOfBirth;
		this.dateOfExpiry = dateOfExpiry;
	}

	public String getDocumentNumber() {
		return documentNumber;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public Date getDateOfExpiry() {
		return dateOfExpiry;
	}

	public String toString() {
		return documentNumber + ", " + SDF.format(dateOfBirth) + ", " + SDF.format(dateOfExpiry);
	}
	
	public int hashCode() {
		int result = 5;
		result = 61 * result + (documentNumber == null ? 0 : documentNumber.hashCode());
		result = 61 * result + (dateOfBirth == null ? 0 : dateOfBirth.hashCode());
		result = 61 * result + (dateOfExpiry == null ? 0: dateOfExpiry.hashCode());
		return result;
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other.getClass() != BACEntry.class) { return false; }
		if (other == this) { return true; }
		BACEntry previous = (BACEntry)other;
		return documentNumber.equals(previous.documentNumber) &&
		dateOfBirth.equals(previous.dateOfBirth) &&
		dateOfExpiry.equals(previous.dateOfExpiry);
	}
}
