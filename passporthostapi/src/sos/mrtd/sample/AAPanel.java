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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.PassportApduService;
import sos.mrtd.PassportAuthService;
import sos.mrtd.PassportService;
import sos.mrtd.SecureMessagingWrapper;
import sos.util.Hex;

/**
 * Convenient GUI component for doing AA.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class AAPanel extends JPanel
implements AuthListener
{
   private JTextArea area;
   private JButton readPubKeyButton, aaButton;

   private PassportApduService apduService;
   private PassportAuthService authService;
   private PassportService passportService;
   
   private SecureMessagingWrapper wrapper;
   
   private PublicKey pubkey;

   public AAPanel(PassportApduService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      super(new FlowLayout());
      this.apduService = service;
      this.authService = new PassportAuthService(apduService);
      this.passportService = new PassportService(authService);
      this.wrapper = null;
      readPubKeyButton = new JButton("Read Public Key");
      add(readPubKeyButton);
      readPubKeyButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               pubkey = passportService.readAAPublicKey();
               area.append("Read pubkey = " + pubkey + "\n");
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      aaButton = new JButton("Do AA");
      add(aaButton);
      aaButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               if (authService.doAA(pubkey)) {
                  area.append("AA succeeded!\n");
                  return;
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
            area.append("AA failed!\n");
         }
      });
      area = new JTextArea(20, 30);
      add(new JScrollPane(area));
   }
   
   public void performedBAC(BACEvent be) {
      this.wrapper = be.getWrapper();
      authService.setWrapper(wrapper);
      passportService.setWrapper(wrapper);
   }
   
   public void performedAA(AAEvent ae) {
      area.append("pubkey = " + ae.getPubkey());
      area.append("m1 = " + Hex.bytesToHexString(ae.getM1()));
      area.append("m2 = " + Hex.bytesToHexString(ae.getM2()));
   }
}

