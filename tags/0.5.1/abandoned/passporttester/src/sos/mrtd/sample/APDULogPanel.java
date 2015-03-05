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
 * $Id: APDULogPanel.java 893 2009-03-23 15:43:42Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.Date;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardTerminalListener;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.util.Hex;

/**
 * Simple APDU-logging GUI component.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 893 $
 */
public class APDULogPanel extends JPanel implements CardTerminalListener, APDUListener, ISO7816
{
   private static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

   private JTextArea area;

   /*@ invariant count >= 0; */
   private int count;

   /**
    * Creates the logger.
    */
   public APDULogPanel() {
      super(new BorderLayout());
      area = new JTextArea(10, 100);
      area.setFont(FONT);
      add(new JScrollPane(area));
   }

   /**
    * Called by the service to write a <i>started</i> entry to the log.
    */
   public void cardInserted(CardEvent ce) {
      append("Session started on ");
      append((new Date()).toString());
      append("\n\n");
   }

   /**
    * Called by the service to write a <i>stopped</i> entry to the log.
    */
   public void cardRemoved(CardEvent ce) {
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
   public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
      append(Integer.toString(count)); append(".");
      append(" C: "); append(capdu.toString());
      append(" Bytes: "); append(Hex.toHexString(capdu.getBytes()));
      append("\n");
      append(whiteSpace(count)); append(" ");
      append(" R: "); append(Hex.toHexString(rapdu.getBytes()));
      append(" ("); append(swToString((short)rapdu.getSW())); append(")");
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

   private static String swToString(short sw) {
      switch(sw) {
         case SW_BYTES_REMAINING_00: return "BYTES REMAINING";
         case SW_END_OF_FILE: return "END OF FILE";
         case SW_WRONG_LENGTH: return "WRONG LENGTH";
         case SW_SECURITY_STATUS_NOT_SATISFIED: return "SECURITY STATUS NOT SATISFIED";
         case SW_FILE_INVALID: return "FILE INVALID";
         case SW_DATA_INVALID: return "DATA INVALID";
         case SW_CONDITIONS_NOT_SATISFIED: return "CONDITIONS NOT SATISFIED";
         case SW_COMMAND_NOT_ALLOWED: return "COMMAND NOT ALLOWED";
         case SW_APPLET_SELECT_FAILED: return "APPLET SELECT FAILED";
         case SW_KEY_USAGE_ERROR: return "KEY USAGE ERROR";
         case SW_WRONG_DATA: return "WRONG DATA";
         case SW_FUNC_NOT_SUPPORTED: return "FUNC NOT SUPPORTED";
         case SW_FILE_NOT_FOUND: return "FILE NOT FOUND";
         case SW_RECORD_NOT_FOUND: return "RECORD NOT FOUND";
         case SW_FILE_FULL: return "FILE FULL";
         case SW_INCORRECT_P1P2: return "INCORRECT P1P2";
         case SW_KEY_NOT_FOUND: return "KEY NOT FOUND";
         case SW_WRONG_P1P2: return "WRONG P1P2";
         case SW_CORRECT_LENGTH_00: return "CORRECT LENGTH";
         case SW_INS_NOT_SUPPORTED: return "INS NOT SUPPORTED";
         case SW_CLA_NOT_SUPPORTED: return "CLA NOT SUPPORTED";
         case SW_UNKNOWN: return "UNKNOWN";
         case SW_CARD_TERMINATED: return "CARD TERMINATED";
         case SW_NO_ERROR: return "NO ERROR";
      }
      return "";
   }
}

