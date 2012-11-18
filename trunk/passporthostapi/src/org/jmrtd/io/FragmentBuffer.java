package org.jmrtd.io;

import java.util.Collection;
import java.util.HashSet;

public class FragmentBuffer {

	/** Buffer with the actual bytes. */
	private byte[] buffer;

	/** Administration of which parts of buffer are filled. */
	private Collection<Fragment> fragments;
	
	public FragmentBuffer(int length) {
		this.buffer = new byte[length];
		this.fragments = new HashSet<Fragment>();
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
	
	public byte[] getBuffer() {
		return buffer;
	}
	
	public int getLength() {
		return buffer.length;
	}
	
	/**
	 * Gets the smallest fragment contained in <code>offset</code> and <code>offset + length</code>
	 * that has <strong>not</strong> been buffered in this buffer.
	 * 
	 * @param fileInfo a file info
	 * @param offset the offset
	 * @param length the length
	 * @return
	 */
	public Fragment getSmallestUnbufferedFragment(int offset, int length) {
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
	
	public static class Fragment {

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
