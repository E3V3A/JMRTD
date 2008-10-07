/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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

import java.util.EventObject;

public class PassportEvent extends EventObject
{
   public static final int REMOVED = 0, INSERTED = 1;

   private int type;
   private PassportService service;
   
   public PassportEvent(int type, PassportService service) {
      super(service);
      this.type = type;
      this.service = service;
   }
   
   public int getType() {
      return type;
   }

   public PassportService getService() {
      return service;
   }

   public String toString() {
      switch (type) {
         case REMOVED: return "Passport removed from " + service;
         case INSERTED: return "Passport inserted in " + service;
      }
      return "CardEvent " + service;
   }
   
   public boolean equals(Object other) {
      if (other == null) { return false; }
      if (other == this) { return true; }
      if (other.getClass() != PassportEvent.class) { return false; }
      PassportEvent otherCardEvent = (PassportEvent)other;
      return type == otherCardEvent.type && service.equals(otherCardEvent.service);
   }
}
