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
 * Buffers an inputstream and can supply clients with fresh "copies" (starting at 0) of that inputstream.
 * 
 * TODO: skip based on fragments.
 * TODO: mark, reset.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class InputStreamBuffer {

	/** Keeps track of bytes read into buffer. */
	private int bufferCounter; /* TODO: instead of (or in addition to?) bufferCounter, have multiple fragments. */
	private byte[] buffer;
	private InputStream inputStream;

	public InputStreamBuffer(byte[] bytes) {
		if (bytes == null) {
			this.buffer = null;
			return;
		}
		this.buffer = new byte[bytes.length];
		System.arraycopy(bytes, 0, buffer, 0, bytes.length);
		inputStream = new ByteArrayInputStream(buffer);
		this.bufferCounter = bytes.length;
	}

	public InputStreamBuffer(InputStream inputStream, int fileLength) {
		this.inputStream = inputStream;
		this.bufferCounter = 0;
		this.buffer = new byte[fileLength];
	}

	/**
	 * Gets a copy of the input stream positioned at 0.
	 *
	 * @return
	 */
	public InputStream getInputStream() {
		synchronized(inputStream) {
			if (bufferCounter >= buffer.length) {
				return new ByteArrayInputStream(buffer);
			} else {
				return new SubInputStream(inputStream);
			}
		}
	}

	public synchronized int getPosition() {
		return bufferCounter;
	}

	public int getLength() {
		return buffer.length;
	}

	public class SubInputStream extends InputStream {

		/** Keeps track of bytes served via this stream. Should be less than or equal to bufferCounter. */
		private int streamCounter;

		private Object syncObject;

		public SubInputStream(Object syncObject) {
			streamCounter = 0;
			this.syncObject = syncObject;
		}

		public int getPos() {
			return streamCounter;
		}

		public int getLength() {
			return buffer.length;
		}

		public int read() throws IOException {
			synchronized(syncObject) {
				assert(streamCounter <= bufferCounter);
				if (streamCounter < bufferCounter) {
					return buffer[streamCounter++] & 0xFF;
				} else {
					/* NOTE: streamCounter == bufferCounter */
					int result = inputStream.read();
					if (result < 0) {
						return -1;
					}
					buffer[streamCounter++] = (byte)result;
					bufferCounter++;
					return result;
				}
			}
		}

		public int read(byte[] dest) throws IOException {
			return read(dest, 0, dest.length);
		}

		public int read(byte[] dest, int offset, int length) throws IOException {
			assert(streamCounter <= bufferCounter);
			int leftInBuffer = bufferCounter - streamCounter; // buffered, not yet served via this sub stream
			if (leftInBuffer <= length) {
				/* Copy what's left in buffer */
				System.arraycopy(buffer, streamCounter, dest, offset, leftInBuffer);
				streamCounter += leftInBuffer;
				assert(streamCounter == bufferCounter);

				/* And try to read (length - leftInBuffer) more bytes from inputStream */
				int bytesRead = inputStream.read(buffer, streamCounter, length - leftInBuffer);
				if (bytesRead <= 0) { return leftInBuffer; }
				System.arraycopy(buffer, streamCounter, dest, offset + leftInBuffer, bytesRead);
				streamCounter += bytesRead;
				bufferCounter += bytesRead;
				assert(streamCounter == bufferCounter);
				return bytesRead;
			} else {
				assert(length <= leftInBuffer);
				System.arraycopy(buffer, streamCounter, dest, offset, length);
				bufferCounter += length;
				return length;
			}
		}

		public long skip(long count) throws IOException {
			int leftInBuffer = bufferCounter - streamCounter; // buffered, not yet served via this sub stream
			if (count <= leftInBuffer) {
				System.out.println("DEBUG: SKIPPING " + count);
				bufferCounter += (int)count;
				return count;
			}
			return super.skip(count); /* super.skip() will just call read(byte[]), instead use inputstream.skip(), but our buffer bookkeeping is not up to that now. */
		}
	}
}
