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

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.PublicKey;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.SecureMessagingWrapper;
import sos.smartcards.Apdu;
import sos.smartcards.CardService;
import sos.util.Hex;

/**
 * Convenient GUI component for sending ranges of APDUs to the passport.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class APDUSenderPanel extends JPanel implements ActionListener, Runnable, AuthListener {

   private static final Border PANEL_BORDER = BorderFactory
         .createEtchedBorder(EtchedBorder.RAISED);

   private CommandAPDUField bApduField, eApduField;

   private JCheckBox smCheckBox;

   private JButton copyButton, sendButton;

   private CardService service;

   private SecureMessagingWrapper wrapper;

   public APDUSenderPanel(CardService service) {
      super(new GridLayout(3, 1));
      this.service = service;
      this.wrapper =  null;
      JPanel beginPanel = new JPanel(new FlowLayout());
      beginPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
            "Begin APDU"));
      bApduField = new CommandAPDUField();
      beginPanel.add(bApduField);

      JPanel endPanel = new JPanel(new FlowLayout());
      endPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
            "End APDU"));
      eApduField = new CommandAPDUField();
      copyButton = new JButton("Copy");
      copyButton.addActionListener(this);
      endPanel.add(copyButton);
      endPanel.add(eApduField);

      JPanel controlPanel = new JPanel(new FlowLayout());
      smCheckBox = new JCheckBox();
      sendButton = new JButton("Send");
      sendButton.addActionListener(this);
      controlPanel.add(new JLabel("Wrap: "));
      controlPanel.add(smCheckBox);
      controlPanel.add(new JLabel(" Send APDUs: "));
      controlPanel.add(sendButton);

      add(beginPanel);
      add(endPanel);
      add(controlPanel);
   }

   public void actionPerformed(ActionEvent ae) {
      JButton button = (JButton) ae.getSource();

      if (button == copyButton) {
         eApduField.setAPDU(bApduField.getAPDU());
      } else if (button == sendButton) {
         (new Thread(this)).start();
      }
   }

   public void run() {
      sendButton.setEnabled(false);
      Apdu bApdu = bApduField.getAPDU();
      Apdu eApdu = eApduField.getAPDU();
      boolean isWrapped = smCheckBox.isSelected();
      send(bApdu, eApdu, isWrapped);
      sendButton.setEnabled(true);
   }

   private void send(Apdu bApdu, Apdu eApdu, boolean isWrapped) {
      /* FIXME: Need to take care of le? */
      byte[] a = bApdu.getCommandApduBuffer();
      byte[] b = eApdu.getCommandApduBuffer();
      if (a.length != b.length) {
         throw new IllegalArgumentException("APDUs should have same length");
      }
      byte[] c = new byte[a.length];
      for (int i = 0; i < a.length; i++) {
         c[i] = (byte) Math.min(a[i] & 0x000000FF, b[i] & 0x000000FF);
      }
      sendAll(a, b, c, 0, isWrapped);
   }

   private void sendAll(byte[] a, byte[] b, byte[] c, int offset,
         boolean isWrapped) {
      int n = a.length - offset;
      Apdu apdu;
      if (n > 0) {
         int min = Math.min(a[offset] & 0x000000FF, b[offset] & 0x000000FF);
         int max = Math.max(a[offset] & 0x000000FF, b[offset] & 0x000000FF);

         for (int i = min; i <= max; i++) {
            c[offset] = (byte) i;
            sendAll(a, b, c, offset + 1, isWrapped);
         }
      } else {
         apdu = new Apdu(c);
         if (isWrapped) {
            apdu.wrapWith(wrapper);
         }
         byte[] rapdu = service.sendAPDU(apdu);
         if (isWrapped) {
            rapdu = wrapper.unwrap(rapdu, rapdu.length);
            System.out.println("PLAIN TEXT RAPDU: "
                  + Hex.bytesToHexString(rapdu));
         }
      }
   }

   public void performedBAC(BACEvent be) {
      this.wrapper = be.getWrapper(); 
   }

   public void performedAA(AAEvent ae) {
   }
}

