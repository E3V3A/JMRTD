package org.jmrtd.io.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.io.SplittableInputStream;

public class SplittableInputStreamTest extends TestCase {

	public void testSkip() {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[15417]);
			SplittableInputStream splittableInputStream = new SplittableInputStream(inputStream, 15417);

			/* Header */
			splittableInputStream.read(new byte[80]);

			/* Shouldn't matter */
			splittableInputStream.getInputStream(0);

			assertTrue(splittableInputStream.getPosition() <= splittableInputStream.getLength());

			/* Skip over the picture */
			System.out.println("DEBUG: pos " + splittableInputStream.getPosition() + "/" + splittableInputStream.getLength());

			splittableInputStream.skip(15337);
			System.out.println("DEBUG: pos " + splittableInputStream.getPosition() + "/" + splittableInputStream.getLength());

			InputStream otherInputStream = splittableInputStream.getInputStream(80);
			DataInputStream dataInputStream = new DataInputStream(otherInputStream);
			dataInputStream.readFully(new byte[15337]);
			System.out.println("DEBUG: pos " + splittableInputStream.getPosition() + "/" + splittableInputStream.getLength());

			assertTrue(splittableInputStream.getPosition() <= splittableInputStream.getLength());


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testSkip2() {
		try {
			byte[] src = new byte[4 + 80 + 17690 + 80 + 15337];
			for (int i = 0; i < src.length; i++) { src[i] = (byte)(i % 356 & 0xFF); }
			ByteArrayInputStream inputStream = new ByteArrayInputStream(src);
			inputStream.skip(4); /* Skip over tag + length */
			SplittableInputStream splittableInputStream = new SplittableInputStream(inputStream, 80 + 17690 + 80 + 15337);

			/* Header */
			byte[] header1 = new byte[80];
			splittableInputStream.read(header1);
			System.out.println("DEBUG: header1 = " + Hex.bytesToHexString(header1));

			/* Shouldn't matter */
			splittableInputStream.getInputStream(0);

			assertTrue(splittableInputStream.getPosition() <= splittableInputStream.getLength());

			/* Skip over the picture */
			System.out.println("DEBUG: pos " + splittableInputStream.getPosition() + "/" + splittableInputStream.getLength());

			splittableInputStream.skip(17690);
			System.out.println("DEBUG: pos " + splittableInputStream.getPosition() + "/" + splittableInputStream.getLength());

			InputStream otherInputStream = splittableInputStream.getInputStream(80);
			DataInputStream dataInputStream = new DataInputStream(otherInputStream);

			byte[] imgBytes = new byte[17690];
			dataInputStream.readFully(imgBytes);



		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSkipMarkNotSupported() {
		try {
			byte[] src = new byte[4 + 80 + 17690 + 80 + 15337];
			for (int i = 0; i < src.length; i++) { src[i] = (byte)(i % 356 & 0xFF); }
			NonMarkSupportingByteArrayInputStream inputStream = new NonMarkSupportingByteArrayInputStream(src);
			inputStream.skip(4); /* Skip over tag + length */
			SplittableInputStream splittableInputStream = new SplittableInputStream(inputStream, 80 + 17690 + 80 + 15337);

			/* Header */
			byte[] header1 = new byte[80];
			splittableInputStream.read(header1);
			System.out.println("DEBUG: header1 = " + Hex.bytesToHexString(header1));

			/* Shouldn't matter */
			splittableInputStream.getInputStream(0);

			assertTrue(splittableInputStream.getPosition() <= splittableInputStream.getLength());

			/* Skip over the picture */
			System.out.println("DEBUG: pos " + splittableInputStream.getPosition() + "/" + splittableInputStream.getLength());

			splittableInputStream.skip(17690);
			System.out.println("DEBUG: pos " + splittableInputStream.getPosition() + "/" + splittableInputStream.getLength());

			InputStream otherInputStream = splittableInputStream.getInputStream(80);
			DataInputStream dataInputStream = new DataInputStream(otherInputStream);

			byte[] imgBytes = new byte[17690];
			dataInputStream.readFully(imgBytes);			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public void testMarkReset() {
		try {
			byte[] src = new byte[4 + 80 + 17690 + 80 + 15337];
			for (int i = 0; i < src.length; i++) { src[i] = (byte)(i % 356 & 0xFF); }
			SplittableInputStream sIn = new SplittableInputStream(new ByteArrayInputStream(src), src.length);
			assertTrue(sIn.markSupported());
			sIn.read(); // 0
			sIn.read(); // 1
			sIn.read(); // 2
			int b = sIn.read(); // 3
			assertEquals(b, src[3]);
			sIn.mark(src.length - 4);
			b = sIn.read(); // 4
			assertEquals(b, src[4]);
			sIn.read(new byte[14000]); // 5 .. 14004
			b = sIn.read(); // 14005
			assertEquals(b, src[14005]);
			sIn.reset();
			b = sIn.read();
			assertEquals(b, src[4]);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testMarkReset2() {
		try {
			byte[] src = new byte[4 + 80 + 17690 + 80 + 15337];
			for (int i = 0; i < src.length; i++) { src[i] = (byte)(i % 356 & 0xFF); }
			SplittableInputStream sIn = new SplittableInputStream(new ByteArrayInputStream(src), src.length);
			assertTrue(sIn.markSupported());
			sIn.read(); // 0
			sIn.read(); // 1
			sIn.read(); // 2
			int b = sIn.read(); // 3
			assertEquals(b, src[3]);
			sIn.skip(15000);
			sIn.read();
			
			InputStream sIn2 = sIn.getInputStream(0);
			
			b = sIn2.read(); // 0
			System.out.println("DEBUG: b = " + b);
			b = sIn2.read(); // 1
			System.out.println("DEBUG: b = " + b);
			b = sIn2.read(); // 2
			System.out.println("DEBUG: b = " + b);
			b = sIn2.read(); // 3
			System.out.println("DEBUG: b = " + b);
			assertEquals(b, src[3]);

			sIn2.mark(src.length - 4);
			b = sIn2.read(); // 4
			assertEquals(b, src[4]);
			sIn2.read(new byte[14000]); // 5 .. 14004
			b = sIn2.read(); // 14005
			assertEquals(b, src[14005]);
			sIn2.reset();
			b = sIn2.read();
			assertEquals(b, src[4]);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	private class NonMarkSupportingByteArrayInputStream extends ByteArrayInputStream {

		public NonMarkSupportingByteArrayInputStream(byte[] buf) {
			super(buf);
		}

		public boolean markSupported() {
			return false;
		}
	}
}
