/*
 * passportapplet - A reference implementation of the MRTD standards.
 *
 * Copyright (C) 2006  SoS group, Radboud University
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id$
 */

package sos.passportapplet;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;

/**
 * Generic helpers for the Passport.
 * 
 * Contains some commented debug code.
 * 
 * @author Cees-Bart Breunese (ceesb@cs.ru.nl)
 *
 */
public class PassportUtil implements ISO7816 {
    
//    public static void throwArrayIndex(byte[] capdu, short s) {
//        ISOException.throwIt((short)(0x6d00 | (capdu[s] & 0xff)));
//    }

    /**
     * Counts the number of set bits in a byte
     * 
     * @param b byte to be counted
     * @return 0 when number of bits in b is even
     */
    public static byte evenBits(byte b) {
        short count = 0;
    
        for (short i = 0; i < 8; i++) {
            count += (b >>> i) & 0x1;
        }
    
        return (byte) (count % 2);
    }

    /**
     * Calculates the xor of byte arrays in1[in1_offset..+length] and 
     * in2[in2_offset..+length] in out[out_offset..length]. 
     * 
     * Arrays may be the same, but regions may not overlap.
     * 
     * 
     * @param in1 input array
     * @param in1_offset offset of input array
     * @param in2 input array
     * @param in2_offset offset of inputarray
     * @param out output array
     * @param out_offset offset of output array
     * @param len length of xor
     */
    public static void xor(byte[] in1, short in1_offset, byte[] in2, short in2_offset, byte[] out, short out_offset, short len) {
        if(in1.length < (short)(in1_offset + len) ||  // sanity checks
           in2.length < (short)(in2_offset + len) ||
           out.length < (short)(out_offset + len)) {        
           ISOException.throwIt((short)0x6d66);
        }
        // untested
        if(in1 == out && ((out_offset >= in1_offset && out_offset < (short)(in1_offset + len)) ||
                          (in1_offset >= out_offset && in1_offset < (short)(out_offset + len)))) {
            ISOException.throwIt((short)0x6d66);
        }
        // untested
        if(in2 == out && ((out_offset >= in2_offset && out_offset < (short)(in2_offset + len)) ||
                          (in2_offset >= out_offset && in2_offset < (short)(out_offset + len)))) {
            ISOException.throwIt((short)0x6d66);
        }
        
        for(short s=0; s < len; s++) {
            out[(short)(out_offset + s)] = (byte)(in1[(short)(in1_offset + s)] ^ in2[(short)(in2_offset + s)]);
        }
    }

    /**
     * Swaps two non-overlapping segments of the same length in the same byte array 
     * in place.
     * 
     * @param buffer a byte array 
     * @param offset1 offset to first byte array
     * @param offset2 offset to the second byte array
     * @param len length of the segments
     */
    public static void swap(byte[] buffer, short offset1, short offset2, short len) {
        if(buffer.length < (short)(offset1 + len) ||  // sanity checks
           buffer.length < (short)(offset2 + len) ||
           (offset1 <= offset2 && offset2 < (short)(offset1 + len)) || // no overlap
           (offset2 <= offset1 && offset1 < (short)(offset2 + len))) {
                ISOException.throwIt((short)0x6d66);
        }
        
        byte byte1, byte2;
        for(short i=0; i<len; i++) {
            byte1 = buffer[(short)(offset1 + i)];
            byte2 = buffer[(short)(offset2 + i)];
            buffer[(short)(offset1 + i)] = byte2;
            buffer[(short)(offset2 + i)] = byte1;
        }
    }
    
    /**
     * Returns the sign bit of a short as a short.
     * @param a a short value
     * @return the sign bit of a as a short
     */
    public static short sign(short a) {
        return (byte)((a >>> (short)15) & 1); 
    }
    
    /**
     * Returns the smallest unsigned short argument.
     * 
     * @param a a short
     * @param b another short
     * @return smallest unsigned value a or b.
     */
    public static short min(short a, short b) {
        if(sign(a) == sign(b))
          return (a < b ? a : b);
        else if(sign(a) == 1)
            return b;
        else 
            return a;
    }

//    public static void throwShort(short s) {
//        ISOException.throwIt((short)(0x6d00 | (s & 0xff)));
//    }
//
//    public static void returnDESKey(DESKey k, APDU apdu) {
//        byte[] b = new byte[(short)(k.getSize()/8)];
//    
//        k.getKey(b, (short) 0);
//        returnBuffer(b, apdu);
//    }

