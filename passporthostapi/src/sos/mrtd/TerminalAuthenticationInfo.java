package sos.mrtd;

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;

import sos.util.Hex;

public class TerminalAuthenticationInfo extends SecurityInfo {

    public static final int VERSION_NUM = 1;
    
    public TerminalAuthenticationInfo(InputStream in) throws IOException {
        super(in);
    }
    
    public TerminalAuthenticationInfo(DERObjectIdentifier identifier, Integer version, DERSequence efCVCA) {
        super(identifier, new DERInteger(version), efCVCA);
    }

    public TerminalAuthenticationInfo(DERObjectIdentifier identifier, Integer version) {
        this(identifier, version, null);
    }
    
    protected void checkFields() {
        try{
            if(!checkRequiredIdentifier(identifier)) {
                throw new IllegalArgumentException("Wrong identifier: "+identifier.getId());
            }
            if(!(requiredData instanceof DERInteger) || ((DERInteger)requiredData).getValue().intValue() != VERSION_NUM) {
                throw new IllegalArgumentException("Wrong version");                
            }
            if(optionalData != null) {
               DERSequence s = (DERSequence)optionalData;
               DEROctetString fid = (DEROctetString)s.getObjectAt(0);
               if(fid.getOctets().length != 2) {
                   throw new IllegalArgumentException("Malformed FID.");
               }
               if(s.size() == 2) {
                   DEROctetString sfi = (DEROctetString)s.getObjectAt(1);
                   if(sfi.getOctets().length != 1) {
                       throw new IllegalArgumentException("Malformed SFI.");
                   }
               }
            }
        }catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Malformed TerminalAuthenticationInfo.");
        }
    }
    
    public static boolean checkRequiredIdentifier(DERObjectIdentifier id) {
        return id.equals(EACObjectIdentifiers.id_TA);
    }
    
    public int getFileID() {
        if(optionalData == null) {
            return -1;
        }
        DERSequence s = (DERSequence)optionalData;
        DEROctetString fid = (DEROctetString)s.getObjectAt(0);
        byte[] fidBytes = fid.getOctets();
        return Hex.hexStringToInt(Hex.bytesToHexString(fidBytes));
    }

    public byte getShortFileID() {
        if(optionalData == null) {
            return -1;
        }
        DERSequence s = (DERSequence)optionalData;
        if(s.size() != 2) {
            return -1;
        }
        return ((DEROctetString)s.getObjectAt(1)).getOctets()[0];
    }
    
    
}
