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
 * $Id: CertificateFrame.java 893 2009-03-23 15:43:42Z martijno $
 */

package org.jmrtd.app;

import java.awt.Container;
import java.awt.Image;
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

import net.sourceforge.scuba.swing.CertificatePanel;
import net.sourceforge.scuba.util.Icons;

/**
 * Frame for displaying (and saving to file) a public key certificate.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 893 $
 */
public class CertificateFrame extends JFrame
{
	private static final long serialVersionUID = 8218341538613049952L;

	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48");

	private static final Icon SAVE_AS_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));

	private CertificatePanel certificatePanel;

	public CertificateFrame(Certificate certificate) {
		this("Certificate", certificate);
	}

	public CertificateFrame(String title, Certificate certificate) {
		super(title);
		setIconImage(JMRTD_ICON);

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
		private static final long serialVersionUID = -7143003047380922518L;

		public SaveAsAction() {
			putValue(SMALL_ICON, SAVE_AS_ICON);
			putValue(LARGE_ICON_KEY, SAVE_AS_ICON);
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
		private static final long serialVersionUID = 5279413086163111656L;

		public CloseAction() {
			putValue(SMALL_ICON, CLOSE_ICON);
			putValue(LARGE_ICON_KEY, CLOSE_ICON);
			putValue(SHORT_DESCRIPTION, "Close Window");
			putValue(NAME, "Close");
		}

		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}
}