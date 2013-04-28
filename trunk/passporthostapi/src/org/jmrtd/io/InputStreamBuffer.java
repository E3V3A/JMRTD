/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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
	public SubInputStream getInputStream() {
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

	public class SubInputStream extends InputStream { // FIXME set class visibility to package

		/** The position within this inputstream. */
		private int position;
		private int markedPosition;

		private Object syncObject;

		public SubInputStream(Object syncObject) {
			position = 0;
			markedPosition = -1;
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
					if (carrier.markSupported()) {
						syncCarrierPosition();
					}
					int result = carrier.read();
					if (result < 0) { return -1; }
					buffer.addFragment(position++, (byte)result);
					return result;
				}
			}
		}

		public long skip(long n) throws IOException {
			synchronized(syncObject) {
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
						syncCarrierPosition();
						skippedBytes = carrier.skip(n - leftInBuffer);
						position += (int)skippedBytes;
					} else {
						skippedBytes = super.skip(n - leftInBuffer);
						/* As super.skip will call read, position will be adjusted automatically. */
					}
					return leftInBuffer + skippedBytes;
				}
			}
		}

		public int available() throws IOException {
			return buffer.getBufferedLength(position);
		}

		public void close() throws IOException {
		}

		public synchronized void mark(int readLimit) {
			markedPosition = position;
		}

		public synchronized void reset() throws IOException {
			if (markedPosition < 0) { throw new IOException("Invalid reset, was mark() called?"); }
			position = markedPosition;
		}

		public boolean markSupported() {
			return true;
		}

		public int getPosition() {
			return position;
		}

		/**
		 * If necessary, resets the carrier (which must support mark) and
		 * skips to the current position in the buffer.
		 * 
		 * @throws IOException on error
		 */
		private void syncCarrierPosition() throws IOException {
			if (position == carrier.getPosition()) {
				return;
			}
			carrier.reset();
			int bytesSkipped = 0;
			while (bytesSkipped < position) {
				bytesSkipped += carrier.skip(position - bytesSkipped);
			}
		}
	}
}
