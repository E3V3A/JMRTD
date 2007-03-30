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
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.gui.HexField;
import sos.mrtd.AuthListener;
import sos.mrtd.PassportApduService;
import sos.mrtd.PassportAuthService;
import sos.mrtd.SecureMessagingWrapper;
import sos.mrtd.Util;

/**
 * Convenient GUI component for step-by-step execution of the
 * BAC protocol.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class BACPanel extends JPanel
{
   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

   private SecretKey kEnc, kMac;
   private SecretKey ksEnc, ksMac;
   private byte[] kICC, kIFD, rndICC, rndIFD;
   private long ssc;
   
   private BACDatabase bacDB = new BACDatabase();
   
   private PassportApduService apduService;
   private PassportAuthService authService;
   
   public BACPanel(PassportApduService service) throws GeneralSecurityException {
      super(new GridLayout(3,1));
      this.apduService = service;
      this.authService = new PassportAuthService(service);
      add(new ChallengePanel());
      add(new MRZPanel());
      add(new MutualAuthPanel());
   }
   
   public void addAuthenticationListener(AuthListener l) {
      authService.addAuthenticationListener(l);
   }
   
   private class ChallengePanel extends JPanel implements ActionListener
   {
      private HexField challengeField;

      public ChallengePanel() {
         super(new FlowLayout());
         setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Get Challenge"));
         challengeField = new HexField(8);
         challengeField.setEditable(false);
         JButton challengeButton = new JButton("Get Challenge");
         challengeButton.addActionListener(this);
         add(new JLabel("RND.ICC: "));
         add(challengeField);
         add(challengeButton);
      }

      public void actionPerformed(ActionEvent ae) {
         byte[] tmpRndICC = new byte[8];
         tmpRndICC = apduService.sendGetChallenge();
         if (tmpRndICC != null && tmpRndICC.length == 8) {
            rndICC = tmpRndICC;
            challengeField.setValue(rndICC);
         }
      }
      
      public byte[] getChallenge() {
         return rndICC;
      }
   }
   
   private class MRZPanel extends JPanel implements ActionListener {

      private JTextField docNrTF, dateOfBirthTF, dateOfExpiryTF;
      private HexField kEncTF, kMacTF;

      public MRZPanel() {
         super(new BorderLayout());
         setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "MRZ"));
         JPanel top = new JPanel(new FlowLayout());
         docNrTF = new JTextField(9);
         dateOfBirthTF = new JTextField(6);
         dateOfExpiryTF = new JTextField(6);
         docNrTF.setText(bacDB.getDocumentNumber());
         dateOfBirthTF.setText(bacDB.getDateOfBirth());
         dateOfExpiryTF.setText(bacDB.getDateOfExpiry());
         kEncTF = new HexField(24);
         kEncTF.setEditable(false);
         kMacTF = new HexField(24);
         kMacTF.setEditable(false);
         top.add(new JLabel("Document number: "));
         top.add(docNrTF);
         top.add(new JLabel("Date of birth: "));
         top.add(dateOfBirthTF);
         top.add(new JLabel("Date of expiry: "));
         top.add(dateOfExpiryTF);
         JButton updateButton = new JButton("Derive Keys");
         top.add(updateButton);
         updateButton.addActionListener(this);
         JPanel center = new JPanel(new FlowLayout());
         JPanel bottom = new JPanel(new GridLayout(2, 2));
         bottom.add(new JLabel("K.ENC: ", JLabel.RIGHT));
         bottom.add(kEncTF);
         bottom.add(new JLabel("K.MAC: ", JLabel.RIGHT));
         bottom.add(kMacTF);
         add(top, BorderLayout.NORTH);
         add(center, BorderLayout.CENTER);
         add(bottom, BorderLayout.SOUTH);
      }

      public void actionPerformed(ActionEvent ae) {
         try {
            byte[] keySeed = Util.computeKeySeed(docNrTF.getText(), dateOfBirthTF
                  .getText(), dateOfExpiryTF.getText());
            kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
            kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
            kEncTF.setValue(kEnc.getEncoded());
            kMacTF.setValue(kMac.getEncoded());
            bacDB.addEntry(docNrTF.getText(), dateOfBirthTF.getText(), dateOfExpiryTF.getText());
         } catch (Exception e) {
            kEnc = null;
            kMac = null;
            kEncTF.clearText();
            kMacTF.clearText();
         }
      }
   }
   
   private class MutualAuthPanel extends JPanel implements ActionListener
   {
      private HexField challengeField, keyField;
      private HexField plaintextField;
      private HexField ksEncTF, ksMacTF, sscTF;
      
      public MutualAuthPanel() {
         super(new BorderLayout());
         setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
               "Mutual Authenticate"));
         JPanel top = new JPanel(new FlowLayout());
         challengeField = new HexField(8);
         // challengeField.setValue(Hex.hexStringToBytes("781723860C06C226"));
         keyField = new HexField(16);
         //  keyField.setValue(Hex
         //        .hexStringToBytes("0B795240CB7049B01C19B33E32804F0B"));
         JButton authButton = new JButton("Mutual Authenticate");
         authButton.addActionListener(this);
         top.add(new JLabel("RND.IFD: "));
         top.add(challengeField);
         top.add(new JLabel("K.IFD: "));
         top.add(keyField);
         top.add(authButton);
         JPanel center = new JPanel(new FlowLayout());
         plaintextField = new HexField(32);
         plaintextField.setEditable(false);
         center.add(new JLabel("[E.ICC]: "));
         center.add(plaintextField);
         JPanel bottom = new JPanel(new GridLayout(3, 2));
         ksEncTF = new HexField(24);
         ksEncTF.setEditable(false);
         ksMacTF = new HexField(24);
         ksMacTF.setEditable(false);
         sscTF = new HexField(8);
         sscTF.setEditable(false);
         bottom.add(new JLabel("KS.ENC: ", JLabel.RIGHT));
         bottom.add(ksEncTF);
         bottom.add(new JLabel("KS.MAC: ", JLabel.RIGHT));
         bottom.add(ksMacTF);
         bottom.add(new JLabel("SSC: ", JLabel.RIGHT));
         bottom.add(sscTF);
         add(top, BorderLayout.NORTH);
         add(center, BorderLayout.CENTER);
         add(bottom, BorderLayout.SOUTH);
      }

      /**
       * FIXME:
       *    preallocate kIFD, kICC, rndIFD, rndICC and copy here from TFs
       *    to prevent allocate & gc.
       */
      public void actionPerformed(ActionEvent ae) {
         try {
            rndIFD = challengeField.getValue();
            kIFD = keyField.getValue();
            byte[] plaintext =
               apduService.sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
            plaintextField.setValue(plaintext);
            if (kICC == null || kICC.length < 16) {
               kICC = new byte[16];
            }
            System.arraycopy(plaintext, 16, kICC, 0, 16);
            byte[] keySeed = new byte[16];
            for (int i = 0; i < 16; i++) {
               keySeed[i] = (byte) ((kIFD[i] & 0x000000FF) ^ (kICC[i] & 0x000000FF));
            }
            ksEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
            ksMac = Util.deriveKey(keySeed, Util.MAC_MODE);
            ksEncTF.setValue(ksEnc.getEncoded());
            ksMacTF.setValue(ksMac.getEncoded());
            ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
            sscTF.setValue(ssc);
            SecureMessagingWrapper wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);
            authService.setWrapper(wrapper);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
}
