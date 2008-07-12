package sos.mrtd.sample.newgui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import sos.gui.KeyPanel;
import sos.util.Icons;

public class KeyFrame extends JFrame
{
	private static final Icon SAVE_AS_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon SAVE_AS_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon CLOSE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));

	private KeyPanel keyPanel;

	public KeyFrame(Key key) {
		this("Key", key);
	}

	public KeyFrame(String title, Key key) {
		super(title);

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		setJMenuBar(menuBar);

		/* Frame content */
		keyPanel = new KeyPanel(key);
		Container cp = getContentPane();
		cp.add(keyPanel);
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Save As...*/
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		fileMenu.add(saveAsItem);
		saveAsItem.setAction(new SaveAsAction());

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(new CloseAction());

		return fileMenu;
	}

	/**
	 * Saves the key in DER format.
	 * 
	 * Use <code>openssl rsa -pubin -inform DER -in &lt;file&gt;</code>
	 * to print the resulting file.
	 */
	private class SaveAsAction extends AbstractAction
	{
		public SaveAsAction() {
			putValue(SMALL_ICON, SAVE_AS_SMALL_ICON);
			putValue(LARGE_ICON_KEY, SAVE_AS_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Save key to file");
			putValue(NAME, "Save As...");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory()
					|| f.getName().endsWith("cer") || f.getName().endsWith("CER")
					|| f.getName().endsWith("der") || f.getName().endsWith("DER")
					|| f.getName().endsWith("x509") || f.getName().endsWith("X509")
					|| f.getName().endsWith("pkcs8") || f.getName().endsWith("PKCS8"); }
				public String getDescription() { return "Key files"; }				
			});
			int choice = fileChooser.showSaveDialog(getContentPane());
			switch (choice) {
			case JFileChooser.APPROVE_OPTION:
				try {
					File file = fileChooser.getSelectedFile();
					FileOutputStream out = new FileOutputStream(file);
					/* FIXME: This is DER encoding? */
					out.write(keyPanel.getKey().getEncoded());
					out.flush();
					out.close();
				} catch (IOException fnfe) {
					fnfe.printStackTrace();
				}
				break;
			default: break;
			}
		}
	}

	private class CloseAction extends AbstractAction
	{
		public CloseAction() {
			putValue(SMALL_ICON, CLOSE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, CLOSE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Close Window");
			putValue(NAME, "Close");
		}

		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}
}
