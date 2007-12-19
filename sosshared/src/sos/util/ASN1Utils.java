/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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
 * $Id: $
 */

package sos.util;

import java.io.ByteArrayOutputStream;

public class ASN1Utils {
    public static byte[] lengthId(int dataLength) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int unitBits = 8;
        int maxUnits = 4;
        int unitsMinusOne = maxUnits;

        if (dataLength < 0x80) {
            out.write(dataLength);
        } else {
            int i = maxUnits - 1;
            for (; i >= 0; i--) {
                byte val = (byte) ((dataLength >>> (i * unitBits)) & 0xff);

                if (val == 0 && unitsMinusOne == maxUnits) {
                    continue;
                }
                if (unitsMinusOne == maxUnits) {
                    unitsMinusOne = i;
                    out.write(0x80 + unitsMinusOne + 1);
                }
                out.write(val);
            }
        }

        return out.toByteArray();
    }

}
