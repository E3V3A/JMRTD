/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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
 * $Id: FacePanel.java 206 2007-03-26 20:19:44Z martijno $
 */

package sos.mrtd.sample.newgui;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;

import sos.data.Country;
import sos.data.Gender;
import sos.gui.CountryEntryField;
import sos.gui.DateEntryField;
import sos.gui.GenderEntryField;
import sos.gui.GridLayout2;
import sos.gui.MRZEntryField;
import sos.mrtd.DG1File;
import sos.mrtd.MRZInfo;

/**
 * Panel for displaying and editing DG1.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public class HolderInfoPanel extends JPanel
{
	private static final Font KEY_FONT = new Font("Sans-serif", Font.PLAIN, 8);
	private static final Font VALUE_FONT = new Font("Monospaced", Font.PLAIN, 12);

	private MRZInfo info;
	private MRZEntryField surNameTF;
	private MRZEntryField givenNamesTF;
	private MRZEntryField documentNumberTF;
	private MRZEntryField personalNumberTF;
	private CountryEntryField nationalityTF;
	private CountryEntryField issuingStateTF;
	private DateEntryField dateOfBirthTF;
	private DateEntryField dateOfExpiryTF;
	private GenderEntryField genderTF;

	private Collection<ActionListener> listeners;

	public HolderInfoPanel(DG1File dg) {
		this(dg.getMRZInfo());
	}

	public HolderInfoPanel(MRZInfo nfo) {
		super(new GridLayout2(9, 2, 3, 3));
		this.info = nfo;
		listeners = new ArrayList<ActionListener>();
	
		StringBuffer nameStr = new StringBuffer();
		String[] firstNames = nfo.getSecondaryIdentifiers();
		for (int i = 0; i < firstNames.length; i++) {
			nameStr.append(firstNames[i]);
			if (i < (firstNames.length - 1)) { nameStr.append(" "); }
		}

		surNameTF = makeMRZEntryField(nfo.getPrimaryIdentifier());
		givenNamesTF = makeMRZEntryField(nameStr.toString());
		documentNumberTF = makeMRZEntryField(nfo.getDocumentNumber(), 9);
		personalNumberTF = makeMRZEntryField(nfo.getPersonalNumber(), 14);
		nationalityTF = makeCountryField(nfo.getNationality());
		issuingStateTF = makeCountryField(nfo.getIssuingState());
		dateOfBirthTF = makeDateEntryField(nfo.getDateOfBirth());
		dateOfExpiryTF = makeDateEntryField(nfo.getDateOfExpiry());
		genderTF = makeGenderEntryField(nfo.getGender());

		surNameTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setPrimaryIdentifier(surNameTF.getText());
				notifyActionPerformed(new ActionEvent(this, 0, "Primary identifier changed"));
			}
		});
		givenNamesTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setSecondaryIdentifiers(givenNamesTF.getText());
				notifyActionPerformed(new ActionEvent(this, 0, "Document number changed"));
			}
		});
		documentNumberTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setDocumentNumber(documentNumberTF.getText());
				notifyActionPerformed(new ActionEvent(this, 0, "Document number changed"));
			}
		});
		personalNumberTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setPersonalNumber(personalNumberTF.getText());
				notifyActionPerformed(new ActionEvent(this, 0, "Personal number changed"));
			}
		});
		nationalityTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setNationality(nationalityTF.getCountry());
				notifyActionPerformed(new ActionEvent(this, 0, "Nationality changed"));
			}
		});
		issuingStateTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setIssuingState(issuingStateTF.getCountry());
				notifyActionPerformed(new ActionEvent(this, 0, "Issuing state changed"));
			}
		});
		dateOfBirthTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setDateOfBirth(dateOfBirthTF.getDate());
				notifyActionPerformed(new ActionEvent(this, 0, "Date of birth changed"));
			}
		});
		dateOfExpiryTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setDateOfExpiry(dateOfExpiryTF.getDate());
				notifyActionPerformed(new ActionEvent(this, 0, "Date of expiry changed"));
			}
		});
		genderTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				info.setGender(genderTF.getGender());
				notifyActionPerformed(new ActionEvent(this, 0, "Gender changed"));
			}
		});

		add(makeKey("Surname")); add(surNameTF);
		add(makeKey("Given names")); add(givenNamesTF);
		add(makeKey("Document number")); add(documentNumberTF);
		add(makeKey("Personal number")); add(personalNumberTF);
		add(makeKey("Nationality")); add(nationalityTF);
		add(makeKey("Issuing state")); add(issuingStateTF);
		add(makeKey("Date of birth")); add(dateOfBirthTF);
		add(makeKey("Date of expiry")); add(dateOfExpiryTF);
		add(makeKey("Gender")); add(genderTF);
	}

	public MRZInfo getMRZ() {
		return info;
	}

	private Component makeKey(String key) {
		key = key.trim();
		JLabel c = new JLabel(key + ": ");
		c.setFont(KEY_FONT);
		return c;
	}

	private DateEntryField makeDateEntryField(Date date) {
		DateEntryField tf = new DateEntryField(date);
		tf.setFont(VALUE_FONT);
		return tf;
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
