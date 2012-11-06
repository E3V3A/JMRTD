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

import org.jmrtd.BACKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Displays and allows editing of the list of BACS.
 * 
 * @author The JMRTD team
 * 
 * Based on code by Max Guenther.
 */
public class BacsAct extends Activity {

	private static final String TAG = "BacsAct";

	private static final String ACTION_LABEL_EDIT = "edit", ACTION_LABEL_DELETE = "delete";
	private static final int ACTION_EDIT_INDEX = 0, ACTION_DELETE_INDEX = 1;		
	private static final String[] ACTION_LABELS = { ACTION_LABEL_EDIT, ACTION_LABEL_DELETE };

	private static final int REQ_EDIT_NEW_BAC = 1;
	private static final int REQ_EDIT_BAC = 2;
	private static final int REQ_READ_PP = 3;
	private Button enterNewBACButton;
	private ListView listView;

	private BACSpecDOStore bacStore;
	private ArrayAdapter<BACKeySpec> listA;

	private BACKeySpec selectedBac;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, "onCreate");

		setContentView(R.layout.bacs);

		bacStore = new BACSpecDOStore(this);
		prepareWidgets();
	}

	private void prepareWidgets() {
		final Activity thisActivity = this;
		enterNewBACButton = (Button) findViewById(R.id.readNewW);
		enterNewBACButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivityForResult(new Intent(thisActivity, BacEditorAct.class), REQ_EDIT_NEW_BAC);
			}
		});

		listView = (ListView) findViewById(R.id.listW);
		listA = new ArrayAdapter<BACKeySpec>(this,
				android.R.layout.simple_list_item_1, bacStore.getEntries());
		listView.setAdapter(listA);
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectedBac = listA.getItem(position);

				AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
				builder.setTitle("Actions");
				builder.setItems(ACTION_LABELS, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switch (item) {
						case ACTION_EDIT_INDEX: edit(selectedBac); break;
						case ACTION_DELETE_INDEX: removeEntry(selectedBac); break;
						default: break;
						}
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
		enableForegroundDispatch(PPDisplayAct.class);
	}

	private void enableForegroundDispatch(Class<?> targetClass) {
		NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
		Intent intent = new Intent(getApplicationContext(), targetClass);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		String[][] filter = new String[][] { new String[] { "android.nfc.tech.IsoDep" } };
		adapter.enableForegroundDispatch(this, pendingIntent, null, filter);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v(TAG, "onActivityResult");
		setIntent(data);
		switch (requestCode) {
		case REQ_EDIT_NEW_BAC:
			switch (resultCode) {
			case RESULT_OK:
				//				BACKeySpec bac = data.getExtras().getParcelable(BACKeySpec.EXTRA_BAC);
				//				bacStore.addEntry(bac);

				/* NOTE: BacEditorAct already stored the entry in the DB */

				refreshAdapter();
				//				read(bac);
				break;
			case RESULT_CANCELED:
				// toastIt("error 1a"); // user pressed back button
				break;
			default:
				toastIt("error 1b");
				break;
			}
			break;
		case REQ_EDIT_BAC:
			switch (resultCode) {
			case RESULT_OK:
				//				BACKeySpec bac = data.getExtras().getParcelable(BACKeySpec.EXTRA_BAC);
				//				bacStore.addEntry(bac);

				/* NOTE: BacEditorAct already stored the entry in the DB */

				refreshAdapter();
				break;
			case RESULT_CANCELED:
				// toastIt("error 2a"); // user pressed back button
				break;
			default:
				toastIt("error 2b");
				break;
			}
			break;
		case REQ_READ_PP:
			switch (resultCode) {
			case RESULT_OK:
				Intent i = new Intent(this, PPDisplayAct.class).putExtras(data.getExtras());
				startActivity(i);
				break;
			case RESULT_CANCELED:
				// toastIt("error 3a"); // user pressed back button
				break;
			default:
				toastIt("error 3b");
			}
			break;
		default:
			toastIt("error 4, " + requestCode);
			System.err.println("DEBUG: default case, in switch, on requestCode = " + requestCode);
			break;
		}
	}

	private void removeEntry(BACKeySpec b) {
		bacStore.removeEntry(b);
		refreshAdapter();
	}

	private void edit(BACKeySpec b) {
		Intent intent = new Intent(this,BacEditorAct.class).putExtra(BacEditorAct.EDIT_BAC, (Parcelable)b);
		startActivityForResult(intent, REQ_EDIT_BAC);
		refreshAdapter();
	}

	private void toastIt(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		System.err.println("DEBUG: TOAST: " + msg);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		//		case R.id.menu_opt_info:
		//			startActivity(new Intent(this, InfoAct.class));
		//			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onDestroy() {
		bacStore.close();
		super.onDestroy();
	}

	private void refreshAdapter() {
		listA.clear();
		for (BACKeySpec b : bacStore.getEntries()) {
			listA.add(b);
		}
	}
}