package org.jmrtd.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream which will wrap another input stream (and yield the same bytes) and which can
 * spawn new fresh input stream copies (using {@link #getInputStream(int)})
 * (that also yield the same bytes).
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: $
 */
public class SplittableInputStream extends InputStream {

	private InputStreamBuffer inputStreamBuffer;
	private InputStreamBuffer.SubInputStream carrier;

	/**
	 * 
	 * @param inputStream the original input stream
	 * @param length
	 */
	public SplittableInputStream(InputStream inputStream, int length) {
		this.inputStreamBuffer = new InputStreamBuffer(inputStream, length);
		this.carrier = inputStreamBuffer.getInputStream();
	}

	/**
	 * Gets a copy of the inputstream positioned at <code>position</code>.
	 * 
	 * @param position a position between <code>0</code> and {@link #getPosition()}
	 * @return a fresh input stream
	 * 
	 * @throws IOException on error
	 */
	public InputStream getInputStream(int position) {
		try {
			InputStream result = inputStreamBuffer.getInputStream();
			if (position > 0) { result.skip(position); }
			return result;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalStateException(ioe.getMessage());
		}
	}

	public int getPosition() {
		return carrier.getPosition();
	}

	public int read() throws IOException {
		return carrier.read();
	}

	public long skip(long n) throws IOException {
		return carrier.skip(n);
	}

	public int available() throws IOException {
		return carrier.available();
	}

	public void close() throws IOException {
		carrier.close();
	}

	public synchronized void mark(int readlimit) {
		carrier.mark(readlimit);
	}

	public synchronized void reset() throws IOException {
		carrier.reset();
	}

	public boolean markSupported() {
		return carrier.markSupported();
	}

	public int getLength() {
		return inputStreamBuffer.getLength();
	}
}
