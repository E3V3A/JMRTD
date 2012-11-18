package org.jmrtd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.FileInfo;
import net.sourceforge.scuba.smartcards.FileSystemStructured;
import net.sourceforge.scuba.tlv.TLVInputStream;

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
 * @version $Revision: 1469 $
 */
class MRTDFileSystem implements FileSystemStructured, Serializable {

	private static final long serialVersionUID = -4357282016708205020L;

	/** Number of bytes to read at start of file to determine file length. */
	private static final int READ_AHEAD_LENGTH = 8;

	private short selectedFID;
	private PassportService service;
	private Map<Short, MRTDFileInfo> files;

	public MRTDFileSystem(PassportService service) {
		this.service = service;
		this.files = new HashMap<Short, MRTDFileInfo>();
	}

	public synchronized FileInfo[] getSelectedPath() {
		try {
			MRTDFileInfo fileInfo = getFileInfo();
			return new MRTDFileInfo[]{ fileInfo };
		} catch (CardServiceException cse) {
			cse.printStackTrace();
			return null;
		}
	}

	public synchronized void selectFile(short fid) throws CardServiceException {
		if (selectedFID == fid) { return; }
		service.sendSelectFile(fid);
		selectedFID = fid;

		/* This will determine file length if the file was not read before. */
		getFileInfo();
	}

	public synchronized byte[] readBinary(int offset, int length) throws CardServiceException {
		MRTDFileInfo fileInfo = null;
		try {
			if (selectedFID == 0) { throw new CardServiceException("No file selected"); }
			boolean readLong = (offset > 0x7FFF);
			fileInfo = getFileInfo();
			assert(fileInfo != null);
			Fragment fragment = fileInfo.getSmallestUnbufferedFragment(offset, length);
			if (fragment.getLength() > 0) {
				byte[] bytes = service.sendReadBinary(fragment.getOffset(), fragment.getLength(), readLong);
				if (fileInfo != null) {
					fileInfo.addFragment(fragment.getOffset(), bytes);
				}
			}
			byte[] buffer = fileInfo.getBuffer();
			byte[] result = new byte[length];
			System.arraycopy(buffer, offset, result, 0, length);
			return result;
		} catch (CardServiceException cse) {
			throw new CardServiceException("Send binary failed on file " + (fileInfo == null ? Integer.toHexString(selectedFID) : fileInfo) + ": " + cse.getMessage(), cse.getSW());
		} catch (Exception e) {
			throw new CardServiceException("Send binary failed on file " + (fileInfo == null ? Integer.toHexString(selectedFID) : fileInfo));
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
		if (selectedFID == 0) { throw new CardServiceException("No file selected"); }

		MRTDFileInfo fileInfo = files.get(selectedFID);
		if (fileInfo != null) { return fileInfo; }

		try {
			/* Each passport file consists of a TLV structure. */
			/* Woj: no, not each, CVCA does not and has a fixed length */
			byte[] prefix = service.sendReadBinary(0, READ_AHEAD_LENGTH, false);
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
			files.put(selectedFID, fileInfo);
			return fileInfo;
		} catch (IOException ioe) {
			throw new CardServiceException(ioe.toString());
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
