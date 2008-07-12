/*
 * $Id $
 */

package sos.mrtd.sample.newgui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.PassportEvent;
import sos.mrtd.PassportListener;
import sos.mrtd.PassportManager;
import sos.mrtd.PassportService;
import sos.smartcards.APDUListener;
import sos.smartcards.CardManager;
import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.util.Files;
import sos.util.Icons;

/**
 * Simple graphical application to demonstrate the
 * JMRTD passport host API.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 308 $
 */
public class PassportApp  implements PassportListener, AuthListener
{  
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
	private static final Icon INFORMATION_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));
	private static final Icon INFORMATION_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("information"));

	/* TODO: read this from a .txt file. */
	private static final String ABOUT_INFO = "JMRTD is brought to you by the JMRTD team!\nVisit http://jmrtd.sourceforge.net/ for more information.";

	private static final Provider PROVIDER =
		new org.bouncycastle.jce.provider.BouncyCastleProvider();

	private Map<CardService, JFrame> serviceToFrameMap;

	private Container contentPane;
	private boolean authenticated;
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
			this.bacStore =  new BACStore();
			serviceToFrameMap = new HashMap<CardService, JFrame>();
			BACStorePanel bacStorePanel = new BACStorePanel(bacStore);
			JFrame mainFrame = new JFrame(MAIN_FRAME_TITLE);
			mainFrame.setIconImage(JMRTD_ICON);
			contentPane = mainFrame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(bacStorePanel, BorderLayout.CENTER);
			JMenuBar menuBar = new JMenuBar();
			menuBar.add(createFileMenu());
			menuBar.add(bacStorePanel.getBACMenu());
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

	public void passportInserted(PassportEvent ce) {
		try {
			PassportService service = ce.getService();
			service.addAPDUListener(new APDUListener() {
				public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
//					System.out.println("DEBUG: capdu = " + Hex.bytesToHexString(capdu.getBytes()));
//					System.out.println("DEBUG: rapdu = " + Hex.bytesToHexString(rapdu.getBytes()));
				}
			});
			service.open();
			service.addAuthenticationListener(this);
			BACStore.BACStoreEntry previousEntry = null;
			while (!authenticated) {
				for (BACStore.BACStoreEntry entry: bacStore.getEntries()) {
					try {
						if (!entry.equals(previousEntry)) {
							String documentNumber = entry.getDocumentNumber();
							String dateOfBirth = entry.getDateOfBirth();
							String dateOfExpiry = entry.getDateOfExpiry();
							service.doBAC(documentNumber, dateOfBirth, dateOfExpiry);
						}
					} catch (CardServiceException cse) {
						/* NOTE: BAC failed? Perhaps this passport doesn't support BAC. */
						// cse.printStackTrace();
						// if (!authenticated) { sessionStarted(service); }
						/* TODO: test this... */
					}
					previousEntry = entry;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void passportRemoved(PassportEvent ce) {
		sessionStopped(ce.getService());
	}

	public void performedAA(AAEvent ae) {
	}

	public void performedBAC(BACEvent be) {
		try {
			PassportService service = be.getService();
			authenticated = true;
			sessionStarted(service);
		} catch (CardServiceException cse) {
			cse.printStackTrace();
		}
	}

	private void sessionStarted(PassportService service) throws CardServiceException {
		PassportFrame gui = new PassportFrame();
		gui.readFromService(service, authenticated);
		serviceToFrameMap.put(service, gui);
	}

	private void sessionStopped(PassportService service) {
		// JFrame frame = serviceToFrameMap.get(service);
		if (service != null) {
			service.close();
		}
		// frame.dispose();
		serviceToFrameMap.remove(service);
		authenticated = false;
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* New... */
		JMenuItem newItem = new JMenuItem();
		fileMenu.add(newItem);
		newItem.setAction(new NewAction());

		/* Open... */
		JMenuItem openItem = new JMenuItem();
		fileMenu.add(openItem);
		openItem.setAction(new OpenAction());

		fileMenu.addSeparator();

		/* Exit */
		JMenuItem closeItem = new JMenuItem();
		fileMenu.add(closeItem);
		closeItem.setAction(new ExitAction());

		return fileMenu;
	}

	private JMenu createTerminalMenu() {
		JMenu terminalMenu = new JMenu("Terminals");
		CardManager cm = CardManager.getInstance();
		Collection<CardTerminal> terminals = cm.getTerminals();
		for (CardTerminal terminal: terminals) {
			JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(new TerminalAction(terminal));
			menuItem.setSelected(true);
			terminalMenu.add(menuItem);
		}
		return terminalMenu;
	}

	private JMenu createHelpMenu() {
		JMenu helpMenu = new JMenu("Help");
		JMenuItem aboutItem = new JMenuItem();
		aboutItem.setAction(new AboutAction());
		helpMenu.add(aboutItem);
		return helpMenu;
	}

	private class NewAction extends AbstractAction
	{
		public NewAction() {
			putValue(SMALL_ICON, NEW_SMALL_ICON);
			putValue(LARGE_ICON_KEY, NEW_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Create new passport");
			putValue(NAME, "New");
		}

		public void actionPerformed(ActionEvent e) {
			System.out.println("DEBUG: TODO: new action");
		}
	}

	private class OpenAction extends AbstractAction
	{

		public OpenAction() {
			putValue(SMALL_ICON, OPEN_SMALL_ICON);
			putValue(LARGE_ICON_KEY, OPEN_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Read passport from file");
			putValue(NAME, "Open File...");
		}

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
					PassportFrame gui = new PassportFrame();
					gui.readFromZipFile(file);
					gui.pack();
					gui.setVisible(true);
				} catch (IOException ioe) {
					/* NOTE: Do nothing. */
				}
				break;
			default: break;
			}
		}
	}

	private class ExitAction extends AbstractAction
	{
		public ExitAction() {
			putValue(SMALL_ICON, EXIT_SMALL_ICON);
			putValue(LARGE_ICON_KEY, EXIT_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Exit application");
			putValue(NAME, "Exit");
		}

		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}

	private class TerminalAction extends AbstractAction
	{
		private CardTerminal terminal;

		public TerminalAction(CardTerminal terminal) {
			this.terminal = terminal;
			putValue(SMALL_ICON, TERMINAL_SMALL_ICON);
			putValue(LARGE_ICON_KEY, TERMINAL_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Do something with " + terminal.getName());
			putValue(NAME, terminal.getName());
		}

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if (src instanceof AbstractButton) {
				AbstractButton button = (AbstractButton)src;
				if (button.isSelected()) {
					System.out.println("DEBUG: TODO: terminal action " + terminal + " switched on");
				} else {
					System.out.println("DEBUG: TODO: terminal action " + terminal + " switched off");
				}
			}
		}	
	}

	private class AboutAction extends AbstractAction
	{	
		public AboutAction() {
			putValue(SMALL_ICON, INFORMATION_SMALL_ICON);
			putValue(LARGE_ICON_KEY, INFORMATION_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "About this application");
			putValue(NAME, "About...");
		}

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
