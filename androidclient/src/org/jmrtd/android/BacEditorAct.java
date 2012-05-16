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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jmrtd.BACKeySpec;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

/**
 * Edit/Create a single BACSpecDO
 * 
 * The controls will be populated with the Intents Extra BACKeySpec (if present)
 * If the 'ok' Button is pressed, an Back-Intent will be created, containing a
 * BACKeySpec with the controls values.
 * 
 * @author Max Guenther
 */
public class BacEditorAct extends Activity {

	public static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	public static final String EDIT_BAC = "EDIT_BAC";
	
	private static final int DIALOG_ID_DOB = 0;
	private static final int DIALOG_ID_DOE = 1;

	
	private EditText docNumW;
	private Button selectDobW;
	private Button selectDoeW;
	private Button okButton;
	private Button cancelButton;

	private Date dob;
	private Date doe;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bac_editor);

		prepareWidgets();
		resolveIntent(getIntent());
	}

	private void resolveIntent(Intent intent) {
		if (intent.getExtras() != null) {
			BACKeySpec bacKeySpec = intent.getExtras().getParcelable(EDIT_BAC);
			if (bacKeySpec == null) { System.err.println("DEBUG: bacKeySpec == null"); }
			if (bacKeySpec != null) {
				docNumW.setText(bacKeySpec.getDocumentNumber());
				try {
					dob = SDF.parse(bacKeySpec.getDateOfBirth());
					doe = SDF.parse(bacKeySpec.getDateOfExpiry());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				updateDisplay();
			}
		}
		if (dob == null) { dob = new Date(); }
		if (doe == null) { doe = new Date(); }
	}

	private void updateDisplay() {
		String dobLabel = getResources().getString(R.string.selectDOB);
		if (dob != null)
			dobLabel += " " + SDF.format(dob);

		selectDobW.setText(dobLabel);

		String doeLabel = getResources().getString(R.string.selectDOE);
		if (doe != null)
			doeLabel += " " + SDF.format(doe);

		selectDoeW.setText(doeLabel);
	}

	private void prepareWidgets() {
		docNumW = (EditText) findViewById(R.id.docNum);

		selectDobW = (Button) findViewById(R.id.selectDOB);
		selectDoeW = (Button) findViewById(R.id.selectDOE);

		okButton = (Button) findViewById(R.id.bacEditor_okW);
		cancelButton = (Button) findViewById(R.id.bacEditor_cancelW);

		final Activity thisActivity = this;

		selectDobW.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				showDialog(DIALOG_ID_DOB);
			}
		});
		selectDoeW.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				showDialog(DIALOG_ID_DOE);				
			}			
		});
		okButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				BACKeySpec bacEntry = new BACSpecDO(docNumW.getText().toString(), dob, doe);
				BACSpecDOStore bacStore = new BACSpecDOStore(thisActivity);
				try { bacStore.addEntry(bacEntry); } finally { bacStore.close(); }
				BACSpecDO b = new BACSpecDO( docNumW.getText().toString(), dob, doe);
                Intent i = new Intent().putExtra(EDIT_BAC, (Parcelable)b);
                setResult(RESULT_OK, i);
                bacStore.close(); /* DEBUG: untested */
				finish();
			}

		});
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
			}			
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ID_DOB:
			return createDatePickerDialog(dob);
		case DIALOG_ID_DOE:
			return createDatePickerDialog(doe);
		}
		return null;
	}

	private Dialog createDatePickerDialog(Date d) {
		return new DatePickerDialog(this, new OnDateSetListener(d),
				d.getYear() + 1900, d.getMonth(), d.getDate());
	}

	private class OnDateSetListener implements DatePickerDialog.OnDateSetListener {

		private Date subject;

		public OnDateSetListener(Date subject) {
			this.subject = subject;
		}

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			subject.setYear(year);
			subject.setMonth(monthOfYear);
			subject.setDate(dayOfMonth);
			updateDisplay();
		}
	}
}
