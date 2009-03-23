/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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
 * $Id$
 */

package org.jmrtd.app;

import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jmrtd.MRZInfo;

import net.sourceforge.scuba.util.Fonts;

/**
 * Panel for displaying the MRZ datagroup on the passport.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision$
 */
public class MRZPanel extends JPanel
{
	private static final long serialVersionUID = -8078528859489283298L;

	private static final String MRZ_FONT_NAME = "OCR-B_10_BT.ttf";

	private Font mrzFont;

	private MRZInfo info;
	private JTextArea c;

	public MRZPanel(MRZInfo info) {
		super(new FlowLayout());
		try {
			mrzFont = Fonts.getFont(MRZ_FONT_NAME, Font.PLAIN, 14);
		} catch (Exception e) {
			mrzFont = new Font("Monospaced", Font.BOLD, 14);
		}
		c = new JTextArea();
		add(c);
		setMRZ(info);
	}

	public void setMRZ(MRZInfo info) {
		this.info = info;
		c.setEditable(false);
		c.setFont(mrzFont);
		c.setText(info.toString().trim());
		revalidate(); repaint();
	}

	public MRZInfo getMRZ() {
		return info;
	}
}
