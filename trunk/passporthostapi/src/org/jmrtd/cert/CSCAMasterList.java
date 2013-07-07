package org.jmrtd.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertSelector;
import java.security.cert.Certificate;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.jce.provider.X509CertificateObject;

public class CSCAMasterList {

	/** Use this to get all certificates, including link certificates. */
	private static final CertSelector IDENTITY_SELECTOR = new X509CertSelector() {
		public boolean match(Certificate cert) {
			if (!(cert instanceof X509Certificate)) { return false; }
			return true;
		}

		public Object clone() { return this; }	
	};

	/** Use this to get self-signed certificates only. (Excludes link certificates.) */
	private static final CertSelector SELF_SIGNED_SELECTOR = new X509CertSelector() {
		public boolean match(Certificate cert) {
			if (!(cert instanceof X509Certificate)) { return false; }
			X509Certificate x509Cert = (X509Certificate)cert;
			X500Principal issuer = x509Cert.getIssuerX500Principal();
			X500Principal subject = x509Cert.getSubjectX500Principal();
			return (issuer == null && subject == null) || subject.equals(issuer);
		}

		public Object clone() { return this; }
	};
	
	private List<Certificate> certificates;

	/** Private constructor, only used locally. */
	private CSCAMasterList() {
		this.certificates = new ArrayList<Certificate>(256);	
	}
	
	/**
	 * Constructs a master lsit from a collection of certificates.
	 * 
	 * @param certificates a collection of certificates
	 */
	public CSCAMasterList(Collection<Certificate> certificates) {
		this();
		this.certificates.addAll(certificates);
	}
	
	public CSCAMasterList(byte[] binary, CertSelector selector) {
		this();
		this.certificates.addAll(searchCertificates(binary, selector));
	}
	
	public CSCAMasterList(byte[] binary) {
		this(binary, IDENTITY_SELECTOR);
	}
	
	public List<Certificate> getCertificates() {
		return certificates;
	}
	
	/* PRIVATE METHODS BELOW */
	
	private static List<Certificate> searchCertificates(byte[] binary, CertSelector selector) {
		List<Certificate> result = new ArrayList<Certificate>();

		try {
			ASN1Sequence sequence = (ASN1Sequence)ASN1Sequence.getInstance(binary);
			List<SignedData> signedDataList = getSignedDataFromDERObject(sequence, null);
			for (SignedData signedData: signedDataList) {

				//			ASN1Set certificatesASN1Set = signedData.getCertificates();
				//			Enumeration certificatesEnum = certificatesASN1Set.getObjects();
				//			while (certificatesEnum.hasMoreElements()) {
				//				Object certificateObject = certificatesEnum.nextElement();
				//				// TODO: interpret certificateObject, and check signature
				//			}

				ContentInfo contentInfo = signedData.getContentInfo();
				Object content = contentInfo.getContent();
				Collection<Certificate> certificates = getCertificatesFromDERObject(content, null);
				for (Certificate certificate: certificates) {
					if (selector.match(certificate)) {
						result.add(certificate);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private static List<SignedData> getSignedDataFromDERObject(Object o, List<SignedData> result) {
		if (result == null) { result = new ArrayList<SignedData>(); }

		try {
			SignedData signedData = SignedData.getInstance(o);
			if (signedData != null) {
				result.add(signedData);
			}
			return result;
		} catch (Exception e) {
		}

		if (o instanceof DERTaggedObject) {
			ASN1Primitive childObject = ((DERTaggedObject)o).getObject();
			return getSignedDataFromDERObject(childObject, result);
		} else if (o instanceof ASN1Sequence) {
			Enumeration<?> derObjects = ((ASN1Sequence)o).getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				result = getSignedDataFromDERObject(nextObject, result);
			}
			return result;
		} else if (o instanceof ASN1Set) {
			Enumeration<?> derObjects = ((ASN1Set)o).getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				result = getSignedDataFromDERObject(nextObject, result);
			}
			return result;
		} else if (o instanceof DEROctetString) {
			DEROctetString derOctetString = (DEROctetString)o;
			byte[] octets = derOctetString.getOctets();
			ASN1InputStream derInputStream = new ASN1InputStream(new ByteArrayInputStream(octets));
			try {
				while (true) {
					ASN1Primitive derObject = derInputStream.readObject();
					if (derObject == null) { break; }
					result = getSignedDataFromDERObject(derObject, result);
				}
				derInputStream.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return result;
		}
		return result;
	}

	private static Collection<Certificate> getCertificatesFromDERObject(Object o, Collection<Certificate> certificates) {
		if (certificates == null) { certificates = new ArrayList<Certificate>(); }

		try {
			org.bouncycastle.asn1.x509.Certificate certAsASN1Object = org.bouncycastle.asn1.x509.Certificate.getInstance(o);
//			certificates.add(new X509CertificateObject(certAsASN1Object)); // NOTE: BC 1.48
			certificates.add(new X509CertificateObject(X509CertificateStructure.getInstance(certAsASN1Object))); // NOTE: BC 1.47
			return certificates;
		} catch (Exception e) {
		}

		if (o instanceof DERTaggedObject) {
			ASN1Primitive childObject = ((DERTaggedObject)o).getObject();
			return getCertificatesFromDERObject(childObject, certificates);
		} else if (o instanceof ASN1Sequence) {
			Enumeration<?> derObjects = ((ASN1Sequence)o).getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				certificates = getCertificatesFromDERObject(nextObject, certificates);
			}
			return certificates;
		} else if (o instanceof ASN1Set) {
			Enumeration<?> derObjects = ((ASN1Set)o).getObjects();
			while (derObjects.hasMoreElements()) {
				Object nextObject = derObjects.nextElement();
				certificates = getCertificatesFromDERObject(nextObject, certificates);
			}
			return certificates;
		} else if (o instanceof DEROctetString) {
			DEROctetString derOctetString = (DEROctetString)o;
			byte[] octets = derOctetString.getOctets();
			ASN1InputStream derInputStream = new ASN1InputStream(new ByteArrayInputStream(octets));
			try {
				while (true) {
					ASN1Primitive derObject = derInputStream.readObject();
					if (derObject == null) { break; }
					certificates = getCertificatesFromDERObject(derObject, certificates);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return certificates;
		} else if (o instanceof SignedData) {
			SignedData signedData = (SignedData)o;
			//			ASN1Set certificatesASN1Set = signedData.getCertificates();
			//			Enumeration certificatesEnum = certificatesASN1Set.getObjects();
			//			while (certificatesEnum.hasMoreElements()) {
			//				Object certificateObject = certificatesEnum.nextElement();
			//				// TODO: interpret certificateObject, and check signature
			//			}

			ContentInfo contentInfo = signedData.getContentInfo();
			Object content = contentInfo.getContent();
			return getCertificatesFromDERObject(content, certificates);
		}
		return certificates;
	}
}
