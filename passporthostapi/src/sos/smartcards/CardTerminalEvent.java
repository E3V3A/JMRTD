package sos.smartcards;

import java.util.EventObject;

import javax.smartcardio.CardTerminal;

public class CardTerminalEvent extends EventObject
{
   public static final int REMOVED = 0, INSERTED = 1;

   private int type;
   private CardTerminal terminal;

   public CardTerminalEvent(int type, CardTerminal terminal) {
      super(terminal);
      this.type = type;
      this.terminal = terminal;
   }
   
   public int getType() {
      return type;
   }

   public CardTerminal getTerminal() {
      return terminal;
   }

   public String toString() {
      switch (type) {
         case REMOVED: return "Card removed from " + terminal.getName();
         case INSERTED: return "Card inserted in " + terminal.getName();
      }
      return "CardTerminalEvent " + terminal.getName();
   }
}
