package org.jmrtd.imageio;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

public class WSQImageWriterSpi extends ImageWriterSpi {

	static final String vendorName = "JMRTD";
	static final String version = "0.0.2";
	static final String writerClassName = "org.jmrtd.imageio.WSQImageWriter";
	static final String[] names = { "WSQ", "wsq", "WSQ FBI" };
	static final String[] suffixes = { "wsq" };
	static final String[] MIMETypes = { "image/x-wsq" };
	static final String[] readerSpiNames = { "org.jmrtd.imageio.WSQImageReaderSpi" };

	static final boolean  supportsStandardStreamMetadataFormat = false;
	static final String   nativeStreamMetadataFormatName = null;
	static final String   nativeStreamMetadataFormatClassName = null;
	static final String[] extraStreamMetadataFormatNames = null;
	static final String[] extraStreamMetadataFormatClassNames = null;
	static final boolean  supportsStandardImageMetadataFormat = true;
	static final String   nativeImageMetadataFormatName = "org.jmrtd.imageio.WSQMetadata_1.0";
	static final String   nativeImageMetadataFormatClassName = "org.jmrtd.imageio.WSQMetadataFormat";
	static final String[] extraImageMetadataFormatNames = null;
	static final String[] extraImageMetadataFormatClassNames = null;

	public WSQImageWriterSpi() {
		super(
				vendorName, 
				version,
				names, 
				suffixes, 
				MIMETypes,
				writerClassName,
				new Class[] { ImageOutputStream.class }, // Write to ImageOutputStreams
				readerSpiNames,
				supportsStandardStreamMetadataFormat,
				nativeStreamMetadataFormatName,
				nativeStreamMetadataFormatClassName,
				extraStreamMetadataFormatNames,
				extraStreamMetadataFormatClassNames,
				supportsStandardImageMetadataFormat,
				nativeImageMetadataFormatName,
				nativeImageMetadataFormatClassName,
				extraImageMetadataFormatNames,
				extraImageMetadataFormatClassNames);
	}

	public boolean canEncodeImage(ImageTypeSpecifier imageType) {
		//Can encode any image, but it will be converted to grayscale.
		return true; 
		//return imageType.getBufferedImageType() == BufferedImage.TYPE_BYTE_GRAY;
	}

	public ImageWriter createWriterInstance(Object extension) throws IOException {
		return new WSQImageWriter(this);
	}

	public String getDescription(Locale locale) {
		return "Wavelet Scalar Quantization (WSQ)";
	}
}
