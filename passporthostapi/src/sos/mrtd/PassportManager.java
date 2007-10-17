package sos.mrtd;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.smartcards.CardEvent;
import sos.smartcards.CardTerminalListener;
import sos.smartcards.CardManager;

public class PassportManager
{
   private enum CardType { OTHER_CARD, PASSPORT };

   private static PassportManager passportManager = new PassportManager();

   private Map<CardService, CardType> cardTypes;
   private Collection<PassportListener> listeners;

   private PassportManager() {
      cardTypes = new Hashtable<CardService, CardType>();
      listeners = new ArrayList<PassportListener>();
      CardManager.addCardTerminalListener(new CardTerminalListener() {

         public void cardInserted(CardEvent ce) {
            CardService service = ce.getService();
            if (isPassportInserted(service)) {
               cardTypes.put(service, CardType.PASSPORT);
               for (PassportListener l : listeners) { l.passportRemoved(ce); };
            } else {
               cardTypes.put(service, CardType.OTHER_CARD);
            }
         }

         public void cardRemoved(CardEvent ce) {
            CardService service = ce.getService();
            CardType cardType = cardTypes.remove(service);
            if (cardType != null
                  && cardTypes.get(service) == CardType.PASSPORT) {
               for (PassportListener l : listeners) { l.passportRemoved(ce); };
            }
         }
      });
   }

   private boolean isPassportInserted(CardService service) {
      try {
         PassportApduService apduService = new PassportApduService(service);
         try {
            apduService.open(); /* Selects applet... */
            return true;
         } catch (CardServiceException cse) {
            return false;
         } finally {
            apduService.close();
         }
      } catch (GeneralSecurityException gse) {
         gse.printStackTrace();
         return false;
      }
   }

   private synchronized void addListener(PassportListener l) {
      listeners.add(l);
      notifyAll();
   }

   private synchronized void removeListener(PassportListener l) {
      listeners.remove(l);
   }

   public static void addPassportListener(PassportListener l) {
      passportManager.addListener(l);
   }

   public static void removePassportListener(PassportListener l) {
      passportManager.removeListener(l);
   }
}
