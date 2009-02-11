package sos.smartcards;

import java.math.BigInteger;

import sos.util.Hex;

/**
 * TODO: Work in progess, very messy at the moment.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 *
 */
public class FileInfo {
    
    /** This class reflects the File Control Parameters included in the FCI as described in ISO7816-4 in Table 12 */ 
    
    public final static byte FCI_BYTE = 0x6F;
    public final static byte FMD_BYTE = 0x64;
    public final static byte FCP_BYTE = 0x62;

    public final static byte DATA_BYTES1 = (byte)0x80;
    public final static byte DATA_BYTES2 = (byte)0x81;
    public final static byte FILE_DESCRIPTOR = (byte)0x82;
    public final static byte FILE_IDENTIFIER = (byte)0x83;
    public final static byte DF_NAME = (byte)0x84;
    public final static byte PROP_INFO = (byte)0x85;
    public final static byte SECURITY_ATTR_PROP = (byte)0x86;
    public final static byte FCI_EXT = (byte)0x87;
    public final static byte SHORT_EF = (byte)0x88;
    public final static byte LCS_BYTE = (byte)0x8A;
    public final static byte SECURITY_ATTR_EXP = (byte)0x8B;
    public final static byte SECURITY_ATTR_COMPACT = (byte)0x8C;
    public final static byte ENV_TEMP_EF = (byte)0x8D;
    public final static byte CHANNEL_SECURITY = (byte)0x8E;
    public final static byte A0 = (byte)0xA0;
    public final static byte A1 = (byte)0xA1;
    public final static byte A2 = (byte)0xA2;
    public final static byte A5 = (byte)0xA5;
    public final static byte AB = (byte)0xAB;
    public final static byte AC = (byte)0xAC;
    
    byte mainTag = -1;
    int fileLength = -1;
    int fileLengthFCI = -1;
    byte descriptorByte = -1;
    byte dataCodingByte = -1;
    short maxRecordSize = -1;
    short maxRecordsCount = -1;
    short fid = -1;
    byte[] dfName = null;
    byte[] propInfo = null;
    byte[] secAttrProp = null;
    byte[] secAttrExp = null;
    byte[] secAttrCompact = null;
    short fciExt = -1;
    short envTempEF = -1;
    byte shortEF = -1;
    byte lcsByte = -1;
    byte channelSecurity = -1;
    byte[] a0 = null;
    byte[] a1 = null;
    byte[] a2 = null;
    byte[] a5 = null;
    byte[] ab = null;
    byte[] ac = null;
    
