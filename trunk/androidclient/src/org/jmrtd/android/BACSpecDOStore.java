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

import java.util.List;
import java.util.Vector;

import org.jmrtd.BACKeySpec;
import org.jmrtd.BACStore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DB-Interface to deal with BACSpecDOs
 * 
 * 
 * @author Max Guenther
 */
public class BACSpecDOStore implements BACStore {

	private static final String DB_NAME = "mrtd.db";
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_BAC = "bac";

	private static final String COL_DOC_NUM = "DOCNUM";
	private static final String COL_DOB = "DOB";
	private static final String COL_DOE = "DOE";

	private DatabaseHelper helper;

	public BACSpecDOStore(Context context) {
		super();
		helper = new DatabaseHelper(context);
	}

	public List<BACKeySpec> getEntries(){
		SQLiteDatabase db = helper.getWritableDatabase();
		Cursor c = db.query(TABLE_BAC, null, null, null, null, null, null);
		Vector<BACKeySpec> result = new Vector<BACKeySpec>();
		if (c.getCount() > 0) {
			c.moveToFirst();
			do {
				result.add(new BACSpecDO(getDocumentNumber(c),
					getDateOfBirth(c), getDateOfExpiry(c)));
			} while (c.moveToNext());
		}
		c.close();
		return result;
	}
	
	public BACKeySpec getEntry(int index) {
		return getEntries().get(index);
	}

	public void removeEntry(int index) {
		BACKeySpec bacKeySpec = getEntry(index);
		if (bacKeySpec == null) { return; }
		removeEntry(getEntry(index));
	}
	
	public void removeEntry(BACKeySpec bacKeySpec) {
		SQLiteDatabase db = helper.getWritableDatabase();
		String where = COL_DOC_NUM + "='" + bacKeySpec.getDocumentNumber() + "'";
		db.delete(TABLE_BAC, where, null);
	}

	public void addEntry(int index, BACKeySpec bacKeySpec) {
		addEntry(bacKeySpec);
	}

	public void addEntry(BACKeySpec bacKeySpec) {
		if (has(bacKeySpec.getDocumentNumber())) {
			update(bacKeySpec);
		} else {
			insert(bacKeySpec);
		}
	}
	
	public void close() {
		helper.close();
	}

	/* ONLY PRIVATE METHODS BELOW */
	
	private boolean has(String docNumber) {
		SQLiteDatabase db = helper.getWritableDatabase();
		String where = COL_DOC_NUM + "='" + docNumber + "'";
		Cursor c = db.query(TABLE_BAC, null, where, null, null, null, null);
		int count = c.getCount();
		c.close();
		return count > 0;
	}
	
	private String getDocumentNumber(Cursor c) {
		return c.getString(c.getColumnIndex(COL_DOC_NUM));
	}

	private String getDateOfBirth(Cursor c) {
		return c.getString(c.getColumnIndex(COL_DOB));
	}

	private String getDateOfExpiry(Cursor c) {
		return c.getString(c.getColumnIndex(COL_DOE));
	}

	private void insert(BACKeySpec b) {
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COL_DOC_NUM, b.getDocumentNumber());
		values.put(COL_DOB, b.getDateOfBirth());
		values.put(COL_DOE, b.getDateOfExpiry());
		db.insert(TABLE_BAC, null, values);
	}

	private void update(BACKeySpec bacKeySpec) {
		SQLiteDatabase db = helper.getWritableDatabase();

		String where = COL_DOC_NUM + "='" + bacKeySpec.getDocumentNumber() + "'";

		ContentValues values = new ContentValues();
		values.put(COL_DOC_NUM, bacKeySpec.getDocumentNumber());
		values.put(COL_DOB, bacKeySpec.getDateOfBirth());
		values.put(COL_DOE, bacKeySpec.getDateOfExpiry());
		db.update(TABLE_BAC, values, where, null);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
	
		public DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_BAC + " (" + COL_DOC_NUM
					+ " STRING PRIMARY KEY," + COL_DOB + " STRING," + COL_DOE
					+ " STRING" + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_BAC);
			onCreate(db);
		}
	}
}
