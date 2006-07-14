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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.Certificate;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.bouncycastle.asn1.icao.DataGroupHash;
import org.bouncycastle.asn1.icao.LDSSecurityObject;

import sos.mrtd.AAEvent;
import sos.mrtd.AuthListener;
import sos.mrtd.BACEvent;
import sos.mrtd.PassportApduService;
import sos.mrtd.PassportFileService;
import sos.mrtd.PassportService;
import sos.mrtd.SecureMessagingWrapper;
import sos.util.Hex;

/**
 * Convenient GUI component examining the security object.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 43 $
 */
public class SOPanel extends JPanel
implements AuthListener
{
   private JTextArea area;
   private JButton readObjectButton, readDSCert, computeHashButton;

   private PassportApduService apduService;
   private PassportFileService fileService;
   private PassportService passportService;
   
   private SecureMessagingWrapper wrapper;
   
   private LDSSecurityObject sod;
   private Certificate docSigningCert;
   private Certificate countrySigningCert;

   public SOPanel(PassportApduService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      super(new FlowLayout());
      this.apduService = service;
      this.fileService = new PassportFileService(apduService);
      this.passportService = new PassportService(fileService);
      this.wrapper = null;
      JPanel buttonPanel = new JPanel(new FlowLayout());
      readObjectButton = new JButton("Read Security Object");
      buttonPanel.add(readObjectButton);
      readObjectButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               sod = passportService.readSecurityObject();
               area.append("Read pubkey security object\n");
               DataGroupHash[] hashes = sod.getDatagroupHash();
               for (int i = 0; i < hashes.length; i++) {
                  area.append(" stored hash of ");
                  area.append("DG" + hashes[i].getDataGroupNumber() + ": ");
                  area.append(Hex.bytesToHexString(hashes[i].getDataGroupHashValue().getOctets()));
                  area.append("\n");
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      readDSCert = new JButton("Read DS cert");
      buttonPanel.add(readDSCert);
      readDSCert.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               docSigningCert = passportService.readDocSigningCertificate();
               area.append("docSigningCert = \n" + docSigningCert);
               area.append("\n");
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      computeHashButton = new JButton("Compute Hashes");
      buttonPanel.add(computeHashButton);
      computeHashButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
               MessageDigest digest = MessageDigest.getInstance("SHA256");
               short[] dg = passportService.readDataGroupList();
               for (int i = 0; i < dg.length; i++) {
                  byte[] file = fileService.readFile(dg[i]);
                  area.append(" computed hash of ");
                  area.append("DG" + (dg[i] & 0xFF) + ": ");
                  area.append(Hex.bytesToHexString(digest.digest(file)));
                  area.append("\n");
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      add(buttonPanel);
      area = new JTextArea(20, 30);
      add(new JScrollPane(area));
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
}

