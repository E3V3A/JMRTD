package sos.mrtd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import sos.smartcards.CardEvent;
import sos.smartcards.CardManager;
import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.smartcards.CardTerminalListener;

public class PassportManager
{
   private enum CardType { OTHER_CARD, PASSPORT };

   private static final PassportManager INSTANCE = new PassportManager();

   private Map<CardService, CardType> cardTypes;
   private Map<CardService, PassportService> passportServices;
   private Collection<PassportListener> listeners;

   private PassportManager() {
      cardTypes = new Hashtable<CardService, CardType>();
      passportServices = new Hashtable<CardService, PassportService>();
      listeners = new ArrayList<PassportListener>();
      CardManager.addCardTerminalListener(new CardTerminalListener() {

         public void cardInserted(CardEvent ce) {
            CardService service = ce.getService();
            
            try {
               PassportService passportService = new PassportService(service);
               passportService.open(); /* Selects applet... */
               cardTypes.put(service, CardType.PASSPORT);
               passportServices.put(service, passportService);
               PassportEvent pe = new PassportEvent(PassportEvent.INSERTED, passportService);
               for (PassportListener l : listeners) { l.passportInserted(pe); };
            } catch (CardServiceException cse) {
               cardTypes.put(service, CardType.OTHER_CARD);
            }
         }

         public void cardRemoved(CardEvent ce) {
            CardService service = ce.getService();
            CardType cardType = cardTypes.remove(service);
            if (cardType != null && cardType == CardType.PASSPORT) {
               PassportService passportService = passportServices.get(service);
               PassportEvent pe = new PassportEvent(PassportEvent.REMOVED, passportService);
               for (PassportListener l : listeners) {  l.passportRemoved(pe); };
            }
         }
      });
   }

   private synchronized void addListener(PassportListener l) {
      listeners.add(l);
   }

   private synchronized void removeListener(PassportListener l) {
      listeners.remove(l);
   }

   public static void addPassportListener(PassportListener l) {
      INSTANCE.addListener(l);
   }

   public static void removePassportListener(PassportListener l) {
      INSTANCE.removeListener(l);
   }
}
