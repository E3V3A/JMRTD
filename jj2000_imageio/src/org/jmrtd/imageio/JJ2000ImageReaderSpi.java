/* $Id: $ */

package org.jmrtd.imageio;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class JJ2000ImageReaderSpi extends ImageReaderSpi
{
	static final String vendorName = "JMRTD";
	static final String version = "0.0.1";
	static final String readerClassName = "org.jmrtd.imageio.JJ2000ImageReader";
	static final String[] names = { "JPEG 2000" };
	static final String[] suffixes = { "jp2", "j2k", "jpeg2000" };
	static final String[] MIMETypes = { "image/jp2", "image/jpeg2000" };
	static final String[] writerSpiNames = { };

	// Metadata formats, more information below
	static final boolean supportsStandardStreamMetadataFormat = false;
	static final String nativeStreamMetadataFormatName = null;
	static final String nativeStreamMetadataFormatClassName = null;
	static final String[] extraStreamMetadataFormatNames = null;
	static final String[] extraStreamMetadataFormatClassNames = null;
	static final boolean supportsStandardImageMetadataFormat = false;
	static final String nativeImageMetadataFormatName = "org.jmrtd.imageio.JJ2000Metadata_1.0";
	static final String nativeImageMetadataFormatClassName = "org.jmrtd.imageio.JJ2000Metadata";
	static final String[] extraImageMetadataFormatNames = null;
	static final String[] extraImageMetadataFormatClassNames = null;

	public JJ2000ImageReaderSpi() {
		super(vendorName, version, names, suffixes, MIMETypes,
				readerClassName,
				STANDARD_INPUT_TYPE, // Accept ImageInputStreams
				writerSpiNames,
				supportsStandardStreamMetadataFormat,
				nativeStreamMetadataFormatName,
				nativeStreamMetadataFormatClassName,
				extraStreamMetadataFormatNames,
				extraStreamMetadataFormatClassNames,
				supportsStandardImageMetadataFormat,
				nativeImageMetadataFormatName,
				nativeStreamMetadataFormatClassName, extraImageMetadataFormatNames,
				extraImageMetadataFormatClassNames);
	}

	public String getDescription(Locale locale) {
		return "Description goes here";
	}

	public boolean canDecodeInput(Object input) throws IOException {
		if (!(input instanceof ImageInputStream)) {
			return false;
		}
		ImageInputStream inStream = (ImageInputStream)input;
		inStream.mark();
//		int header = inStream.readUnsignedShort();
//		inStream.reset();
//		return (header & 0xFFFF) == 0xFFA0; // magic byte for WSQ, not for JPEG 2000
		return true;
	}

	public ImageReader createReaderInstance(Object extension) {
		return new JJ2000ImageReader(this);
	}
}