    public FileInfo(byte[] fileInfo) throws CardServiceException {
        if(fileInfo.length == 0) {
            return;
        }
        if(fileInfo[0] != FCI_BYTE && fileInfo[0] != FCP_BYTE && fileInfo[0] != FMD_BYTE) {
            throw new CardServiceException("Malformed FCI data");
        }else{
            this.mainTag = fileInfo[0];            
        }
        byte[] tmp = new byte[fileInfo[1]];
        System.arraycopy(fileInfo, 2, tmp, 0, fileInfo[1]);
        fileInfo = tmp;
        int offset = 0;
        BigInteger integer = null;
        int off = 0;
        try {
          while(offset < fileInfo.length) {
            byte tag = fileInfo[offset++];
            byte len = fileInfo[offset++];
            byte[] contents = new byte[len];
            System.arraycopy(fileInfo, offset, contents, 0, len);
            offset += len;
            switch(tag) {
              case DATA_BYTES1:
                integer = new BigInteger(contents);
                integer.abs();
                this.fileLength = integer.intValue();
                break;
              case DATA_BYTES2:
                  checkLen(len, 2);
                  integer = new BigInteger(contents);
                  this.fileLengthFCI = integer.intValue();
                  break;
              case FILE_DESCRIPTOR:
                  checkLen(len, 1, 6);
                  off = 0;
                  this.descriptorByte = contents[off++];
                  if(off == contents.length)
                      break;
                  this.dataCodingByte = contents[off++];
                  if(off == contents.length)
                      break;
                  if(contents.length == 3) {
                      this.maxRecordSize = contents[off++];
                  }else{
                      integer = new BigInteger(new byte[]{contents[off++], contents[off++]});
                      this.maxRecordSize = integer.shortValue();
                  }
                  if(off == contents.length)
                      break;
                  if(contents.length == 5) {
                      this.maxRecordsCount = contents[off++];
                  }else{
                      integer = new BigInteger(new byte[]{contents[off++], contents[off++]});
                      this.maxRecordsCount = integer.shortValue();
                  }
                  break;
              case FILE_IDENTIFIER:
                  checkLen(len, 2);
                  integer = new BigInteger(contents);
                  this.fid = integer.shortValue();
                  break;
              case DF_NAME:
                  checkLen(len, 0, 16);
                  this.dfName = new byte[contents.length];
                  System.arraycopy(contents, 0, this.dfName, 0, contents.length);
                  break;
              case PROP_INFO:
                  this.propInfo = new byte[contents.length];
                  System.arraycopy(contents, 0, this.propInfo, 0, contents.length);
                  break;
              case SECURITY_ATTR_PROP:
                  this.secAttrProp = new byte[contents.length];
                  System.arraycopy(contents, 0, this.secAttrProp, 0, contents.length);
                  break;
              case FCI_EXT:
                  checkLen(len, 2);
                  integer = new BigInteger(contents);
                  this.fciExt = integer.shortValue();
                  break;
              case SHORT_EF:
                  checkLen(len, 0, 1);
                  if(len == 0) {
                      this.shortEF = 0;
                  }else{
                      this.shortEF = contents[0];
                  }
                  break;
              case LCS_BYTE:
                  checkLen(len, 1);
                  this.lcsByte = contents[0];
                  break;
              case SECURITY_ATTR_EXP:
                  this.secAttrExp = new byte[contents.length];
                  System.arraycopy(contents, 0, this.secAttrExp, 0, contents.length);
                  break;
              case SECURITY_ATTR_COMPACT:
                  this.secAttrCompact = new byte[contents.length];
                  System.arraycopy(contents, 0, this.secAttrCompact, 0, contents.length);
                  break;
              case ENV_TEMP_EF:
                  checkLen(len, 2);
                  integer = new BigInteger(contents);
                  this.envTempEF = integer.shortValue();
                  break;
              case CHANNEL_SECURITY:
                  checkLen(len, 1);
                  this.channelSecurity = contents[0];
                  break;
              case A0:
                  this.a0 = new byte[contents.length];
                  System.arraycopy(contents, 0, this.a0, 0, contents.length);
                  break;
              case A1:
                  this.a1 = new byte[contents.length];
                  System.arraycopy(contents, 0, this.a1, 0, contents.length);
                  break;
              case A2:
                  this.a2 = new byte[contents.length];
                  System.arraycopy(contents, 0, this.a2, 0, contents.length);
                  break;
              case A5:
                  this.a5 = new byte[contents.length];
                  System.arraycopy(contents, 0, this.a5, 0, contents.length);
                  break;
              case AB:
                  this.ab = new byte[contents.length];
                  System.arraycopy(contents, 0, this.ab, 0, contents.length);
                  break;
              case AC:
                  this.ac = new byte[contents.length];
                  System.arraycopy(contents, 0, this.ac, 0, contents.length);
                  break;
              default:
                  throw new CardServiceException("Malformed FCI: unrecognized tag.");
              }
          }
        }catch(ArrayIndexOutOfBoundsException aioobe) {
            throw new CardServiceException("Malformed FCI.");
        }        
    }

    private static void checkLen(int len, int value) throws CardServiceException {
        if(len != value) { 
            throw new CardServiceException("Malformed FCI.");
        }
    }