    /***
     * Pads an input buffer with max 8 bytes padding (0x80 followed by optional zeros) 
     * relative to the offset and length given.
     * 
     * @param buffer array to pad
     * @param offset to data
     * @param length of data
     * @return new length, with padding, of data 
     * 
     */
    public static short pad(byte[] buffer, short offset, short len) {
        short padbytes = (short)(8 - (len % 8));
        
        if((short)buffer.length < (short)(padbytes + offset + len)) {
            ISOException.throwIt((short)0x6d66);
        }
        
        for(short i=0; i<padbytes; i++) {
            buffer[(short)(offset+len+i)] = (i == 0 ? (byte)0x80 : 0x00);
        }
        
        return (short)(len + padbytes);
    }
    
//    public static void pad(APDU aapdu, short pad_len) {
//        byte[] apdu = aapdu.getBuffer();
//        short apdu_len=0;
//        short lc = (short)(apdu[ISO7816.OFFSET_LC] & 0xff);
//        try {
//        apdu_len = (short)(ISO7816.OFFSET_CDATA + lc);
//        } catch(NullPointerException e) {
//            ISOException.throwIt((short)0x6d66);
//        }
//        
//        if(apdu_len < apdu.length)
//        if(pad_len < CREFPassportCrypto.PAD_DATA.length)
//        Util.arrayCopy(CREFPassportCrypto.PAD_DATA, (short)0, apdu, 
//                apdu_len, 
//                pad_len);
//    }

//    public static void returnBuffer(byte[] b, APDU apdu) {
//        returnBuffer(b, (short)b.length, apdu);
//    }
//    
//    public static void returnBuffer(byte[] b, short length, APDU apdu) {
//        returnBuffer(b, (short)0, length, apdu);
//    }
    
//    public static void returnBuffer(byte[] b, short offset, short length, APDU aapdu) {
//        byte[] apdu = aapdu.getBuffer();
//        short le = aapdu.setOutgoing(); //(short)(apdu[(short)(apdu.length-2)] & 0xff);   
//    
//        if (le >= b.length) {
//            aapdu.setOutgoingLength(le);
//            if(le > b.length) {
//                pad(aapdu, (short)(le - (short)b.length));
//            }
//        }
//        else
//        {            
//            ISOException.throwIt(SW_WRONG_LENGTH);
//        }
//        
//        aapdu.setOutgoingLength(length);
//        Util.arrayCopy(b, offset, apdu, (short)0, (short) length);
//        aapdu.sendBytes((short) 0,length);
//    
//    }

//    public static void returnByte(byte b, APDU apdu) {
//        byte[] buffer = apdu.getBuffer();
//        short le = apdu.setOutgoing();
//        if (le != 1) {
//            ISOException.throwIt(SW_WRONG_LENGTH);
//        }
//    
//        apdu.setOutgoingLength((short) 1);
//        buffer[0] = b;
//        apdu.sendBytes((short) 0, (short) 1);
//    }

    /***
     * Computes the actual length of a data block as byte value, without the padding.
     * 
     * @param apdu containing data
     * @param offset to data
     * @param length of data
     * @return new length of data, without padding
     */
    public static byte calcLcFromPaddedData(byte[] apdu, short offset, short length) {
        for(short i=(short)(length - 1) ; i>=0; i--)
            if(apdu[(short)(offset + i)] != 0)
                if((apdu[(short)(offset + i)] & 0xff)!= 0x80)
                    // not padded
                    return (byte)(length & 0xff);       
                else
                    return (byte)(i & 0xff);       
        
        return 0;
    }

    /***
     * Does return (state & bitmask) == bitmask.
     * 
     * @param state
     * @param bitmask
     * @return (state & bitmask) == bitmask
     */
    public static boolean hasBitMask(byte state, byte bitmask) {
        return (state & bitmask) == bitmask;
    }

}
