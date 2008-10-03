package sos.mrtd;

import java.io.IOException;
import java.io.InputStream;

public class DG4File extends CBEFFDataGroup
{
	public byte[] getEncoded() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTag() {
		return EF_DG4_TAG;
	}

	protected void readBiometricData(InputStream in, int length)
	throws IOException {
	}

	public String toString() {
		return "DG4File";
	}


}
