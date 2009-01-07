package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.Action;
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
	private Collection<CardTerminal> terminalsToStart, terminalsToStop;
	private Map<CardTerminal, JCheckBox> checkBoxMap;

	public PreferencesPanel(CardManager cm) {
		super(new BorderLayout());
		this.cm = cm;
		terminalsToStart = new HashSet<CardTerminal>();
		terminalsToStop = new HashSet<CardTerminal>();
		checkBoxMap = new HashMap<CardTerminal, JCheckBox>();
		List<CardTerminal> terminalList = cm.getTerminals();
		
		JPanel cmPanel = new JPanel(new GridLayout(terminalList.size(), 2));
		cmPanel.setBorder(CARD_MANAGER_BORDER);
		for (CardTerminal terminal: terminalList){
			JCheckBox checkBox = new JCheckBox(terminal.getName(), cm.isPolling(terminal));
			checkBoxMap.put(terminal, checkBox);
			checkBox.setAction(getSetTerminalAction(terminal, checkBox));
			cmPanel.add(checkBox);
		}
		add(cmPanel, BorderLayout.CENTER);
	}

	public String getName() {
		return "Preferences";
	}

	public void commit() {
		for (CardTerminal terminal: terminalsToStart) { cm.startPolling(terminal); }
		for (CardTerminal terminal: terminalsToStop) { cm.stopPolling(terminal); }
		terminalsToStart.clear();
		terminalsToStop.clear();
	}

	public void abort() {
		terminalsToStart.clear();
		terminalsToStop.clear();
		for (CardTerminal terminal: checkBoxMap.keySet()) {
			JCheckBox checkBox = checkBoxMap.get(terminal);
			checkBox.setSelected(cm.isPolling(terminal));
		}
	}

	public Action getSetTerminalAction(final CardTerminal terminal, final JCheckBox checkBox) {
		final Component parent = this;
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (checkBox.isSelected()) {
					terminalsToStart.add(terminal);
				} else {
					terminalsToStop.add(terminal);
				}
			}
		};
		action.putValue(Action.SHORT_DESCRIPTION, "Change polling behavior for " + terminal.getName());
		action.putValue(Action.NAME, terminal.getName());
		return action;
	}
}
