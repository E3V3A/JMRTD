package sos.smartcards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

/**
 * Manages card terminals.
 * Source of card insertion and removal events.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class CardManager
{
   private CardManager() {
      (new Thread(new Runnable() {
         public void run() {
            try {
               while (true) {
                  monitor();
                  Thread.sleep(200);
               }
            } catch (InterruptedException ie) {
               // NOTE: interrupted during blocked state, so quit running
            }
         }
      })).start();
   }

   private synchronized void monitor() throws InterruptedException {
   }
   
   public static void addCardTerminalListener(CardTerminalListener l) {
      PCSCCardManager.addCardTerminalListener(l);
   }  
   
   public static void removeCardTerminalListener(CardTerminalListener l) {
      PCSCCardManager.removeCardTerminalListener(l);
   }
}
