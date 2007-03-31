package sos.smartcards;

import java.util.EventListener;

public interface CardTerminalListener extends EventListener
{
   void cardInserted(CardTerminalEvent ce);
   
   void cardRemoved(CardTerminalEvent ce);
}
