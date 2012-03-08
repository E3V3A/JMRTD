package org.jmrtd.cert;

/**
 * Parameters for PKD backed certificate store, selecting certificates provided
 * in CSCA master lists.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
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
