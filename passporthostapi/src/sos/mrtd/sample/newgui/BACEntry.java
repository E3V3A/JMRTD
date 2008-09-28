package sos.mrtd.sample.newgui;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BACEntry
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("ddMMyy");

	private String documentNumber;
	private String dateOfBirth;
	private String dateOfExpiry;

	public BACEntry(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		this(documentNumber, SDF.format(dateOfBirth), SDF.format(dateOfExpiry));
	}
	
	public BACEntry(String documentNumber, String dateOfBirth, String dateOfExpiry) {
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
		if (other.getClass() != BACEntry.class) { return false; }
		if (other == this) { return true; }
		BACEntry previous = (BACEntry)other;
		return documentNumber.equals(previous.documentNumber) &&
		dateOfBirth.equals(previous.dateOfBirth) &&
		dateOfExpiry.equals(previous.dateOfExpiry);
	}
}
