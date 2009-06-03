package org.jmrtd.test; 

import java.text.ParseException; 
import java.text.SimpleDateFormat; 
import java.util.Date; 

import javax.smartcardio.CardTerminal; 
import javax.smartcardio.CardTerminals; 
import javax.smartcardio.TerminalFactory; 

import org.jmrtd.PassportService; 
import net.sourceforge.scuba.smartcards.CardServiceException; 
import net.sourceforge.scuba.smartcards.TerminalCardService; 

/*********************************************************************/ 

import java.net.*; 
import java.io.*; 

/** ****************************************************************** */ 

/******************************************************************************* 
  * Adapter between TorXakis and Passport Communication with TorXakis via 
  * stream-mode socket Communication with Passport via Java Passport   
API Command 
  * line argument: <port number of Server socket in this process> 
   
******************************************************************************/ 

public class PPadapter 

{ 
/** service to talk to the passport */ 
private static PassportService service; 

/* Data format for dates */ 
private static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd"); 

/*************************************************************************** 
* 
* setup connection with the card reader 
*/ 
public static void setupCard() { 
try { 
TerminalFactory tf = TerminalFactory.getInstance("PC/SC", null); 
CardTerminals terminals = tf.terminals(); 
for (CardTerminal terminal : terminals 
.list(CardTerminals.State.CARD_PRESENT)) { 
service = new PassportService(new TerminalCardService(terminal)); 
if (service != null) { 
service.open(); 
break; 
} 
} 
if (service == null) { 
System.exit(-23); 
} 
} catch (Exception e) { 
e.printStackTrace(); 
} 
} 

/** 
* reset connection to the card reader 
* 
* @throws CardServiceException 
*             if card reader is not working 
*/ 
protected static void resetCard() throws CardServiceException { 
// This actually properly resets the card. 
if (service.isOpen()) { 
service.close(); 
} 
service.open(); 
} 

/*************************************************************************** 
* 
* Sample piece of code to try and do BAC, with hard coded MRZ 
* 
* @throws ParseException 
*             if dates are mistyped 
*/ 
public static boolean myDoBAC() throws ParseException { 
try { 
Date birthDate = SDF.parse("19560507"); 
Date expiryDate = SDF.parse("20100101"); 
String number = "PPNUMMER0"; 
service.doBAC(number, birthDate, expiryDate); 
return true; // BAC succeeded 
} catch (CardServiceException e) { 
return false; 
} // BAC failed 
} 

/*************************************************************************** 
* 
* Try selecting a file, with or without Secure Messaging 
*/ 
public static boolean myCanSelectFile(short fid, boolean useSM) { 
try { 
if (useSM) { 
service.sendSelectFile(service.getWrapper(), fid); 
} else { 
service.sendSelectFile(null, fid); 
} 
return true; 
} catch (CardServiceException e) { 
return false; 
} 
} 

/** ****************************************************************** */ 

public static void main(String[] args) throws Exception 

{ 
if (args.length != 1) { 
System.out.println("own port number required"); 
} else { 
try { 
int portNo = Integer.parseInt(args[0]); 

// instantiate a socket for accepting a connection 
ServerSocket servsock = new ServerSocket(portNo); 

// wait to accept a connecion request 
// then a data socket is created 
Socket sock = servsock.accept(); 

// get an input stream for reading from the data socket 
InputStream inStream = sock.getInputStream(); 
// create a BufferedReader object for text line input 
BufferedReader sockin = new BufferedReader( 
new InputStreamReader(inStream)); 

// get an output stream for writing to the data socket 
OutputStream outStream = sock.getOutputStream(); 
// create a PrinterWriter object for character-mode output 
PrintWriter sockout = new PrintWriter(new OutputStreamWriter( 
outStream)); 

// initialize cardreader 
setupCard(); 
resetCard(); 

while (true) { // read a line from the data stream 
String inAction = sockin.readLine(); 

if (inAction.equals("BAC_Call")) { 
boolean r = myDoBAC(); 
if (r) { 
sockout.println("BAC_OK"); 
} else { 
sockout.println("BAC_NOK"); 
} 
sockout.flush(); 
} 
} 
} catch (Exception e) { 
e.printStackTrace(); 
} 
} 
} 
} 

/** ****************************************************************** */ 

<>