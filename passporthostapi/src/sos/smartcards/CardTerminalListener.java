package sos.smartcards;

import java.util.EventListener;

public interface CardTerminalListener extends EventListener
{
   void cardInserted(CardEvent ce);
   
   void cardRemoved(CardEvent ce);
}
