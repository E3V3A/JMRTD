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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import net.sourceforge.scuba.data.Country;
import net.sourceforge.scuba.data.Gender;
import net.sourceforge.scuba.swing.CountryEntryField;
import net.sourceforge.scuba.swing.DateEntryField;
import net.sourceforge.scuba.swing.GenderEntryField;
import net.sourceforge.scuba.swing.MRZEntryField;

import org.jmrtd.lds.MRZInfo;

/**
 * Panel for displaying and editing DG1.
 * TODO: maybe also involve DG11.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 894 $
 */
public class HolderEditPanel extends JPanel
{
	private static final long serialVersionUID = -6169486570387029561L;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");	
	
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

	private Collection<ActionListener> listeners;

	public HolderEditPanel(MRZInfo nfo) {
		this.info = nfo;
		listeners = new ArrayList<ActionListener>();
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
			if (keyComp.getHeight() > valueComp.getHeight()) {
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
		key = key.trim();
		JLabel c = new JLabel(key + ": ");
		c.setFont(KEY_FONT);
		return c;
	}

	private Component makeValueComp(Field field, MRZInfo nfo) {
		switch(field) {
		case SURNAME: {
			final MRZEntryField tf = makeMRZEntryField(nfo.getPrimaryIdentifier());
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setPrimaryIdentifier(tf.getText());
					notifyActionPerformed(new ActionEvent(this, 0, "Primary identifier changed"));
				}
			});
			return tf;
		}
		case GIVEN_NAMES: {
			StringBuffer nameStr = new StringBuffer();
			String[] firstNames = nfo.getSecondaryIdentifiers();
			for (int i = 0; i < firstNames.length; i++) {
				nameStr.append(firstNames[i]);
				if (i < (firstNames.length - 1)) { nameStr.append(" "); }
			}
			final MRZEntryField tf = makeMRZEntryField(nameStr.toString());
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setSecondaryIdentifiers(tf.getText());
					notifyActionPerformed(new ActionEvent(this, 0, "Document number changed"));
				}
			});
			return tf;
		}
		case DOCUMENT_NUMBER: {
			final MRZEntryField tf = makeMRZEntryField(nfo.getDocumentNumber(), 9);
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setDocumentNumber(tf.getText());
					notifyActionPerformed(new ActionEvent(this, 0, "Document number changed"));
				}
			});
			return tf;
		}
		case PERSONAL_NUMBER: {
			final MRZEntryField tf = makeMRZEntryField(nfo.getPersonalNumber(), 14);
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setPersonalNumber(tf.getText());
					notifyActionPerformed(new ActionEvent(this, 0, "Personal number changed"));
				}
			});
			return tf;
		}
		case NATIONALITY: {
			final CountryEntryField tf = makeCountryField(nfo.getNationality());
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setNationality(tf.getCountry());
					notifyActionPerformed(new ActionEvent(this, 0, "Nationality changed"));
				}
			});
			return tf;
		}
		case ISSUING_STATE: {
			final CountryEntryField tf = makeCountryField(nfo.getIssuingState());
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setIssuingState(tf.getCountry());
					notifyActionPerformed(new ActionEvent(this, 0, "Issuing state changed"));
				}
			});
			return tf;
		}
		case DATE_OF_BIRTH: {
			final DateEntryField tf = makeDateEntryField(nfo.getDateOfBirth());
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setDateOfBirth(tf.toCompactString(DateEntryField.YEAR_MODE_2_DIGITS));
					notifyActionPerformed(new ActionEvent(this, 0, "Date of birth changed"));
				}
			});
			return tf;
		}
		case DATE_OF_EXPIRY: {
			final DateEntryField tf = makeDateEntryField(nfo.getDateOfExpiry());
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setDateOfExpiry(tf.toCompactString(DateEntryField.YEAR_MODE_2_DIGITS));
					notifyActionPerformed(new ActionEvent(this, 0, "Date of expiry changed"));
				}
			});
			return tf;
		}
		case GENDER: {
			final GenderEntryField tf = makeGenderEntryField(nfo.getGender());
			tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					info.setGender(tf.getGender());
					notifyActionPerformed(new ActionEvent(this, 0, "Gender changed"));
				}
			});
			return tf;
		}
		}
		return null;
	}

	private DateEntryField makeDateEntryField(String date) {
		try {
			DateEntryField tf = new DateEntryField(date);
			tf.setFont(VALUE_FONT);
			return tf;
		} catch (ParseException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Illegal date " + date);
		}
	}

	private MRZEntryField makeMRZEntryField(String value) {
		return makeMRZEntryField(value, 20);
	}

	private MRZEntryField makeMRZEntryField(String value, int width) {
		value = value.trim();
		int textSize = Math.max(value.length(), width);
		MRZEntryField tf = new MRZEntryField(textSize);
		tf.setText(value);
		tf.setFont(VALUE_FONT);
		return tf;
	}

	private CountryEntryField makeCountryField(Country country) {
		final CountryEntryField tf =  new CountryEntryField(country);
		tf.setFont(VALUE_FONT);
		return tf;
	}

	private GenderEntryField makeGenderEntryField(Gender gender) {
		GenderEntryField tf = new GenderEntryField(gender);
		tf.setFont(VALUE_FONT);
		return tf;
	}

	public void addActionListener(ActionListener l) {
		listeners.add(l);
	}

	private void notifyActionPerformed(ActionEvent e) {
		for (ActionListener l: listeners) { l.actionPerformed(e); }
	}
}
