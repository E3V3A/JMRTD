/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id: $
 */

package org.jnbis.imageio;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class WSQImageReaderSpi extends ImageReaderSpi {

	static final String   vendorName = "JMRTD";
	static final String   version = "0.0.2";
	static final String   readerClassName = WSQImageReader.class.getName();
	static final String[] names = { "WSQ", "wsq", "WSQ FBI" };
	static final String[] suffixes = { "wsq" };
	static final String[] MIMETypes = { "image/x-wsq" };
	static final String[] writerSpiNames = { };

	// Metadata formats, more information below
	static final boolean  supportsStandardStreamMetadataFormat = false;
	static final String   nativeStreamMetadataFormatName = null;
	static final String   nativeStreamMetadataFormatClassName = null;
	static final String[] extraStreamMetadataFormatNames = null;
	static final String[] extraStreamMetadataFormatClassNames = null;
	static final boolean  supportsStandardImageMetadataFormat = true;
	static final String nativeImageMetadataFormatName = "org.jnbis.imageio.WSQMetadata_1.0";
	static final String nativeImageMetadataFormatClassName = "org.jnbis.imageio.WSQMetadata";
	static final String[] extraImageMetadataFormatNames = null;
	static final String[] extraImageMetadataFormatClassNames = null;

	public WSQImageReaderSpi() {
		super(
				vendorName, 
				version, 
				names, 
				suffixes, 
				MIMETypes,
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
		return "Wavelet Scalar Quantization (WSQ)";
	}

	public boolean canDecodeInput(Object input) throws IOException {
		if (!(input instanceof ImageInputStream)) {
			return false;
		}
		ImageInputStream inStream = (ImageInputStream)input;
		inStream.mark();
		int header = inStream.readUnsignedShort();
		inStream.reset();
		return header == 0xFFA0;
	}

	public ImageReader createReaderInstance(Object extension) {
		return new WSQImageReader(this);
	}
}
