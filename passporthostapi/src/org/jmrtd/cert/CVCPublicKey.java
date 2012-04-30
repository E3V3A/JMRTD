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
 * $Id:  $
 */

package org.jmrtd.cert;

import java.security.PublicKey;

import org.ejbca.cvc.AlgorithmUtil;
import org.ejbca.cvc.OIDField;

/**
 * Card verifiable certificate public key.
 * This just wraps the EJBCA implementation.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class CVCPublicKey implements PublicKey {

	private static final long serialVersionUID = -3329395619057945924L;

	private org.ejbca.cvc.CVCPublicKey publicKey;
	
	protected CVCPublicKey(org.ejbca.cvc.CVCPublicKey publicKey) {
		this.publicKey = publicKey;
	}
	
    /**
     * Gets the standard algorithm name for this key. For
     * example, "DSA" would indicate that this key is a DSA key.
     * See Appendix A in the <a href=
     * "../../../technotes/guides/security/crypto/CryptoSpec.html#AppA">
     * Java Cryptography Architecture API Specification &amp; Reference </a>
     * for information about standard algorithm names.
     *
     * @return the name of the algorithm associated with this key.
     */
	public String getAlgorithm() {
		OIDField oid = null;
		try {
			oid = publicKey.getObjectIdentifier();
			return AlgorithmUtil.getAlgorithmName(publicKey.getObjectIdentifier());
		} catch (Exception e) {
			e.printStackTrace();
			String algorithm = publicKey.getAlgorithm();
			if (algorithm != null) { return algorithm; }
			return oid.getAsText();
		}
	}

    /**
     * Gets the key in its primary encoding format, or null
     * if this key does not support encoding.
     *
     * @return the encoded key, or null if the key does not support
     * encoding.
     */
	public byte[] getEncoded() {
		return publicKey.getEncoded();
	}

    /**
     * Gets the name of the primary encoding format of this key,
     * or null if this key does not support encoding.
     *
     * @return the primary encoding format of the key.
     */
	public String getFormat() {
		return publicKey.getFormat();
	}
	
	/**
	 * Gets a textual representation of this key.
	 * 
	 * @return a string
	 */
	@Override
	public String toString() {
		return publicKey.toString();
	}
	
	/**
	 * Determines if this key equals the other object.
	 * 
	 * @param otherObj some other object
	 * 
	 * @return whether this key equals the other object
	 */
	public boolean equals(Object otherObj) {
		if (otherObj == null) { return false; }
		if (otherObj == this) { return true; }
		if (!this.getClass().equals(otherObj.getClass())) { return false; }
		return this.publicKey.equals(((CVCPublicKey)otherObj).publicKey);
	}

	/**
	 * Computes a hash code for this key.
	 * 
	 * @return a hash code for this key
	 */
	public int hashCode() {
		return publicKey.hashCode() * 3 + 309011;
	}
}
