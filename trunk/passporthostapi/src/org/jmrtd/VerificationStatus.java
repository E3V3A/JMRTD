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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sourceforge.scuba.util.Hex;

/**
 * A data type for communicating document verification check information.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 */
public class VerificationStatus {

	/**
	 * Outcome of a verification process.
	 * 
	 * @author The JMRTD team (info@jmrtd.org)
	 *
	 * @version $Revision$
	 */
	public enum Verdict {
		UNKNOWN,		/* Unknown */
		NOT_PRESENT,	/* Not present */
		NOT_CHECKED,	/* Present, not checked */
		FAILED,			/* Present, checked, and not ok */
		SUCCEEDED;		/* Present, checked, and ok */
	};
	
	/* Verdict for this verification feature. */
	private Verdict aa, bac, sac, cs, ht, ds, eac;

	/* Textual reason for the verdict. */
	private String aaReason, bacReason, sacReason, csReason, htReason, dsReason, eacReason;

	/* By products of the verification process that may be useful for relying parties to display. */
	private List<BACKeySpec> triedBACEntries; /* As a result of BAC testing, this contains all tried BAC entries. */
	private Map<Integer, HashMatchResult> hashResults; /* As a result of HT testing, this contains stored and computed hashes. */
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

	public List<BACKeySpec> getTriedBACEntries() {
		return triedBACEntries;
	}
	
	/**
	 * Sets the BAC verdict.
	 * 
	 * @param v the status to set
	 */
	public void setBAC(Verdict v, String reason, List<BACKeySpec> triedBACEntries) {
		this.bac = v;
		this.bacReason = reason;
		this.triedBACEntries = triedBACEntries;
	}

	public Verdict getSAC() {
		return sac;
	}
	
	public String getSACReason() {
		return sacReason;
	}

	public void setSAC(Verdict v, String reason) {
		this.sac = v;
		this.sacReason = reason;
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
	
	public Map<Integer, HashMatchResult> getHashResults() {
		return hashResults;
	}
	
	public void setHT(Verdict v, String reason, Map<Integer, HashMatchResult> hashResults) {
		this.ht = v;
		this.htReason = reason;
		this.hashResults = hashResults;
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
		setHT(verdict, reason, null);
		setEAC(verdict, reason);
	}
	
	/**
	 * The result of matching the stored and computed hashes of a single datagroup.
	 * 
	 * FIXME: perhaps that boolean should be more like verdict, including a reason for mismatch if known (e.g. access denied for EAC datagroup) -- MO
	 */
	public static class HashMatchResult implements Serializable {
	
		private static final long serialVersionUID = 263961258911936111L;

		private byte[] storedHash, computedHash;
		
		/**
		 * Use <code>null</code> for computed hash if access was denied.
		 * 
		 * @param storedHash
		 * @param computedHash
		 */
		public HashMatchResult(byte[] storedHash, byte[] computedHash) {
			this.storedHash = storedHash;
			this.computedHash = computedHash;
		}
		
		public byte[] getStoredHash() {
			return storedHash;
		}
		
		public byte[] getComputedHash() {
			return computedHash;
		}

		public boolean isMatch() {
			return Arrays.equals(storedHash, computedHash);
		}
		
		public String toString() {
			return "HashResult [" + isMatch() + ", stored: " + Hex.bytesToHexString(storedHash) + ", computed: " + Hex.bytesToHexString(computedHash);
		}
		
		public int hashCode() {
			return 11 + 3 * Arrays.hashCode(storedHash) + 5 * Arrays.hashCode(computedHash);
		}
		
		public boolean equals(Object other) {
			if (other == null) { return false; }
			if (other == this) { return true; }
			if (!other.getClass().equals(this.getClass())) { return false; }
			HashMatchResult otherHashResult = (HashMatchResult)other;
			return Arrays.equals(otherHashResult.computedHash, computedHash)
					&& Arrays.equals(otherHashResult.storedHash, storedHash);
		}
		
		/* NOTE: Part of our serializable implementation. */
		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
//			inputStream.defaultReadObject();
			storedHash = readBytes(inputStream);
			computedHash = readBytes(inputStream);
		}

		/* NOTE: Part of our serializable implementation. */
		private void writeObject(ObjectOutputStream outputStream) throws IOException {
//			outputStream.defaultWriteObject();
			writeByteArray(storedHash, outputStream);
			writeByteArray(computedHash, outputStream);
		}

		private byte[] readBytes(ObjectInputStream inputStream) throws IOException {
			int length = inputStream.readInt();
			if (length < 0) {
				return null;
			}
			byte[] bytes = new byte[length];
			for (int i = 0; i < length; i++) {
				int b = inputStream.readInt();
				bytes[i] = (byte)b;
			}
			return bytes;
		}
		
		private void writeByteArray(byte[] bytes, ObjectOutputStream outputStream) throws IOException {
			if (bytes == null) {
				outputStream.writeInt(-1);
			} else {
				outputStream.writeInt(bytes.length);
				for (byte b: bytes) {
					outputStream.writeInt(b);
				}
			}
		}
	}
}
