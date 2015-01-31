package org.jmrtd.lds;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Encapsulates the terminal key and associated certificate chain for terminal authentication.
 */
public class EACCredentials {
	private PrivateKey privateKey;
	private Certificate[] chain;

	/**
	 * Creates EAC credentials.
	 * 
	 * @param privateKey
	 * @param chain
	 */
	public EACCredentials(PrivateKey privateKey, Certificate[] chain) {
		this.privateKey = privateKey;
		this.chain = chain;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public Certificate[] getChain() {
		return chain;
	}		
}
