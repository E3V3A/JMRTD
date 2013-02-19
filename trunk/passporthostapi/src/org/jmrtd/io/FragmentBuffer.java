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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * A buffer that can be partially filled.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class FragmentBuffer {

	/** Buffer with the actual bytes. */
	private byte[] buffer; // FIXME can we make this buffer grow dynamically?
	
	/** Administration of which parts of buffer are filled. */
	private Collection<Fragment> fragments;

	/**
	 * Creates a fragment buffer.
	 */
	public FragmentBuffer() {
		this(1024);
	}
	
	/**
	 * Creates a fragment buffer.
	 * 
	 * @param length the length of the buffer
	 */
	public FragmentBuffer(int length) {
		this.buffer = new byte[length];
		this.fragments = new HashSet<Fragment>();
	}
	
	public synchronized void addFragment(int offset, byte b) {
		/* FIXME: can this be done more efficiently for common case resulting from InputStreamBuffer read, scan all fragments and extend neighboring one */
		addFragment(offset, new byte[] { b });
	}
	
	/**
	 * Adds a fragment of bytes at a specific offset to this file.
	 * 
	 * @param offset the fragment offset
	 * @param bytes the bytes from which fragment content will be copied
	 */
	public synchronized void addFragment(int offset, byte[] bytes) {
		addFragment(offset, bytes, 0, bytes.length);
	}

	/**
	 * Adds a fragment of bytes at a specific offset to this file.
	 * 
	 * @param offset the fragment offset
	 * @param bytes the bytes from which fragment contents will be copied
	 * @param srcOffset the offset within bytes where the contents of the fragment start
	 * @param srcLength the length of the fragment
	 */
	public synchronized void addFragment(int offset, byte[] bytes, int srcOffset, int srcLength) {
		if (offset + srcLength > buffer.length) {
			setLength(2 * Math.max(offset + srcLength, buffer.length));
		}

		System.arraycopy(bytes, srcOffset, buffer, offset, srcLength);
		int thisOffset = offset;
		int thisLength = srcLength;
		final Collection<Fragment> otherFragments = new ArrayList<Fragment>(fragments);
		for (Fragment other: otherFragments) {
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
			} else if (thisOffset <= other.getOffset() && other.getOffset() <= thisOffset + thisLength) {
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
	
	public synchronized int getPosition() {
		int result = 0;
		for (int i = 0; i < buffer.length; i++) {
			if (isCoveredByFragment(i)) {
				result = i + 1;
			}
		}
		return result;
	}
	
	public synchronized int getBytesBuffered() {
		int result = 0;
		for (int i = 0; i < buffer.length; i++) {
			if (isCoveredByFragment(i)) {
				result++;
			}
		}
		return result;
	}
	
	public synchronized boolean isCoveredByFragment(int offset) {
		return isCoveredByFragment(offset, 1);
	}
	
	public synchronized boolean isCoveredByFragment(int offset, int length) {
		for (Fragment fragment: fragments) {
			int left = fragment.getOffset();
			int right = fragment.getOffset() + fragment.getLength();
			if (left <= offset && offset + length <= right) { return true; }
		}
		return false;		
	}
	
	/**
	 * Calculates the number of bytes left in the buffer starting from index <code>index</code>.
	 * 
	 * @param index the index
	 *
	 * @return the number of bytes left in the buffer
	 */
	public synchronized int getBufferedLength(int index) {
		int result = 0;
		if (index >= buffer.length) { return 0; }
		for (Fragment fragment: fragments) {
			int left = fragment.getOffset();
			int right = fragment.getOffset() + fragment.getLength();
			if (left <= index && index < right) {
				int newResult = right - index;
				if (newResult > result) { result = newResult; }
			}
		}
		return result;
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
	public synchronized Fragment getSmallestUnbufferedFragment(int offset, int length) {
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
	
	public synchronized String toString() {
		return "FragmentBuffer [" + buffer.length + ", " + fragments + "]";
	}
	
	public synchronized boolean equals(Object otherObject) {
		if (otherObject == null) { return false; }
		if (otherObject == this) { return true; }
		if (!otherObject.getClass().equals(FragmentBuffer.class)) { return false; }
		FragmentBuffer otherBuffer = (FragmentBuffer)otherObject;
		if (otherBuffer.buffer == null && this.buffer != null) { return false; }
		if (otherBuffer.buffer != null && this.buffer == null) { return false; }
		if (otherBuffer.fragments == null && this.fragments != null) { return false; }
		if (otherBuffer.fragments != null && this.fragments == null) { return false; }
		return Arrays.equals(otherBuffer.buffer, this.buffer) && otherBuffer.fragments.equals(this.fragments);
	}

	private synchronized void setLength(int length) {
		if (length <= buffer.length) { return; }
		byte[] newBuffer = new byte[length];
		System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.length);
		this.buffer = newBuffer;
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
