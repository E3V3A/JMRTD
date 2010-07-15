package org.jmrtd.lds;

import java.awt.image.BufferedImage;

public class DisplayedImageInfo
{
	public static final int
		TYPE_PORTRAIT = 0,
		TYPE_SIGNATURE_OR_MARK = 1;
	
	private int type;
	private BufferedImage image;
	
	public DisplayedImageInfo(int type, BufferedImage image) {
		this.type = type;
		this.image = image;
	}
	
	public int getType() {
		return type;
	}
	
	public BufferedImage getImage() {
		return image;
	}
}
