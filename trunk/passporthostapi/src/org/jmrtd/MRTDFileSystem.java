package org.jmrtd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.FileInfo;
import net.sourceforge.scuba.smartcards.FileSystemStructured;
import net.sourceforge.scuba.tlv.TLVInputStream;

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
			Fragment fragment = getSmallestFragment(fileInfo, offset, length);
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
	 * Gets the smallest fragment contained in <code>offset</code> and <code>offset + length</code>
	 * that has not been buffered in <code>fileInfo</code>'s buffer.
	 * 
	 * @param fileInfo a file info
	 * @param offset the offset
	 * @param length the length
	 * @return
	 */
	private Fragment getSmallestFragment(MRTDFileInfo fileInfo, int offset, int length) {
		Collection<Fragment> fragments = fileInfo.getFragments();
		int thisOffset = offset, thisLength = length;
		for (Fragment other: fragments) {
			/* On partial overlap we change this fragment, removing sections already buffered. */
			if (other.getOffset() <= thisOffset && thisOffset + thisLength <= other.getOffset() + other.getLength()) {
				/*
				 * [...other fragment.........]
				 *    [...this fragment...]
				 *    
				 * This fragment is already contained in other. Don't add and return immediately.
				 */
				thisLength = 0; /* NOTE: we don't care about offset */
				break;
			} else if (other.getOffset() <= thisOffset && thisOffset < other.getOffset() + other.getLength()) {
				/*
				 * [...other fragment...]
				 *         [...this fragment...]
				 *         
				 * This fragment is partially contained in other. Only fetch the trailing part of this fragment.
				 */
				int newOffset = other.getOffset() + other.getLength();
				int newLength = thisOffset + thisLength - newOffset;
				thisOffset = newOffset;
				thisLength = newLength;
			}  else if (thisOffset <= other.getOffset() && other.getOffset() + other.getLength() <= thisOffset + thisLength) {
				/*
				 *    [...other fragment...]
				 * [...this fragment...........]
				 * 
				 * The other fragment is contained in this fragment. We send this fragment as is.
				 */
				continue;
			} else if (offset <= other.getOffset() && other.getOffset() < thisOffset + thisLength) {
				/*
				 *        [...other fragment...]
				 * [...this fragment...]
				 * 
				 * This fragment is partially contained in other. Only send the leading part of this fragment.
				 */
				thisLength = other.getOffset() - thisOffset;
			}
		}
		return Fragment.getInstance(thisOffset, thisLength);
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
		private byte[] buffer;

		/** Administration of which parts of buffer are filled. */
		private Collection<Fragment> fragments;

		public MRTDFileInfo(short fid, int length) {
			this.fid = fid;
			this.buffer = new byte[length];
			this.fragments = new HashSet<Fragment>();
		}

		public byte[] getBuffer() {
			return buffer;
		}

		public short getFID() { return fid; }

		public int getFileLength() { return buffer.length; }

		public String toString() {
			return Integer.toHexString(fid);
		}

		/**
		 * Adds a fragment of bytes at a specific offset to this file.
		 * 
		 * @param offset the offset
		 * @param bytes the bytes
		 */
		public void addFragment(int offset, byte[] bytes) {
			System.arraycopy(bytes, 0, buffer, offset, bytes.length);
			int thisOffset = offset;
			int thisLength = bytes.length;
			for (Fragment other: fragments) {
				/* On partial overlap we change this fragment, possibly remove the other overlapping fragments we encounter. */
				if (other.getOffset() <= thisOffset && thisOffset + thisLength <= other.getOffset() + other.getLength()) {
					/*
					 * [...other fragment.........]
					 *    [...this fragment...]
					 *    
					 * This fragment is already contained in other. Don't add and return immediately.
					 */
					return;
//				} else if (other.getOffset() <= thisOffset && thisOffset < other.getOffset() + other.getLength()) {
				} else if (other.getOffset() <= thisOffset && thisOffset <= other.getOffset() + other.getLength()) {
					/*
					 * [...other fragment...]
					 *         [...this fragment...]
					 *         
					 * This fragment is partially contained in other. Extend this fragment to size of other, remove other.
					 */
					thisLength = thisOffset + thisLength - other.getOffset();
					thisOffset = other.getOffset();
					fragments.remove(other);
				}  else if (thisOffset <= other.getOffset() && other.getOffset() + other.getLength() <= thisOffset + thisLength) {
					/*
					 *    [...other fragment...]
					 * [...this fragment...........]
					 * 
					 * The other fragment is contained in this fragment. Remove other.
					 */
					fragments.remove(other);
//				} else if (offset <= other.getOffset() && other.getOffset() < thisOffset + thisLength) {
				} else if (other.getOffset() <= thisOffset && thisOffset <= other.getOffset() + other.getLength()) {
					/*
					 *        [...other fragment...]
					 * [...this fragment...]
					 * 
					 * This fragment is partially contained in other. Extend this fragment to size of other, remove other.
					 */
					thisLength = other.getOffset() + other.getLength() - thisOffset;
					fragments.remove(other);
				}
			}
			fragments.add(Fragment.getInstance(thisOffset, thisLength));			
		}

		public Collection<Fragment> getFragments() {
			return fragments;
		}
	}

	private static class Fragment {

		private int offset, length;

		public int getOffset() {
			return offset;
		}

		public int getLength() {
			return length;
		}

		private Fragment(int offset, int length) {
			this.offset = offset;
			this.length = length;
		}

		public static Fragment getInstance(int offset, int length) {
			return new Fragment(offset, length);
		}

		public String toString() {
			return "[" + offset + " .. " + (offset + length - 1)  + " (" + length + ")]";
		}

		public boolean equals(Object otherObject) {
			if (otherObject == null) { return false; }
			if (otherObject == this) { return true; }
			if (!otherObject.getClass().equals(Fragment.class)) { return false; }
			Fragment otherFragment = (Fragment)otherObject;
			return otherFragment.offset == offset && otherFragment.length == length;
		}

		public int hashCode() {
			return 2 * offset + 3 * length + 5;
		}
	}
}
