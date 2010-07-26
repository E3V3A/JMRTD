package org.jmrtd.cvc;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Date;

import org.ejbca.cvc.AccessRightEnum;
import org.ejbca.cvc.AuthorizationRoleEnum;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.HolderReferenceField;
import org.ejbca.cvc.exception.ConstructionException;

public class CVCertificateGenerator
{
	private org.ejbca.cvc.CertificateGenerator generator;

	protected CVCertificateGenerator(org.ejbca.cvc.CertificateGenerator generator) {
		this.generator = generator;
	}

	public static CVCertificate createCertificate(
			PublicKey              publicKey,
			PrivateKey             signerKey,
			String                 algorithmName, 
			CVCPrincipal       caRef, 
			CVCPrincipal   holderRef, 
			AuthorizationRoleEnum  authRole,
			AccessRightEnum        rights,
			Date                   validFrom,
			Date                   validTo,
			String                 provider) 
	throws IOException, NoSuchAlgorithmException,
	NoSuchProviderException, InvalidKeyException, SignatureException, ConstructionException {
		return new CVCertificate(
				org.ejbca.cvc.CertificateGenerator.createCertificate(
						publicKey,
						signerKey,
						algorithmName,
						new CAReferenceField(caRef.getCountry().toAlpha2Code(), caRef.getMnemonic(), caRef.getSeqNumber()),
						new HolderReferenceField(holderRef.getCountry().toAlpha2Code(), holderRef.getMnemonic(), holderRef.getSeqNumber()),
						authRole,
						rights,
						validFrom,
						validTo,
						provider));
	}
}
