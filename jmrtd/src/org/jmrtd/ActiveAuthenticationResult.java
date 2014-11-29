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
 * $Id: $
 */

package org.jmrtd;

import java.security.PublicKey;
import java.util.Arrays;

/**
 * The result of an active authentication run.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 * 
 * @since 0.5.1
 */
public class ActiveAuthenticationResult {

	private PublicKey publicKey;
	private String digestAlgorithm;
	private String signatureAlgorithm;
	private byte[] challenge;
	private byte[] response;

	/**
	 * Creates a result.
	 * 
	 * @param challenge the challenge
	 * @param response the response
	 */
	public ActiveAuthenticationResult(PublicKey publicKey, String digestAlgorithm, String signatureAlgorithm, byte[] challenge, byte[] response) {
		this.publicKey = publicKey;
		this.digestAlgorithm = digestAlgorithm;
		this.signatureAlgorithm = signatureAlgorithm;
		this.challenge = challenge;
		this.response = response;
	}	
	
	/**
	 * Gets the public key.
	 * 
	 * @return a public key
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}

	/**
	 * Gets the digest algorithm.
	 * 
	 * @return a mnemonic Java algorithm string
	 */
	public String getDigestAlgorithm() {
		return digestAlgorithm;
	}

	/**
	 * Gets the signature algorithm.
	 * 
	 * @return a mnemonic Java algorithm string
	 */
	public String getSignatureAlgorithm() {
		return signatureAlgorithm;
	}

	/**
	 * Gets the challenge.
	 * 
	 * @return the challenge sent to the ICC
	 */
	public byte[] getChallenge() {
		return challenge;
	}

	/**
	 * Gets the response.
	 * 
	 * @return the response from the ICC
	 */
	public byte[] getResponse() {
		return response;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(challenge);
		result = prime * result
				+ ((digestAlgorithm == null) ? 0 : digestAlgorithm.hashCode());
		result = prime * result
				+ ((publicKey == null) ? 0 : publicKey.hashCode());
		result = prime * result + Arrays.hashCode(response);
		result = prime
				* result
				+ ((signatureAlgorithm == null) ? 0 : signatureAlgorithm
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ActiveAuthenticationResult other = (ActiveAuthenticationResult) obj;
		if (!Arrays.equals(challenge, other.challenge))
			return false;
		if (digestAlgorithm == null) {
			if (other.digestAlgorithm != null)
				return false;
		} else if (!digestAlgorithm.equals(other.digestAlgorithm))
			return false;
		if (publicKey == null) {
			if (other.publicKey != null)
				return false;
		} else if (!publicKey.equals(other.publicKey))
			return false;
		if (!Arrays.equals(response, other.response))
			return false;
		if (signatureAlgorithm == null) {
			if (other.signatureAlgorithm != null)
				return false;
		} else if (!signatureAlgorithm.equals(other.signatureAlgorithm))
			return false;
		return true;
	}
}
