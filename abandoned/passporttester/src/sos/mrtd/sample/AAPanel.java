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
 * $Id: AAPanel.java 894 2009-03-23 15:50:46Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.PublicKey;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.EACEvent;
import org.jmrtd.PassportApduService;
import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;
import org.jmrtd.lds.DG15File;

/**
 * Convenient GUI component for doing AA.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 894 $
 */
public class AAPanel extends JPanel
implements AuthListener
{
   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

   private JTextArea area;
   private JButton readPubKeyButton, aaButton;

   private PassportApduService apduService;
   private PassportService passportService;
   
   private SecureMessagingWrapper wrapper;
   
   private PublicKey pubkey;

   public AAPanel(PassportApduService service)
   throws CardServiceException {
      super(new BorderLayout());
      setService(service);
      this.wrapper = null;
      JPanel buttonPanel = new JPanel(new FlowLayout());
      readPubKeyButton = new JButton("Read Public Key");
      buttonPanel.add(readPubKeyButton);
      readPubKeyButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               DG15File dg15 =
                  new DG15File(passportService.readFile(PassportService.EF_DG15));
               pubkey = dg15.getPublicKey();
               area.append("Read pubkey = " + pubkey + "\n");
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      aaButton = new JButton("Do AA");
      buttonPanel.add(aaButton);
      aaButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               if (passportService.doAA(pubkey)) {
                  area.append(" --> AA succeeded!\n");
                  return;
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
            area.append(" --> AA failed!\n");
         }
      });
      buttonPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "AA"));
      add(buttonPanel, BorderLayout.WEST);
      area = new JTextArea(20, 30);
      add(new JScrollPane(area), BorderLayout.CENTER);
   }
   
   public void setService(PassportApduService service) throws CardServiceException {
	   this.apduService = service;
	   this.passportService = new PassportService(apduService);	
   }

   public void performedBAC(BACEvent be) {
      this.wrapper = be.getWrapper();
      passportService.setWrapper(wrapper);
   }
   
   public void performedAA(AAEvent ae) {
      area.append("pubkey = " + ae.getPubkey());
      area.append("m1 = " + Hex.bytesToHexString(ae.getM1()));
      area.append("m2 = " + Hex.bytesToHexString(ae.getM2()));
   }

   public void performedEAC(EACEvent ee) {
       this.wrapper = ee.getWrapper();
       passportService.setWrapper(wrapper);
   }
}

