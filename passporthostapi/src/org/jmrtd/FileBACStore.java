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
 * $Id$
 */

package org.jmrtd;

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

import net.sourceforge.scuba.util.Files;

/**
 * Flat file based database for BAC entries.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)  *
 * @version $Rev$
 */
public class FileBACStore implements BACStore
{
	private static final File
		JMRTD_USER_DIR = Files.getApplicationDataDir("jmrtd"),
		DEFAULT_BACDB_FILE = new File(JMRTD_USER_DIR, "bacdb.txt");

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");
	
	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private File location;

	private List<BACKeySpec> entries;

	public FileBACStore() {
		this(DEFAULT_BACDB_FILE);
	}

	public FileBACStore(File location) {
		setLocation(location);
		entries = new ArrayList<BACKeySpec>();
		read(location, entries);
	}

	public FileBACStore(URL location) {
		this(Files.toFile(location));
	}

	public File getLocation() {
		return location;
	}

	public void setLocation(File location) {
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

	public List<BACKeySpec> getEntries() {
		return entries;
	}

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

	public synchronized  void addEntry(int i, BACKeySpec entry) {
		entries.add(i, entry);
		write(entries, location);
	}

	private static String[] getFields(String entry) {
		StringTokenizer st = new StringTokenizer(entry.trim(), ",");
		int tokenCount = st.countTokens();
		String[] result = new String[tokenCount];
		for (int i = 0; i < tokenCount; i++) {
			result[i] = st.nextToken().trim();
		}
		return result;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		for (BACKeySpec entry: entries) {
			result.append(entry.toString());
			result.append('\n');
		}
		return result.toString();
	}

	public synchronized void removeEntry(int index) {
		entries.remove(index);
		write(entries, location);
	}

	public BACKeySpec getEntry(int entryRowIndex) {
		return entries.get(entryRowIndex);
	}
	
	private static void read(File location, List<BACKeySpec> entries) {
		try {
			BufferedReader d = new BufferedReader(new FileReader(location));
			while (true) {
				String line = d.readLine();
				if (line == null) { break; }
				String[] fields = getFields(line);
				entries.add(new BACKeySpec(fields[0], SDF.parse(fields[1]), SDF.parse(fields[2])));
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