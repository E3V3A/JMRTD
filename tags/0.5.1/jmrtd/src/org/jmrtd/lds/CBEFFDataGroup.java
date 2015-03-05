/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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
import java.util.Random;
import java.util.logging.Logger;

import net.sf.scuba.tlv.TLVOutputStream;

import org.jmrtd.cbeff.BiometricDataBlock;
import org.jmrtd.cbeff.ISO781611;

/**
 * Datagroup containing a list of biometric information templates (BITs).
 * The {@link DG2File}, {@link DG3File}, and {@link DG4File} datagroups
 * are based on this type.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @param <R> the type of the elements
 * 
 * @version $Revision$
 */
abstract class CBEFFDataGroup<R extends BiometricDataBlock> extends DataGroup {

	private static final long serialVersionUID = 2702959939408371946L;

	protected static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private Random random;
	
	/** Records in the BIT group. Each record represents a single BIT. */
	private List<R> subRecords;

	CBEFFDataGroup(int dataGroupTag, List<R> subRecords) {
		super(dataGroupTag);
		addAll(subRecords);
		this.random = new Random();
	}
	
	/**
	 * Constructs an instance.
	 * 
	 * @param inputStream an input stream
	 * @param dataGroupTag the datagroup tag to use
	 * @param biometricDataGroupTag the biometric data block tag to use (either <code>5F2E</code> or <code>7F2E</code>).
	 */
	CBEFFDataGroup(int dataGroupTag, InputStream inputStream) throws IOException {
		super(dataGroupTag, inputStream);
		this.random = new Random();
	}

	void add(R record) {
		if (subRecords == null) { subRecords = new ArrayList<R>(); }
		subRecords.add(record);
	}

	void addAll(List<R> records) {
		if (subRecords == null) { subRecords = new ArrayList<R>(); }
		subRecords.addAll(records);
	}

	void remove(int index) {
		if (subRecords == null) { subRecords = new ArrayList<R>(); }
		subRecords.remove(index);		
	}

	List<R> getSubRecords() {
		if (subRecords == null) { subRecords = new ArrayList<R>(); }
		return new ArrayList<R>(subRecords);
	}

	public boolean equals(Object other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!(other instanceof CBEFFDataGroup<?>)) { return false; }
		try {
			@SuppressWarnings("unchecked")
			CBEFFDataGroup<R> otherDG = (CBEFFDataGroup<R>)other;
			List<R> subRecords = getSubRecords();
			List<R> otherSubRecords = otherDG.getSubRecords();
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

	/**
	 * Concrete implementations of EAC protected CBEFF DataGroups should call this
	 * method at the end of their {@link #writeContent(OutputStream)} method to add
	 * some random data if the record contains zero biometric templates.
	 * See supplement to ICAO Doc 9303 R7-p1_v2_sIII_0057.
	 * 
	 * @param outputStream the outputstream
	 * 
	 * @throws IOException on I/O errors
	 */
	protected void writeOptionalRandomData(OutputStream outputStream) throws IOException {
		if (subRecords.size() == 0) {
			TLVOutputStream tlvOut = outputStream instanceof TLVOutputStream ? (TLVOutputStream)outputStream : new TLVOutputStream(outputStream);
			tlvOut.writeTag(ISO781611.DISCRETIONARY_DATA_FOR_PAYLOAD_TAG);
			byte[] value = new byte[8];
			random.nextBytes(value);
			tlvOut.writeValue(value);
		}
	}
}
