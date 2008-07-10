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

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import sos.data.Country;
import sos.data.Gender;
import sos.gui.CountryLabel;
import sos.gui.GenderLabel;
import sos.gui.GridLayout2;
import sos.mrtd.DG1File;
import sos.mrtd.MRZInfo;

/**
 * GUI component for displaying the MRZ datagroup on the passport.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: $
 */
public class HolderInfoPanel extends JPanel
{
	private static final Color KEY_COLOR = new Color(0x1A1A1A);
	private static final Font KEY_FONT = new Font("Sans-serif", Font.PLAIN, 8);

	private static final Color VALUE_FOREGROUND_COLOR = new Color(0x1A1A1A);
	private static final Color VALUE_BACKGROUND_COLOR = new Color(0xFFF8F8);
	private static final Font VALUE_FONT = new Font("Monospaced", Font.PLAIN, 12);

	private static final Color MRZ_FOREGROUND_COLOR = new Color(0x000000);
	private static final Color MRZ_BACKGROUND_COLOR = new Color(0xFFFFFF);
	private static final Font MRZ_FONT = new Font("Monospaced", Font.BOLD, 16);

	private static final SimpleDateFormat SDF =
		new SimpleDateFormat("MMMM dd, yyyy");

	private MRZInfo info;

	public HolderInfoPanel(DG1File dg) {
		super(new FlowLayout());
		info = dg.getMRZInfo();
		List<String> keys = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();

		StringBuffer nameStr = new StringBuffer();
		String[] firstNames = info.getSecondaryIdentifiers();
		for (int i = 0; i < firstNames.length; i++) {
			nameStr.append(firstNames[i]);
			if (i < (firstNames.length - 1)) { nameStr.append(" "); }
		}
		keys.add("Surname"); values.add(info.getPrimaryIdentifier());
		keys.add("Given names"); values.add(nameStr.toString());
		keys.add("Document number"); values.add(info.getDocumentNumber());
		keys.add("Personal number"); values.add(info.getPersonalNumber());
		keys.add("Nationality"); values.add(info.getNationality());
		keys.add("Issuing state"); values.add(info.getIssuingState());
		keys.add("Date of birth"); values.add(info.getDateOfBirth());
		keys.add("Date of expiry"); values.add(info.getDateOfExpiry());
		keys.add("Gender"); values.add(info.getGender());
		add(makePropertiesDisplay(keys, values));
	}

	private Component makePropertiesDisplay(List<String> keys, List<Object> values) {
		JPanel result = new JPanel(new GridLayout2(keys.size(), 2, 3, 3));
		Iterator<Object> valuesIt = values.listIterator();
		for (String key: keys) {
			Object value = valuesIt.next();
			result.add(makeKey(key));
			result.add(makeValue(key, value));
		}
		return result;
	}

	
	private Component makeValue(String key, Object value) {
		key = key.trim();
		if (value instanceof Date) {
			String valueString = SDF.format((Date)value).trim();
			JLabel lbl = new JLabel(valueString);
			lbl.setFont(VALUE_FONT);
			return lbl;
		} else if (value instanceof Gender) {
			GenderLabel lbl = new GenderLabel((Gender)value);
			lbl.setFont(VALUE_FONT);
			return lbl;
		} else if (value instanceof Country) {
			CountryLabel lbl =  new CountryLabel((Country)value);
			lbl.setFont(VALUE_FONT);
			return lbl;
		} else {
			String valueString = value.toString().trim();
			JTextField tf = new JTextField(Math.max(valueString.length(), 20));
			tf.setText(valueString);
			tf.setFont(VALUE_FONT);
			return tf;
		}
	}

	private Component makeKey(String key) {
		key = key.trim();
		JLabel c = new JLabel(key + ": ");
		c.setFont(KEY_FONT);
//		c.setForeground(KEY_COLOR);
		return c;
	}
}