    private static void checkLen(int len, int minValue, int maxValue) throws CardServiceException {
        if(!(len >= minValue && len <= maxValue)) { 
            throw new CardServiceException("Malformed FCI.");
        }
    }

    
    public byte[] getFormatted() {
        byte[] result = new byte[0];
        if(mainTag == -1) {
            return result;
        }
        byte[] piece = null;
        if(fileLength != -1) {
            piece = getArray(DATA_BYTES1, Hex.hexStringToBytes(Hex.shortToHexString((short)fileLength)));
            result = catArray(result, piece);
        }
        if(fileLengthFCI != -1) {
            piece = getArray(DATA_BYTES2, Hex.hexStringToBytes(Hex.shortToHexString((short)fileLengthFCI)));
            result = catArray(result, piece);
        
        }
        if(descriptorByte != -1) {
            byte[] ar1 = new byte[] {descriptorByte};
            byte[] ar2 = new byte[0];
            if(dataCodingByte != -1) {
                ar2 = new byte[] {dataCodingByte};                
            }
            byte[] ar3 = new byte[0];
            if(maxRecordSize != -1) {
                String x = null;
                if(maxRecordSize <= 256) {
                    if(maxRecordsCount == -1 ) {
                    x = Hex.byteToHexString((byte)maxRecordSize);
                    }else{
                        x = Hex.shortToHexString(maxRecordSize);                        
                    }
                }else{
                    x = Hex.shortToHexString(maxRecordSize);
                }
                ar3 = Hex.hexStringToBytes(x);
            }
            byte[] ar4 = new byte[0];
            if(maxRecordsCount != -1) {
                String x = null;
                if(maxRecordsCount <= 256) {
                    x = Hex.byteToHexString((byte)maxRecordsCount);
                }else{
                    x = Hex.shortToHexString(maxRecordsCount);
                }
                ar4 = Hex.hexStringToBytes(x);
            }
            piece = getArray(FILE_DESCRIPTOR, catArray(catArray(catArray(ar1, ar2), ar3), ar4));
            result = catArray(result, piece);
        }
        if(fid != -1) {
            piece = getArray(FILE_IDENTIFIER, Hex.hexStringToBytes(Hex.shortToHexString(fid)));
            result = catArray(result, piece);
        }
        if(dfName != null) {
            piece = getArray(DF_NAME, dfName);
            result = catArray(result, piece);            
        }
        if(propInfo != null) {
            piece = getArray(PROP_INFO, propInfo);
            result = catArray(result, piece);                        
        }
        if(secAttrProp != null) {
            piece = getArray(SECURITY_ATTR_PROP, secAttrProp);
            result = catArray(result, piece);                        
        }
        if(fciExt != -1) {
            piece = getArray(FCI_EXT, Hex.hexStringToBytes(Hex.shortToHexString(fciExt)));
            result = catArray(result, piece);
        }
        if(shortEF != -1) {
            piece = getArray(SHORT_EF, (shortEF ==0) ? new byte[0] : new byte[]{shortEF});
            result = catArray(result, piece);
        }
        if(lcsByte != -1) {
            piece = getArray(LCS_BYTE, new byte[]{lcsByte});
            result = catArray(result, piece);
        }
        if(secAttrExp != null) {
            piece = getArray(SECURITY_ATTR_EXP, secAttrExp);
            result = catArray(result, piece);                        
        }
        if(secAttrCompact != null) {
            piece = getArray(SECURITY_ATTR_COMPACT, secAttrCompact);
            result = catArray(result, piece);                        
        }
        if(envTempEF != -1) {
            piece = getArray(ENV_TEMP_EF, Hex.hexStringToBytes(Hex.shortToHexString(envTempEF)));
            result = catArray(result, piece);
        }
        if(channelSecurity != -1) {
            piece = getArray(CHANNEL_SECURITY, new byte[]{channelSecurity});
            result = catArray(result, piece);
        }
        if(a0 != null) {
            piece = getArray(A0, a0);
            result = catArray(result, piece);                        
        }
        if(a1 != null) {
            piece = getArray(A1, a1);
            result = catArray(result, piece);                        
        }
        if(a2 != null) {
            piece = getArray(A2, a2);
            result = catArray(result, piece);                        
        }
        if(a5 != null) {
            piece = getArray(A5, a5);
            result = catArray(result, piece);                        
        }
        if(ab != null) {
            piece = getArray(AB, ab);
            result = catArray(result, piece);                        
        }
        if(ac != null) {
            piece = getArray(AC, ac);
            result = catArray(result, piece);                        
        }
        return getArray(mainTag, result);
        
    }
    
