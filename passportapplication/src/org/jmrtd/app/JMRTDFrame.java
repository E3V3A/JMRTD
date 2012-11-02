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
 * $Id: KeyFrame.java 893 2009-03-23 15:43:42Z martijno $
 */

package org.jmrtd.app;

import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jmrtd.app.util.IconUtil;

/**
 * Base class for JMRTD windows.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class JMRTDFrame extends JFrame {

	private static final long serialVersionUID = 7491093832758295366L;

	private static final Image JMRTD_ICON = IconUtil.getImage("jmrtd_logo-48x48", JMRTDApp.class);

	protected static final Icon SAVE_AS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("disk"));
	protected static final Icon CLOSE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("bin"));

	/**
	 * Constructs a JMRTD frame.
	 * 
	 * @param title the title of this frame (without the &quot;JMRTD -&quot; prefix)
	 */
	public JMRTDFrame(String title) {
		super("JMRTD - " + title);
		setIconImage(JMRTD_ICON);
	}	
}
