package org.jmrtd.jj2000;

import java.io.IOException;

import jj2000.j2k.image.DataBlk;
import jj2000.j2k.image.DataBlkInt;
import jj2000.j2k.image.input.ImgReader;

/**
 * ImgReader that wraps our Bitmap data type.
 * 
 * @author The JMRTD open source project
 * 
 * @version 0.0.2
 */
class BitmapDataSrc extends ImgReader {

	private Bitmap bitmap;
	private int nomRangeBits;

	public BitmapDataSrc(Bitmap bitmap) {
		this.bitmap = bitmap;
		this.w = bitmap.getWidth();
		this.h = bitmap.getHeight();
		this.nc = 3;
		this.nomRangeBits = 8;
	}

	public int getFixedPoint(int c) {
		if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
		return 0;
	}

	public int getNomRangeBits(int c) {
		if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
		return nomRangeBits;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public boolean isOrigSigned(int c) {
		if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
		return false;
	}

	public DataBlk getInternCompData(DataBlk blk, int c) {
		return getCompData(blk, c);
	}

	public DataBlk getCompData(DataBlk blk, int c) {
		if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
		int[] pixels = bitmap.getPixels();
		if (blk == null) { blk = new DataBlkInt(0, 0, w, h); }
		int blkDataLength = (blk.h - blk.uly) * (blk.w - blk.ulx);
		int[] compData = (int[])blk.getData();
		if (compData == null) {
			compData = new int[blkDataLength];
		} else {
			assert compData.length == blkDataLength;
		}
		int i = 0;
		for (int y = 0; y < blk.h; y++) {
			for (int x = 0; x < blk.w; x++) {
				int bitmapIndex = (blk.uly + y) * blk.w + (blk.ulx + x);
				int signedCompValue = JJ2000Util.unsignedARGBToSignedComponent(pixels[bitmapIndex], c, nc, nomRangeBits);
				compData[i] = signedCompValue;
				i++;
			}
		}
		blk.setData(compData);
		return blk;
	}
}

