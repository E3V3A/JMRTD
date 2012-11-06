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

import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.sourceforge.scuba.data.Country;

import org.jmrtd.app.util.IconUtil;

public class CountryEntryField extends Box
{
	private static final long serialVersionUID = 3508225695511326613L;

	private static final int MAX_COUNTRY_NAME_LENGTH = 20;

	private JLabel iconLabel;
	private JComboBox<CountryDisplayContainer> comboBox;

	public CountryEntryField() {
		super(BoxLayout.X_AXIS);
		iconLabel = new JLabel();
		comboBox = new JComboBox<CountryDisplayContainer>();
		Country[] countryValues = Country.values();
		for (int i = 0; i < countryValues.length; i++) {
			comboBox.addItem(new CountryDisplayContainer(countryValues[i]));
		}
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setCountry(getCountry());
			}
		});
		comboBox.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				setCountry(getCountry());
			}

			public void focusLost(FocusEvent e) {
				setCountry(getCountry());
			}	
		});
		add(iconLabel);
		add(Box.createHorizontalStrut(10));
		add(comboBox);
	}

	public CountryEntryField(Country country) {
		this();
		setCountry(country);
	}

	public Country getCountry() {
		CountryDisplayContainer c = (CountryDisplayContainer)comboBox.getSelectedItem();
		return c.getCountry();
	}

	public void setCountry(Country country) {
		iconLabel.setIcon(getIcon(country));
		iconLabel.revalidate(); iconLabel.repaint();
		CountryDisplayContainer c = new CountryDisplayContainer(country);
		comboBox.setSelectedItem(c);
		comboBox.revalidate(); comboBox.repaint();
	}

	public void setFont(Font font) {
		super.setFont(font);
		comboBox.setFont(font);
		int itemCount = comboBox.getItemCount();
		for (int i = 0; i < itemCount; i++) {
			Object item = comboBox.getItemAt(i);
			if (item instanceof Component) {
				((Component)item).setFont(font);
			}
		}
	}

	private Icon getIcon(Country country) {
		ImageIcon flagIcon = new ImageIcon();
		Image flagImage = IconUtil.getFlagImage(country);
		if (flagImage != null) { flagIcon.setImage(flagImage); }
		return flagIcon;
	}

	private static class CountryDisplayContainer {

		private Country country;

		public CountryDisplayContainer(Country country) {
			this.country = country;
		}

		public Country getCountry() { return country; }

		public String toString() {
			String name = country.getName();
			int length = Math.min(name.length(), MAX_COUNTRY_NAME_LENGTH);
			if (length == name.length()) {
				return country.toAlpha2Code() + " " + name;
			} else {
				return country.toAlpha2Code() + " " + name.substring(0, MAX_COUNTRY_NAME_LENGTH - 3) + "...";
			}
		}

		public boolean equals(Object other) {
			if (other == null) { return false; }
			if (other == this) { return true; }
			if (!other.getClass().equals(this.getClass())) { return false; }
			return ((CountryDisplayContainer)other).getCountry().equals(country);
		}
		
		public int hashCode() {
			return 7 * country.hashCode() + 111;
		}
	}

	public void addActionListener(ActionListener l) {
		comboBox.addActionListener(l);
	}
}
