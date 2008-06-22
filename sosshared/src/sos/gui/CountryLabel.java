package sos.gui;

import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.net.URL;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import sos.data.Country;

public class CountryLabel extends Box
{
	private Country country;
	private JLabel flagLabel, nameLabel;

	public CountryLabel(Country country) {
		super(BoxLayout.X_AXIS);
		this.country = country;
		ImageIcon flagIcon = new ImageIcon();
		Image flagImage = getFlagImage(country);
		if (flagImage != null) { flagIcon.setImage(flagImage); }
		flagLabel = new JLabel(flagIcon);
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
	private Image getFlagImage(Country country) {
		ClassLoader cl = (new Object() {
			public String toString() { return super.toString(); }
		}).getClass().getClassLoader();

		try {
			String fileName = country.toString().toLowerCase() + ".png";

			/* Find image directory. */
//			URL basePath = new URL("file:.");
			URL basePathURL = cl.getResource(".");
			URL imagesDirURL = new URL(basePathURL + "/images");
			if (basePathURL.getProtocol().toLowerCase().startsWith("file")) {
				File basePathFile = new File(basePathURL.getFile());
				File imagesDirFile = new File(basePathFile, "images");
				if (!imagesDirFile.isDirectory()) {
					basePathFile = new File(basePathFile.getParent());
					imagesDirFile = new File(basePathFile, "images");
					imagesDirURL = new URL("file:" + imagesDirFile);
				}
			}

			URL flagImagesDirURL = new URL(imagesDirURL + "/flags");
			URL flagImageFileURL = new URL(flagImagesDirURL + "/" + fileName);
			System.out.println("DEBUG: flagImageFile = " + flagImageFileURL);
			try {
				Image flagImage = ImageIO.read(flagImageFileURL);
				return flagImage;
			} catch (IIOException e) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
}
