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
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import org.jnbis.BitmapWithMetadata;
import org.jnbis.WSQDecoder;

public class WSQImageReader extends ImageReader {

	private WSQMetadata metadata;
	private BufferedImage image;

	public WSQImageReader(ImageReaderSpi provider) {
		super(provider);
	}

	public void setInput(Object input) {
		super.setInput(input); // NOTE: should be setInput(input, false, false);
	}

	public void setInput(Object input, boolean seekForwardOnly) {
		super.setInput(input, seekForwardOnly);  // NOTE: should be setInput(input, seekForwardOnly, false);
	}

	public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetaData) {
		super.setInput(input, seekForwardOnly, ignoreMetaData);
	}

	public int getNumImages(boolean allowSearch) throws IIOException {
		processInput(0);
		return 1;
	}

	public BufferedImage read(int imageIndex, ImageReadParam param) throws IIOException {
		processInput(imageIndex);

		//TODO:Subsampling accordingly to ImageReadParam

		return image;
	}

	public int getWidth(int imageIndex) throws IOException {
		processInput(imageIndex);
		return image.getWidth();
	}

	public int getHeight(int imageIndex) throws IOException {
		processInput(imageIndex);
		return image.getHeight();
	}

	public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
		processInput(imageIndex);
		return metadata;
	}

	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		processInput(imageIndex);
		return Collections.singletonList(ImageTypeSpecifier.createFromRenderedImage(image)).iterator();
	}

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
			
			BitmapWithMetadata bitmap = WSQDecoder.decode((ImageInputStream)getInput());
			metadata = new WSQMetadata(); 
			
			for (Map.Entry<String, String> entry : bitmap.getMetadata().entrySet()) {
				//System.out.println(entry.getKey() + ": " + entry.getValue());
				metadata.setProperty(entry.getKey(), entry.getValue());
			}
			for (String s:bitmap.getComments()) {
				//System.out.println("//"+s);
				metadata.addComment(s);
			}
			
			image = new BufferedImage(bitmap.getWidth(), bitmap.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			image.getRaster().setDataElements(0, 0, bitmap.getWidth(), bitmap.getHeight(), bitmap.getPixels());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			this.image = null;
		}
	}
}
