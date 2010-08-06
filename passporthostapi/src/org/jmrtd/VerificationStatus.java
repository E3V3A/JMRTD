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
 * $Id$
 */

package org.jmrtd;

/**
 * A data type for communicating document verification check information.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class VerificationStatus
{
	public enum Verdict {
		UNKNOWN,	/* Not present or not checked */
		SUCCEEDED,	/* Present, checked, and ok */
		FAILED,		/* Present, checked, and not ok */
		NOT_PRESENT	/* Not present */
	};

	private Verdict aa, bac, cs, ds, eac;

	/**
	 * Constructs a new status with all verdicts
	 * set to <code>UNKNOWN</code>.
	 */
	public VerificationStatus() {
		setAA(Verdict.UNKNOWN);
		setBAC(Verdict.UNKNOWN);
		setCS(Verdict.UNKNOWN);
		setDS(Verdict.UNKNOWN);
		setEAC(Verdict.UNKNOWN);
	}
	
	/**
	 * @return the AA status
	 */
	public Verdict getAA() {
		return aa;
	}

	/**
	 * @param v the status to set
	 */
	public void setAA(Verdict v) {
		this.aa = v;
	}

	/**
	 * @return the BAC status
	 */
	public Verdict getBAC() {
		return bac;
	}

	/**
	 * @param v the status to set
	 */
	public void setBAC(Verdict v) {
		this.bac = v;
	}

	/**
	 * @return the CS status
	 */
	public Verdict getCS() {
		return cs;
	}

	/**
	 * @param v the status to set
	 */
	public void setCS(Verdict v) {
		this.cs = v;
	}

	/**
	 * @return the DS status
	 */
	public Verdict getDS() {
		return ds;
	}

	/**
	 * @param v the status to set
	 */
	public void setDS(Verdict v) {
		this.ds = v;
	}

	/**
	 * @return the EAC status
	 */
	public Verdict getEAC() {
		return eac;
	}

	/**
	 * @param v the status to set
	 */
	public void setEAC(Verdict v) {
		this.eac = v;
	}
	
	public void setAll(Verdict v) {
		setAA(v);
		setBAC(v);
		setCS(v);
		setDS(v);
		setEAC(v);
	}
}
