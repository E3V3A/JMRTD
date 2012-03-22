package org.jmrtd.imageio;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
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
			this.image = JJ2000Util.read(new ByteArrayInputStream(inputBytes));
			this.width = image.getWidth();
			this.height = image.getHeight();
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
		list.add(ImageTypeSpecifier.createFromRenderedImage(image)); // (8, DataBuffer.TYPE_INT, false));
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
