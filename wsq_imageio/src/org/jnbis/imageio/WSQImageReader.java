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

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import org.jnbis.Bitmap;
import org.jnbis.WSQDecoder;

public class WSQImageReader extends ImageReader {

	private WSQMetadata metadata;
	private BufferedImage image;

	public WSQImageReader(ImageReaderSpi provider) {
		super(provider);
	}

	@Override
	public void setInput(Object input) {
		super.setInput(input); // NOTE: should be setInput(input, false, false);
	}

	@Override
	public void setInput(Object input, boolean seekForwardOnly) {
		super.setInput(input, seekForwardOnly);  // NOTE: should be setInput(input, seekForwardOnly, false);
	}

	@Override
	public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetaData) {
		super.setInput(input, seekForwardOnly, ignoreMetaData);
	}

	@Override
	public int getNumImages(boolean allowSearch) throws IIOException {
		processInput(0);
		return 1;
	}

	@Override
	public BufferedImage read(int imageIndex, ImageReadParam param) throws IIOException {
		processInput(imageIndex);

		//TODO:Subsampling accordingly to ImageReadParam

		return image;
	}

	@Override
	public int getWidth(int imageIndex) throws IOException {
		processInput(imageIndex);
		return image.getWidth();
	}

	@Override
	public int getHeight(int imageIndex) throws IOException {
		processInput(imageIndex);
		return image.getHeight();
	}

	@Override
	public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
		processInput(imageIndex);
		return metadata;
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		processInput(imageIndex);
		return Collections.singletonList(ImageTypeSpecifier.createFromRenderedImage(image)).iterator();
	}

	@Override
	public IIOMetadata getStreamMetadata() throws IOException {
		return null;
	}

	private void processInput(int imageIndex) {
		try {
			if (imageIndex != 0) { throw new IndexOutOfBoundsException("imageIndex " + imageIndex); }

			/* Alread processed */
			if (image != null) { return; }

			Object input = getInput();
			if (input == null) {
				this.image = null;
				return;
			}
			if (!(input instanceof ImageInputStream)) { throw new IllegalArgumentException("bad input: " + input.getClass().getCanonicalName()); }
			Bitmap bitmap = WSQDecoder.decode(new ImageInputStreamAdapter((ImageInputStream)input));
			this.metadata = new WSQMetadata(bitmap.getPpi());
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			byte[] pixels = bitmap.getPixels();
			this.image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			WritableRaster raster = image.getRaster();
			raster.setDataElements(0, 0, width, height, pixels);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			this.image = null;
		}
	}
}
