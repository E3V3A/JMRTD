package sos.smartcards;

import java.io.IOException;
import java.io.InputStream;

/**
 * Inputstream for reading ISO 7816 file system cards.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class CardFileInputStream extends InputStream
{
	private short fid;
	private final byte[] buffer;
	private int bufferLength;
	private int offsetBufferInFile;
	private int offsetInBuffer;
	private int markedOffset;
	private int fileLength;
	private FileSystemStructured fs;

	public CardFileInputStream(short fid, int maxBlockSize, FileSystemStructured fs) throws CardServiceException {
		this.fid = fid;
		this.fs = fs;
		fs.selectFile(fid);
		fileLength = fs.getFileLength();
		buffer = new byte[maxBlockSize];
		bufferLength = 0;
		offsetBufferInFile = 0;
		offsetInBuffer = 0;
		markedOffset = -1;
	}

	public int read() throws IOException {
		int offsetInFile = offsetBufferInFile + offsetInBuffer;
		if (offsetInFile >= fileLength) { return -1; }
		if (offsetInBuffer >= bufferLength) {
			int le = Math.min(buffer.length, fileLength - offsetInFile);
			try {
				offsetBufferInFile += bufferLength;
				offsetInBuffer = 0;
				bufferLength = fillBufferFromFile(fid, offsetBufferInFile, le);
			} catch (CardServiceException cse) {
				throw new IOException(cse.toString());
			}
		}
		int result = buffer[offsetInBuffer] & 0xFF;
		offsetInBuffer++;
		return result;
	}

	public long skip(long n) {
		int available = available();
		if (n > available) { n = available; }
		if (n < (buffer.length - offsetInBuffer)) {
			offsetInBuffer += n;
		} else {
			int absoluteOffset = offsetBufferInFile + offsetInBuffer;
			offsetBufferInFile = (int)(absoluteOffset + n);
			offsetInBuffer = 0;
		}
		return n;
	}

	public synchronized int available() {
		return bufferLength - offsetInBuffer;
	}

	public void mark(int readLimit) {
		markedOffset = offsetBufferInFile + offsetInBuffer;
	}

	public void reset() throws IOException {
		if (markedOffset < 0) { throw new IOException("Mark not set"); }
		offsetBufferInFile = markedOffset;
		offsetInBuffer = 0;
		bufferLength = 0;
	}

	public boolean markSupported() {
		return true;
	}
	
	/**
	 * Gets the length of the underlying card file.
	 *
	 * @return the length of the underlying card file.
	 */
	public int getFileLength() {
		return fileLength;
	}
	
	public int getFilePos() {
		return offsetBufferInFile + offsetInBuffer;
	}

	/**
	 * Reads from file with id <code>fid</code>.
	 *
	 * @param fid the file to read
	 * @param offsetInFile starting offset in file
	 * @param length the number of bytes to read, or -1 to read until EOF
	 *
	 * @return the contents of the file.
	 */
	private int fillBufferFromFile(short fid, int offsetInFile, int le) throws CardServiceException {
		synchronized(fs) {
			if (le > buffer.length) { throw new IllegalArgumentException("length too big"); }
			if (fs.getSelectedFID() != fid) { fs.selectFile(fid); }
			byte[] data = fs.readBinary((short)offsetInFile, le);
			System.arraycopy(data, 0, buffer, 0, data.length);
			return data.length;
		}
	}
}
