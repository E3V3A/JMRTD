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
 * $Id$
 */

package org.jmrtd.lds;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * File structure for the EF_DG15 file.
 * Datagroup 15 contains the public key used in AA.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class DG15File extends DataGroup {

	private static final long serialVersionUID = 3834304239673755744L;

	private PublicKey publicKey;

	/**
	 * Constructs a new file.
	 * 
	 * @param publicKey the key to store in this file
	 */
	public DG15File(PublicKey publicKey) {
		super(EF_DG15_TAG);
		this.publicKey = publicKey;
	}

	/**
	 * Constructs a new file from binary representation.
	 * 
	 * @param in an input stream
	 */
	public DG15File(InputStream in) throws IOException {
		super(EF_DG15_TAG, in);
	}
	
	protected void readContent(InputStream inputStream) throws IOException {
		DataInputStream dataInputStream = inputStream instanceof DataInputStream ? (DataInputStream)inputStream : new DataInputStream(inputStream);
		try {
			byte[] value = new byte[getLength()];
			dataInputStream.readFully(value);
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(value);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			publicKey = keyFactory.generatePublic(pubKeySpec);
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException(e.toString());
		}
	}

	protected void writeContent(OutputStream out) throws IOException {
		out.write(publicKey.getEncoded());
	}

	/**
	 * Gets the public key stored in this file.
	 * 
	 * @return the public key
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}
	
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj.getClass() != this.getClass()) { return false; }
		DG15File other = (DG15File)obj;
		return publicKey.equals(other.publicKey);
	}
	
	public int hashCode() {
		return 5 * publicKey.hashCode() + 61;
	}
	
	public String toString() {
		return "DG15File [" + publicKey.toString() + "]";
	}
}
