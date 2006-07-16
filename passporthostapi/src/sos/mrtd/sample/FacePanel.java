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

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import sos.gui.ImagePanel;
import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.PassportService;
import sos.mrtd.SecureMessagingWrapper;
import sos.smartcards.CardService;

/**
 * Convenient GUI component for accessing the face image on the passport.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class FacePanel extends JPanel
implements Runnable, ActionListener, AuthListener
{
   private ImagePanel ipanel;
   private JFrame iframe;
   private JButton showButton, hideButton, readButton;
   private CardService service;
   private SecureMessagingWrapper wrapper;

   public FacePanel(CardService service) {
      super(new FlowLayout());
      this.service = service;
      this.wrapper = null;
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

   public void performedBAC(BACEvent be) {
      this.wrapper = be.getWrapper();
   }

   public void performedAA(AAEvent ae) {
   }     
}
