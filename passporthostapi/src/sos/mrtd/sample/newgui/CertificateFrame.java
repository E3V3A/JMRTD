package sos.mrtd.sample.newgui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import sos.gui.CertificatePanel;
import sos.gui.ImagePanel;
import sos.util.Icons;

/* TODO: implement a frame instead of the dialog!
 *    including a menu bar with menu items:
 *       file -> save as JPEG, PNG, ...,
 *       view -> featurepoints (checkbox menu item)
 */
public class CertificateFrame extends JFrame
{
	private static final Icon SAVE_AS_PEM_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon SAVE_AS_PEM_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon CLOSE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));

	private CertificatePanel certificatePanel;

	public CertificateFrame(Certificate certificate) {
		this("Certificate", certificate);
	}
	
	public CertificateFrame(String title, Certificate certificate) {
		super(title);

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		setJMenuBar(menuBar);

		/* Frame content */
		certificatePanel = new CertificatePanel(certificate);
		Container cp = getContentPane();
		cp.add(certificatePanel);
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Save As...*/
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		fileMenu.add(saveAsItem);
		saveAsItem.setAction(new SaveAsPEMAction());

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(new CloseAction());

		return fileMenu;
	}

	private class SaveAsPEMAction extends AbstractAction
	{
		public SaveAsPEMAction() {
			putValue(SMALL_ICON, SAVE_AS_PEM_SMALL_ICON);
			putValue(LARGE_ICON_KEY, SAVE_AS_PEM_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Save certificate in PEM format");
			putValue(NAME, "Save As PEM...");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith("pem") || f.getName().endsWith("PEM"); }
				public String getDescription() { return "PEM files"; }				
			});
			int choice = fileChooser.showSaveDialog(getContentPane());
			switch (choice) {
			case JFileChooser.APPROVE_OPTION:
				try {
					File file = fileChooser.getSelectedFile();
					throw new IOException("TODO");

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
