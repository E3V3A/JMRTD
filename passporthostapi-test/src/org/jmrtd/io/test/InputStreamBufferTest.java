package org.jmrtd.io.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.jmrtd.io.InputStreamBuffer;
import org.jmrtd.io.PositionInputStream;

public class InputStreamBufferTest extends TestCase {

	public void testReadLoop() {
		try {
			int length = 999;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			InputStream a = inputStreamBuffer.getInputStream();
			for (int i = 0; i < length; i++) {
				int v = a.read();
				assertEquals(v, bytes[i] & 0xFF);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReadPosition() {
		try {
			int length = 999;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			assertEquals(inputStreamBuffer.getPosition(), 0);
			InputStream a = inputStreamBuffer.getInputStream();
			a.read();
			assertEquals(inputStreamBuffer.getPosition(), 1);
			a.read();
			assertEquals(inputStreamBuffer.getPosition(), 2);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/*
	 * Tests read(byte[], ...).
	 */
	public void testReadBytes() {
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
			assertTrue(Arrays.equals(bBytes, cBytes));
			assertTrue(Arrays.equals(bBytes, bytes));
			assertTrue(Arrays.equals(bytes, cBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReadReadBytes() {
		int length = 999;
		byte[] bytes = new byte[length];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)(Math.random());
		}		
		InputStream a = new ByteArrayInputStream(bytes);
		testReadReadBytes(a, bytes, bytes.length);
		testReadReadBytes(new PositionInputStream(new ByteArrayInputStream(bytes)), bytes, bytes.length);
	}

	/*
	 * Tests read() and read(byte[], ...).
	 */
	public void testReadReadBytes(InputStream a, byte[] bytes, int length) {
		try {
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(a, length);
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

				/* At EOS */
				assertTrue(b.read() < 0);
				assertTrue(b.read() < 0);

				assertTrue(Arrays.equals(bytes, bBytes));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/*
	 * Order of getInputStream doesn't matter.
	 */
	public void testReadOrder() {
		try {
			int length = 999;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);

			InputStream a = inputStreamBuffer.getInputStream();
			InputStream b = inputStreamBuffer.getInputStream();
			byte[] aBytes = new byte[99];
			byte[] bBytes = new byte[99];
			int aBytesRead = a.read(aBytes);
			int bBytesRead = b.read(bBytes);
			assertEquals(aBytesRead, bBytesRead);
			assertEquals(aBytesRead, aBytes.length);
			assertTrue(Arrays.equals(aBytes, bBytes));

			InputStream c = inputStreamBuffer.getInputStream();
			byte[] cBytes = new byte[99];
			int cBytesRead = c.read(aBytes);
			InputStream d = inputStreamBuffer.getInputStream();
			byte[] dBytes = new byte[99];
			int dBytesRead = d.read(aBytes);

			assertEquals(cBytesRead, dBytesRead);
			assertEquals(cBytesRead, cBytes.length);
			assertTrue(Arrays.equals(cBytes, dBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSkip() {
		int testCount = 999;
		for (int i = 0; i < testCount; i++) {
			testSkip(i);
		}
	}

	public void testPrefix() {
		int testCount = 999;
		for (int i = 0; i < testCount; i++) {
			testPrefix(i);
		}
	}

	/*
	 * Tests relative start of resulting inputstream.
	 */
	private void testPrefix(int testIndex) {
		try {
			int length = 999;
			assertTrue(length > 2);
			byte[] bytes = new byte[length];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte)(Math.random());
			}
			InputStream a = new ByteArrayInputStream(bytes);

			int prefixLength = (int)(1 + Math.abs(Math.random() * (length - 2)));
			assertTrue(prefixLength > 0);
			assertTrue(prefixLength < length);
			byte[] prefix = new byte[prefixLength];

			int prefixBytesRead = a.read(prefix);
			assertTrue(prefixBytesRead > 0);
			assertTrue(prefixBytesRead < length);

			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(a, bytes.length - prefixLength);
			DataInputStream b = new DataInputStream(inputStreamBuffer.getInputStream());
			byte[] bBytes = new byte[length - prefixBytesRead];
			b.readFully(bBytes);

			/* Really at EOS */
			assertTrue(b.read() < 0);
			assertTrue(b.read() < 0);
			assertTrue(b.read() < 0);

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
	public void testSkip(int testIndex) {
		try {
			int length = 999;
			assertTrue(length > 0);
			int bytesToSkip = (int)(1 + Math.abs(Math.random() * (length - 1)));
			assertTrue(bytesToSkip > 0);
			int bytesToReadBeforeSkip = (int)(Math.abs(Math.random() * (length - bytesToSkip - 1)));
			assertTrue(bytesToReadBeforeSkip >= 0);
			assertTrue(0 <= (bytesToReadBeforeSkip + bytesToSkip));
			assertTrue((bytesToReadBeforeSkip + bytesToSkip) <= length);
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
			assertTrue(b.read() < 0);

			byte[] originalPrefix = new byte[bytesReadBeforeSkip];
			System.arraycopy(bytes, 0, originalPrefix, 0, originalPrefix.length);
			assertTrue(Arrays.equals(prefix,  originalPrefix));
			byte[] originalPostfix = new byte[bytesReadAfterSkip];
			System.arraycopy(bytes, bytesToReadBeforeSkip + bytesToSkip, originalPostfix, 0, originalPostfix.length);
			assertTrue(Arrays.equals(postfix,  originalPostfix));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/*
	 * Read prefix, skip, read postfix. Tests getPosition() versus getBytesBuffered().
	 */
	public void testReadSkipRead() {
		try {
			byte[] bytes = getRandomBytes(999);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, bytes.length);
			assertEquals(inputStreamBuffer.getPosition(), 0);
			InputStream a = inputStreamBuffer.getInputStream();
			InputStream b = inputStreamBuffer.getInputStream();
			byte[] aPrefix = new byte[99];
			int aPrefixBytesRead = a.read(aPrefix);
			assertEquals(inputStreamBuffer.getPosition(), aPrefixBytesRead);
			int aBytesSkipped = (int)a.skip(42);
			assertEquals(aBytesSkipped, 42);
			byte[] aPostfix = new byte[99];
			int aPostfixBytesRead = a.read(aPostfix);
			assertEquals(inputStreamBuffer.getPosition(), aPrefixBytesRead + aBytesSkipped + aPostfixBytesRead);
			int bytesBuffered = inputStreamBuffer.getBytesBuffered();
			assertTrue(aPrefixBytesRead + aPostfixBytesRead <= bytesBuffered);
			assertTrue(bytesBuffered <= aPrefixBytesRead + aBytesSkipped + aPostfixBytesRead);

			assertTrue(inputStreamBuffer.getBytesBuffered() <= inputStreamBuffer.getPosition());

			b.skip(aPrefixBytesRead);
			byte[] bBytes = new byte[aBytesSkipped];
			int bBytesRead = b.read(bBytes);
			assertEquals(bBytesRead, aBytesSkipped);
			assertEquals(inputStreamBuffer.getBytesBuffered(), inputStreamBuffer.getPosition());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSkipCopy() {
		try {
			int length = 40;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			InputStream a = inputStreamBuffer.getInputStream();
			assertNotNull(a);
			byte[] prefix = new byte[12];
			byte[] postfix = new byte[11];
			a.read(prefix);
			a.skip(length - prefix.length - postfix.length);
			a.read(postfix);
			byte[] aBytes = new byte[prefix.length + postfix.length];
			System.arraycopy(prefix, 0, aBytes, 0, prefix.length);
			System.arraycopy(postfix, 0, aBytes, prefix.length, postfix.length);

			assertEquals(inputStreamBuffer.getBytesBuffered(), prefix.length + postfix.length);

			InputStream b = inputStreamBuffer.getInputStream();
			assertNotNull(b);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			for (int i = 0; i < prefix.length; i++) {
				outputStream.write(b.read());
			}
			b.skip(length - prefix.length - postfix.length);
			for (int i = 0; i < postfix.length; i++) {
				outputStream.write(b.read());
			}

			/* Both a and b at EOS */
			assertTrue(a.read() < 0);
			assertTrue(b.read() < 0);
			assertTrue(Arrays.equals(aBytes, outputStream.toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSkipEOS() {
		try {
			int length = 30;
			int bytesToRead = 20;
			int bytesToSkip = 40;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);

			InputStream a = inputStreamBuffer.getInputStream();
			for (int i = 0; i < bytesToRead; i++) {
				a.read();
			}
			long bytesSkipped = a.skip(bytesToSkip);
			assertEquals(bytesSkipped, length - bytesToRead);
			assertTrue(inputStreamBuffer.getBytesBuffered() >= bytesToRead);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/*
	 * Tests if skip really skips (and doesn't secretly depend on read and thus buffers bytes).
	 * (As is the case in default InputStream.skip().)
	 */
	public void testStrictSkip() {
		try {
			int length = 30;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream carrier = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(carrier, length);
			InputStream a = inputStreamBuffer.getInputStream();
			int bytesSkipped = (int)a.skip(13);
			assertEquals(bytesSkipped, 13);
			int v = a.read();
			assertEquals(v, bytes[13] & 0xFF);
			if (carrier.markSupported()) {
				assertTrue(inputStreamBuffer.getBytesBuffered() < inputStreamBuffer.getPosition());
				assertEquals(inputStreamBuffer.getPosition() - inputStreamBuffer.getBytesBuffered(), bytesSkipped);
			} else {
				assertTrue(inputStreamBuffer.getBytesBuffered() <= inputStreamBuffer.getPosition());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testMoreStrictSkip() {
		try {
			int length = 30;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream carrier = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(carrier, length);
			InputStream b = inputStreamBuffer.getInputStream();
			int pos = 0;
			b.read(); pos++; /* 1 */
			pos += b.skip(5); /* +5 = 6 */
			pos += b.skip(2); /* +2 = 8 */
			pos += b.skip(5); /* +5 = 13 */
			b.read(); pos++; /* +1 = 14 */
			pos += b.skip(12); /* +12 = 26 */
			int v = b.read(); pos++;
			assertEquals(pos, inputStreamBuffer.getPosition());
			if (carrier.markSupported()) {
				assertTrue(inputStreamBuffer.getBytesBuffered() < inputStreamBuffer.getPosition());
				assertEquals(inputStreamBuffer.getPosition() - inputStreamBuffer.getBytesBuffered(), 5 + 2 + 5 + 12);
			} else {
				assertTrue(inputStreamBuffer.getBytesBuffered() <= inputStreamBuffer.getPosition());				
			}
			assertEquals(v, bytes[pos - 1] & 0xFF);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSkipSkipSkip() {
		try {
			int length = 999;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			InputStream a = inputStreamBuffer.getInputStream();
			a.skip(23);
			a.skip(23);
			byte[] aBytes = new byte[99];
			a.read(aBytes);
			InputStream b = inputStreamBuffer.getInputStream();
			b.skip(46);
			byte[] bBytes = new byte[99];
			b.read(bBytes);
			assertTrue(Arrays.equals(aBytes, bBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testMarkReset() {
		try {
			int length = 999;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			InputStream a = inputStreamBuffer.getInputStream();
			byte[] aBytes = new byte[99];
			int aBytesRead = a.read(aBytes); /* First block of 99 bytes */
			assertEquals(aBytesRead, aBytes.length);
			aBytesRead = a.read(aBytes); /* Again, so second block of 99 bytes */
			assertEquals(aBytesRead, aBytes.length);
			byte[] aaBytes = new byte[aBytes.length];
			a.reset();
			a.skip(10);
			a.skip(89); /* Skipped 99 bytes */
			int aaBytesRead = a.read(aaBytes);
			assertEquals(aaBytesRead, aBytesRead);
			assertTrue(Arrays.equals(aBytes, aaBytes));
			a.reset();
			for (int i = 0; i < aBytes.length; i++) { /* Read first 99 bytes */
				a.read();
			}
			byte[] aaaBytes = new byte[aBytes.length];
			int aaaBytesRead = a.read(aaaBytes);
			assertEquals(aaaBytesRead, aBytesRead);
			assertTrue(Arrays.equals(aBytes, aaaBytes));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSkipReadDifferentStream() {
		try {
			int length = 99;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			InputStream a = inputStreamBuffer.getInputStream();
			a.skip(80);

			InputStream b = inputStreamBuffer.getInputStream();
			ByteArrayOutputStream bOutputStream = new ByteArrayOutputStream();
			for (int i = 0; i < 85; i++) {
				bOutputStream.write(b.read()); /* Using single byte read here. */
			}

			InputStream c = inputStreamBuffer.getInputStream();
			byte[] cBytes = new byte[85];
			DataInputStream cDataInputStream = new DataInputStream(c);
			cDataInputStream.readFully(cBytes);

			assertTrue(Arrays.equals(cBytes, bOutputStream.toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testPeekLast() {
		try {
			int length = 24;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			InputStream a = inputStreamBuffer.getInputStream();
			
			// a.mark(bytes.length);
			int indexLast = bytes.length - 1;
			a.skip(indexLast);
			int v = a.read();

			InputStream b = inputStreamBuffer.getInputStream();
			byte[] bBytes = new byte[bytes.length];
			int bBytesRead = b.read(bBytes);

			assertEquals(bBytesRead, bBytes.length);
			
			assertTrue(Arrays.equals(bBytes, bytes));
			assertEquals(bBytes[indexLast] & 0xFF, v);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReadBig() {
		try {
			int length = 823;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			InputStream a = inputStreamBuffer.getInputStream();
			byte[] block = new byte[1024];
			int bytesRead = a.read(block, 0, block.length);
			assertEquals(bytesRead, length);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReadMinusOnOnEOS() {
		try {
			int length = 823;
			byte[] bytes = getRandomBytes(length);
			assertNotNull(bytes);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, length);
			InputStream a = inputStreamBuffer.getInputStream();
			byte[] block = new byte[1024];
			int bytesRead = a.read(block, 0, block.length);
			assertEquals(bytesRead, length);
			assertTrue(a.read() < 0); /* At EOS */
			bytesRead = a.read(block, 0, block.length);
			assertEquals(bytesRead, -1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void readImage() {
		try {
			int width = 320, height = 200;
			byte[] imageBytes = createTrivialJPEGBytes(width, height);
			InputStream inputStream = new ByteArrayInputStream(imageBytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, imageBytes.length);
			InputStream a = inputStreamBuffer.getInputStream();
			BufferedImage image = ImageIO.read(a);
			assertEquals(image.getWidth(), width);
			assertEquals(image.getHeight(), height);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testReadSkipAvailable() {
		try {
			byte[] bytes = getRandomBytes(30);
			InputStream inputStream = new ByteArrayInputStream(bytes);
			InputStreamBuffer inputStreamBuffer = new InputStreamBuffer(inputStream, bytes.length);
			InputStream a = inputStreamBuffer.getInputStream();
			assertEquals(a.available(), 0);
			a.skip(20);
			assertEquals(a.available(), 0);

			a.read();
			a.read();
			
			InputStream aa = inputStreamBuffer.getInputStream();
			byte[] someBytes = new byte[20];
			aa.read(someBytes);
						
			InputStream b = inputStreamBuffer.getInputStream();
			assertEquals(b.available(), someBytes.length + 2);

			byte[] lastBytes = new byte[bytes.length - someBytes.length - 2];
			a.read(lastBytes);

			assertEquals(b.available(), someBytes.length + 2 + lastBytes.length);

			InputStream c = inputStreamBuffer.getInputStream();
			assertEquals(c.available(), someBytes.length + 2 + lastBytes.length);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private static byte[] getRandomBytes(int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)(Math.abs(Math.random() * 256));
		}
		return bytes;
	}

	private static byte[] createTrivialJPEGBytes(int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", out);
			out.flush();
			byte[] bytes = out.toByteArray();
			return bytes;
		} catch (Exception e) {
			fail(e.toString());
			return null;
		}
	}
}
