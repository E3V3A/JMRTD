package org.jmrtd.cvc;

import java.security.PublicKey;

import org.ejbca.cvc.AlgorithmUtil;
import org.ejbca.cvc.OIDField;

public class CVCPublicKey implements PublicKey {

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
			String superAlg = publicKey.getAlgorithm();
			if (superAlg != null) {
				return superAlg;
			}
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
