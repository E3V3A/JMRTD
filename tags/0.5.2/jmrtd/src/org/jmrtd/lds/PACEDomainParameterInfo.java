/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
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

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * PACE Domain Parameter Info object as per SAC TR 1.01, November 11, 2010.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 * 
 * @since 0.5.0
 */
public class PACEDomainParameterInfo extends SecurityInfo {

	private static final long serialVersionUID = -5851251908152594728L;

	/**
	 * Value for parameter algorithm OID (part of parameters AlgorithmIdentifier).
	 * <code>dhpublicnumber OBJECT IDENTIFIER ::= { iso(1) member-body(2) us(840) ansi-x942(10046) number-type(2) 1 }</code>.
	 */
	private static final String ID_DH_PUBLIC_NUMBER = "1.2.840.10046.2.1";

	/**
	 * Value for parameter algorithm OID (part of parameters AlgorithmIdentifier).
	 * <code>ecPublicKey OBJECT IDENTIFIER ::= { iso(1) member-body(2) us(840) ansi-x962(10045) keyType(2) 1 }</code>.
	 */
	private static final String ID_EC_PUBLIC_KEY = "1.2.840.10045.2.1";

	private String protocolOID;
	private AlgorithmIdentifier domainParameter;
	private int parameterId;

	/**
	 * 
	 * @param protocolOID Must be @see SecurityInfo.#ID_PACE_DH_GM, @see SecurityInfo.#ID_PACE_ECDH_GM, @see SecurityInfo.#ID_PACE_DH_IM, @see SecurityInfo.#ID_PACE_ECDH_IM
	 * @param parameters Parameters 
	 */
	public PACEDomainParameterInfo(String protocolOID, ASN1Encodable parameters) {
		this(protocolOID, parameters, -1);
	}

	public PACEDomainParameterInfo(String protocolOID, ASN1Encodable parameters, int parameterId) {
		this(protocolOID, toAlgorithmIdentifier(protocolOID, parameters), parameterId);
	}

	private PACEDomainParameterInfo(String protocolOID, AlgorithmIdentifier domainParameter, int parameterId) {
		if (!checkRequiredIdentifier(protocolOID)) { throw new IllegalArgumentException("Invalid protocol id: " + protocolOID); }
		this.protocolOID = protocolOID;
		this.domainParameter = domainParameter;
		this.parameterId = parameterId;
	}

	@Override
	public String getObjectIdentifier() {
		return protocolOID;
	}

	/**
	 * Gets the parameter id, or -1 if this is the only domain parameter info.
	 * 
	 * @return the parameter id or -1
	 */
	public int getParameterId() {
		return parameterId;
	}

	public ASN1Encodable getParameters() {
		return domainParameter.getParameters();
	}

	@Override
	ASN1Primitive getDERObject() {
		ASN1EncodableVector vector = new ASN1EncodableVector();
		
		/* Protocol */
		vector.add(new ASN1ObjectIdentifier(protocolOID));

		/* Required data */
		vector.add(domainParameter);

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
		result.append(", domainParameter: " + domainParameter.toString());
		if (parameterId >= 0) {
			result.append(", parameterId: " + parameterId);
		}
		result.append("]");
		return result.toString();
	}

	public int hashCode() {
		return 111111111
				+ 7 * protocolOID.hashCode()
				+ 5 * domainParameter.hashCode()
				+ 3 * parameterId;
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!PACEDomainParameterInfo.class.equals(other.getClass())) { return false; }
		PACEDomainParameterInfo otherPACEDomainParameterInfo = (PACEDomainParameterInfo)other;
		return getDERObject().equals(otherPACEDomainParameterInfo.getDERObject());
	}

	public static boolean checkRequiredIdentifier(String oid) {
		return ID_PACE_DH_GM.equals(oid) || ID_PACE_ECDH_GM.equals(oid) || ID_PACE_DH_IM.equals(oid) || ID_PACE_ECDH_IM.equals(oid);
	}

	/* ONLY PRIVATE METHODS BELOW */

	private static AlgorithmIdentifier toAlgorithmIdentifier(String protocolOID, ASN1Encodable parameters) {
		if (ID_PACE_DH_GM.equals(protocolOID) || ID_PACE_DH_IM.equals(protocolOID)) {
			return new AlgorithmIdentifier(new ASN1ObjectIdentifier(ID_DH_PUBLIC_NUMBER), parameters);
		} else if (ID_PACE_ECDH_GM.equals(protocolOID) || ID_PACE_ECDH_IM.equals(protocolOID)) {
			return new AlgorithmIdentifier(new ASN1ObjectIdentifier(ID_EC_PUBLIC_KEY), parameters);
		}
		throw new IllegalArgumentException("Cannot infer algorithm OID from protocol OID: " + protocolOID);
	}
}
