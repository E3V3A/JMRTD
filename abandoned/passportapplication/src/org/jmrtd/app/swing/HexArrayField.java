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

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * A panel with a variable number of singleton <code>HexField</code> cells.
 * It provides resizing buttons and a length indicator.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision: 183 $
 */
public class HexArrayField extends JPanel implements ActionListener
{
	private static final long serialVersionUID = 5538349254614320575L;

	private int length;
	private JLabel lcLbl;
	private HexField lcHexLbl;
	private BasicArrowButton smallerButton, largerButton;

	private static final Font FONT = new Font("Monospaced",Font.PLAIN,10);
	private static final Color FOREGROUND = Color.black;

	/**
	 * Constructs a new field of 0 cells.
	 */
	public HexArrayField() {
		super(new FlowLayout(FlowLayout.LEFT,0,0));
		setFont(FONT);
		setForeground(FOREGROUND);
		lcLbl = new JLabel("lc: ");
		lcLbl.setEnabled(false);
		lcHexLbl = new HexField(1);
		lcHexLbl.setEditable(false);
		lcHexLbl.setEnabled(false);
		smallerButton = new BasicArrowButton(BasicArrowButton.WEST);
		smallerButton.addActionListener(this);
		largerButton = new BasicArrowButton(BasicArrowButton.EAST);
		largerButton.addActionListener(this);
		add(lcLbl);
		add(lcHexLbl);
		add(smallerButton);
		add(largerButton);
		this.length = 0;
	}

	/**
	 * Constructs a new field of <code>length</code> cells.
	 *
	 * @param length The number of cells.
	 */
	public HexArrayField(int length) {
		this();
		this.length = length;
		for (int i = 0; i < length; i++) {
			addCell();
		}
	}

	/**
	 * The hexadecimal value that is contained
	 * in the cells of this field.
	 *
	 * @return The hexadecimal value of this.
	 */
	public byte[] getValue() {
		byte[] data = new byte[length];
		for (int i=0; i < length; i++) {
			/* (1) lcLbl, (2) lcHexLbl, (3) smallerButton, (4) largerButton. */
			HexField tf = (HexField)getComponent(4 + i);
			data[i] = tf.getValue()[0];
		}
		return data;
	}

	/**
	 * Sets a new value.
	 * 
	 * @param data the new value to set
	 */
	public void setValue(byte[] data) {
		if (data == null) {
			reset();
			return;
		}
		int cellCount = length;
		if (data.length < cellCount) {
			for (int i = 0; i < (cellCount - data.length); i++) {
				removeCell();
			}
		} else if (data.length > cellCount) {
			for (int i = 0; i < (data.length - cellCount); i++) {
				addCell();
			}
		}
		/*@ assert data.length == length; */
		for (int i = 0; i < data.length; i++) {    
			/* (1) lcLbl, (2) lcHexLbl, (3) smallerButton, (4) largerButton. */
			HexField tf = (HexField)getComponent(4 + i);
			tf.setValue(data[i]);
		}
	}

	public void setEnabled(boolean b) {
		super.setEnabled(b);
		for (int i = 0; i < 4 + length; i++) {
			getComponent(i).setEnabled(b);
		}
	}

	/**
	 * The number of cells in this field.
	 *
	 * @return The number of cells in this field.
	 */
	public int length() {
		return length;
	}

	/**
	 * Adds a new cell to the right.
	 */
	public void addCell() {
		if (length < 255) {
			length++;
			lcHexLbl.setValue((byte)length);
			add(new HexField());
		} else {
			length = 255;
			lcHexLbl.setValue((byte)length);
		}
		lcLbl.setEnabled(length > 0);
		lcHexLbl.setEnabled(length > 0);
	}

	/**
	 * Removes the rightmost cell.
	 */
	public void removeCell() {
		if (length > 0) {
			/* (1) lcLbl, (2) lcHexLbl, (3) smallerButton, (4) largerButton. */
			length--;
			lcHexLbl.setValue((byte)length);
			remove(length + 4);
		} else {
			length = 0;
			lcHexLbl.setValue((byte)length);
		}
		lcLbl.setEnabled(length > 0);
		lcHexLbl.setEnabled(length > 0);
	}

	/**
	 * Resets to a zero-length hex array field.
	 */
	public void reset() {
		int cellCount = length;
		for (int i = 0; i < cellCount; i++) {
			removeCell();
		}
	}

	/**
	 * Gets called when the user presses one of the
	 * buttons to resize the number of cells.
	 *
	 * @param ae the event
	 */
	public void actionPerformed(ActionEvent ae) {
		JButton but = (JButton)ae.getSource();
		if (but == largerButton) {
			addCell();
		} else if (but == smallerButton) {
			removeCell();
		}
		getParent().validate();
	}
}

