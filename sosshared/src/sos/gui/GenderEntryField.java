package sos.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import sos.data.Gender;
import sos.util.Icons;

public class GenderEntryField extends Box
{
	private static final Icon MALE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("male"));
	private static final Icon FEMALE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("female"));
	private static final Icon UNKNOWN_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("error"));
	private static final Icon UNSPECIFIED_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("error"));
	
	private JLabel iconLabel;
	private JComboBox comboBox;

	public GenderEntryField() {
		this(Gender.UNSPECIFIED);
	}
	
	public GenderEntryField(Gender gender) {
		super(BoxLayout.X_AXIS);
		iconLabel = new JLabel(getIcon(gender));
		comboBox = new JComboBox();
		comboBox.addItem("Male");
		comboBox.addItem("Female");
		comboBox.addItem("Unknown");
		comboBox.addItem("Unspecified");
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setGender(getGender());
			}
		});
		setGender(gender);
		add(iconLabel);
		add(Box.createHorizontalStrut(10));
		add(comboBox);
	}

	public Gender getGender() {
		String genderString = (String)comboBox.getSelectedItem();
		return Gender.valueOf(genderString.toUpperCase());
	}

	public void setGender(Gender gender) {
		iconLabel.setIcon(getIcon(gender));
		iconLabel.revalidate(); iconLabel.repaint();
		switch (gender) {
		case MALE: comboBox.setSelectedIndex(0); break;
		case FEMALE: comboBox.setSelectedIndex(1); break;
		case UNKNOWN: comboBox.setSelectedIndex(2); break;
		case UNSPECIFIED: comboBox.setSelectedIndex(3); break;
		}
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
	
	private Icon getIcon(Gender gender) {
		switch (gender) {
		case MALE: return MALE_ICON;
		case FEMALE: return FEMALE_ICON;
		case UNSPECIFIED: return UNSPECIFIED_ICON;
		default: return UNKNOWN_ICON;
		}
	}
}
