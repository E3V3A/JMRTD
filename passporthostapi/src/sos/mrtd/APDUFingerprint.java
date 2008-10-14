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
 * $Id: AuthListener.java 562 2008-10-07 14:27:33Z martijno $
 */

package sos.mrtd;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import sos.smartcards.CardServiceException;
import sos.smartcards.ISO7816;
import sos.util.Files;
import sos.util.Hex;

/**
 * Fingerprint data structure. Basically maps commands to response codes (status words).
 * Under construction. Reads files in <code>basedir/fingerprintfiles</code>
 * filestructures of Henning.
 * 
 * TODO: We should think of some other fileformat: Henning's format is based on
 * <code>toString()</code> of APDU classes, not under our control.
 *
 * @author Henning Richter (hrichter@fh-lausitz.de)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class APDUFingerprint
{
	private static final FilenameFilter HENNING_FILE_FILENAME_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith("txt") || name.endsWith("TXT");
		}			
	};

	private static final CommandAPDU[] FINGERPRINT_COMMANDS = 
	{
		new CommandAPDU((byte)0x00, (byte)0x44, (byte)0x00, (byte)0x00, new byte[0], 0x00),
		new CommandAPDU((byte)0x00, (byte)0x82, (byte)0x00, (byte)0x00, new byte[0], 0x00),
		new CommandAPDU((byte)0x00, (byte)0x84, (byte)0x00, (byte)0x00, new byte[0], 0x00),
		new CommandAPDU((byte)0x00, (byte)0x88, (byte)0x00, (byte)0x00, new byte[0], 0x00),
		new CommandAPDU((byte)0x00, (byte)0xA4, (byte)0x00, (byte)0x00, new byte[0], 0x00),
		new CommandAPDU((byte)0x00, (byte)0xB0, (byte)0x00, (byte)0x00, new byte[0], 0x00),
		new CommandAPDU((byte)0x00, (byte)0xB1, (byte)0x00, (byte)0x00, new byte[0], 0x00)
	};
	/** Country fingerprints are read from file during static init. */
	private static final Map<APDUFingerprint, String> COUNTRY_FINGERPRINTS = new HashMap<APDUFingerprint, String>();
	static {
		/* TODO: the fileformat needs some cleaning up!
		 * Make filenames follow ISO country codes? (Or use field within file to define country)
		 */
		try {
			File baseDir = Files.getBaseDirAsFile();
			File countryFingerprintsDir = new File(baseDir, "fingerprintfiles");
			File[] fingerPrintFiles = countryFingerprintsDir.listFiles(HENNING_FILE_FILENAME_FILTER);
			if (fingerPrintFiles == null) {
				System.out.println("DEBUG: cannot open country fingerprint dir: " + countryFingerprintsDir);
			}
			String fileName = "";
			for (File fingerPrintFile: fingerPrintFiles) {
				try {
					fileName = fingerPrintFile.getName();
					APDUFingerprint fingerprint = getFromHenningFile(fingerPrintFile);
					COUNTRY_FINGERPRINTS.put(fingerprint, fileName);
				} catch (Exception e) {
					System.out.println("WARNING: ignoring Henning-file \"" + fileName + "\"");
					continue; /* Try next file. */
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	};

	/** Maps commands (headers) to responses (status words). */
	private Map<CommandAPDU, Short> commandResponsePairs;

	/**
	 * Constructs an empty fingerprint.
	 */
	public APDUFingerprint() {
		commandResponsePairs = new HashMap<CommandAPDU, Short>();
	}

	public APDUFingerprint(PassportService service) {
		this();
		try {
			if (service.isOpen()) { service.close(); }
			service.open();
			for (CommandAPDU capdu: FINGERPRINT_COMMANDS) {
				ResponseAPDU rapdu = service.transmit(capdu);
				if (rapdu.getSW() != ISO7816.SW_NO_ERROR) {
					put(capdu, rapdu);
				}
			}
		} catch (CardServiceException cse) {
			cse.printStackTrace();
		}
	}

	public void put(CommandAPDU capdu, ResponseAPDU rapdu) {
		put(capdu.getCLA(), capdu.getINS(), capdu.getP1(), capdu.getP2(), capdu.getNc(), capdu.getNe(), rapdu.getSW());
	}

	public void put(int cla, int ins, int p1, int p2, int nc, int ne, int sw) {
		commandResponsePairs.put(new CommandAPDU(cla, ins, p1, p2, new byte[nc], ne), (short)sw);
	}

	public String guessCountry() {
		for (APDUFingerprint countryPrint: COUNTRY_FINGERPRINTS.keySet()) {
			if (isAllowedBy(this, countryPrint)) {
				return COUNTRY_FINGERPRINTS.get(countryPrint);
			}
		}
		return null;
	}

	/**
	 * Gets a fingerprint from a Henning-file.
	 * 
	 * @param file a henning file
	 * @return a fingerprint
	 */
	private static APDUFingerprint getFromHenningFile(File file) {
		APDUFingerprint result = new APDUFingerprint();
		try {
			byte[] fileContentBytes = new byte[(int)file.length()];
			DataInputStream in = new DataInputStream(new FileInputStream(file));
			in.readFully(fileContentBytes);
			String fileContents = new String(fileContentBytes, "UTF-8");			
			StringTokenizer st = new StringTokenizer(fileContents, "\n");
			int cla = -1, ins = -1, p1 = -1, p2 = -1, nc = -1, ne = -1;
			int sw = -1;
			while (st.hasMoreTokens()) {
				String line = st.nextToken().trim().toLowerCase();
				if (line.indexOf("instruction:") >= 0) {
					cla = -1;
					ins = getByteValue("instruction:", line);
					p1 = getByteValue("p1:", line);
					p2 = getByteValue("p2:", line);
					nc = -1;
					ne = -1;
					sw = -1;
				} else if (line.indexOf("c:") >= 0) {
					ins = getByteValue("c:", line);
					nc = getByteValue("nc=", line);
					ne = getByteValue("ne=", line);
					sw = getShortValue("r:", line);
					CommandAPDU pair = new CommandAPDU(cla, ins, p1, p2, new byte[nc], ne);
					result.commandResponsePairs.put(pair, (short)(sw & 0xFFFF));
				} else {
					/* Skip this line. */
					cla = -1;
					ins = -1;
					p1 = -1;
					p2 = -1;
					continue;
				}
			}
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Cannot read from file " + file);
		}
		return result;
	}

	private static int getByteValue(String key, String line) {
		try {
			String value = getValue(key, line);
			return Integer.parseInt(value, 16) & 0xFF;
		} catch (NumberFormatException nfe) {
			return -1; 
		}
	}

	private static int getShortValue(String key, String line) {
		try {
			return Integer.parseInt(getValue(key, line), 16) & 0xFFFF;
		} catch (NumberFormatException nfe) {
			return -1;
		}
	}

	/**
	 * Searches for key in Henning-file lines, returns hex value of next token.
	 *
	 * @param key
	 * @param line
	 * @return value of key
	 */
	private static String getValue(String key, String line) {
		line = line.trim();
		line = line.substring(line.indexOf(key) + key.length());
		line = line.trim();
		StringBuffer value = new StringBuffer();
		for (int i = 0; i < line.length() && "0123456789abcdef".indexOf(line.charAt(i)) >= 0; i++) {
			value.append(line.charAt(i));
		}
		return value.toString();
	}

	private int getResponse(CommandAPDU capdu) {
		Short r = commandResponsePairs.get(capdu);
		if (r == null) {
			return -1;
		}
		return (r.shortValue() & 0xFFFF);
	}

	/**
	 * 
	 * @param otherPrint pattern
	 * @return
	 */
	private static boolean isAllowedBy(APDUFingerprint thisPrint, APDUFingerprint otherPrint) {
		for (CommandAPDU c: thisPrint.commandResponsePairs.keySet()) {
			CommandAPDU otherC = getSimilarCommandAPDU(c, otherPrint);
			if (otherC == null) { continue; }
			if (!isAllowedBy(c, otherC)) { return false; }
			int response = thisPrint.getResponse(c);
			int otherResponse = otherPrint.getResponse(otherC);
			if (response == -1 || otherResponse == -1) { continue; }
			if (response != otherResponse) { return false; }
		}
		System.out.println("DEBUG: isAllowedBy(" + thisPrint + ", " + otherPrint + ")");
		return true;
	}

	private static CommandAPDU getSimilarCommandAPDU(CommandAPDU capdu, APDUFingerprint print) {
		for (CommandAPDU c: print.commandResponsePairs.keySet()) {
			if (capdu.getINS() == c.getINS()) { return c; }
		}
		return null;
	}

	private static boolean isAllowedBy(CommandAPDU capdu, CommandAPDU otherCapdu) {
		return isAllowedBy(capdu.getINS(), otherCapdu.getINS());
	}

	private static boolean isAllowedBy(int a, int b) {
		return b == -1 || a == b;
	}

	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) { return false; }
		APDUFingerprint other = (APDUFingerprint)obj;
		return commandResponsePairs.equals(other.commandResponsePairs);
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		int i = 0, n = commandResponsePairs.size();
		result.append("[");
		for (CommandAPDU c: commandResponsePairs.keySet()) {
			short sw = commandResponsePairs.get(c);
			result.append(Hex.bytesToHexString(c.getBytes()) + " -> " + Hex.shortToHexString((short)(sw & 0xFFFF)));
			i++;
			if (i < n) { result.append(", "); }
		}
		result.append("]");
		return result.toString();
	}
}
