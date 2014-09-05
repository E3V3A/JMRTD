package org.jmrtd.imageio;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

public class WSQImageWriteParam extends ImageWriteParam {

	private double bitRate = Double.NaN;

	public WSQImageWriteParam(Locale locale) {
		super(locale);
	}

	public double getBitRate() {
		return bitRate;
	}

	public void setBitrate(double bitRate) {
		this.bitRate = bitRate;
	}
}
