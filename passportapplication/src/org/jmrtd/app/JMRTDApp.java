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
 * $Id: PassportApp.java 894 2009-03-23 15:50:46Z martijno $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.CardTerminalListener;

import org.jmrtd.BACDeniedException;
import org.jmrtd.BACStore;
import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.MRTDTrustStore;
import org.jmrtd.Passport;
import org.jmrtd.PassportService;
import org.jmrtd.app.PreferencesDialog.ReadingMode;
import org.jmrtd.app.util.FileUtil;
import org.jmrtd.app.util.IconUtil;
import org.jmrtd.app.util.OSXAdapter;

/**
 * Simple graphical application to demonstrate the
 * JMRTD passport host API.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 894 $
 */
public class JMRTDApp {

	private static final String MAIN_FRAME_TITLE = "Main";

	private static final Icon CSCA_ANCHORS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("anchor"));
	private static final Icon NEW_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("lightning"));
	private static final Icon OPEN_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("folder"));
	private static final Icon EXIT_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("door_out"));
	private static final Icon RELOAD_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("arrow_rotate_clockwise"));
	private static final Icon PREFERENCES_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("wrench"));
	private static final Icon INFORMATION_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("information"));

	public static final String
	READING_MODE_KEY = "mode.reading",
	TERMINAL_KEY_PREFIX = "terminal.",
	APDU_TRACING_KEY = "trace.apdu",
	BAC_STORE_KEY = "location.bac",
	CSCA_STORE_KEY ="location.csca",
	CVCA_STORE_KEY ="location.cvca",
	PASSPORT_ZIP_FILES_DIR_KEY = "location.passportzipfiles",
	PASSPORT_LOG_FILES_DIR_KEY = "location.passportlogfiles",
	IMAGE_FILES_DIR_KEY = "location.imagefiles",
	CERT_AND_KEY_FILES_DIR_KEY = "location.certfiles";

	private static final Provider
	JMRTD_PROVIDER = JMRTDSecurityProvider.getInstance(),
	BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	static {
		/* So that BC stuff knows about CVC certificates. */
		BC_PROVIDER.put("CertificateFactory.CVC", JMRTD_PROVIDER.get("CertificateFactory.CVC"));
		Security.insertProviderAt(BC_PROVIDER, 1);
		Security.addProvider(JMRTD_PROVIDER);

		Provider[] providers = Security.getProviders();
		for (Provider provider: providers) {
			LOGGER.info("Provider " + provider.getName() + ": " + provider.getClass().getCanonicalName());
		}
	}

	/* FIXME: I know, it's ugly. */
	private boolean isOSX;

	private ActionMap actionMap;

	private JFrame mainFrame;
	private CardManager cardManager;
	private BACStore bacStore;
	private MRTDTrustStore trustManager;

	private AboutDialog aboutDialog;
	private PreferencesDialog preferencesDialog;
	
	private BACStorePanel bacStorePanel;

	private APDUTraceFrame apduTraceFrame;

	/**
	 * Constructs the GUI.
	 *
	 * @param isOSX whether we're on OS X
	 */
	public JMRTDApp(boolean isOSX) {
		try {	
			actionMap = new ActionMap();
			cardManager = CardManager.getInstance();

			this.isOSX = isOSX;

			mainFrame = new JMRTDFrame(MAIN_FRAME_TITLE);

			aboutDialog = new AboutDialog(mainFrame);
			aboutDialog.pack();

			this.bacStore = new FileBACStore();
			trustManager = new MRTDTrustStore();

			preferencesDialog = new PreferencesDialog(mainFrame, getTerminalPollingMap(), this.getClass());
			preferencesDialog.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					updateFromPreferences();
				}
			});
			preferencesDialog.pack();
			if (preferencesDialog.isBackingStoreNew()) {
				/*
				 * If there are no previously saved preferences, we have some
				 * suggestions, e.g. the CSCA trust store that
				 * came with the installation of JMRTD.
				 */
				URI defaultCSCAURI = null;
				File defaultCSCAFile = new File(new File(System.getProperty("user.dir")).getParentFile(), "csca.ks");
				if (defaultCSCAFile.exists()) {
					defaultCSCAURI = defaultCSCAFile.toURI();
				} else {
					URI baseDir = FileUtil.getBaseDirAsURI(this.getClass());
					defaultCSCAURI = baseDir.resolve("csca.ks");
				}
				LOGGER.info("Adding " + defaultCSCAURI.toString() + " as CSCA store.");
				preferencesDialog.addCSCAStoreLocation(defaultCSCAURI);
				/* NOTE: GUI will perhaps need updating, delay until end of constructor. */
			}
			bacStorePanel = new BACStorePanel(bacStore);

			Container contentPane = mainFrame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(bacStorePanel, BorderLayout.CENTER);

			final MRZKeyListener keySource = new MRZKeyListener(bacStorePanel);
			addMRZKeyListener(mainFrame, keySource);
			JMenuBar menuBar = new JMenuBar();
			menuBar.add(createFileMenu());
			menuBar.add(createToolsMenu());
			menuBar.add(createHelpMenu());
			mainFrame.setJMenuBar(menuBar);

			/* On OS X "About" and "Preferences" go in the JMRTD menu. */
			if (isOSX) {
				try {
					OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAboutDialog", (Class[])null));
					OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("showPreferencesDialog", (Class[])null));
					OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("exit", (Class[])null));
				} catch (NoSuchMethodException nsme) {
					nsme.printStackTrace();
				}
			}

			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			mainFrame.pack();
			mainFrame.setVisible(true);
			try {
				addTerminalProvider(cardManager, "ACR122", "net.sourceforge.scuba.smartcards.ACR122Provider");
				LOGGER.info("Added ACR terminal provider");
			} catch (Exception e) {
				LOGGER.warning("Not adding ACR terminal provider: " + e.getMessage());
			}
			cardManager.addCardTerminalListener(new CardTerminalListener() {
				@Override
				public void cardInserted(CardEvent ce) {
					try {
						PassportService service = new PassportService(ce.getService());
						if (apduTraceFrame != null) {
							service.addPlainTextAPDUListener(apduTraceFrame.getPlainTextAPDUListener());
						}
						try {
							service.open();
							readPassport(service);
						} catch (CardServiceException cse) {
							LOGGER.info("Could not open passport: " + cse.getMessage());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void cardRemoved(CardEvent ce) {
					CardService service = ce.getService();
					if (service != null) {
						service.close();
					}
				}
			});
			cardManager.addTerminalFactoryListener(preferencesDialog.getTerminalFactoryListener());
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

	private static void addTerminalProvider(CardManager cardManager, String providerName, String providerClassName) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchAlgorithmException {
		Class<?> providerClass = Class.forName(providerClassName);
		Provider provider = (Provider)providerClass.newInstance();
		TerminalFactory factory = TerminalFactory.getInstance(providerName, null, provider);
		cardManager.addTerminals(factory, true);
	}

	private void updateFromPreferences() {
		List<CardTerminal> terminals = cardManager.getTerminals();
		for (CardTerminal terminal: terminals) {
			if (preferencesDialog.isTerminalChecked(terminal)) {
				if (!cardManager.isPolling(terminal)) { cardManager.startPolling(terminal); }
			} else {
				if (cardManager.isPolling(terminal)) { cardManager.stopPolling(terminal); }
			}
		}

		if (preferencesDialog.isAPDUTracing()) {
			if (apduTraceFrame == null) {
				apduTraceFrame = new APDUTraceFrame("APDU trace");
			}
			apduTraceFrame.pack();
			apduTraceFrame.setVisible(true);
			cardManager.addAPDUListener(apduTraceFrame.getRawAPDUListener());
		} else {
			if (apduTraceFrame != null) {
				apduTraceFrame.setVisible(false);
				cardManager.removeAPDUListener(apduTraceFrame.getRawAPDUListener());
				apduTraceFrame = null;
			}
		}
		trustManager.clear();
		trustManager.addCSCAStores(preferencesDialog.getCSCAStoreLocations());
		trustManager.addCVCAStores(preferencesDialog.getCVCAStoreLocations());
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

	/**
	 * Reads the passport from a service by attempting to
	 * perform BAC and firing up a PassportFrame.
	 * 
	 * @param service
	 * @throws CardServiceException
	 */
	private void readPassport(PassportService service) throws CardServiceException {
		try {
			Passport passport = new Passport(service, trustManager, bacStore, 1);
			DocumentViewFrame passportFrame = new DocumentViewFrame(passport, preferencesDialog.getReadingMode(), apduTraceFrame == null ? null : apduTraceFrame.getRawAPDUListener());
			passportFrame.pack();
			passportFrame.setVisible(true);
		} catch (BACDeniedException bde) {
			bacStorePanel.getAddAction().actionPerformed(new ActionEvent(this, 0, "Add BAC"));
		} catch (CardServiceException cse) {
			cse.printStackTrace();
		}
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

		if (isOSX) {
			/* NOTE: We're using the Application framework for the Quit menu item on OS X. */
		} else {
			menu.addSeparator();

			/* Exit */
			JMenuItem exitItem = new JMenuItem();
			menu.add(exitItem);
			exitItem.setAction(getExitAction());
		}

		return menu;
	}

	private JMenu createToolsMenu() {
		JMenu menu = new JMenu("Tools");

		JMenuItem cscaCertsItem = new JMenuItem();
		cscaCertsItem.setAction(getCSCACertsAction());
		menu.add(cscaCertsItem);

		JMenuItem reloadItem = new JMenuItem();
		reloadItem.setAction(getReloadAction());
		menu.add(reloadItem);

		if (isOSX) {
			/* NOTE: We're using the Application framework for the Preferences menu item on OS X. */
		} else {
			JMenuItem preferencesItem = new JMenuItem();
			preferencesItem.setAction(getPreferencesAction());
			menu.add(preferencesItem);
		}

		return menu;
	}

	private JMenu createHelpMenu() {
		JMenu menu = new JMenu("Help");
		if (isOSX) {
			/* NOTE: We're using the Application framework for the About menu item on OS X. */
		} else {
			JMenuItem aboutItem = new JMenuItem();
			aboutItem.setAction(getAboutAction());
			menu.add(aboutItem);
		}
		return menu;
	}

	private Action getNewAction() {
		Action action = actionMap.get("New");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = 2866114377708028964L;

			public void actionPerformed(ActionEvent e) {
				try {
					Passport passport = DocumentFactory.createEmptyMRTD("P<", trustManager);
					DocumentEditFrame passportFrame = new DocumentEditFrame(passport, ReadingMode.SAFE_MODE, apduTraceFrame == null ? null : apduTraceFrame.getRawAPDUListener());
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
		actionMap.put("New", action);
		return action;
	}

	private Action getOpenAction() {
		Action action = actionMap.get("Open");
		if (action != null) { return action; }
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		action = new AbstractAction() {
			private static final long serialVersionUID = -9209238098024027906L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(FileUtil.ZIP_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(mainFrame);
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, file.getParent());
						Passport passport = new Passport(file, trustManager);
						DocumentViewFrame passportFrame = new DocumentViewFrame(passport, ReadingMode.SAFE_MODE, apduTraceFrame == null ? null : apduTraceFrame.getRawAPDUListener());
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
		actionMap.put("Open", action);
		return action;
	}

	private Action getExitAction() {
		Action action = actionMap.get("Exit");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -6229877165532173683L;

			public void actionPerformed(ActionEvent e) {
				System.exit(0); /* NOTE: shuts down the entire VM. */
			}
		};
		action.putValue(Action.SMALL_ICON, EXIT_ICON);
		action.putValue(Action.LARGE_ICON_KEY, EXIT_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Exit application");
		action.putValue(Action.NAME, "Exit");
		actionMap.put("Exit", action);
		return action;
	}

	private Action getCSCACertsAction() {
		Action action = actionMap.get("CSCAAnchors");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = 6389151122469369737L;

			public void actionPerformed(ActionEvent e) {
				JFrame frame = new CertificateMasterListFrame("CSCA Anchors", trustManager.getCSCAAnchors());
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.pack();
				frame.setVisible(true);				
			}			
		};
		action.putValue(Action.SMALL_ICON, CSCA_ANCHORS_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CSCA_ANCHORS_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Show CSCA anchors");
		action.putValue(Action.NAME, "CSCA anchors...");
		actionMap.put("CSCAAnchorss", action);
		return action;
	}

	private Action getReloadAction() {
		Action action = actionMap.get("Reload");
		if (action != null) { return action; }
		action = new AbstractAction() {

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
									service = CardService.getInstance(terminal);
									if (apduTraceFrame != null) { service.addAPDUListener(apduTraceFrame.getRawAPDUListener()); }
									PassportService passportService = new PassportService(service);
									if (apduTraceFrame != null) { passportService.addPlainTextAPDUListener(apduTraceFrame.getPlainTextAPDUListener()); }
									readPassport(passportService);

									if (isPolling) { cardManager.startPolling(terminal); }
								}
							} catch (CardException ce) {
								/* NOTE: skip this terminal */
							} catch (CardServiceException cse) {
								/* NOTE: skip this terminal */
							} catch (Exception e) {
								e.printStackTrace();
								LOGGER.warning("skipping " + terminal.getName() + ", cannot open because of " + e.toString());
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
		actionMap.put("Reload", action);
		return action;
	}

	private Action getPreferencesAction() {
		Action action = actionMap.get("Preferences");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = 11962156923823504L;

			public void actionPerformed(ActionEvent e) {
				showPreferencesDialog();
			}
		};
		action.putValue(Action.SMALL_ICON, PREFERENCES_ICON);
		action.putValue(Action.LARGE_ICON_KEY, PREFERENCES_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Open the preferences dialog");
		action.putValue(Action.NAME, "Preferences...");
		actionMap.put("Preferences", action);
		return action;
	}

	private Action getAboutAction() {
		Action action = actionMap.get("About");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = 1528395261878587434L;

			public void actionPerformed(ActionEvent e) {
				showAboutDialog();
			}
		};
		action.putValue(Action.SMALL_ICON, INFORMATION_ICON);
		action.putValue(Action.LARGE_ICON_KEY, INFORMATION_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Display information about this application");
		action.putValue(Action.NAME, "About...");
		actionMap.put("About", action);
		return action;
	}

	/* NOTE: Public because OSXAdapter calls this on OS X via reflection. */
	public void showAboutDialog() {
		aboutDialog.setLocationRelativeTo(mainFrame);
		aboutDialog.setVisible(true);
	}

	/* NOTE: Public because OSXAdapter calls this on OS X via reflection. */
	public void showPreferencesDialog() {
		preferencesDialog.setLocationRelativeTo(mainFrame);
		preferencesDialog.setVisible(true);
	}

	/* NOTE: Public because OSXAdapter calls this on OS X via reflection. */
	public void exit() {
		System.exit(0);
	}

	/**
	 * Main method creates an instance.
	 *
	 * @param arg command line arguments.
	 */
	public static void main(String[] arg) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					String osName = System.getProperty("os.name");
					String systemLookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
					LOGGER.info("OS name = " + osName);
					LOGGER.info("System look and feel class name = " + systemLookAndFeelClassName);
					boolean isMacOSX = false;
					if (osName.toLowerCase().startsWith("windows")) {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					} else if (osName.toLowerCase().startsWith("mac os x") || System.getProperty("mrj.version") != null) {
						/* Better to set these on command line (in jmrtd.sh for MacOS X):
						 * 
						 * java -Dcom.apple.macos.useScreenMenuBar=true \
						 * 		-Dcom.apple.mrj.application.apple.menu.about.name=JMRTD \
						 * 		-Xdock:name="JMRTD" \
						 * 		-jar jmrtd.jar
						 */
						System.setProperty("apple.laf.useScreenMenuBar", "true");
						System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JMRTD");
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
						isMacOSX = true;
					}
					new JMRTDApp(isMacOSX);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (UnsupportedLookAndFeelException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
