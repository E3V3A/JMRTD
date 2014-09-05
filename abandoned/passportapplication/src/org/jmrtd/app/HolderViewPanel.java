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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.ParseException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.Gender;

import org.jmrtd.app.swing.CountryLabel;
import org.jmrtd.app.swing.DateLabel;
import org.jmrtd.app.swing.GenderLabel;
import org.jmrtd.lds.ICAOCountry;
import org.jmrtd.lds.MRZInfo;

/**
 * Panel for displaying and editing DG1.
 * TODO: maybe also involve DG11.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 894 $
 */
public class HolderViewPanel extends JPanel {

	private static final long serialVersionUID = -6169487570387029561L;

	private static final Dimension MAX_KEY_SIZE = new Dimension(100, 15);

	private enum Field {
		SURNAME,
		GIVEN_NAMES,
		DOCUMENT_NUMBER,
		PERSONAL_NUMBER,
		NATIONALITY,
		ISSUING_STATE,
		DATE_OF_BIRTH,
		DATE_OF_EXPIRY,
		GENDER;

		public String toString() {
			String s = super.toString();
			s = s.replace('_', ' ');
			s = s.substring(0, 1) + s.substring(1).toLowerCase();
			return s;
		}
	};

	private static final Font KEY_FONT = new Font("Sans-serif", Font.PLAIN, 10);
	private static final Font VALUE_FONT = new Font("Monospaced", Font.PLAIN, 12);

	private MRZInfo info;

	public HolderViewPanel(MRZInfo nfo) {
		this.info = nfo;
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		Component prevComp = null;
		for (Field field: Field.values()) {
			Component keyComp = makeKeyComp(field.toString());
			Component valueComp = makeValueComp(field, nfo);
			if (prevComp == null) {
				layout.putConstraint(SpringLayout.NORTH, keyComp, 5, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.NORTH, valueComp, 5, SpringLayout.NORTH, this);
			} else {
				layout.putConstraint(SpringLayout.NORTH, valueComp, 5, SpringLayout.SOUTH, prevComp);
				layout.putConstraint(SpringLayout.NORTH, keyComp, 5, SpringLayout.SOUTH, prevComp);
			}
			layout.putConstraint(SpringLayout.WEST, keyComp, 5, SpringLayout.WEST, this);
			layout.putConstraint(SpringLayout.WEST, valueComp, 5, SpringLayout.EAST, keyComp);
			add(keyComp);
			add(valueComp);
			if (keyComp.getHeight() >= valueComp.getHeight()) {
				prevComp = keyComp;
			} else {
				prevComp = valueComp;
			}
		}
	}

	public MRZInfo getMRZ() {
		return info;
	}

	private Component makeKeyComp(String key) {
		String keyString = key.trim() + ": ";
		JLabel c = new JLabel() {
			public Dimension getPreferredSize() {
				return MAX_KEY_SIZE;
			}
		};
		c.setText(keyString);
		c.setFont(KEY_FONT);
		return c;
	}

	private Component makeValueComp(Field field, MRZInfo nfo) {
		switch(field) {
		case SURNAME: {
			final JLabel tf = makeMRZLabel(nfo.getPrimaryIdentifier());
			return tf;
		}
		case GIVEN_NAMES: {
			StringBuffer nameStr = new StringBuffer();
			String[] firstNames = nfo.getSecondaryIdentifierComponents();
			for (int i = 0; i < firstNames.length; i++) {
				nameStr.append(firstNames[i]);
				if (i < (firstNames.length - 1)) { nameStr.append(" "); }
			}
			final JLabel tf = makeMRZLabel(nameStr.toString());
			return tf;
		}
		case DOCUMENT_NUMBER: {
			final JLabel tf = makeMRZLabel(nfo.getDocumentNumber(), 9);
			return tf;
		}
		case PERSONAL_NUMBER: {
			final JLabel tf = makeMRZLabel(nfo.getPersonalNumber(), 14);
			return tf;
		}
		case NATIONALITY: {
			/* FIXME: germany uses "D<<" instead of "DEU". */
			final CountryLabel tf = makeCountryField(ICAOCountry.getInstance(nfo.getNationality()));
			return tf;
		}
		case ISSUING_STATE: {
			/* FIXME: germany uses "D<<" instead of "DEU". */
			final CountryLabel tf = makeCountryField(ICAOCountry.getInstance(nfo.getIssuingState()));
			return tf;
		}
		case DATE_OF_BIRTH: {
			final DateLabel tf = makeDateLabel(nfo.getDateOfBirth());
			return tf;
		}
		case DATE_OF_EXPIRY: {
			final DateLabel tf = makeDateLabel(nfo.getDateOfExpiry());
			return tf;
		}
		case GENDER: {
			final GenderLabel tf = makeGenderLabel(nfo.getGender());
			return tf;
		}
		}
		return null;
	}

	private DateLabel makeDateLabel(String date) {
		try {
			DateLabel tf = new DateLabel(date);
			tf.setFont(VALUE_FONT);
			return tf;
		} catch (ParseException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid date");
		}
	}

	private JLabel makeMRZLabel(String value) {
		return makeMRZLabel(value, 20);
	}

	private JLabel makeMRZLabel(String value, int width) {
		value = value.trim();
		JLabel tf = new JLabel(value);
		tf.setFont(VALUE_FONT);
		return tf;
	}

	private CountryLabel makeCountryField(Country country) {
		final CountryLabel tf =  new CountryLabel(country);
		tf.setFont(VALUE_FONT);
		return tf;
	}

	private GenderLabel makeGenderLabel(Gender gender) {
		GenderLabel tf = new GenderLabel(gender);
		tf.setFont(VALUE_FONT);
		return tf;
	}
}
