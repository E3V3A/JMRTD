package org.jmrtd.io.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

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

}
