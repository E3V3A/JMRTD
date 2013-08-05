/* $Id: $ */

package org.jmrtd.jj2000;

class JJ2000Util {

	private JJ2000Util() {
	}

	/**
	 * 
	 * @param unsignedPixel
	 * @param c component index
	 * @param nc number of components, should be 3
	 * @param nomRangeBits, should be 8
	 * @return
	 */
	public static int unsignedARGBToSignedComponent(int unsignedPixel, int c, int nc, int nomRangeBits) {
		if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
		int unsignedCompValue = getUnsignedComponent(unsignedPixel, c, nc, nomRangeBits);
		assert 127 == (1 << (nomRangeBits - 1));
		int signedCompValue = unsignedCompValue - (1 << (nomRangeBits - 1));
		return signedCompValue;
	}

	public static int getUnsignedComponent(int unsignedPixel, int c, int nc, int nomRangeBits) {
		int byteIndex = nc - c - 1;
		int unsignedCompValue = ((unsignedPixel & (0xFF << (byteIndex * 8))) >> (byteIndex * 8) & 0xFF);
		return unsignedCompValue;
	}

	public static int signedGrayScaleIntToUnsignedARGB(int p) {
		if (p < -127) { return 0xFF000000; }
		if (p > 127) { return 0xFFFFFFFF; }
		p = (p + 127) & 0xFF;
		return 0xFF000000 | (p << 16) | (p << 8) | p;
	}
	
	public static int signedComponentsToUnsignedARGB(int r, int g, int b, int nomRangeBits) {
		if (r < -(1 << (nomRangeBits - 1))) { r = 0x00; } else if (r > ((1 << (nomRangeBits - 1)) - 1)) { r = (1 << nomRangeBits) - 1; } else { r += (1 << (nomRangeBits -1)); }
		if (g < -(1 << (nomRangeBits - 1))) { g = 0x00; } else if (g > ((1 << (nomRangeBits - 1)) - 1)) { g = (1 << nomRangeBits) - 1; } else { g += (1 << (nomRangeBits -1)); }
		if (b < -(1 << (nomRangeBits - 1))) { b = 0x00; } else if (b > ((1 << (nomRangeBits - 1)) - 1)) { b = (1 << nomRangeBits) - 1; } else { b += (1 << (nomRangeBits -1)); }
		return 0xFF000000 | ((r & 0xFF) << (2 * nomRangeBits)) | ((g & 0xFF) << nomRangeBits) | (b & 0xFF);
	}	
}
