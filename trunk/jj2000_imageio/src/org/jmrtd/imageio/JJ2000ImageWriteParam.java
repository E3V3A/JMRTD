/* $Id$ */

package org.jmrtd.imageio;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

public class JJ2000ImageWriteParam extends ImageWriteParam {

	private double bitRate = Double.NaN;

	public JJ2000ImageWriteParam(Locale locale) {
		super(locale);
	}

	public double getBitRate() {
		return bitRate;
	}

	public void setBitrate(double bitRate) {
		this.bitRate = bitRate;
	}
}
