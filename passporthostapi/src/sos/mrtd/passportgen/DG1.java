package sos.mrtd.passportgen;

import java.io.IOException;
import java.util.Date;

import sos.mrtd.*;
import sos.smartcards.BERTLVObject;

public class DG1 {
    MRZInfo mrz;

    public DG1(MRZInfo mrz) {
        this.mrz = mrz;
    }

    public byte[] getEncoded() {

        BERTLVObject ef0101=null;
        try {
            ef0101 = new BERTLVObject(PassportASN1Service.EF_DG1_TAG,
                                                   new BERTLVObject(0x5f1f,
                                                                    mrz.getEncoded()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ef0101.getEncoded();
    }
}