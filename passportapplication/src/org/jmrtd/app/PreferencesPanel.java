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
 * $Id: PreferencesPanel.java 893 2009-03-23 15:43:42Z martijno $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import net.sourceforge.scuba.smartcards.CardManager;

/**
 * Preferences panel.
 *
 * FIXME: Can probably be done more generically...
 *
 * TODO: Perhaps add a blanket reading mode, where user is asked to continue at own risk...
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
	private static final boolean DEFAULT_APDU_TRACING_SETTING = false;

	private static final String READING_MODE_KEY = "mode.reading";
	private static final String TERMINAL_KEY_PREFIX = "terminal.";
	private static final String APDU_TRACING_KEY = "trace.apdu";
	private static final String BAC_STORE_KEY = "bac.store.dir";
	private static final String CSCA_STORE_KEY ="csca.store.dir";
	private static final String CVCA_STORE_KEY ="cvca.store.dir";
	
	private static final long serialVersionUID = 5429621553165149988L;

	private PreferencesState changedState;
	private PreferencesState state;
	private CardManager cardManager;

	private Map<CardTerminal, JCheckBox> cardTerminalPollingCheckBoxMap;
	private Map<ReadingMode, JRadioButton> readingModeRadioButtonMap;
	private JCheckBox apduTracingCheckBox;
	private APDUTraceFrame apduTraceFrame;

	/**
	 * Creates the preferences panel.
	 *
	 * @param cm the card manager
	 * @param preferencesFile file for storing preferences
	 */
	public PreferencesPanel(CardManager cm, File preferencesFile) {
		super(new BorderLayout());
		this.cardManager = cm;
		cardTerminalPollingCheckBoxMap = new HashMap<CardTerminal, JCheckBox>();		

		JTabbedPane jtb = new JTabbedPane();
		jtb.addTab("Terminals", createTerminalsPreferencesTab(cm));
		jtb.addTab("Certificate Files", createCertificatesPanel());
		add(jtb, BorderLayout.CENTER);
		
		this.changedState = new PreferencesState(state);
	}

	private JComponent createTerminalsPreferencesTab(CardManager cm) {
		List<CardTerminal> terminalList = cm.getTerminals();

		JPanel cardTerminalsPreferencesPanel = new JPanel(new GridLayout(terminalList.size(), 1));
		cardTerminalsPreferencesPanel.setBorder(BorderFactory.createTitledBorder("Card Terminals"));
		if (terminalList.size() == 0) {
			cardTerminalsPreferencesPanel.add(new JLabel("No card terminals!"));
		}
		this.state = new PreferencesState();
		for (CardTerminal terminal: terminalList){
			boolean isChecked = cm.isPolling(terminal);
			JCheckBox checkBox = new JCheckBox(terminal.getName(), isChecked);
			cardTerminalPollingCheckBoxMap.put(terminal, checkBox);
			checkBox.setAction(getSetTerminalAction(terminal, checkBox));
			cardTerminalsPreferencesPanel.add(checkBox);
			state.setTerminalChecked(terminal, isChecked);
		}

		ReadingMode[] modes = ReadingMode.values();
		readingModeRadioButtonMap = new HashMap<ReadingMode, JRadioButton>();
		JPanel readingModePreferencesPanel = new JPanel(new GridLayout(modes.length, 1));
		readingModePreferencesPanel.setBorder(BorderFactory.createTitledBorder("Reading Mode"));
		ButtonGroup buttonGroup = new ButtonGroup();
		for (ReadingMode mode: modes) {
			JRadioButton radioButton = new JRadioButton(mode.toString(), mode == state.getReadingMode());
			radioButton.setAction(getSetModeAction(mode));
			readingModeRadioButtonMap.put(mode, radioButton);
			buttonGroup.add(radioButton);
			readingModePreferencesPanel.add(radioButton);
		}
		JPanel terminalSettingsTab = new JPanel(new BorderLayout());

		JPanel apduTracePreferencesPanel = new JPanel(new FlowLayout());
		apduTracingCheckBox = new JCheckBox("Trace APDUs", state.isAPDUTracing());

		apduTracingCheckBox.setAction(getSetAPDUTracingAction(apduTracingCheckBox));
		apduTracePreferencesPanel.add(apduTracingCheckBox);
		apduTracePreferencesPanel.setBorder(BorderFactory.createTitledBorder("APDU Tracing"));


		JPanel northPanel = new JPanel(new FlowLayout());
		northPanel.add(readingModePreferencesPanel);
		northPanel.add(apduTracePreferencesPanel);
		terminalSettingsTab.add(northPanel, BorderLayout.NORTH);
		terminalSettingsTab.add(cardTerminalsPreferencesPanel, BorderLayout.CENTER);
		return terminalSettingsTab;
	}

	private JComponent createCertificatesPanel() {
		final JPanel panel = new JPanel(new GridLayout(2,1));
		panel.setBorder(BorderFactory.createTitledBorder("Certificate and key stores"));
		final JComponent cvcaPanel = new FolderBrowser("CVCA store");
		final JComponent cscaPanel = new FolderBrowser("CSCA store");
		panel.add(cscaPanel);
		panel.add(cvcaPanel);
		return panel;
	}
		
	private static class FolderBrowser extends JPanel
	{
		private static final long serialVersionUID = -1280621620589365355L;

		public FolderBrowser(String title) {
			super(new FlowLayout());
			final Component c = this;
			setBorder(BorderFactory.createTitledBorder(title));
			final JTextField cvcaDirectoryField = new JTextField(30);
			final JButton browseCVCADirectoryButton = new JButton("...");
			add(cvcaDirectoryField);
			add(browseCVCADirectoryButton);
			browseCVCADirectoryButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setAcceptAllFileFilterUsed(false);
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fileChooser.setFileFilter(new FileFilter() {
						public boolean accept(File f) { return f.isDirectory(); }
						public String getDescription() { return "Directories"; }               
					});
					int choice = fileChooser.showOpenDialog(c);
					switch (choice) {
					case JFileChooser.APPROVE_OPTION:
						try {
							File file = fileChooser.getSelectedFile();
							cvcaDirectoryField.setText(file.getCanonicalPath());
						} catch (IOException ioe) {
							/* NOTE: Do nothing. */
							ioe.printStackTrace();
						}
						break;
					default: break;
					}
				}
			});
		}
	}

	public String getName() {
		return "Preferences";
	}

	public ReadingMode getReadingMode() {
		return state.getReadingMode();
	}

	public boolean isAPDUTracing() {
		return state.isAPDUTracing();
	}

	public void setAPDUTracing(boolean b) {
		state.setAPDUTracing(b);
	}

	public void commit() {
		PreferencesState oldState = new PreferencesState(state);
		state = new PreferencesState(changedState);
		updateCardManager(state, cardManager);
		if (!oldState.isAPDUTracing() && state.isAPDUTracing()) {
			apduTraceFrame = new APDUTraceFrame("APDU trace");
			apduTraceFrame.pack();
			apduTraceFrame.setVisible(true);
			cardManager.addAPDUListener(apduTraceFrame);
		} else if (oldState.isAPDUTracing() && !state.isAPDUTracing()) {
			apduTraceFrame.setVisible(false);
			cardManager.removeAPDUListener(apduTraceFrame);
			apduTraceFrame = null;
		}
	}

	public void abort() {
		changedState = new PreferencesState(state);
		updateGUIFromState(state);
	}

	private void updateGUIFromState(PreferencesState state) {
		for (CardTerminal terminal: cardTerminalPollingCheckBoxMap.keySet()) {
			JCheckBox checkBox = cardTerminalPollingCheckBoxMap.get(terminal);
			checkBox.setSelected(state.isTerminalChecked(terminal));
		}
		for (ReadingMode mode: readingModeRadioButtonMap.keySet()) {
			JRadioButton radioButton = readingModeRadioButtonMap.get(mode);
			radioButton.setSelected(mode.equals(state.getReadingMode()));
		}
		apduTracingCheckBox.setSelected(state.isAPDUTracing());
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
	
	public URL getBACStore() {
		return state.getBACStore();
	}
	
	public URL getCSCAStore() {
		return state.getCSCAStore();
	}
	
	public URL getCVCAStore() {
		return state.getCVCAStore();
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

	private Action getSetAPDUTracingAction(final JCheckBox checkBox) {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				changedState.setAPDUTracing(checkBox.isSelected());
			}
		};
		action.putValue(Action.SHORT_DESCRIPTION, "Toggle APDU tracing");
		action.putValue(Action.NAME, "Trace APDUs");
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

		public boolean isAPDUTracing() {
			Boolean isAPDUTracing = (Boolean)properties.get(APDU_TRACING_KEY);
			if (isAPDUTracing == null) {
				return DEFAULT_APDU_TRACING_SETTING;
			}
			return isAPDUTracing;
		}

		public void setAPDUTracing(boolean b) {
			properties.put(APDU_TRACING_KEY, b);
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

		public URL getBACStore() {
			return (URL)properties.get(BAC_STORE_KEY);
		}
		
		public void setBACStore(URL url) {
			properties.put(CSCA_STORE_KEY, url);
		}
		
		public URL getCSCAStore() {
			return (URL)properties.get(CSCA_STORE_KEY);
		}
		
		public void setCSCAStore(URL url) {
			properties.put(CSCA_STORE_KEY, url);
		}
		
		public URL getCVCAStore() {
			return (URL)properties.get(CVCA_STORE_KEY);
		}
		
		public void setCVCAStore(URL url) {
			properties.put(CVCA_STORE_KEY, url);
		}
	}
}
