/**
 * 
 */
package sos.passportapplet.evil;

import sos.passportapplet.PassportApplet;
import sos.passportapplet.PassportCrypto;

/**
 * @author ronny
 *
 */
public class EvilPassportApplet extends PassportApplet {
    EvilPassportApplet (byte mode) {
    	super (mode);
    }
    
    /**
     * Installs an instance of the applet.
     * 
     * @param buffer
     * @param offset
     * @param length
     * @see javacard.framework.Applet#install(byte[], byte, byte)
     */
    public static void install(byte[] buffer, short offset, byte length) {
        (new EvilPassportApplet(PassportCrypto.JCOP41_MODE)).register();
    }
}
