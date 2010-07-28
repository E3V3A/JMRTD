package org.jmrtd.cert;

import java.security.cert.CertStoreParameters;

public class PKDCertStoreParameters implements Cloneable, CertStoreParameters
{
	private static final String DEFAULT_SERVER_NAME = "localhost";
	private static final int DEFAULT_PORT = 389;
	private static final String DEFAULT_BASE_DN = "dc=data,dc=pkdDownload";

	private String serverName;
	private int port;
	private String baseDN;

	public PKDCertStoreParameters() {
		this(DEFAULT_SERVER_NAME, DEFAULT_PORT, DEFAULT_BASE_DN);
	}

	public PKDCertStoreParameters(String serverName) {
		this(serverName, DEFAULT_PORT, DEFAULT_BASE_DN);
	}
	
	public PKDCertStoreParameters(String serverName, String baseDN) {
		this(serverName, DEFAULT_PORT, baseDN);
	}

	public PKDCertStoreParameters(String serverName, int port) {
		this(serverName, port, DEFAULT_BASE_DN);
	}

	public PKDCertStoreParameters(String serverName, int port, String baseDN) {
		this.serverName = serverName;
		this.port = port;
		this.baseDN = baseDN;
	}

	/**
	 * @return the serverName
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return the baseDN
	 */
	public String getBaseDN() {
		return baseDN;
	}

	public Object clone() {
		return this;
	}
	
	public String toString() {
		return "PKDCertStoreParameters [" + serverName + ":" + port + "/" + baseDN + "]";
	}
	
	public boolean equals(Object otherObj) {
		if (otherObj == null) { return false; }
		if (otherObj == this) { return true; }
		if (!this.getClass().equals(otherObj.getClass())) { return false; }
		PKDCertStoreParameters otherParams = (PKDCertStoreParameters)otherObj;
		return otherParams.serverName.equals(this.serverName)
			&& otherParams.port == this.port
			&& otherParams.baseDN.equals(this.baseDN);
	}
	
	public int hashCode() {
		return (serverName.hashCode() + port + baseDN.hashCode()) * 2 + 303;
	}
}
