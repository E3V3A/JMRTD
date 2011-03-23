package org.jmrtd.cert;

public class PKDMasterListCertStoreParameters extends PKDCertStoreParameters {
	
	private static final String DEFAULT_BASE_DN = "dc=CSCAMasterList,dc=pkdDownload";
	
	public PKDMasterListCertStoreParameters() {
		super();
	}

	public PKDMasterListCertStoreParameters(String serverName) {
		this(serverName, DEFAULT_BASE_DN);
	}

	public PKDMasterListCertStoreParameters(String serverName, String baseDN) {
		super(serverName, baseDN);
	}

	public PKDMasterListCertStoreParameters(String serverName, int port) {
		this(serverName, port, DEFAULT_BASE_DN);
	}

	public PKDMasterListCertStoreParameters(String serverName, int port, String baseDN) {
		super(serverName, port, baseDN);
	}
}
