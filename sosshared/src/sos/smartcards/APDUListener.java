package sos.smartcards;

import java.util.EventListener;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Specifies an event handler type to react to apdu events.
 *
 * @author Engelbert Hubbers (hubbers@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public interface APDUListener extends EventListener
{
   /**
    * Is called after an apdu was exchanged.
    *
    * @param capdu a Command apdu
    * @param rapdu the Response apdu
    */
   void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu);
}

