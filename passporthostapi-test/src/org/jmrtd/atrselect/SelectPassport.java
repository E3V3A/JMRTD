package org.jmrtd.atrselect;

import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

public class SelectPassport {

	static final byte[] AID = { (byte) 0xA0, 0x00, 0x00, 0x02,
                        0x47, 0x10, 0x01};

	public SelectPassport() {
		loggers = new Vector<PrintStream>();
		loggers.add(System.out);
	}

	private CardChannel channel = null;
      private Card card = null;

	public void init() throws NoSuchAlgorithmException, CardException {
       TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null);
       CardTerminals terminals = tf.terminals();
       for (CardTerminal terminal : terminals.list(CardTerminals.State.CARD_PRESENT)) {
               // examine Card in terminal, return if it matches
               try {
                 card = terminal.connect("*");
                 card.beginExclusive();
                 ATR a = card.getATR();
                 log("ATR: "+byteArrayToString(a.getBytes())+", Historical: "+byteArrayToString(a.getHistoricalBytes()));
                 channel = card.getBasicChannel();
               }catch(CardException ce) {
                   System.out.println("Couldn't esatablish the connection.");
                   ce.printStackTrace();
               }
           }
	}

	public void deinit() throws CardException {
		card.endExclusive();
	}	
	

	public void testSelect() {
		byte[] result = null;

        try {
  	   for(int i=0;i<256;i++) {
               result = selectAID(AID, i);
               log("Selecting applet: "+byteArrayToString(AID)+"with Le: "+i);
               log("Result: "+byteArrayToString(result));
	   }
        }catch(CardException ce) {
        }

        }	

    public byte[] selectAID(byte[] aid, int le) throws CardException {
        CommandAPDU  cmd = new CommandAPDU(
                (byte)0, (byte)0xA4,
                (byte) 0x04, (byte) 0x00, aid, le);
        ResponseAPDU resp = channel.transmit(cmd);
//        System.out.println(byteArrayToString(cmd.getBytes()));
//        System.out.println(byteArrayToString(resp.getBytes()));
        if(resp.getSW() != SW_OK) {
            throw new CardException("Could not select applet "+(aid == null ? "NULL" : byteArrayToString(aid)));
        }
        return resp.getBytes();
    }

	
	public static final int SW_OK = 0x9000;

	public static void main(String[] args) {
		SelectPassport test= null;
		try {
			test = new SelectPassport();

            test.init();
            
				test.testSelect();
			test.deinit();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private Vector<PrintStream> loggers;

	public void log(Object o) {
        for(PrintStream p : loggers) {
            p.println(o.toString());
        }
	}

	   public void flushLog() {
            for(PrintStream p : loggers) {
                p.flush();
            }
	   }

	   public void addLogger(String fn) throws IOException {
	     loggers.add(new PrintStream(fn));
	   }

	   /**
	      Returns a string representation of a byte array
	   */
	   public static String byteArrayToString(byte[] a) {
	      String result = "";
	      String onebyte = null;
	      for(int i=0; i< a.length; i++) {
	        onebyte = Integer.toHexString(a[i]);
	        if(onebyte.length() == 1)
	          onebyte = "0" + onebyte;
	        else
	          onebyte = onebyte.substring(onebyte.length()-2);
	        result = result + onebyte.toUpperCase() + " ";
	      }
	      return result;
	   }

	   
}
