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

/**
 * Security features of this identity document.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
public class FeatureStatus {

	/**
	 * Outcome of a feature presence check.
	 * 
	 * @author The JMRTD team (info@jmrtd.org)
	 *
	 * @version $Revision$
	 */
	public enum Verdict {
		UNKNOWN,		/* Presence unknown */
		PRESENT,		/* Present */
		NOT_PRESENT;	/* Not present */
	};

	private Verdict hasSAC, hasBAC, hasAA, hasEAC;

	public FeatureStatus() {
		this.hasSAC = Verdict.UNKNOWN;
		this.hasBAC = Verdict.UNKNOWN;
		this.hasAA = Verdict.UNKNOWN;
		this.hasEAC = Verdict.UNKNOWN;
	}

	public void setSAC(Verdict hasSAC) {
		this.hasSAC = hasSAC;
	}
	
	public Verdict hasSAC() {
		return hasSAC;
	}

	
	public void setBAC(Verdict hasBAC) {
		this.hasBAC = hasBAC;
	}
	
	public Verdict hasBAC() {
		return hasBAC;
	}
	
	public void setAA(Verdict hasAA) {
		this.hasAA = hasAA;
	}
	
	public Verdict hasAA() {
		return hasAA;
	}
	
	public void setEAC(Verdict hasEAC) {
		this.hasEAC = hasEAC;
	}
	
	public Verdict hasEAC() {
		return hasEAC;
	}
}
