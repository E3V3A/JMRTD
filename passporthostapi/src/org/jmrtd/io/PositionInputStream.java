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

public class PositionInputStream extends InputStream {

	private static final long MARK_NOT_SET = -1L;
	
	private InputStream carrier;
	
	private long position;
	private long markedPosition;

	public PositionInputStream(InputStream carrier) {
		this.carrier = carrier;
		position = 0L;
		markedPosition = MARK_NOT_SET;
	}

	public int read() throws IOException {
		int b = carrier.read();
		if (b >= 0) { position++; }
		return b;
	}

	public int read(byte[] dest) throws IOException {
		return read(dest, 0, dest.length);
	}
	
	public int read(byte[] dest, int offset, int length) throws IOException {
		int bytesRead = carrier.read(dest, offset, length);
		position += bytesRead;
		return bytesRead;
	}
	
	public long skip(long n) throws IOException {
		long skippedBytes = carrier.skip(n);
		if (skippedBytes <= 0) {
//			throw new IllegalStateException("DEBUG: WARNING carrier (" + carrier.getClass().getCanonicalName() + ")'s skip(" + n + ") only skipped " + skippedBytes + ", position = " + position);

			System.out.println("DEBUG: WARNING carrier (" + carrier.getClass().getCanonicalName() + ")'s skip(" + n + ") only skipped " + skippedBytes + ", position = " + position);
		}

		position += skippedBytes;
		return skippedBytes;
	}
	
	public void mark(int readLimit) {
		carrier.mark(readLimit);
		markedPosition = position;
	}
	
	public void reset() throws IOException {
		carrier.reset();
		position = markedPosition;
	}
	
	public boolean markSupported() {
		return carrier.markSupported();
	}
	
	public long getPosition() {
		return position;
	}
}
