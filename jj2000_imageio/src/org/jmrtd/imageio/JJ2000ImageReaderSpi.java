/* $Id: $ */

package org.jmrtd.imageio;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class JJ2000ImageReaderSpi extends ImageReaderSpi
{
	private static final byte[] MAGIC_BYTES = {
		0x00, 0x00, 0x00, 0x0C, 0x6A, 0x50, 0x20, 0x20,
		0x0D, 0x0A, (byte)0x87, 0x0A, 0x00, 0x00, 0x00, 0x14,
		0x66, 0x74, 0x79, 0x70, 0x6A, 0x70, 0x32
	};

	static final String vendorName = "JMRTD";
	static final String version = "0.0.1";
	static final String readerClassName = "org.jmrtd.imageio.JJ2000ImageReader";
	static final String[] names = { "jpeg 2000", "JPEG 2000", "jpeg2000", "JPEG2000" };
	static final String[] suffixes = { "jp2" };
	static final String[] MIMETypes = { "image/jp2", "image/jpeg2000" };
	static final String[] writerSpiNames = { };

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
				new Class[] { ImageInputStream.class },
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
		try {
			int bytesRead = 0;
			byte[] headerBytes = new byte[MAGIC_BYTES.length];
			while (bytesRead < MAGIC_BYTES.length) {
				int actualBytesRead = inStream.read(headerBytes, bytesRead, MAGIC_BYTES.length - bytesRead);
				if (actualBytesRead < 0) { return false; }
			}
			return Arrays.equals(MAGIC_BYTES, headerBytes);
		} finally {
			inStream.reset();
		}
	}

	public ImageReader createReaderInstance(Object extension) {
		return new JJ2000ImageReader(this);
	}
}
