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
 * $Id$
 */

package org.jmrtd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.smartcards.FileInfo;
import net.sf.scuba.smartcards.FileSystemStructured;
import net.sf.scuba.tlv.TLVInputStream;

import org.jmrtd.io.FragmentBuffer;
import org.jmrtd.io.FragmentBuffer.Fragment;
import org.jmrtd.lds.CVCAFile;

/**
 * A file system for ICAO MRTDs.
 * 
 * TODO: use maxBlockSize to fetch extra bytes in APDU when space left (e.g. first APDU after length determination will be 0xD7 instead of 0xDF
 * TODO: join fragments in addFragment that are next to each other (overlap 0, currently only on positive overlap)
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision$
 */
class MRTDFileSystem implements FileSystemStructured, Serializable {

	private static final long serialVersionUID = -4357282016708205020L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	/** Number of bytes to read at start of file to determine file length. */
	private static final int READ_AHEAD_LENGTH = 8;

	/** Indicates the file that is (or should be) selected. */
	private short selectedFID;

	/** Indicates whether we actually already sent the SELECT command to select <code>selectedFID</code>. */
	private boolean isSelected;

	private PassportService service;
	private Map<Short, MRTDFileInfo> fileInfos;

	public MRTDFileSystem(PassportService service) {
		this.service = service;
		this.fileInfos = new HashMap<Short, MRTDFileInfo>();
		this.selectedFID = 0;
		this.isSelected = false;
	}

	public synchronized FileInfo[] getSelectedPath() throws CardServiceException {
		MRTDFileInfo fileInfo = getFileInfo();
		return new MRTDFileInfo[]{ fileInfo };
	}

	/*
	 * NOTE: This doesn't actually send a select file command. ReadBinary will do so
	 * if needed.
	 */
	public synchronized void selectFile(short fid) throws CardServiceException {
		if (selectedFID == fid) { return; }
		selectedFID = fid;
		isSelected = false;
	}

	public synchronized byte[] readBinary(int offset, int length) throws CardServiceException {
		MRTDFileInfo fileInfo = null;
		try {
			if (selectedFID <= 0) { throw new CardServiceException("No file selected"); }
			boolean isExtendedLength = (offset > 0x7FFF);
			if (!isSelected) {
				service.sendSelectFile(selectedFID);
				isSelected = true;
			}

			/* Check buffer to see if we already have some of the bytes. */
			fileInfo = getFileInfo();
			assert(fileInfo != null);
			Fragment fragment = fileInfo.getSmallestUnbufferedFragment(offset, length);
			if (fragment.getLength() > 0) {
				byte[] bytes = service.sendReadBinary(fragment.getOffset(), fragment.getLength(), isExtendedLength);
				/* Update buffer with newly read bytes. */
				fileInfo.addFragment(fragment.getOffset(), bytes);
			}
			/* Shrink wrap the bytes that are now buffered. */
			/* FIXME: that arraycopy looks costly, consider using dest array and offset params instead of byte[] result... -- MO */
			byte[] buffer = fileInfo.getBuffer();
			byte[] result = new byte[length];
			System.arraycopy(buffer, offset, result, 0, length);
			return result;
		} catch (CardServiceException cse) {
			throw new CardServiceException("Read binary failed on file " + (fileInfo == null ? Integer.toHexString(selectedFID) : fileInfo) + ": " + cse.getMessage(), cse.getSW());
		} catch (Exception e) {
			throw new CardServiceException("Read binary failed on file " + (fileInfo == null ? Integer.toHexString(selectedFID) : fileInfo));
		}
	}

	/**
	 * Gets the file info object for the currently selected file. If this
	 * executes normally the result is non-null. If the file has not been
	 * read before this will send a READ_BINARY to determine length.
	 * 
	 * @return a non-null MRTDFileInfo
	 * 
	 * @throws CardServiceException on error
	 */
	private synchronized MRTDFileInfo getFileInfo() throws CardServiceException {
		if (selectedFID <= 0) { throw new CardServiceException("No file selected"); }

		MRTDFileInfo fileInfo = fileInfos.get(selectedFID);

		/* If known file, use file info from cache. */
		if (fileInfo != null) { return fileInfo; }

		/* Not cached, actually read some bytes to determine file info. */
		try {
			if (!isSelected) {
				service.sendSelectFile(selectedFID);
				isSelected = true;
			}

			/*
			 * Each passport file consists of a TLV structure, read ahead to determine length.
			 * EF.CVCA is the exception and has a fixed length of CVCAFile.LENGTH.
			 */
			byte[] prefix = service.sendReadBinary(0, READ_AHEAD_LENGTH, false);
			if (prefix == null || prefix.length != READ_AHEAD_LENGTH) {
				LOGGER.severe("Something is wrong with prefix, prefix = " + Arrays.toString(prefix));
			}
			ByteArrayInputStream baInputStream = new ByteArrayInputStream(prefix);
			TLVInputStream tlvInputStream = new TLVInputStream(baInputStream);
			int fileLength = 0;
			int tag = tlvInputStream.readTag();
			if (tag == CVCAFile.CAR_TAG) {
				fileLength = CVCAFile.LENGTH;
			} else {
				int vLength = tlvInputStream.readLength();
				int tlLength = prefix.length - baInputStream.available(); /* NOTE: we're using a specific property of ByteArrayInputStream's available method here! */
				fileLength = tlLength + vLength;
			}
			fileInfo = new MRTDFileInfo(selectedFID, fileLength);
			fileInfo.addFragment(0, prefix);
			fileInfos.put(selectedFID, fileInfo);
			return fileInfo;
		} catch (IOException ioe) {
			throw new CardServiceException(ioe.toString() + " getting file info for " + Integer.toHexString(selectedFID));
		}
	}

	private static class MRTDFileInfo extends FileInfo implements Serializable {

		private static final long serialVersionUID = 6727369753765119839L;

		private short fid;
		private FragmentBuffer buffer;

		public MRTDFileInfo(short fid, int length) {
			this.fid = fid;
			this.buffer = new FragmentBuffer(length);
		}

		public byte[] getBuffer() {
			return buffer.getBuffer();
		}

		public short getFID() { return fid; }

		public int getFileLength() { return buffer.getLength(); }

		public String toString() {
			return Integer.toHexString(fid);
		}

		public Fragment getSmallestUnbufferedFragment(int offset, int length) {
			return buffer.getSmallestUnbufferedFragment(offset, length);
		}

		/**
		 * Adds a fragment of bytes at a specific offset to this file.
		 * 
		 * @param offset the offset
		 * @param bytes the bytes
		 */
		public void addFragment(int offset, byte[] bytes) {
			buffer.addFragment(offset, bytes);
		}
	}
}
