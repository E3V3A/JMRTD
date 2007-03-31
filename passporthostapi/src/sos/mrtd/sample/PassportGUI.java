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

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import sos.mrtd.PassportApduService;
import sos.smartcards.CardTerminalEvent;
import sos.smartcards.CardTerminalListener;
import sos.smartcards.CardTerminalManager;
import sos.smartcards.PCSCCardService;

/**
 * Simple graphical application to demonstrate the
 * JMRTD passport host API.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public class PassportGUI extends JPanel implements CardTerminalListener
{
   public static final File JMRTD_USER_DIR =
      new File(new File(System.getProperty("user.home")), ".jmrtd"); 
   
   private static final Provider PROVIDER =
      new org.bouncycastle.jce.provider.BouncyCastleProvider();

   private PassportApduService service;
   private APDULogPanel log;
   private JComboBox terminalsComboBox;

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
         terminalsComboBox = new JComboBox();
         String[] terminals = service.getTerminals();
         for (String terminal: terminals) {
            terminalsComboBox.addItem(terminal);
         }
 
         northPanel.add(terminalsComboBox);
         add(northPanel, BorderLayout.NORTH);

         log = new APDULogPanel();
         add(log, BorderLayout.SOUTH);

         JTabbedPane tabbedPane = new JTabbedPane();
         BACPanel bacPanel = new BACPanel(service);
         LDSPanel ldsPanel = new LDSPanel(service);
         FacePanel facePanel = new FacePanel(service);
         PAPanel paPanel = new PAPanel(service);
         AAPanel aaPanel = new AAPanel(service);
         bacPanel.addAuthenticationListener(ldsPanel);
         bacPanel.addAuthenticationListener(facePanel);
         bacPanel.addAuthenticationListener(paPanel);
         bacPanel.addAuthenticationListener(aaPanel);
         tabbedPane.addTab("BAC", bacPanel);
         tabbedPane.addTab("LDS", ldsPanel);
         tabbedPane.addTab("Face", facePanel);
         tabbedPane.addTab("PA", paPanel);
         tabbedPane.addTab("AA", aaPanel);
         add(tabbedPane, BorderLayout.CENTER);
         service.addAPDUListener(log);
         CardTerminalManager.addCardTerminalListener(this);
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
         PassportGUI gui = new PassportGUI(arg);
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

   public void cardInserted(CardTerminalEvent ce) {
      if (service != null) {
         String[] terminals = service.getTerminals();
         String terminal = terminals[terminalsComboBox.getSelectedIndex()];
         service.open(terminal);
         setEnabled(true);
      }
   }

   public void cardRemoved(CardTerminalEvent ce) {
      if (service != null) {
         service.close();
         setEnabled(false);
      }
   }
}
