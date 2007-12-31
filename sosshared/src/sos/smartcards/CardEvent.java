package sos.smartcards;

import java.util.EventObject;

public class CardEvent extends EventObject
{
   public static final int REMOVED = 0, INSERTED = 1;

   private int type;
   private CardService service;
   
   public CardEvent(int type, CardService service) {
      super(service);
      this.type = type;
      this.service = service;
   }
   
   public int getType() {
      return type;
   }

   public CardService getService() {
      return service;
   }

   public String toString() {
      switch (type) {
         case REMOVED: return "Card removed from " + service;
         case INSERTED: return "Card inserted in " + service;
      }
      return "CardTerminalEvent " + service;
   }
}
