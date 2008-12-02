package sos.smartcards;

import java.security.Provider;

/**
 * A provider for JCOP Emulator.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @version $Revision: $
 */
public class JCOPTerminalProvider extends Provider
{
	private static final long serialVersionUID = 6049577128262232444L;

	/**
	 * Constructs the provider.
	 */
	public JCOPTerminalProvider() {
		super("JCOPTerminalProvider", 0.2d, "JCOP Emulator Card Terminal Provider");
		put("TerminalFactory.JCOP", "sos.smartcards.JCOPEmulatorTerminalFactorySpi");
	}
}
