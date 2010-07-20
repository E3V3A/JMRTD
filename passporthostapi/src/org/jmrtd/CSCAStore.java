package org.jmrtd;

import java.security.KeyStoreException;
import java.security.cert.Certificate;

public abstract class CSCAStore {
	
	public abstract String getLocation();
	
	public abstract void setLocation(String location);
	
	public abstract Certificate getCertificate(Certificate issuerCertificate) throws KeyStoreException;
}
