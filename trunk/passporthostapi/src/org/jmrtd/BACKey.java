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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A BAC key entry.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
public class BACKey implements BACKeySpec {

	private static final long serialVersionUID = -1059774581180524710L;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private String documentNumber;
	private String dateOfBirth;
	private String dateOfExpiry;

	protected BACKey() {
	}
	
	public BACKey(String documentNumber, String dateOfBirth, String dateOfExpiry) {
		if (documentNumber == null) {
			throw new IllegalArgumentException("Illegal document number");
		}
		if (dateOfBirth == null || dateOfBirth.length() != 6) {
			throw new IllegalArgumentException("Illegal date: " + dateOfBirth);
		}
		if (dateOfExpiry == null || dateOfExpiry.length() != 6) {
			throw new IllegalArgumentException("Illegal date: " + dateOfExpiry);
		}
		while (documentNumber.length() < 9) { documentNumber += "<"; }
		this.documentNumber = documentNumber.trim();
		this.dateOfBirth = dateOfBirth;
		this.dateOfExpiry = dateOfExpiry;
	}
	
	public BACKey(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
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
		assert(dateOfBirth != null && dateOfBirth.length() == 6);
		assert(dateOfExpiry != null && dateOfExpiry.length() == 6);
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
		BACKey previous = (BACKey)o;
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
	
	protected void setDocumentNumber(String documentNumber) {
		this.documentNumber = documentNumber;
	}
	
	protected void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	protected void setDateOfExpiry(String dateOfExpiry) {
		this.dateOfExpiry = dateOfExpiry;
	}
	
	private static synchronized String toString(Date date) {
		return SDF.format(date);
	}
}
