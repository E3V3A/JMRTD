package org.jmrtd.imageio;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

public class WSQImageWriteParam extends ImageWriteParam
{
	private int ppi;
	private double bitRate;
	
	public WSQImageWriteParam(Locale locale) {
		super(locale);
		this.bitRate = 1.5;
		this.ppi = 75;
	}
	
	public int getPPI() {
		return ppi;
	}
	
	public double getBitRate() {
		return bitRate;
	}
	
	public void setPPI(int ppi) {
		this.ppi = ppi;
	}
	
	public void setBitrate(float bitRate) {
		this.bitRate = bitRate;
	}
}
