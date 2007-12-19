package sos.smartcards;

import java.util.EventListener;

public interface SessionListener extends EventListener
{
   /**
    * Is called after a session (re)starts.
    */
   void sessionStarted(CardService service);

   /**
    * Is called after a session stops.
    */
   void sessionStopped(CardService service);
}

