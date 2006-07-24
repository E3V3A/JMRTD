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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.gui.ImagePanel;
import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.FaceInfo;
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
   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
   
   private ImagePanel ipanel;
   private JButton readButton;
   private CardService service;
   private SecureMessagingWrapper wrapper;

   public FacePanel(CardService service) {
      super(new BorderLayout());
      this.service = service;
      this.wrapper = null;
      JPanel buttonPanel = new JPanel(new FlowLayout());
      readButton = new JButton("Read from DG2");
      readButton.addActionListener(this);
      ipanel = new ImagePanel();
      // buttonPanel.add(showButton);
      // buttonPanel.add(hideButton);
      buttonPanel.add(readButton);
      buttonPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Face"));
      add(buttonPanel, BorderLayout.WEST);
      add(new JScrollPane(ipanel), BorderLayout.CENTER);
   }

   public void actionPerformed(ActionEvent ae) {
      ipanel.clearImage();
      (new Thread(this)).start();
   }

   public void run() {
      try {
         readButton.setEnabled(false);
         PassportService s = new PassportService(service, wrapper);
         FaceInfo info = s.readFace()[0];
         ipanel.setImage(info.getImage());
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
