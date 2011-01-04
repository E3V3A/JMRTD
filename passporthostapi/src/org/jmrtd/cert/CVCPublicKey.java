/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2011  The JMRTD team
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

public class CVCPublicKey implements PublicKey {

	private static final long serialVersionUID = -3329395619057945924L;

	private org.ejbca.cvc.CVCPublicKey publicKey;
	
	protected CVCPublicKey(org.ejbca.cvc.CVCPublicKey publicKey) {
		this.publicKey = publicKey;
	}
	
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

	public byte[] getEncoded() {
		return publicKey.getEncoded();
	}

	public String getFormat() {
		return publicKey.getFormat();
	}
	
	public String toString() {
		return publicKey.toString();
	}
	
	public boolean equals(Object otherObj) {
		if (otherObj == null) { return false; }
		if (otherObj == this) { return true; }
		if (!this.getClass().equals(otherObj.getClass())) { return false; }
		return this.publicKey.equals(((CVCPublicKey)otherObj).publicKey);
	}
	
	public int hashCode() {
		return publicKey.hashCode() * 3 + 309011;
	}
}
