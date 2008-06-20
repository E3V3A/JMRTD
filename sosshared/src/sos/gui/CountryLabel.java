package sos.gui;

import java.awt.Font;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import sos.country.Country;

public class CountryLabel extends Box
{
	private Country country;
	private JLabel flagLabel, nameLabel;

	public CountryLabel(Country country) {
		super(BoxLayout.X_AXIS);
		this.country = country;
		ImageIcon flag = new ImageIcon(getFlagURL(country));
		flagLabel = new JLabel(flag);
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

	/**
	 * TODO: Get flags from zip file? Inside jar?
	 * 
	 * @param country the country who's flag we're looking for
	 * @return URL of the image of the country's flag
	 */
	private URL getFlagURL(Country country) {
		try {
			return new URL("file:images/flags/" + country.toString().toLowerCase() + ".png");
		} catch (MalformedURLException mfue) {
			/* NOTE: shouldn't happen... */
			throw new IllegalStateException(mfue.toString());
		}
	}
}
