package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.crypto.SecretKey;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.gui.HexField;
import sos.mrtd.Util;

public class BACKeyPanel extends JPanel implements ActionListener
{
   private SecretKey kEnc, kMac;
   
   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
   
   private JTextField docNrTF, dateOfBirthTF, dateOfExpiryTF;
   private HexField kEncTF, kMacTF;

   private BACDatabase bacDB = new BACDatabase();

   public BACKeyPanel() {
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
   
   public SecretKey getKEnc() {
      return kEnc;
   }
   
   public SecretKey getKMac() {
      return kMac;
   }

}
