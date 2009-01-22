/*
 * $Id $
 */

package sos.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;

/**
 * Component for setting date (just day, month, year for now).
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class DateEntryField extends Box
{
	private static final long serialVersionUID = -8604165563369764876L;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd MMM yyyy");
	private static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

	private Calendar cal;
	private JComboBox monthComboBox;
	private NumField dayNumField;
	private NumField yearNumField ;
	
	private Collection<ActionListener> listeners;

	public DateEntryField() {
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
		yearNumField = new NumField(4, 0000, 9999);
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
		this();
		setDate(date);
	}

	public void setDate(Date date) {
		cal.setTime(date);
		dayNumField.setValue(cal.get(Calendar.DATE));
		monthComboBox.setSelectedIndex(cal.get(Calendar.MONTH));
		yearNumField.setValue(cal.get(Calendar.YEAR));
		revalidate();
		repaint();
		notifyActionPerformed(new ActionEvent(this, 0, "Date changed"));
	}

	public Date getDate() {
		return cal.getTime();
	}

	public String toString() {
		return SDF.format(cal.getTime());
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
		hy = yearNumField.getPreferredSize().getHeight();

		return new Dimension((int)(wd + wm + wy), (int)Math.max(Math.max(hd, hm), hy));
	}
}
