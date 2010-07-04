package org.jmrtd.imageio;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

public class WSQImageWriterSpi extends ImageWriterSpi
{
	static final String vendorName = "JMRTD";
	static final String version = "0.0.1";
	static final String writerClassName =
		"org.jmrtd.imageio.WSQImageWriter";
	static final String[] names = { "wsq" };
	static final String[] suffixes = { "wsq" };
	static final String[] MIMETypes = { "image/x-wsq" };
	static final String[] readerSpiNames = {
	"org.jmrtd.imageio.WSQImageReaderSpi" };

	static final boolean supportsStandardStreamMetadataFormat = false;
	static final String nativeStreamMetadataFormatName = null;
	static final String nativeStreamMetadataFormatClassName = null;
	static final String[] extraStreamMetadataFormatNames = null;
	static final String[] extraStreamMetadataFormatClassNames = null;
	static final boolean supportsStandardImageMetadataFormat = false;
	static final String nativeImageMetadataFormatName =
		"org.jmrtd.imageio.WSQMetadata_1.0";
	static final String nativeImageMetadataFormatClassName =
		"org.jmrtd.imageio.WSQMetadata";
	static final String[] extraImageMetadataFormatNames = null;
	static final String[] extraImageMetadataFormatClassNames = null;

	public WSQImageWriterSpi() {
		super(vendorName, version,
				names, suffixes, MIMETypes,
				writerClassName,
				STANDARD_OUTPUT_TYPE, // Write to ImageOutputStreams
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
		/* FIXME: check gray scale? */
		System.out.println("DEBUG: WSQImageWriterSpi.canEncodeImage()");
		return true;
	}

	public ImageWriter createWriterInstance(Object extension) throws IOException {
		return new WSQImageWriter(this);
	}

	public String getDescription(Locale locale) {
		return "Description goes here";
	}
}
