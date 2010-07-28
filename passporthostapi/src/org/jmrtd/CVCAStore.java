/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id: $
 */

package org.jmrtd;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jmrtd.cert.CVCertificate;

/**
 * Stores terminal CV certificate and keys.
 * 
 * TODO: make proper error handling
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 */
public class CVCAStore
{
	private static final FileFilter
	DIRECTORY_FILE_FILTER = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	},
	TERMINAL_CERT_FILE_FILTER = new FileFilter() {
		public boolean accept(File pathname) {
			return (pathname.isFile()
					&& pathname.getName().startsWith("terminalcert")
					&& pathname.getName().endsWith(".cvcert"));
		}
	},
	TERMINAL_KEY_FILE_FILTER = new FileFilter() {
		public boolean accept(File pathname) {
			return (pathname.isFile()
					&& pathname.getName().equals("terminalkey.der"));
		}
	};
	
	private static final Provider JMRTD_PROVIDER = new JMRTDSecurityProvider();

	private URI location;

	private final Map<String, List<CVCertificate>> certificateListsMap;
	private final Map<String, PrivateKey> keysMap;

	private Logger logger = Logger.getLogger("org.jmrtd");

	public CVCAStore(URI location) {
		certificateListsMap = new HashMap<String, List<CVCertificate>>();
		keysMap = new HashMap<String, PrivateKey>();
		setLocation(location);	
	}

	/**
	 * Assigns the given certificates and private key to the given alias.
	 * 
	 * @param alias an alias (not already added)
	 * @param certificates public key certificates to add
	 * @param privateKey private key to add
	 */
	public void addEntry(String alias, List<CVCertificate> certificates, PrivateKey privateKey) {
		if (alias == null) { throw new IllegalArgumentException("Alias should not be null"); }
		if (certificateListsMap.containsKey(alias)) { throw new IllegalArgumentException("Alias already present"); }
		if (certificates == null || privateKey == null || certificates.size() == 0) {
			throw new IllegalArgumentException();
		}
		certificateListsMap.put(alias, new ArrayList<CVCertificate>(certificates));
		keysMap.put(alias, privateKey);
	}

	public URI getLocation() {
		return location;
	}

	public void setLocation(URI location) {
		this.location = location;
		if (location.getScheme() != "file") {
//			logger.warning("CVCAStore can only be a directory");
			throw new IllegalArgumentException("CVCAStore \"" + location + "\" is not a directory.");
		}
		File directory = new File(location);
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("CVCAStore \"" + directory.getAbsolutePath() + "\" is not a directory.");
		}
		File[] dirs = directory.listFiles(DIRECTORY_FILE_FILTER);
		try {
			for (File f : dirs) { scanOneDirectory(f); }
		} catch (Exception e) {
			e.printStackTrace();
			// throw new IOException();
		}
	}

	private void scanOneDirectory(File dir) throws IOException {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("File " + dir.getCanonicalPath() + " is not a directory.");
		}
		File[] certFiles = dir.listFiles(TERMINAL_CERT_FILE_FILTER);
		File[] keyFiles = dir.listFiles(TERMINAL_KEY_FILE_FILTER);
		certFiles = sortFiles(certFiles);
		List<CVCertificate> terminalCertificates = new ArrayList<CVCertificate>();
		String keyAlgName = "RSA";
		for (File file : certFiles) {
			System.out.println("Found certificate file: " + file);
			CVCertificate cvCertificate = readCVCertificateFromFile(file);
			if (cvCertificate == null) { throw new IOException(); }
			terminalCertificates.add(cvCertificate);
			keyAlgName = cvCertificate.getPublicKey().getAlgorithm();
		}
		if (keyFiles.length != 1) { throw new IOException(); }
		System.out.println("Found key file: " + keyFiles[0]);
		PrivateKey k = readKeyFromFile(keyFiles[0], keyAlgName);
		if (k == null) { throw new IOException(); }
		try {
			String ref = terminalCertificates.get(0).getAuthorityReference().getName();
			addEntry(ref, terminalCertificates, k);
		} catch (CertificateException ce) {
			throw new IOException(ce.getMessage());
		}
	}

	/**
	 * Gets the certificates associated with the given alias.
	 * 
	 * @param alias the alias to search for
	 * 
	 * @return the certificates associated with the given alias
	 */
	public List<CVCertificate> getCertificates(String alias) {
		return certificateListsMap.get(alias);
	}

	/**
	 * Get the private key associated with the given alias.
	 * 
	 * @param alias the alias to search for
	 * 
	 * @return the private key associated with the given alias
	 */
	public PrivateKey getPrivateKey(String alias) {
		return keysMap.get(alias);
	}

	/**
	 * Lists all the aliases in this store.
	 * 
	 * @return an iterator to iterate through all the aliases in this store
	 */
	public Iterator<String> aliases() {
		return certificateListsMap.keySet().iterator();
	}

	/**
	 * Checks if the given alias exists in this store.
	 * 
	 * @param alias the alias to check for
	 * 
	 * @return true if the alias exists, false otherwise
	 */
	public boolean containsAlias(String alias) {
		return certificateListsMap.containsKey(alias);
	}

	private static File[] sortFiles(File[] files) {
		List<File> l = new ArrayList<File>();
		for(File f : files) { l.add(f); }
		Comparator<File> c = new Comparator<File>() {
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
		Collections.sort(l, c);
		l.toArray(files);
		return files;
	}

	private static byte[] loadFile(File file) throws IOException {
		DataInputStream in = null;
		byte[] result = new byte[(int)file.length()];
		in = new DataInputStream(new FileInputStream(file));
		in.readFully(result);
		in.close();
		return result;
	}

	private static CVCertificate readCVCertificateFromFile(File f) {
		try {
			CertificateFactory cf = CertificateFactory.getInstance("CVC", JMRTD_PROVIDER);
			return (CVCertificate)cf.generateCertificate(new FileInputStream(f));
		} catch (Exception e) {
			return null;
		}
	}

	private static PrivateKey readKeyFromFile(File f, String algName) {
		try {
			byte[] data = loadFile(f);
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
			KeyFactory gen = KeyFactory.getInstance(algName);
			return gen.generatePrivate(spec);
		} catch (Exception e) {
			return null;
		}
	}	
}
