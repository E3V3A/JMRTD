/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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
 */

package org.jmrtd;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CertificateParser;

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

	private static final String DEFAULT_CVCA_DIR = "/home/sos/woj/terminals";

	private File dir;
	
	private final Map<String, List<CVCertificate>> certificateListsMap;
	private final Map<String, PrivateKey> keysMap;

	public CVCAStore() {
		this(getDefaultCVCADir());
	}
	
	public CVCAStore(File dir) {
		certificateListsMap = new HashMap<String, List<CVCertificate>>();
		keysMap = new HashMap<String, PrivateKey>();
		setLocation(dir);
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

	public void setLocation(File dir) {
		this.dir = dir;
		if (!dir.isDirectory()) {
			String message = "File " + dir.getAbsolutePath() + " is not a directory.";
			System.err.println("WARNING: " + message);
			// throw new IllegalArgumentException(message);
			return;
		}
		File[] dirs = dir.listFiles(DIRECTORY_FILE_FILTER);
		try {
			for (File f : dirs) { scanOneDirectory(f); }
		} catch (Exception e) {
			e.printStackTrace();
			// throw new IOException();
		}
	}
	
	private void scanOneDirectory(File dir) throws IOException {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("File " + dir.getAbsolutePath() + " is not a directory.");
		}
		File[] certFiles = dir.listFiles(TERMINAL_CERT_FILE_FILTER);
		File[] keyFiles = dir.listFiles(TERMINAL_KEY_FILE_FILTER);
		certFiles = sortFiles(certFiles);
		List<CVCertificate> terminalCertificates = new ArrayList<CVCertificate>();
		String keyAlgName = "RSA";
		for (File file : certFiles) {
			System.out.println("Found certificate file: "+file);
			CVCertificate c = readCVCertificateFromFile(file);
			if (c == null) { throw new IOException(); }
			terminalCertificates.add(c);
			try {
				keyAlgName = c.getCertificateBody().getPublicKey().getAlgorithm();
			} catch(NoSuchFieldException nsfe) {
				/* FIXME: why silent? */
			}
		}
		if (keyFiles.length != 1) { throw new IOException(); }
		System.out.println("Found key file: " + keyFiles[0]);
		PrivateKey k = readKeyFromFile(keyFiles[0], keyAlgName);
		if (k == null) { throw new IOException(); }
		try {
			String ref = terminalCertificates.get(0).getCertificateBody().getAuthorityReference().getConcatenated();
			addEntry(ref, terminalCertificates, k);
		} catch (NoSuchFieldException e) {
			throw new IOException();
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
		byte[] dataBuffer = null;
		FileInputStream inStream = null;
		try {
			// Simple file loader...
			int length = (int) file.length();
			dataBuffer = new byte[length];
			inStream = new FileInputStream(file);

			int offset = 0;
			int readBytes = 0;
			boolean readMore = true;
			while (readMore) {
				readBytes = inStream.read(dataBuffer, offset, length - offset);
				offset += readBytes;
				readMore = readBytes > 0 && offset != length;
			}
		} finally {
			try {
				if (inStream != null)
					inStream.close();
			} catch (IOException e1) {
				System.out.println("loadFile - error when closing: " + e1);
			}
		}
		return dataBuffer;
	}

	private static CVCertificate readCVCertificateFromFile(File f) {
		try {
			byte[] data = loadFile(f);
			CVCObject parsedObject = CertificateParser.parseCertificate(data);
			CVCertificate c = (CVCertificate) parsedObject;
			return c;
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
	
	private static File getDefaultCVCADir() {
		return new File(DEFAULT_CVCA_DIR);
	}
}
