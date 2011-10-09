package org.jmrtd.lds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

abstract class AbstractInfo implements LDSInfo {

	public byte[] getEncoded() {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			writeObject(outputStream);
			outputStream.flush();
			return outputStream.toByteArray();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	abstract void writeObject(OutputStream outputStream) throws IOException;
}
