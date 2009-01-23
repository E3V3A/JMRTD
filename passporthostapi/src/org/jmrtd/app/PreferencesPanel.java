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
 * $Id: $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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

import sos.smartcards.CardManager;


/**
 * Preferences panel.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class PreferencesPanel extends JPanel
{
	private enum ReadingMode {
		SAFE_MODE, // completely read files, check their signature, then display only if valid
		PROGRESSIVE_MODE; // display files while still reading, then check their signature
		
		public String toString() {
			String s = super.toString();
			s = s.replace('_', ' ');
			s = s.substring(0, 1) + s.substring(1).toLowerCase();
			return s;
		}
	};
	
	private static final long serialVersionUID = 5429621553165149988L;

	private static Border READING_MODE_BORDER = BorderFactory.createTitledBorder("Reading Mode");
	private static Border CARD_TERMINALS_BORDER = BorderFactory.createTitledBorder("Card Terminals");


	private ReadingMode readingMode;
	private CardManager cm;
	private Collection<CardTerminal> terminalsToStart, terminalsToStop;
	private Map<CardTerminal, JCheckBox> checkBoxMap;

	/**
	 * Creates the preferences panel.
	 *
	 * @param cm the card manager
	 * @param preferencesFile file for storing preferences
	 */
	public PreferencesPanel(CardManager cm, File preferencesFile) {
		super(new FlowLayout());
		this.cm = cm;
		terminalsToStart = new HashSet<CardTerminal>();
		terminalsToStop = new HashSet<CardTerminal>();
		checkBoxMap = new HashMap<CardTerminal, JCheckBox>();
		List<CardTerminal> terminalList = cm.getTerminals();
		
		JPanel cmPanel = new JPanel(new GridLayout(terminalList.size(), 1));
		cmPanel.setBorder(CARD_TERMINALS_BORDER);
		if (terminalList.size() == 0) {
		   cmPanel.add(new JLabel("No card terminals!"));
		}
		for (CardTerminal terminal: terminalList){
			JCheckBox checkBox = new JCheckBox(terminal.getName(), cm.isPolling(terminal));
			checkBoxMap.put(terminal, checkBox);
			checkBox.setAction(getSetTerminalAction(terminal, checkBox));
			cmPanel.add(checkBox);
		}

		readingMode = ReadingMode.PROGRESSIVE_MODE;
		ReadingMode[] modes = ReadingMode.values();
		JPanel rmPanel = new JPanel(new GridLayout(modes.length, 1));
		rmPanel.setBorder(READING_MODE_BORDER);
		ButtonGroup buttonGroup = new ButtonGroup();
		for (ReadingMode mode: modes) {
			JRadioButton radioButton = new JRadioButton(mode.toString(), mode == readingMode);
			radioButton.setAction(getSetModeAction(mode));
			buttonGroup.add(radioButton);
			rmPanel.add(radioButton);
		}
		
		add(rmPanel);
		add(cmPanel);
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
	
	  public Action getSetModeAction(final ReadingMode mode) {
	      Action action = new AbstractAction() {
	         public void actionPerformed(ActionEvent e) {
	            readingMode = mode;
	         }
	      };
	      StringBuffer shortDescription = new StringBuffer();
	      shortDescription.append("Set reading mode to ");
	      shortDescription.append(mode.toString());
	      shortDescription.append(": ");
	      switch (mode) {
	         case SAFE_MODE: shortDescription.append("Completely read files, check their signature, then display only if valid."); break;
	         case PROGRESSIVE_MODE: shortDescription.append("Display files while still reading, then check their signature."); break;
	      }
	      action.putValue(Action.SHORT_DESCRIPTION, shortDescription.toString());
	      action.putValue(Action.NAME, mode.toString());
	      return action;
	   }
}
