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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Flat file based database for BAC entries.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: $
 */
public class BACStore
{
	private static final File BACDB_FILE =
		new File(PassportApp.JMRTD_USER_DIR, "bacdb.txt");
	
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private List<BACEntry> entries;

	public BACStore() {
		entries = new ArrayList<BACEntry>();
		read();
	}

	public List<BACEntry> getEntries() {
		return entries;
	}

//	public void addEntry(String documentNumber, String dateOfBirth, String dateOfExpiry) {
//		BACEntry entry = new BACEntry(documentNumber.trim(), dateOfBirth.trim(), dateOfExpiry.trim());
//		addEntry(entry);
//	}

	public void addEntry(BACEntry entry) {
		if (!entries.contains(entry)) {
			entries.add(entry);
			write();
		} else {
			entries.remove(entry);
			entries.add(entry);
			write();
		}
	}

	public void addEntry(int i, BACEntry entry) {
		entries.add(i, entry);
		write();
	}

	public String getDocumentNumber() {
		return getMostRecentEntry().getDocumentNumber();
	}

	public Date getDateOfBirth() {
		return getMostRecentEntry().getDateOfBirth();
	}

	public Date getDateOfExpiry() {
		return getMostRecentEntry().getDateOfExpiry();
	}

	private BACEntry getMostRecentEntry() {
		if (entries.isEmpty()) {
			String defaultDocumentNumber = "";
			Date defaultDateOfBirth = Calendar.getInstance().getTime();
			Date defaultDateOfExpiry = Calendar.getInstance().getTime();
			return new BACEntry(defaultDocumentNumber, defaultDateOfBirth, defaultDateOfExpiry);
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
				entries.add(new BACEntry(fields[0], SDF.parse(fields[1]), SDF.parse(fields[2])));
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
			for (BACEntry entry: entries) {
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
		for (BACEntry entry: entries) {
			result.append(entry.toString());
			result.append('\n');
		}
		return result.toString();
	}

	public void removeEntry(int index) {
		entries.remove(index);
		write();
	}

	public BACEntry getEntry(int entryRowIndex) {
		return entries.get(entryRowIndex);
	}
}
