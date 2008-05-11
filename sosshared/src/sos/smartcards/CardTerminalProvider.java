package sos.smartcards;

import java.security.Provider;

public class CardTerminalProvider extends Provider {

	protected CardTerminalProvider() {
		super("CardTerminalProvider", 0.1d, "SoS Card Terminal Provider");
		put("TerminalFactory.CREF", "sos.smartcards.CREFEmulatorTerminalFactorySpi");
	}
}
