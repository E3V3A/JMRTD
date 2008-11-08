package sos.smartcards;

import java.security.Provider;

public class CardTerminalProvider extends Provider
{
	private static final long serialVersionUID = 6049577128262232444L;

	public CardTerminalProvider() {
		super("CardTerminalProvider", 0.1d, "SoS Card Terminal Provider");
		put("TerminalFactory.CREF", "sos.smartcards.CREFEmulatorTerminalFactorySpi");
		put("TerminalFactory.JCOP", "sos.smartcards.JCOPEmulatorTerminalFactorySpi");
	}
}
