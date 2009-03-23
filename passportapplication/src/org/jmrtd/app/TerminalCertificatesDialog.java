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
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sourceforge.scuba.util.Files;

import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CertificateParser;

public class TerminalCertificatesDialog extends JDialog implements
        ActionListener {

    private static final String C_ADD = "add";

    private static final String C_INSERT = "insert";

    private static final String C_REMOVE = "remove";

    private static final String C_VIEW = "view";

    private static final String C_CLOSE = "close";

    private List<CVCertificate> certificates;

    private JList list;

    public TerminalCertificatesDialog(Frame parent,
            List<CVCertificate> certificates, boolean writeMode) {
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
            for (CVCertificate cert : certificates) {
                model.addElement(cert.getCertificateBody().getHolderReference()
                        .getConcatenated());
            }
        } catch (Exception e) {

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
                CVCertificateFrame f = new CVCertificateFrame(
                        "Terminal Certificate", certificates.get(index));
                f.pack();
                f.setVisible(true);
            }
        } else if (C_REMOVE.equals(e.getActionCommand())) {
            int index = list.getSelectedIndex();
            if (index != -1) {
                DefaultListModel model = ((DefaultListModel) list.getModel());
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

        CVCertificate cert = null;
        String name = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(Files.CV_CERTIFICATE_FILE_FILTER);
        int choice = fileChooser.showOpenDialog(getContentPane());
        switch (choice) {
        case JFileChooser.APPROVE_OPTION:
            try {
                File file = fileChooser.getSelectedFile();
                FileInputStream in = new FileInputStream(file);
                byte[] certData = new byte[(int) file.length()];
                int c = in.read();
                int i = 0;
                while (c != -1) {
                    certData[i++] = (byte) c;
                    c = in.read();
                }
                cert = (CVCertificate) CertificateParser
                        .parseCertificate(certData);
                name = cert.getCertificateBody().getHolderReference()
                        .getConcatenated();
            } catch (Exception ex) {
                ex.printStackTrace();
                // TODO: handle this somehow
            }
            break;
        default:
            break;
        }

        if (cert != null) {
            DefaultListModel model = (DefaultListModel) list.getModel();
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
