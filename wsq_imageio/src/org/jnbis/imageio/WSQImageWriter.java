package org.jnbis.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.jnbis.Bitmap;
import org.jnbis.WSQEncoder;
import org.w3c.dom.Node;

public class WSQImageWriter extends ImageWriter {

	public static final double DEFAULT_PPI = -1; //Unknown PPI

	public static final double DEFAULT_BITRATE = 1.5; // MO - shouldn't this also be -1 if unknown?

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
		}
		return null;
	}

	public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
		return new WSQMetadata();
	}

	public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
		return null;
	}

	public void write(IIOMetadata streamMetaData, IIOImage image, ImageWriteParam param) throws IIOException {
		try {
			double bitRate = DEFAULT_BITRATE;
			double ppi = DEFAULT_PPI;

			IIOMetadata metadata = image.getMetadata();
			WSQMetadata wsqMetadata;
			if (metadata instanceof WSQMetadata) {
				// Use specified WSQ metadata
				wsqMetadata = (WSQMetadata) metadata;
			} else {
				wsqMetadata = new WSQMetadata();
				if (metadata != null) {
					// Try to convert different type of metadata using standard metadata format
					Node standardMetadataTree = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
					if (standardMetadataTree != null) {
						wsqMetadata.setFromTree(IIOMetadataFormatImpl.standardMetadataFormatName, standardMetadataTree);
					}
				}
			}

			//Extract PPI from metadata
			if (!Double.isNaN(wsqMetadata.getPPI()))
				ppi=wsqMetadata.getPPI();

			//Extract Bitrate from metadata or WriteParam
			if (!Double.isNaN(wsqMetadata.getBitrate()))
				bitRate=wsqMetadata.getBitrate();
			if (param instanceof WSQImageWriteParam) {
				WSQImageWriteParam wsqParam = (WSQImageWriteParam)param;
				if (!Double.isNaN(wsqParam.getBitRate()))
					bitRate = wsqParam.getBitRate();
			}

			BufferedImage bufferedImage = convertRenderedImage(image.getRenderedImage());

			//TODO: Subsampling accordingly to ImageWriteParam

			Object output = getOutput();
			if (output == null || !(output instanceof ImageOutputStream)) {
				throw new IllegalStateException("bad output");
			}
			try (ImageOutputStream imageOutput = (ImageOutputStream) output) {
				Bitmap bitmap = new Bitmap(
						(byte[]) bufferedImage.getRaster().getDataElements(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null),
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						(int) Math.round(ppi),
						8, 1);
				WSQEncoder.encode((ImageOutputStream) getOutput(), bitmap, bitRate, wsqMetadata.nistcom, "Encoded with JNBIS");
			}
		} catch (Throwable t) {
			throw new IIOException(t.getMessage(), t);
		}
	}

	/**
	 * Converts the given image into a BufferedImage of type {@link BufferedImage#TYPE_BYTE_GRAY}. 
	 */
	private BufferedImage convertRenderedImage(RenderedImage renderedImage) {
		if (renderedImage instanceof BufferedImage) {
			BufferedImage bufferedImage = (BufferedImage)renderedImage;
			if (bufferedImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
				return bufferedImage;
			}
		}
		BufferedImage result = new BufferedImage(renderedImage.getWidth(), renderedImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		renderedImage.copyData(result.getRaster());
		return result;
	}
}
