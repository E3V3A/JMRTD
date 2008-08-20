/*
 * Database for storing BAC key material.
 */

package sos.mrtd.sample.newgui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Flat file based database for BAC entries.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class BACStore {

	private static final File BACDB_FILE =
		new File(PassportApp.JMRTD_USER_DIR, "bacdb.txt");

	private List<BACStoreEntry> entries;

	public BACStore() {
		entries = new ArrayList<BACStoreEntry>();
		read();
	}

	public List<BACStoreEntry> getEntries() {
		return entries;
	}

	public void addEntry(String documentNumber, String dateOfBirth, String dateOfExpiry) {
		BACStoreEntry entry = new BACStoreEntry(documentNumber.trim(), dateOfBirth.trim(), dateOfExpiry.trim());
		addEntry(entry);
	}

	public void addEntry(BACStoreEntry entry) {
		if (!entries.contains(entry)) {
			entries.add(entry);
			write();
		} else {
			entries.remove(entry);
			entries.add(entry);
			write();
		}
	}

	public void addEntry(int i, BACStoreEntry entry) {
		entries.add(i, entry);
		write();
	}

	public String getDocumentNumber() {
		return getMostRecentEntry().getDocumentNumber();
	}

	public String getDateOfBirth() {
		return getMostRecentEntry().getDateOfBirth();
	}

	public String getDateOfExpiry() {
		return getMostRecentEntry().getDateOfExpiry();
	}

	private BACStoreEntry getMostRecentEntry() {
		if (entries.isEmpty()) {
			return new BACStoreEntry("", "", "");
		}
		return entries.get(entries.size() - 1);
	}

	private String[] getFields(String entry) {
		StringTokenizer st = new StringTokenizer(entry.trim(), ",");
		int tokenCount = st.countTokens();
		String[] result = new String[tokenCount];
		for (int i = 0; i < tokenCount; i++) {
			result[i] = st.nextToken().trim();
		}
		return result;
	}

	private void read() {
		try {
			BufferedReader d = new BufferedReader(new FileReader(BACDB_FILE));
			while (true) {
				String line = d.readLine();
				if (line == null) { break; }
				String[] fields = getFields(line);
				entries.add(new BACStoreEntry(fields[0], fields[1], fields[2]));
			}
		} catch (FileNotFoundException fnfe) {
			/* NOTE: no problem... */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void write() {
		try {
			if (!BACDB_FILE.exists()) {
				if (!PassportApp.JMRTD_USER_DIR.isDirectory()) {
					PassportApp.JMRTD_USER_DIR.mkdirs();
				}
				BACDB_FILE.createNewFile();
			}
			PrintWriter d = new PrintWriter(new FileWriter(BACDB_FILE));
			for (BACStoreEntry entry: entries) {
				d.println(entry);
			}
			d.flush();
			d.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (BACStoreEntry entry: entries) {
			result.append(entry.toString());
			result.append('\n');
		}
		return result.toString();
	}

	public void removeEntry(int index) {
		entries.remove(index);
		write();
	}

	public class BACStoreEntry
	{
		private String documentNumber;
		private String dateOfBirth;
		private String dateOfExpiry;

		public BACStoreEntry(String documentNumber, String dateOfBirth, String dateOfExpiry) {
			this.documentNumber = documentNumber.trim();
			this.dateOfBirth = dateOfBirth.trim();
			this.dateOfExpiry = dateOfExpiry.trim();
		}

		public String getDocumentNumber() {
			return documentNumber;
		}

		public String getDateOfBirth() {
			return dateOfBirth;
		}

		public String getDateOfExpiry() {
			return dateOfExpiry;
		}

		public String toString() {
			return documentNumber + ", " + dateOfBirth + ", " + dateOfExpiry;
		}

		public boolean equals(Object other) {
			if (other == null) { return false; }
			if (other.getClass() != BACStoreEntry.class) { return false; }
			if (other == this) { return true; }
			BACStoreEntry previous = (BACStoreEntry)other;
			return documentNumber.equals(previous.documentNumber) &&
			dateOfBirth.equals(previous.dateOfBirth) &&
			dateOfExpiry.equals(previous.dateOfExpiry);
		}
	}

	public BACStoreEntry getEntry(int entryRowIndex) {
		return entries.get(entryRowIndex);
	}
}
