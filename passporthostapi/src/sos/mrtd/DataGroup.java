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

public class DataGroup extends PassportFile
{
   /**
    * Constructor only visible to the other
    * classes in this package.
    */
   DataGroup() {
   }
   
   /**
    * Constructor only visible to the other
    * classes in this package.
    * 
    * @param object datagroup contents.
    */
   DataGroup(BERTLVObject object) {
      sourceObject = object;
      isSourceConsistent = true;
   }
   
   @Override
   public byte[] getEncoded() {
      if (isSourceConsistent) {
         return sourceObject.getEncoded();
      }
      return null;
   }
   
   @Override
   public String toString() {
      if (isSourceConsistent) {
         return sourceObject.toString();
      }
      return super.toString();
   }
}
