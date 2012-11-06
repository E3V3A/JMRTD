package org.jmrtd.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;

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
		return null;
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

			//Use default metadata if not available
			JJ2000Metadata metadata = (JJ2000Metadata)image.getMetadata();
			if (metadata == null)
				metadata = new JJ2000Metadata();

			BufferedImage bufferedImage = convertRenderedImage(image.getRenderedImage());

			//TODO: Subsampling accordingly to ImageWriteParam

			Object output = getOutput();
			if (output == null || !(output instanceof ImageOutputStream)) { throw new IllegalStateException("bad output"); }

			ImageOutputStream imageOutputStream = (ImageOutputStream)output;
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixels = new int[raster.getWidth() * raster.getHeight()];
			raster.getDataElements(0, 0, raster.getWidth(), raster.getHeight(), pixels);
			Bitmap bitmap = new Bitmap(pixels, bufferedImage.getWidth(), bufferedImage.getHeight(), 8, -1, true, 3);
			JJ2000Encoder.encode(new ImageOutputStreamAdapter(imageOutputStream), bitmap, 3);
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
	
	/**
	 * Adapts an {@code ImageOutputStream} for classes requiring an
	 * {@code OutputStream}.
	 *
	 * @author Werner Randelshofer
	 * @version 1.1 2011-01-07 Fixes performance.
	 * <br>1.0 2010-12-26 Created.
	 * 
	 * FIXME: move this to utility package so that wsq_imageio and jj2000_imageio
	 * can use it.
	 * 
	 * FIXME: check license.
	 */
	static class ImageOutputStreamAdapter extends OutputStream {

	    /**
	     * The underlying output stream to be filtered.
	     */
	    protected ImageOutputStream out;

	    /**
	     * Creates an output stream filter built on top of the specified
	     * underlying output stream.
	     *
	     * @param   out   the underlying output stream to be assigned to
	     *                the field <tt>this.out</tt> for later use, or
	     *                <code>null</code> if this instance is to be
	     *                created without an underlying stream.
	     */
	    public ImageOutputStreamAdapter(ImageOutputStream out) {
	        this.out = out;
	    }

	    /**
	     * Writes the specified <code>byte</code> to this output stream.
	     * <p>
	     * The <code>write</code> method of <code>FilterOutputStream</code>
	     * calls the <code>write</code> method of its underlying output stream,
	     * that is, it performs <tt>out.write(b)</tt>.
	     * <p>
	     * Implements the abstract <tt>write</tt> method of <tt>OutputStream</tt>.
	     *
	     * @param      b   the <code>byte</code>.
	     * @exception  IOException  if an I/O error occurs.
	     */
	    @Override
	    public void write(int b) throws IOException {
	        out.write(b);
	    }

	    /**
	     * Writes <code>b.length</code> bytes to this output stream.
	     * <p>
	     * The <code>write</code> method of <code>FilterOutputStream</code>
	     * calls its <code>write</code> method of three arguments with the
	     * arguments <code>b</code>, <code>0</code>, and
	     * <code>b.length</code>.
	     * <p>
	     * Note that this method does not call the one-argument
	     * <code>write</code> method of its underlying stream with the single
	     * argument <code>b</code>.
	     *
	     * @param      b   the data to be written.
	     * @exception  IOException  if an I/O error occurs.
	     * @see        java.io.FilterOutputStream#write(byte[], int, int)
	     */
	    @Override
	    public void write(byte b[]) throws IOException {
	        write(b, 0, b.length);
	    }

	    /**
	     * Writes <code>len</code> bytes from the specified
	     * <code>byte</code> array starting at offset <code>off</code> to
	     * this output stream.
	     * <p>
	     * The <code>write</code> method of <code>FilterOutputStream</code>
	     * calls the <code>write</code> method of one argument on each
	     * <code>byte</code> to output.
	     * <p>
	     * Note that this method does not call the <code>write</code> method
	     * of its underlying input stream with the same arguments. Subclasses
	     * of <code>FilterOutputStream</code> should provide a more efficient
	     * implementation of this method.
	     *
	     * @param      b     the data.
	     * @param      off   the start offset in the data.
	     * @param      len   the number of bytes to write.
	     * @exception  IOException  if an I/O error occurs.
	     * @see        java.io.FilterOutputStream#write(int)
	     */
	    @Override
	    public void write(byte b[], int off, int len) throws IOException {
	        out.write(b,off,len);
	    }

	    /**
	     * Flushes this output stream and forces any buffered output bytes
	     * to be written out to the stream.
	     * <p>
	     * The <code>flush</code> method of <code>FilterOutputStream</code>
	     * calls the <code>flush</code> method of its underlying output stream.
	     *
	     * @exception  IOException  if an I/O error occurs.
	     * @see        java.io.FilterOutputStream#out
	     */
	    @Override
	    public void flush() throws IOException {
	        out.flush();
	    }

	    /**
	     * Closes this output stream and releases any system resources
	     * associated with the stream.
	     * <p>
	     * The <code>close</code> method of <code>FilterOutputStream</code>
	     * calls its <code>flush</code> method, and then calls the
	     * <code>close</code> method of its underlying output stream.
	     *
	     * @exception  IOException  if an I/O error occurs.
	     * @see        java.io.FilterOutputStream#flush()
	     * @see        java.io.FilterOutputStream#out
	     */
	    @Override
	    public void close() throws IOException {
	        try {
	            flush();
	        } finally {
	            out.close();
	        }
	    }
	}
}
