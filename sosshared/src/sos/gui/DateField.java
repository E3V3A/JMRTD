/*
 * $Id $
 */

package sos.gui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;

/**
 * Component for setting date (just day, month, year for now).
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class DateField extends Box
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd MMM HH:mm:ss");
	private static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

	private Calendar cal;

	public DateField() {
		super(BoxLayout.X_AXIS);
		cal = Calendar.getInstance();
		JComboBox monthComboBox = new JComboBox();
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

		final NumField dayNumField = new NumField(2, 1, 31);
		final NumField yearNumField = new NumField(4, 0000, 9999);
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
			}
		});

		dayNumField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int day = (int)dayNumField.getValue();
				cal.set(Calendar.DATE, day);
			}
		});

		dayNumField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {				
			}

			public void focusLost(FocusEvent e) {
				int day = (int)dayNumField.getValue();
				cal.set(Calendar.DATE, day);
			}
		});

		yearNumField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int year = (int)yearNumField.getValue();
				cal.set(Calendar.YEAR, year);
			}
		});

		yearNumField.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {				
			}

			public void focusLost(FocusEvent e) {
				int year = (int)yearNumField.getValue();
				cal.set(Calendar.YEAR, year);
			}
		});
	}

	public void setDate(Date date) {
		cal.setTime(date);
	}

	public Date getDate() {
		return cal.getTime();
	}

	public String toString() {
		return SDF.format(cal.getTime());
	}
}
