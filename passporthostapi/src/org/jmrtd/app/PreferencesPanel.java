/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id$
 */

package org.jmrtd.app;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import net.sourceforge.scuba.smartcards.CardManager;

/**
 * Preferences panel.
 *
 * FIXME: Can probably be done more generically...
 * TODO: Perhaps a blanket mode, where reader can continue at own risk...
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class PreferencesPanel extends JPanel
{	
	public enum ReadingMode {
		SAFE_MODE, // completely read files, check their signature, then display only if valid
		PROGRESSIVE_MODE; // display files while still reading, then check their signature
	};
	
	private static final ReadingMode DEFAULT_READING_MODE = ReadingMode.SAFE_MODE;
	
	private static final String READING_MODE_KEY = "mode.reading";
	private static final String TERMINAL_KEY_PREFIX = "terminal.";

	private static final long serialVersionUID = 5429621553165149988L;

	private static Border READING_MODE_BORDER = BorderFactory.createTitledBorder("Reading Mode");
	private static Border CARD_TERMINALS_BORDER = BorderFactory.createTitledBorder("Card Terminals");

	private PreferencesState changedState;
	private PreferencesState state;
	private CardManager cm;
	private Map<CardTerminal, JCheckBox> checkBoxMap;
	private Map<ReadingMode, JRadioButton> radioButtonMap;

	/**
	 * Creates the preferences panel.
	 *
	 * @param cm the card manager
	 * @param preferencesFile file for storing preferences
	 */
	public PreferencesPanel(CardManager cm, File preferencesFile) {
		super(new FlowLayout());
		this.cm = cm;
		checkBoxMap = new HashMap<CardTerminal, JCheckBox>();
		List<CardTerminal> terminalList = cm.getTerminals();

		JPanel cmPanel = new JPanel(new GridLayout(terminalList.size(), 1));
		cmPanel.setBorder(CARD_TERMINALS_BORDER);
		if (terminalList.size() == 0) {
			cmPanel.add(new JLabel("No card terminals!"));
		}
		this.state = new PreferencesState();
		for (CardTerminal terminal: terminalList){
			boolean isChecked = cm.isPolling(terminal);
			JCheckBox checkBox = new JCheckBox(terminal.getName(), isChecked);
			checkBoxMap.put(terminal, checkBox);
			checkBox.setAction(getSetTerminalAction(terminal, checkBox));
			cmPanel.add(checkBox);
			state.setTerminalChecked(terminal, isChecked);
		}

		ReadingMode[] modes = ReadingMode.values();
		radioButtonMap = new HashMap<ReadingMode, JRadioButton>();
		JPanel rmPanel = new JPanel(new GridLayout(modes.length, 1));
		rmPanel.setBorder(READING_MODE_BORDER);
		ButtonGroup buttonGroup = new ButtonGroup();
		for (ReadingMode mode: modes) {
			JRadioButton radioButton = new JRadioButton(mode.toString(), mode == state.getReadingMode());
			radioButton.setAction(getSetModeAction(mode));
			radioButtonMap.put(mode, radioButton);
			buttonGroup.add(radioButton);
			rmPanel.add(radioButton);
		}
		this.changedState = new PreferencesState(state);

		add(rmPanel);
		add(cmPanel);
	}

	public String getName() {
		return "Preferences";
	}

	public ReadingMode getReadingMode() {
		return state.getReadingMode();
	}

	public void commit() {
		state = new PreferencesState(changedState);
		updateCardManager(state, cm);
	}

	public void abort() {
		changedState = new PreferencesState(state);
		updateGUIFromState(state);
	}
	
	private void updateGUIFromState(PreferencesState state) {
		for (CardTerminal terminal: checkBoxMap.keySet()) {
			JCheckBox checkBox = checkBoxMap.get(terminal);
			checkBox.setSelected(state.isTerminalChecked(terminal));
		}
		for (ReadingMode mode: radioButtonMap.keySet()) {
			JRadioButton radioButton = radioButtonMap.get(mode);
			radioButton.setSelected(mode.equals(state.getReadingMode()));
		}
	}
	
	private void updateCardManager(PreferencesState state, CardManager cm) {
		List<CardTerminal> terminals = cm.getTerminals();
		for (CardTerminal terminal: terminals) {
			if (state.isTerminalChecked(terminal)) {
				if (!cm.isPolling(terminal)) { cm.startPolling(terminal); }
			} else {
				if (cm.isPolling(terminal)) { cm.stopPolling(terminal); }
			}
		}
	}

	public Action getSetTerminalAction(final CardTerminal terminal, final JCheckBox checkBox) {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				changedState.setTerminalChecked(terminal, checkBox.isSelected());
			}
		};
		action.putValue(Action.SHORT_DESCRIPTION, "Change polling behavior for " + terminal.getName());
		action.putValue(Action.NAME, terminal.getName());
		return action;
	}

	public Action getSetModeAction(final ReadingMode mode) {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				changedState.setReadingMode(mode);
			}
		};
		String modeString = mode.toString();
		modeString = modeString.replace('_', ' ');
		modeString = modeString.substring(0, 1) + modeString.substring(1).toLowerCase();
		StringBuffer shortDescription = new StringBuffer();
		shortDescription.append("Set reading mode to ");
		shortDescription.append(modeString);
		shortDescription.append(": ");
		switch (mode) {
		case SAFE_MODE: shortDescription.append("Completely read files, check their signature, then display only if valid."); break;
		case PROGRESSIVE_MODE: shortDescription.append("Display files while still reading, then check their signature."); break;
		}
		action.putValue(Action.SHORT_DESCRIPTION, shortDescription.toString());
		action.putValue(Action.NAME, modeString);
		return action;
	}
	
	private class PreferencesState implements Cloneable
	{	
		private Properties properties;
		
		private PreferencesState(Properties properties) {
			this.properties = (Properties)properties.clone();
		}
		
		public PreferencesState() {
			this(new Properties());
		}
		
		public PreferencesState(PreferencesState otherState) {
			this(otherState.properties);
		}

		public String toString() {
			return properties.toString();
		}
		
		public boolean isTerminalChecked(CardTerminal terminal) {
			Boolean result = (Boolean)properties.get(createTerminalKey(terminal.getName()));
			return result != null && result;
		}
		
		public void setTerminalChecked(CardTerminal terminal, boolean b) {
			setTerminalChecked(terminal.getName(), b);
		}
		
		public ReadingMode getReadingMode() {
			ReadingMode readingMode = (ReadingMode)properties.get(READING_MODE_KEY);
			if (readingMode == null) { return DEFAULT_READING_MODE; }
			return readingMode;
		}
		
		public void setReadingMode(ReadingMode readingMode) {
			properties.put(READING_MODE_KEY, readingMode);
			
		}
		
		public Object clone() {
			return new PreferencesState(properties);
		}

		private void setTerminalChecked(String terminalName, boolean b) {
			properties.put(createTerminalKey(terminalName), b);
		}

		private String createTerminalKey(String terminalName) {
			return TERMINAL_KEY_PREFIX + terminalName.trim();
		}
	}
}
