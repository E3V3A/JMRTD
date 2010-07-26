package org.jmrtd.cvc;

import java.security.PublicKey;

import org.ejbca.cvc.AlgorithmUtil;

public class CVCPublicKey implements PublicKey {

	private org.ejbca.cvc.CVCPublicKey publicKey;
	
	protected CVCPublicKey(org.ejbca.cvc.CVCPublicKey publicKey) {
		this.publicKey = publicKey;
	}
	
	public String getAlgorithm() {
		try {
			return AlgorithmUtil.getAlgorithmName(publicKey.getObjectIdentifier());
		} catch (NoSuchFieldException nsfe) {
			return publicKey.getAlgorithm();
		}
	}

	public byte[] getEncoded() {
		return publicKey.getEncoded();
	}

	public String getFormat() {
		return publicKey.getFormat();
	}
}
