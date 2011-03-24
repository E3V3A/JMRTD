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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.security.auth.x500.X500Principal;
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
import org.jmrtd.FileBACStore;
import org.jmrtd.Passport;
import org.jmrtd.PassportEvent;
import org.jmrtd.PassportListener;
import org.jmrtd.PassportManager;
import org.jmrtd.PassportService;
import org.jmrtd.app.PreferencesPanel.ReadingMode;
import org.jmrtd.cert.KeyStoreCertStoreParameters;
import org.jmrtd.cert.PKDCertStoreParameters;
import org.jmrtd.cert.PKDMasterListCertStoreParameters;
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

	private static final CertSelector SELF_SIGNED_X509_CERT_SELECTOR = new X509CertSelector() {
		public boolean match(Certificate cert) {
			if (!(cert instanceof X509Certificate)) { return false; }
			X509Certificate x509Cert = (X509Certificate)cert;
			X500Principal issuer = x509Cert.getIssuerX500Principal();
			X500Principal subject = x509Cert.getSubjectX500Principal();
			return (issuer == null && subject == null) || subject.equals(issuer);
		}

		public Object clone() { return this; }		
	};
	
	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48", JMRTDApp.class);
	private static final Icon NEW_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("lightning"));
	private static final Icon OPEN_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder"));
	private static final Icon EXIT_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("door_out"));
	private static final Icon RELOAD_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_rotate_clockwise"));
	private static final Icon PREFERENCES_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("wrench"));
	private static final Icon INFORMATION_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));

	private static final String ABOUT_JMRTD_DEFAULT_TEXT = "JMRTD is brought to you by the JMRTD team!\nVisit http://jmrtd.org/ for more information.";
	private static final String ABOUT_JMRTD_LOGO = "jmrtd_logo-100x100";

	private static final Provider
	JMRTD_PROVIDER = new org.jmrtd.JMRTDSecurityProvider(),
	BC_PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();

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

	private ActionMap actionMap;

	private Container contentPane;
	private CardManager cardManager;
	private PreferencesPanel preferencesPanel;
	private BACStore bacStore;
	private Set<TrustAnchor> cscaAnchors;
	private List<CertStore> cscaStores;
	private List<KeyStore> cvcaStores;

	private APDUTraceFrame apduTraceFrame;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	/**
	 * Constructs the GUI.
	 *
	 * @param arg command line arguments, are ignored for now.
	 */
	public JMRTDApp() {
		try {
			/* So that BC stuff knows about CVC certificates */
			BC_PROVIDER.put("CertificateFactory.CVC", "org.jmrtd.cert.CVCertificateFactorySpi");
			Security.insertProviderAt(BC_PROVIDER, 1);
			Security.addProvider(JMRTD_PROVIDER);
			actionMap = new ActionMap();
			cardManager = CardManager.getInstance();
			PassportManager passportManager = PassportManager.getInstance();

			this.bacStore = new FileBACStore();

			preferencesPanel = new PreferencesPanel(getTerminalPollingMap(), this.getClass());
			preferencesPanel.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					updateFromPreferences();
				}
			});
			if (preferencesPanel.isBackingStoreNew()) {
				/*
				 * If there are no previously saved preferences, we have some
				 * suggestions, e.g. certificates for document verification that
				 * came with the installation of JMRTD.
				 */
				URI defaultCSCAURI = null;
				File defaultCSCAFile = new File(new File(System.getProperty("user.dir")).getParentFile(), "csca.ks");
				if (defaultCSCAFile.exists()) {
					defaultCSCAURI = defaultCSCAFile.toURI();
				} else {
					URI baseDir = Files.getBaseDirAsURI(this.getClass());
					defaultCSCAURI = baseDir.resolve("csca.ks");
				}
				LOGGER.info("Adding " + defaultCSCAURI.toString() + " as CSCA store.");
				preferencesPanel.addCSCAStoreLocation(defaultCSCAURI);
				/* NOTE: GUI will perhaps need updating, delay until end of constructor. */
			}
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
		updateCSCACertStoresFromPreferences();
		updateCVCACertStoresFromPreferences();
	}
	
	private void updateCSCACertStoresFromPreferences() {
		List<URI> cscaStoreLocations = preferencesPanel.getCSCAStoreLocations();
		this.cscaStores = new ArrayList<CertStore>(cscaStoreLocations.size());
		if (cscaAnchors == null) { cscaAnchors = new HashSet<TrustAnchor>(); }
		if (cscaStoreLocations != null) {
			for (URI uri: cscaStoreLocations) {
				if (uri == null) { LOGGER.severe("location == null"); continue; }
				String scheme = uri.getScheme();
				if (scheme == null) { LOGGER.severe("scheme == null, location = " + uri); continue; }
				try {
					if (scheme != null && scheme.equals("ldap")) {
						String server = uri.getHost();
						int port = uri.getPort();
						CertStoreParameters params = port < 0 ? new PKDCertStoreParameters(server) : new PKDCertStoreParameters(server, port);
						CertStoreParameters cscaParams = port < 0 ? new PKDMasterListCertStoreParameters(server) : new PKDMasterListCertStoreParameters(server, port);
						CertStore certStore = CertStore.getInstance("PKD", params);
						if (certStore != null) { cscaStores.add(certStore); }
						CertStore cscaStore = CertStore.getInstance("PKD", cscaParams);
						if (cscaStore != null) { cscaStores.add(cscaStore); }
						Collection<? extends Certificate> rootCerts = cscaStore.getCertificates(SELF_SIGNED_X509_CERT_SELECTOR);
						cscaAnchors.addAll(getAsAnchors(rootCerts));
					} else {
						/* TODO: Should we check that scheme is "file" or "http"? */
						try {
							CertStoreParameters params = new KeyStoreCertStoreParameters(uri, "JKS");
							CertStore certStore = CertStore.getInstance("JKS", params);
							cscaStores.add(certStore);
							Collection<? extends Certificate> rootCerts = certStore.getCertificates(SELF_SIGNED_X509_CERT_SELECTOR);
							cscaAnchors.addAll(getAsAnchors(rootCerts));
						} catch (KeyStoreException kse) {
							kse.printStackTrace();
						}
					}
				} catch (GeneralSecurityException gse) {
					gse.printStackTrace();
				}
			}
		}
	}
	
	private void updateCVCACertStoresFromPreferences() {
		List<URI> cvcaStoreLocations = preferencesPanel.getCVCAStoreLocations();
		this.cvcaStores = new ArrayList<KeyStore>(cvcaStoreLocations.size());
		// We have to try both store types, only Bouncy Castle Store (BKS) 
		// knows about unnamed EC keys
		String[] storeTypes = new String[] {"JKS", "BKS" }; 
		for (URI uri: cvcaStoreLocations) {
			for(String storeType : storeTypes) {
				try {
					KeyStore cvcaStore = KeyStore.getInstance(storeType);
					URLConnection uc = uri.toURL().openConnection();
					InputStream in = uc.getInputStream();
					cvcaStore.load(in, "".toCharArray());
					cvcaStores.add(cvcaStore);
				} catch (Exception e) {
					LOGGER.warning("Could not initialize CVCA: " + e.getMessage());
				}
			}
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
		Passport passport = new Passport(service, cscaAnchors, cscaStores, cvcaStores, bacStore);
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
		Action action = actionMap.get("New");
		if (action != null) { return action; }
		action = new AbstractAction() {

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
				fileChooser.setFileFilter(Files.ZIP_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(contentPane);
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, file.getParent());
						Passport passport = new Passport(file, cscaAnchors, cscaStores);

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
		actionMap.put("Preferences", action);
		return action;
	}

	private Action getAboutAction() {
		Action action = actionMap.get("About");
		if (action != null) { return action; }
		action = new AbstractAction() {

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
		actionMap.put("About", action);
		return action;
	}

	/**
	 * Returns a set of trust anchors based on the X509 certificates in <code>certificates</code>.
	 * 
	 * @param certificates a collection of X509 certificates
	 * 
	 * @return a set of trust anchors
	 */
	private Set<TrustAnchor> getAsAnchors(Collection<? extends Certificate> certificates) {
		Set<TrustAnchor> anchors = new HashSet<TrustAnchor>(certificates.size());
		for (Certificate certificate: certificates) {
			if (certificate instanceof X509Certificate) {
				anchors.add(new TrustAnchor((X509Certificate)certificate, null));
			}
		}
		return anchors;
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
