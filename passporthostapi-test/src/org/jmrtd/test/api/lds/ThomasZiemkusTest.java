package org.jmrtd.test.api.lds;

import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.lds.SODFile;

public class ThomasZiemkusTest extends TestCase {

	private static final String SOD_FILE_LOCATION = "t:/paspoort/test/thomas/EF_SOD.BIN";

	public void testSODFile() {
		try {
			FileInputStream in = new FileInputStream(SOD_FILE_LOCATION);
			SODFile sodFile = new SODFile(in);
			
			
			byte[] eContent = sodFile.getEContent();
			System.out.println("EContent");
			System.out.println(Hex.bytesToPrettyString(eContent));
			
			byte[] encryptedDigest = sodFile.getEncryptedDigest();
			System.out.println("Encrypted Digest");
			System.out.println(Hex.bytesToPrettyString(encryptedDigest));
		} catch (IOException ioe) {
			ioe.printStackTrace();
			fail(ioe.getMessage());
		}
	}

}
