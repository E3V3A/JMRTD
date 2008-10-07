/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

package sos.mrtd;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import sos.tlv.BERTLVInputStream;
import sos.tlv.BERTLVObject;

/**
 * File structure for the EF_DG15 file.
 * Datagroup 15 contains the public key used in AA.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class DG15File extends DataGroup
{
	private PublicKey publicKey;

	/**
	 * Constructs a new file.
	 * 
	 * @param publicKey the key to store in this file
	 */
	public DG15File(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public DG15File(InputStream in) {
		try {
			BERTLVInputStream tlvIn = new BERTLVInputStream(in);
			tlvIn.readTag();
			tlvIn.readLength();
			byte[] value = tlvIn.readValue();
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(value);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			publicKey = keyFactory.generatePublic(pubKeySpec);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.toString());
		}
	}

	public byte[] getEncoded() {
		if (isSourceConsistent) {
			return sourceObject.getEncoded();
		}
		try {
			BERTLVObject ef010F =
				new BERTLVObject(PassportFile.EF_DG15_TAG,
						publicKey.getEncoded());
			sourceObject = ef010F;
			isSourceConsistent = true;
			return ef010F.getEncoded();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getTag() {
		return EF_DG15_TAG;
	}

	/**
	 * Gets the public key stored in this file.
	 * 
	 * @return the public key
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}
}
