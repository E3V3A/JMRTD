package sos.smartcards;

/**
 * Manages card terminals.
 * Facade for concrete card terminal managers.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public abstract class CardManager
{
   public static void addCardTerminalListener(CardTerminalListener l) {
      PCSCCardManager.addCardTerminalListener(l);
   }  

   public static void removeCardTerminalListener(CardTerminalListener l) {
      PCSCCardManager.removeCardTerminalListener(l);
   }
}
