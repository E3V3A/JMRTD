/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jmrtd.app.util.FileUtil;
import org.jmrtd.cert.CardVerifiableCertificate;

public class TerminalCertificatesDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1694098989850878787L;

	private static final String
	C_ADD = "add",
	C_INSERT = "insert",
	C_REMOVE = "remove",
	C_VIEW = "view",
	C_CLOSE = "close";

	private List<CardVerifiableCertificate> certificates;

	private JList list;

	public TerminalCertificatesDialog(Frame parent, List<CardVerifiableCertificate> certificates, boolean writeMode) {
		super(parent);
		setTitle("Terminal Certificates");
		this.certificates = certificates;
		setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.gridx = 0;
		c.gridy = 0;
		JButton button = null;
		if (writeMode) {
			button = new JButton("Add...");
			button.setEnabled(true);
			button.setActionCommand(C_ADD);
			button.addActionListener(this);
			buttonPanel.add(button, c);

			c.gridx++;
			button = new JButton("Insert...");
			button.setEnabled(true);
			button.setActionCommand(C_INSERT);
			button.addActionListener(this);
			buttonPanel.add(button, c);

			c.gridx++;
			button = new JButton("Remove");
			button.setEnabled(true);
			button.setActionCommand(C_REMOVE);
			button.addActionListener(this);
			buttonPanel.add(button, c);

			c.gridx++;
		}
		button = new JButton("View...");
		button.setEnabled(true);
		button.setActionCommand(C_VIEW);
		button.addActionListener(this);
		buttonPanel.add(button, c);

		c.gridx++;
		button = new JButton("Close");
		button.setEnabled(true);
		button.setActionCommand(C_CLOSE);
		button.addActionListener(this);
		buttonPanel.add(button, c);

		DefaultListModel model = new DefaultListModel();
		try {
			for (CardVerifiableCertificate cert : certificates) {
				model.addElement(cert.getHolderReference().getName());
			}
		} catch (Exception e) {
			/* FIXME: silent?!? */
		}

		list = new JList(model);

		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize(new Dimension(150, 100));

		this.add(listScroller, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);
		setModal(false);
		setSize(new Dimension(450, 200));
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (C_CLOSE.equals(e.getActionCommand())) {
			dispose();
		} else if (C_VIEW.equals(e.getActionCommand())) {
			int index = list.getSelectedIndex();
			if (index != -1) {
				CVCertificateFrame f = new CVCertificateFrame("Terminal Certificate", certificates.get(index));
				f.pack();
				f.setVisible(true);
			}
		} else if (C_REMOVE.equals(e.getActionCommand())) {
			int index = list.getSelectedIndex();
			if (index != -1) {
				DefaultListModel model = ((DefaultListModel)list.getModel());
				model.remove(index);
				certificates.remove(index);
				if (model.size() > 0) {
					if (index == model.getSize()) {
						index--;
					}
					list.setSelectedIndex(index);
					list.ensureIndexIsVisible(index);
				}
			}
		} else if (C_ADD.equals(e.getActionCommand())) {
			actionAdd(list.getModel().getSize());
		} else if (C_INSERT.equals(e.getActionCommand())) {
			actionAdd(list.getSelectedIndex());
		}
	}

	private void actionAdd(int index) {
		CardVerifiableCertificate cert = null;
		String name = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(FileUtil.CV_CERTIFICATE_FILE_FILTER);
		int choice = fileChooser.showOpenDialog(getContentPane());
		switch (choice) {
		case JFileChooser.APPROVE_OPTION:
			try {
				File file = fileChooser.getSelectedFile();
				CertificateFactory cf = CertificateFactory.getInstance("CVC");
				cert = (CardVerifiableCertificate)cf.generateCertificate(new FileInputStream(file));
				name = cert.getHolderReference().getName();
			} catch (Exception ex) {
				ex.printStackTrace();
				/* TODO: handle this somehow */
			}
			break;
		default:
			break;
		}

		if (cert != null) {
			DefaultListModel model = (DefaultListModel)list.getModel();
			if (index == model.getSize() || index == -1) {
				certificates.add(cert);
				model.addElement(name);
			} else {
				certificates.add(index, cert);
				model.add(index, name);
			}
			list.setSelectedIndex(index);
			list.ensureIndexIsVisible(index);
		}
	}
}
