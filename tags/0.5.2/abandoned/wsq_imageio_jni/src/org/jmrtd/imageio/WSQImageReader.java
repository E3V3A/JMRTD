package org.jmrtd.imageio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class WSQImageReader extends ImageReader {
	private WSQMetadata metadata;
	private BufferedImage image;
	private Throwable parseException = null;

	public WSQImageReader(ImageReaderSpi provider) {
		super(provider);
	}

	public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetaData) {
		super.setInput(input, seekForwardOnly, ignoreMetaData);
		
		this.parseException = null;
		this.image = null;
		this.metadata = null;
	}

	public void parseInput(int imageIndex) throws IIOException {
		//Invalid index
		if (imageIndex != 0)
			throw new IndexOutOfBoundsException("ImageIndex="+imageIndex);

		//Already parsed!
		if (image != null)
			return;

		//Haven't tried yet
		if (parseException == null) {		
			try {
				/* In-progress: Use JNBIS Library
				Bitmap bitmap = new WsqDecoder().decode(readBytes());
				image = new BufferedImage(bitmap.getWidth(), bitmap.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
				WritableRaster raster = image.getRaster();
				raster.setDataElements(0, 0, bitmap.getWidth(), bitmap.getHeight(), bitmap.getPixels());
				metadata = new WSQMetadata(bitmap.getPpi()); */
				
				WSQUtil.loadLibrary();
				metadata = new WSQMetadata(); 
				image = decodeWSQ(readBytes(), metadata);
			} catch (Throwable t) {
				metadata = null;
				image=null;
				parseException = t;
			}
		}

		//Failed
		if (parseException != null)
			throw new IIOException("Failed to decode WSQ Image", parseException);
	}

	private byte[] readBytes() throws IIOException {
		try {
			ImageInputStream stream = (ImageInputStream)getInput();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
	
			int lastB=-1;
			while (true) {
				int B = stream.read();
				if (B < 0)
					throw new EOFException();
	
				out.write(B);
				if (out.size()==2) {
					if (lastB!=0xFF || B!=0xA0) {
						throw new IIOException("Missing WSQ Header 0xFF 0xA0");
					}						
				}
	
				//Check EOI Mark
				if (lastB==0xFF && B==0xA1)
					break;
	
				lastB=B;
			}
			out.flush();
			return out.toByteArray();
		} catch (IIOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IIOException(t.getMessage(), t);
		}
	}

	public int getNumImages(boolean allowSearch) throws IIOException {
		parseInput(0);
		return 1;
	}

	@Override
	public BufferedImage read(int imageIndex, ImageReadParam param) throws IIOException {
		parseInput(imageIndex);
		
		//TODO:Subsampling accordingly to ImageReadParam
		
		return image;
	}

	public int getWidth(int imageIndex) throws IOException {
		parseInput(imageIndex);
		return image.getWidth();
	}

	public int getHeight(int imageIndex) throws IOException {
		parseInput(imageIndex);
		return image.getHeight();
	}

	public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
		parseInput(imageIndex);
		return metadata;
	}

	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		parseInput(imageIndex);
		return Collections.singletonList( ImageTypeSpecifier.createFromRenderedImage(image) ).iterator();
	}

	public IIOMetadata getStreamMetadata() throws IOException {
		return null;
	}

	private native BufferedImage decodeWSQ(byte[] in, WSQMetadata metadata) throws IOException;
}
