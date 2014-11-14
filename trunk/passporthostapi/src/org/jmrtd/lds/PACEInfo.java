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

package org.jmrtd.lds;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;

import javax.crypto.spec.DHParameterSpec;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.crypto.agreement.DHStandardGroups;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.jmrtd.Util;

/**
 * PACE Info object as per SAC TR 1.01, November 11, 2010.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 * 
 * @since 0.5.0
 */
public class PACEInfo extends SecurityInfo {

	private static final long serialVersionUID = 7960925013249578359L;

	/** Generic mapping and Integrated mapping. */
	public enum MappingType { GM, IM };

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
	PARAM_ID_ECP_NIST_P521_R1 = 18;
	/* RFU 19-31 */

	private static final DHParameterSpec
	PARAMS_GFP_1024_160 = Util.toExplicitDHParameterSpec(DHStandardGroups.rfc5114_1024_160),
	PARAMS_GFP_2048_224 = Util.toExplicitDHParameterSpec(DHStandardGroups.rfc5114_2048_224),
	PARAMS_GFP_2048_256 = Util.toExplicitDHParameterSpec(DHStandardGroups.rfc5114_2048_256);

	private static final ECParameterSpec
	PARAMS_ECP_NIST_P192_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("secp192r1")),
	PARAMS_ECP_NIST_P224_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("secp224r1")),
	PARAMS_ECP_NIST_P256_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("secp256r1")),
	PARAMS_ECP_NIST_P384_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("secp384r1")),
	PARAMS_ECP_NIST_P521_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("secp521r1")),
	PARAMS_ECP_BRAINPOOL_P192_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("brainpoolp192r1")),
	PARAMS_ECP_BRAINPOOL_P224_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("brainpoolp224r1")),
	PARAMS_ECP_BRAINPOOL_P256_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("brainpoolp256r1")),
	PARAMS_ECP_BRAINPOOL_P320_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("brainpoolp320r1")),
	PARAMS_ECP_BRAINPOOL_P384_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("brainpoolp384r1")),
	PARAMS_ECP_BRAINPOOL_P512_R1 = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("brainpoolp512r1"));

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

	public static PACEInfo createPACEInfo(byte[] paceInfoBytes) {
		/*
		 * FIXME: Should add a constructor to PACEInfo that takes byte[] or InputStream, or
		 * align this with SecurityInfo.getInstance().
		 */
		ASN1Sequence sequence = ASN1Sequence.getInstance(paceInfoBytes);
		String oid = ((ASN1ObjectIdentifier)sequence.getObjectAt(0)).getId();
		ASN1Primitive requiredData = sequence.getObjectAt(1).toASN1Primitive();
		ASN1Primitive optionalData = null;
		if (sequence.size() == 3) {
			optionalData = sequence.getObjectAt(2).toASN1Primitive();
		}

		int version = ((ASN1Integer)requiredData).getValue().intValue();
		int parameterId = -1;
		if (optionalData != null) {
			parameterId = ((ASN1Integer)optionalData).getValue().intValue();
		}

		return new PACEInfo(oid, version, parameterId);
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
		return toMappingType(oid) != null;
	}

	/*
	 * FIXME: perhaps we should introduce an enum for PACE identifiers (with a String toOID() method),
	 * so that we can get rid of static methods below. -- MO
	 */

	public static MappingType toMappingType(String oid) {
		if (ID_PACE_DH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_256.equals(oid)) {	
			return MappingType.GM;
		} else if (ID_PACE_DH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_256.equals(oid)) {
			return MappingType.IM;
		}
		//		return null;
		throw new NumberFormatException("Unknown OID: \"" + oid + "\"");
	}

	public static String toKeyAgreementAlgorithm(String oid) {
		if (ID_PACE_DH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_DH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_256.equals(oid)) {
			return "DH";
		} else if (ID_PACE_ECDH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_256.equals(oid)) {
			return "ECDH";
		}
		//		return null;
		throw new NumberFormatException("Unknown OID: \"" + oid + "\"");
	}

	public static String toCipherAlgorithm(String oid) {
		if (ID_PACE_DH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CBC.equals(oid)				
				) {
			return "DESede";
		} else if (ID_PACE_DH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_256.equals(oid)) {
			return "AES";
		}
		//			return null;
		throw new NumberFormatException("Unknown OID: \"" + oid + "\"");
	}

	public static String toDigestAlgorithm(String oid) {
		if (ID_PACE_DH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_128.equals(oid)) {
			return "SHA-1";
		} else if (ID_PACE_DH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_256.equals(oid)) {
			return "SHA-256";
		}
		//			return null;
		throw new NumberFormatException("Unknown OID: \"" + oid + "\"");
	}

	public static int toKeyLength(String oid) {
		if (ID_PACE_DH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_GM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_ECDH_IM_3DES_CBC_CBC.equals(oid)
				|| ID_PACE_DH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_128.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_128.equals(oid)
				) {
			return 128;
		} else if (ID_PACE_DH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_192.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_192.equals(oid)) {
			return 192;
		} else if (ID_PACE_DH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_DH_IM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_GM_AES_CBC_CMAC_256.equals(oid)
				|| ID_PACE_ECDH_IM_AES_CBC_CMAC_256.equals(oid)) {
			return 256;
		} else {
			// return -1;
			throw new NumberFormatException("Unknown OID: \"" + oid + "\"");
		}
	}
	
	public static AlgorithmParameterSpec toParameterSpec(int stdDomainParam) {
		switch (stdDomainParam) {
		case PARAM_ID_GFP_1024_160: return PARAMS_GFP_1024_160;
		case PARAM_ID_GFP_2048_224: return PARAMS_GFP_2048_224;
		case PARAM_ID_GFP_2048_256: return PARAMS_GFP_2048_256;
		case PARAM_ID_ECP_NIST_P192_R1: return PARAMS_ECP_NIST_P192_R1;
		case PARAM_ID_ECP_BRAINPOOL_P192_R1:return PARAMS_ECP_BRAINPOOL_P192_R1;
		case PARAM_ID_ECP_NIST_P224_R1: return PARAMS_ECP_NIST_P224_R1;
		case PARAM_ID_ECP_BRAINPOOL_P224_R1: return PARAMS_ECP_BRAINPOOL_P224_R1;
		case PARAM_ID_ECP_NST_P256_R1: return PARAMS_ECP_NIST_P256_R1;
		case PARAM_ID_ECP_BRAINPOOL_P256_R1: return PARAMS_ECP_BRAINPOOL_P256_R1;
		case PARAM_ID_ECP_BRAINPOOL_P320_R1: return PARAMS_ECP_BRAINPOOL_P320_R1;
		case PARAM_ID_ECP_NIST_P384_R1: return PARAMS_ECP_NIST_P384_R1;
		case PARAM_ID_ECP_BRAINPOOL_P384_R1: return PARAMS_ECP_BRAINPOOL_P384_R1;
		case PARAM_ID_ECP_BRAINPOOL_P512_R1: return PARAMS_ECP_BRAINPOOL_P512_R1;
		case PARAM_ID_ECP_NIST_P521_R1: return PARAMS_ECP_NIST_P521_R1;
		default: throw new NumberFormatException("Unknown standardized domain parameters");
		}
	}
}
