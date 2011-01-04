/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2011  The JMRTD team
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
 * $Id:  $
 */

package org.jmrtd;

import java.security.Provider;

/**
 * Security provider for JMRTD specific implementations.
 * 
 * @author JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class JMRTDSecurityProvider extends Provider
{
	private static final long serialVersionUID = -2881416441551680704L;

	public JMRTDSecurityProvider() {
		super("JMRTD", 0.1, "JMRTD Security Provider");
        put("CertificateFactory.CVC", "org.jmrtd.cert.CVCertificateFactorySpi");
        put("CertStore.PKD", "org.jmrtd.cert.PKDCertStoreSpi");
        put("CertStore.JKS", "org.jmrtd.cert.KeyStoreCertStoreSpi");
        put("CertStore.PKCS12", "org.jmrtd.cert.KeyStoreCertStoreSpi");
	}
}
