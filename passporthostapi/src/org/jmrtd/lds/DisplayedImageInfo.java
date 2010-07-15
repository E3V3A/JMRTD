package org.jmrtd.lds;

import java.awt.image.BufferedImage;

public class DisplayedImageInfo
{
	public static final int
	TYPE_PORTRAIT = 0,
	TYPE_SIGNATURE_OR_MARK = 1,
	TYPE_FINGER = 2,
	TYPE_IRIS = 3;

	protected int type;
	private BufferedImage image;

	public DisplayedImageInfo(int type) {
		this.type = type;
	}

	public DisplayedImageInfo(int type, BufferedImage image) {
		this(type);
		this.image = image;
	}

	public int getType() {
		return type;
	}

	public BufferedImage getImage() {
		return getImage(false);
	}

	public BufferedImage getImage(boolean isProgressive) {
		return image;
	}
}
