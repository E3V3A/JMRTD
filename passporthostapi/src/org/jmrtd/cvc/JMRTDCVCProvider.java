package org.jmrtd.cvc;

import java.security.Provider;

public class JMRTDCVCProvider extends Provider
{
	private static final long serialVersionUID = -2881416441551680704L;

	public JMRTDCVCProvider() {
		super("JMRTDCVC", 0.1, "JMRTD CVC Provider");
        put("CertificateFactory.CVC", "org.jmrtd.cvc.CVCertificateFactorySpi");
	}
}
