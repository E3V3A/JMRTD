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
 * $Id: APDUSenderPanel.java 894 2009-03-23 15:50:46Z martijno $
 */

package sos.mrtd.sample;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.EACEvent;
import org.jmrtd.SecureMessagingWrapper;

/**
 * Convenient GUI component for sending ranges of APDUs to the passport.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 894 $
 */
public class APDUSenderPanel extends JPanel implements ActionListener, Runnable, AuthListener {

   private static final Border PANEL_BORDER = BorderFactory
   .createEtchedBorder(EtchedBorder.RAISED);

   private CommandAPDUField bApduField, eApduField;

   private JCheckBox smCheckBox;
   private JCheckBox enabledCheckBox;

   private JButton copyButton, sendButton;

   private CardService service;

   private SecureMessagingWrapper wrapper;

   public APDUSenderPanel(CardService service) {
      super(new GridLayout(3, 1));
      setService(service);
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

      endPanel.add(eApduField);

      JPanel controlPanel = new JPanel(new FlowLayout());
      enabledCheckBox = new JCheckBox();
      enabledCheckBox.setSelected(true);
      controlPanel.add(new JLabel("Enable end: "));
      controlPanel.add(enabledCheckBox);
      copyButton = new JButton("Copy");
      copyButton.addActionListener(this);
      controlPanel.add(copyButton);
      enabledCheckBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            eApduField.setEnabled(enabledCheckBox.isSelected());
            copyButton.setEnabled(enabledCheckBox.isSelected());
         }
      });

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
   
   public void setService(CardService service) {
	      this.service = service;
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
      try {
         sendButton.setEnabled(false);
         CommandAPDU bApdu = bApduField.getAPDU();
         boolean isWrapped = smCheckBox.isSelected();
         if (enabledCheckBox.isSelected()) {
            CommandAPDU eApdu = eApduField.getAPDU();
            send(bApdu, eApdu, isWrapped);
         } else {
            send(bApdu, isWrapped);
         }
      } finally {
         sendButton.setEnabled(true);
      }
   }

   private void send(CommandAPDU capdu, boolean isWrapped) {
      if (isWrapped) {
         capdu = wrapper.wrap(capdu);
      }
      try {
         ResponseAPDU rapdu = service.transmit(capdu);
         if (isWrapped) {
            rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
            System.out.println("PLAIN: C: "
                  + Hex.bytesToHexString(capdu.getBytes())
                  + ", R: "
                  + Hex.bytesToHexString(rapdu.getBytes()));
         }
      } catch (CardServiceException cse) {
         System.out.println("failed to send!");
      }
   }

   private void send(CommandAPDU bApdu, CommandAPDU eApdu, boolean isWrapped) {
      /* FIXME: Need to take care of le? */
      byte[] a = bApdu.getBytes();
      byte[] b = eApdu.getBytes();
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
      CommandAPDU apdu;
      if (n > 0) {
         int min = Math.min(a[offset] & 0x000000FF, b[offset] & 0x000000FF);
         int max = Math.max(a[offset] & 0x000000FF, b[offset] & 0x000000FF);

         for (int i = min; i <= max; i++) {
            c[offset] = (byte) i;
            sendAll(a, b, c, offset + 1, isWrapped);
         }
      } else {
         apdu = new CommandAPDU(c);
         send(apdu, isWrapped);
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

