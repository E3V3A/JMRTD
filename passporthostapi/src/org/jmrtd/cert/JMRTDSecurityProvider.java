package org.jmrtd.cert;

import java.security.Provider;

public class JMRTDSecurityProvider extends Provider
{
	private static final long serialVersionUID = -2881416441551680704L;

	public JMRTDSecurityProvider() {
		super("JMRTDCVC", 0.1, "JMRTD CVC Provider");
        put("CertificateFactory.CVC", "org.jmrtd.cvc.CVCertificateFactorySpi");
	}
}
