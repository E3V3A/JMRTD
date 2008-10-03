package sos.mrtd;

import java.io.IOException;
import java.io.InputStream;

public class DG3File extends CBEFFDataGroup
{
	public byte[] getEncoded() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTag() {
		return EF_DG3_TAG;
	}

	public String toString() {
		return "DG3File";
	}

	protected void readBiometricData(InputStream in, int length)
			throws IOException {
		// TODO Auto-generated method stub
	}
}
