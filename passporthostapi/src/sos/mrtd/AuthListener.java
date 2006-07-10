/*
 * Created on Jul 10, 2006
 */
package sos.mrtd;

import java.security.PublicKey;

/**
 * Listener for authentication events.
 * FIXME: perhaps add some more details to the events (which nonces were exchanged, for instance)
 * and wrap those into an actual event object.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public interface AuthListener {

   void performedBAC(SecureMessagingWrapper wrapper);
   
   void performedAA(PublicKey pubkey, boolean success);
   
}
