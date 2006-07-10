/*
 * Created on Jul 6, 2006
 */
package sos.mrtd.sample;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.security.PublicKey;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import sos.gui.ImagePanel;
import sos.mrtd.AuthListener;
import sos.mrtd.PassportService;
import sos.mrtd.SecureMessagingWrapper;
import sos.smartcards.CardService;

public class FacePanel extends JPanel implements Runnable, ActionListener, AuthListener
{
   private ImagePanel ipanel;
   private JFrame iframe;
   private JButton showButton, hideButton, readButton;
   private CardService service;
   private SecureMessagingWrapper wrapper;

   public FacePanel(CardService service) {
      super(new FlowLayout());
      this.service = service;
      this.wrapper = null;
      showButton = new JButton("Show Image");
      showButton.addActionListener(this);
      hideButton = new JButton("Hide Image");
      hideButton.addActionListener(this);
      hideButton.setEnabled(false);
      readButton = new JButton("Read Image");
      readButton.addActionListener(this);
      ipanel = new ImagePanel();
      add(showButton);
      add(hideButton);
      add(readButton);
      iframe = new JFrame("Face");
      Container cp = iframe.getContentPane();
      cp.add(new JScrollPane(ipanel));
      iframe.pack();
   }

   public void actionPerformed(ActionEvent ae) {
      JButton but = (JButton)ae.getSource();
      if (but.getText().startsWith("Show")) {
         iframe.setVisible(true);
         showButton.setEnabled(false);
         hideButton.setEnabled(true);
      } else if (but.getText().startsWith("Hide")) {
         iframe.setVisible(false);
         hideButton.setEnabled(false);
         showButton.setEnabled(true);
      } else if (but.getText().startsWith("Read")) {
         ipanel.clearImage();
         (new Thread(this)).start();
         showButton.setEnabled(false);
         iframe.setVisible(true);
         hideButton.setEnabled(true);
      }
   }

   public void run() {
      try {
         readButton.setEnabled(false);
         PassportService s = new PassportService(service, wrapper);
         BufferedImage img = s.readFace();
         ipanel.setImage(img);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         readButton.setEnabled(true);
      }
   }

   public void performedBAC(SecureMessagingWrapper wrapper) {
      this.wrapper = wrapper;
   }

   public void performedAA(PublicKey pubkey, boolean success) {
   }     
}