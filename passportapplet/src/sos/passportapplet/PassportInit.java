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

import javacard.framework.ISOException;
import javacard.framework.Util;

public class PassportInit {

    /**
     * Looks up the numerical value for MRZ characters. In order to be able to
     * compute check digits.
     * 
     * @param ch
     *            a character from the MRZ.
     * 
     * @return the numerical value of the character.
     * 
     * @throws NumberFormatException
     *             if <code>ch</code> is not a valid MRZ character.
     */
    private static byte decodeMRZDigit(byte ch) {
        switch (ch) {
        case '<':
        case '0':
            return 0;
        case '1':
            return 1;
        case '2':
            return 2;
        case '3':
            return 3;
        case '4':
            return 4;
        case '5':
            return 5;
        case '6':
            return 6;
        case '7':
            return 7;
        case '8':
            return 8;
        case '9':
            return 9;
        case 'a':
        case 'A':
            return 10;
        case 'b':
        case 'B':
            return 11;
        case 'c':
        case 'C':
            return 12;
        case 'd':
        case 'D':
            return 13;
        case 'e':
        case 'E':
            return 14;
        case 'f':
        case 'F':
            return 15;
        case 'g':
        case 'G':
            return 16;
        case 'h':
        case 'H':
            return 17;
        case 'i':
        case 'I':
            return 18;
        case 'j':
        case 'J':
            return 19;
        case 'k':
        case 'K':
            return 20;
        case 'l':
        case 'L':
            return 21;
        case 'm':
        case 'M':
            return 22;
        case 'n':
        case 'N':
            return 23;
        case 'o':
        case 'O':
            return 24;
        case 'p':
        case 'P':
            return 25;
        case 'q':
        case 'Q':
            return 26;
        case 'r':
        case 'R':
            return 27;
        case 's':
        case 'S':
            return 28;
        case 't':
        case 'T':
            return 29;
        case 'u':
        case 'U':
            return 30;
        case 'v':
        case 'V':
            return 31;
        case 'w':
        case 'W':
            return 32;
        case 'x':
        case 'X':
            return 33;
        case 'y':
        case 'Y':
            return 34;
        case 'z':
        case 'Z':
            return 35;
        default:
            throw new ISOException((short) 0x6d04);
        }
    }

    /**
     * Computes the 7-3-1 check digit for part of the MRZ.
     * 
     * @param chars
     *            a part of the MRZ.
     * 
     * @return the resulting check digit.
     */
    private static byte checkDigit(byte[] chars) {
        byte[] weights = { 7, 3, 1 };
        byte result = 0;
        for (short i = 0; i < chars.length; i++) {
            result = (byte) ((short) ((result + weights[i % 3]
                    * decodeMRZDigit(chars[i]))) % 10);
        }
        return result;
    }

    /**
     * Computes the static key seed, based on information from the MRZ.
     * 
     * @param docNrStr
     *            a string containing the document number.
     * @param dateOfBirthStr
     *            a string containing the date of birth (YYMMDD).
     * @param dateOfExpiryStr
     *            a string containing the date of expiry (YYMMDD).
     * 
     * @return a byte array of length 16 containing the key seed.
     */
    public static byte[] computeKeySeed(byte[] docNr,
            byte[] dateOfBirth, byte[] dateOfExpiry) {
        if (docNr.length != 9 || dateOfBirth.length != 6
                || dateOfExpiry.length != 6) {
            ISOException.throwIt((short) 0x6d03);
        }
    
        byte[] mrz_info = new byte[24];
        Util.arrayCopy(docNr, (short) 0, mrz_info, (short) 0, (short) 9);
        mrz_info[9] = (byte) (checkDigit(docNr) + 0x30); // ascii character
        Util.arrayCopy(dateOfBirth, (short) 0, mrz_info, (short) 10, (short) 6);
        mrz_info[16] = (byte) (checkDigit(dateOfBirth) + 0x30); // ascii
        // character
        Util.arrayCopy(dateOfExpiry, (short) 0, mrz_info, (short) 17, (short) 6);
        mrz_info[23] = (byte) (checkDigit(dateOfExpiry) + 0x30); // ascii
        // character
    
        byte[] hash = new byte[(short) 80];
        PassportCrypto.createHash(mrz_info, (short)0, (short)24, hash, (short)0);
        byte[] keySeed = new byte[16];
        Util.arrayCopy(hash, (short) 0, keySeed, (short) 0, (short) 16);
        return keySeed;
    }

}
