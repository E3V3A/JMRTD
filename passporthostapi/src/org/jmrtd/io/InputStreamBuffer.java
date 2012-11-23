/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
 *
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
 * $Id:  $
 */

package org.jmrtd.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Buffers an inputstream (whose length is known in advance) and can supply clients with fresh
 * "copies" of that inputstream served from the buffer.
 * 
 * NOTE: the original inputstream should no longer be read from, only read bytes from the
 * sub-inputstreams.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class InputStreamBuffer {

	private PositionInputStream carrier;
	private FragmentBuffer buffer;

	public InputStreamBuffer(byte[] bytes) {
		if (bytes == null) {
			throw new IllegalArgumentException("Null buffer");
		}
		carrier = new PositionInputStream(new ByteArrayInputStream(bytes));
		carrier.mark(bytes.length);
		this.buffer = new FragmentBuffer(bytes.length);
		this.buffer.addFragment(0, bytes);
	}

	public InputStreamBuffer(InputStream inputStream, int length) {
		this.carrier = new PositionInputStream(inputStream);
		this.carrier.mark(length);
		this.buffer = new FragmentBuffer(length);
	}

	/**
	 * Gets a copy of the input stream positioned at 0.
	 *
	 * @return
	 */
	public InputStream getInputStream() {
		synchronized(carrier) {
			return new SubInputStream(carrier);
		}
	}

	public synchronized int getPosition() {
		return buffer.getPosition();
	}

	public synchronized int getBytesBuffered() {
		return buffer.getBytesBuffered();
	}

	public int getLength() {
		return buffer.getLength();
	}

	public String toString() {
		return "InputStreamBuffer [" + buffer + "]";
	}

	private class SubInputStream extends InputStream {

		/** The position within this inputstream. */
		private int position;
		private int markedPosition;

		private Object syncObject;

		public SubInputStream(Object syncObject) {
			position = 0;
			this.syncObject = syncObject;
		}

		public int read() throws IOException {
			synchronized(syncObject) {
				if (position >= buffer.getLength()) {
					return -1;
				} else if (buffer.isCoveredByFragment(position)) {
					/* Serve the byte from the buffer */
					return buffer.getBuffer()[position++] & 0xFF;
				} else {
					/* Get it from the carrier */
					if (carrier.markSupported()) { setCarrierPosition(); }
					int result = carrier.read();
					if (result < 0) { return -1; }
					buffer.addFragment(position++, (byte)result);
					return result;
				}
			}
		}

		public int read(byte[] dest) throws IOException {
			synchronized(syncObject) {
				return read(dest, 0, dest.length);
			}
		}

//		public int read(byte[] dest, int offset, int length) throws IOException {
//			if (dest == null) {
//				throw new NullPointerException();
//			} else if (offset < 0 || length < 0 || length > dest.length - offset) {
//				throw new IndexOutOfBoundsException();
//			} else if (length == 0) {
//				return 0;
//			}
//			if (position >= buffer.getLength()) { return -1; }
//			length = Math.min(length, buffer.getLength() - position);
//			int leftInBuffer = buffer.getBufferedLength(position);
//			if (length <= leftInBuffer) {
//				assert(length <= leftInBuffer);
//				System.arraycopy(buffer.getBuffer(), position, dest, offset, length);
//				position += length;
//				return length;
//			} else {
//				if (leftInBuffer < 0) { throw new IndexOutOfBoundsException(); }
//
//				if (leftInBuffer > 0) {
//					/* Copy what's left in buffer to dest */
//					System.arraycopy(buffer.getBuffer(), position, dest, offset, leftInBuffer);
//					position += leftInBuffer;
//				}
//
//				/* And try to read (length - leftInBuffer) more bytes from inputStream, store result in dest */
//
//				if (carrier.markSupported()) { setCarrierPosition(); }
//
//				int bytesRead = carrier.read(dest, offset + leftInBuffer, length - leftInBuffer);
//				if (bytesRead <= 0) { return leftInBuffer; }
//				assert(leftInBuffer + bytesRead <= length);
//				/* Copy what's in dest back to the buffer */
//				try {
//					buffer.addFragment(position + leftInBuffer, dest, offset + leftInBuffer, bytesRead);
//				} catch (ArrayIndexOutOfBoundsException aa) {
//					aa.printStackTrace();
//				}
//				position += bytesRead;
//				return leftInBuffer + bytesRead;
//			}
//
//		}

		public long skip(long n) throws IOException {
				int leftInBuffer = buffer.getBufferedLength(position);
				if (n <= leftInBuffer) {
					/* If we can skip within the buffer, we do */
					position += n;
					return n;
				} else {
					assert(leftInBuffer < n);
					/* Otherwise, skip what's left in buffer, then skip within carrier... */
					position += leftInBuffer;
					long skippedBytes = 0;
					if (carrier.markSupported()) {
						/* First reposition carrier (by reset() and skip()) if not in sync with our position */
						setCarrierPosition();
						skippedBytes = carrier.skip(n - leftInBuffer);
					} else {
						skippedBytes = super.skip(n - leftInBuffer);
					}
					position += (int)skippedBytes;
					return leftInBuffer + skippedBytes;
				}
		}

		public int available() throws IOException {
				return buffer.getBufferedLength(position);
		}

		public void close() throws IOException {
		}

		public synchronized void mark(int readlimit) {
			markedPosition = position;
		}

		public synchronized void reset() throws IOException {
			position = markedPosition;
		}

		public boolean markSupported() {
			return true;
		}

		private void setCarrierPosition() throws IOException {
			if (position < carrier.getPosition()) {
				carrier.reset();
				int bytesSkipped = 0;
				while (bytesSkipped < position) {
					bytesSkipped += carrier.skip(position - bytesSkipped);
				}
			}
		}
	}
}
