package sos.smartcards;

import java.security.Provider;

/**
 * A provider for CREF Emulator.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @version $Revision: $
 */
public class CREFTerminalProvider extends Provider
{
	private static final long serialVersionUID = 6049577128262232444L;

	/**
	 * Constructs the provider.
	 */
	public CREFTerminalProvider() {
		super("CREFTerminalProvider", 0.1d, "CREF Emulator Card Terminal Provider");
		put("TerminalFactory.CREF", "sos.smartcards.CREFEmulatorTerminalFactorySpi");
	}
}
