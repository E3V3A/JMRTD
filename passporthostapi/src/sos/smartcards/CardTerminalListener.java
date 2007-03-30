package sos.smartcards;

public interface CardTerminalListener
{
   void cardInserted(CardTerminalEvent ce);
   
   void cardRemoved(CardTerminalEvent ce);
}
