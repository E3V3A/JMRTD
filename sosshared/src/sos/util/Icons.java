package sos.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;

import sos.data.Country;

/**
 * Utility class with methods for reading icons and other images from file.
 * Assumes file structure (relative to class path) something like this:
 * <ul>
 *    <li>images/</li>
 *    <ul>
 *       <li>flags/</li>
 *       <ul>
 *          <li><i>alpha2countrycode</i>.png</li>
 *       </ul>
 *       <li>icons/</li>
 *       <ul>
 *          <li><i>action</i>.png</li>
 *       </ul>
 *       <li><i>image</i>.png</li>
 *    </ul>
 * </ul>
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class Icons
{	
	private static final Image DEFAULT_16X16_IMAGE =  new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
	DEFAULT_16X11_IMAGE =  new BufferedImage(16, 11, BufferedImage.TYPE_INT_ARGB);

	private static URL getImagesDir() {
		try {
			URL basePathURL = Files.getBaseDir();
			URL imagesDirURL = new URL(basePathURL + "/images");
			return imagesDirURL;
		} catch (Exception e) {
			return null;
		}
	}

	public static Image getImage(String imageName) {
		try {
			URL imagesDir = getImagesDir();
			String fileName = imageName.toLowerCase() + ".png";
			Image image = ImageIO.read(new URL(imagesDir + "/" + fileName));
			return image;
		} catch (Exception e) {
			return null;
		}

	}

	public static Image getFlagImage(Country country) {
		try {
			URL flagsDir = new URL(getImagesDir() + "/flags");
			String fileName = country.toString().toLowerCase() + ".png";
			Image flagImage = ImageIO.read(new URL(flagsDir + "/" + fileName));
			return flagImage;
		} catch (Exception e) {
			return DEFAULT_16X11_IMAGE;
		}
	}

	/**
	 * Gets small icon from file.
	 * 
	 * @param iconName name without the .png or .gif
	 * @return
	 */
	public static Image getSmallIcon(String iconName) {
		return getFamFamFamSilkIcon(iconName);
	}

	/**
	 * Gets large icon from file.
	 * 
	 * @param iconName name without the .png or .gif
	 * @return
	 */
	public static Image getLargeIcon(String iconName) {
		return getFamFamFamSilkIcon(iconName);
	}

	/**
	 * Gets icon from file.
	 * 
	 * @param iconName name without the .png or .gif
	 * @return
	 */
	public static Image getFamFamFamSilkIcon(String iconName) {
		try {
			URL iconsDir = new URL(getImagesDir() + "/famfamfam_silk");
			String fileName = iconName.toLowerCase() + ".png";
			Image iconImage = ImageIO.read(new URL(iconsDir + "/" + fileName));
			return iconImage;
		} catch (Exception e) {
			return DEFAULT_16X16_IMAGE;
		}
	}
}
