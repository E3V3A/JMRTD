package org.jmrtd.imageio;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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

import org.jnbis.Bitmap;
import org.jnbis.WsqDecoder;

public class WSQImageReader extends ImageReader {	
	ImageInputStream stream;
	int width, height;
	private BufferedImage image;
	private WsqDecoder decoder;
	
	public WSQImageReader(ImageReaderSpi provider) {
		super(provider);
		decoder = new WsqDecoder();
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
			this.image = decodeWSQ(inputBytes);
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

	private byte[] readBytes(Object input) throws IOException {
		if (input == null) { return null; }
		if (input instanceof ImageInputStream) {
			this.stream = (ImageInputStream)input;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while (true) {
				int b = stream.read();
				if (b < 0) { break; }
				out.write(b);

				/* NOTE: Check EOI marker FFA1 */
				if (b  == 0xFF) {
					int b1 = stream.read();
					if (b1 < 0) { break; }
					out.write(b1);
					if (b1 == 0xA1) { break; }
				}
			}
			out.flush();
			return out.toByteArray();
		} else {
			throw new IllegalArgumentException("bad input");
		}
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
		list.add(ImageTypeSpecifier.createFromRenderedImage(image)); // createGrayscale(8, DataBuffer.TYPE_BYTE, false));
		return list.iterator();
	}

	public IIOMetadata getStreamMetadata() throws IOException {
		return null;
	}

	private BufferedImage decodeWSQ(byte[] in) throws IOException {
		Bitmap bitmap = decoder.decode(in);
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		byte[] data = bitmap.getPixels();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image.getRaster();
		raster.setDataElements(0, 0, width, height, data);
		return image;
	}
}
