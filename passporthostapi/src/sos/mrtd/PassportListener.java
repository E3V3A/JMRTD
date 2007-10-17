package sos.mrtd;

import java.util.EventListener;

import sos.smartcards.CardEvent;

public interface PassportListener extends EventListener
{
   void passportInserted(CardEvent ce);

   void passportRemoved(CardEvent ce);
}