    private static byte[] getArray(byte tag, byte[] contents) {
        byte[] result = new byte[contents.length + 2];
        result[0] = tag;
        result[1] = (byte)contents.length;
        System.arraycopy(contents, 0, result, 2, contents.length);
        return result;
    }
    
    private static byte[] catArray(byte[] a1, byte[] a2) {
        byte[] result = new byte[a1.length + a2.length];
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
        
    }
    
    public String toString() {
        return 
          "Length: "+fileLength + "\n" +
          "Length FCI: "+fileLengthFCI + "\n" +
          "Desc byte: "+descriptorByte + "\n" +
          "Data byte: "+dataCodingByte + "\n" +
          "Record size: "+maxRecordSize + "\n" +
          "Record count: "+maxRecordsCount + "\n" +
          "FID: "+Hex.shortToHexString(fid) + "\n" +
          "DF name: "+Hex.bytesToHexString(dfName) + "\n" +
          "propInfo: "+Hex.bytesToHexString(propInfo) + "\n" +
          "secAttrProp: "+Hex.bytesToHexString(secAttrProp) + "\n" +
          "secAttrExp: "+Hex.bytesToHexString(secAttrExp) + "\n" +
          "secAttrComp: "+Hex.bytesToHexString(secAttrCompact) + "\n" +
          "FCI ext: "+Hex.shortToHexString(fciExt) + "\n" +
          "EF env temp: "+Hex.shortToHexString(envTempEF) + "\n" +
          "Short EF: "+Hex.byteToHexString(shortEF) + "\n" +
          "LCS byte: "+Hex.byteToHexString(lcsByte) + "\n" +
          "Channel sec: "+Hex.byteToHexString(channelSecurity) + "\n" +
          "a0: "+Hex.bytesToHexString(a0) + "\n" +
          "a1: "+Hex.bytesToHexString(a1) + "\n" +
          "a2: "+Hex.bytesToHexString(a2) + "\n" +
          "a5: "+Hex.bytesToHexString(a5) + "\n" +
          "ab: "+Hex.bytesToHexString(ab) + "\n" +
          "ac: "+Hex.bytesToHexString(ac) + "\n";
    }

    public static void main(String[] args) {
        // TODO: This class needs way more testing... 
    
    byte[] fci2 =  {
            0x6F, 37,
    (byte)0x82, 0x01, 0x38, (byte)0x83, 0x02, (byte)0xEE, (byte)0xEE,
    (byte)0x84, 0x10, (byte)0xD2, 0x33, 0x00, 0x00, 0x01, 0x00,
    0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00,(byte) 0x85, 0x02, 0x15, (byte)0xC2, (byte)0x8A, 0x01,
    0x05, (byte)0xA1, 0x03, (byte)0x8B, 0x01, 0x02
    };

    byte[] fci3 =  {
            0x6F, 23,
            (byte)0x82, 0x05, 0x04, 0x41, 0x00, 0x32, 0x10, (byte)0x83,
    0x02, 0x50, 0x44, (byte)0x85, 0x02, 0x01, (byte)0x8C, (byte)0x8A,
    0x01, 0x05, (byte)0xA1, 0x03, (byte)0x8B, 0x01, 0x01
    };
    try {
      FileInfo f2 = new FileInfo(fci2);
      System.out.println(f2);
      FileInfo f3 = new FileInfo(fci3);
      System.out.println(f3);
      System.out.println(Hex.bytesToHexString(fci2));
      System.out.println(Hex.bytesToHexString(f2.getFormatted()));
      System.out.println(Hex.bytesToHexString(fci3));
      System.out.println(Hex.bytesToHexString(f3.getFormatted()));
    }catch(CardServiceException cse) {
        cse.printStackTrace();
    }
    }


    
}
