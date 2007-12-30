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
 * $Id: PassportGUI.java 221 2007-03-30 18:24:59Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.Provider;
import java.security.Security;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import sos.mrtd.PassportApduService;
import sos.smartcards.CREFEmulatorService;
import sos.smartcards.CardServiceException;
import sos.smartcards.JCOPEmulatorService;
import sos.smartcards.PCSCCardService;

/**
 * The original simple graphical application for experimenting
 * with the passport apdu service as developed by the SoS group
 * in Nijmegen.
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Engelbert Hubbers (hubbers@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @author Ronny Wichers Schreur (ronny@cs.ru.nl)
 *
 * @version $Revision: 221 $
 */
public class PassportTester extends JPanel
{
   public static final File JMRTD_USER_DIR =
      new File(new File(System.getProperty("user.home")), ".jmrtd"); 
   
   private static final Provider PROVIDER =
      new org.bouncycastle.jce.provider.BouncyCastleProvider();

   private PassportApduService service;
   private APDULogPanel log;
   private JButton openButton, closeButton;
   private JComboBox terminalsComboBox;

   private boolean isDemo;

   /**
    * Constructs the GUI.
    *
    * @param arg command line arguments, are ignored for now.
    */
   public PassportTester(String[] arg) {
      super(new BorderLayout());
      try {
         Security.insertProviderAt(PROVIDER, 4);
         isDemo = (arg != null && arg.length > 0 && (arg[0].equals("demo")));
         if (arg != null && arg.length > 0 && 
               (arg[0].equals("apduio") || arg[0].equals("jcop"))) {
            if(arg[0].equals("apduio")) {
               service = new PassportApduService(new CREFEmulatorService("localhost", 9025));
            } else {
               service = new PassportApduService(new JCOPEmulatorService());
            }
         } else {
            try {
               service = new PassportApduService(new PCSCCardService());
            } catch (NoClassDefFoundError ncdfe) {
                  throw new IllegalStateException("Could not connect to PC/SC layer");
            }
         }

         JPanel northPanel = new JPanel(new FlowLayout());
         terminalsComboBox = new JComboBox();
         String[] terminals = service.getTerminals();
         for (String terminal: terminals) {
            terminalsComboBox.addItem(terminal);
         }
         openButton = new JButton("Open");
         openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               try {
               String[] terminals = service.getTerminals();
               String terminal = terminals[terminalsComboBox.getSelectedIndex()];
               service.open(terminal);
               openButton.setEnabled(false);
               closeButton.setEnabled(true);
               } catch (CardServiceException cse) {
                  cse.printStackTrace();
               }
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

         JTabbedPane tabbedPane = new JTabbedPane();
         BACPanel bacPanel = new BACPanel(service);
         APDUSenderPanel apduSenderPanel = new APDUSenderPanel(service);
         LDSPanel ldsPanel = new LDSPanel(service);
         FacePanel facePanel = new FacePanel(service);
         PAPanel paPanel = new PAPanel(service);
         AAPanel aaPanel = new AAPanel(service);
         EvilPanel evilPanel = new EvilPanel(service);
         PersoPanel initPanel = new PersoPanel(service);
         bacPanel.addAuthenticationListener(apduSenderPanel);
         bacPanel.addAuthenticationListener(ldsPanel);
         bacPanel.addAuthenticationListener(facePanel);
         bacPanel.addAuthenticationListener(paPanel);
         bacPanel.addAuthenticationListener(aaPanel);
         bacPanel.addAuthenticationListener(initPanel);
         tabbedPane.addTab("BAC", bacPanel);
         tabbedPane.addTab("APDU", apduSenderPanel);
         tabbedPane.addTab("LDS", ldsPanel);
         tabbedPane.addTab("Face", facePanel);
         tabbedPane.addTab("PA", paPanel);
         tabbedPane.addTab("AA", aaPanel);
         tabbedPane.addTab("Init", initPanel);
         tabbedPane.addTab("Evil", evilPanel);
         if (isDemo) {
            tabbedPane.setEnabledAt(1, false); // APDU
            tabbedPane.setEnabledAt(2, false); // LDS
            tabbedPane.setEnabledAt(6, false); // Init
            tabbedPane.setEnabledAt(7, false); // Evil
         }
         add(tabbedPane, BorderLayout.CENTER);
         service.addAPDUListener(log);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   /**
    * Main method creates a GUI instance and puts it in a frame.
    *
    * @param arg command line arguments.
    */
   public static void main(String[] arg) {
      try {
         PassportTester gui = new PassportTester(arg);
         JFrame frame = new JFrame("PassportGUI");
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
