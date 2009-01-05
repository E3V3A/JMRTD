package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.smartcardio.CardTerminal;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.Border;

import sos.smartcards.CardManager;

public class PreferencesPanel extends JPanel
{
	private static final long serialVersionUID = 5429621553165149988L;

	private static Border CARD_MANAGER_BORDER = BorderFactory.createTitledBorder("Card Manager");

	private CardManager cm;
	
	public PreferencesPanel(CardManager cm) {
		super(new BorderLayout());
		this.cm = cm;
		List<CardTerminal> terminalList = cm.getTerminals();
		JPanel cmPanel = new JPanel(new GridLayout(terminalList.size(), 2));
		cmPanel.setBorder(CARD_MANAGER_BORDER);
		for (CardTerminal terminal: terminalList){
			JCheckBox checkBox = new JCheckBox(terminal.getName(), cm.isPolling(terminal));
			cmPanel.add(checkBox);
		}
		add(cmPanel, BorderLayout.CENTER);
	}

	public String getName() {
		return "Preferences";
	}

	public void commit() {
	}

	public void abort() {
	}
}
