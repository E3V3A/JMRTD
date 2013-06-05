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

import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

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
	
	/* Verdict for this verification feature. */
	private Verdict aa, bac, cs, ht, ds, eac;

	/* Textual reason for the verdict. */
	private String aaReason, bacReason, csReason, htReason, dsReason, eacReason;

	/* By products of the verification process that may be useful for relying parties to display. */
	private List<BACKey> triedBACEntries; /* As a result of BAC testing, this contains all tried BAC entries. */
	private Map<Integer, byte[]> storedHashes, computedHashes; /* As a result of HT testing, this contains stored and computed hashes. */
	private List<Certificate> certificateChain; /* As a result of CS testing, this contains certificate chain from DSC to CSCA. */
	
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
		this.aaReason = reason;
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

	public List<BACKey> getTriedBACEntries() {
		return triedBACEntries;
	}
	
	/**
	 * Sets the BAC verdict.
	 * 
	 * @param v the status to set
	 */
	public void setBAC(Verdict v, String reason, List<BACKey> triedBACEntries) {
		this.bac = v;
		this.bacReason = reason;
		this.triedBACEntries = triedBACEntries;
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

	public List<Certificate> getCertificateChain() {
		return certificateChain;
	}
	
	/**
	 * Gets the CS verdict.
	 * 
	 * @param v the status to set
	 */
	public void setCS(Verdict v, String reason, List<Certificate> certificateChain) {
		this.cs = v;
		this.csReason = reason;
		this.certificateChain = certificateChain;
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
		this.dsReason = reason;
	}
	
	public Verdict getHT() {
		return ht;
	}
	
	public String getHTReason() {
		return htReason;
	}
	
	public Map<Integer, byte[]> getStoredHashes() {
		return storedHashes;
	}
	
	public Map<Integer, byte[]> getComputedHashes() {
		return computedHashes;
	}

	public void setHT(Verdict v, String reason, Map<Integer, byte[]> storedHashes, Map<Integer, byte[]> computedHashes) {
		this.ht = v;
		this.htReason = reason;
		this.storedHashes = storedHashes;
		this.computedHashes = computedHashes;
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
		this.eacReason = reason;
	}
	
	/**
	 * Sets all vedicts to <code>v</code>.
	 * 
	 * @param verdict the status to set
	 */
	public void setAll(Verdict verdict, String reason) {
		setAA(verdict, reason);
		setBAC(verdict, reason, null);
		setCS(verdict, reason, null);
		setDS(verdict, reason);
		setHT(verdict, reason, null, null);
		setEAC(verdict, reason);
	}
}
