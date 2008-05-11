package sos.mrtd;

import java.util.EventListener;

public interface PassportListener extends EventListener
{
   void passportInserted(PassportEvent ce);

   void passportRemoved(PassportEvent ce);
}
