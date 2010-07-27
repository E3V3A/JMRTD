package org.jmrtd.cert;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactorySpi;
import java.util.Collection;

import net.sourceforge.scuba.tlv.BERTLVInputStream;
import net.sourceforge.scuba.tlv.BERTLVObject;

import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.cvc.exception.ConstructionException;
import org.ejbca.cvc.exception.ParseException;

public class CVCertificateFactorySpi extends CertificateFactorySpi
{
	private static final int CV_CERTIFICATE_TAG = 0x7F21;

	public Certificate engineGenerateCertificate(InputStream in) throws CertificateException
	{
		try {
			/* Read certificate as byte[] */
			BERTLVInputStream tlvIn = new BERTLVInputStream(in);
			int tag = tlvIn.readTag();
			if (tag != CV_CERTIFICATE_TAG) { throw new CertificateException("Expected CV_CERTIFICATE_TAG, found " + Integer.toHexString(tag)); }
			/* int length = */ tlvIn.readLength();
			byte[] value = tlvIn.readValue();
			byte[] data = (new BERTLVObject(tag, value)).getEncoded();

			CVCObject parsedObject = CertificateParser.parseCertificate(data);
			return new CVCertificate((org.ejbca.cvc.CVCertificate)parsedObject);
		} catch (IOException ioe) {
			throw new CertificateException(ioe.getMessage());
		} catch (ConstructionException ce) {
			throw new CertificateException(ce.getMessage());
		} catch (ParseException pe) {
			throw new CertificateException(pe.getMessage());
		}
	}

	public CRL engineGenerateCRL(InputStream in) throws CRLException {
		return null;
	}

	public Collection<? extends CRL> engineGenerateCRLs(InputStream in) throws CRLException {
		return null;
	}

	public Collection<? extends Certificate> engineGenerateCertificates(InputStream in) throws CertificateException {
		return null;
	}
}
