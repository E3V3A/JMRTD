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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.jmrtd.app.util.IconUtil;

/**
 * Component for setting date (just day, month, year for now).
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class DateEntryField extends Box
{
	public static final int YEAR_MODE_2_DIGITS = 2, YEAR_MODE_4_DIGITS = 4;

	private static final long serialVersionUID = -8604165563369764876L;

	private static final SimpleDateFormat
	PRESENTATION_SDF = new SimpleDateFormat("dd MMM yyyy"),
	PARSER_6_DIGITS_SDF = new SimpleDateFormat("yyMMdd"),
	PARSER_8_DIGITS_SDF = new SimpleDateFormat("yyyyMMdd");
//	YEAR_2_SDF = new SimpleDateFormat("yy"),
//	YEAR_4_SDF = new SimpleDateFormat("yyyy")

	private static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);
	private static final Icon DATE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("date"));

	private int yearMode;
	private Calendar cal;
	private JComboBox monthComboBox;
	private NumField dayNumField;
	private NumField yearNumField ;

	private Collection<ActionListener> listeners;

	private DateEntryField() {
		super(BoxLayout.X_AXIS);
		listeners = new ArrayList<ActionListener>();
		cal = Calendar.getInstance();
		monthComboBox = new JComboBox();
		monthComboBox.setFont(FONT);
		monthComboBox.addItem("Jan");
		monthComboBox.addItem("Feb");
		monthComboBox.addItem("Mar");
		monthComboBox.addItem("Apr");
		monthComboBox.addItem("May");
		monthComboBox.addItem("Jun");
		monthComboBox.addItem("Jul");
		monthComboBox.addItem("Aug");
		monthComboBox.addItem("Sep");
		monthComboBox.addItem("Oct");
		monthComboBox.addItem("Nov");
		monthComboBox.addItem("Dec");
		cal.set(Calendar.MONTH, 0);
		dayNumField = new NumField(2, 1, 31);
	}

	public DateEntryField(int yearMode) {
		this();
		this.yearMode = yearMode;
		switch (yearMode) {
		case YEAR_MODE_2_DIGITS:
			yearNumField = new NumField(2, 00, 99);
			break;
		case YEAR_MODE_4_DIGITS:
			yearNumField = new NumField(2, 0000, 9999);
			break;
		default:
			throw new IllegalArgumentException("Illegal year mode.");
		}

		add(new JLabel(DATE_ICON));
		add(Box.createHorizontalStrut(10));
		add(dayNumField);
		add(monthComboBox);
		add(yearNumField);

		monthComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String choice = e.getItem().toString();
				int month = 0;
				if (choice.equals("Jan")) { month = 0; }
				if (choice.equals("Feb")) { month = 1; }
				if (choice.equals("Mar")) { month = 2; }
				if (choice.equals("Apr")) { month = 3; }
				if (choice.equals("May")) { month = 4; }
				if (choice.equals("Jun")) { month = 5; }
				if (choice.equals("Jul")) { month = 6; }
				if (choice.equals("Aug")) { month = 7; }
				if (choice.equals("Sep")) { month = 8; }
				if (choice.equals("Oct")) { month = 9; }
				if (choice.equals("Nov")) { month = 10; }
				if (choice.equals("Dec")) { month = 11; }
				cal.set(Calendar.MONTH, month);
				notifyActionPerformed(new ActionEvent(this, 0, "Date changed"));
			}
		});

		dayNumField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int day = (int)dayNumField.getValue();
				cal.set(Calendar.DATE, day);
				notifyActionPerformed(new ActionEvent(this, 0, "Date changed"));
			}
		});

		dayNumField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {				
			}

			public void focusLost(FocusEvent e) {
				int day = (int)dayNumField.getValue();
				cal.set(Calendar.DATE, day);
				notifyActionPerformed(new ActionEvent(this, 0, "Date changed"));
			}
		});

		yearNumField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int year = (int)yearNumField.getValue();
				cal.set(Calendar.YEAR, year);
				notifyActionPerformed(new ActionEvent(this, 0, "Date changed"));
			}
		});

		yearNumField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {				
			}

			public void focusLost(FocusEvent e) {
				int year = (int)yearNumField.getValue();
				cal.set(Calendar.YEAR, year);
				notifyActionPerformed(new ActionEvent(this, 0, "Date changed"));
			}
		});
	}

	public DateEntryField(Date date) {
		this(YEAR_MODE_4_DIGITS);
		setDate(date);
	}

	public DateEntryField(String dateString) throws ParseException {
		this(getYearMode(dateString.trim()));
		setDate(dateString);
	}

	private static int getYearMode(String dateString) {
		switch(dateString.length()) {
		case 6: return YEAR_MODE_2_DIGITS;
		case 8: return YEAR_MODE_4_DIGITS;
		default: throw new IllegalArgumentException("Invalid date " + dateString);
		}
	}

	public void setDate(String dateString) throws ParseException {
		if (dateString == null) {
			throw new IllegalArgumentException("Invalid date: null");
		}
		dateString = dateString.trim();
		switch(dateString.length()) {
		case 6:
			setDate(PARSER_6_DIGITS_SDF.parse(dateString));
			break;
		case 8:
			setDate(PARSER_8_DIGITS_SDF.parse(dateString));
			break;
		default:
			throw new IllegalArgumentException("Invalid date: " + dateString);
		}
	}

	public void setDate(Date date) {
		cal.setTime(date);
		dayNumField.setValue(cal.get(Calendar.DATE));
		monthComboBox.setSelectedIndex(cal.get(Calendar.MONTH));
		switch(yearMode) {
		case YEAR_MODE_2_DIGITS:
			yearNumField.setValue(cal.get(Calendar.YEAR) % 100);
			break;
		case YEAR_MODE_4_DIGITS:
			yearNumField.setValue(cal.get(Calendar.YEAR));
			break;
		default:
			throw new IllegalStateException("Illegal year mode");
		}
		revalidate();
		repaint();
		notifyActionPerformed(new ActionEvent(this, 0, "Date changed"));
	}

	public Date getDate() {
		return cal.getTime();
	}

	public String toString() {
		return PRESENTATION_SDF.format(cal.getTime());
	}

	public String toCompactString(int yearMode) {
		switch (yearMode) {
		case YEAR_MODE_2_DIGITS:
			return PARSER_6_DIGITS_SDF.format(cal.getTime());
		case YEAR_MODE_4_DIGITS:
			return PARSER_8_DIGITS_SDF.format(cal.getTime());
		}
		throw new IllegalStateException("Undetermined year mode");
	}

	public void setEnabled(boolean b) {
		monthComboBox.setEnabled(b);
		dayNumField.setEnabled(b);
		yearNumField.setEnabled(b);
	}

	public void addActionListener(ActionListener l) {
		listeners.add(l);
	}

	private void notifyActionPerformed(ActionEvent e) {
		for (ActionListener l: listeners) {
			l.actionPerformed(e);
		}
	}

	public Dimension getPreferredSize() {
		double wm = monthComboBox.getPreferredSize().getWidth(),
		wd = dayNumField.getPreferredSize().getWidth(),
		wy = yearNumField.getPreferredSize().getWidth(),
		hm = monthComboBox.getPreferredSize().getHeight(),
		hd = dayNumField.getPreferredSize().getHeight(),
		hy = yearNumField.getPreferredSize().getHeight(),
		iw = DATE_ICON.getIconWidth(),
		ih = DATE_ICON.getIconHeight();

		return new Dimension((int)(iw + 10 + wd + wm + wy), (int)Math.max(Math.max(hd, hm), Math.max(ih, hy)));
	}
}
