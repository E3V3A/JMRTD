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

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;
import javacard.security.DESKey;

/**
 * @author ceesb
 *
 */
public class PassportUtil implements ISO7816 {

    public static void throwArrayIndex(byte[] capdu, short s) {
        ISOException.throwIt((short)(0x6d00 | (capdu[s] & 0xff)));
    }

    /**
     * Counts the number of set bits in a byte
     * 
     * @param b
     *            the byte to be counted
     * @return 0 when number of bits in b is even
     */
    public static byte evenBits(byte b) {
        short count = 0;
    
        for (short i = 0; i < 8; i++) {
            count += (b >>> i) & 0x1;
        }
    
        return (byte) (count % 2);
    }

    public static void xor(byte[] in1, byte[] in2, byte[] out) {
        if(in1.length != in2.length || in2.length != out.length)
            ISOException.throwIt((short)0x6d06);
        
        short len = (short)in1.length;
        
        for(short s=0; s < len; s++) {
            out[s] = (byte)(in1[s] ^ in2[s]);
        }
    }

    public static short sign(short a) {
        return (byte)((a >>> (short)15) & 1); 
    }
    
    public static short min(short a, short b) {
        if(sign(a) == sign(b))
          return (a < b ? a : b);
        else if(sign(a) == 1)
            return b;
        else 
            return a;
    }

    public static void throwShort(short s) {
        ISOException.throwIt((short)(0x6d00 | (s & 0xff)));
    }

    public static void returnDESKey(DESKey k, APDU apdu) {
        byte[] b = new byte[(short)(k.getSize()/8)];
    
        k.getKey(b, (short) 0);
        returnBuffer(b, apdu);
    }

    public static void pad(APDU aapdu, short pad_len) {
        byte[] apdu = aapdu.getBuffer();
        short apdu_len=0;
        short lc = (short)(apdu[ISO7816.OFFSET_LC] & 0xff);
        try {
        apdu_len = (short)(ISO7816.OFFSET_CDATA + lc);
        } catch(NullPointerException e) {
            ISOException.throwIt((short)0x6d66);
        }
        
        if(apdu_len < apdu.length)
        if(pad_len < CREFPassportCrypto.PAD_DATA.length)
        Util.arrayCopy(CREFPassportCrypto.PAD_DATA, (short)0, apdu, 
                apdu_len, 
                pad_len);
    }

    public static void returnBuffer(byte[] b, APDU apdu) {
        returnBuffer(b, (short)b.length, apdu);
    }
    
    public static void returnBuffer(byte[] b, short length, APDU apdu) {
        returnBuffer(b, (short)0, length, apdu);
    }
    
    public static void returnBuffer(byte[] b, short offset, short length, APDU aapdu) {
        byte[] apdu = aapdu.getBuffer();
        short le = aapdu.setOutgoing(); //(short)(apdu[(short)(apdu.length-2)] & 0xff);   
    
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
        
        aapdu.setOutgoingLength(length);
        Util.arrayCopy(b, offset, apdu, (short)0, (short) length);
        aapdu.sendBytes((short) 0,length);
    
    }

    public static void returnByte(byte b, APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short le = apdu.setOutgoing();
        if (le != 1) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }
    
        apdu.setOutgoingLength((short) 1);
        buffer[0] = b;
        apdu.sendBytes((short) 0, (short) 1);
    }

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

}
