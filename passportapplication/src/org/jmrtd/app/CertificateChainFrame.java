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
 * $Id: $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;

import org.jmrtd.app.swing.CertificateChainPanel;

/**
 * Frame for displaying certifcate chains.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 893 $
 */
public class CertificateChainFrame extends JMRTDFrame {

	private static final long serialVersionUID = 8218341538613049952L;

	private ActionMap actionMap;

	private CertificateChainPanel certificatePanel;

	/**
	 * Constructs a certificate chain frame from a certificate.
	 * 
	 * @param certificate the certificate
	 */
	public CertificateChainFrame(Certificate certificate) {
		this("Certificate", Collections.singletonList(certificate), false, false);
	}
	
	/**
	 * Constructs a certificate chain frame from a certificate.
	 * 
	 * @param certificate the certificate
	 * @param isValid whether the chain should be displayed as valid
	 */
	public CertificateChainFrame(Certificate certificate, boolean isValid) {
		this("Certificate", Collections.singletonList(certificate), true, isValid);
	}

	/**
	 * Constructs a certificate chain frame from a certificate.
	 * 
	 * @param title the title to use for the frame
	 * @param certificate the certificate
	 * @param isValid whether the chain should be displayed as valid
	 */
	public CertificateChainFrame(String title, Certificate certificate, boolean isValid) {
		this(title, Collections.singletonList(certificate), true, isValid);		
	}

	/**
	 * Constructs a certificate chain frame from a list of certificates.
	 * 
	 * @param certificates the certificates
	 * @param isValid whether the chain should be displayed as valid
	 */
	public CertificateChainFrame(List<Certificate> certificates, boolean isValid) {
		this("Certificates", certificates, true, isValid);
	}

	/**
	 * Constructs a certificate chain frame from a list of certificates.
	 * 
	 * @param title the title to use for the frame
	 * @param certificates the certificates
	 * @param isValid whether the chain should be displayed as valid
	 */
	public CertificateChainFrame(String title, List<Certificate> certificates, boolean isValid) {
		this("Certificates", certificates, true, isValid);
	}

	/**
	 * Private constructor used by the other constructors.
	 * 
	 * @param title the title to use
	 * @param certificates the certificates
	 * @param showValidity whether to show validity
	 * @param isValid whether to chain is valid
	 */
	private CertificateChainFrame(String title, List<Certificate> certificates, boolean showValidity, boolean isValid) {
		super(title);

		actionMap = new ActionMap();

		/* Menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		setJMenuBar(menuBar);

		JToolBar toolBar = new JToolBar();
		toolBar.add(getSaveAsAction());
		toolBar.add(getCloseAction());

		/* Frame content */
		if (showValidity) {
			certificatePanel = new CertificateChainPanel(certificates, isValid);
		} else {
			certificatePanel = new CertificateChainPanel(certificates);	
		}
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(toolBar, BorderLayout.NORTH);
		cp.add(certificatePanel, BorderLayout.CENTER);
	}

	/* ONLY PRIVATE METHODS BELOW */
	
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
	 * Use <code>openssl x509 -inform DER -in &lt;file&gt;</code>
	 * to print the resulting file.
	 */
	private Action getSaveAsAction() {
		Action action = actionMap.get("SaveAs");
		if (action != null) { return action; }
		action = new AbstractAction() {
			private static final long serialVersionUID = -7143003047380922518L;

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
			private static final long serialVersionUID = 5279413086163111656L;

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
}
