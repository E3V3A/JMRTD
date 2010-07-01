package org.jmrtd.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
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

public class WSQImageReader extends ImageReader
{	
	ImageInputStream stream;
	int width, height;
	private BufferedImage image;

	public WSQImageReader(ImageReaderSpi provider) {
		super(provider);
		String libraryName = "j2wsq";
		try {
			System.loadLibrary(libraryName);
		} catch (UnsatisfiedLinkError ule1) {
			String separator = System.getProperty("file.separator");
			if (separator == null) { separator = "/"; }
			String pwd = System.getProperty("user.dir");
			System.load(pwd + separator + System.mapLibraryName(libraryName));
		}
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
			byte[] inputBytes = getBytes(input);
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

	private byte[] getBytes(Object input) throws IOException {
		if (input == null) { return null; }
		if (input instanceof byte[]) { return (byte[])input; }
		if (input instanceof ImageInputStream) {
			this.stream = (ImageInputStream)input;
			this.stream.mark();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			while (true) {
				int bytesRead = stream.read(buf, 0, 1024);
				if (bytesRead < 0) { break; }
				out.write(buf, 0, bytesRead);
			}
			out.flush();
			this.stream.reset();
			return out.toByteArray();
		} else {
			throw new IllegalArgumentException("bad input");
		}
	}

	private native BufferedImage decodeWSQ(byte[] in);

	public BufferedImage read(int imageIndex, ImageReadParam param) throws IIOException {
		if (imageIndex != 0) { throw new IllegalArgumentException("bad input"); }
		return image;
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
		list.add(ImageTypeSpecifier.createGrayscale(8, DataBuffer.TYPE_BYTE, false));
		return list.iterator();
	}

	public IIOMetadata getStreamMetadata() throws IOException {
		return null;
	}	
}
