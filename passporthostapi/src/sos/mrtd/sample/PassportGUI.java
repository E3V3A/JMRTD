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
 * $Id$
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.Provider;
import java.security.Security;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.mrtd.PassportApduService;
import sos.mrtd.SecureMessagingWrapper;
import sos.smartcards.APDUIOService;
import sos.smartcards.JCOPEmulatorService;
import sos.smartcards.JPCSCService;

/**
 * Simple graphical application for experimenting with the passport
 * apdu service.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class PassportGUI extends JPanel
{
   /* Default passport... */
   public static String DEFAULT_DOC_NR;
   public static String DEFAULT_DATE_OF_BIRTH;
   public static String DEFAULT_DATE_OF_EXPIRY;

   private static final Provider PROVIDER =
      new org.bouncycastle.jce.provider.BouncyCastleProvider();

   private static final byte[] ZERO_DATA = new byte[256];

   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

   private PassportApduService service;
   private APDULogPanel log;
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
          Security.insertProviderAt(PROVIDER, 4);
          
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
              DEFAULT_DOC_NR = "XX0001328";
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
               service.open(terminal);
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

         JTabbedPane tabbedPane = new JTabbedPane();
         BACPanel bacPanel = new BACPanel(service);
         APDUSenderPanel apduSenderPanel = new APDUSenderPanel(service);
         LDSPanel ldsPanel = new LDSPanel(service);
         FacePanel facePanel = new FacePanel(service);
         PAPanel paPanel = new PAPanel(service);
         AAPanel aaPanel = new AAPanel(service);
         InitPassportPanel initPanel = new InitPassportPanel(service);
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
      PassportGUI gui = new PassportGUI(arg);
      JFrame frame = new JFrame("PassportGUI");
      Container cp = frame.getContentPane();
      cp.add(gui);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
   }
}

