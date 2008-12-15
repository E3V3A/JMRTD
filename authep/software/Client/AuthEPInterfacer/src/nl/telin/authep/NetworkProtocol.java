package nl.telin.authep;


/**
 * A lightweigth protocol is used for communication between the interfacer
 * and the AuthEP Identity Provider. This class contains constants that are
 * being used.
 * The NetworkListener is constantly listening for incoming commands from the IdP.
 * The interfacer will only send responses back.
 * @author Dirk-jan.vanDijk
 *
 */
public class NetworkProtocol {

	/**
	 * Close the connection between the interfacer and IdP
	 */
	public static final int CLOSE = 0;
	/**
	 * Request a file from the interfacer. This command
	 * is followed by a singly byte indicating the id of the file to get.
	 */
	public static final int REQUEST_FILE = 1;
	/**
	 * Request the passport to sign a message proving it's an official passport
	 * and not a clone. This command is first followed by first a byte indicating
	 * the size of the message and second the actual message itself.
	 */
	public static final int SEND_CHALLENGE = 2;
	/**
	 * Send the BAC data to the interfacer. The BAC data consists of:<br>
	 * byte: size of document number<br>
	 * byte[]: document number string<br>
	 * byte: size of date of birth string<br>
	 * byte[]: date of birth string<br>
	 * byte: size of date of expiry string<br>
	 * byte[]: date of expiry string<br>
	 */
	public static final int SEND_BAC = 3;
}
