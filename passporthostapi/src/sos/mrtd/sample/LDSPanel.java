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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sos.gui.HexField;
import sos.gui.HexViewPanel;
import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.PassportApduService;
import sos.mrtd.PassportFileService;
import sos.mrtd.SecureMessagingWrapper;

/**
 * Convenient GUI component for accessing the LDS.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class LDSPanel extends JPanel
implements ActionListener, AuthListener
{
   private static final byte[] ZERO_DATA = new byte[256];
   private HexField fidTF, offsetTF, leTF;
   private HexViewPanel hexviewer;
   private JButton selectButton, readBinaryButton, readNextButton, saveButton;

   short offset;
   int bytesRead;

   private PassportApduService service;
   private SecureMessagingWrapper wrapper;

   public LDSPanel(PassportApduService service) {
      super(new BorderLayout());
      this.service = service;
      this.wrapper = null;
      hexviewer = new HexViewPanel(ZERO_DATA);
      JPanel north = new JPanel(new FlowLayout());
      fidTF = new HexField(2);
      selectButton = new JButton("Select File");
      selectButton.addActionListener(this);
      saveButton = new JButton("Save File");
      saveButton.addActionListener(this);
      north.add(new JLabel("File: "));
      north.add(fidTF);
      north.add(selectButton);
      north.add(saveButton);
      JPanel south = new JPanel(new FlowLayout());
      leTF = new HexField(1);
      leTF.setValue(0xFF);
      offsetTF = new HexField(2);
      readBinaryButton = new JButton("Read Binary");
      readBinaryButton.addActionListener(this);
      readNextButton = new JButton("Read Next");
      readNextButton.addActionListener(this);
      south.add(new JLabel("Offset: "));
      south.add(offsetTF);
      south.add(new JLabel("Length: "));
      south.add(leTF);
      south.add(readBinaryButton);
      south.add(readNextButton);
      add(north, BorderLayout.NORTH);
      add(hexviewer, BorderLayout.CENTER);
      add(south, BorderLayout.SOUTH);
   }
   
   public void actionPerformed(ActionEvent ae) {
      try {
         JButton but = (JButton) ae.getSource();
         if (but == selectButton) {
            pressedSelectButton();
         } else if (but == readBinaryButton) {
            pressedReadBinaryButton();
         } else if (but == readNextButton) {
            pressedReadNextButton();
         } else if (but == saveButton) {
            pressedSaveButton();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void pressedSaveButton() throws Exception {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Save file");
      // chooser.setCurrentDirectory(currentDir);
      chooser.setFileHidingEnabled(false);
      int n = chooser.showOpenDialog(this);
      if (n != JFileChooser.APPROVE_OPTION) {
         System.out.println("DEBUG: select file canceled...");
         return;
      }
      final byte[] fid = fidTF.getValue();
      final File file = chooser.getSelectedFile();
      new Thread(new Runnable() {
         public void run() {
            try {
               PassportFileService s = new PassportFileService(service);
               s.setWrapper(wrapper);
               byte[] data =
                  s.readFile((short) (((fid[0] & 0x000000FF) << 8)
                                      | (fid[1] & 0x000000FF)));
               OutputStream out = new FileOutputStream(file);
               out.write(data, 0, data.length);
               out.close();
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }).start();
   }

   private void pressedSelectButton() throws Exception {
      byte[] fid = fidTF.getValue();
      service.sendSelectFile(wrapper,
            (short) (((fid[0] & 0x000000FF) << 8) | (fid[1] & 0x000000FF)));
   }

   private void pressedReadBinaryButton() throws Exception {
      bytesRead = 0;
      int le = leTF.getValue()[0] & 0x000000FF;
      byte[] offsetBytes = offsetTF.getValue();
      offset = (short)(((offsetBytes[0] & 0x000000FF) << 8)
                 | (offsetBytes[1] & 0x000000FF));
      byte[] data = service.sendReadBinary(wrapper, offset, le);
      remove(hexviewer);
      hexviewer = new HexViewPanel(data, offset);
      add(hexviewer, BorderLayout.CENTER);
      bytesRead = data.length;
      setVisible(false);
      setVisible(true);
      // repaint();
   }

   private void pressedReadNextButton() throws Exception {
      offset += bytesRead;
      offsetTF.setValue(offset & 0x000000000000FFFFL);
      pressedReadBinaryButton();
   }
   
   public void performedBAC(BACEvent be) {
      this.wrapper = be.getWrapper();
   }
   
   public void performedAA(AAEvent ae) {
   }
}

