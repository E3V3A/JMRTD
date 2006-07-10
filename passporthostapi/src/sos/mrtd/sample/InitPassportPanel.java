/*
 * Created on Jul 10, 2006
 */
package sos.mrtd.sample;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.PublicKey;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sos.gui.HexField;
import sos.mrtd.AuthListener;
import sos.mrtd.PassportApduService;
import sos.mrtd.SecureMessagingWrapper;

public class InitPassportPanel extends JPanel implements ActionListener, AuthListener
{
   private JButton createFileButton;
   private JButton selectFileButton;
   private JButton selectLocalFileButton;
   private JButton updateBinaryButton;
   private HexField lenField;
   private HexField fidField;
   private File fileToUpload;
   private PassportApduService service;
   private SecureMessagingWrapper wrapper;

   public InitPassportPanel(PassportApduService service) {
      super(new FlowLayout());
      this.service = service;
      this.wrapper = null;
      selectLocalFileButton = new JButton("Select local file ... ");
      createFileButton = new JButton("Create file");
      selectFileButton = new JButton("Select file");
      updateBinaryButton = new JButton("Update binary");
      selectLocalFileButton.addActionListener(this);
      createFileButton.addActionListener(this);
      selectFileButton.addActionListener(this);
      updateBinaryButton.addActionListener(this);
      fidField = new HexField(2);
      lenField = new HexField(2);
      lenField.setEditable(false);
      add(selectLocalFileButton);
      add(new JLabel("file: "));
      add(fidField);
      add(new JLabel("length:"));
      add(lenField);
      add(createFileButton);
      add(selectFileButton);
      add(updateBinaryButton);
   }

   public void actionPerformed(ActionEvent ae) {
      JButton butt = (JButton) ae.getSource();

      try {
         if (butt == selectLocalFileButton) {
            pressedSelectLocalFileButton();
         } else if (butt == createFileButton) {
            pressedCreateFileButton();
         } else if (butt == selectFileButton) {
            pressedSelectFileButton();
         } else if (butt == updateBinaryButton) {
            pressedUpdateBinaryButton();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void pressedUpdateBinaryButton() {
      final byte[] fid = fidField.getValue();

      new Thread(new Runnable() {
         public void run() {
            try {
               FileInputStream in = new FileInputStream(fileToUpload);
               service
                     .writeFile(
                           wrapper,
                           (short) (((fid[0] & 0x000000FF) << 8) | (fid[1] & 0x000000FF)),
                           in);
               in.close();
            } catch (IOException ioe) {
               ioe.printStackTrace();
            }
         }
      }).start();
   }

   private void pressedSelectFileButton() {
      final byte[] fid = fidField.getValue();

      new Thread(new Runnable() {
         public void run() {
            try {
               service.selectFile(wrapper, fid);
            } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
         }
      }).start();
   }

   private void pressedCreateFileButton() {
      final byte[] fid = fidField.getValue();
      final byte[] len = lenField.getValue();

      new Thread(new Runnable() {
         public void run() {
            service.createFile(wrapper, fid, len);
         }
      }).start();
   }

   private void pressedSelectLocalFileButton() throws Exception {
      final JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Select file");
      // chooser.setCurrentDirectory(currentDir);
      chooser.setFileHidingEnabled(false);
      int n = chooser.showOpenDialog(this);
      if (n != JFileChooser.APPROVE_OPTION) {
         System.out.println("DEBUG: select file canceled...");
         return;
      }

      fileToUpload = chooser.getSelectedFile();
      lenField.setValue(fileToUpload.length());
   }

   public void performedBAC(SecureMessagingWrapper wrapper) {
      this.wrapper = wrapper;
   }

   public void performedAA(PublicKey pubkey, boolean success) {
   }
}
