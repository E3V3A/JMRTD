package org.jmrtd.imageio;

import java.awt.Rectangle;
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

public class WSQImageWriter extends ImageWriter {

	private ImageOutputStream stream;
	private boolean isLibraryLoaded;

	public WSQImageWriter(ImageWriterSpi provider) {
		super(provider);
		this.isLibraryLoaded = false;
		try {
			WSQUtil.loadLibrary();
			this.isLibraryLoaded = true;
		} catch (Error t) {
			this.isLibraryLoaded = false;
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
		return new WSQImageWriteParam(getLocale());
	}

	public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
		return null;
	}

	public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
		if (inData instanceof WSQMetadata) {
			return inData;
		} else {
			return null;
		}
	}

	public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
		return new WSQMetadata();
	}

	public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
		return null;
	}

	public void write(IIOMetadata streamMetaData, IIOImage image, ImageWriteParam param) throws IOException {
		if (!isLibraryLoaded) { throw new IllegalStateException("Unsatisfied link in WSQ reader/writer"); }
		double bitRate = 1.5;
		int ppi = 75;
		if (param instanceof WSQImageWriteParam) {
			WSQImageWriteParam wsqParam = (WSQImageWriteParam)param;
			bitRate = wsqParam.getBitRate();
			ppi = wsqParam.getPPI();
		}
		RenderedImage renderedImg = image.getRenderedImage();

		Rectangle sourceRegion =
				new Rectangle(0, 0, renderedImg.getWidth(), renderedImg.getHeight());
		int sourceXSubsampling = 1;
		int sourceYSubsampling = 1;
		int[] sourceBands = null;
		if (param != null) {
			if (param.getSourceRegion() != null) {
				sourceRegion = sourceRegion.intersection(param.getSourceRegion());
			}
			sourceXSubsampling = param.getSourceXSubsampling();
			sourceYSubsampling = param.getSourceYSubsampling();
			sourceBands = param.getSourceBands();

			int subsampleXOffset = param.getSubsamplingXOffset();
			int subsampleYOffset = param.getSubsamplingYOffset();
			sourceRegion.x += subsampleXOffset;
			sourceRegion.y += subsampleYOffset;
			sourceRegion.width -= subsampleXOffset;
			sourceRegion.height -= subsampleYOffset;
		}

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
