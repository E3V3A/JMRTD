/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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
 * $Id: BACEntry.java 764 2009-02-04 13:49:38Z martijno $
 */

package org.jmrtd;

import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A BAC key entry.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 764 $
 */
public class BACKeySpec implements KeySpec {

	private static final long serialVersionUID = -1059774581180524710L;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private String documentNumber;
	private String dateOfBirth;
	private String dateOfExpiry;

	public BACKeySpec(String documentNumber, String dateOfBirth, String dateOfExpiry) {
		if (documentNumber == null) {
			throw new IllegalArgumentException("Illegal document number");
		}
		while (documentNumber.length() < 9) { documentNumber += "<"; }
		this.documentNumber = documentNumber.trim();
		this.dateOfBirth = dateOfBirth;
		this.dateOfExpiry = dateOfExpiry;
	}
	
	public BACKeySpec(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		this(documentNumber, toString(dateOfBirth), toString(dateOfExpiry));
	}

	public String getDocumentNumber() {
		return documentNumber;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public String getDateOfExpiry() {
		return dateOfExpiry;
	}

	public String toString() {
		return documentNumber + ", " + dateOfBirth + ", " + dateOfExpiry;
	}

	public int hashCode() {
		int result = 5;
		result = 61 * result + (documentNumber == null ? 0 : documentNumber.hashCode());
		result = 61 * result + (dateOfBirth == null ? 0 : dateOfBirth.hashCode());
		result = 61 * result + (dateOfExpiry == null ? 0: dateOfExpiry.hashCode());
		return result;
	}

	public boolean equals(Object o) {
		if (o == null) { return false; }
		if (!o.getClass().equals(this.getClass())) { return false; }
		if (o == this) { return true; }
		BACKeySpec previous = (BACKeySpec)o;
		return documentNumber.equals(previous.documentNumber) &&
		dateOfBirth.equals(previous.dateOfBirth) &&
		dateOfExpiry.equals(previous.dateOfExpiry);
	}

	/**
	 * The algorithm of this key specification.
	 * 
	 * @return constant &quot;BAC&quot;
	 */
	public String getAlgorithm() {
		return "BAC";
	}

	/* FIXME: not implemented? -- MO */
	public byte[] getEncoded() {
		return null;
	}

	/* FIXME: not implemented? -- MO */
	public String getFormat() {
		return null;
	}

	private static synchronized String toString(Date date) {
		return SDF.format(date);
	}
}
