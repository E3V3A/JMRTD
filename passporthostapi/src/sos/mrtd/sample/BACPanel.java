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
 * $Id: BACPanel.java 297 2008-01-02 14:31:26Z martijno $
 */

package sos.mrtd.sample;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import sos.mrtd.AuthListener;
import sos.mrtd.PassportService;
import sos.smartcards.CardServiceException;

/**
 * Convenient GUI component for step-by-step execution of the
 * BAC protocol.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 297 $
 */
public class BACPanel extends JPanel
{
   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

   private BACKeyPanel bacKeyPanel;
   private PassportService service;

   public BACPanel(PassportService passportService) throws CardServiceException {
      super(new FlowLayout());
      this.service = passportService;
      bacKeyPanel = new BACKeyPanel(BACKeyPanel.DONT_SHOW_DERIVED_KEYS);
      add(bacKeyPanel);
      bacKeyPanel.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            try {
               String docNr = bacKeyPanel.getDocumentNumber();
               String dateOfBirth = bacKeyPanel.getDateOfBirth();
               String dateOfExpiry = bacKeyPanel.getDateOfExpirty();
               service.doBAC(docNr, dateOfBirth, dateOfExpiry);
            } catch (CardServiceException cse) {
               System.out.println("DEBUG: BAC failed!");
            }
         }         
      });
   }

   public void addAuthenticationListener(AuthListener l) {
      service.addAuthenticationListener(l);
   }
}
