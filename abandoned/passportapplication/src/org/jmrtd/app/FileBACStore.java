/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
 * $Id$
 */

package org.jmrtd.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.app.util.FileUtil;

/**
 * Flat file based database for BAC entries.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
public class FileBACStore implements MutableBACStore {

	private static final File
		JMRTD_USER_DIR = FileUtil.getApplicationDataDir("jmrtd"),
		DEFAULT_BACDB_FILE = new File(JMRTD_USER_DIR, "bacdb.txt");

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");
	
	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private File location;

	private List<BACKeySpec> entries;

	/**
	 * Constructs a BAC store using a default file location.
	 */
	public FileBACStore() {
		this(DEFAULT_BACDB_FILE);
	}

	/**
	 * Constructs a BAC store using the given file location.
	 * 
	 * @param location a file
	 */
	public FileBACStore(File location) {
		setLocation(location);
		entries = new ArrayList<BACKeySpec>();
		read(location, entries);
	}

	/**
	 * Constructs a BAC store using the given file location.
	 * 
	 * @param location a URL for a file
	 */
	public FileBACStore(URL location) {
		this(FileUtil.toFile(location));
	}

	/**
	 * Gets the file that stores the BAC store.
	 * 
	 * @return a file
	 */
	public synchronized File getLocation() {
		return location;
	}

	/**
	 * Sets the file location for the BAC store.
	 * 
	 * @param location a file
	 */
	public synchronized void setLocation(File location) {
		this.location = location;
		try {
			if (!location.exists()) {
				File parent = location.getParentFile();
				if (!parent.isDirectory()) { 
					if (!parent.mkdirs()) {
						LOGGER.warning("Could not create directory for bacDBFile \"" + parent.getCanonicalPath() + "\"");
					}
				}
				if (!location.createNewFile()) {
					LOGGER.warning("Could not create bacDBFile \"" + location.getCanonicalPath() + "\"");
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Gets the BAC key entries.
	 * 
	 * @return a list of BAC key entries
	 */
	public List<BACKeySpec> getEntries() {
		return entries;
	}

	/**
	 * Adds an entry to this BAC store.
	 * 
	 * @param entry the BAC key entry to add
	 */
	public synchronized void addEntry(BACKeySpec entry) {
		if (!entries.contains(entry)) {
			entries.add(entry);
			write(entries, location);
		} else {
			entries.remove(entry);
			entries.add(entry);
			write(entries, location);
		}
	}

	/**
	 * Adds an entry to this BAC store at a specific index.
	 * 
	 * @param index the index
	 * @param entry the BAC key entry to add
	 */
	public synchronized  void addEntry(int index, BACKeySpec entry) {
		entries.add(index, entry);
		write(entries, location);
	}

	/**
	 * Removes an entry from the BAC store.
	 * 
	 * @param index the index of the BAC key entry to remove
	 */
	public synchronized void removeEntry(int index) {
		entries.remove(index);
		write(entries, location);
	}

	/**
	 * Gets an entry from this BAC store.
	 * 
	 * @param index the index of the entry to get
	 * 
	 * @return a BAC key entry
	 */
	public BACKeySpec getEntry(int index) {
		return entries.get(index);
	}
	
	/**
	 * Gets a textual representation of this BAC store.
	 * 
	 * @return a textual representation of this BAC store
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (BACKeySpec entry: entries) {
			result.append(entry.toString());
			result.append('\n');
		}
		return result.toString();
	}
	
	/* ONLY PRIVATE METHODS BELOW */
	
	private static String[] getFields(String entry) {
		StringTokenizer st = new StringTokenizer(entry.trim(), ",");
		int tokenCount = st.countTokens();
		String[] result = new String[tokenCount];
		for (int i = 0; i < tokenCount; i++) {
			result[i] = st.nextToken().trim();
		}
		return result;
	}

	private static void read(File location, List<BACKeySpec> entries) {
		try {
			BufferedReader d = new BufferedReader(new FileReader(location));
			while (true) {
				String line = d.readLine();
				if (line == null) { break; }
				String[] fields = getFields(line);
				entries.add(new BACKey(fields[0], SDF.parse(fields[1]), SDF.parse(fields[2])));
			}
			d.close();
		} catch (FileNotFoundException fnfe) {
			/* NOTE: no problem... */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void write(List<BACKeySpec> entries, File file) {
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(file));
			for (BACKeySpec entry: entries) { writer.println(entry); }
			writer.flush();
			writer.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
