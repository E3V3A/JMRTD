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

import java.awt.Font;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sourceforge.scuba.data.Gender;

import org.jmrtd.app.util.IconUtil;

public class GenderLabel extends Box
{
	private static final long serialVersionUID = -4210216741229320608L;

	private Gender gender;
	private JLabel  textLabel;

	public GenderLabel(Gender gender) {
		super(BoxLayout.X_AXIS);
		this.gender = gender;
		String name = null;
		Image image = null;
		switch (gender) {
		case MALE: name = "Male"; image = IconUtil.getFamFamFamSilkIcon("male"); break;
		case FEMALE: name = "Female"; image = IconUtil.getFamFamFamSilkIcon("female"); break;
		case UNKNOWN: name = "Unknown"; image = IconUtil.getFamFamFamSilkIcon("error"); break;
		case UNSPECIFIED: name = "Unspecified"; IconUtil.getFamFamFamSilkIcon("error"); break;
		}
		if (name != null) {	
			if (image != null) {
				add(new JLabel(new ImageIcon(image)));
				add(Box.createHorizontalStrut(10));
			}
			textLabel = new JLabel(name);
			add(textLabel);
		}
	}

	public void setFont(Font font) {
		super.setFont(font);
		textLabel.setFont(font);
	}

	public Gender getGender() {
		return gender;
	}
	
	public String toString() {
		return gender.toString();
	}
}
