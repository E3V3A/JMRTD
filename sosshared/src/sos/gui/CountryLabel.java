package sos.gui;

import java.awt.Font;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import sos.data.Country;
import sos.util.Icons;

public class CountryLabel extends Box
{
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
		Image flagImage = Icons.getFlagImage(country);
		if (flagImage != null) { flagIcon.setImage(flagImage); }
		return flagIcon;
	}
}
