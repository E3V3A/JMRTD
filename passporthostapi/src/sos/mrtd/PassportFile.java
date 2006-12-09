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

package sos.mrtd;

/**
 * Passport file structure.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public abstract class PassportFile
{
   /**
    * Constructor only visible to the other
    * classes in this package.
    */
   PassportFile() {
   }
   
   /**
    * Gets the passportfile encoded in <code>obj</code>.
    * 
    * @param obj the object we want converted
    * 
    * @return a passport file structure
    * 
    * @throws IllegalArgumentException if the input object cannot be converted
    */
   public static PassportFile getInstance(Object obj) {
      // FIXME
      return null;
   }
   
   /**
    * Gets the contents of this file as byte array,
    * includes the ICAO tag and length.
    * 
    * @return a byte array containing the file
    */
   public abstract byte[] getEncoded();
   
   /* TODO: public abstract int getTag(); */
   /* TODO: public abstract int getLength(); */
}
