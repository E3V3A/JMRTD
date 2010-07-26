package org.jmrtd.cvc;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Date;

import net.sourceforge.scuba.data.ISOCountry;

import org.ejbca.cvc.CVCAuthorizationTemplate;
import org.ejbca.cvc.ReferenceField;

/**
 * CVCertificates as specified in TR 03110.
 * Just a wrapper around <code>org.ejbca.cvc.CVCertificate</code> so that we can subclass
 * <code>java.security.cert.Certificate</code>.
 */
public class CVCertificate extends Certificate
{
	private org.ejbca.cvc.CVCertificate cvCertificate;

	protected CVCertificate(org.ejbca.cvc.CVCertificate cvCertificate) {
		super("CV");
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
			return new CVCPublicKey(cvCertificate.getCertificateBody().getPublicKey());
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

	// TODO: this is a nasty hack, but unfortunately the cv-cert lib does not
	// provide a proper API
	// call for this
	//
	// Woj, this used to live in PassportService. Moved it here... -- MO
	public byte[] getCertSignatureData() throws IOException,
	CertificateException {
		try {
			byte[] data = cvCertificate.getDEREncoded();
			byte[] body = cvCertificate.getCertificateBody().getDEREncoded();
			int index = 0;
			byte b1 = body[0];
			byte b2 = body[1];
			while (index < data.length) {
				if (data[index] == b1 && data[index + 1] == b2) {
					break;
				}
				index++;
			}
			index += body.length;
			if (index < data.length) {
				byte[] result = new byte[data.length - index];
				System.arraycopy(data, index, result, 0, result.length);
				// Sanity check:
				assert result[0] == (byte) 0x5F && result[1] == (byte) 0x37;
				return result;
			}
			return null;
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
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

	public CVCAuthorizationTemplate getAuthorizationTemplate() throws NoSuchFieldException {
		return (CVCAuthorizationTemplate)cvCertificate.getCertificateBody().getAuthorizationTemplate();
	}

//	public org.ejbca.cvc.CVCertificateBody getCertificateBody() throws NoSuchFieldException {
//		return cvCertificate.getCertificateBody();
//	}

	public byte[] getSignature() throws NoSuchFieldException {
		return cvCertificate.getSignature();
	}
}