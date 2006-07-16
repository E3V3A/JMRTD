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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.icao.DataGroupHash;
import org.bouncycastle.asn1.icao.LDSSecurityObject;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.x509.DigestInfo;

import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.PassportApduService;
import sos.mrtd.PassportFileService;
import sos.mrtd.PassportService;
import sos.mrtd.SecureMessagingWrapper;
import sos.mrtd.Util;
import sos.util.Hex;

/**
 * Convenient GUI component examining the security object.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 43 $
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
   
   private LDSSecurityObject sod;
   private Certificate docSigningCert;
   private Certificate countrySigningCert;

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
               sod = passportService.readSecurityObject();
               area.append("Read pubkey security object\n");
               DataGroupHash[] hashes = sod.getDatagroupHash();
               for (int i = 0; i < hashes.length; i++) {
                  area.append("   stored hash of ");
                  area.append("DG" + hashes[i].getDataGroupNumber() + ": ");
                  area.append(Hex.bytesToHexString(hashes[i].getDataGroupHashValue().getOctets()));
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
                     MessageDigest digest = MessageDigest.getInstance("SHA256");
                     short[] dg = passportService.readDataGroupList();
                     for (int i = 0; i < dg.length; i++) {
                        byte[] file = fileService.readFile(dg[i]);
                        area.append("   computed hash of ");
                        area.append("DG" + (dg[i] & 0xFF) + ": ");
                        area.append(Hex.bytesToHexString(digest.digest(file)));
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
               docSigningCert = passportService.readDocSigningCertificate();
               area.append("docSigningCert = \n" + docSigningCert);
               area.append("\n");
               // ((X509Certificate)docSigningCert).checkValidity();
               String sigAlg = ((X509Certificate)docSigningCert).getSigAlgName();
               // Signature sig = Signature.getInstance("SHA256WithRSA");
               Signature sig = Signature.getInstance(sigAlg);
               sig.initVerify(docSigningCert);
               sig.update(passportService.readSecurityObjectContent());
               boolean succes = sig.verify(passportService.readEncryptedDigest());
               area.append("Signature check: " + succes + "\n");           
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
               File file = chooser.getSelectedFile();
               FileInputStream fileIn = new FileInputStream(file);
               CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
               Collection coll = certFactory.generateCertificates(fileIn);
               for (Iterator it = coll.iterator(); it.hasNext();) {
                  countrySigningCert = (Certificate)it.next();
                  area.append("Contents of: ");
                  area.append("\"" + file.toString() + "\"\n");
                  area.append(countrySigningCert.toString() + "\n");
                  PublicKey pubkey = countrySigningCert.getPublicKey();
                  try {
                     docSigningCert.verify(pubkey);
                     area.append("Signature check: DS cert was signed with CS key!");
                  } catch (Exception se) {
                     area.append("Signature check: failed \n" + se.toString());
                     se.printStackTrace();
                  }
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
      area.append("pubkey = " + ae.getPubkey());
      area.append("m1 = " + Hex.bytesToHexString(ae.getM1()));
      area.append("m2 = " + Hex.bytesToHexString(ae.getM2()));
   }
   
   private static void debug(Object obj) {
      debug("DEBUG: ", obj);
   }
   
   private static void debug(String indent, Object obj) {
      if (obj == null) {
         System.out.println(indent + "null");
         return;
      } else if (obj instanceof byte[]) {
         try {
            ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(
                  (byte[]) obj));
            while (true) {
               obj = in.readObject();
               if (obj == null) {
                  return;
               }
               debug(indent, obj);
            }
         } catch (Exception e) {
            System.out.println(indent + Hex.bytesToHexString((byte[]) obj));
         }
      } else if (obj instanceof ASN1Sequence) {
         ASN1Sequence s = (ASN1Sequence) obj;
         System.out.println(indent + "SEQUENCE");
         indent += "         ";
         for (int j = 0; j < s.size(); j++) {
            debug(indent + " " + j + " ", s.getObjectAt(j));
         }
      } else if (obj instanceof ASN1Set) {
         ASN1Set s = (ASN1Set) obj;
         System.out.println(indent + "SET");
         indent += "         ";
         for (int j = 0; j < s.size(); j++) {
            debug(indent + " " + j + " ", s.getObjectAt(j));
         }
      } else if (obj instanceof ASN1OctetString) {
         debug(indent + "OCTETSTRING ", ((ASN1OctetString) obj).getOctets());
      } else {
         System.out.println(indent + obj.toString());
      }
   }
}

