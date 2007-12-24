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
 * Manages the cards in PCSC card terminals.
 * This is the source of card insertion and removal events.
 * Ideally this should be the only place where PCSCCardService
 * instances are created.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
class PCSCCardManager
{
   private static PCSCCardManager manager = new PCSCCardManager();
   private Collection<CardTerminalListener> listeners;
   private Map<CardTerminal, Boolean> cardPresentList;
   private Map<CardTerminal, CardService> terminalServices;
   private Collection<CardTerminal> terminals;

   private PCSCCardManager() {
      listeners = new ArrayList<CardTerminalListener>();
      terminals = new HashSet<CardTerminal>();
      cardPresentList = new Hashtable<CardTerminal, Boolean>();
      terminalServices = new Hashtable<CardTerminal, CardService>();
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
      while (listeners.isEmpty()) {
         wait();
      }
      try {
         TerminalFactory terminalFactory = TerminalFactory.getDefault();
         terminals.addAll(terminalFactory.terminals().list());
         for (CardTerminal terminal: terminals) {
            boolean wasCardPresent = terminal != null && cardPresentList.containsKey(terminal) && cardPresentList.get(terminal);
            boolean isCardPresent = false;
            try {
               isCardPresent = terminal != null && terminal.isCardPresent();
            } catch (CardException ce) {
               /* On error, card no longer present... */
            }
            
            if (wasCardPresent && !isCardPresent) {
               CardService service = terminalServices.get(terminal);
               if (service != null) {
                  for (CardTerminalListener l: listeners) {
                     l.cardRemoved(new CardEvent(CardEvent.REMOVED, service));
                  }
                  service.close();
                  terminalServices.remove(terminal);
               }
               cardPresentList.put(terminal, false);
            } else if (!wasCardPresent && isCardPresent) {
               PCSCCardService service = new PCSCCardService();
               service.open(terminal);
               terminalServices.put(terminal, service);
               for (CardTerminalListener l: listeners) {
                  l.cardInserted(new CardEvent(CardEvent.INSERTED, service));
               }
               cardPresentList.put(terminal, true);
            }
         }
      } catch (CardException ce) {
         // NOTE: remain in same state?!?
      }
   }
   
   private synchronized void addListener(CardTerminalListener l) {
      listeners.add(l);
      notifyAll();
   }

   private synchronized void removeListener(CardTerminalListener l) {
      listeners.remove(l);
   }

   public static void addCardTerminalListener(CardTerminalListener l) {
      manager.addListener(l);
   }  
   
   public static void removeCardTerminalListener(CardTerminalListener l) {
      manager.removeListener(l);
   }
}
