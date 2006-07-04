/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, Radboud University
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
 * $Id: PassportGUI.java,v 1.39 2006/06/20 15:27:20 ceesb Exp $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Provider;
import java.security.Security;

import javax.crypto.SecretKey;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.gui.HexField;
import sos.gui.HexViewPanel;
import sos.gui.ImagePanel;
import sos.mrtd.PassportApduService;
import sos.mrtd.PassportFileService;
import sos.mrtd.PassportService;
import sos.mrtd.SecureMessagingWrapper;
import sos.mrtd.Util;
import sos.smartcards.APDUIOService;
import sos.smartcards.Apdu;
import sos.smartcards.JCOPEmulatorService;
import sos.smartcards.JPCSCService;
import sos.util.Hex;

/**
 * Simple graphical application for experimenting with the passport
 * apdu service.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 1.39 $
 */
public class PassportGUI extends JPanel
{
   /* Default passport... */
   private static String DEFAULT_DOC_NR;
   private static String DEFAULT_DATE_OF_BIRTH;
   private static String DEFAULT_DATE_OF_EXPIRY;

   private static final Provider PROVIDER =
      new org.bouncycastle.jce.provider.BouncyCastleProvider();

   private static final byte[] ZERO_DATA = new byte[256];

   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

   private PassportApduService service;
   private APDULogPanel log;

   private SecretKey kEnc, kMac, ksEnc, ksMac;
   private byte[] rndICC, rndIFD, kICC, kIFD;
   private long ssc;

   private SecureMessagingWrapper wrapper;

   private JButton openButton, closeButton;
   private JComboBox terminalsComboBox;

