/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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
 * $Id$
 */

package sos.mrtd.sample;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.gui.HexField;
import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.PassportFileService;
import sos.mrtd.PassportInitService;
import sos.mrtd.SecureMessagingWrapper;
import sos.smartcards.CardService;

/**
 * Convenient GUI component for sending initialization commands to the passport.
 * Will only work when communicating with a not-yet-personalized passport
 * applet.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class InitPassportPanel extends JPanel implements ActionListener,
        AuthListener {
    private JButton createFileButton;
    private JButton selectFileButton;
    private JButton selectLocalFileButton;
    private JButton updateBinaryButton;
    private HexField lenField;
    private HexField fidField;
    private File fileToUpload;
    private PassportInitService service;
    private SecureMessagingWrapper wrapper;
    private JButton personalisationButton;
    private JTextField docNrField;
    private JTextField dobField;
    private JTextField doeField;
    private JButton generateKeyPairButton;
    private JButton uploadPrivateKey;
    private JButton uploadPublicKey;
    private JButton lockButton;
    
    private KeyPair keyPair;

    private static final Border PANEL_BORDER = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

    public InitPassportPanel(CardService service)
            throws GeneralSecurityException, UnsupportedEncodingException {
        super(new GridLayout(3, 1));

        JPanel personalisationPanel = new JPanel(new FlowLayout());
        personalisationPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
                                                                        "Set MRZ details"));
        JPanel fileSendingPanel = new JPanel(new FlowLayout());
        fileSendingPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
                                                                    "Upload raw passport data"));
        JPanel initAAPanel = new JPanel(new FlowLayout());
        initAAPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
                                                               "Generate Active Authentication keypair"));

        add(personalisationPanel);
        add(fileSendingPanel);
        add(initAAPanel);

        this.service = new PassportInitService(service);
        this.wrapper = null;

        selectLocalFileButton = new JButton("Select local file ... ");
        createFileButton = new JButton("Create file");
        selectFileButton = new JButton("Select file");
        updateBinaryButton = new JButton("Update binary");
        selectLocalFileButton.addActionListener(this);
        createFileButton.addActionListener(this);
        selectFileButton.addActionListener(this);
        updateBinaryButton.addActionListener(this);
        fidField = new HexField(2);
        lenField = new HexField(2);
        fileSendingPanel.add(selectLocalFileButton);
        fileSendingPanel.add(new JLabel("file: "));
        fileSendingPanel.add(fidField);
        fileSendingPanel.add(new JLabel("length:"));
        fileSendingPanel.add(lenField);
        fileSendingPanel.add(createFileButton);
        fileSendingPanel.add(selectFileButton);
        fileSendingPanel.add(updateBinaryButton);

        personalisationButton = new JButton("Personalize");
        personalisationButton.addActionListener(this);
        docNrField = new JTextField(9);
        dobField = new JTextField(6);
        doeField = new JTextField(6);
        docNrField.setText(PassportGUI.DEFAULT_DOC_NR);
        dobField.setText(PassportGUI.DEFAULT_DATE_OF_BIRTH);
        doeField.setText(PassportGUI.DEFAULT_DATE_OF_EXPIRY);
        lockButton = new JButton("Lock");
        lockButton.addActionListener(this);

        personalisationPanel.add(new JLabel("Document number:"));
        personalisationPanel.add(docNrField);
        personalisationPanel.add(new JLabel("Date of birth:"));
        personalisationPanel.add(dobField);
        personalisationPanel.add(new JLabel("Date of expiry:"));
        personalisationPanel.add(doeField);
        personalisationPanel.add(personalisationButton);
        personalisationPanel.add(lockButton);
        
        generateKeyPairButton = new JButton("Generate keypair");
        generateKeyPairButton.addActionListener(this);
        uploadPrivateKey = new JButton("Upload private key");
        uploadPrivateKey.addActionListener(this);
        uploadPublicKey = new JButton("Upload public key (DG15)");
        uploadPublicKey.addActionListener(this);
        initAAPanel.add(generateKeyPairButton);
        initAAPanel.add(uploadPrivateKey);
        initAAPanel.add(uploadPublicKey);
    }

    public void actionPerformed(ActionEvent ae) {
        JButton butt = (JButton) ae.getSource();

        try {
            if (butt == selectLocalFileButton) {
                pressedSelectLocalFileButton();
            } else if (butt == createFileButton) {
                pressedCreateFileButton();
            } else if (butt == selectFileButton) {
                pressedSelectFileButton();
            } else if (butt == updateBinaryButton) {
                pressedUpdateBinaryButton();
            } else if (butt == personalisationButton) {
                pressedPersonalisationButton();
            } else if (butt == generateKeyPairButton) {
                pressedGenerateKeyPairButton();
            } else if (butt == uploadPrivateKey) {
                pressedUploadPrivateKey();
            } else if (butt == uploadPublicKey) {
                pressedUploadPublicKey();
            } else if (butt == lockButton) {
                pressedLockButton();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pressedLockButton() {
//        new Thread(new Runnable() {
//            public void run() {
                service.lockApplet(wrapper);
//            }
//        });
    }

    private void pressedUploadPublicKey() throws IOException {

        final InputStream DG15 = service.createDG15(keyPair.getPublic());

        new Thread(new Runnable() {
            public void run() {
                try {
                    service.createFile(wrapper,
                                       PassportFileService.EF_DG15,
                                       (short) DG15.available());
                    service.selectFile(wrapper, PassportFileService.EF_DG15);
                    service.writeFile(wrapper,
                                      PassportFileService.EF_DG15,
                                      DG15);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
//    private void saveKey(Key k, File file) 
//    throws IOException {
//        
//        if (file.exists()) {
//            if (!file.canWrite()) {
//                System.out.println("DEBUG: file " + file
//                        + " exists, cannot write.");
//                return;
//            }
//        } else {
//            if (!file.createNewFile()) {
//                System.out.println("DEBUG: cannot create " + file + ".");
//                return;
//            }
//        }
//
//        FileOutputStream fileStream = new FileOutputStream(file);
//        fileStream.write(k.getEncoded());
//        fileStream.close();
//    }

    private void pressedUploadPrivateKey() throws IOException {

        new Thread(new Runnable() {
            public void run() {
                try {
                    service.putPrivateKey(wrapper, keyPair.getPrivate());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void pressedGenerateKeyPairButton() throws GeneralSecurityException, IOException {
        keyPair = PassportInitService.generateAAKeyPair();                
    }

    private void pressedPersonalisationButton() {
        try {
            final byte[] docNr = docNrField.getText().getBytes("ASCII");
            final byte[] dob = dobField.getText().getBytes("ASCII");
            final byte[] doe = doeField.getText().getBytes("ASCII");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        service.putMRZ(docNr, dob, doe);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void pressedUpdateBinaryButton() {
        final byte[] fid = fidField.getValue();

        new Thread(new Runnable() {
            public void run() {
                try {
                    FileInputStream in = new FileInputStream(fileToUpload);
                    service.writeFile(wrapper,
                                      (short) (((fid[0] & 0x000000FF) << 8) | (fid[1] & 0x000000FF)),
                                      in);
                    in.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }).start();
    }

    private void pressedSelectFileButton() {
        final byte[] fid = fidField.getValue();

        new Thread(new Runnable() {
            public void run() {
                try {
                    service.selectFile(wrapper, fid);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void pressedCreateFileButton() {
        final short fid = bytesToShort(fidField.getValue());
        final short len = bytesToShort(lenField.getValue());

        new Thread(new Runnable() {
            public void run() {
                service.createFile(wrapper, fid, len);
            }
        }).start();
    }

    private short bytesToShort(byte[] value) {
        short total = 0;
        for (int i = 0; i < value.length; i++) {
            total = (short) (total << 8);
            total += (value[i] & 0xff);
        }
        return total;
    }

    private void pressedSelectLocalFileButton() throws Exception {
        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select file");
        // chooser.setCurrentDirectory(currentDir);
        chooser.setFileHidingEnabled(false);
        int n = chooser.showOpenDialog(this);
        if (n != JFileChooser.APPROVE_OPTION) {
            System.out.println("DEBUG: select file canceled...");
            return;
        }

        fileToUpload = chooser.getSelectedFile();
        lenField.setValue(fileToUpload.length());
    }

    public void performedBAC(BACEvent be) {
        this.wrapper = be.getWrapper();
    }

    public void performedAA(AAEvent ae) {
    }
}
