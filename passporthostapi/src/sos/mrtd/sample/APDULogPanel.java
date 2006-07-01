/*
 * JMRTD - A Java package for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  ICIS, Radboud University
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
 * $Id: APDULogPanel.java,v 1.13 2006/06/12 12:24:21 martijno Exp $
 */

package sos.mrtd.sample;

import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sos.smartcards.APDUListener;
import sos.smartcards.Apdu;
import sos.util.Hex;

/**
 * Simple APDU-logging GUI component.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 1.13 $
 */
public class APDULogPanel extends JPanel implements APDUListener
{
   private static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

   private JTextArea area;

   /*@ invariant count >= 0; */
   private int count;

   /**
    * Creates the logger.
    */
   public APDULogPanel() {
      super(new FlowLayout());
      area = new JTextArea(10, 100);
      area.setFont(FONT);
      add(new JScrollPane(area));
   }

   /**
    * Called by the service to write a <i>started</i> entry to the log.
    */
   public void startedAPDUSession() {
      append("Session started on ");
      append((new Date()).toString());
      append("\n\n");
   }

   /**
    * Called by the service to write a <i>stopped</i> entry to the log.
    */
   public void stoppedAPDUSession() {
      append("Session stopped on ");
      append((new Date()).toString());
      append("\n\n");
   }

   /**
    * Called by the service to write an <i>exchanged apdu</i> entry
    * to the log.
    *
    * @param capdu the command apdu including explicit lc and le
    * @param rapdu the response apdu including sw
    */
   public void exchangedAPDU(Apdu capdu, byte[] rapdu) {
      append(Integer.toString(count)); append(".");
      append(" Command: "); append(capdu.toString());
      append("\n");
      append(whiteSpace(count)); append(" ");
      append(" Response: "); append(Hex.toHexString(rapdu));
      append("\n\n");
      count++;
   }

   private void append(String txt) {
      area.append(txt);
      area.setCaretPosition(area.getDocument().getLength() - 1);
   }

   /**
    * The whitespace needed to indent line following the first line
    * of an entry with entry number <code>count</code>.
    * 
    * @param count an entry number.
    *
    * @return a string containing spaces.
    */
   private /*@ pure */ static String whiteSpace(int count) {
      if (count == 0) {
         return " ";
      }
      char[] result = new char[(int)(Math.log(count) / Math.log(10)) + 1];
      Arrays.fill(result, ' ');
      return new String(result);
   }
}

