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

package org.jmrtd.cert;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

import net.sourceforge.scuba.data.ISOCountry;

import org.ejbca.cvc.ReferenceField;

/**
 * Card verifiable certificates as specified in TR 03110.
 * 
 * Just a wrapper around <code>org.ejbca.cvc.CVCertificate</code> by Keijo Kurkinen of EJBCA.org,
 * so that we can subclass <code>java.security.cert.Certificate</code>.
 * 
 * We also hide some of the internal structure (no more calls to get the "body" just to get some
 * attributes).
 */
public class CardVerifiableCertificate extends Certificate
{
	private static final long serialVersionUID = -3585440601605666288L;

	private org.ejbca.cvc.CVCertificate cvCertificate;

	private transient KeyFactory rsaKeyFactory;

	protected CardVerifiableCertificate(org.ejbca.cvc.CVCertificate cvCertificate) {
		super("CVC");
		try {
			rsaKeyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException nsae) {
			/* NOTE: never happens, RSA will be provided. */
			nsae.printStackTrace();
		}
		this.cvCertificate = cvCertificate;
	}

	public byte[] getEncoded() throws CertificateEncodingException {
		try {
			return cvCertificate.getDEREncoded();
		} catch (IOException ioe) {
			throw new CertificateEncodingException(ioe.getMessage());
		}
	}

	public PublicKey getPublicKey() {
		try {
			org.ejbca.cvc.CVCPublicKey publicKey = cvCertificate.getCertificateBody().getPublicKey();
			if (publicKey.getAlgorithm().equals("RSA")) { // TODO: something similar for EC / ECDSA?
				RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
				try {
					return rsaKeyFactory.generatePublic(new RSAPublicKeySpec(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent()));
				} catch (GeneralSecurityException gse) {
					gse.printStackTrace();
					return new CVCPublicKey(publicKey);
				}
			}
			return new CVCPublicKey(publicKey);
		} catch (NoSuchFieldException nsfe) {
			nsfe.printStackTrace();
			return null;
		}
	}

	public String toString() {
		return cvCertificate.toString();
	}

	public void verify(PublicKey key) throws CertificateException,
	NoSuchAlgorithmException, InvalidKeyException,
	NoSuchProviderException, SignatureException {
		Provider[] providers = Security.getProviders();
		boolean foundProvider = false;
		for (Provider provider: providers) {
			try {
				cvCertificate.verify(key, provider.getName());
				foundProvider = true;
				break;
			} catch (NoSuchAlgorithmException nse) {
				continue;
			}
		}
		if (!foundProvider) {
			throw new NoSuchAlgorithmException("Tried all security providers: None was able to provide this signature algorithm.");
		}
	}

	public void verify(PublicKey key, String provider)
	throws CertificateException, NoSuchAlgorithmException,
	InvalidKeyException, NoSuchProviderException, SignatureException {
		cvCertificate.verify(key, provider);
	}

	public byte[] getCertBodyData() throws CertificateException, IOException {
		try {
			return cvCertificate.getCertificateBody().getDEREncoded();
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Returns 'Effective Date' 
	 * @returns
	 */
	public Date getNotBefore() throws CertificateException {
		try {
			return cvCertificate.getCertificateBody().getValidFrom();
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Returns 'Expiration Date' 
	 * @return
	 */
	public Date getNotAfter() throws CertificateException {
		try {
			return cvCertificate.getCertificateBody().getValidTo();
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	public CVCPrincipal getAuthorityReference() throws CertificateException {
		try  {
			ReferenceField rf = cvCertificate.getCertificateBody().getAuthorityReference();
			return new CVCPrincipal(ISOCountry.getInstance(rf.getCountry().toUpperCase()), rf.getMnemonic(), rf.getSequence());
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	public CVCPrincipal getHolderReference() throws CertificateException {
		try  {
			ReferenceField rf = cvCertificate.getCertificateBody().getHolderReference();
			return new CVCPrincipal(ISOCountry.getInstance(rf.getCountry().toUpperCase()), rf.getMnemonic(), rf.getSequence());
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Gets the holder authorization template.
	 * 
	 * @return the holder authorization template
	 * @throws CertificateException
	 */
	public CVCAuthorizationTemplate getAuthorizationTemplate() throws CertificateException {
		try {
			org.ejbca.cvc.CVCAuthorizationTemplate template = cvCertificate.getCertificateBody().getAuthorizationTemplate();
			return new CVCAuthorizationTemplate(template);
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Returns the signature (just the value, without the <code>0x5F37</code> tag).
	 * @return the signature bytes
	 * 
	 * @throws CertificateException if certificate doesn't contain a signature
	 */
	public byte[] getSignature() throws CertificateException {
		try {
			return cvCertificate.getSignature();
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	public boolean equals(Object otherObj) {
		if (otherObj == null) { return false; }
		if (this == otherObj) { return true; }
		if (!this.getClass().equals(otherObj.getClass())) { return false; }
		return this.cvCertificate.equals(((CardVerifiableCertificate)otherObj).cvCertificate);
	}

	public int hashCode() {
		return cvCertificate.hashCode() * 2 - 1030507011;
	}
}