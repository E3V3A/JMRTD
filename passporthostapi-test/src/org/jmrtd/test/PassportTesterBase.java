package org.jmrtd.test;

import java.security.Security;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import junit.framework.TestCase;
import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.*;

public abstract class PassportTesterBase extends TestCase implements APDUListener {

    static { Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); }
    
    protected PassportService service = null; 
    
    public PassportTesterBase(String name) {
        super(name);
        try {
            TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null);
            CardTerminals terminals = tf.terminals(); 
            for (CardTerminal terminal : terminals.list(CardTerminals.State.CARD_PRESENT)) {
                 service = new PassportService(new TerminalCardService(terminal));
                 service.addAPDUListener(this);
                 if(service != null) {
                     service.open();
                     break;
                 }
            }
            if(service == null) {
                fail("No card found.");
            }
        }catch(Exception e) {
            fail(e.getMessage());
        }
    }

    private static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd"); 
    
    protected Date getDate(String s) {
        try {
            return SDF.parse(s);
          }catch(ParseException pe) {
              return null;
          }        
    }

    protected void resetCard() throws CardServiceException {
        // This actually properly resets the card.
        if(service.isOpen()) {
           service.close();
        }
        service.open();
    }

    protected boolean traceApdu = false;
    
    public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
        if(traceApdu) {
            System.out.println("C: "+Hex.bytesToHexString(capdu.getBytes()));
            System.out.println("R: "+Hex.bytesToHexString(rapdu.getBytes()));
        }
        
    }
    
}
