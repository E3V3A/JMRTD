package sos.mrtd;

import java.io.IOException;
import java.util.Date;

import sos.mrtd.*;
import sos.smartcards.BERTLVObject;

public class DG1File extends PassportFile
{
   private MRZInfo mrz;

   public DG1File(MRZInfo mrz) {
      this.mrz = mrz;
   }

   public MRZInfo getMRZInfo() {
      return mrz;
   }

   public byte[] getEncoded() {
      try {
         BERTLVObject ef0101 =
            new BERTLVObject(PassportASN1Service.EF_DG1_TAG,
               new BERTLVObject(0x5f1f, mrz.getEncoded()));
         return ef0101.getEncoded();
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }
}
