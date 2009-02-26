package sos.mrtd;

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class ChipAuthenticationPublicKeyInfo extends SecurityInfo {

    public ChipAuthenticationPublicKeyInfo(SecurityInfo si) {
        super(si);
    }
    
    public ChipAuthenticationPublicKeyInfo(InputStream in) throws IOException {
        super(in);
    }
    
    public ChipAuthenticationPublicKeyInfo(DERObjectIdentifier identifier, SubjectPublicKeyInfo publicKeyInfo, Integer keyId) {
        super(identifier, publicKeyInfo.getDERObject(), keyId != null ? new DERInteger(keyId) : null);
    }

    public ChipAuthenticationPublicKeyInfo(DERObjectIdentifier identifier, SubjectPublicKeyInfo publicKeyInfo) {
        this(identifier, publicKeyInfo, null);
    }
    
    public Integer getKeyId() {
        if(optionalData == null) {
            return null;
        }
        return ((DERInteger)optionalData).getValue().intValue();
    }
    
    public SubjectPublicKeyInfo getSubjectPublicKeyInfo() {
        return new SubjectPublicKeyInfo((DERSequence)requiredData);
    }
    
    protected void checkFields() {
        try {
            if(!checkRequiredIdentifier(identifier)) {
                throw new IllegalArgumentException("Wrong identifier: "+identifier.getId());
            }
            getSubjectPublicKeyInfo();
            if(optionalData != null && !(optionalData instanceof DERInteger)) {
                throw new IllegalArgumentException("Key ID not an integer: "+optionalData.getClass().getName());
            }
        }catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Malformed ChipAuthenticationInfo.");
        }
     }
    
     public static boolean checkRequiredIdentifier(DERObjectIdentifier id) {
         return id.equals(EACObjectIdentifiers.id_PK_DH) || id.equals(EACObjectIdentifiers.id_PK_ECDH);         
     }

}
