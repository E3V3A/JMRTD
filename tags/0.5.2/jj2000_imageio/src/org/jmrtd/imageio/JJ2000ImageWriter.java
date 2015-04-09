/* $Id: $ */

package org.jmrtd.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Locale;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.jmrtd.jj2000.Bitmap;
import org.jmrtd.jj2000.JJ2000Encoder;

public class JJ2000ImageWriter extends ImageWriter {

	private static final double DEFAULT_BITRATE = 0.80;
	
	public JJ2000ImageWriter(ImageWriterSpi provider) {
		super(provider);
	}

	/**
	 * Progressive, tiling, etcetera disabled.
	 * 
	 * @see javax.imageio.ImageWriter#getDefaultWriteParam()
	 */
	@Override
	public ImageWriteParam getDefaultWriteParam() {
		JJ2000ImageWriteParam writeParam = new JJ2000ImageWriteParam(Locale.getDefault());
		writeParam.setBitrate(DEFAULT_BITRATE);
		return writeParam;
	}

	@Override
	public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
		return null;
	}

	@Override
	public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
		if (inData instanceof JJ2000Metadata) {
			return inData;
		}
		return null;
	}

	@Override
	public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
		return new JJ2000Metadata();
	}

	@Override
	public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
		return null;
	}

	@Override
	public void write(IIOMetadata streamMetaData, IIOImage image, ImageWriteParam param) throws IIOException {
		try {
			double rate = DEFAULT_BITRATE;
			
			/* Use image metadata if provided, and default metadata if not. */
			JJ2000Metadata metadata = (JJ2000Metadata)image.getMetadata();
			if (metadata == null) {
				metadata = new JJ2000Metadata();
				metadata.setBitRate(DEFAULT_BITRATE);
			}

			/* Use the bit rate from metadata. */
			rate = metadata.getBitRate();

			/* But if write parameters were provided, use that bit rate instead. */
			if (param != null && param instanceof JJ2000ImageWriteParam) {
				JJ2000ImageWriteParam jj2000ImageWriteParam  = (JJ2000ImageWriteParam)param;
				rate = jj2000ImageWriteParam.getBitRate();
			}

			BufferedImage bufferedImage = convertRenderedImage(image.getRenderedImage());

			Object output = getOutput();
			if (output == null || !(output instanceof ImageOutputStream)) { throw new IllegalStateException("bad output"); }

			ImageOutputStream imageOutputStream = (ImageOutputStream)output;
			WritableRaster raster = bufferedImage.getRaster();
			switch(raster.getTransferType()) {
			case DataBuffer.TYPE_BYTE: {
				byte[] pixels = (byte[])raster.getDataElements(0, 0, raster.getWidth(), raster.getHeight(), null);
				int[] rgbPixels = new int[pixels.length];
				for (int i = 0; i < pixels.length; i++) {
					rgbPixels[i] = 0xFF000000 | ((pixels[i] & 0xFF) << 16) | ((pixels[i] & 0xFF) << 8) | (pixels[i] & 0xFF);
				}
				Bitmap bitmap = new Bitmap(rgbPixels, bufferedImage.getWidth(), bufferedImage.getHeight(), 8, -1, true, 3);
				JJ2000Encoder.encode(new ImageOutputStreamAdapter(imageOutputStream), bitmap, rate);
				break;
			}
			case DataBuffer.TYPE_INT:
			default: {
				int[] pixels = (int[])raster.getDataElements(0, 0, raster.getWidth(), raster.getHeight(), null);
				Bitmap bitmap = new Bitmap(pixels, bufferedImage.getWidth(), bufferedImage.getHeight(), 8, -1, true, 3);
				JJ2000Encoder.encode(new ImageOutputStreamAdapter(imageOutputStream), bitmap, rate);
				break;
			}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IIOException(t.getMessage(), t);
		}
	}

	/**
	 * Converts the given image into a BufferedImage. 
	 */
	private BufferedImage convertRenderedImage(RenderedImage renderedImage) {
		if (renderedImage instanceof BufferedImage) {
			BufferedImage bufferedImage = (BufferedImage)renderedImage;
			return bufferedImage;
		}
		BufferedImage result = new BufferedImage(renderedImage.getWidth(), renderedImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		renderedImage.copyData(result.getRaster());
		return result;
	}

}
