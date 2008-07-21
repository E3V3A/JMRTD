/*
 * $Id: $
 */

package sos.tlv;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * TLV input stream.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class BERTLVInputStream extends InputStream
{
	/** Carrier. */
	private DataInputStream in;

	private State state;
	private State markedState;

	/**
	 * Constructs a new TLV stream based on another stream.
	 * 
	 * @param in a TLV object
	 */
	public BERTLVInputStream(InputStream in) {
		this.in = new DataInputStream(in);
		state = new State();
		markedState = null;
	}

	public int readTag() throws IOException {
		int tag = -1;
		int bytesRead = 0;
		try {
			int b = in.readUnsignedByte(); bytesRead++;
			while (b == 0x00 || b == 0xFF) {
				b = in.readUnsignedByte(); bytesRead++; /* skip 00 and FF */
			}
			switch (b & 0x1F) {
			case 0x1F:
				tag = b; /* We store the first byte including LHS nibble */
				b = in.readUnsignedByte(); bytesRead++;
				while ((b & 0x80) == 0x80) {
					tag <<= 8;
					tag |= (b & 0x7F);
					b = in.readUnsignedByte(); bytesRead++;
				}
				tag <<= 8;
				tag |= (b & 0x7F);
				/*
				 * Byte with MSB set is last byte of
				 * tag...
				 */
				break;
			default:
				tag = b;
			break;
			}
			state.setTagRead(tag, bytesRead);
			return tag;
		} catch (IOException e) {
			throw e;
		}
	}

	public int readLength() throws IOException {
		try {
			if (!state.isAtStartOfLength()) { throw new IllegalStateException("Not at start of length"); }
			int bytesRead = 0;
			int length = 0;
			int b = in.readUnsignedByte(); bytesRead++;
			if ((b & 0x80) == 0x00) {
				/* short form */
				length = b;
			} else {
				/* long form */
				int count = b & 0x7F;
				length = 0;
				for (int i = 0; i < count; i++) {
					b = in.readUnsignedByte(); bytesRead++;
					length <<= 8;
					length |= b;
				}
			}
			state.setLengthRead(length, bytesRead);
			return length;
		} catch (IOException e) {
			throw e;
		}
	}

	public byte[] readValue() throws IOException {
		try {
			int length = state.getLength();
			byte[] value = new byte[length];
			in.readFully(value);
			state.updateValueBytesRead(length);
			return value;
		} catch (IOException e) {
			throw e;
		}
	}

	private long skipValue() throws IOException {
		if (state.isAtStartOfTag()) { return 0; }
		if (state.isAtStartOfLength()) { return 0; }
		int bytesLeft = state.getValueBytesLeft();
		return skip(bytesLeft);
	}


	/**
	 * Skips in this stream until a given tag is found (depth first).
	 * The stream is positioned right after the first occurence of the tag.
	 * 
	 * @param tag the tag to search for
	 * @throws IOException
	 */
	public void skipToTag(short searchTag) throws IOException {
		while (true) {
			/* Get the next tag. */
			int tag = -1;
			if (state.isAtStartOfTag()) {
				/* Nothing. */
			} else if (state.isAtStartOfLength()) {
				readLength();
				if (isPrimitive(state.getTag())) { skipValue(); }
			} else {
				if (isPrimitive(state.getTag())) { skipValue(); }

			}
			tag = readTag();
			if  (tag == searchTag) { return; }

			if (isPrimitive(tag)) {
				readLength();
				skipValue(); /* Now at next tag. */
			}
		}
	}

	public int available() throws IOException {
		return in.available();
	}

	public int read() throws IOException {
		int result = in.read();
		state.updateValueBytesRead(1);
		return result;
	}

	public long skip(long n) throws IOException {
		long result = in.skip(n);
		state.updateValueBytesRead((int)result);
		return result;
	}

	public synchronized void mark(int readLimit) {
		in.mark(readLimit);
		markedState = state; /* FIXME: need deep copy */
	}


	public boolean markSupported() {
		return false; // FIXME: see mark(), in.markSupported();
	}

	public synchronized void reset() throws IOException {
		if (!markSupported()) {
			throw new IOException("mark/reset not supported");
		}
		in.reset();
		state = markedState;
	}

	public void close() throws IOException {
		in.close();
	}

	private static boolean isPrimitive(int tag) {
		int i = 3;
		for (; i >= 0; i--) {
			int mask = (0xFF << (8 * i));
			if ((tag & mask) != 0x00) { break; }
		}
		int msByte = (((tag & (0xFF << (8 * i))) >> (8 * i)) & 0xFF);
		boolean result = ((msByte & 0x20) == 0x00);
		return result;
	}

	/**
	 *
	 */
	private class State
	{
		private Stack<TLStruct> state;
		private boolean isAtStartOfTag;

		public State() {
			state = new Stack<TLStruct>();
			isAtStartOfTag = true;
		}

		public boolean isAtStartOfTag() { /* FIXME: wrong */
			return isAtStartOfTag;
//			if (state.isEmpty()) { return true; }
//			TLStruct currentObject = state.peek();
//			return (currentObject.getLength() >= 0 && currentObject.getBytesRead() == 0);
		}

		public boolean isAtStartOfLength() {
			if (state.isEmpty()) { return false; }
			TLStruct currentObject = state.peek();
			return currentObject.getLength() < 0;
		}

		public int getTag() {
			if (state.isEmpty()) {
				throw new IllegalStateException("Tag not yet read.");
			}
			TLStruct currentObject = state.peek();
			return currentObject.getTag();
		}

		public int getLength() {
			if (state.isEmpty()) {
				throw new IllegalStateException("Length not yet read.");
			}
			TLStruct currentObject = state.peek();
			int length = currentObject.getLength();
			if (length < 0) {
				throw new IllegalStateException("Length not yet read.");
			}
			return length;
		}

		public int getValueBytesLeft() {
			if (state.isEmpty()) {
				throw new IllegalStateException("Not yet reading value.");
			}
			TLStruct currentObject = state.peek();
			int currentLength = currentObject.getLength();
			if (currentLength < 0) {
				throw new IllegalStateException("Not yet reading value.");
			}
			int currentBytesRead = currentObject.getBytesRead();
			return currentLength - currentBytesRead;
		}

		public void setTagRead(int tag, int bytesRead) {
			TLStruct obj = new TLStruct(tag, -1);
			if (!state.isEmpty()) {
				TLStruct parent = state.peek();
				parent.updateValueBytesRead(bytesRead);
			}
			state.push(obj);
			isAtStartOfTag = false;
		}

		public void setLengthRead(int length, int bytesRead) {
			if (length < 0) {
				throw new IllegalArgumentException("Cannot set negative length (length = " + length + ").");
			}
			TLStruct obj = state.pop();
			if (!state.isEmpty()) {
				TLStruct parent = state.peek();
				parent.updateValueBytesRead(bytesRead);
			}
			obj.setLength(length);
			state.push(obj);
			isAtStartOfTag = false;
		}

		public void updateValueBytesRead(int n) {
			if (state.isEmpty()) { return; }
			TLStruct currentObject = state.peek();
			int bytesLeft = currentObject.getLength() - currentObject.getBytesRead();
			if (n > bytesLeft) {
				throw new IllegalArgumentException("Cannot read " + n + " bytes! Only " + bytesLeft + " bytes left in this TLV object " + currentObject);
			}
			currentObject.updateValueBytesRead(n);
			int currentLength = currentObject.getLength();
			if (currentObject.getBytesRead() == currentLength) {
				state.pop();
				/* Recursively update parent. */
				updateValueBytesRead(currentLength);
				isAtStartOfTag = true;
			} else {
				isAtStartOfTag = false;
			}
		}

		private class TLStruct implements Cloneable
		{
			private int tag, length, bytesRead;

			public TLStruct(int tag, int length) {
				this.tag = tag; this.length = length; this.bytesRead = 0;
			}

			public void setLength(int length) {
				this.length = length;
			}

			public int getTag() { return tag; }

			public int getLength() { return length; }

			public int getBytesRead() { return bytesRead; }

			public void updateValueBytesRead(int n) {
				this.bytesRead += n;
			}

			public Object clone() {
				TLStruct result = new TLStruct(tag, length);
				result.bytesRead = bytesRead;
				return result;
			}

			public String toString() { return "[TLStruct " + Integer.toHexString(tag) + ", " + length + ", " + bytesRead + "]"; }
		}
	}
}
