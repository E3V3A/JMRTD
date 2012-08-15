package org.jmrtd.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

public class WSQImageWriter extends ImageWriter {
	public static final double DEFAULT_PPI=-1; //Unknown PPI
	public static final double DEFAULT_BITRATE=1.5;
	
	public WSQImageWriter(ImageWriterSpi provider) {
		super(provider);
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

	public void write(IIOMetadata streamMetaData, IIOImage image, ImageWriteParam param) throws IIOException {
		try {
			WSQUtil.loadLibrary();

			double bitRate = DEFAULT_BITRATE;
			double ppi = DEFAULT_PPI;

			//Use default metadata if not available
			WSQMetadata metadata = (WSQMetadata)image.getMetadata();
			if (metadata == null)
				metadata = new WSQMetadata();

			//Extract PPI from metadata
			if (!Double.isNaN(metadata.getPPI())) 
				ppi=metadata.getPPI();

			//Extract Bitrate from metadata or WriteParam
			if (!Double.isNaN(metadata.getBitrate())) 
				bitRate=metadata.getBitrate();
			if (param instanceof WSQImageWriteParam) {
				WSQImageWriteParam wsqParam = (WSQImageWriteParam)param;
				if (!Double.isNaN(wsqParam.getBitRate()))
					bitRate = wsqParam.getBitRate();
			}

			BufferedImage bufferedImg = convertRenderedImage( image.getRenderedImage() );

			//TODO:Subsampling accordingly to ImageWriteParam

			byte[] encodedBytes = encodeWSQ(bufferedImg, bitRate, (int)Math.round(ppi), metadata.getNistcom());
			((ImageOutputStream)getOutput()).write(encodedBytes);
			((ImageOutputStream)getOutput()).flush();
		} catch (Throwable t) {
			throw new IIOException(t.getMessage(), t);
		}
	}

	/**
	 * Converts the given image into a BufferedImage of type {@link BufferedImage#TYPE_BYTE_GRAY}. 
	 */
	private BufferedImage convertRenderedImage(RenderedImage img) {
		if (img instanceof BufferedImage) {
			BufferedImage buf = (BufferedImage)img;
			if (buf.getType()==BufferedImage.TYPE_BYTE_GRAY)
				return buf;
		}
		BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		img.copyData(result.getRaster());
		return result;
	}

	private native byte[] encodeWSQ(BufferedImage img, double bitRate, int ppi, String nistcom) throws IOException;
}
