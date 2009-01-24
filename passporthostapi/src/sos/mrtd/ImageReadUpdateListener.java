package sos.mrtd;

import java.awt.image.BufferedImage;

/**
 * Interface for image update observers.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Id: $
 */
public interface ImageReadUpdateListener
{
	/**
	 * Called when a new approximation of the image is available.
	 *
	 * @param image an approximation of the image
	 * @param percentage percentage done
	 */
	void passComplete(BufferedImage image, double percentage);
}
