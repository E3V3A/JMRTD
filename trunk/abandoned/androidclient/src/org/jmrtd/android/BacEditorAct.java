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
import java.util.Calendar;

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

	private Calendar dob;
	private Calendar doe;

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
			if (dob == null) { dob = Calendar.getInstance(); }
			if (doe == null) { doe = Calendar.getInstance(); }
			if (bacKeySpec == null) { System.err.println("DEBUG: bacKeySpec == null"); }
			if (bacKeySpec != null) {
				docNumW.setText(bacKeySpec.getDocumentNumber());
				try {
					dob.setTime(SDF.parse(bacKeySpec.getDateOfBirth()));
					doe.setTime(SDF.parse(bacKeySpec.getDateOfExpiry()));
				} catch (ParseException e) {
					e.printStackTrace();
				}
				updateDisplay();
			}
		}
	}

	private void updateDisplay() {
		String dobLabel = getResources().getString(R.string.selectDOB);
		if (dob != null) {
			dobLabel += " " + SDF.format(dob.getTime());
		}
		
//		System.err.println("DEBUG: ---> dob.getTime() = " + dob == null ? " null" : dob.getTime());
//		System.err.println("DEBUG: ---> dobLabel = " + dobLabel);
		selectDobW.setText(dobLabel);

		String doeLabel = getResources().getString(R.string.selectDOE);
		if (doe != null) {
			doeLabel += " " + SDF.format(doe.getTime());
		}
//		System.err.println("DEBUG: ---> doe.getTime() = " + doe == null ? "null" : doe.getTime());
//		System.err.println("DEBUG: ---> doeLabel = " + doeLabel);

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
				BACKeySpec bacEntry = new BACSpecDO(docNumW.getText().toString(), dob.getTime(), doe.getTime());
				BACSpecDOStore bacStore = new BACSpecDOStore(thisActivity);
				try { bacStore.addEntry(bacEntry); } finally { bacStore.close(); }
				BACSpecDO b = new BACSpecDO( docNumW.getText().toString(), dob.getTime(), doe.getTime());
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
			if (dob == null) { dob = Calendar.getInstance(); }
			return createDatePickerDialog(dob);
		case DIALOG_ID_DOE:
			if (doe == null) { doe = Calendar.getInstance(); }
			return createDatePickerDialog(doe);
		}
		return null;
	}

	private Dialog createDatePickerDialog(Calendar cal) {
		return new DatePickerDialog(this, new OnDateSetListener(cal),
				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
	}

	private class OnDateSetListener implements DatePickerDialog.OnDateSetListener {

		private Calendar cal;

		public OnDateSetListener(Calendar cal) {
			this.cal = cal;
		}

		public void onDateSet(DatePicker view, int year, int month, int date) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, month);
			cal.set(Calendar.DATE, date);
			updateDisplay();
		}
	}
}
