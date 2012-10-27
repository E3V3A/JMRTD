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
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1469 $
 */
class MRTDFileSystem implements FileSystemStructured, Serializable {

	private static final long serialVersionUID = -4357282016708205020L;

	private short selectedFID;
	private PassportService service;
	private Map<Short, MRTDFileInfo> files;

	public MRTDFileSystem(PassportService service) {
		this.service = service;
		this.files = new HashMap<Short, MRTDFileInfo>();
	}

	public synchronized byte[] readBinary(int offset, int length) throws CardServiceException {
		MRTDFileInfo fileInfo = null;
		try {
			if (selectedFID == 0) { throw new CardServiceException("No file selected"); }
			boolean readLong = (offset > 0x7FFF);
			fileInfo = files.get(selectedFID);
			byte[] bytes = service.sendReadBinary(offset, length, readLong);
			if (fileInfo != null) {
				fileInfo.addFragment(offset, bytes);
			}
			return bytes;
		} catch (CardServiceException cse) {
			throw new CardServiceException("Send binary failed on file " + (fileInfo == null ? Integer.toHexString(selectedFID) : fileInfo) + ": " + cse.getMessage(), cse.getSW());
		} catch (Exception e) {
			throw new CardServiceException("Send binary failed on file " + (fileInfo == null ? Integer.toHexString(selectedFID) : fileInfo));
		}
	}

	public synchronized void selectFile(short fid) throws CardServiceException {
		if (selectedFID == fid) { return; }
		service.sendSelectFile(fid);
		selectedFID = fid;

		/* This will determine file length if the file was not selected before. */
		getFileInfo();
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

	//		public synchronized void selectFile(short[] path) throws CardServiceException {
	//			if (path == null) { throw new CardServiceException("Path is null"); }
	//			if (path.length <= 0) { throw new CardServiceException("Cannot select empty path"); }
	//			short fid = path[path.length - 1];
	//			selectFile(fid);
	//		}

	private synchronized MRTDFileInfo getFileInfo() throws CardServiceException {
		if (selectedFID == 0) { throw new CardServiceException("No file selected"); }

		MRTDFileInfo fileInfo = files.get(selectedFID);
		if (fileInfo != null) { return fileInfo; }

		try {
			/* Each passport file consists of a TLV structure. */
			/* Woj: no, not each, CVCA does not and has a fixed length */
			byte[] prefix = readBinary(0, 8);
			ByteArrayInputStream baInputStream = new ByteArrayInputStream(prefix);
			TLVInputStream tlvInputStream = new TLVInputStream(baInputStream);
			int fileLength = 0;
			int tag = tlvInputStream.readTag();
			if (tag == CVCAFile.CAR_TAG) {
				fileLength = CVCAFile.LENGTH;
			} else {
				int vLength = tlvInputStream.readLength();
				int tlLength = prefix.length - baInputStream.available();
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
		private int length;

		private byte[] buffer;

		/** Administration of which parts of buffer are filled. */
		private Collection<Fragment> fragments;

		public MRTDFileInfo(short fid, int length) {
			this.fid = fid;
			this.length = length;
			this.buffer = new byte[length];
			this.fragments = new HashSet<Fragment>();
		}

		public short getFID() { return fid; }

		public int getFileLength() { return length; }

		public String toString() {
			return Integer.toHexString(fid);
		}

		public void addFragment(int offset, byte[] bytes) {
			System.arraycopy(bytes, 0, buffer, offset, bytes.length);
			int thisOffset = offset;
			int thisLength = bytes.length;
			for (Fragment other: fragments) {
				/* On partial overlap we change this fragment, possibly remove the other overlapping fragments we encounter. */
				if (other.offset <= thisOffset && thisOffset + thisLength <= other.offset + other.length) {
					/*
					 * [...other fragment.........]
					 *    [...this fragment...]
					 *    
					 * This fragment is already contained in other. Don't add and return immediately.
					 */
					return;
				} else if (other.offset <= thisOffset && thisOffset < other.offset + other.length) {
					/*
					 * [...other fragment...]
					 *         [...this fragment...]
					 *         
					 * This fragment is partially contained in other. Extend this fragment to size of other, remove other.
					 */
					thisLength = thisOffset + thisLength - other.offset;
					thisOffset = other.offset;
					fragments.remove(other);
				}  else if (thisOffset <= other.offset && other.offset + other.length <= thisOffset + thisLength) {
					/*
					 *    [...other fragment...]
					 * [...this fragment...........]
					 * 
					 * The other fragment is contained in this fragment. Remove other.
					 */
					fragments.remove(other);
				} else if (offset <= other.offset && other.offset < thisOffset + thisLength) {
					/*
					 *        [...other fragment...]
					 * [...this fragment...]
					 * 
					 * This fragment is partially contained in other. Extend this fragment to size of other, remove other.
					 */
					thisLength = other.offset + other.length - thisOffset;
					fragments.remove(other);
				}
			}
			fragments.add(new Fragment(thisOffset, thisLength));			
		}

		private static class Fragment {
			private int offset, length;

			public Fragment(int offset, int length) {
				this.offset = offset;
				this.length = length;
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
}
