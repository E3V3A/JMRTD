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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.bouncycastle.asn1.icao.DataGroupHash;

import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.COMFile;
import sos.mrtd.DataGroup;
import sos.mrtd.PassportApduService;
import sos.mrtd.PassportFileService;
import sos.mrtd.PassportService;
import sos.mrtd.SODFile;
import sos.mrtd.SecureMessagingWrapper;
import sos.util.Hex;

/**
 * Convenient GUI component examining the security object.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class PAPanel extends JPanel implements AuthListener
{
   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
   
   private JTextArea area;
   private JButton readObjectButton, readDSCertButton, loadCSCertButton, computeHashButton;

   private PassportApduService apduService;
   private PassportFileService fileService;
   private PassportService passportService;
   
   private SecureMessagingWrapper wrapper;
   
   private Certificate docSigningCert;
   private Certificate countrySigningCert;

   private Object[] storedHashes;
   
   public PAPanel(PassportApduService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      super(new BorderLayout());
      this.apduService = service;
      this.fileService = new PassportFileService(apduService);
      this.passportService = new PassportService(fileService);
      this.wrapper = null;
      
      final JPanel hashesPanel = new JPanel(new FlowLayout());
      readObjectButton = new JButton("Read from SO");
      hashesPanel.add(readObjectButton);
      readObjectButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               SODFile sodFile = passportService.getSODFile();
               area.append("Read pubkey security object\n");
               DataGroupHash[] hashes = sodFile.getDataGroupHashes();
               storedHashes = new Object[hashes.length];
               for (int i = 0; i < hashes.length; i++) {
                  storedHashes[i] = hashes[i].getDataGroupHashValue().getOctets();
                  area.append("   stored hash of ");
                  area.append("DG" + hashes[i].getDataGroupNumber() + ": ");
                  area.append(Hex.bytesToHexString((byte[])storedHashes[i]));
                  area.append("\n");
               }
               area.append("\n");
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      computeHashButton = new JButton("Compute");
      hashesPanel.add(computeHashButton);
      computeHashButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            (new Thread(new Runnable() {
               public void run() {
                  try {
                     String alg = "SHA256";
                     // TODO: this is a hack... find out how to properly parse the sig. alg from the security object
                     if (storedHashes != null && storedHashes.length > 0
                        && ((byte[])storedHashes[0]).length == 20) {
                        alg = "SHA1";
                     }
                     MessageDigest digest = MessageDigest.getInstance(alg);
                     COMFile comFile = passportService.readCOMFile();
                     byte[] tags = comFile.getTagList();
                     for (int i = 0; i < tags.length; i++) {
                        DataGroup file = passportService.readDataGroup(tags[i]);
                        byte[] computedHash = digest.digest(file.getEncoded());
                        area.append("   computed hash of ");
                        area.append("DG " + Integer.toHexString(tags[i] & 0xFF) + ": ");
                        area.append(Hex.bytesToHexString(computedHash));
                        if (storedHashes != null && storedHashes.length > i
                              && Arrays.equals((byte[])storedHashes[i], computedHash)) {
                           area.append(" --> OK");
                        } else {
                           area.append(" --> FAIL");
                        }
                        area.append("\n");
                     }
                     area.append("\n");
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            })).start();
         }
      });
      
      JPanel certsPanel = new JPanel();
      readDSCertButton = new JButton("DS cert");
      certsPanel.add(readDSCertButton);
      readDSCertButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               SODFile sodFile = passportService.getSODFile();
               docSigningCert = sodFile.getDocSigningCertificate();
               area.append("docSigningCert = \n" + docSigningCert);
               area.append("\n");
               
               boolean succes = sodFile.checkDocSignature(docSigningCert);
               area.append(" --> Signature check: " + succes + "\n");           
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      loadCSCertButton = new JButton("CS cert");
      certsPanel.add(loadCSCertButton);
      loadCSCertButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               final JFileChooser chooser = new JFileChooser();
               chooser.setDialogTitle("Select certificate file");
               // chooser.setCurrentDirectory(currentDir);
               chooser.setFileHidingEnabled(false);
               int n = chooser.showOpenDialog(hashesPanel);
               if (n != JFileChooser.APPROVE_OPTION) {
                  System.out.println("DEBUG: select file canceled...");
                  return;
               }
               
               /* DEBUG... */
               System.out.println("WRITING...");
               FileOutputStream out = new FileOutputStream("docsigning_cert.der");
               out.write(docSigningCert.getEncoded());
               out.flush();
               out.close();
               
               System.out.println("DEBUG: docSigningCert.getClass() == " + docSigningCert.getClass());
             
               System.out.println(((X509Certificate)docSigningCert).getType());
               
               File file = chooser.getSelectedFile();
               CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
               Certificate countrySigningCert = certFactory.generateCertificate(new FileInputStream(file));
               area.append("Contents of: ");
               area.append("\"" + file.toString() + "\"\n");
               area.append(countrySigningCert.toString() + "\n");
               try {
                  area.append(" --> Signature check: ");
                  docSigningCert.verify(countrySigningCert.getPublicKey());
                  area.append("check: true");
               } catch (Exception e) {
                  area.append("check: false - " + e.toString());
                  e.printStackTrace();
               } finally {
                  area.append("\n");
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      
      hashesPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Hashes"));
      certsPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Certificates"));
      JPanel leftPanel = new JPanel(new GridLayout(2,1));
      leftPanel.add(hashesPanel);
      leftPanel.add(certsPanel);
      add(leftPanel, BorderLayout.WEST);
      area = new JTextArea(20, 30);
      add(new JScrollPane(area), BorderLayout.CENTER);
   }
   
   public void performedBAC(BACEvent be) {
      this.wrapper = be.getWrapper();
      fileService.setWrapper(wrapper);
      passportService.setWrapper(wrapper);
   }
   
   public void performedAA(AAEvent ae) {
   }  
}
