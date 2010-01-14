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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

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
import javax.swing.filechooser.FileFilter;

import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;
import net.sourceforge.scuba.util.Files;
import net.sourceforge.scuba.util.Icons;

import org.jmrtd.BACKey;
import org.jmrtd.BACStore;
import org.jmrtd.COMFile;
import org.jmrtd.CSCAStore;
import org.jmrtd.PassportEvent;
import org.jmrtd.PassportListener;
import org.jmrtd.PassportManager;
import org.jmrtd.PassportService;
import org.jmrtd.CVCAStore;

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

	public static final File JMRTD_USER_DIR = new File(new File(System.getProperty("user.home")), ".jmrtd");
	private static final File PREFERENCES_FILE = new File(JMRTD_USER_DIR, "jmrtd.properties");

	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48", JMRTDApp.class);
	private static final Icon NEW_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("lightning"));
	private static final Icon OPEN_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder"));
	private static final Icon EXIT_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("door_out"));
	private static final Icon RELOAD_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("arrow_rotate_clockwise"));
	private static final Icon PREFERENCES_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("wrench"));
	private static final Icon INFORMATION_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));

	private static final String ABOUT_INFO = "JMRTD is brought to you by the JMRTD team!\nVisit http://jmrtd.org/ for more information.";

	private static final Provider PROVIDER =
		new org.bouncycastle.jce.provider.BouncyCastleProvider();

	private Container contentPane;
	private CardManager cardManager;
	private PreferencesPanel preferencesPanel;
	private BACStore bacStore;
	private CSCAStore cscaStore;
	private CVCAStore cvcaStore;

	/**
	 * Constructs the GUI.
	 *
	 * @param arg command line arguments, are ignored for now.
	 */
	public JMRTDApp() {
		try {
			Security.insertProviderAt(PROVIDER, 4);
			PassportManager passportManager = PassportManager.getInstance();
			passportManager.addPassportListener(this);
			cardManager = CardManager.getInstance();
			preferencesPanel = new PreferencesPanel(cardManager, PREFERENCES_FILE);
			this.bacStore = new BACStore();
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

	private void readPassport(PassportService service) throws CardServiceException {
		try {
			service.open();
		} catch (Exception e) {
			Object message = "Sorry, " + e.getMessage();
			JOptionPane.showMessageDialog(contentPane, message, "Cannot open passport!", JOptionPane.ERROR_MESSAGE, null);
			return;
		}
		boolean isBACPassport = false;
		BACKey bacEntry = null;
		try {
			CardFileInputStream comIn = service.readFile(PassportService.EF_COM);
			new COMFile(comIn); /* NOTE: EF.COM is read here to test if BAC is implemented */
			isBACPassport = false;
		} catch (CardServiceException cse) {
			isBACPassport = true;
		} catch (IOException e) {
			e.printStackTrace();
			// FIXME: now what?
		}
		if (isBACPassport) {
			int tries = 10;
			List<BACKey> triedBACEntries = new ArrayList<BACKey>();
			try {
				/* NOTE: outer loop, try 10 times all entries (user may be entering new entries meanwhile). */
				while (bacEntry == null && tries-- > 0) {
					/* NOTE: inner loop, loops through stored BAC entries. */
					for (BACKey bacKey: bacStore.getEntries()) {
						try {
							if (!triedBACEntries.contains(bacKey)) {
								service.doBAC(bacKey);
								/* NOTE: if successful, doBAC terminates normally, otherwise exception. */
								bacEntry = bacKey;
								break; /* out of inner for loop */
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
		if (!isBACPassport || bacEntry != null) {
			PassportFrame passportFrame = new PassportFrame(cscaStore, cvcaStore);
			passportFrame.readFromService(service, bacEntry, preferencesPanel.getReadingMode());
		} else {
			/*
			 * Passport requires BAC, but we failed to authenticate.
			 */
			String message = "Cannot get access to passport.";
			JOptionPane.showMessageDialog(contentPane, message, "Basic Access denied!", JOptionPane.INFORMATION_MESSAGE, null);
			return;
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

		/* Terminal Certs... */
		JMenuItem loadItem = new JMenuItem();
		menu.add(loadItem);
		loadItem.setAction(getLoadTerminalCertsAction());

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
				PassportFrame passportFrame = new PassportFrame(cscaStore, cvcaStore);
				passportFrame.readFromEmptyPassport();
				passportFrame.pack();
				passportFrame.setVisible(true);
			}
		};
		action.putValue(Action.SMALL_ICON, NEW_ICON);
		action.putValue(Action.LARGE_ICON_KEY, NEW_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Create new passport");
		action.putValue(Action.NAME, "New");
		return action;
	}

	private Action getOpenAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -9209238098024027906L;

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
						PassportFrame passportFrame = new PassportFrame(cscaStore, cvcaStore);
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
		action.putValue(Action.SMALL_ICON, OPEN_ICON);
		action.putValue(Action.LARGE_ICON_KEY, OPEN_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Read passport from file");
		action.putValue(Action.NAME, "Open File...");
		return action;
	}

	private Action getLoadTerminalCertsAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -3009238098024027906L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setFileFilter(new FileFilter() {
					public boolean accept(File f) { return f.isDirectory(); }
					public String getDescription() { return "Directories"; }               
				});
				int choice = fileChooser.showOpenDialog(contentPane);
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					cvcaStore.setLocation(file);
					break;
				default: break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, OPEN_ICON);
		action.putValue(Action.LARGE_ICON_KEY, OPEN_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Load terminals certificate & key information");
		action.putValue(Action.NAME, "Terminal Certs...");
		return action;
	}

	private Action getExitAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -6229877165532173683L;

			public void actionPerformed(ActionEvent e) {
				System.exit(0);
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
								System.out.println("DEBUG: skipping " + terminal.getName() + ", cannot open because of " + e.toString());
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
				case JOptionPane.OK_OPTION: preferencesPanel.commit(); break;
				default: preferencesPanel.abort();
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
				Image iconImage = null;
				try {
					iconImage = Icons.getImage("jmrtd_logo-100x100", getClass());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				try {
					JTextArea area = new JTextArea(20, 35);
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
					JOptionPane.showMessageDialog(contentPane, new JScrollPane(area), "About JMRTD", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(iconImage));
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(contentPane, ABOUT_INFO, "About JMRTD", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(iconImage));
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
