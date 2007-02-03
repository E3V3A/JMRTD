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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.gui.ImagePanel;
import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.DG2File;
import sos.mrtd.FaceInfo;
import sos.mrtd.PassportFile;
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
   
   private FaceInfo info;
   private ImagePanel ipanel;
   private JButton readButton;
   private JTextArea infoArea;
   private CardService service;
   private SecureMessagingWrapper wrapper;

   public FacePanel(CardService service) {
      super(new BorderLayout());
      this.service = service;
      this.wrapper = null;
      JPanel buttonPanel = new JPanel(new FlowLayout());
      readButton = new JButton("Read from DG2");
      readButton.addActionListener(this);
      JCheckBox featureCheckBox = new JCheckBox();
      featureCheckBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            JCheckBox cb = (JCheckBox)ae.getSource();
            FaceInfo.FeaturePoint[] featurePoints = info.getFeaturePoints();
            if (cb.isSelected()) {
               for (int i = 0; i < featurePoints.length; i++) {
                  FaceInfo.FeaturePoint p = featurePoints[i];
                  String key = Integer.toString(p.getMajorCode())
                     + "."+ Integer.toString(p.getMinorCode());
                  ipanel.highlightPoint(key, p.getX(), p.getY());
               }
            } else {
               for (int i = 0; i < featurePoints.length; i++) {
                  FaceInfo.FeaturePoint p = featurePoints[i];
                  String key = Integer.toString(p.getMajorCode())
                     + "." + Integer.toString(p.getMinorCode());
                  ipanel.deHighlightPoint(key);
               } 
            }
            repaint();
         }
      });
      ipanel = new ImagePanel();
      // buttonPanel.add(showButton);
      // buttonPanel.add(hideButton);
      buttonPanel.add(readButton);
      buttonPanel.add(new JLabel("FP: "));
      buttonPanel.add(featureCheckBox);
      buttonPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Face"));
      infoArea = new JTextArea(5, 5);
      infoArea.setEditable(false);
      JPanel westPanel = new JPanel(new GridLayout(2,1));
      westPanel.add(buttonPanel);
      westPanel.add(new JScrollPane(infoArea));
      add(westPanel, BorderLayout.WEST);
      add(new JScrollPane(ipanel), BorderLayout.CENTER);
   }

   public void actionPerformed(ActionEvent ae) {
      ipanel.clearImage();
      (new Thread(this)).start();
   }

   public void run() {
      try {
         readButton.setEnabled(false);
         PassportService s = new PassportService(service);
         s.setWrapper(wrapper);
         DG2File dg2 = (DG2File)s.readDataGroup(PassportFile.EF_DG2_TAG);
         info = dg2.getFaces().get(0);
         ipanel.setImage(info.getImage());
         infoArea.setText(info.toString());
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
