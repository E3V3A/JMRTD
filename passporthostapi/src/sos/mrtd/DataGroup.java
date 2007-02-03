/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2007  SoS group, ICIS, Radboud University
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

package sos.mrtd;

import sos.smartcards.BERTLVObject;

public abstract class DataGroup extends PassportFile
{
   /* 
    * We're using a dual representation with a "dirty-bit": When the DG is
    * read from a passport we need to store the binary information as-is
    * since our constructed getEncoded() method might not result in exactly
    * the same byte[] (messing up any cryptographic hash computations needed
    * to validate the security object). -- MO
    */
   BERTLVObject sourceObject;
   boolean isSourceConsistent;
   
   @Override
   /*@ ensures
    *@    isSourceConsistent ==> \result.equals(sourceObject.getEncoded());
    */
   public abstract byte[] getEncoded();
}
