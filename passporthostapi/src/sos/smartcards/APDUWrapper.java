/* $Id: $ */
package sos.smartcards;

/**
 * Wrapper interface for command Apdu wrapping.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @version $Revision: 206 $
 */
public interface APDUWrapper
{
   /**
    * Wraps the command apdu buffer.
    * 
    * @param buffer should contain a header (length 4), an explicit lc (0 if
    *           no cdata), the cdata (of length lc), and an explicit le (0 if
    *           not specified).
    * @return wrapped apdu buffer
    */
   CommandAPDU wrap(CommandAPDU capdu);
   
   ResponseAPDU unwrap(ResponseAPDU rapdu, int len);
}