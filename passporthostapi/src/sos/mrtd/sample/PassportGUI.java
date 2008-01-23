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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.security.Provider;
import java.security.Security;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import sos.mrtd.PassportApduService;
import sos.mrtd.PassportListener;
import sos.mrtd.PassportManager;
import sos.mrtd.PassportService;
import sos.smartcards.CardEvent;
import sos.smartcards.PCSCCardService;

/**
 * Simple graphical application to demonstrate the
 * JMRTD passport host API.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class PassportGUI extends JPanel implements PassportListener
{  
   public static final File JMRTD_USER_DIR =
      new File(new File(System.getProperty("user.home")), ".jmrtd");
   
   private static final String APPLICATION_NAME = "JMRTD (jmrtd.sourceforge.net)";
   
   private static final Provider PROVIDER =
      new org.bouncycastle.jce.provider.BouncyCastleProvider();

   private PassportApduService service;
   private APDULogPanel log;
   private JLabel blockSizeLabel;
   private JTextField blockSizeText;
   private JTabbedPane tabbedPane;

   /**
    * Constructs the GUI.
    *
    * @param arg command line arguments, are ignored for now.
    */
   public PassportGUI(String[] arg) {
      super(new BorderLayout());
      try {
         Security.insertProviderAt(PROVIDER, 4);
         service = new PassportApduService(new PCSCCardService());

         JPanel northPanel = new JPanel(new FlowLayout());
         blockSizeLabel = new JLabel("   Max. read file block:");
         blockSizeText = new JTextField("255");
         blockSizeText.setEditable(true);
         blockSizeText.setEnabled(true);
         blockSizeText.addCaretListener(new CaretListener(){
            public void caretUpdate(CaretEvent e) {
                JTextField f = (JTextField)e.getSource();
                try {
                  int n = Integer.parseInt(f.getText());
                  PassportService.maxBlockSize = n;
                } catch(NumberFormatException nfe) {
                }
            }             
         });
         northPanel.add(blockSizeLabel);
         northPanel.add(blockSizeText);
         add(northPanel, BorderLayout.NORTH);
         log = new APDULogPanel();
         add(log, BorderLayout.SOUTH);

         tabbedPane = new JTabbedPane();
         ManualBACPanel bacPanel = new ManualBACPanel(service);
         LDSPanel ldsPanel = new LDSPanel(service);
         MRZPanel mrzPanel = new MRZPanel(service);
         FacePanel facePanel = new FacePanel(service);
         PAPanel paPanel = new PAPanel(service);
         AAPanel aaPanel = new AAPanel(service);
         bacPanel.addAuthenticationListener(ldsPanel);
         bacPanel.addAuthenticationListener(mrzPanel);
         bacPanel.addAuthenticationListener(facePanel);
         bacPanel.addAuthenticationListener(paPanel);
         bacPanel.addAuthenticationListener(aaPanel);
         tabbedPane.addTab("BAC", bacPanel);
         tabbedPane.addTab("LDS", ldsPanel);
         tabbedPane.addTab("MRZ", mrzPanel);
         tabbedPane.addTab("Face", facePanel);
         tabbedPane.addTab("PA", paPanel);
         tabbedPane.addTab("AA", aaPanel);
         add(tabbedPane, BorderLayout.CENTER);
         PassportManager.addPassportListener(this);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   public void passportInserted(CardEvent ce) {
      try {
         service.setService(ce.getService());
         service.addAPDUListener(log);
         service.open();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
      setEnabled(true);
   }

   public void passportRemoved(CardEvent ce) {
      if (service != null) {
         service.close();
      }
      setEnabled(false);
   }
   
   public void setEnabled(boolean enabled) {
      super.setEnabled(enabled);
      tabbedPane.setEnabled(enabled);
   }
   
   /**
    * Main method creates a GUI instance and puts it in a frame.
    *
    * @param arg command line arguments.
    */
   public static void main(String[] arg) {
      try {
         PassportGUI gui = new PassportGUI(arg);
         JFrame frame = new JFrame(APPLICATION_NAME);
         frame.getContentPane().add(gui);
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.pack();
         frame.setVisible(true);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
}
