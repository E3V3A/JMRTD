/* $Id: $ */

package org.jmrtd.imageio;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import org.jmrtd.jj2000.Bitmap;
import org.jmrtd.jj2000.JJ2000Decoder;

public class JJ2000ImageReader extends ImageReader {

	ImageInputStream stream;
	int width, height;
	private BufferedImage image;

	public JJ2000ImageReader(ImageReaderSpi provider) {
		super(provider);
	}

	public void setInput(Object input) {
		super.setInput(input); // NOTE: should be setInput(input, false, false);
	}

	public void setInput(Object input, boolean seekForwardOnly) {
		super.setInput(input, seekForwardOnly);  // NOTE: should be setInput(input, seekForwardOnly, false);
	}

	public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetaData) {
		super.setInput(input, seekForwardOnly, ignoreMetaData);
		if (input == null) {
			this.image = null;
			return;
		}
		try {
			/* We're reading the complete image already, just to get the width and height. */
			byte[] inputBytes = readBytes(input);
			Bitmap bitmap = JJ2000Decoder.decode(new ByteArrayInputStream(inputBytes));
			this.width = bitmap.getWidth();
			this.height = bitmap.getHeight();
			int[] pixels = bitmap.getPixels();
            this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            WritableRaster raster = image.getRaster();
            raster.setDataElements(0, 0, width, height, pixels);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			this.image = null;
		}
	}

	public int getNumImages(boolean allowSearch) throws IIOException {
		return 1;
	}

	public BufferedImage read(int imageIndex, ImageReadParam param) throws IIOException {
		if (imageIndex != 0) { throw new IllegalArgumentException("bad input"); }
		try {
			Point destinationOffset = new Point(0, 0);
			if (param != null) { destinationOffset = param.getDestinationOffset(); }
			BufferedImage dst = getDestination(param, getImageTypes(0), width, height);
			dst.getRaster().setRect((int)destinationOffset.getX(), (int)destinationOffset.getY(), image.getRaster());
			return dst;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IIOException(ioe.getMessage());
		}
	}

	public int getWidth(int imageIndex) throws IOException {
		if (imageIndex != 0) { throw new IllegalArgumentException("bad input"); }
		return width;
	}

	public int getHeight(int imageIndex) throws IOException {
		if (imageIndex != 0) { throw new IllegalArgumentException("bad input"); }
		return height;
	}

	public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
		if (imageIndex != 0) { throw new IllegalArgumentException("bad input"); }
		return null;
	}

	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		if (imageIndex != 0) { throw new IllegalArgumentException("bad input"); }
		List<ImageTypeSpecifier> list = new ArrayList<ImageTypeSpecifier>();
		list.add(ImageTypeSpecifier.createFromRenderedImage(image));
		return list.iterator();
	}

	public IIOMetadata getStreamMetadata() throws IOException {
		return null;
	}

	/* ONLY PRIVATE METHODS BELOW */

	private byte[] readBytes(Object input) throws IOException {
		if (input == null) { return null; }
		if (input instanceof ImageInputStream) {
			this.stream = (ImageInputStream)input;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while (true) {
				int b = stream.read();
				if (b < 0) { break; }
				out.write(b);
			}
			out.flush();
			byte[] bytes = out.toByteArray();
			return bytes;
		} else {
			throw new IllegalArgumentException("bad input");
		}
	}
}
