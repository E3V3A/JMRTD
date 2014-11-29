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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sf.scuba.util.Hex;

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
	private TerminalAuthenticationResult eacResult;
	private ActiveAuthenticationResult aaResult;
	
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
	
	/**
	 * Gets the AA reason string.
	 * 
	 * @return a reason string
	 */
	public String getAAReason() {
		return aaReason;
	}
	
	/**
	 * Gets the AA result.
	 * 
	 * @return the AA result
	 */
	public ActiveAuthenticationResult getAAResult() {
		return aaResult;
	}

	/**
	 * Sets the AA verdict.
	 * 
	 * @param v the status to set
	 * @param reason a reason string
	 * @param aaResult the result
	 */
	public void setAA(Verdict v, String reason, ActiveAuthenticationResult aaResult) {
		this.aa = v;
		this.aaReason = reason;
		this.aaResult = aaResult;
	}

	/**
	 * Gets the BAC verdict.
	 * 
	 * @return the BAC status
	 */
	public Verdict getBAC() {
		return bac;
	}
	
	/**
	 * Gets the BAC verdict string.
	 * 
	 * @return a verdict string
	 */
	public String getBACReason() {
		return bacReason;
	}

	/**
	 * Gets the tried BAC entries.
	 * 
	 * @return a list of BAC keys
	 */
	public List<BACKeySpec> getTriedBACEntries() {
		return triedBACEntries;
	}
	
	/**
	 * Sets the BAC verdict.
	 * 
	 * @param v the status to set
	 * @param reason a reason string
	 * @param triedBACEntries the list of BAC entries that were tried
	 */
	public void setBAC(Verdict v, String reason, List<BACKeySpec> triedBACEntries) {
		this.bac = v;
		this.bacReason = reason;
		this.triedBACEntries = triedBACEntries;
	}

	/**
	 * Gets the SAC verdict.
	 * 
	 * @return the SAC verdict
	 */
	public Verdict getSAC() {
		return sac;
	}

	/**
	 * Gets the SAC reason.
	 * 
	 * @return a reason string
	 */
	public String getSACReason() {
		return sacReason;
	}

	/**
	 * Sets the SAC verdict and reason string.
	 * 
	 * @param v a verdict
	 * @param reason a reason string
	 */
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
	
	/**
	 * Gets the country signature reason string.
	 * 
	 * @return a reason string
	 */
	public String getCSReason() {
		return csReason;
	}

	/**
	 * Gets the certificate chain between DS and CSCA.
	 * 
	 * @return a certificate chain
	 */
	public List<Certificate> getCertificateChain() {
		return certificateChain;
	}
	
	/**
	 * Gets the CS verdict.
	 * 
	 * @param v the status to set
	 * @param reason the reason string
	 * @param certificateChain the certificate chain between DS and CSCA
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
	
	/**
	 * Gets the document signature verdict reason string.
	 *
	 * @return a reason string
	 */
	public String getDSReason() {
		return dsReason;
	}
	
	
	/**
	 * Sets the DS verdict.
	 * 
	 * @param v the status to set
	 * @param reason reason string
	 */
	public void setDS(Verdict v, String reason) {
		this.ds = v;
		this.dsReason = reason;
	}
	
	/**
	 * Gets the hash table verdict.
	 * 
	 * @return a verdict
	 */
	public Verdict getHT() {
		return ht;
	}

	/**
	 * Gets the hash table reason string.
	 * 
	 * @return a reason string
	 */
	public String getHTReason() {
		return htReason;
	}

	/**
	 * Gets the hash match results.
	 * 
	 * @return a list of hash match results
	 */
	public Map<Integer, HashMatchResult> getHashResults() {
		return hashResults;
	}
	
	/**
	 * Sets the hash table status.
	 * 
	 * @param v a verdict
	 * @param reason the reason string
	 * @param hashResults the hash match results
	 */
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

	/**
	 * Gets the EAC reason string.
	 * 
	 * @return a reasons string
	 */
	public String getEACReason() {
		return eacReason;
	}
	
	/**
	 * Gets the EAC result.
	 * 
	 * @return the EAC result
	 */
	public TerminalAuthenticationResult getEACResult() {
		return eacResult;
	}
	
	/**
	 * Sets the EAC verdict.
	 * 
	 * @param v the status to set
	 * @param eacResult the EAC result
	 * @param reason reason string
	 */
	public void setEAC(Verdict v, String reason, TerminalAuthenticationResult eacResult) {
		this.eac = v;
		this.eacReason = reason;
		this.eacResult = eacResult;
	}

	/**
	 * Sets all vedicts to <code>v</code>.
	 * 
	 * @param verdict the status to set
	 * @param reason reason string
	 */
	public void setAll(Verdict verdict, String reason) {
		setAA(verdict, reason, null);
		setBAC(verdict, reason, null);
		setCS(verdict, reason, null);
		setDS(verdict, reason);
		setHT(verdict, reason, null);
		setEAC(verdict, reason, null);
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
		 * @param storedHash the hash stored in SOd
		 * @param computedHash the computed hash
		 */
		public HashMatchResult(byte[] storedHash, byte[] computedHash) {
			this.storedHash = storedHash;
			this.computedHash = computedHash;
		}
		
		/**
		 * Gets the stored hash.
		 * 
		 * @return a hash
		 */
		public byte[] getStoredHash() {
			return storedHash;
		}
		
		/**
		 * Gets the computed hash.
		 * 
		 * @return a hash
		 */
		public byte[] getComputedHash() {
			return computedHash;
		}

		/**
		 * Whether the hashes match.
		 * 
		 * @return a boolean
		 */
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
