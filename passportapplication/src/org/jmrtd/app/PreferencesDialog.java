/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardTerminalEvent;
import net.sourceforge.scuba.smartcards.TerminalFactoryListener;

import org.jmrtd.app.swing.URIListEditor;

public class PreferencesDialog extends JDialog {

	private static final long serialVersionUID = -2915531202910732443L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	private static final Dimension PREFERRED_SIZE = new Dimension(350, 320);
	private static final String COMMIT_TEXT = "OK", ABORT_TEXT = "Cancel";
	private static final Object[] OPTIONS = { COMMIT_TEXT, ABORT_TEXT };

	private JOptionPane optionPane; 
	private TerminalFactoryListener terminalFactoryListener;
	private JPanel cardTerminalsPreferencesPanel;

	/**
	 * Reading mode values.
	 * 
	 * @author The JMRTD team (info@jmrtd.org)
	 * 
	 * @version $Revision: $
	 */
	public enum ReadingMode {
		SAFE_MODE, // completely read files, check their signature, then display only if valid
		PROGRESSIVE_MODE; // display files while still reading, then check their signature
	};

	private static final ReadingMode DEFAULT_READING_MODE = ReadingMode.SAFE_MODE;
	private static final boolean DEFAULT_APDU_TRACING_SETTING = false;

	private PreferencesState state;
	private PreferencesState changedState;

	private Preferences preferences;
	private boolean existsBackingStore;

	private Map<String, JCheckBox> cardTerminalPollingCheckBoxMap;
	private Map<ReadingMode, JRadioButton> readingModeRadioButtonMap;
	private JCheckBox apduTracingCheckBox;

	private URIListEditor cscaLocationsDisplay, cvcaLocationsDisplay;

	private Collection<ChangeListener> changeListeners;

	public PreferencesDialog(Frame frame, final Map<CardTerminal, Boolean> cm, Class<?> applicationClass) {
		super(frame, true);
		this.changeListeners = new ArrayList<ChangeListener>();
		this.preferences = Preferences.userNodeForPackage(applicationClass);
		try {
			existsBackingStore = false;
			existsBackingStore = (preferences.keys() != null && preferences.keys().length > 0);
		} catch (BackingStoreException bse) {
			/* NOTE it probably doesn't exist */
		}
		cardTerminalPollingCheckBoxMap = new HashMap<String, JCheckBox>();

		this.state = new PreferencesState();
		update();

		this.changedState = new PreferencesState(state);

		JTabbedPane jtb = new JTabbedPane();
		jtb.addTab("Terminals", createTerminalsPreferencesTab(cm));
		jtb.addTab("Certificates", createCertificatesPanel());
		buildDialog("JMRTD - Preferences", jtb);

		updateGUIFromState(state);
		terminalFactoryListener = new TerminalFactoryListener() {
			@Override
			public void cardTerminalAdded(CardTerminalEvent cte) {
				CardTerminal terminal = cte.getTerminal();
				LOGGER.info("Adding terminal to preferences " + terminal.getName());
				updateGUIFromState(state);
			}

			@Override
			public void cardTerminalRemoved(CardTerminalEvent cte) {
				CardTerminal terminal = cte.getTerminal();
				LOGGER.info("Removing terminal from preferences " + terminal.getName());
				updateGUIFromState(state);
			}
		};
	}

