package sos.mrtd;

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;

public class ChipAuthenticationInfo extends SecurityInfo {

    public static final int VERSION_NUM = 1;
    
    public ChipAuthenticationInfo(SecurityInfo si) {
        super(si);
    }
    
    public ChipAuthenticationInfo(InputStream in) throws IOException {
        super(in);
    }
    
    public ChipAuthenticationInfo(DERObjectIdentifier identifier, Integer version, Integer keyId) {
        super(identifier, new DERInteger(version), keyId != null ? new DERInteger(keyId) : null);
    }

    public ChipAuthenticationInfo(DERObjectIdentifier identifier, Integer version) {
        this(identifier, version, null);
    }
    
    public Integer getKeyId() {
        if(optionalData == null) {
            return null;
        }
        return ((DERInteger)optionalData).getValue().intValue();
    }

    protected void checkFields() {
       try {
           if(!checkRequiredIdentifier(identifier)) {
               throw new IllegalArgumentException("Wrong identifier: "+identifier.getId());
           }
           if(!(requiredData instanceof DERInteger) || ((DERInteger)requiredData).getValue().intValue() != VERSION_NUM) {
               throw new IllegalArgumentException("Wrong version");                
           }
           if(optionalData != null && !(optionalData instanceof DERInteger)) {
               throw new IllegalArgumentException("Key ID not an integer: "+optionalData.getClass().getName());
           }
       }catch(Exception e) {
           e.printStackTrace();
           throw new IllegalArgumentException("Malformed ChipAuthenticationInfo.");
       }
    }
    
    public static boolean checkRequiredIdentifier(DERObjectIdentifier id) {
        return id.equals(EACObjectIdentifiers.OID_ID_CA_DH_3DES_CBC_CBC) || id.equals(EACObjectIdentifiers.OID_ID_CA_ECDH_3DES_CBC_CBC);
    }
    
}
