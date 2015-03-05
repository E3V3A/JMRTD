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
 * $Id: CommandAPDUField.java 893 2009-03-23 15:43:42Z martijno $
 */

package sos.mrtd.sample;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.smartcardio.CommandAPDU;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.swing.HexArrayField;
import net.sourceforge.scuba.swing.HexField;

/**
 * A GUI component for entering a CommandAPDU.
 * TODO: (MO) check dependency on Apdu.command.
 *
 * @version $Revision: 893 $
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 */
public class CommandAPDUField extends JPanel {

   private static final Border PANEL_BORDER =
      BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

   private HexField claTF, insTF, p1TF, p2TF, leTF;
   private JCheckBox leCheckBox;
   private HexArrayField cdataTF;

   /**
    * Constructs a new field.
    */
   public CommandAPDUField() {
      setLayout(new GridLayout(2,1));
      JPanel topPnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
      JLabel claLbl = new JLabel("cla:"); topPnl.add(claLbl);
      claTF = new HexField(); topPnl.add(claTF);
      JLabel insLbl = new JLabel("ins:"); topPnl.add(insLbl);
      insTF = new HexField(); topPnl.add(insTF);
      JLabel p1Lbl = new JLabel("p1:"); topPnl.add(p1Lbl);
      p1TF = new HexField(); topPnl.add(p1TF);
      JLabel p2Lbl = new JLabel("p2:"); topPnl.add(p2Lbl);
      p2TF = new HexField(); topPnl.add(p2TF);
      JLabel leLbl = new JLabel("le:"); topPnl.add(leLbl);
      leTF = new HexField(); topPnl.add(leTF);
      leLbl.setEnabled(false);
      leTF.setEnabled(false);
      leCheckBox = new JCheckBox();
      ComponentToggler leToggler = new ComponentToggler();
      leToggler.add(leLbl);
      leToggler.add(leTF);
      leCheckBox.addActionListener(leToggler);
      topPnl.add(leCheckBox);
      add(topPnl);
      JPanel botPnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
      botPnl.setAlignmentX(JPanel.LEFT_ALIGNMENT);
      cdataTF = new HexArrayField();
      botPnl.add(cdataTF);
      add(botPnl);
      setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Command APDU"));
   }

   /**
    * Resets the field to a <code>CommandAPDU</code> with
    * zero length buffer and cla, ins, p1, p2 equal to zero.
    */
   public void reset() {
      claTF.setValue((byte)0x00);
      insTF.setValue((byte)0x00);
      p1TF.setValue((byte)0x00);
      p2TF.setValue((byte)0x00);
      leTF.setValue((byte)0x00);
      cdataTF.reset();
      validate();
   }

   public void setEnabled(boolean b) {
      reset();
      for (int i = 0; i < getComponentCount(); i++) {
         getComponent(i).setEnabled(b);
      }
      validate();
   }

   /**
    * Gets the <code>CommandAPDU</code> entered in this field.
    *
    * @return The <code>CommandAPDU</code> entered in this field.
    */
   public CommandAPDU getAPDU() {
      CommandAPDU apdu = new CommandAPDU(claTF.getValue()[0],
            insTF.getValue()[0], p1TF.getValue()[0], p2TF.getValue()[0],
            cdataTF.getValue(), leTF.getValue()[0] & 0x000000FF);
      return apdu;
   }

   public void setAPDU(CommandAPDU apdu) {
      byte[] buffer = apdu.getBytes();
      claTF.setValue(buffer[ISO7816.OFFSET_CLA]);
      insTF.setValue(buffer[ISO7816.OFFSET_INS]);
      p1TF.setValue(buffer[ISO7816.OFFSET_P1]);
      p2TF.setValue(buffer[ISO7816.OFFSET_P2]);
      int lc = apdu.getNc();
      byte[] data = new byte[lc];
      System.arraycopy(buffer, ISO7816.OFFSET_CDATA, data, 0, lc);
      cdataTF.setValue(data);
      int le = apdu.getNe();
      if (le > 0) {
         leTF.setValue(le);
         leCheckBox.setSelected(true);
      }
      validate();
   }

   /**
    * Inner class for enabling and disabling components
    * based on <code>ActionEvent</code>s.
    */
   private class ComponentToggler implements ActionListener {

      private Collection<JComponent> components;

      /**
       * Creates a new toggler.
       */
      public ComponentToggler() {
         components = new ArrayList<JComponent>();
      }

      /**
       * Adds <code>component</code> to this toggler.
       * The component <code>component</code> will be enabled or
       * disabled as soon as this toggler receives an
       * <code>ActionEvent</code>.
       *
       * @param component The component to add.
       */
      public void add(JComponent component) {
         components.add(component);
      }

      /**
       * Gets called when an <code>ActionEvent</code> <code>e</code>
       * occurs. For all components added to this toggler:
       * Enables the component if it is disabled,
       * disables the component if it is enabled.
       *
       * @param e The <code>ActionEvent</code> indicating something
       *    happened.
       */
      public void actionPerformed(ActionEvent e) {
         for (JComponent component: components) {
            if (component.isEnabled()) {
               component.setEnabled(false);
            } else {
               component.setEnabled(true);
            }
         }
      }
   }
}

