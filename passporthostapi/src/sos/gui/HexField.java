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
 * $Id: HexField.java 12 2006-07-05 10:11:46Z martijno $
 */

package sos.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextField;

import sos.util.Hex;

/**
 * GUI text field component that only accepts hexadecimal
 * representations of byte array of given length.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class HexField extends Box implements KeyListener, ActionListener
{
   static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

   private int length;
   private Collection listeners;
   private JTextField textfield;

   /**
    * Constructs a new hex field of length 1.
    */
   public HexField() {
      this(1);
   }

   /**
    * Constructs a new hex field of length <code>length</code>.
    *
    * @param length the length of this new hex field (in bytes).
    */
   public HexField(int length) {
      super(BoxLayout.X_AXIS);
      textfield = new JTextField(2 * length + 1);
      textfield.setFont(FONT);
      this.length = length;
      this.listeners = new ArrayList();
      textfield.addActionListener(this);
      add(textfield);
      textfield.addKeyListener(this);
      setEditable(true);
   }

   /**
    * The length of this hex field.
    *
    * @return the length (number of bytes) of this hex field.
    */
   public int length() {
      return length;
   }

   /**
    * Sets the length of this hex field to <code>length</code>.
    *
    * @param length the new length of this hex field.
    */
   public void setLength(int length) {
      if (length >= 0) {
         this.length = length;
         clearText();
         textfield.setColumns(2 * length + 1);
      }
   }

   /**
    * Sets the editability of this hex field.
    *
    * @param editable indicates whether to enable or
    *    disable editability.
    */
   public void setEditable(boolean editable) {
      textfield.setEditable(editable);
   }

   /**
    * Clears this hex field.
    */
   public void clearText() {
      textfield.setText("");
   }

   /**
    * This is needed here since this class implements
    * <code>KeyListener</code>. 
    * 
    * @param e the event indicating a key was pressed.
    */
   public void keyPressed(KeyEvent e) {
   }
   
   /**
    * This is needed here since this class implements
    * <code>KeyListener</code>. 
    * 
    * @param e the event indicating a key was released.
    */
   public void keyReleased(KeyEvent e) {
   }

   /**
    * Makes sure the string value typed constitutes a
    * hexadecimal string.
    *
    * @param e the event indicating a key was typed.
    */
   public void keyTyped(KeyEvent e) {
      String validhex = "0123456789abcdefABCDEF";
      char c = e.getKeyChar();
      int len = textfield.getText().length();
      if ((len < 2 * length || textfield.getSelectedText() != null) &&
             validhex.indexOf(c) >= 0) {
         e.setKeyChar(Character.toUpperCase(c));
         return;
      } else if (c == KeyEvent.VK_ENTER) {
         format();
      } else if (c == KeyEvent.VK_BACK_SPACE ||
               c == KeyEvent.VK_DELETE ||
               c == KeyEvent.VK_TAB ||
               e.isActionKey()) {
         return;
      } else {
         e.consume();
      }
   }

   /**
    * Formats the text.
    */
   void format() {
      textfield.setText(format(textfield.getText()));
   }

   /**
    * Formats the text.
    *
    * @param The text to be formatted.
    *
    * @return The formatted text.
    */
   private String format(String text) {
      String result = text.trim();
      int N = 2 * length - result.length();
      for (int i = 0; i < N; i++) {
         result = "0" + result;
      }
      return result;
   }

   /**
    * The hexadecimal value entered in the hex field.
    *
    * @return the hexadecimal value entered in the hex field.
    */
   public byte[] getValue() {
      format();
      return Hex.hexStringToBytes(textfield.getText());
   }

   public void setValue(long value) {
      byte[] newValue = new byte[length];
      for (int i = 0; i < length; i++) {
         int s = 8 * (length - i - 1);
         long mask = (long)(0x00000000000000FFL << s);
         newValue[i] =
            (byte)((long)(value & mask) >> s);
      }
      setValue(newValue);
   }

   /**
    * Sets the value of this hex field to <code>value</code>.
    *
    * @param value the new value.
    */
   public void setValue(byte[] value) {
      String result = "";
      for (int i=0; i < value.length; i++) {
         result += Hex.byteToHexString(value[i]);
      }
      textfield.setText(result);
   }

   /**
    * Responds to action event (input changed).
    *
    * @param ae the action event.
    */
   public void actionPerformed(ActionEvent ae) {
      inputChanged(ae);
   }

   /**
    * Notifies this component of changes in its input.
    *
    * @param ae the action event indicating something has changed.
    */
   void inputChanged(ActionEvent ae) {
      format();
      String desc = "Hexfield changed value";
      Iterator it = listeners.iterator();
      while (it.hasNext()) {
         HexField l = (HexField)it.next();
         l.inputChanged(new ActionEvent(this, 0, desc));
      }
   }

   /**
    * Adds <code>l</code> to the action listener list of this
    * hex field.
    *
    * @param l the <code>ActionListener</code> to add.
    */
   public void addActionListener(HexField l) {
      listeners.add(l);
   }

   /**
    * Gets the preferred size of this component.
    *
    * @return the preferred size of this component.
    */
   public Dimension getPreferredSize() {
      int width = (int)super.getPreferredSize().getWidth();
      int height = (int)textfield.getPreferredSize().getHeight();
      return new Dimension(width, height);
   }
}

