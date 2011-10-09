package org.jmrtd.lds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * List info containing list of elements.
 * 
 * @author martijn.oostdijk
 *
 * @param <R> the type of the elements
 */
abstract class ListInfo<R> extends AbstractInfo {

	private List<R> subRecords;

	List<R> getSubRecords() {
		if (this.subRecords == null) { this.subRecords = new ArrayList<R>(); }
		return new ArrayList<R>(this.subRecords);
	}

	void add(R subRecord) {
		if (this.subRecords == null) { this.subRecords = new ArrayList<R>(); }
		this.subRecords.add(subRecord);
	}

	void addAll(List<R> subRecords) {
		if (this.subRecords == null) { this.subRecords = new ArrayList<R>(); }
		this.subRecords.addAll(subRecords);
	}

	void remove(int index) {
		if (this.subRecords == null) { this.subRecords = new ArrayList<R>(); }
		this.subRecords.remove(index);
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!(other instanceof ListInfo<?>)) { return false; }
		try {
			@SuppressWarnings("unchecked")
			ListInfo<R> otherRecord = (ListInfo<R>)other;
			List<R> subRecords = getSubRecords();
			List<R> otherSubRecords = otherRecord.getSubRecords();
			int subRecordCount = subRecords.size();
			if (subRecordCount != otherSubRecords.size()) { return false; }
			for (int i = 0; i < subRecordCount; i++) {
				R subRecord = subRecords.get(i);
				R otherSubRecord = otherSubRecords.get(i);
				if (subRecord == null) {
					if (otherSubRecord != null) {
						return false;
					}
				} else if (!subRecord.equals(otherSubRecord)) {
					return false;
				}
			}
			return true;
		} catch (ClassCastException cce) {
			return false;
		}
	}
	
	public int hashCode() {
		int result = 1234567891;
		List<R> subRecords = getSubRecords();
		for (R record: subRecords) {
			if (record == null) {
				result = 3 * result + 5;
			} else {
			result = 5 * (result + record.hashCode()) + 7;
			}
		}
		return 7 * result + 11;
	}
	
	public abstract void writeObject(OutputStream out) throws IOException;

	public abstract void readObject(InputStream in) throws IOException;
}
