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
 * $Id: FacePanel.java 206 2007-03-26 20:19:44Z martijno $
 */

package sos.mrtd.sample.newgui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import sos.mrtd.DG1File;
import sos.mrtd.MRZInfo;

/**
 * GUI component for displaying the MRZ datagroup on the passport.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public class MRZPanel extends JPanel
{
   private static final Color MRZ_FOREGROUND_COLOR = new Color(0x000000);
   private static final Color MRZ_BACKGROUND_COLOR = new Color(0xFFFFFF);
   private static final Font MRZ_FONT = new Font("Monospaced", Font.BOLD, 15);

   private MRZInfo info;

   public MRZPanel(DG1File dg) {
      super(new FlowLayout());
      info = dg.getMRZInfo();
      add(makeMRZ(info));
   }

   private Component makeMRZ(MRZInfo info) {
      JTextArea c = new JTextArea();
      c.setEditable(false);
//      c.setForeground(MRZ_FOREGROUND_COLOR);
//      c.setBackground(MRZ_BACKGROUND_COLOR);
      c.setFont(MRZ_FONT);
      c.setText(info.toString().trim());
      return c;
   }
}
