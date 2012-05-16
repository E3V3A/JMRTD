/*
 * JMRTD Android client
 * 
 * Copyright (C) 2006 - 2012  The JMRTD team
 * 
 * Originally based on:
 * 
 * aJMRTD - An Android Client for JMRTD, a Java API for accessing machine readable travel documents.
 *
 * Max Guenther, max.math.guenther@googlemail.com
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
 */

package org.jmrtd.android;

import java.util.Date;

import org.jmrtd.BACKey;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Represents a BAC KeySpec
 * 
 * 
 * @author Max Guenther
 *
 */
public class BACSpecDO extends BACKey implements Parcelable {	

	private static final long serialVersionUID = -3362366262542327922L;

	public BACSpecDO(String documentNumber, String dateOfBirth, String dateOfExpiry) {
		super(documentNumber, dateOfBirth, dateOfExpiry);
	}

	public BACSpecDO(String documentNumber, Date dateOfBirth, Date dateOfExpiry) {
		super(documentNumber, dateOfBirth, dateOfExpiry);
	}

	private BACSpecDO(Parcel in) {
		setDocumentNumber(in.readString());
		setDateOfBirth(in.readString());
		setDateOfExpiry(in.readString());
	}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(getDocumentNumber());
		out.writeString(getDateOfBirth());
		out.writeString(getDateOfExpiry());
	}

	public static final Parcelable.Creator<BACSpecDO> CREATOR
	= new Parcelable.Creator<BACSpecDO>() {
		public BACSpecDO createFromParcel(Parcel in) {
			return new BACSpecDO(in);
		}

		public BACSpecDO[] newArray(int size) {
			return new BACSpecDO[size];
		}
	};	
}
