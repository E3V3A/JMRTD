package org.jmrtd.io.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;

import org.jmrtd.io.InputStreamBuffer;

public class InputStreamBufferTest extends TestCase {

	/*
	 * Tests read(byte[], ...).
	 */
	public void testGetInputStreamReadBytes() {
		try {
			byte[] bytes = new byte[30];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte)(25 + i);
			}
			InputStream a = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(a, bytes.length);
			DataInputStream b = new DataInputStream(inputStreamBuffer.getInputStream());
			DataInputStream c = new DataInputStream(inputStreamBuffer.getInputStream());
			byte[] bBytes = new byte[bytes.length];
			byte[] cBytes = new byte[bytes.length];
			b.readFully(bBytes);
			c.readFully(cBytes);
			assert(Arrays.equals(bBytes, cBytes));
			assert(Arrays.equals(bBytes, bytes));
			assert(Arrays.equals(bytes, cBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/*
	 * Tests read() and read(byte[], ...).
	 */
	public void testGetInputStreamReadReadBytes() {
		try {
			int length = 999;
			byte[] bytes = new byte[length];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte)(Math.random());
			}		
			InputStream a = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(a, bytes.length);

			int testCount = 99;
			for (int testIndex = 0; testIndex < testCount; testIndex ++) {
				InputStream b = inputStreamBuffer.getInputStream();
				byte[] bBytes = new byte[length];

				/* Read prefix with single reads */
				int normalReadCount = (int)(Math.abs(Math.random() * length)); 
				for (int i = 0; i < normalReadCount; i++) {
					bBytes[i] = (byte)b.read();
				}

				/* Read the rest with one block read */
				b.read(bBytes, normalReadCount, length - normalReadCount);

				assert(Arrays.equals(bytes, bBytes));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testInputStreamBufferSkip() {
		int testCount = 999;
		for (int i = 0; i < testCount; i++) {
			testInputStreamBufferSkip(i);
		}
	}

	public void testInputStreamBufferPrefix() {
		int testCount = 999;
		for (int i = 0; i < testCount; i++) {
			testInputStreamBufferPrefix(i);
		}
	}
	
	/*
	 * Tests relative start of resulting inputstream.
	 */
	private void testInputStreamBufferPrefix(int testIndex) {
		try {
			int length = 999;
			assert(length > 2);
			byte[] bytes = new byte[length];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte)(Math.random());
			}
			InputStream a = new ByteArrayInputStream(bytes);

			int prefixLength = (int)(1 + Math.abs(Math.random() * (length - 2)));
			assert(prefixLength > 0);
			assert(prefixLength < length);
			byte[] prefix = new byte[prefixLength];

			int prefixBytesRead = a.read(prefix);
			assert(prefixBytesRead > 0);
			assert(prefixBytesRead < length);
			
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(a, bytes.length);
			DataInputStream b = new DataInputStream(inputStreamBuffer.getInputStream());
			byte[] bBytes = new byte[length - prefixBytesRead];
			b.readFully(bBytes);

			/* Really at EOS */
			assert(b.read() < 0);
			assert(b.read() < 0);
			assert(b.read() < 0);

			for (int i = 0; i < prefixBytesRead; i++) {
				assertEquals(prefix[i], bytes[i + 1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/*
	 * Tests skip(int) in combination with read(byte[], ...).
	 */
	public void testInputStreamBufferSkip(int testIndex) {
		try {
			int length = 999;
			assert(length > 0);
			int bytesToSkip = (int)(1 + Math.abs(Math.random() * (length - 1)));
			assert(bytesToSkip > 0);
			int bytesToReadBeforeSkip = (int)(Math.abs(Math.random() * (length - bytesToSkip - 1)));
			assert(bytesToReadBeforeSkip >= 0);
			assert(0 <= (bytesToReadBeforeSkip + bytesToSkip));
			assert((bytesToReadBeforeSkip + bytesToSkip) <= length);
			byte[] bytes = new byte[length];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte)(Math.random());
			}		
			InputStream a = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(a, bytes.length);

			InputStream b = inputStreamBuffer.getInputStream();
			byte[] prefix = new byte[bytesToReadBeforeSkip];
			int bytesReadBeforeSkip = b.read(prefix);
			int skippedBytes = (int)b.skip(bytesToSkip);
			if (skippedBytes != bytesToSkip) {
				System.out.println("DEBUG: bytesToSkip was " + bytesToSkip + ", but skippedBytes was " + skippedBytes);
			}
			byte[] postfix = new byte[length - bytesToReadBeforeSkip - skippedBytes];
			int bytesReadAfterSkip = b.read(postfix);
			
			/* At EOS */
			assert(b.read() < 0);
			
			byte[] originalPrefix = new byte[bytesReadBeforeSkip];
			System.arraycopy(bytes, 0, originalPrefix, 0, originalPrefix.length);
			assert(Arrays.equals(prefix,  originalPrefix));
			byte[] originalPostfix = new byte[bytesReadAfterSkip];
			System.arraycopy(bytes, bytesToReadBeforeSkip + bytesToSkip, originalPostfix, 0, originalPostfix.length);
			assert(Arrays.equals(postfix,  originalPostfix));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
