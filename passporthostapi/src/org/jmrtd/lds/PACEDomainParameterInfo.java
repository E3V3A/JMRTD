package org.jmrtd.lds;

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
 * @since 0.4.10
 */
public class PACEDomainParameterInfo extends SecurityInfo {

	private static final long serialVersionUID = -5851251908152594728L;

	/** Value for algorithm OID. */
	public static final String
	ID_DH_PUBLIC_NUMBER = "1.2.840.10046.2.1",
	ID_EC_PUBLIC_KEY = "1.2.840.10045.2.1";

	private String protocolOID;
	private AlgorithmIdentifier domainParameter;
	private int parameterId;

	public PACEDomainParameterInfo(String protocolOID, AlgorithmIdentifier domainParameter) {
		this(protocolOID, domainParameter, -1);
	}
	
	public PACEDomainParameterInfo(String protocolOID, AlgorithmIdentifier domainParameter, int parameterId) {
		if (!checkRequiredIdentifier(protocolOID)) { throw new IllegalArgumentException("Invalid protocol id"); }
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
	
	public AlgorithmIdentifier getDomainParameter() {
		return domainParameter;
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
}
