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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sourceforge.scuba.data.Country;

import org.jmrtd.app.util.IconUtil;

public class CountryLabel extends Box
{
	private static final long serialVersionUID = 4580680157430310682L;

	private Country country;
	private JLabel flagLabel, nameLabel;

	public CountryLabel(Country country) {
		super(BoxLayout.X_AXIS);
		this.country = country;
		flagLabel = new JLabel(getIcon(country));
		nameLabel = new JLabel(country.getName());
		add(flagLabel);
		add(Box.createHorizontalStrut(10));
		add(nameLabel);
	}

	public void setFont(Font font) {
		super.setFont(font);
		nameLabel.setFont(font);
	}

	public Country getCountry() {
		return country;
	}

	private Icon getIcon(Country country) {
		ImageIcon flagIcon = new ImageIcon();
		Image flagImage = IconUtil.getFlagImage(country);
		if (flagImage != null) { flagIcon.setImage(flagImage); }
		return flagIcon;
	}
}
