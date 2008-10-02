package sos.mrtd;

import java.io.IOException;

import sos.tlv.BERTLVInputStream;

public class DG4File extends CBEFFDataGroup
{
	public byte[] getEncoded() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTag() {
		return EF_DG4_TAG;
	}

	protected void readBioData(BERTLVInputStream tlvIn, int valueLength)
			throws IOException {
		// TODO Auto-generated method stub

	}

	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}
}
