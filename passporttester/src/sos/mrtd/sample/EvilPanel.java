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
 * $Id: AAPanel.java 73 2006-07-17 11:03:28Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.scuba.smartcards.CardServiceException;

import org.jmrtd.PassportApduService;

import sos.mrtd.evil.EvilService;

/**
 * Convenient GUI component for doing AA.
 *
 * @author Ronny Wichers Schreur (ronny@cs.ru.nl)
 *
 * @version $Revision: 73 $
 */
public class EvilPanel extends JPanel
{
   private JTextArea area;
   private JButton openBackDoorButton, closeBackDoorButton;

   private PassportApduService apduService;
   private EvilService evilService;
   
   public EvilPanel(PassportApduService service)
   throws CardServiceException {
      super(new BorderLayout());
      setService(service);

      JPanel northPanel = new JPanel(new FlowLayout());
      openBackDoorButton = new JButton("Open Back Door");
      northPanel.add(openBackDoorButton);
      openBackDoorButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
            	short version;
            	
            	version = evilService.openBackDoor();
                area.append(" --> Evil Applet version" + version + "!\n");
                openBackDoorButton.setEnabled(false);
                closeBackDoorButton.setEnabled(true);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      closeBackDoorButton = new JButton("Close Back Door");
      closeBackDoorButton.setEnabled(false);
      northPanel.add(closeBackDoorButton);
      closeBackDoorButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            try {
            	evilService.closeBackDoor();
                openBackDoorButton.setEnabled(true);
                closeBackDoorButton.setEnabled(false);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
      add(northPanel, BorderLayout.NORTH);
      area = new JTextArea(20, 30);
      add(new JScrollPane(area), BorderLayout.CENTER);
   }

   public void setService(PassportApduService service) throws CardServiceException {
      this.apduService = service;
      this.evilService = new EvilService(apduService);
   }
}
