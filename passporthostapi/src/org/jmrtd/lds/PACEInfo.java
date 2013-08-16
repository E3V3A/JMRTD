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

package org.jmrtd.lds;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DLSequence;

/**
 * PACE Info object as per SAC TR 1.01, November 11, 2010.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 * 
 * @since 0.4.10
 */
public class PACEInfo extends SecurityInfo {

	private static final long serialVersionUID = 7960925013249578359L;

	/** Standardized domain parameters. Based on Table 6. */
	public static final int
	PARAM_ID_GFP_1024_160 = 0,
	PARAM_ID_GFP_2048_224 = 1,
	PARAM_ID_GFP_2048_256 = 2,
	/* RFU 3 - 7 */
	PARAM_ID_ECP_NIST_P192_R1 = 8,
	PARAM_ID_ECP_BRAINPOOL_P192_R1 = 9,
	PARAM_ID_ECP_NIST_P224_R1 = 10,
	PARAM_ID_ECP_BRAINPOOL_P224_R1 = 11,
	PARAM_ID_ECP_NST_P256_R1 = 12,
	PARAM_ID_ECP_BRAINPOOL_P256_R1 = 13,
	PARAM_ID_ECP_BRAINPOOL_P320_R1 = 14,
	PARAM_ID_ECP_NIST_P384_R1 = 15,
	PARAM_ID_ECP_BRAINPOOL_P384_R1 = 16,
	PARAM_ID_ECP_BRAINPOOL_P512_R1 = 17,
	PARAM_ID_ECP_NIST_P512_R1 = 18;
	/* RFU 19-31 */

	private String protocolOID;
	private int version;
	private int parameterId;

	/**
	 * Creates a PACEInfo instance.
	 * 
	 * @param oid the OID
	 * @param version should be 2
	 * @param parameterId either a standardized domain parameter id from table 6 or a proprietary domain parameter
	 */
	public PACEInfo(String oid, int version, int parameterId) {
		if (!checkRequiredIdentifier(oid)) { throw new IllegalArgumentException("Invalid OID"); }
		if (version != 2) { throw new IllegalArgumentException("Invalid version, must be 2"); }
		this.protocolOID = oid;
		this.version = version;
		this.parameterId = parameterId;
	}

	@Override
	public String getObjectIdentifier() {
		return protocolOID;
	}
	
	public int getVersion() {
		return version;
	}
	
	public int getParameterId() {
		return parameterId;
	}

	@Override
	ASN1Primitive getDERObject() {
		ASN1EncodableVector vector = new ASN1EncodableVector();
		
		/* Protocol */
		vector.add(new ASN1ObjectIdentifier(protocolOID));

		/* Required data */
		vector.add(new ASN1Integer(version));

		/* Optional data */
		if (parameterId >= 0) {
			vector.add(new ASN1Integer(parameterId));
		}
		return new DLSequence(vector);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("PaceInfo");
		result.append("[");
		result.append("protocol: " + protocolOID);
		result.append(", version: " + version);
		if (parameterId >= 0) {
			result.append(", parameterId: " + parameterId);
		}
		result.append("]");
		return result.toString();
	}

	public int hashCode() {
		return 1234567891
				+ 7 * protocolOID.hashCode()
				+ 5 * version
				+ 3 * parameterId;
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!PACEInfo.class.equals(other.getClass())) { return false; }
		PACEInfo otherPACEInfo = (PACEInfo)other;
		return getDERObject().equals(otherPACEInfo.getDERObject());
	}

	public static boolean checkRequiredIdentifier(String oid) {
		return ID_PACE_DH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_GM_3DES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_GM_3DES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_GM_3DES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_GM_3DES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_GM_3DES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_GM_3DES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_DH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_IM_3DES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_IM_3DES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_IM_3DES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CMAC_256.equals(oid);
	}
}
