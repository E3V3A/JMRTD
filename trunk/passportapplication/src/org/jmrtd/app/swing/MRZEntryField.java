/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
 * $Id: $
 */

package org.jmrtd.app.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextField;

/**
 * GUI text field component for entering MRZ values.
 * 
 * @version $Revision: 183 $
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class MRZEntryField extends Box
{
	private static final long serialVersionUID = -421722490816671919L;

	private static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

	private int length;
	private JTextField textField;

	private Collection<ActionListener> listeners;

	/**
	 * Constructs a new numeric field of length 8.
	 */
	public MRZEntryField() {
		this(20);
	}

	/**
	 * Constructs a new numeric field of length <code>length</code>.
	 * 
	 * @param length the length (or rather: width) of this new numeric field.
	 */
	public MRZEntryField(int newLength) {
		super(BoxLayout.X_AXIS);
		listeners = new ArrayList<ActionListener>();
		this.length = newLength;
		textField = new JTextField(this.length + 1);
		textField.setFont(FONT);
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				scrubInput();
				notifyActionPerformed(new ActionEvent(this, 0, "Value changed"));
			}
		});
		textField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				scrubInput();
				notifyActionPerformed(new ActionEvent(this, 0, "Value changed"));
			}
		});
		add(textField);
		textField.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				scrubKeyTyped(e);
				// notifyActionPerformed(new ActionEvent(this, 0, "Value changed"));
			}
		});
		setEditable(true);
	}

	/**
	 * The length (width?) of this field.
	 * 
	 * @return The length (number of bytes) of this numeric field.
	 */
	public int length() {
		return length;
	}

	/**
	 * Sets the length (width?) of this field to <code>length</code>.
	 * 
	 * @param length The new length of this numeric field.
	 */
	public void setLength(int length) {
		if (0 <= length && length <= 8) {
			this.length = length;
			clearText();
			textField.setColumns(2 * length + 1);
		}
	}

	/**
	 * Sets the editability of this numeric field.
	 * 
	 * @param editable Indicates whether to enable or disable editability.
	 */
	public void setEditable(boolean editable) {
		textField.setEditable(editable);
	}

	/**
	 * Clears this numeric field.
	 */
	void clearText() {
		textField.setText("");
	}

	private void scrubInput() {
		textField.setText(format(textField.getText()));
	}

	private void scrubKeyTyped(KeyEvent e) {
		String validhex = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789< ";
		char c = e.getKeyChar();
		int len = textField.getText().length();
		if ((len < length || textField.getSelectedText() != null)
				&& validhex.indexOf(c) >= 0) {
			if (c == ' ') {
				c = '<';	
			} else {
				c = Character.toUpperCase(c);
			}
			e.setKeyChar(c);

			return;
		} else if (c == KeyEvent.VK_ENTER) {
			return;
		} else if (c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE
				|| c == KeyEvent.VK_TAB || e.isActionKey()) {
			return;
		} else {
			e.consume();
		}
	}

	/**
	 * Formats the text.
	 * 
	 * @param The text to be formatted.
	 * @return The formatted text.
	 */
	private String format(String text) {
		return text;
		/*
		 * String result = text.trim(); int N = length - result.length(); for (int
		 * i=0; i<N; i++) { result = "0"+result; } return result;
		 */
	}

	public String getText() {
		scrubInput();
		return textField.getText();
	}

	public int parseInt(char c) {
		switch (c) {
		case '0':
			return 0;
		case '1':
			return 1;
		case '2':
			return 2;
		case '3':
			return 3;
		case '4':
			return 4;
		case '5':
			return 5;
		case '6':
			return 6;
		case '7':
			return 7;
		case '8':
			return 8;
		case '9':
			return 9;
		default:
			throw new NumberFormatException();
		}
	}

	/**
	 * Sets the value of this numeric field to <code>value</code>.
	 * 
	 * @param value The new value.
	 */
	public void setText(String value) {
		textField.setText(value);
		scrubInput();
	}

	/**
	 * Adds <code>l</code> to the action listener list of this field.
	 * 
	 * @param l The <code>ActionListener</code> to add.
	 */
	public void addActionListener(ActionListener l) {
		listeners.add(l);
	}

	public void addFocusListener(FocusListener l) {
		textField.addFocusListener(l);
	}

	public void setAction(Action a) {
		textField.setAction(a);
	}

	public void setEnabled(boolean b) {
		textField.setEnabled(b);
	}

	/**
	 * Gets the preferred size of this component.
	 * 
	 * @return The preferred size of this component.
	 */
	public Dimension getPreferredSize() {
		int width = (int)super.getPreferredSize().getWidth();
		int height = (int)textField.getPreferredSize().getHeight();
		return new Dimension(width, height);
	}

	private void notifyActionPerformed(ActionEvent e) {
		for (ActionListener l: listeners) {
			l.actionPerformed(e);
		}
	}
}
