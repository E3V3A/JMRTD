package sos.mrtd;

import java.util.EventListener;

import sos.smartcards.CardEvent;

public interface PassportListener extends EventListener
{
   void passportInserted(PassportEvent ce);

   void passportRemoved(PassportEvent ce);
}
