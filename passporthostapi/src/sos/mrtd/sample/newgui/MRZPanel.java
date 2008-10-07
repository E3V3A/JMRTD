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
 * $Id: $
 */

package sos.mrtd.sample.newgui;

import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import sos.mrtd.DG1File;
import sos.mrtd.MRZInfo;

/**
 * Panel for displaying the MRZ datagroup on the passport.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public class MRZPanel extends JPanel
{
	private static final Font MRZ_FONT = new Font("Monospaced", Font.BOLD, 15);

	private MRZInfo info;
	private JTextArea c;

	public MRZPanel(DG1File dg) {
		this(dg.getMRZInfo());
	}

	public MRZPanel(MRZInfo info) {
		super(new FlowLayout());
		c = new JTextArea();
		add(c);
		setMRZ(info);
	}

	public void setMRZ(MRZInfo info) {
		this.info = info;
		c.setEditable(false);
		c.setFont(MRZ_FONT);
		c.setText(info.toString().trim());
		revalidate(); repaint();
	}
	
	public MRZInfo getMRZ() {
		return info;
	}
}
