package sos.smartcards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

public class CardTerminalManager
{
   private static CardTerminalManager terminalManager = new CardTerminalManager();
   private Collection<CardTerminalListener> listeners;
   private Map<CardTerminal, Boolean> wasCardPresent;
   private Map<CardTerminal, CardService> terminalServices;
   private Collection<CardTerminal> terminals;

   private CardTerminalManager() {
      listeners = new ArrayList<CardTerminalListener>();
      terminals = new HashSet<CardTerminal>();
      wasCardPresent = new Hashtable<CardTerminal, Boolean>();
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
            if (terminal != null && (!wasCardPresent.containsKey(terminal) ||
                  wasCardPresent.get(terminal) && !terminal.isCardPresent())) {
               CardService service = terminalServices.get(terminal);
               if (service != null) {
                  for (CardTerminalListener l: listeners) {
                     l.cardRemoved(new CardTerminalEvent(CardTerminalEvent.REMOVED, service));
                  }
                  service.close();
                  terminalServices.remove(terminal);
               }
               wasCardPresent.put(terminal, false);
            } else if (terminal != null && (!wasCardPresent.containsKey(terminal) ||
                  !wasCardPresent.get(terminal) && terminal.isCardPresent())) {
               PCSCCardService service = new PCSCCardService();
               service.open(terminal);
               terminalServices.put(terminal, service);
               for (CardTerminalListener l: listeners) {
                  l.cardInserted(new CardTerminalEvent(CardTerminalEvent.INSERTED, service));
               }
               wasCardPresent.put(terminal, true);
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
      terminalManager.addListener(l);
   }  
   
   public static void removeCardTerminalListener(CardTerminalListener l) {
      terminalManager.removeListener(l);
   }
}
