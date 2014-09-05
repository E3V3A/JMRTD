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
 * $Id: FacePanel.java 894 2009-03-23 15:50:46Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ProgressMonitor;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.swing.ImagePanel;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.EACEvent;
import org.jmrtd.PassportApduService;
import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.FaceInfo;

/**
 * GUI component for accessing the face image on the passport.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 894 $
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
   private PassportService passportService;
   private SecureMessagingWrapper wrapper;

   public FacePanel(PassportApduService service) throws CardServiceException {
      super(new BorderLayout());
      setService(service);
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
   
   public void setService(PassportApduService service) throws CardServiceException {
	   this.passportService = new PassportService(service);
   }

   public void actionPerformed(ActionEvent ae) {
      ipanel.clearImage();
      (new Thread(this)).start();

   }

   public void run() {
      try {
         readButton.setEnabled(false);
         passportService.setWrapper(wrapper);
         final InputStream in = passportService.readFile(PassportService.EF_DG2);
         (new Thread(new Runnable() {
            public void run() {
               try {
                  int fileLength = in.available();
                  ProgressMonitor m = new ProgressMonitor(ipanel, "Reading DG2", "[" + 0 + "/" + fileLength + "]", 0, in.available());
                  while (in.available() >  0) {
                     Thread.sleep(200);
                     int bytesRead = fileLength - in.available();
                     m.setProgress(bytesRead);
                     m.setNote("[" + bytesRead + "/" + fileLength + "]");
                  }
               } catch (InterruptedException ie) {
               } catch (Exception e) {
               }
            }
         })).start();
         DG2File dg2 = new DG2File(in);
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

   public void performedEAC(EACEvent ee) {
       this.wrapper = ee.getWrapper();
    }

   public void performedAA(AAEvent ae) {
   }     
}
