package org.jmrtd.imageio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class WSQImageReader extends ImageReader
{
	ImageInputStream stream = null;

	public WSQImageReader(ImageReaderSpi provider) {
		super(provider);
		System.loadLibrary("j2wsq");
	}

	public void setInput(Object input, boolean isStreamable) {
		super.setInput(input, isStreamable);
		if (input == null) {
			this.stream = null;
			return;
		}
		if (input instanceof ImageInputStream) {
			this.stream = (ImageInputStream)input;
		} else {
			throw new IllegalArgumentException("bad input");
		}
	}

	public int getNumImages(boolean allowSearch) throws IIOException {
		return 1;
	}

	private native BufferedImage decodeWSQ(byte[] in);

	public BufferedImage read(int imageIndex, ImageReadParam param) throws IIOException {
		if (imageIndex != 0) {
			throw new IllegalArgumentException("bad input");
		}
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			while (true) {
				int bytesRead = stream.read(buf, 0, 1024);
				if (bytesRead < 0) { break; }
				out.write(buf, 0, bytesRead);
			}
			out.flush();
			byte[] inputBytes = out.toByteArray();
			out.close();
			return decodeWSQ(inputBytes);
		} catch (IOException e) {
			throw new IIOException(e.getMessage());
		}
	}

	public int getWidth(int imageIndex) throws IOException {
		return 0;
	}
	
	public int getHeight(int imageIndex) throws IOException {
		return 0;
	}

	public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
		return null;
	}

	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		return null;
	}

	public IIOMetadata getStreamMetadata() throws IOException {
		return null;
	}	
}
