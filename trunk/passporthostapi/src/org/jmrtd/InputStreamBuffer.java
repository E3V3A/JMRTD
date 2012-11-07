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

package org.jmrtd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Buffers an inputstream and can supply clients with fresh copies (starting at 0) of that inputstream.
 * 
 * TODO: implement read(byte[], ...), skip, mark, reset.
 * TODO: implement getInputStream with offset.
 * TODO: this should probably be moved to some utility package, not JMRTD specific.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
class InputStreamBuffer {

	private int bufferCounter;
	private int fileLength;
	private byte[] buffer;
	private InputStream inputStream;

	public InputStreamBuffer(byte[] bytes) {
		this.buffer = bytes; // FIXME deep copy?
		this.fileLength = bytes.length;
		this.bufferCounter = bytes.length;
	}

	public InputStreamBuffer(InputStream inputStream, int fileLength) {
		this.inputStream = inputStream;
		this.bufferCounter = 0;
		this.fileLength = fileLength;
		this.buffer = new byte[fileLength];
	}

	/**
	 * Gets a copy of the input stream positioned at 0.
	 *
	 * @return
	 */
	public InputStream getInputStream() {
		synchronized(inputStream) {
			if (bufferCounter >= fileLength) {
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
		return fileLength;
	}

	public class SubInputStream extends InputStream {

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
			return fileLength;
		}
		
		public int read() throws IOException {
			synchronized(syncObject) {
				assert(streamCounter <= bufferCounter);
				if (streamCounter < bufferCounter) {
					return buffer[streamCounter++] & 0xFF;
				} else {
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
	}
}
