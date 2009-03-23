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
 * $Id: ManualBACPanel.java 894 2009-03-23 15:50:46Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.crypto.SecretKey;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.swing.HexField;

import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.PassportApduService;
import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;
import org.jmrtd.Util;

/**
 * Convenient GUI component for step-by-step execution of the
 * BAC protocol.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 894 $
 */
public class ManualBACPanel extends JPanel
{
   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

   private BACKeyPanel bacKeyPanel;
   private MutualAuthPanel mutualAuthPanel;
   private SecretKey ksEnc, ksMac;
   private byte[] kICC, kIFD, rndICC, rndIFD;
   private long ssc;

   private PassportApduService apduService;
   private PassportService authService;

   public ManualBACPanel(PassportApduService service) throws CardServiceException {
      super(new GridLayout(3,1));
      setService(service);
      add(new ChallengePanel());
      bacKeyPanel = new BACKeyPanel(BACKeyPanel.SHOW_DERIVED_KEYS);
      add(bacKeyPanel);
      mutualAuthPanel = new MutualAuthPanel();
      add(mutualAuthPanel);
   }
   
   public void setService(PassportApduService service) throws CardServiceException {
	      this.apduService = service;
	      this.authService = new PassportService(service);
   }

   public void addAuthenticationListener(AuthListener l) {
      mutualAuthPanel.addAuthenticationListener(l);
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
         try {
            byte[] tmpRndICC = new byte[8];
            tmpRndICC = apduService.sendGetChallenge();
            if (tmpRndICC != null && tmpRndICC.length == 8) {
               rndICC = tmpRndICC;
               challengeField.setValue(rndICC);
            }
         } catch (CardServiceException cse) {
            cse.printStackTrace();
         }
      }

      public byte[] getChallenge() {
         return rndICC;
      }
   }

   private class MutualAuthPanel extends JPanel implements ActionListener
   {
      private HexField challengeField, keyField;
      private HexField plaintextField;
      private HexField ksEncTF, ksMacTF, sscTF;
      private Collection<AuthListener> listeners;

      public MutualAuthPanel() {
         super(new BorderLayout());
         listeners = new ArrayList<AuthListener>();
         setBorder(BorderFactory.createTitledBorder(PANEL_BORDER,
               "Mutual Authenticate"));
         JPanel top = new JPanel(new FlowLayout());
         challengeField = new HexField(8);
         keyField = new HexField(16);
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
      
      public void addAuthenticationListener(AuthListener l) {
    	  listeners.add(l);
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
               apduService.sendMutualAuth(rndIFD, rndICC, kIFD,
                     bacKeyPanel.getKEnc(), bacKeyPanel.getKMac());
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
            BACEvent event = new BACEvent(authService, rndICC, rndIFD, kICC, kIFD, true);
            notifyBACPerformed(event);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      
      /**
       * Notifies listeners about BAC events.
       * 
       * @param event BAC event
       */
      protected void notifyBACPerformed(BACEvent event) {
         for (AuthListener l : listeners) {
            l.performedBAC(event);
         }
      }
   }
}
