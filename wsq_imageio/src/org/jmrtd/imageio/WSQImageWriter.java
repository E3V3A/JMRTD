package org.jmrtd.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

public class WSQImageWriter extends ImageWriter
{
	ImageOutputStream stream;

	public WSQImageWriter(ImageWriterSpi provider) {
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

	public void setOutput(Object output) {
		super.setOutput(output);
		if (output == null) { stream =null; return; }
		if (!(output instanceof ImageOutputStream)) { throw new IllegalArgumentException("bad input"); }
		this.stream = (ImageOutputStream)output;
	}

	/**
	 * Progressive, tiling, etcetera disabled.
	 * 
	 * @see javax.imageio.ImageWriter#getDefaultWriteParam()
	 */
	public ImageWriteParam getDefaultWriteParam() {
		return new ImageWriteParam(getLocale());
	}

	public IIOMetadata convertImageMetadata(IIOMetadata inData,
			ImageTypeSpecifier imageType, ImageWriteParam param) {
		return null;
	}

	public IIOMetadata convertStreamMetadata(IIOMetadata inData,
			ImageWriteParam param) {
		// We only understand our own metadata
		if (inData instanceof WSQMetadata) {
			return inData;
		} else {
			return null;
		}

	}

	public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType,
			ImageWriteParam param) {
		return new WSQMetadata();
	}

	public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
		return null;
	}

	public void write(IIOMetadata streamMetaData, IIOImage image, ImageWriteParam param)
	throws IOException {
		/* FIXME: get these from params? */
		double bitRate = 1.5;
		int ppi = 25;
		RenderedImage renderedImg = image.getRenderedImage();
		BufferedImage bufferedImg = convertRenderedImage(renderedImg);
		byte[] encodedBytes = encodeWSQ(bufferedImg, bitRate, ppi);
		stream.write(encodedBytes);
		stream.flush();
	}

	private BufferedImage convertRenderedImage(RenderedImage img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage)img;	
		}	
		ColorModel cm = img.getColorModel();
		int width = img.getWidth();
		int height = img.getHeight();
		WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		Hashtable<String,Object> properties = new Hashtable<String, Object>();
		String[] keys = img.getPropertyNames();
		if (keys != null) {
			for (int i = 0; i < keys.length; i++) {
				properties.put(keys[i], img.getProperty(keys[i]));
			}
		}
		BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
		img.copyData(raster);
		return result;
	}

	private native byte[] encodeWSQ(BufferedImage image, double bitRate, int ppi) throws IOException;
}
