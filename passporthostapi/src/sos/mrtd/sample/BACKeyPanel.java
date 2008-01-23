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

public class BACKeyPanel extends JPanel
{
   private SecretKey kEnc, kMac;

   final static boolean DONT_SHOW_DERIVED_KEYS = false, SHOW_DERIVED_KEYS = true;

   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

   private JButton deriveKeyButton;
   private JTextField docNrTF, dateOfBirthTF, dateOfExpiryTF;
   private HexField kEncTF, kMacTF;

   private BACDatabase bacDB = new BACDatabase();

   public BACKeyPanel(boolean showDerivedKeys) {
      super(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "BAC Keys"));
      JPanel top = new JPanel(new FlowLayout());
      docNrTF = new JTextField(9);
      dateOfBirthTF = new JTextField(6);
      dateOfExpiryTF = new JTextField(6);
      docNrTF.setText(bacDB.getDocumentNumber());
      dateOfBirthTF.setText(bacDB.getDateOfBirth());
      dateOfExpiryTF.setText(bacDB.getDateOfExpiry());
      top.add(new JLabel("Document number: "));
      top.add(docNrTF);
      top.add(new JLabel("Date of birth: "));
      top.add(dateOfBirthTF);
      top.add(new JLabel("Date of expiry: "));
      top.add(dateOfExpiryTF);
      deriveKeyButton = new JButton("Derive Keys");
      top.add(deriveKeyButton);
      KeyConstructor keyConstructor = new KeyConstructor();
      deriveKeyButton.addActionListener(keyConstructor);
      docNrTF.addActionListener(keyConstructor);
      dateOfBirthTF.addActionListener(keyConstructor);
      dateOfExpiryTF.addActionListener(keyConstructor);
      JPanel center = new JPanel(new FlowLayout());
      add(top, BorderLayout.NORTH);
      add(center, BorderLayout.CENTER);
      if (showDerivedKeys) {
         kEncTF = new HexField(24);
         kEncTF.setEditable(false);
         kMacTF = new HexField(24);
         kMacTF.setEditable(false);
         JPanel bottom = new JPanel(new GridLayout(2, 2));
         bottom.add(new JLabel("K.ENC: ", JLabel.RIGHT));
         bottom.add(kEncTF);
         bottom.add(new JLabel("K.MAC: ", JLabel.RIGHT));
         bottom.add(kMacTF);
         add(bottom, BorderLayout.SOUTH);
      }
   }
   
   public void addActionListener(ActionListener l) {
      deriveKeyButton.addActionListener(l);
   }

   private class KeyConstructor implements ActionListener
   {
      public void actionPerformed(ActionEvent ae) {
         try {
            String docNr = docNrTF.getText(),
            dateOfBirth = dateOfBirthTF.getText(),
            dateOfExpiry = dateOfExpiryTF.getText();
            byte[] keySeed = Util.computeKeySeed(docNr, dateOfBirth, dateOfExpiry);
            kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
            kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
            kEncTF.setValue(kEnc.getEncoded());
            kMacTF.setValue(kMac.getEncoded());
            bacDB.addEntry(docNr, dateOfBirth, dateOfExpiry);
         } catch (Exception e) {
            kEnc = null;
            kMac = null;
            if (kEncTF != null) { kEncTF.clearText(); }
            if (kMacTF != null) { kMacTF.clearText(); }
         }
      }
   }
   
   /* Accessors. */
   
   public String getDateOfBirth() {
      return dateOfBirthTF.getText();
   }
   
   public String getDateOfExpirty() {
      return dateOfExpiryTF.getText();
   }
   
   public String getDocumentNumber() {
      return docNrTF.getText();
   }

   public SecretKey getKEnc() {
      return kEnc;
   }

   public SecretKey getKMac() {
      return kMac;
   }
}
