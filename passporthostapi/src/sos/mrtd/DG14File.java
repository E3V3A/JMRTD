package sos.mrtd;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSet;

import sos.tlv.BERTLVInputStream;
import sos.tlv.BERTLVObject;

public class DG14File extends DataGroup {

    private List<SecurityInfo> securityInfos = new ArrayList<SecurityInfo>();
    
    public DG14File(InputStream in) {
        try {
            BERTLVInputStream tlvIn = new BERTLVInputStream(in);
            tlvIn.readTag();
            tlvIn.readLength();
            byte[] value = tlvIn.readValue();
            ASN1InputStream asn1in = new ASN1InputStream(value);
            DERSet set = (DERSet)asn1in.readObject();
            for(int i=0; i<set.size();i++) {
                DERObject o = set.getObjectAt(i).getDERObject();
                SecurityInfo si = new SecurityInfo(o);
                if(ChipAuthenticationPublicKeyInfo.checkRequiredIdentifier(si.getObjectIdentifier())) {
                    si = new ChipAuthenticationPublicKeyInfo(si);
                }else if(ChipAuthenticationInfo.checkRequiredIdentifier(si.getObjectIdentifier())) {
                    si = new ChipAuthenticationInfo(si);                    
                }else if(TerminalAuthenticationInfo.checkRequiredIdentifier(si.getObjectIdentifier())) {
                    si = new TerminalAuthenticationInfo(si);                    
                }
                securityInfos.add(si);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }
    
    public byte[] getEncoded() {
        if(isSourceConsistent) {
            return sourceObject;
        }
        try {
            ASN1EncodableVector v = new ASN1EncodableVector();
            for(SecurityInfo si : securityInfos) {
                v.add(si.getDERObject());
            }
            DERSet s = new DERSet(v);
            BERTLVObject secInfos =
                new BERTLVObject(PassportFile.EF_DG14_TAG,
                        s.getDEREncoded());
            sourceObject = secInfos.getEncoded();
            isSourceConsistent = true;
            return sourceObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    
        
    }

}
