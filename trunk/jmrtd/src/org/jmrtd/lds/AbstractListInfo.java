/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
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

package org.jmrtd.lds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for several data structures used in the LDS
 * containing a list of elements.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision$
 *
 * @param <R> the type of the elements
 */
abstract class AbstractListInfo<R> extends AbstractLDSInfo {

	private static final long serialVersionUID = 2970076896364365191L;

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
		if (!(other instanceof AbstractListInfo<?>)) { return false; }
		try {
			@SuppressWarnings("unchecked")
			AbstractListInfo<R> otherRecord = (AbstractListInfo<R>)other;
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
	
	public abstract void writeObject(OutputStream outputStream) throws IOException;

	public abstract void readObject(InputStream inputStream) throws IOException;
}
