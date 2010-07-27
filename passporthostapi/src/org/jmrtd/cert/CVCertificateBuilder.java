package org.jmrtd.cert;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Date;

import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.HolderReferenceField;
import org.ejbca.cvc.exception.ConstructionException;
import org.jmrtd.cert.CVCAuthorizationTemplate.Permission;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;

public class CVCertificateBuilder
{
	public static CVCertificate createCertificate(PublicKey publicKey,
			PrivateKey signerKey, String algorithmName, CVCPrincipal caRef,
			CVCPrincipal holderRef, CVCAuthorizationTemplate authZTemplate, Date validFrom, Date validTo,
			String provider) throws IOException, NoSuchAlgorithmException,
			NoSuchProviderException, InvalidKeyException, SignatureException,
			ConstructionException {
		return new CVCertificate(org.ejbca.cvc.CertificateGenerator
				.createCertificate(publicKey, signerKey, algorithmName,
						new CAReferenceField(caRef.getCountry().toAlpha2Code(),
								caRef.getMnemonic(), caRef.getSeqNumber()),
						new HolderReferenceField(holderRef.getCountry()
								.toAlpha2Code(), holderRef.getMnemonic(),
								holderRef.getSeqNumber()), getRole(authZTemplate.getRole()), getAccessRight(authZTemplate.getAccessRight()),
						validFrom, validTo, provider));
	}
	
	private static org.ejbca.cvc.AuthorizationRoleEnum getRole(Role role) {
		switch (role) {
		case CVCA: return org.ejbca.cvc.AuthorizationRoleEnum.CVCA;
		case DV_D: return org.ejbca.cvc.AuthorizationRoleEnum.DV_D;
		case DV_F: return org.ejbca.cvc.AuthorizationRoleEnum.DV_F;
		case IS: return org.ejbca.cvc.AuthorizationRoleEnum.IS;
		}
		throw new NumberFormatException("Cannot decode role " + role);
	}
	
	private static org.ejbca.cvc.AccessRightEnum getAccessRight(Permission accessRight) {
		switch (accessRight) {
		case READ_ACCESS_NONE: return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_NONE;
		case READ_ACCESS_DG3: return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG3;
		case READ_ACCESS_DG4: return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG4;
		case READ_ACCESS_DG3_AND_DG4: return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG3_AND_DG4;
		}
		throw new NumberFormatException("Cannot decode access right " + accessRight);
	}
}
