package org.jmrtd.io.test;

import java.util.Arrays;

import junit.framework.TestCase;

import org.jmrtd.io.FragmentBuffer;
import org.jmrtd.io.FragmentBuffer.Fragment;

public class FragmentBufferTest extends TestCase {

	public void testIsFullyCoveredByFragments() {
		int testCount = 99;
		for (int i = 0; i < testCount; i++) {
			testIsFullyCoveredByFragments(i);
		}
	}
	
	private void testIsFullyCoveredByFragments(int textIndex) {
		FragmentBuffer buffer = createFullyCoveredBuffer(99);
		assertNotNull(buffer);
		byte[] bytes = buffer.getBuffer();
		assertNotNull(bytes);
		for (int i = 0; i < bytes.length; i++) {
			assertTrue(buffer.isCoveredByFragment(i));
		}
	}
	
	public void testIsCoveredByFragment() {
		int totalLength = 99;
		int fragOffset = 42;
		int fragLength = 9;
		byte[] bytes = getRandomBytes(fragLength);
		
		FragmentBuffer buffer = new FragmentBuffer(totalLength);		
		buffer.addFragment(fragOffset, bytes);
		
		for (int i = 0; i < totalLength; i++) {
			if (fragOffset <= i && i < fragOffset + fragLength) { 
				assertTrue(buffer.isCoveredByFragment(i));
			} else {
				assertTrue(!buffer.isCoveredByFragment(i));
				assertTrue(buffer.getBuffer()[i] == 0x00);
			}
		}
	}
	
	public void testAddFragment() {
		FragmentBuffer buffer = new FragmentBuffer(999);
		byte[] bytes = getRandomBytes(20);
		buffer.addFragment(3, bytes);
		buffer.addFragment(10, bytes);
		assertEquals(buffer.getFragments().size(), 1);
		for (Fragment fragment: buffer.getFragments()) {
			assertEquals(fragment.getOffset(), 3);
			assertEquals(fragment.getLength(), 27); /* 27 = 20 + (20 - 10 - 3) */
		}
	}
	
	public void testGetBufferedLength() {
		FragmentBuffer buffer = new FragmentBuffer(999);
		byte[] bytes = getRandomBytes(20);
		buffer.addFragment(3, bytes);
		int bufferedLength = buffer.getBufferedLength(5);
		assertEquals(bufferedLength, 18); /* 18 = 3 + 20 - 5 */
		buffer.addFragment(20, bytes);
		bufferedLength = buffer.getBufferedLength(5);
		assertEquals(bufferedLength, 35);
	}
	
	public void testAddFullyFragments() {
		FragmentBuffer buffer = createFullyCoveredBuffer(999);
		assertEquals(buffer.getFragments().size(), 1);
	}

	public void testAddAdjacentFragmentsWithSingleCountExactOne() {
		FragmentBuffer buffer = new FragmentBuffer(30);
		buffer.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		buffer.addFragment(20, (byte)1);
		
		assertEquals(buffer.getFragments().size(), 1);
	}

	public void testAddAdjacentFragmentsCountExactOne() {
		FragmentBuffer buffer = new FragmentBuffer(30);
		buffer.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		buffer.addFragment(20, new byte[] { 1, 1, 1 });
		
		assertEquals(buffer.getFragments().size(), 1);
	}

	public void testAddAdjacentFragmentsCountExactOneUnordered() {
		FragmentBuffer buffer = new FragmentBuffer(30);
		buffer.addFragment(20, new byte[] { 1, 1, 1 });
		buffer.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		
		assertEquals(buffer.getFragments().size(), 1);
	}
	
	public void testAddAdjacentFragmentsCountEquals() {
		FragmentBuffer buffer = new FragmentBuffer(30);
		buffer.addFragment(20, new byte[] { 1, 1, 1 });
		buffer.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		
		FragmentBuffer buffer2 = new FragmentBuffer(30);
		buffer2.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		buffer2.addFragment(20, new byte[] { 1, 1, 1 });
		
		assertEquals(buffer.getFragments().size(), buffer2.getFragments().size());
	}
	
	public void testAddAdjacentFragmentsWithSingleOrderCountEquals() {
		FragmentBuffer buffer = new FragmentBuffer(30);
		buffer.addFragment(20, (byte)1);
		buffer.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		
		FragmentBuffer buffer2 = new FragmentBuffer(30);
		buffer2.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		buffer2.addFragment(20, (byte)1);
		
		assertEquals(buffer.getFragments().size(), buffer2.getFragments().size());
	}

	public void testAddAdjacentFragmentsOrderEquals() {
		FragmentBuffer buffer = new FragmentBuffer(30);
		buffer.addFragment(20, (byte)1);
		buffer.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		
		FragmentBuffer buffer2 = new FragmentBuffer(30);
		buffer2.addFragment(10, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 });
		buffer2.addFragment(20, (byte)1);
		
		assertEquals(buffer, buffer2);
	}
	
	private FragmentBuffer createFullyCoveredBuffer(int length) {
		byte[] bytes = getRandomBytes(length);
		int fragmentCount = Math.min((int)(Math.abs(Math.random() * Integer.MAX_VALUE)) % bytes.length + 1, bytes.length - 1);
		assertTrue(0 < fragmentCount);
		assertTrue(fragmentCount < bytes.length);

		FragmentBuffer buffer = new FragmentBuffer(bytes.length);
		int fragmentOffset = 0;
		for (int fragmentIndex = 0; fragmentIndex < fragmentCount; fragmentIndex++) {
			int fragmentLength = (int)(Math.abs(Math.random() * (bytes.length - fragmentOffset)) / (fragmentCount - fragmentIndex));
			if (fragmentLength > (fragmentCount - fragmentIndex + 1)) { fragmentLength = fragmentCount - fragmentIndex + 1; }
			if (fragmentIndex == fragmentCount - 1) {
				fragmentLength = bytes.length - fragmentOffset;
			}
			byte[] fragmentBytes = new byte[fragmentLength];
			System.arraycopy(bytes, fragmentOffset, fragmentBytes, 0, fragmentLength);
			buffer.addFragment(fragmentOffset, fragmentBytes);
		}
		assertTrue(Arrays.equals(bytes, buffer.getBuffer()));
		return buffer;
	}
	
	private static byte[] getRandomBytes(int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)(Math.abs(Math.random() * 256));
		}
		return bytes;
	}
}
