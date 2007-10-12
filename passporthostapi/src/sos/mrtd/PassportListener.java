package sos.mrtd;

import java.util.EventListener;

import sos.smartcards.CardTerminalEvent;

public interface PassportListener extends EventListener
{
   void unknownPassportInserted(CardTerminalEvent ce);

   void knownPassportInserted(CardTerminalEvent ce);

   void passportRemoved(CardTerminalEvent ce);
}
