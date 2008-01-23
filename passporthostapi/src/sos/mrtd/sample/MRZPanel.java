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
 * $Id: FacePanel.java 206 2007-03-26 20:19:44Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.DG1File;
import sos.mrtd.MRZInfo;
import sos.mrtd.PassportService;
import sos.mrtd.SecureMessagingWrapper;
import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;

/**
 * GUI component for accessing the MRZ datagroup on the passport.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public class MRZPanel extends JPanel
implements Runnable, ActionListener, AuthListener
{

   private static final Font FONT = new Font("Monospaced", Font.PLAIN, 14);
   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
   private static final SimpleDateFormat SDF = new SimpleDateFormat(
   "MMMM dd, yyyy");

   private MRZInfo info;
   private JTextArea infoArea;
   private JButton readButton;
   private PassportService service;
   private SecureMessagingWrapper wrapper;

   public MRZPanel(CardService service) throws CardServiceException {
      super(new BorderLayout());
      this.service = new PassportService(service);
      JPanel buttonPanel = new JPanel(new FlowLayout());
      readButton = new JButton("Read from DG1");
      readButton.addActionListener(this);
      buttonPanel.add(readButton);
      buttonPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "MRZ"));
      infoArea = new JTextArea(20, 10);
      infoArea.setEditable(false);
      infoArea.setFont(FONT);
      add(buttonPanel, BorderLayout.NORTH);
      add(new JScrollPane(infoArea), BorderLayout.CENTER);
   }

   public void actionPerformed(ActionEvent ae) {
      (new Thread(this)).start();
   }

   public void run() {
      try {
         readButton.setEnabled(false);
         PassportService s = new PassportService(service);
         s.setWrapper(wrapper);
         DG1File dg1 =
            new DG1File(s.readFile(PassportService.EF_DG1));
         info = dg1.getMRZInfo();
         infoArea.setText(info.toString());
         infoArea.append("\n");
         infoArea.append("DocumentType: \"" + info.getDocumentType() + "\"\n");
         infoArea.append("DocumentNumber: \"" + info.getDocumentNumber() + "\"\n");
         infoArea.append("Nationality: \"" + info.getNationality() + "\"\n");
         infoArea.append("IssuingState: \"" + info.getIssuingState() + "\"\n");
         infoArea.append("PersonalNumber: \"" + info.getPersonalNumber() + "\"\n");
         infoArea.append("PrimaryIdentifier: \"" + info.getPrimaryIdentifier() + "\"\n");
         String[] firstNames = info.getSecondaryIdentifiers();
         for (int i = 0; i < firstNames.length; i++) {
            infoArea.append("SecondaryIdentifier " + (i + 1) + ": \"" + firstNames[i] + "\"\n");
         }
         infoArea.append("DateOfBirth: \"" + SDF.format(info.getDateOfBirth()) + "\"\n");
         infoArea.append("DateOfExpiry: \"" + SDF.format(info.getDateOfExpiry()) + "\"\n");
         infoArea.append("Gender: \"" + info.getGender() + "\"\n");
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