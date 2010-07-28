package org.jmrtd;

import java.security.Provider;

public class JMRTDSecurityProvider extends Provider
{
	private static final long serialVersionUID = -2881416441551680704L;

	public JMRTDSecurityProvider() {
		super("JMRTD", 0.1, "JMRTD Security Provider");
        put("CertificateFactory.CVC", "org.jmrtd.cert.CVCertificateFactorySpi");
        put("CertStore.PKD", "org.jmrtd.cert.PKDCertStoreSpi");
	}
}
