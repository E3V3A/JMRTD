package sos.mrtd;

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

public class SecurityInfo  {

    protected DERObjectIdentifier identifier;
    protected DERObject requiredData;
    protected DERObject optionalData;
    
    public SecurityInfo(SecurityInfo securityInfo) {
        this(securityInfo.identifier, securityInfo.requiredData, securityInfo.optionalData);
    }

    
    public SecurityInfo(DERObjectIdentifier identifier, DERObject requiredData) {
        this(identifier, requiredData, null);
    }
    
    public SecurityInfo(DERObjectIdentifier identifier, DERObject requiredData, DERObject optionalData) {
        this.identifier = identifier;
        this.requiredData = requiredData;
        this.optionalData = optionalData;
        checkFields();
    }

    public SecurityInfo(DERObject obj) {
        try {
            DERSequence s = (DERSequence)obj;
            identifier = (DERObjectIdentifier)s.getObjectAt(0);
            requiredData = s.getObjectAt(1).getDERObject();
            if(s.size() == 3) {
                optionalData = s.getObjectAt(2).getDERObject();
            }            
            checkFields();
        }catch(Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Malfromed input stream.");            
        }
    }
    
    public SecurityInfo(InputStream in) throws IOException {
        this(new ASN1InputStream(in).readObject());
    }
    
    public DERObject getDERObject() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(identifier);
        v.add(requiredData);
        if(optionalData != null) {
            v.add(optionalData);
        }
        return new DERSequence(v);
    }
    
    public DERObjectIdentifier getObjectIdentifier() {
        return identifier;
    }
    
    protected void checkFields() {
        
    }
    
}
