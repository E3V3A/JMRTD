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
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jmrtd.app.swing.KeyPanel;
import org.jmrtd.app.util.FileUtil;
import org.jmrtd.app.util.IconUtil;
import org.jmrtd.cert.CardVerifiableCertificate;

/**
 * Frame for displaying (and saving to file) a card verifiable public key certificate.
 *
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class CVCertificateFrame extends JMRTDFrame {

	private static final long serialVersionUID = 2118341538613049952L;

	private static final Icon SAVE_AS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("bin"));


	private ActionMap actionMap;
	
	private CVCertificatePanel certificatePanel;

	/**
	 * Constructs a CV certificate frame from a certificate.
	 * 
	 * @param certificate the certificate
	 */
	public CVCertificateFrame(CardVerifiableCertificate certificate) {
		this("CV Certificate", certificate);
	}

	/**
	 * Constructs a CV certificate frame from a certificate.
	 * 
	 * @param title the title to use
	 * @param certificate the certificate
	 */
	public CVCertificateFrame(String title, CardVerifiableCertificate certificate) {
		super(title);

		actionMap = new ActionMap();

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		setJMenuBar(menuBar);

		/* Frame content */
		certificatePanel = new CVCertificatePanel(certificate);
		Container cp = getContentPane();
		cp.add(certificatePanel);
	}

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Save As...*/
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		fileMenu.add(saveAsItem);
		saveAsItem.setAction(getSaveAsAction());

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(getCloseAction());

		return fileMenu;
	}

	/**
	 * Saves the certificate to file.
	 * 
	 * @return an action for <i>SaveAs</i>
	 */
	private Action getSaveAsAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = actionMap.get("SaveAs");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -7143003045680922518L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(FileUtil.CV_CERTIFICATE_FILE_FILTER);
				int choice = fileChooser.showSaveDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
						FileOutputStream out = new FileOutputStream(file);
						out.write(certificatePanel.getCertificate().getEncoded());
						out.flush();
						out.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					break;
				default: break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, SAVE_AS_ICON);
		action.putValue(Action.LARGE_ICON_KEY, SAVE_AS_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Save certificate to file");
		action.putValue(Action.NAME, "Save As...");
		actionMap.put("SaveAs", action);
		return action;
	}

	private Action getCloseAction() {
		Action action = actionMap.get("Close");
		if (action != null) { return action; }
		action = new AbstractAction() {
			private static final long serialVersionUID = 2579413086163111656L;

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		action.putValue(Action.SMALL_ICON, CLOSE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CLOSE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Close Window");
		action.putValue(Action.NAME, "Close");
		actionMap.put("Close", action);
		return action;
	}

	private static String certificateToString(CardVerifiableCertificate certificate) {
		try{
			if (certificate == null) { return "null"; }
			StringBuffer result = new StringBuffer();
			CardVerifiableCertificate cert = (CardVerifiableCertificate)certificate;
			result.append("subject: " );
			result.append(cert.getHolderReference().getName());
			result.append('\n');
			result.append("issuer: ");
			result.append(cert.getAuthorityReference().getName());
			result.append('\n');
			result.append("Not before: " + cert.getNotBefore() + "\n");
			result.append("Not after: " +  cert.getNotAfter()+ "\n");
			return result.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "null";
		}
	}

	private static class CVCertificatePanel extends JPanel
	{
		private static final long serialVersionUID = 2109469067988004311L;

		private CardVerifiableCertificate certificate;
		private JTextArea area;

		/**
		 * Constructs a certificate panel from a certificate.
		 * 
		 * @param certificate the certificate
		 */
		public CVCertificatePanel(CardVerifiableCertificate certificate) {
			super(new BorderLayout());
			try{
				this.certificate = certificate;
				area = new JTextArea(20, 40);
				area.append(certificateToString(certificate));
				area.setEditable(false);
				add(new JScrollPane(area), BorderLayout.CENTER);
				add(new KeyPanel(certificate.getPublicKey()), BorderLayout.SOUTH);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}

		/**
		 * Gets the certificate.
		 * 
		 * @return a certificate
		 */
		public CardVerifiableCertificate getCertificate() {
			return certificate;
		}

		/**
		 * Sets the font.
		 * 
		 * @param font a font
		 */
		public void setFont(Font font) {
			super.setFont(font);
			if (area != null) { area.setFont(font); }
		}
	}
}
