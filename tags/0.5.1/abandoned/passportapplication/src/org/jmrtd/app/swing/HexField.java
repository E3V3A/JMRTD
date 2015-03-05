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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sourceforge.scuba.util.Hex;

/**
 * GUI text field component that only accepts hexadecimal representations
 * of byte arrays of given length.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @version $Revision: 183 $
 */
public class HexField extends Box
{
	private static final long serialVersionUID = 8103090655226548370L;

	private static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

	private int length;
	private JTextField textField;

	/**
	 * Constructs a new hex field of length 1.
	 */
	public HexField() {
		this(1);
	}

	/**
	 * Constructs a new hex field of length <code>byteCount</code>.
	 * 
	 * @param byteCount the length of this new hex field (in bytes)
	 */
	public HexField(int byteCount) {
		super(BoxLayout.X_AXIS);
		JLabel xLabel = new JLabel("0x");
		xLabel.setFont(FONT);
		add(xLabel);
		textField = new JTextField(2 * byteCount + 1);
		textField.setFont(FONT);
		this.length = byteCount;
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				format();
			}
		});
		textField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				format();
			}
		});
		add(textField);
		textField.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				scrubKeyTyped(e);
			}
		});
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
	 * @param length the new length of this hex fields
	 */
	public void setLength(int length) {
		if (length >= 0) {
			this.length = length;
			clearText();
			textField.setColumns(2 * length + 1);
		}
	}

	/**
	 * Sets the editability of this hex field.
	 * 
	 * @param editable indicates whether to enable or disable editability
	 */
	public void setEditable(boolean editable) {
		textField.setEditable(editable);
	}

	/**
	 * Clears this hex field.
	 */
	public void clearText() {
		textField.setText("");
	}

	/**
	 * Formats the text.
	 */
	void format() {
		textField.setText(format(textField.getText()));
	}

	private void scrubKeyTyped(KeyEvent e) {
		String validhex = "0123456789abcdefABCDEF";
		char c = e.getKeyChar();
		int len = textField.getText().length();
		if ((len < 2 * length || textField.getSelectedText() != null)
				&& validhex.indexOf(c) >= 0) {
			e.setKeyChar(Character.toUpperCase(c));
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
	 * @param The text to be formatted
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
		return Hex.hexStringToBytes(textField.getText());
	}

	public void setValue(long value) {
		byte[] newValue = new byte[length];
		for (int i = 0; i < length; i++) {
			int s = 8 * (length - i - 1);
			long mask = (0x00000000000000FFL << s);
			newValue[i] = (byte)((value & mask) >> s);
		}
		setValue(newValue);
	}

	/**
	 * Sets the value of this hex field to <code>value</code>.
	 * 
	 * @param value the new value
	 */
	public void setValue(byte[] value) {
		StringBuffer result = new StringBuffer();
		for (byte element : value) {
			result.append(Hex.byteToHexString(element));
		}
		textField.setText(result.toString());
	}

	/**
	 * Adds <code>l</code> to the action listener list of this hex field.
	 * 
	 * @param l the <code>ActionListener</code> to add
	 */
	public void addActionListener(ActionListener l) {
		textField.addActionListener(l);
	}

	public void addFocusListener(FocusListener l) {
		textField.addFocusListener(l);
	}

	/**
	 * Gets the preferred size of this component.
	 * 
	 * @return the preferred size of this component.
	 */
	public Dimension getPreferredSize() {
		int width = (int)super.getPreferredSize().getWidth();
		int height = (int)textField.getPreferredSize().getHeight();
		return new Dimension(width, height);
	}
}
