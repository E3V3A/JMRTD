package sos.mrtd;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import sos.smartcards.BERTLVObject;

/**
 * See A 13.3 in MRTD's LDS document.
 */
public class DG2File extends PassportFile
{
    private static short BIOMETRIC_INFO_GROUP_TAG = 0x7F61;
    private static short BIOMETRIC_INFO_TAG = 0x7F60;
    private static byte BIOMETRIC_INFO_COUNT_TAG = 0x02;
    private static byte BIOMETRIC_HEADER_BASE_TAG = (byte) 0xA1;
    private static short BIOMETRIC_DATA_TAG = 0x5F2E;

    private static byte FORMAT_OWNER_TAG = (byte) 0x87;
    private static byte FORMAT_TYPE_TAG = (byte) 0x88;

    // Facial Record Header, Sect. 5.4, ISO SC37
    private static byte[] FORMAT_IDENTIFIER = { 'F', 'A', 'C', 0x00 };
    private static byte[] VERSION_NUMBER = { '0', '1', '0', 0x00 };

    private List<FaceInfo> faces;
    
    public DG2File(FaceInfo fi) {
        faces = new ArrayList<FaceInfo>();
        addFaceInfo(fi);
    }

    public void addFaceInfo(FaceInfo fi) {
        faces.add(fi);
    }

    private byte[] formatOwner(Image i) {
        // FIXME
        byte[] ownr = { 0x01, 0x01 };
        return ownr;
    }

    private byte[] formatType(Image i) {
        // FIXME
        byte[] fmt = { 0x00, 0x08 };
        return fmt;
    }

    public byte[] getEncoded() {
       try {
          BERTLVObject group = new BERTLVObject(BIOMETRIC_INFO_GROUP_TAG,
                new BERTLVObject(BIOMETRIC_INFO_COUNT_TAG,
                      (byte) faces.size()));
          BERTLVObject dg2 = new BERTLVObject(PassportASN1Service.EF_DG2_TAG,
                group);

          Iterator<FaceInfo> i = faces.iterator();
          while (i.hasNext()) {
             FaceInfo info = i.next();

             BERTLVObject header = new BERTLVObject(BIOMETRIC_HEADER_BASE_TAG++,
                   new BERTLVObject(FORMAT_TYPE_TAG,
                         formatType(info.getImage())));
             header.addSubObject(new BERTLVObject(FORMAT_OWNER_TAG,
                   formatOwner(info.getImage())));

             BERTLVObject face = new BERTLVObject(BIOMETRIC_INFO_TAG, header);
             group.addSubObject(face);
             // NOTE: multiple FaceInfos may be embedded in two ways:
                // 1) as multiple images in the same record (see Fig. 3 in ISO/IEC
                      // 19794-5)
                // 2) as multiple records (see A 13.3 in LDS technical report).
             // We choose option 2, because otherwise we have to precalc the
             // total length of all FaceInfos, which sucks.
             byte[] faceInfoBytes = info.getEncoded();
             int lengthOfRecord = FORMAT_IDENTIFIER.length
             + VERSION_NUMBER.length + 4 + 2;
             short nrOfImagesInRecord = 1;
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             DataOutputStream dataOut = new DataOutputStream(out);
             dataOut.write(FORMAT_IDENTIFIER);
             dataOut.write(VERSION_NUMBER);
             dataOut.writeInt(lengthOfRecord);
             dataOut.writeShort(nrOfImagesInRecord);
             dataOut.write(faceInfoBytes);
             dataOut.flush();
             byte[] facialRecord = out.toByteArray();

             face.addSubObject(new BERTLVObject(BIOMETRIC_DATA_TAG, facialRecord));
          }

          return dg2.getEncoded();
       } catch (IOException ioe) {
          return null;
       }
    }

    public static void main(String[] s) {
        BufferedImage image = null;
        DG2File dg2 = null;

        Iterator iter = Arrays.asList(s).iterator();
        while (iter.hasNext()) {
            try {
                image = ImageIO.read(new File((String) iter.next()));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            FaceInfo info = new FaceInfo(MRZInfo.Gender.MALE,
                                         FaceInfo.EyeColor.BLUE,
                                         FaceInfo.HAIR_COLOR_BLACK,
                                         FaceInfo.EXPRESSION_FROWNING,
                                         FaceInfo.SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM,
                                         image);

            if (dg2 == null) {
                dg2 = new DG2File(info);
            } else {
                dg2.addFaceInfo(info);
            }
        }
        try {
            System.out.println(BERTLVObject.getInstance(new DataInputStream(new ByteArrayInputStream(dg2.getEncoded()))));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
