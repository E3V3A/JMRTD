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
