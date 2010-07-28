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
 * $Id: PassportApp.java 894 2009-03-23 15:50:46Z martijno $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;
import net.sourceforge.scuba.util.Files;
import net.sourceforge.scuba.util.Icons;

import org.jmrtd.BACStore;
import org.jmrtd.CVCAStore;
import org.jmrtd.Passport;
import org.jmrtd.PassportEvent;
import org.jmrtd.PassportListener;
import org.jmrtd.PassportManager;
import org.jmrtd.PassportService;
import org.jmrtd.app.PreferencesPanel.ReadingMode;
import org.jmrtd.cert.PKCS12FileStore;
import org.jmrtd.cert.PKDCertStore;
import org.jmrtd.cert.TrustStore;
import org.jmrtd.lds.MRZInfo;

/**
 * Simple graphical application to demonstrate the
 * JMRTD passport host API.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 *
 * @version $Revision: 894 $
 */
public class JMRTDApp  implements PassportListener
{
	private static final String MAIN_FRAME_TITLE = "JMRTD";

	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48", JMRTDApp.class);
	private static final Icon NEW_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("lightning"));
	private static final Icon OPEN_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder"));
	private static final Icon EXIT_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("door_out"));
	private static final Icon RELOAD_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_rotate_clockwise"));
	private static final Icon PREFERENCES_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("wrench"));
	private static final Icon INFORMATION_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));

	private static final String ABOUT_JMRTD_DEFAULT_TEXT = "JMRTD is brought to you by the JMRTD team!\nVisit http://jmrtd.org/ for more information.";
	private static final String ABOUT_JMRTD_LOGO = "jmrtd_logo-100x100";

	private static final Provider BC_PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();

	public static final String
	READING_MODE_KEY = "mode.reading",
	TERMINAL_KEY_PREFIX = "terminal.",
	APDU_TRACING_KEY = "trace.apdu",
	BAC_STORE_KEY = "location.bac",
	CSCA_STORE_KEY ="location.csca",
	CVCA_STORE_KEY ="location.cvca",
	PASSPORT_ZIP_FILES_DIR_KEY = "location.passportzipfiles",
	IMAGE_FILES_DIR_KEY = "location.imagefiles",
	CERT_AND_KEY_FILES_DIR_KEY = "location.certfiles";

	private Container contentPane;
	private CardManager cardManager;
	private PreferencesPanel preferencesPanel;
	private BACStore bacStore;
	private List<TrustStore> cscaStores;
	private CVCAStore cvcaStore;

	private APDUTraceFrame apduTraceFrame;

	private Logger logger = Logger.getLogger("org.jmrtd");

	/**
	 * Constructs the GUI.
	 *
	 * @param arg command line arguments, are ignored for now.
	 */
	public JMRTDApp() {
		try {
			Security.insertProviderAt(BC_PROVIDER, 4);
			cardManager = CardManager.getInstance();
			PassportManager passportManager = PassportManager.getInstance();

			this.bacStore = new BACStore();

			preferencesPanel = new PreferencesPanel(getTerminalPollingMap(), this.getClass());
			preferencesPanel.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					updateFromPreferences();
				}
			});
			BACStorePanel bacStorePanel = new BACStorePanel(bacStore);

			final JFrame mainFrame = new JFrame(MAIN_FRAME_TITLE);
			mainFrame.setIconImage(JMRTD_ICON);
			contentPane = mainFrame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(bacStorePanel, BorderLayout.CENTER);

			final MRZKeyListener keySource = new MRZKeyListener(bacStorePanel);
			addMRZKeyListener(mainFrame, keySource);
			JMenuBar menuBar = new JMenuBar();
			menuBar.add(createFileMenu());
			menuBar.add(createToolsMenu());
			menuBar.add(createHelpMenu());
			mainFrame.setJMenuBar(menuBar);
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			mainFrame.pack();
			mainFrame.setVisible(true);

			passportManager.addPassportListener(this);
			updateFromPreferences();
		} catch (Exception e) {
			/* NOTE: if it propagated this far, something is wrong... */
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	private Map<CardTerminal, Boolean> getTerminalPollingMap() {
		Map<CardTerminal, Boolean> terminalPollingMap = new HashMap<CardTerminal, Boolean>();
		List<CardTerminal> terminals = cardManager.getTerminals();
		for (CardTerminal terminal: terminals) { terminalPollingMap.put(terminal, cardManager.isPolling(terminal)); }
		return terminalPollingMap;
	}

	private void updateFromPreferences() {
		List<CardTerminal> terminals = cardManager.getTerminals();
		for (CardTerminal terminal: terminals) {
			if (preferencesPanel.isTerminalChecked(terminal)) {
				if (!cardManager.isPolling(terminal)) { cardManager.startPolling(terminal); }
			} else {
				if (cardManager.isPolling(terminal)) { cardManager.stopPolling(terminal); }
			}
		}

		if (preferencesPanel.isAPDUTracing()) {
			if (apduTraceFrame == null) {
				apduTraceFrame = new APDUTraceFrame("APDU trace");
			}
			apduTraceFrame.pack();
			apduTraceFrame.setVisible(true);
			cardManager.addAPDUListener(apduTraceFrame);
		} else {
			if (apduTraceFrame != null) {
				apduTraceFrame.setVisible(false);
				cardManager.removeAPDUListener(apduTraceFrame);
				apduTraceFrame = null;
			}
		}
		this.cscaStores = new ArrayList<TrustStore>();
		List<URI> cscaStoreLocations = preferencesPanel.getCSCAStoreLocations();
		if (cscaStoreLocations != null) {
			for (URI location: cscaStoreLocations) {
				if (location == null) { logger.warning("DEBUG: location == null"); continue; }
				TrustStore store = null;

				String scheme = location.getScheme();
				if (scheme == null) { logger.warning("DEBUG: scheme == null, location = " + location); continue; }
				if (scheme != null && scheme.equals("ldap")) {
					store = new PKDCertStore(location);
				} else {
					/* TODO: Should we check that scheme is "file" or "http"? */
					store = new PKCS12FileStore(location);
				}
				if (store != null) {
					cscaStores.add(store);
				}
			}
		}

		try {
			this.cvcaStore = new CVCAStore(preferencesPanel.getCVCAStoreLocation());
		} catch (Exception e) {
			logger.warning("Could not initialize CVCA: " + e.getMessage());
		}
	}

	private void addMRZKeyListener(JFrame frame, KeyListener l) {
		final Component component = frame;
		final KeyListener keyListener = l;
		ActionMap am = frame.getRootPane().getActionMap();
		InputMap im = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		String mrzChars = "<0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		for (int i = 0; i < mrzChars.length(); i++) {
			final char c = mrzChars.charAt(i);
			String actionMapKey = "KeyActionFor_" + Character.toString(c);
			am.put(actionMapKey, new AbstractAction() {

				private static final long serialVersionUID = 2298695182878423540L;

				public void actionPerformed(ActionEvent e) {
					KeyEvent ke = new KeyEvent(component, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, Character.toUpperCase(c));
					keyListener.keyTyped(ke);
				}
			});
			im.put(KeyStroke.getKeyStroke(c), actionMapKey);
		}
	}

	public void passportInserted(PassportEvent ce) {
		try {
			PassportService service = ce.getService();
			readPassport(service);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void passportRemoved(PassportEvent ce) {
		/* Do nothing. */
	}

	public void cardInserted(CardEvent ce) {
		/* Ignore non-passport card. */
	}

	public void cardRemoved(CardEvent ce) {
		CardService service = ce.getService();
		if (service != null) {
			service.close();
		}
	}

	/**
	 * Reads the passport from a service by attempting to
	 * perform BAC and firing up a PassportFrame.
	 * 
	 * FIXME: this logic could be moved into the passporthostapi's Passport class.
	 * 
	 * @param service
	 * @throws CardServiceException
	 */
	private void readPassport(PassportService service) throws CardServiceException {
		Passport passport = new Passport(service, cscaStores, cvcaStore, bacStore);
		PassportViewFrame passportFrame = new PassportViewFrame(passport, preferencesPanel.getReadingMode());
	}

	private JMenu createFileMenu() {
		JMenu menu = new JMenu("File");

		/* New... */
		JMenuItem newItem = new JMenuItem();
		menu.add(newItem);
		newItem.setAction(getNewAction());

		/* Open... */
		JMenuItem openItem = new JMenuItem();
		menu.add(openItem);
		openItem.setAction(getOpenAction());

		menu.addSeparator();

		/* Exit */
		JMenuItem closeItem = new JMenuItem();
		menu.add(closeItem);
		closeItem.setAction(getExitAction());

		return menu;
	}

	private JMenu createToolsMenu() {
		JMenu menu = new JMenu("Tools");

		JMenuItem reloadItem = new JMenuItem();
		reloadItem.setAction(getReloadAction());
		menu.add(reloadItem);

		JMenuItem preferencesItem = new JMenuItem();
		preferencesItem.setAction(getPreferencesAction());
		menu.add(preferencesItem);

		return menu;
	}

	private JMenu createHelpMenu() {
		JMenu menu = new JMenu("Help");
		JMenuItem aboutItem = new JMenuItem();
		aboutItem.setAction(getAboutAction());
		menu.add(aboutItem);
		return menu;
	}

	private Action getNewAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 2866114377708028964L;

			public void actionPerformed(ActionEvent e) {
				try {
					Passport passport = new Passport(MRZInfo.DOC_TYPE_ID3);
					PassportViewFrame passportFrame = new PassportViewFrame(passport, ReadingMode.SAFE_MODE);
					passportFrame.pack();
					passportFrame.setVisible(true);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		action.putValue(Action.SMALL_ICON, NEW_ICON);
		action.putValue(Action.LARGE_ICON_KEY, NEW_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Create new passport");
		action.putValue(Action.NAME, "New");
		return action;
	}

	private Action getOpenAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {
			private static final long serialVersionUID = -9209238098024027906L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(Files.ZIP_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(contentPane);
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, file.getParent());
						Passport passport = null; // new Passport(file, cscaStore);
						passport = new Passport(file, cscaStores);

						PassportViewFrame passportFrame = new PassportViewFrame(passport, ReadingMode.SAFE_MODE);
						passportFrame.pack();
						passportFrame.setVisible(true);
					} catch (/* IO */ Exception ioe) {
						/* NOTE: Do nothing. */
					}
					break;
				default: break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, OPEN_ICON);
		action.putValue(Action.LARGE_ICON_KEY, OPEN_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Read passport from file");
		action.putValue(Action.NAME, "Open File...");
		return action;
	}

	private Action getExitAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -6229877165532173683L;

			public void actionPerformed(ActionEvent e) {
				System.exit(0); /* NOTE: shuts down the entire VM. */
			}
		};
		action.putValue(Action.SMALL_ICON, EXIT_ICON);
		action.putValue(Action.LARGE_ICON_KEY, EXIT_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Exit application");
		action.putValue(Action.NAME, "Exit");
		return action;
	}

	private Action getReloadAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 7099324456389820159L;

			public void actionPerformed(ActionEvent e) {
				List<CardTerminal> terminals = cardManager.getTerminals();
				for (final CardTerminal terminal: terminals) {
					(new Thread(new Runnable() {
						public void run() {	
							try {
								if (/* cardManager.isPolling(terminal) && */ terminal.isCardPresent()) {
									boolean isPolling = cardManager.isPolling(terminal);
									if (isPolling) { cardManager.stopPolling(terminal); }
									CardService service = cardManager.getService(terminal);
									if (service != null) { service.close(); }
									PassportService passportService = new PassportService(new TerminalCardService(terminal));
									readPassport(passportService);

									if (isPolling) { cardManager.startPolling(terminal); }
								}
							} catch (CardException ce) {
								/* NOTE: skip this terminal */
							} catch (CardServiceException cse) {
								/* NOTE: skip this terminal */
							} catch (Exception e) {
								e.printStackTrace();
								logger.warning("DEBUG: skipping " + terminal.getName() + ", cannot open because of " + e.toString());
							}
						}
					})).start();
				}
			}
		};
		action.putValue(Action.SMALL_ICON, RELOAD_ICON);
		action.putValue(Action.LARGE_ICON_KEY, RELOAD_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Reload any connected cards");
		action.putValue(Action.NAME, "Reload cards");
		return action;
	}

	private Action getPreferencesAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 11962156923823504L;

			public void actionPerformed(ActionEvent e) {
				int n = JOptionPane.showConfirmDialog(contentPane, preferencesPanel, preferencesPanel.getName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
				switch (n) {
				case JOptionPane.OK_OPTION:
					preferencesPanel.commit();
					break;
				default:
					preferencesPanel.abort();
				}
			}
		};
		action.putValue(Action.SMALL_ICON, PREFERENCES_ICON);
		action.putValue(Action.LARGE_ICON_KEY, PREFERENCES_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Open the preferences dialog");
		action.putValue(Action.NAME, "Preferences...");
		return action;
	}

	private Action getAboutAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 1528395261878587434L;

			public void actionPerformed(ActionEvent e) {
				URL readMeFile = null;
				try {
					readMeFile = new URL(Files.getBaseDir(getClass()) + "/README");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				ImageIcon aboutJMRTDImageIcon = null;
				Image aboutJMRTDImage = Icons.getImage(ABOUT_JMRTD_LOGO, getClass());
				if (aboutJMRTDImage != null) { aboutJMRTDImageIcon = new ImageIcon(aboutJMRTDImage); }

				try {
					JTextArea area = new JTextArea(20, 35);
					if (readMeFile == null) { throw new Exception("Could not open README file"); }
					BufferedReader in = new BufferedReader(new InputStreamReader(readMeFile.openStream()));
					while (true) {
						String line = in.readLine();
						if (line == null) { break; }
						area.append("  " + line.trim());
						area.append("\n");
					}
					in.close();
					area.setCaretPosition(0);
					area.setEditable(false);
					JOptionPane.showMessageDialog(contentPane, new JScrollPane(area), "About JMRTD", JOptionPane.INFORMATION_MESSAGE, aboutJMRTDImageIcon);
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(contentPane, ABOUT_JMRTD_DEFAULT_TEXT, "About JMRTD", JOptionPane.INFORMATION_MESSAGE, aboutJMRTDImageIcon);
				}
			}
		};
		action.putValue(Action.SMALL_ICON, INFORMATION_ICON);
		action.putValue(Action.LARGE_ICON_KEY, INFORMATION_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Display information about this application");
		action.putValue(Action.NAME, "About...");
		return action;
	}

	/**
	 * Main method creates an instance.
	 *
	 * @param arg command line arguments.
	 */
	public static void main(String[] arg) {
		new JMRTDApp();
	}
}