   /**
    * Constructs the GUI.
    *
    * @param arg command line arguments, are ignored for now.
    */
   public PassportGUI(String[] arg) {
      try {
          Security.insertProviderAt(PROVIDER, 2);
          
          if(arg != null && arg.length > 0 && 
        		  (arg[0].equals("apduio") || arg[0].equals("jcop"))) {
        	  if(arg[0].equals("apduio"))
                service = new PassportApduService(new APDUIOService());
        	  else
        		service = new PassportApduService(new JCOPEmulatorService());
              // sample data from icao
              DEFAULT_DOC_NR = "L898902C<";
              DEFAULT_DATE_OF_BIRTH = "690806";
              DEFAULT_DATE_OF_EXPIRY = "940623";
         }
          else {
              service = new PassportApduService(new JPCSCService());
              // Loes's passport
              DEFAULT_DOC_NR = "XX0001027";
              DEFAULT_DATE_OF_BIRTH = "711019";
              DEFAULT_DATE_OF_EXPIRY = "111001";
         }
          
         setLayout(new BorderLayout());

         JPanel northPanel = new JPanel(new FlowLayout());
         terminalsComboBox = new JComboBox();
         String[] terminals = service.getTerminals();
         for (int i = 0; i < terminals.length; i++) {
            terminalsComboBox.addItem(terminals[i]);
         }
         openButton = new JButton("Open");
         openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               String[] terminals = service.getTerminals();
               String terminal = terminals[terminalsComboBox.getSelectedIndex()];
               service.open();
               openButton.setEnabled(false);
               closeButton.setEnabled(true);
            }
         });
         northPanel.add(terminalsComboBox);
         northPanel.add(openButton);
         closeButton = new JButton("Close");
         closeButton.setEnabled(false);
         closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               service.close();
               closeButton.setEnabled(false);
               openButton.setEnabled(true);
            }
         });
         northPanel.add(closeButton);
         add(northPanel, BorderLayout.NORTH);

         log = new APDULogPanel();
         add(log, BorderLayout.SOUTH);

         JPanel bacPanel = new JPanel(new GridLayout(3, 1));
         bacPanel.add(new ChallengePanel());
         bacPanel.add(new MRZPanel());
         bacPanel.add(new MutualAuthPanel());

         JTabbedPane tabbedPane = new JTabbedPane();
         tabbedPane.addTab("BAC", bacPanel);
         tabbedPane.addTab("APDU", new APDUSenderPanel());
         tabbedPane.addTab("LDS", new LDSPanel());
         tabbedPane.addTab("Face", new FacePanel());
         tabbedPane.addTab("Init", new InitPassportPanel());
         add(tabbedPane, BorderLayout.CENTER);
         
         service.addAPDUListener(log);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   private class APDUSenderPanel extends JPanel implements ActionListener, Runnable
   {
      private CommandAPDUField bApduField, eApduField;
      private JCheckBox smCheckBox;
      private JButton copyButton, sendButton;

      public APDUSenderPanel() {
         super(new GridLayout(3,1));
 
         JPanel beginPanel = new JPanel(new FlowLayout());
         beginPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Begin APDU"));
         bApduField = new CommandAPDUField();
         beginPanel.add(bApduField);

         JPanel endPanel = new JPanel(new FlowLayout());
         endPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "End APDU"));
         eApduField = new CommandAPDUField();
         copyButton = new JButton("Copy");
         copyButton.addActionListener(this);
         endPanel.add(copyButton);
         endPanel.add(eApduField);

         JPanel controlPanel = new JPanel(new FlowLayout());
         smCheckBox = new JCheckBox();
         sendButton = new JButton("Send");
         sendButton.addActionListener(this);
         controlPanel.add(new JLabel("Wrap: "));
         controlPanel.add(smCheckBox);
         controlPanel.add(new JLabel(" Send APDUs: "));
         controlPanel.add(sendButton);

         add(beginPanel);
         add(endPanel);
         add(controlPanel);
      }

      public void actionPerformed(ActionEvent ae) {
         JButton button = (JButton)ae.getSource();

         if (button == copyButton) {
            eApduField.setAPDU(bApduField.getAPDU());
         } else if (button == sendButton) {          
            (new Thread(this)).start();           
         }
      }

      public void run() {
         sendButton.setEnabled(false);
         Apdu bApdu = bApduField.getAPDU();
         Apdu eApdu = eApduField.getAPDU();
         boolean isWrapped = smCheckBox.isSelected();
         send(bApdu, eApdu, isWrapped);
         sendButton.setEnabled(true);
      }

      private void send(Apdu bApdu, Apdu eApdu, boolean isWrapped) {
         /* FIXME: Need to take care of le? */
         byte[] a = bApdu.getCommandApduBuffer();
         byte[] b = eApdu.getCommandApduBuffer();
         if (a.length != b.length) {
            throw new IllegalArgumentException("APDUs should have same length");
         }
         byte[] c = new byte[a.length];
         for (int i = 0; i < a.length; i++) {
            c[i] = (byte)Math.min(a[i] & 0x000000FF, b[i] & 0x000000FF);
         }
         sendAll(a, b, c, 0, isWrapped);
      }
      
      private void sendAll(byte[] a, byte[] b, byte[] c, int offset, boolean isWrapped) {
         int n = a.length - offset;
         Apdu apdu;
         if (n > 0) {
            int min = Math.min(a[offset] & 0x000000FF, b[offset] & 0x000000FF);
            int max = Math.max(a[offset] & 0x000000FF, b[offset] & 0x000000FF);

            for (int i = min; i <= max; i++) {
               c[offset] = (byte)i;
               sendAll(a, b, c, offset + 1, isWrapped);
            }
         } else {
            apdu = new Apdu(c);
            if (isWrapped) {
               apdu.wrapWith(wrapper);
            }
            byte[] rapdu = service.sendAPDU(apdu);
            if (isWrapped) {
               rapdu = wrapper.unwrap(rapdu, rapdu.length);
               System.out.println("PLAIN TEXT RAPDU: " + Hex.bytesToHexString(rapdu));
            }
         }
      }
   }

   private class FacePanel extends JPanel implements Runnable, ActionListener
   {
      private ImagePanel ipanel;
      private JFrame iframe;
      private JButton showButton, hideButton, readButton;

      public FacePanel() {
         super(new FlowLayout());
         showButton = new JButton("Show Image");
         showButton.addActionListener(this);
         hideButton = new JButton("Hide Image");
         hideButton.addActionListener(this);
         hideButton.setEnabled(false);
         readButton = new JButton("Read Image");
         readButton.addActionListener(this);
         ipanel = new ImagePanel();
         add(showButton);
         add(hideButton);
         add(readButton);
         iframe = new JFrame("Face");
         Container cp = iframe.getContentPane();
         cp.add(new JScrollPane(ipanel));
         iframe.pack();
      }

      public void actionPerformed(ActionEvent ae) {
         JButton but = (JButton)ae.getSource();
         if (but.getText().startsWith("Show")) {
            iframe.setVisible(true);
            showButton.setEnabled(false);
            hideButton.setEnabled(true);
         } else if (but.getText().startsWith("Hide")) {
            iframe.setVisible(false);
            hideButton.setEnabled(false);
            showButton.setEnabled(true);
         } else if (but.getText().startsWith("Read")) {
            ipanel.clearImage();
            (new Thread(this)).start();
            showButton.setEnabled(false);
            iframe.setVisible(true);
            hideButton.setEnabled(true);
         }
      }

      public void run() {
         try {
            readButton.setEnabled(false);
            PassportService s = new PassportService(service, wrapper);
            BufferedImage img = s.readFace();
            ipanel.setImage(img);
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            readButton.setEnabled(true);
         }
      }     
   }

   private class InitPassportPanel extends JPanel implements ActionListener 
   {
       private JButton createFileButton;
       private JButton selectFileButton;
       private JButton updateBinaryButton;
       private HexField lenField;
       private HexField fidField;
       
       public InitPassportPanel() {
           super(new FlowLayout());
           updateBinaryButton = new JButton("Upload local file ... ");
           createFileButton = new JButton("Create file");
           selectFileButton = new JButton("Select file");
           updateBinaryButton.addActionListener(this);
           createFileButton.addActionListener(this);
           selectFileButton.addActionListener(this);
           fidField = new HexField(2);
           lenField = new HexField(2);
           add(new JLabel("file: "));
           add(fidField);
           add(new JLabel("length:"));
           add(lenField);
           add(createFileButton);
           add(selectFileButton);
           add(updateBinaryButton);
       }
       
       public void actionPerformed(ActionEvent ae) {
           JButton butt = (JButton)ae.getSource();
                     
           try {
           if(butt == updateBinaryButton) {
               pressedUploadButton();
           } else if(butt == createFileButton) {
               pressedCreateFileButton();
           } else if(butt == selectFileButton) {
               pressedSelectFileButton();
           }
           } catch(Exception e) {
               e.printStackTrace();
           }
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
        final byte[] fid = fidField.getValue();
        final byte[] len = lenField.getValue();
        
        new Thread(new Runnable() {
            public void run() {
               service.createFile(wrapper, fid, len);
            }
         }).start();
    }

    private void pressedUploadButton() throws Exception {
           final JFileChooser chooser = new JFileChooser();
           chooser.setDialogTitle("Upload file");
           // chooser.setCurrentDirectory(currentDir);
           chooser.setFileHidingEnabled(false);
           int n = chooser.showOpenDialog(this);
           if (n != JFileChooser.APPROVE_OPTION) {
              System.out.println("DEBUG: select file canceled...");
              return;
           }        
           
           final byte[] fid = fidField.getValue();
           
           new Thread(new Runnable() {
              public void run() {
                 try {
                     BufferedInputStream in = new BufferedInputStream(
                             new FileInputStream(chooser.getSelectedFile()));
                    service.writeFile(wrapper, (short)(((fid[0] & 0x000000FF) << 8)
                          | (fid[1] & 0x000000FF)), in);
                    in.close();
                 } catch (IOException ioe) {
                    ioe.printStackTrace();
                 }
              }
           }).start();
   
        }
   }
   
   private class LDSPanel extends JPanel implements ActionListener
   {
      private HexField fidTF, offsetTF, leTF;
      private HexViewPanel hexviewer;
      private JButton selectButton, readBinaryButton, readNextButton, saveButton;
      short offset;
      int bytesRead;

      public LDSPanel() {
         super(new BorderLayout());
         hexviewer = new HexViewPanel(ZERO_DATA);
         JPanel north = new JPanel(new FlowLayout());
         fidTF = new HexField(2);
         selectButton = new JButton("Select File");
         selectButton.addActionListener(this);
         saveButton = new JButton("Save File");
         saveButton.addActionListener(this);
         north.add(new JLabel("File: "));
         north.add(fidTF);
         north.add(selectButton);
         north.add(saveButton);
         JPanel south = new JPanel(new FlowLayout());
         leTF = new HexField(1);
         leTF.setValue(0xFF);
         offsetTF = new HexField(2);
         readBinaryButton = new JButton("Read Binary");
         readBinaryButton.addActionListener(this);
         readNextButton = new JButton("Read Next");
         readNextButton.addActionListener(this);
         south.add(new JLabel("Offset: "));
         south.add(offsetTF);
         south.add(new JLabel("Length: "));
         south.add(leTF);
         south.add(readBinaryButton);
         south.add(readNextButton);
         add(north, BorderLayout.NORTH);
         add(hexviewer, BorderLayout.CENTER);
         add(south, BorderLayout.SOUTH);
      }

      public void actionPerformed(ActionEvent ae) {
         try {
            JButton but = (JButton)ae.getSource();
            if (but == selectButton) {
               pressedSelectButton();
            } else if (but == readBinaryButton) {
               pressedReadBinaryButton();
            } else if (but == readNextButton) {
               pressedReadNextButton();
            } else if (but == saveButton) {
               pressedSaveButton();
            }
        } catch (Exception e) {
           e.printStackTrace();
        }
      }

      private void pressedSaveButton() throws Exception {
         JFileChooser chooser = new JFileChooser();
         chooser.setDialogTitle("Save file");
         // chooser.setCurrentDirectory(currentDir);
         chooser.setFileHidingEnabled(false);
         int n = chooser.showOpenDialog(this);
         if (n != JFileChooser.APPROVE_OPTION) {
            System.out.println("DEBUG: select file canceled...");
            return;
         }        
         final byte[] fid = fidTF.getValue();
         final File file = chooser.getSelectedFile();
         new Thread(new Runnable() {
            public void run() {
               try {
                  PassportFileService s = new PassportFileService(service, wrapper);
                  byte[] data = s.readFile((short)(((fid[0] & 0x000000FF) << 8) | (fid[1] & 0x000000FF)));
                  OutputStream out = new FileOutputStream(file);
                  out.write(data, 0, data.length);
                  out.close();
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }).start();
      }
      
      private void pressedSelectButton() throws Exception {
         byte[] fid = fidTF.getValue();
         service.sendSelectFile(wrapper, (short)(((fid[0] & 0x000000FF) << 8) | (fid[1] & 0x000000FF)));
      }
      
      private void pressedReadBinaryButton() throws Exception {
         bytesRead = 0;
         int le = leTF.getValue()[0] & 0x000000FF;
         byte[] offsetBytes = offsetTF.getValue();
         offset = (short)(((offsetBytes[0] & 0x000000FF) << 8)
               | (offsetBytes[1] & 0x000000FF));
         byte[] data = service.sendReadBinary(wrapper, offset, le);
         remove(hexviewer);
         hexviewer = new HexViewPanel(data, offset);
         add(hexviewer, BorderLayout.CENTER);
         bytesRead = data.length;
         setVisible(false);
         setVisible(true);
         // repaint();
      }
      
      private void pressedReadNextButton() throws Exception {
         offset += bytesRead;
         offsetTF.setValue(offset & 0x000000000000FFFFL);
         pressedReadBinaryButton();
      }
   }

   private class MRZPanel extends JPanel implements ActionListener
   {
      private JTextField docNrTF, dateOfBirthTF, dateOfExpiryTF;
      private HexField kEncTF, kMacTF;

      public MRZPanel() {
         super(new BorderLayout());
         setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "MRZ"));
         JPanel top = new JPanel(new FlowLayout());
         docNrTF = new JTextField(9);
         dateOfBirthTF = new JTextField(6);
         dateOfExpiryTF = new JTextField(6);
         docNrTF.setText(DEFAULT_DOC_NR);
         dateOfBirthTF.setText(DEFAULT_DATE_OF_BIRTH);
         dateOfExpiryTF.setText(DEFAULT_DATE_OF_EXPIRY);
         kEncTF = new HexField(24); kEncTF.setEditable(false);
         kMacTF = new HexField(24); kMacTF.setEditable(false);
         top.add(new JLabel("Document number: ")); top.add(docNrTF);
         top.add(new JLabel("Date of birth: ")); top.add(dateOfBirthTF);
         top.add(new JLabel("Date of expiry: ")); top.add(dateOfExpiryTF);
         JButton updateButton = new JButton("Derive Keys");
         top.add(updateButton);
         updateButton.addActionListener(this);
         JPanel center = new JPanel(new FlowLayout());
         JPanel bottom = new JPanel(new GridLayout(2, 2));
         bottom.add(new JLabel("K.ENC: ", JLabel.RIGHT)); bottom.add(kEncTF);
         bottom.add(new JLabel("K.MAC: ", JLabel.RIGHT)); bottom.add(kMacTF);
         add(top, BorderLayout.NORTH);
         add(center, BorderLayout.CENTER);
         add(bottom, BorderLayout.SOUTH);
      }

      public void actionPerformed(ActionEvent ae) {
         try {
            byte[] keySeed = Util.computeKeySeed(docNrTF.getText(),
                                                 dateOfBirthTF.getText(),
                                                 dateOfExpiryTF.getText());
            kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
            kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
            kEncTF.setValue(kEnc.getEncoded());
            kMacTF.setValue(kMac.getEncoded());
         } catch (Exception e) {
            kEnc = null;
            kMac = null;
            kEncTF.clearText();
            kMacTF.clearText();
         }
      }
   }

   private class ChallengePanel extends JPanel implements ActionListener
   {
      private HexField challengeField;
 
      public ChallengePanel() {
         super(new FlowLayout());
         setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
                   "Get Challenge"));
         challengeField = new HexField(8);
         challengeField.setEditable(false);
         JButton challengeButton = new JButton("Get Challenge");
         challengeButton.addActionListener(this);
         add(new JLabel("RND.ICC: "));
         add(challengeField);
         add(challengeButton);
      }

      public void actionPerformed(ActionEvent ae) {
         rndICC = service.sendGetChallenge();
         challengeField.setValue(rndICC);
      }
   }

   private class MutualAuthPanel extends JPanel implements ActionListener
   {
      private HexField challengeField, keyField;
      private HexField plaintextField;
      private HexField ksEncTF, ksMacTF, sscTF;

      public MutualAuthPanel() {
         super(new BorderLayout());
         setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
                   "Mutual Authenticate"));
         JPanel top = new JPanel(new FlowLayout());
         challengeField = new HexField(8);
         challengeField.setValue(Hex.hexStringToBytes("781723860C06C226"));
         keyField = new HexField(16);
         keyField.setValue(Hex.hexStringToBytes("0B795240CB7049B01C19B33E32804F0B"));
         JButton authButton = new JButton("Mutual Authenticate");
         authButton.addActionListener(this);
         top.add(new JLabel("RND.IFD: ")); top.add(challengeField);
         top.add(new JLabel("K.IFD: ")); top.add(keyField);
         top.add(authButton);
         JPanel center = new JPanel(new FlowLayout());
         plaintextField = new HexField(32);
         plaintextField.setEditable(false);
         center.add(new JLabel("[E.ICC]: "));
         center.add(plaintextField);
         JPanel bottom = new JPanel(new GridLayout(3,2));
         ksEncTF = new HexField(24);
         ksEncTF.setEditable(false);
         ksMacTF = new HexField(24);
         ksMacTF.setEditable(false);
         sscTF = new HexField(8);
         sscTF.setEditable(false);
         bottom.add(new JLabel("KS.ENC: ", JLabel.RIGHT)); bottom.add(ksEncTF);
         bottom.add(new JLabel("KS.MAC: ", JLabel.RIGHT)); bottom.add(ksMacTF);
         bottom.add(new JLabel("SSC: ", JLabel.RIGHT)); bottom.add(sscTF);
         add(top, BorderLayout.NORTH);
         add(center, BorderLayout.CENTER);
         add(bottom, BorderLayout.SOUTH);
      }

      /**
       * FIXME:
       *    preallocate kIFD, kICC, rndIFD, rndICC and copy here from TFs
       *    to prevent allocate & gc.
       */
      public void actionPerformed(ActionEvent ae) {
         try {
            rndIFD = challengeField.getValue();
            kIFD = keyField.getValue();
            byte[] plaintext =
               service.sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
            plaintextField.setValue(plaintext);
            if (kICC == null || kICC.length < 16) {
               kICC = new byte[16];
            }
            System.arraycopy(plaintext, 16, kICC, 0, 16);
            byte[] keySeed = new byte[16];
            for (int i = 0; i < 16; i++) {
               keySeed[i] =
                  (byte)((kIFD[i] & 0x000000FF) ^ (kICC[i] & 0x000000FF));
            }
            ksEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
            ksMac = Util.deriveKey(keySeed, Util.MAC_MODE);
            ksEncTF.setValue(ksEnc.getEncoded());
            ksMacTF.setValue(ksMac.getEncoded());
            ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
            sscTF.setValue(ssc);
            wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }  

   /**
    * Main method creates a GUI instance and puts it in a frame.
    *
    * @param arg command line arguments.
    */
   public static void main(String[] arg) {
      PassportGUI gui = new PassportGUI(arg);
      JFrame frame = new JFrame("PassportGUI");
      Container cp = frame.getContentPane();
      cp.add(gui);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
   }
}

