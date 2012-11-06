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
 * $Id: $
 */

package org.jmrtd.lds;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jmrtd.cert.CVCPrincipal;

/**
 * File structure for CVCA file (on EAC protected documents).
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
/* TODO: Use CVCPrincipal instead of String for references? */
public class CVCAFile extends AbstractLDSFile {

	private static final long serialVersionUID = -1100904058684365703L;

	public static final byte CAR_TAG = 0x42;
	public static final int LENGTH = 36;

	private String caReference = null;

	private String altCaReference = null;

	/**
	 * Constructs a new CVCA file from the data contained in <code>in</code>.
	 * 
	 * @param in stream with the data to be parsed
	 */
	public CVCAFile(InputStream in) {
		try {
			readObject(in);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Malformed input data");
		}
	}

	/**
	 * Constructs a new CVCA file with the given certificate references
	 * 
	 * @param caReference
	 *            main CA certificate reference
	 * @param altCaReference
	 *            second (alternative) CA certificate reference
	 */
	public CVCAFile(String caReference, String altCaReference) {
		if (caReference == null || caReference.length() > 16
				|| (altCaReference != null && altCaReference.length() > 16)) {
			throw new IllegalArgumentException();
		}
		this.caReference = caReference;
		this.altCaReference = altCaReference;
	}

	/**
	 * Constructs a new CVCA file with the given certificate reference
	 * 
	 * @param caReference
	 *            main CA certificate reference
	 */
	public CVCAFile(String caReference) {
		this(caReference, null);
	}


	protected void readObject(InputStream in) throws IOException {
		DataInputStream dataIn = new DataInputStream(in);
		int tag = dataIn.read();
		if (tag != CAR_TAG) { throw new IllegalArgumentException("Wrong tag."); }
		int length = dataIn.read();
		if (length > 16) { throw new IllegalArgumentException("Wrong length."); }
		byte[] data = new byte[length];
		dataIn.readFully(data);
		caReference = new String(data);
		tag = dataIn.read();
		if (tag != 0) {
			if (tag != CAR_TAG) { throw new IllegalArgumentException("Wrong tag."); }
			length = dataIn.read();
			if (length > 16) { throw new IllegalArgumentException("Wrong length."); }
			data = new byte[length];
			dataIn.readFully(data);
			altCaReference = new String(data);
			tag = dataIn.read();
		}
		while (tag != -1) {
			if (tag != 0) { throw new IllegalArgumentException("Bad file padding."); }
			tag = dataIn.read();
		}
	}
	
	protected void writeObject(OutputStream out) throws IOException {
		byte[] result = new byte[LENGTH];
		result[0] = CAR_TAG;
		result[1] = (byte)caReference.length();
		System.arraycopy(caReference.getBytes(), 0, result, 2, result[1]);
		if (altCaReference != null) {
			int index = result[1] + 2;
			result[index] = CAR_TAG;
			result[index + 1] = (byte) altCaReference.length();
			System.arraycopy(altCaReference.getBytes(), 0, result, index + 2,
					result[index + 1]);
		}
		out.write(result);
	}

	/**
	 * Returns the CA Certificate identifier
	 * 
	 * @return the CA Certificate identifier
	 */
	public CVCPrincipal getCAReference() {
		return caReference == null ? null : new CVCPrincipal(caReference);
	}

	/**
	 * Returns the second (alternative) CA Certificate identifier, null if none
	 * exists.
	 * 
	 * @return the second (alternative) CA Certificate identifier
	 */
	public CVCPrincipal getAltCAReference() {
		return altCaReference == null ? null : new CVCPrincipal(altCaReference);
	}

	/**
	 * Gets a textual representation of this CVCAFile.
	 * 
	 * @return a textual representation of this CVCAFile
	 */
	public String toString() {
		return "CA reference: \"" + caReference + "\""
		+ ((altCaReference != null) ? ", Alternative CA reference: " + altCaReference : "");
	}

	/**
	 * Tests whether this CVCAFile is equal to the provided object.
	 * 
	 * @param other some other object
	 * 
	 * @return whether this CVCAFile equals the other object
	 */
	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (!this.getClass().equals(other.getClass())) { return false; }
		CVCAFile otherCVCAFile = (CVCAFile)other;
		return caReference.equals(otherCVCAFile.caReference)
		&& ((altCaReference == null && otherCVCAFile.altCaReference == null)
				|| (altCaReference != null && altCaReference.equals(otherCVCAFile.altCaReference)));
	}

	/**
	 * Computes a hash code of this CVCAFile.
	 * 
	 * @return a hash code
	 */
	public int hashCode() {
		return 11 * caReference.hashCode()
		+ ((altCaReference != null) ? 13 * altCaReference.hashCode() : 0)
		+ 5;
	}
	
	/**
	 * Gets the length of the content of this CVCA file. This always returns {@value #LENGTH}.
	 * 
	 * @return {@value #LENGTH}
	 */
	public int getLength() {
		return LENGTH;
	}
}
