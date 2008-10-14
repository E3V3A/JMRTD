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
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import sos.mrtd.COMFile;
import sos.mrtd.PassportEvent;
import sos.mrtd.PassportListener;
import sos.mrtd.PassportManager;
import sos.mrtd.PassportService;
import sos.smartcards.APDUFingerprint;
import sos.smartcards.APDUListener;
import sos.smartcards.CardFileInputStream;
import sos.smartcards.CardManager;
import sos.smartcards.CardServiceException;
import sos.smartcards.TerminalCardService;
import sos.util.Files;
import sos.util.Hex;
import sos.util.Icons;

/**
 * Simple graphical application to demonstrate the
 * JMRTD passport host API.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 308 $
 */
public class PassportApp  implements PassportListener
{
	private static final boolean APDU_DEBUG = false;
	private static final String MAIN_FRAME_TITLE = "JMRTD";

	public static final File JMRTD_USER_DIR =
		new File(new File(System.getProperty("user.home")), ".jmrtd");

	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_icon");
	private static final Icon NEW_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("lightning"));
	private static final Icon NEW_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("lightning"));
	private static final Icon OPEN_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder"));
	private static final Icon OPEN_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder"));
	private static final Icon EXIT_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("door_out"));
	private static final Icon EXIT_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("door_out"));
	private static final Icon TERMINAL_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive"));
	private static final Icon TERMINAL_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive"));
	private static final Icon TERMINAL_GO_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_go"));
	private static final Icon TERMINAL_GO_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_go"));
	private static final Icon INFORMATION_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));
	private static final Icon INFORMATION_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));

	private static final String ABOUT_INFO = "JMRTD is brought to you by the JMRTD team!\nVisit http://jmrtd.org/ for more information.";

	private static final Provider PROVIDER =
		new org.bouncycastle.jce.provider.BouncyCastleProvider();

	private Container contentPane;
	private BACStore bacStore;

	/**
	 * Constructs the GUI.
	 *
	 * @param arg command line arguments, are ignored for now.
	 */
	public PassportApp() {
		try {
			Security.insertProviderAt(PROVIDER, 4);
			PassportManager pm = PassportManager.getInstance();
			pm.addPassportListener(this);
			CardManager cm = CardManager.getInstance();
			cm.stop();
			this.bacStore =  new BACStore();
			BACStorePanel bacStorePanel = new BACStorePanel(bacStore);
			final JFrame mainFrame = new JFrame(MAIN_FRAME_TITLE);
			mainFrame.setIconImage(JMRTD_ICON);
			contentPane = mainFrame.getContentPane();
			contentPane.setLayout(new BorderLayout());

			JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.addTab("BAC", bacStorePanel);
			tabbedPane.addTab("Terminals", new PassportManagerPanel());
			contentPane.add(tabbedPane, BorderLayout.CENTER);

			final MRZKeyListener keySource = new MRZKeyListener(bacStorePanel);
			addMRZKeyListener(mainFrame, keySource);
			JMenuBar menuBar = new JMenuBar();
			menuBar.add(createFileMenu());
			menuBar.add(createTerminalMenu());
			menuBar.add(createHelpMenu());
			mainFrame.setJMenuBar(menuBar);
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			mainFrame.pack();
			mainFrame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
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

	private void readPassport(PassportService service) throws CardServiceException {
		if (APDU_DEBUG) {
			service.addAPDUListener(new APDUListener() {
				public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
					System.out.println("DEBUG: capdu = " + Hex.bytesToHexString(capdu.getBytes()));
					System.out.println("DEBUG: rapdu = " + Hex.bytesToHexString(rapdu.getBytes()));
				}
			});
		}
		service.open();
		boolean isBACPassport = false;
		boolean isBACAuthenticated = false;
		try {
			CardFileInputStream comIn = service.readFile(PassportService.EF_COM);
			COMFile com = new COMFile(comIn);
			isBACPassport = false;
		} catch (CardServiceException cse) {
			isBACPassport = true;
		}
		if (isBACPassport) {
			int tries = 10;
			List<BACEntry> triedBACEntries = new ArrayList<BACEntry>();
			try {
				while (!isBACAuthenticated && tries-- > 0) {
					for (BACEntry entry: bacStore.getEntries()) {
						try {
							if (!triedBACEntries.contains(entry)) {
								String documentNumber = entry.getDocumentNumber();
								Date dateOfBirth = entry.getDateOfBirth();
								Date dateOfExpiry = entry.getDateOfExpiry();
								service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
								/* NOTE: if authentication was ok, performedBAC will be called back. */
								isBACAuthenticated = true;
								break; /* out of for loop */
							}
							Thread.sleep(500);
						} catch (CardServiceException cse) {
							/* NOTE: BAC failed? Try next BACEntry */
						}
					}
				}
			} catch (InterruptedException ie) {
				/* NOTE: Interrupted? leave loop. */
			}
		}
		if (!isBACPassport) {
			sessionStarted(service, false);
		} else if (isBACAuthenticated) {
			sessionStarted(service, true);
		} else {
			/* Passport requires BAC, but we failed to authenticate. */
			APDUFingerprint fp = new APDUFingerprint(service);
			String message = "Cannot get access to passport.";
			Properties properties = fp.guessProperties();
			message += "\nFingerprint information: \"" + properties + "\"";
			JOptionPane.showMessageDialog(contentPane, message, "Basic Access denied!", JOptionPane.INFORMATION_MESSAGE, null);
		}
	}

	public void passportRemoved(PassportEvent ce) {
		sessionStopped(ce.getService());
	}

	private void sessionStarted(PassportService service, boolean authenticated) throws CardServiceException {
		PassportFrame passportFrame = new PassportFrame();
		passportFrame.readFromService(service, authenticated);
	}

	private void sessionStopped(PassportService service) {
		if (service != null) {
			service.close();
		}
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* New... */
		JMenuItem newItem = new JMenuItem();
		fileMenu.add(newItem);
		newItem.setAction(getNewAction());

		/* Open... */
		JMenuItem openItem = new JMenuItem();
		fileMenu.add(openItem);
		openItem.setAction(getOpenAction());

		fileMenu.addSeparator();

		/* Exit */
		JMenuItem closeItem = new JMenuItem();
		fileMenu.add(closeItem);
		closeItem.setAction(getExitAction());

		return fileMenu;
	}

	private JMenu createTerminalMenu() {
		JMenu terminalMenu = new JMenu("Tools");
		CardManager cm = CardManager.getInstance();

		Collection<CardTerminal> terminals = cm.getTerminals();
		for (CardTerminal terminal: terminals) {
			JMenuItem menuItem = new JMenuItem(getUseTerminalAction(terminal));
			terminalMenu.add(menuItem);
		}
		return terminalMenu;
	}

	private JMenu createHelpMenu() {
		JMenu helpMenu = new JMenu("Help");
		JMenuItem aboutItem = new JMenuItem();
		aboutItem.setAction(getAboutAction());
		helpMenu.add(aboutItem);
		return helpMenu;
	}

	private Action getNewAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("DEBUG: TODO: new action");
				PassportFrame passportFrame = new PassportFrame();
				passportFrame.createEmptyPassport();
				passportFrame.pack();
				passportFrame.setVisible(true);
			}
		};
		action.putValue(Action.SMALL_ICON, NEW_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, NEW_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Create new passport");
		action.putValue(Action.NAME, "New");
		return action;
	}

	private Action getOpenAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(new FileFilter() {
					public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith("zip") || f.getName().endsWith("ZIP"); }
					public String getDescription() { return "ZIP archives"; }				
				});
				int choice = fileChooser.showOpenDialog(contentPane);
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						PassportFrame passportFrame = new PassportFrame();
						passportFrame.readFromZipFile(file);
						passportFrame.pack();
						passportFrame.setVisible(true);
					} catch (IOException ioe) {
						/* NOTE: Do nothing. */
					}
					break;
				default: break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, OPEN_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, OPEN_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Read passport from file");
		action.putValue(Action.NAME, "Open File...");
		return action;
	}

	private Action getExitAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		};
		action.putValue(Action.SMALL_ICON, EXIT_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, EXIT_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Exit application");
		action.putValue(Action.NAME, "Exit");
		return action;
	}

	private Action getUseTerminalAction(final CardTerminal terminal) {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					public void run() {
						try {
							PassportService service = new PassportService(new TerminalCardService(terminal));
							readPassport(service);
						} catch (CardServiceException cse) {
							cse.printStackTrace();
						}
					}
				}).start();

			}	
		};
		action.putValue(Action.SMALL_ICON, TERMINAL_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, TERMINAL_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Read card from " + terminal.getName());
		action.putValue(Action.NAME, "Read from " + terminal.getName());
		return action;
	}

	private Action getAboutAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				try {
					JTextArea area = new JTextArea(20, 35);
					URL readMeFile = new URL(Files.getBaseDir() + "/README");
					BufferedReader in = new BufferedReader(new InputStreamReader(readMeFile.openStream()));
					while (true) {
						String line = in.readLine();
						if (line == null) { break; }
						line.trim();
						area.append("  " + line);
						area.append("\n");
					}
					area.setCaretPosition(0);
					area.setEditable(false);
					JOptionPane.showMessageDialog(contentPane, new JScrollPane(area), "About JMRTD", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(Icons.getImage("jmrtd")));
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(contentPane, ABOUT_INFO, "About JMRTD", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(Icons.getImage("jmrtd")));
				}
			}
		};
		action.putValue(Action.SMALL_ICON, INFORMATION_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, INFORMATION_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "About this application");
		action.putValue(Action.NAME, "About...");
		return action;
	}

	/**
	 * Main method creates an instance.
	 *
	 * @param arg command line arguments.
	 */
	public static void main(String[] arg) {
		new PassportApp();
	}
}
