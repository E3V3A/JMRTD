/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.scuba.swing.URIListEditor;

/**
 * Preferences panel.
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

	private static final long serialVersionUID = 5429621553165149988L;

	private PreferencesState state;
	private PreferencesState changedState;

	private Preferences preferences;
	private boolean existsBackingStore;

	private Map<CardTerminal, JCheckBox> cardTerminalPollingCheckBoxMap;
	private Map<ReadingMode, JRadioButton> readingModeRadioButtonMap;
	private JCheckBox apduTracingCheckBox;

	private URIListEditor cscaLocationsDisplay, cvcaLocationsDisplay;

	private Collection<ChangeListener> changeListeners;

	/**
	 * Creates the preferences panel.
	 * 
	 * @param cm the terminal map
	 * @param applicationClass the class used as an identifier for the preferences
	 */
	public PreferencesPanel(Map<CardTerminal, Boolean> cm, Class<?> applicationClass) {
		super(new BorderLayout());
		this.changeListeners = new ArrayList<ChangeListener>();
		this.preferences = Preferences.userNodeForPackage(applicationClass);
		try {
			existsBackingStore = false;
			existsBackingStore = (preferences.keys() != null && preferences.keys().length > 0);
		} catch (BackingStoreException bse) {
			/* NOTE it probably doesn't exist */
		}
		cardTerminalPollingCheckBoxMap = new HashMap<CardTerminal, JCheckBox>();		
		this.state = new PreferencesState();
		update();
		JTabbedPane jtb = new JTabbedPane();
		jtb.addTab("Terminals", createTerminalsPreferencesTab(cm));
		jtb.addTab("Certificates", createCertificatesPanel());
		add(jtb, BorderLayout.CENTER);

		this.changedState = new PreferencesState(state);
		updateGUIFromState(state);
	}

	private JComponent createTerminalsPreferencesTab(Map<CardTerminal, Boolean> terminalList) {
		JPanel cardTerminalsPreferencesPanel = new JPanel(new GridLayout(terminalList.size(), 1));
		cardTerminalsPreferencesPanel.setBorder(BorderFactory.createTitledBorder("Card Terminals"));
		if (terminalList.size() == 0) {
			cardTerminalsPreferencesPanel.add(new JLabel("No card terminals!"));
		}
		for (Entry<CardTerminal, Boolean> entry: terminalList.entrySet()) {
			CardTerminal terminal = entry.getKey();
			Boolean isCheckedValue = entry.getValue();
			boolean isChecked = isCheckedValue == null ? false : isCheckedValue; 
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
		terminalSettingsTab.add(cardTerminalsPreferencesPanel, BorderLayout.SOUTH);
		return terminalSettingsTab;
	}

	private JComponent createCertificatesPanel() {
		final JPanel panel = new JPanel(new GridLayout(2,1));
		cscaLocationsDisplay = new URIListEditor("Document validation", state.getCSCAStoreLocations());
		cscaLocationsDisplay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<URI> uriList = cscaLocationsDisplay.getURIList();
				changedState.setCSCAStoreLocations(uriList);
			}
		});

		panel.add(cscaLocationsDisplay);

		cvcaLocationsDisplay = new URIListEditor("Access to biometrics", state.getCVCAStoreLocations());
		cvcaLocationsDisplay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<URI> uriList = cvcaLocationsDisplay.getURIList();
				changedState.setCVCAStoreLocations(uriList);
			}
		});

		panel.add(cvcaLocationsDisplay);

		return panel;
	}

	public String getName() {
		return "Preferences";
	}

	public ReadingMode getReadingMode() {
		return state.getReadingMode();
	}

	public boolean isTerminalChecked(CardTerminal terminal) {
		return state.isTerminalChecked(terminal);
	}

	public boolean isAPDUTracing() {
		return state.isAPDUTracing();
	}

	public void setAPDUTracing(boolean b) {
		state.setAPDUTracing(b);
	}

	public void commit() {
		//		PreferencesState oldState = new PreferencesState(state);
		state = new PreferencesState(changedState);
		state.store(preferences);
		ChangeEvent ce = new ChangeEvent(this);
		notifyChangeListeners(ce);
	}

	public void update() {
		state.load(preferences);
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

		cscaLocationsDisplay.setURIList(state.getCSCAStoreLocations());
	}

	public Action getSetTerminalAction(final CardTerminal terminal, final JCheckBox checkBox) {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 8704502832790166859L;

			public void actionPerformed(ActionEvent e) {
				changedState.setTerminalChecked(terminal, checkBox.isSelected());
			}
		};
		action.putValue(Action.SHORT_DESCRIPTION, "Change polling behavior for " + terminal.getName());
		action.putValue(Action.NAME, terminal.getName());
		return action;
	}

	/**
	 * Returns <code>true</code> if the preferences backing store did
	 * not exist during construction of this preferences panel.
	 *
	 * @return the state of existance of the preferences' backing store
	 *         during construction of this preferences panel
	 */
	public boolean isBackingStoreNew() {
		return !existsBackingStore;
	}

	public URL getBACStoreLocation() {
		return state.getBACStoreLocation();
	}

	public List<URI> getCSCAStoreLocations() {
		return state.getCSCAStoreLocations();
	}

	public List<URI> getCVCAStoreLocations() {
		return state.getCVCAStoreLocations();
	}

	public void addCSCAStoreLocation(URI uri) {
		changedState.addCSCAStoreLocation(uri);
		updateGUIFromState(changedState);
		commit();
	}

	private Action getSetModeAction(final ReadingMode mode) {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -7253305323528439137L;

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

			private static final long serialVersionUID = -585154276383750505L;

			public void actionPerformed(ActionEvent e) {
				changedState.setAPDUTracing(checkBox.isSelected());
			}
		};
		action.putValue(Action.SHORT_DESCRIPTION, "Toggle APDU tracing");
		action.putValue(Action.NAME, "Trace APDUs");
		return action;
	}

	public void addChangeListener(ChangeListener l) {
		changeListeners.add(l);	
	}

	private void notifyChangeListeners(ChangeEvent ce) {
		for (ChangeListener l: changeListeners) { l.stateChanged(ce); }
	}

	/**
	 * The state that needs to be saved to or restored from file. The panel
	 * collects user changes in one instance while maintaining the original
	 * settings in another instance.
	 */
	private class PreferencesState implements Cloneable, Serializable
	{	
		private static final long serialVersionUID = -256804944538594379L;

		private Properties properties;

		public PreferencesState() {
			this(new Properties());
		}

		public PreferencesState(Properties properties) {
			this.properties = (Properties)properties.clone();
		}

		public PreferencesState(PreferencesState otherState) {
			this(otherState.properties);
		}

		public String toString() {
			return properties.toString();
		}

		public boolean isTerminalChecked(CardTerminal terminal) {
			Object obj = properties.get(createTerminalKey(terminal.getName()));
			if (obj == null) { return false; }
			String value = ((String)obj).trim();
			if ("".equals(value)) { return false; }
			Boolean result = Boolean.parseBoolean(value);
			return result;
		}

		public void setTerminalChecked(CardTerminal terminal, boolean b) {
			setTerminalChecked(terminal.getName(), b);
		}

		public ReadingMode getReadingMode() {
			Object obj = properties.get(JMRTDApp.READING_MODE_KEY);
			if (obj == null) {
				properties.put(JMRTDApp.READING_MODE_KEY, DEFAULT_READING_MODE.toString());
				return DEFAULT_READING_MODE;
			}
			ReadingMode readingMode = ReadingMode.valueOf(properties.get(JMRTDApp.READING_MODE_KEY).toString());
			return readingMode;
		}

		public void setReadingMode(ReadingMode readingMode) {
			if (readingMode == null) { return; }
			properties.put(JMRTDApp.READING_MODE_KEY, readingMode.toString());
		}

		public boolean isAPDUTracing() {
			Object obj = properties.get(JMRTDApp.APDU_TRACING_KEY);
			if (obj == null) {
				properties.put(JMRTDApp.APDU_TRACING_KEY, Boolean.toString(DEFAULT_APDU_TRACING_SETTING));
				return DEFAULT_APDU_TRACING_SETTING;
			}
			String value = ((String)obj).trim();
			if ("".equals(value)) { return false; }
			boolean isAPDUTracing = Boolean.parseBoolean(value);
			return isAPDUTracing;
		}

		public void setAPDUTracing(boolean b) {
			properties.put(JMRTDApp.APDU_TRACING_KEY, Boolean.toString(b));
		}

		public Object clone() {
			return new PreferencesState(properties);
		}

		private void setTerminalChecked(String terminalName, boolean b) {
			properties.put(createTerminalKey(terminalName), Boolean.toString(b));
		}

		private String createTerminalKey(String terminalName) {
			return JMRTDApp.TERMINAL_KEY_PREFIX + terminalName.trim();
		}

		public URL getBACStoreLocation() {
			return (URL)properties.get(JMRTDApp.BAC_STORE_KEY);
		}

		public void setBACStore(URL url) {
			if (url == null) { return; }
			properties.put(JMRTDApp.CSCA_STORE_KEY, url.toString());
		}

		public List<URI> getCSCAStoreLocations() {
			return parseURIList((String)properties.get(JMRTDApp.CSCA_STORE_KEY));
		}

		public List<URI> getCVCAStoreLocations() {
			return parseURIList((String)properties.get(JMRTDApp.CVCA_STORE_KEY));
		}

		public void addCSCAStoreLocation(URI uri) {
			List<URI> locations = getCSCAStoreLocations();
			if (!locations.contains(uri)) {
				locations.add(uri);
				setCSCAStoreLocations(locations);
			}
		}

		public void setCSCAStoreLocations(List<URI> uris) {
			if (uris == null) { return; }
			properties.put(JMRTDApp.CSCA_STORE_KEY, uris.toString());
		}

		public void setCVCAStoreLocations(List<URI> uris) {
			if (uris == null) { return; }
			properties.put(JMRTDApp.CVCA_STORE_KEY, uris.toString());
		}

		public void load(Preferences preferences) {
			try {
				String[] keys = preferences.keys();
				for (String key: keys) {
					String value = preferences.get(key, null);
					if (value != null) {
						properties.put(key, value);
					}
				}
			} catch (BackingStoreException bse) {
				bse.printStackTrace();
				/* NOTE: can we ignore this silently? */
			}
		}

		public void store(Preferences preferences) {
			for (Entry<Object, Object> entry: properties.entrySet()) {
				String key = (String)entry.getKey(), value = (String)entry.getValue();
				preferences.put(key, value);
			}
		}
	}

	private static List<URI> parseURIList(String value) {
		List<URI> result = new ArrayList<URI>();
		if (value == null) { return result; }
		if (!value.startsWith("[") || !value.endsWith("]")) {
			try {
				result.add(new URI(value.trim()));
			} catch (URISyntaxException use) {
				use.printStackTrace();
			}
			return result;
		}
		value = value.substring(1, value.length() - 1);
		StringTokenizer tokenizer = new StringTokenizer(value, ", ");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			try {
				result.add(new URI(token));
			} catch (URISyntaxException use) {
				use.printStackTrace();
			}
		}
		return result;
	}
}
