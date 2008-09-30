package sos.mrtd.sample.newgui;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BACEntry
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private String documentNumber;
	private Date dateOfBirth;
	private Date dateOfExpiry;

	public BACEntry(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		this.documentNumber = documentNumber.trim();
		this.dateOfBirth = dateOfBirth;
		this.dateOfExpiry = dateOfExpiry;
	}

	public String getDocumentNumber() {
		return documentNumber;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public Date getDateOfExpiry() {
		return dateOfExpiry;
	}

	public String toString() {
		return documentNumber + ", " + SDF.format(dateOfBirth) + ", " + SDF.format(dateOfExpiry);
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
