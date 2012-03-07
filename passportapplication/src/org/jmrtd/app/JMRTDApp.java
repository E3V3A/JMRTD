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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.Gender;
import net.sourceforge.scuba.data.TestCountry;
import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.CardTerminalListener;
import net.sourceforge.scuba.smartcards.SCFactory;
import net.sourceforge.scuba.smartcards.ScubaSmartcards;
import net.sourceforge.scuba.smartcards.TerminalCardService;
import net.sourceforge.scuba.util.FileUtil;
import net.sourceforge.scuba.util.IconUtil;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jmrtd.BACStore;
import org.jmrtd.FileBACStore;
import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.MRTDTrustStore;
import org.jmrtd.Passport;
import org.jmrtd.PassportService;
import org.jmrtd.app.PreferencesPanel.ReadingMode;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.DataGroup;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceImageInfo.EyeColor;
import org.jmrtd.lds.FaceImageInfo.FeaturePoint;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.SODFile;

/**
 * Simple graphical application to demonstrate the
 * JMRTD passport host API.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 *
 * @version $Revision: 894 $
 */
public class JMRTDApp implements CardTerminalListener<CommandAPDU, ResponseAPDU>
{
	private static final String MAIN_FRAME_TITLE = "Main";

	private static final Icon CSCA_ANCHORS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("anchor"));
	private static final Icon NEW_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("lightning"));
	private static final Icon OPEN_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("folder"));
	private static final Icon EXIT_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("door_out"));
	private static final Icon RELOAD_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("arrow_rotate_clockwise"));
	private static final Icon PREFERENCES_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("wrench"));
	private static final Icon INFORMATION_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("information"));

	private static final String ABOUT_JMRTD_DEFAULT_TEXT = "JMRTD is brought to you by the JMRTD team!\nVisit http://jmrtd.org/ for more information.";
	private static final String ABOUT_JMRTD_LOGO = "jmrtd_logo-100x100";

	private static final Calendar CALENDAR = Calendar.getInstance(); 

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

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

	private ActionMap actionMap;

	private Container contentPane;
	private CardManager cardManager;
	private PreferencesPanel preferencesPanel;
	private BACStore bacStore;
	private MRTDTrustStore trustManager;

	private APDUTraceFrame apduTraceFrame;

	/**
	 * Constructs the GUI.
	 *
	 * @param arg command line arguments, are ignored for now.
	 */
	public JMRTDApp() {
		try {			
			ScubaSmartcards<CommandAPDU, ResponseAPDU> sc = ScubaSmartcards.getInstance();
			SCFactory apduFactory = new SCFactory();
			sc.init(apduFactory);

			actionMap = new ActionMap();

			cardManager = CardManager.getInstance();
			//			PassportManager passportManager = PassportManager.getInstance();
			this.bacStore = new FileBACStore();
			trustManager = new MRTDTrustStore();

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
					URI baseDir = FileUtil.getBaseDirAsURI(this.getClass());
					defaultCSCAURI = baseDir.resolve("csca.ks");
				}
				LOGGER.info("Adding " + defaultCSCAURI.toString() + " as CSCA store.");
				preferencesPanel.addCSCAStoreLocation(defaultCSCAURI);
				/* NOTE: GUI will perhaps need updating, delay until end of constructor. */
			}
			BACStorePanel bacStorePanel = new BACStorePanel(bacStore);

			final JFrame mainFrame = new JMRTDFrame(MAIN_FRAME_TITLE);
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
			cardManager.addCardTerminalListener(this);
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
			cardManager.addAPDUListener(apduTraceFrame.getRawAPDUListener());
		} else {
			if (apduTraceFrame != null) {
				apduTraceFrame.setVisible(false);
				cardManager.removeAPDUListener(apduTraceFrame.getRawAPDUListener());
				apduTraceFrame = null;
			}
		}
		trustManager.clear();
		trustManager.addCSCAStores(preferencesPanel.getCSCAStoreLocations());
		trustManager.addCVCAStores(preferencesPanel.getCVCAStoreLocations());
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

	public void cardInserted(CardEvent<CommandAPDU, ResponseAPDU> ce) {
		try {
			PassportService<CommandAPDU, ResponseAPDU> service = new PassportService<CommandAPDU, ResponseAPDU>(ce.getService());
			if (apduTraceFrame != null) {
				service.addPlainTextAPDUListener(apduTraceFrame.getPlainTextAPDUListener());
			}
			service.open();
			readPassport(service);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cardRemoved(CardEvent<CommandAPDU, ResponseAPDU> ce) {
		CardService<CommandAPDU, ResponseAPDU> service = ce.getService();
		if (service != null) {
			service.close();
		}
	}

	/**
	 * Reads the passport from a service by attempting to
	 * perform BAC and firing up a PassportFrame.
	 * 
	 * @param service
	 * @throws CardServiceException
	 */
	private void readPassport(PassportService<CommandAPDU, ResponseAPDU> service) throws CardServiceException {
		Passport<CommandAPDU, ResponseAPDU> passport = new Passport<CommandAPDU, ResponseAPDU>(service, trustManager, bacStore);
		DocumentViewFrame passportFrame = new DocumentViewFrame(passport, preferencesPanel.getReadingMode());
		passportFrame.pack();
		passportFrame.setVisible(true);
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

		JMenuItem cscaCertsItem = new JMenuItem();
		cscaCertsItem.setAction(getCSCACertsAction());
		menu.add(cscaCertsItem);

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
					Passport<CommandAPDU, ResponseAPDU> passport = createEmptyPassport("P<", trustManager);
					DocumentEditFrame passportFrame = new DocumentEditFrame(passport, ReadingMode.SAFE_MODE);
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
				int choice = fileChooser.showOpenDialog(contentPane);
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, file.getParent());
						Passport<CommandAPDU, ResponseAPDU> passport = new Passport<CommandAPDU, ResponseAPDU>(file, trustManager);

						DocumentViewFrame passportFrame = new DocumentViewFrame(passport, ReadingMode.SAFE_MODE);
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
									CardService<CommandAPDU, ResponseAPDU> service = cardManager.getService(terminal);
									if (service != null) { service.close(); }
									service = new TerminalCardService(terminal);
									service.addAPDUListener(apduTraceFrame.getRawAPDUListener());
									PassportService<CommandAPDU, ResponseAPDU> passportService = new PassportService<CommandAPDU, ResponseAPDU>(service);
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
					readMeFile = new URL(FileUtil.getBaseDir(getClass()) + "/README");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				ImageIcon aboutJMRTDImageIcon = null;
				Image aboutJMRTDImage = IconUtil.getImage(ABOUT_JMRTD_LOGO, getClass());
				if (aboutJMRTDImage != null) { aboutJMRTDImageIcon = new ImageIcon(aboutJMRTDImage); }

				try {
					JTextArea area = new JTextArea(20, 35);
					// HIER
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
	 * Creates passport from scratch.
	 * 
	 * @param docType either <code>MRZInfo.DOC_TYPE_ID1</code> or <code>MRZInfo.DOC_TYPE_ID3</code>
	 * @throws GeneralSecurityException if something wrong
	 */
	private static Passport<CommandAPDU,ResponseAPDU> createEmptyPassport(String docType, MRTDTrustStore trustManager) throws GeneralSecurityException {

		/* EF.COM */
		int[] tagList = { LDSFile.EF_DG1_TAG, LDSFile.EF_DG2_TAG };
		COMFile comFile = new COMFile("1.7", "4.0.0", tagList);

		/* EF.DG1 */
		Date today = CALENDAR.getTime();
		String todayString = SDF.format(today);
		String primaryIdentifier = "TRAVELER";
		String secondaryIdentifiers = "HAPPY";
		String documentNumber = "123456789";
		Country country = TestCountry.UT;
		Gender gender = Gender.FEMALE;
		String optionalData = "";
		MRZInfo mrzInfo = new MRZInfo(docType, country.toAlpha3Code(), primaryIdentifier, secondaryIdentifiers, documentNumber, country.toAlpha3Code(), todayString, gender, todayString, optionalData);
		DG1File dg1 = new DG1File(mrzInfo);

		/* EF.DG2 */
		FaceImageInfo faceImageInfo = createFaceImageInfo();
		FaceInfo faceInfo = new FaceInfo(Arrays.asList(new FaceImageInfo[] { faceImageInfo }));
		DG2File dg2 = new DG2File(Arrays.asList(new FaceInfo[] { faceInfo }));

		/* EF.SOD */
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		Date dateOfIssuing = today;
		Date dateOfExpiry = today;
		String digestAlgorithm = "SHA256";
		String signatureAlgorithm = "SHA256withRSA";
		String issuer = "C=UT, O=JMRTD, OU=DSCA, CN=jmrtd.org";
		String subject = "C=UT, O=JMRTD, OU=DSCA, CN=jmrtd.org";
		X509Certificate docSigningCert = generateSelfSignedCertificate(issuer, subject, dateOfIssuing, dateOfExpiry, publicKey, privateKey, signatureAlgorithm);
		PrivateKey docSigningPrivateKey = privateKey;
		Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
		MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
		hashes.put(1, digest.digest(dg1.getEncoded()));
		hashes.put(2, digest.digest(dg2.getEncoded()));
		SODFile sodFile = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, privateKey, docSigningCert);
		return new Passport(comFile, Arrays.asList(new DataGroup[] { dg1, dg2 }), sodFile, docSigningPrivateKey, trustManager);
	}

	private static FaceImageInfo createFaceImageInfo() {
		int width = 449, height = 599;
		byte[] jpegImageBytes = createTrivialJPEGBytes(width, height);
		Gender gender = Gender.UNSPECIFIED;
		EyeColor eyeColor = EyeColor.UNSPECIFIED;
		int hairColor = FaceImageInfo.HAIR_COLOR_UNSPECIFIED;
		int featureMask = 0;
		short expression = FaceImageInfo.EXPRESSION_UNSPECIFIED;
		int[] poseAngle = { 0, 0, 0 };
		int[] poseAngleUncertainty = { 0, 0, 0 };
		int faceImageType = FaceImageInfo.FACE_IMAGE_TYPE_FULL_FRONTAL;
		int colorSpace = 0x00;
		int sourceType = FaceImageInfo.SOURCE_TYPE_UNSPECIFIED;
		int deviceType = 0x0000;
		int quality = 0x0000;
		int imageDataType = FaceImageInfo.IMAGE_DATA_TYPE_JPEG;	
		FeaturePoint[] featurePoints = { };
		FaceImageInfo imageInfo = new FaceImageInfo(
				gender,  eyeColor, hairColor,
				featureMask,
				expression,
				poseAngle, poseAngleUncertainty,
				faceImageType,
				colorSpace,
				sourceType,
				deviceType,
				quality,
				featurePoints,
				width, height,
				jpegImageBytes, imageDataType);
		return imageInfo;
	}

	private static byte[] createTrivialJPEGBytes(int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", out);
			out.flush();
			byte[] bytes = out.toByteArray();
			return bytes;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static X509Certificate generateSelfSignedCertificate(String issuer, String subject, Date dateOfIssuing, Date dateOfExpiry,
			PublicKey publicKey, PrivateKey privateKey, String signatureAlgorithm) throws CertificateEncodingException, InvalidKeyException, IllegalStateException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException {
		X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
		certGenerator.setSerialNumber(new BigInteger("1"));
		certGenerator.setIssuerDN(new X509Name(issuer));
		certGenerator.setSubjectDN(new X509Name(subject));
		certGenerator.setNotBefore(dateOfIssuing);
		certGenerator.setNotAfter(dateOfExpiry);
		certGenerator.setPublicKey(publicKey);
		certGenerator.setSignatureAlgorithm(signatureAlgorithm);
		X509Certificate certificate = (X509Certificate)certGenerator.generate(privateKey, "BC");
		return certificate;
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
					if (osName.contains("Windows")) {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					} else if (osName.contains("Mac")) {
						System.setProperty("apple.laf.useScreenMenuBar", "true");
						System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JMRTD");
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					}
					new JMRTDApp();
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
