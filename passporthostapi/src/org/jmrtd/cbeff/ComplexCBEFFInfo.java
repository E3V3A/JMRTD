/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2011  The JMRTD team
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
 * $Id: $
 */

package org.jmrtd.cbeff;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex (nested) CBEFF BIR.
 * Specified in ISO 19785-1 (version 2.0) and NISTIR 6529-A (version 1.1).
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class ComplexCBEFFInfo implements CBEFFInfo {

	private List<CBEFFInfo> subRecords;

	public List<CBEFFInfo> getSubRecords() {
		if (this.subRecords == null) { this.subRecords = new ArrayList<CBEFFInfo>(); }
		return new ArrayList<CBEFFInfo>(this.subRecords);
	}

	public void add(CBEFFInfo subRecord) {
		if (this.subRecords == null) { this.subRecords = new ArrayList<CBEFFInfo>(); }
		this.subRecords.add(subRecord);
	}

	public void addAll(List<CBEFFInfo> subRecords) {
		if (this.subRecords == null) { this.subRecords = new ArrayList<CBEFFInfo>(); }
		this.subRecords.addAll(subRecords);
	}

	public void remove(int index) {
		if (this.subRecords == null) { this.subRecords = new ArrayList<CBEFFInfo>(); }
		this.subRecords.remove(index);
	}

	public boolean equals(Object other) {
		if (this.subRecords == null) { this.subRecords = new ArrayList<CBEFFInfo>(); }
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!(other.getClass().equals(ComplexCBEFFInfo.class))) { return false; }
		ComplexCBEFFInfo otherRecord = (ComplexCBEFFInfo)other;
		return subRecords.equals(otherRecord.getSubRecords());
	}

	public int hashCode() {
		if (this.subRecords == null) { this.subRecords = new ArrayList<CBEFFInfo>(); }
		return 7 * subRecords.hashCode() + 11;
	}
}
