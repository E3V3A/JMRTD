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
 * $Id: VerificationStatus.java 1382 2012-03-17 22:04:23Z martijno $
 */

package org.jmrtd;

/**
 * A data type for communicating document verification check information.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: 1382 $
 */
public class VerificationStatus {

	/**
	 * Outcome of a verification process.
	 * 
	 * @author The JMRTD team (info@jmrtd.org)
	 *
	 * @version $Revision: 1382 $
	 */
	public enum Verdict {
		UNKNOWN,		/* Unknown */
		NOT_PRESENT,	/* Not present */
		NOT_CHECKED,	/* Present, not checked */
		FAILED,			/* Present, checked, and not ok */
		SUCCEEDED;		/* Present, checked, and ok */
	};

	private Verdict aa, bac, cs, hashes, ds, eac;
	private String aaReason, bacReason, csReason, hashesReason, dsReason, eacReason;

	/**
	 * Constructs a new status with all verdicts
	 * set to <code>UNKNOWN</code>.
	 */
	public VerificationStatus() {
		setAll(Verdict.UNKNOWN, null);
	}
	
	/**
	 * Gets the AA verdict.
	 * 
	 * @return the AA status
	 */
	public Verdict getAA() {
		return aa;
	}
	
	public String getAAReason() {
		return aaReason;
	}

	/**
	 * Sets the AA verdict.
	 * 
	 * @param v the status to set
	 */
	public void setAA(Verdict v, String reason) {
		this.aa = v;
		aaReason = reason;
	}

	/**
	 * Gets the BAC verdict.
	 * 
	 * @return the BAC status
	 */
	public Verdict getBAC() {
		return bac;
	}
	
	public String getBACReason() {
		return bacReason;
	}

	/**
	 * Sets the BAC verdict.
	 * 
	 * @param v the status to set
	 */
	public void setBAC(Verdict v, String reason) {
		this.bac = v;
		bacReason = reason;
	}

	/**
	 * Gets the CS verdict.
	 * 
	 * @return the CS status
	 */
	public Verdict getCS() {
		return cs;
	}
	
	public String getCSReason() {
		return csReason;
	}

	/**
	 * Gets the CS verdict.
	 * 
	 * @param v the status to set
	 */
	public void setCS(Verdict v, String reason) {
		this.cs = v;
		csReason = reason;
	}

	/**
	 * Gets the DS verdict.
	 * 
	 * @return the DS status
	 */
	public Verdict getDS() {
		return ds;
	}
	
	public String getDSReason() {
		return dsReason;
	}
	
	
	/**
	 * Sets the DS verdict.
	 * 
	 * @param v the status to set
	 */
	public void setDS(Verdict v, String reason) {
		this.ds = v;
		dsReason = reason;
	}
	
	public Verdict getHT() {
		return hashes;
	}
	
	public String getHTReason() {
		return hashesReason;
	}

	public void setHT(Verdict v, String reason) {
		this.hashes = v;
		hashesReason = reason;
	}

	/**
	 * Gets the EAC verdict.
	 * 
	 * @return the EAC status
	 */
	public Verdict getEAC() {
		return eac;
	}

	public String getEACReason() {
		return eacReason;
	}
	
	/**
	 * Sets the EAC verdict.
	 * 
	 * @param v the status to set
	 */
	public void setEAC(Verdict v, String reason) {
		this.eac = v;
	}
	
	/**
	 * Sets all vedicts to <code>v</code>.
	 * 
	 * @param verdict the status to set
	 */
	public void setAll(Verdict verdict, String reason) {
		setAA(verdict, reason);
		setBAC(verdict, reason);
		setCS(verdict, reason);
		setDS(verdict, reason);
		setHT(verdict, reason);
		setEAC(verdict, reason);
	}
}
