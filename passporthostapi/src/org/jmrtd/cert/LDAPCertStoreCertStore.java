/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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
 * $Id: $
 */

package org.jmrtd.cert;

import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.LDAPCertStoreParameters;
import java.util.Collection;


public class LDAPCertStoreCertStore extends TrustStore
{
	private URI location;

	private CertStore cs;

	public LDAPCertStoreCertStore(URI server) {
		try {

			// FIXME: check scheme is ldap...
			CertStoreParameters params = new LDAPCertStoreParameters(server.getHost());
			cs = CertStore.getInstance("LDAP", params);
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Collection<? extends Certificate> getCertificates(CertSelector selector) {
		System.out.println("DEBUG: selector = " + selector);
		try {
			return cs.getCertificates(selector);
		} catch (CertStoreException cse) {
			cse.printStackTrace();
			return null;
		}
	}

	public Collection<Key> getKeys() {
		return null;
	}

	public URI getLocation() {
		return location;
	}
}
