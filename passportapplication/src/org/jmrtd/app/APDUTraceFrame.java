/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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

package org.jmrtd.app;

import java.awt.Font;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.util.Hex;

public class APDUTraceFrame extends JFrame implements APDUListener
{
	private static final long serialVersionUID = -584060710792989841L;

	private JTextArea area;

	public APDUTraceFrame() {
		this("APDU trace");
	}
	
	public APDUTraceFrame(String title) {
		super(title);
		area = new JTextArea(40, 80);
		area.setFont(new Font("Monospaced", Font.PLAIN, 9));
		getContentPane().add(new JScrollPane(area));
	}

	public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
		area.append("C:\n" + Hex.bytesToPrettyString(capdu.getBytes()) + "\n");
		area.append("R:\n" + Hex.bytesToPrettyString(rapdu.getBytes()) + "\n");
		area.setCaretPosition(area.getDocument().getLength() - 1);
	}	
}