	@Override
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	public TerminalFactoryListener getTerminalFactoryListener() {
		return terminalFactoryListener;
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

	public void addChangeListener(ChangeListener l) {
		changeListeners.add(l);	
	}

	public void removeChangeListener(ChangeListener l) {
		changeListeners.remove(l);
	}

	/* ONLY PRIVATE METHODS BELOW */

	private void buildDialog(String title, JTabbedPane jtb) {
		try {
			setTitle(title);
			JPanel preferencesPanel = new JPanel(new BorderLayout());
			preferencesPanel.add(jtb, BorderLayout.CENTER);

			optionPane = new JOptionPane(
					preferencesPanel,
					JOptionPane.PLAIN_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION,
					null,
					OPTIONS,
					OPTIONS[0]);

			/* Handle buttons. */
			optionPane.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent e) {
					String prop = e.getPropertyName();
					if (isVisible()
							&& (e.getSource() == optionPane)
							&& (JOptionPane.VALUE_PROPERTY.equals(prop) || JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
						Object value = optionPane.getValue();
						if (value == JOptionPane.UNINITIALIZED_VALUE) {
							/* NOTE: ignore reset. */
							return;
						}
						if (COMMIT_TEXT.equals(value)) {
							commit();
							setVisible(false);
						} else if (ABORT_TEXT.equals(value)) {
							/* NOTE: closed via abort button. */
							abort();
							setVisible(false);
						} else {
							/* NOTE: closed via close button. */
							abort();
							setVisible(false);
						}
						optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
					}
				}
			});

			/* Handle window closing correctly. */
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					/*
					 * Instead of directly closing the window,
					 * we're going to change the JOptionPane's
					 * value property.
					 */
					optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
				}
			});

			setContentPane(optionPane);
		} catch (Exception ex) {
			LOGGER.severe("Could not build dialog: " + ex.getMessage());
			//			ex.printStackTrace();
		}
	}

	private JComponent createTerminalsPreferencesTab(Map<CardTerminal, Boolean> terminalList) {
		cardTerminalsPreferencesPanel = new JPanel(new GridLayout(terminalList.size(), 1));
		cardTerminalsPreferencesPanel.setBorder(BorderFactory.createTitledBorder("Card Terminals"));
		//		for (Entry<CardTerminal, Boolean> entry: terminalList.entrySet()) {
		//			CardTerminal terminal = entry.getKey();
		//			Boolean isCheckedValue = entry.getValue();
		//			boolean isChecked = isCheckedValue == null ? false : isCheckedValue; 
		//			addTerminal(terminal, isChecked);
		//		}

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

	private synchronized void addTerminal(CardTerminal terminal, boolean isChecked) {
		if (terminal == null) { throw new IllegalArgumentException("Terminal cannot be null"); }
		String terminalName = terminal.getName();
//		state.setTerminalChecked(terminal, isChecked);

		/* Already have such a check box in the map? */
		JCheckBox checkBox = cardTerminalPollingCheckBoxMap.get(terminalName);
		if (checkBox == null) {
			/* No, create one, add it  */
			checkBox = new JCheckBox(terminalName, isChecked);
			checkBox.setAction(getSetTerminalAction(terminal, checkBox));
			cardTerminalPollingCheckBoxMap.put(terminalName, checkBox);
		}
		
		/* Already have such a check box in the GUI? */
		for (Component component: cardTerminalsPreferencesPanel.getComponents()) {
			if (component instanceof JCheckBox) {
				JCheckBox cb = (JCheckBox)component;
				if (cb.getText().equals(terminalName)) {
					if (checkBox != cb) { System.out.println("DEBUG: different checkbox instances in terminal preferences!"); }
					cb.setSelected(state.isTerminalChecked(terminal));
					validate();
					return;
				}
			}
		}
		cardTerminalsPreferencesPanel.add(checkBox);
		validate();
	}

	/**
	 * Removes a terminal name and checkbox mapping from the checkbox map.
	 * Removes the checkbox from the preferencespanel.
	 * 
	 * @param terminalName the name of the terminal
	 */
	private synchronized void removeTerminal(String terminalName) {
		if (terminalName == null) { throw new IllegalArgumentException("Terminal cannot be null"); }
		JCheckBox checkBox = cardTerminalPollingCheckBoxMap.remove(terminalName);
		if (checkBox != null) { cardTerminalsPreferencesPanel.remove(checkBox); }
		for (Component component: cardTerminalsPreferencesPanel.getComponents()) {
			if (component instanceof JCheckBox) {
				JCheckBox cb = (JCheckBox)component;
				if (terminalName.equals(cb.getText())) {
					System.out.println("DEBUG: removing excess terminal checkbox in terminal preferences!");
					cardTerminalsPreferencesPanel.remove(cb);
				}
			}
		}
		validate();
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

	private synchronized void updateGUIFromState(PreferencesState state) {
		CardManager cm = CardManager.getInstance();
		Collection<CardTerminal> cmTerminals = new ArrayList<CardTerminal>(cm.getTerminals());
		Collection<String> checkBoxTerminalNames = new ArrayList<String>(cardTerminalPollingCheckBoxMap.keySet());

		/* Remove terminals no longer in CM. */		
		for (String terminalName: checkBoxTerminalNames) {
			boolean occursInCM = false;
			for (CardTerminal terminal: cmTerminals) {
				if (terminal.getName().equals(terminalName) && state.isTerminalPresent(terminal)) {
					occursInCM = true;
					break;
				}
			}
			if (!occursInCM) { removeTerminal(terminalName); }
		}
		/* Add terminals from CM. */
		for (CardTerminal terminal: cmTerminals) {
			if (cardTerminalPollingCheckBoxMap.keySet().contains(terminal.getName())) {
				JCheckBox checkBox = cardTerminalPollingCheckBoxMap.get(terminal.getName());
				checkBox.setSelected(state.isTerminalChecked(terminal));
			} else {
				if (state.isTerminalPresent(terminal)) {
					addTerminal(terminal, cm.isPolling(terminal));
				}
			}
		}
		for (ReadingMode mode: readingModeRadioButtonMap.keySet()) {
			JRadioButton radioButton = readingModeRadioButtonMap.get(mode);
			radioButton.setSelected(mode.equals(state.getReadingMode()));
		}
		apduTracingCheckBox.setSelected(state.isAPDUTracing());

		cscaLocationsDisplay.setURIList(state.getCSCAStoreLocations());
		validate();
	}

	private Action getSetTerminalAction(final CardTerminal terminal, final JCheckBox checkBox) {
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

	/**
	 * The state that needs to be saved to or restored from file. The panel
	 * collects user changes in one instance while maintaining the original
	 * settings in another instance.
	 */
	private static class PreferencesState implements Cloneable, Serializable
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

		public boolean isTerminalPresent(CardTerminal terminal) {
			Object obj = properties.get(createTerminalKey(terminal.getName()));
			if (obj == null) { return false; }
			String value = ((String)obj).trim();
			if ("".equals(value)) { return false; }
			return true;
		}

		public boolean isTerminalChecked(CardTerminal terminal) {
			Object obj = properties.get(createTerminalKey(terminal.getName()));
			if (obj == null) { return false; }
			String value = ((String)obj).trim();
			if ("".equals(value)) { return false; }
			Boolean result = Boolean.parseBoolean(value);
			return result;
		}

		public void setTerminalPresent(CardTerminal terminal, boolean b) {
			if (terminal == null) { throw new IllegalArgumentException("Terminal cannot be null"); }
			if (b) {
				/* NOTE: set it unchecked. */
				properties.put(createTerminalKey(terminal.getName()), Boolean.toString(false));
			} else {
				properties.remove(createTerminalKey(terminal.getName()));
			}
		}

		public void setTerminalChecked(CardTerminal terminal, boolean b) {
			properties.put(createTerminalKey(terminal.getName()), Boolean.toString(b));
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

	private void notifyChangeListeners(ChangeEvent ce) {
		for (ChangeListener l: changeListeners) { l.stateChanged(ce); }
	}
}
