package sos.mrtd.sample.newgui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import sos.gui.CertificatePanel;
import sos.util.Icons;

/**
 * Frame for displaying (and saving to file) a public key certificate.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class CertificateFrame extends JFrame
{
	private static final Icon SAVE_AS_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon SAVE_AS_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
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
		saveAsItem.setAction(new SaveAsAction());

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(new CloseAction());

		return fileMenu;
	}

	/**
	 * Saves the certificate to file.
	 * 
	 * Use <code>openssl x509 -inform DER -in &lt;file&gt;</code>
	 * to print the resulting file.
	 */
	private class SaveAsAction extends AbstractAction
	{
		public SaveAsAction() {
			putValue(SMALL_ICON, SAVE_AS_SMALL_ICON);
			putValue(LARGE_ICON_KEY, SAVE_AS_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Save certificate to file");
			putValue(NAME, "Save As...");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory()
					|| f.getName().endsWith("pem") || f.getName().endsWith("PEM")
					|| f.getName().endsWith("cer") || f.getName().endsWith("CER")
					|| f.getName().endsWith("der") || f.getName().endsWith("DER")
					|| f.getName().endsWith("crt") || f.getName().endsWith("CRT")
					|| f.getName().endsWith("cert") || f.getName().endsWith("CERT"); }
				public String getDescription() { return "Certificate files"; }				
			});
			int choice = fileChooser.showSaveDialog(getContentPane());
			switch (choice) {
			case JFileChooser.APPROVE_OPTION:
				try {
					File file = fileChooser.getSelectedFile();
					FileOutputStream out = new FileOutputStream(file);
					/* FIXME: This is DER encoding? */
					out.write(certificatePanel.getCertificate().getEncoded());
					out.flush();
					out.close();
				} catch (CertificateEncodingException cee) {
					cee.printStackTrace();
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
