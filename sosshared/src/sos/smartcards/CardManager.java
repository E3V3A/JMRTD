package sos.smartcards;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

/**
 * Manages all card terminals.
 * This is the source of card insertion and removal events.
 * Ideally this should be the only place where low level CardService
 * instances are created.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class CardManager
{
   private static final CardManager INSTANCE = new CardManager();
   private static final int POLL_INTERVAL = 200;

   private Map<CardTerminal, CardService> terminalServices;
   private Collection<CardTerminal> terminals;
   private Collection<CardTerminalListener> listeners;

   private CardManager() {
      listeners = new ArrayList<CardTerminalListener>();
      terminals = new HashSet<CardTerminal>();
      terminalServices = new Hashtable<CardTerminal, CardService>();
      addEmulator("localhost", 9025);
      (new Thread(new Runnable() {
         public void run() {
            try {
               while (true) {
                  poll();
                  Thread.sleep(POLL_INTERVAL);
               }
            } catch (InterruptedException ie) {
               // NOTE: interrupted during blocked state, so quit running
            }
         }
      })).start();
   }



   private synchronized void poll() throws InterruptedException {
      while (hasNoListeners()) {
         wait();
      }
      try {
         TerminalFactory terminalFactory = TerminalFactory.getDefault();
         /* TODO: check terminals are disconnected. */
         terminals.addAll(terminalFactory.terminals().list());
         for (CardTerminal terminal: terminals) {
            boolean wasCardPresent = false;
            boolean isCardPresent = false;
            CardService service = null;
            if (terminalServices.containsKey(terminal)) {
               service = terminalServices.get(terminal);
               wasCardPresent = true;
            }
            if (service == null) {
               try {
                  if (terminal instanceof CREFEmulator) {
                     String host = ((CREFEmulator)terminal).getHost();
                     int port = ((CREFEmulator)terminal).getPort();
                     service = new CREFEmulatorService(host, port);
                  } else {
                     service = new PCSCCardService();
                     ((PCSCCardService)service).open(terminal);                        
                  }
               } catch (Exception e) {
                  if (service != null) { service.close(); }
               }
            }
            isCardPresent = terminal.isCardPresent();

            if (wasCardPresent && !isCardPresent) {
               if (service != null) {
                  notifyCardRemoved(service);
                  service.close();
               }
               terminalServices.remove(terminal);
            } else if (!wasCardPresent && isCardPresent) {
               if (service != null) {
                  terminalServices.put(terminal, service);
                  notifyCardInserted(service);
               }
            }
         }
      } catch (CardException ce) {
         // NOTE: remain in same state?!?
      }
   }

   public Iterator<String> getTerminals() {
      Collection<String> result = new ArrayList<String>();
      for (CardTerminal terminal: terminals) {
         result.add(terminal.getName());
      }
      return result.iterator();
   }

   private class CREFEmulator extends CardTerminal
   {
      private String host;
      private int port;

      public CREFEmulator(String host, int port) {
         this.host = host;
         this.port = port;
      }

      public Card connect(String arg0) throws CardException {
         return null; // TODO
      }

      public String getName() {
         return "CREF emulator at " + host + ":" + port;
      }

      public String getHost() {
         return host;
      }

      public int getPort() {
         return port;
      }

      /**
       * Checks whether CREF is present by connecting to host:port.
       * 
       * NOTE: Only works if service already running!
       * Otherwise CREF gets confused.
       */
      public boolean isCardPresent() throws CardException {
         try {
            Socket sock = new Socket(host, port);
            sock.close();
         } catch (Exception e) {
            return false;
         }
         return true;            
      }

      public boolean waitForCardAbsent(long arg0) throws CardException {
         return false; // TODO
      }

      public boolean waitForCardPresent(long arg0) throws CardException {
         return false; // TODO
      }
   }

   private synchronized void addListener(CardTerminalListener l) {
      listeners.add(l);
      notifyAll();
   }

   private synchronized void removeListener(CardTerminalListener l) {
      listeners.remove(l);
   }

   private void notifyCardInserted(CardService service) {
      for (CardTerminalListener l: listeners) {
         l.cardInserted(new CardEvent(CardEvent.INSERTED, service));
      }
   }

   private void notifyCardRemoved(CardService service) {
      for (CardTerminalListener l: listeners) {
         l.cardRemoved(new CardEvent(CardEvent.REMOVED, service));
      }
   }

   private boolean hasNoListeners() {
      return listeners.isEmpty();
   }

   public static void addCardTerminalListener(CardTerminalListener l) {
      INSTANCE.addListener(l);
   }

   public static void removeCardTerminalListener(CardTerminalListener l) {
      INSTANCE.removeListener(l);
   }
   
   /**
    * Starts monitoring host:port for presence of CREF emulator.
    * 
    * @param host
    * @param port
    */
   private synchronized void addEmulator(String host, int port) {
      terminals.add(new CREFEmulator(host, port));
   }
   
   public static void addCREFEmulator(String host, int port) {
      INSTANCE.addEmulator(host, port);
   }
}
