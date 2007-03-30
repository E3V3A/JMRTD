package sos.smartcards;

import javax.smartcardio.CardTerminal;

public class CardTerminalEvent
{
   public static final int REMOVED = 0, INSERTED = 1;

   private int type;
   private CardTerminal terminal;

   public CardTerminalEvent(int type, CardTerminal terminal) {
      this.type = type;
      this.terminal = terminal;
   }

   public CardTerminal getTerminal() {
      return terminal;
   }

   public String toString() {
      switch (type) {
         case REMOVED: return "Card removed from " + terminal.getName();
         case INSERTED: return "Card inserted in " + terminal.getName();
      }
      return "CardEvent " + terminal.getName();
   }
}
