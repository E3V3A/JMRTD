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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.IsoDepCardService;

import org.jmrtd.BACDeniedException;
import org.jmrtd.MRTDTrustStore;
import org.jmrtd.Passport;
import org.jmrtd.PassportService;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.DataGroup;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.MRZInfo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Display Passport content
 * 
 * @author Max Guenther (with changes by MO) 
 */
public class PPDisplayAct extends Activity {

	private static final String TAG = "PPDisplayAct";

	private BACSpecDOStore bacStore;

	//	private ProgressDialog progressDialog;
	private Handler progressHandler;

	private ImageView imageView;
	private TextView documentNumberW;
	private TextView personalNumberW;
	private TextView issuingStateW;
	private TextView primaryIdentifierW;
	private TextView secondaryIdentifiersW;
	private TextView genderW;
	private TextView nationalityW;
	private TextView dobW;
	private TextView doeW;
	private ProgressBar progressBar;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bacStore = new BACSpecDOStore(this);
		setContentView(R.layout.pp_display);
		prepareWidgets();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");

		Intent intent = getIntent();
		resolveIntent(intent);

		//		if (progressDialog != null && progressDialog.isShowing()) {
		//			progressDialog.dismiss();
		//		}
		//		
		//		progressDialog = new ProgressDialog(this);
		//		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		//		progressDialog.setTitle("Provide passport");
		//		progressDialog.setMessage("Looking for document...");
		//		progressDialog.show();
		enableForegroundDispatch(); // Why not call from onCreate? -- MO
	}

	private void enableForegroundDispatch() {
		NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
		Intent intent = new Intent(getApplicationContext(), this.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		String[][] filter = new String[][] { new String[] { "android.nfc.tech.IsoDep" } };
		adapter.enableForegroundDispatch(this, pendingIntent, null, filter);
	}

	private void prepareWidgets() {
		imageView = (ImageView)findViewById(R.id.pp_display_iv);
		documentNumberW = (TextView)findViewById(R.id.ppd_documentNumberW);
		personalNumberW = (TextView)findViewById(R.id.ppd_personalNumberW);
		issuingStateW = (TextView)findViewById(R.id.ppd_issuingStateW);
		primaryIdentifierW = (TextView)findViewById(R.id.ppd_primaryIdentifierW);
		secondaryIdentifiersW = (TextView)findViewById(R.id.ppd_secondaryIdentifiersW);
		genderW = (TextView)findViewById(R.id.ppd_genderW);
		nationalityW = (TextView)findViewById(R.id.ppd_nationalityW);
		dobW = (TextView)findViewById(R.id.ppd_dateOfBirthW);
		doeW = (TextView)findViewById(R.id.ppd_dateOfExpiryW);
		progressBar = (ProgressBar)findViewById(R.id.ppd_progressW);
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.v(TAG, "onNewIntent");
		setIntent(intent);
		resolveIntent(intent);
	}

	private void resolveIntent(Intent intent) {
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			Tag t = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
			if (Arrays.asList(t.getTechList()).contains("android.nfc.tech.IsoDep")) {
				handleIsoDepFound(IsoDep.get(t));
			}
		} else {
			System.err.println("DEBUG: unhandled action " + action);
		}
	}

	private void handleIsoDepFound(IsoDep isoDep) {
		//		if (progressDialog != null && progressDialog.isShowing()) {
		//			progressDialog.dismiss();
		//		}

		//		progressDialog = new ProgressDialog(this);
		//		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		//		progressDialog.setTitle("Please wait");
		//		progressDialog.setMessage("Reading...");
		//		if (!progressDialog.isShowing()) {
		//			progressDialog.show();
		//		}

		Log.v(TAG, "handleIsoDepFound " + isoDep);
		try {
			isoDep.setTimeout(10000);
			new AsyncPassportCreate().execute(isoDep);
		} catch (Exception ex) {
			Log.e(TAG, "error " + ex.toString(), ex);
		}
	}

	class AsyncPassportCreate extends AsyncTask<IsoDep, String, Passport> {

		@Override
		protected Passport doInBackground(IsoDep... params) {
			try {
				IsoDep isoDep = params[0];
				Log.v(TAG, "isoDep = " + isoDep);
				IsoDepCardService service = new IsoDepCardService(isoDep);
				service.open();
				PassportService passportService = new PassportService(service);

				/* Try all BACs */
//				BACStore abacStore = new MemoryBACStore();
//				for (BACSpecDO bacSpec : bacStore) {
//					if (bacSpec == null) { continue; }
//					abacStore.addEntry(toBACKeySpec(bacSpec));
//				}

				try {
					Passport passport = new Passport(passportService, new MRTDTrustStore(), bacStore, 1);
					Log.v(TAG, "passport = " + passport);
					return passport;
				} catch (BACDeniedException cse) {
					// postexecute will goto bac entry activity
				} 
				return null;
			} catch (CardServiceException cse) {
				Log.w(TAG, "DEBUG: EXCEPTION: " + cse.getMessage());				
				cse.printStackTrace();
				return null;
			} catch (Exception e) {
				Log.w(TAG, "DEBUG: EXCEPTION: " + e.getMessage());
				return null;
			} finally {
				bacStore.close(); // DEBUG
			}
		}

		@Override
		protected void onPostExecute(Passport passport) {
			// if (passport == null) { throw new IllegalArgumentException("Failed to get a passport"); }
			if (passport == null) {
				// goto bac entry activity
				System.err.println("DEBUG: starting baceditor because BAC failed");
				Intent myIntent = new Intent(PPDisplayAct.this, BacEditorAct.class);
				setResult(RESULT_OK, myIntent);
				finish();
				PPDisplayAct.this.startActivity(myIntent);
				System.err.println("BAC denied!");
			} else {
				handlePassportCreated(passport);
			}
		}
	}

	private void handlePassportCreated(Passport passport) {
		if (passport == null) { throw new IllegalArgumentException("Failed to get a passport"); }

		//		if (progressDialog != null && progressDialog.isShowing()) {
		//			progressDialog.dismiss();
		//		}
		//		progressDialog = new ProgressDialog(this);

		progressBar.setMax(passport.getTotalLength()); /* DEBUG */
		progressBar.setProgress(passport.getBytesRead());

		//		progressDialog.setMax(passport.getTotalLength());
		//		progressDialog.setProgress(passport.getBytesRead());
		//		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		//		progressDialog.setTitle("Please wait");
		//		progressDialog.setMessage("Reading...");
		//		progressDialog.setCancelable(true);
		//		progressDialog.setOnCancelListener(new OnCancelListener() {
		//			public void onCancel(DialogInterface dialog) {
		//				setResult(RESULT_CANCELED);
		//				finish();
		//			}
		//		});
		progressHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				/* get the value from the Message */
				int progress = msg.arg1;
				//				progressDialog.setProgress(progress);
				progressBar.setProgress(progress);
			}
		};
		//		progressDialog.show();

		passport.addProgressListener(new Passport.ProgressListener() {
			public void changed(int progress, int max) {
				Message message = new Message();
				message.arg1 = progress;
				progressHandler.sendMessage(message);
			}

		});
		new AsyncPassportInterpret().execute(passport);
	}

	class AsyncPassportInterpret extends AsyncTask<Passport, PassportProgress, Integer> {

		@Override
		protected Integer doInBackground(Passport... params) {
			try {
				Passport passport = params[0];

				List<Short> fileList = passport.getFileList();
				Collections.sort(fileList);

				for (short fid: fileList) {
					switch(fid) {
					case PassportService.EF_COM:
						//						COMFile comFile = new COMFile(passport.getInputStream(fid));
						break;
					case PassportService.EF_DG1:
						publishProgress(new PassportProgress(PassportProgress.STARTED, LDSFile.EF_DG1_TAG));
						DG1File dg1File = new DG1File(passport.getInputStream(fid));
						publishProgress(new PassportProgress(PassportProgress.FINISHED, dg1File));
						break;
					case PassportService.EF_DG2:
						publishProgress(new PassportProgress(PassportProgress.STARTED, LDSFile.EF_DG2_TAG));
						DG2File dg2File = new DG2File(passport.getInputStream(fid));
						publishProgress(new PassportProgress(PassportProgress.FINISHED, dg2File));
						break;
					case PassportService.EF_DG15:
						publishProgress(new PassportProgress(PassportProgress.STARTED, LDSFile.EF_DG15_TAG));
						DG15File dg15File = new DG15File(passport.getInputStream(fid));
						publishProgress(new PassportProgress(PassportProgress.FINISHED, dg15File));
						break;
					case PassportService.EF_SOD:
						//						SODFile sodFile = new SODFile(passport.getInputStream(fid));
						break;
					}
				}
				return 0;
			} catch (CardServiceException cse) {
				cse.printStackTrace();
				return null;
			} catch (Exception e) {
				System.err.println("DEBUG: EXCEPTION: " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onProgressUpdate(PassportProgress... progress) {
			PassportProgress e = progress[0];
			switch (e.getType()) {
			case PassportProgress.STARTED:
				try {
					//					progressDialog.setTitle("Please wait");
					//					progressDialog.setMessage("Examining DG" + LDSFileUtil.lookupDataGroupNumberByTag(e.getTag()));
				} catch (NumberFormatException nfe) {
				}
				break;
			case PassportProgress.FINISHED:
				LDSFile file = e.getFile();
				if (file != null && file instanceof DataGroup) {
					handleDataGroupInterpreted((DataGroup)file);
				}
				break;
			default:
				return;
			}
		}

		@Override
		protected void onPostExecute(Integer i) {
			//			progressDialog.dismiss();
		}
	}

	private void handleDataGroupInterpreted(DataGroup dg) {
		switch (dg.getTag()) {
		case LDSFile.EF_DG1_TAG:
			DG1File dg1 = (DG1File)dg;
			MRZInfo mrzInfo = dg1.getMRZInfo();
			documentNumberW.setText(mrzInfo.getDocumentNumber());
			personalNumberW.setText(mrzInfo.getPersonalNumber());
			issuingStateW.setText(mrzInfo.getIssuingState());
			primaryIdentifierW.setText(mrzInfo.getPrimaryIdentifier().replace("<", " ").trim());
			secondaryIdentifiersW.setText(mrzInfo.getSecondaryIdentifier().replace("<", " ").trim());
			genderW.setText(mrzInfo.getGender().toString());
			nationalityW.setText(mrzInfo.getNationality());
			dobW.setText(mrzInfo.getDateOfBirth());
			doeW.setText(mrzInfo.getDateOfExpiry());
			break;
		case LDSFile.EF_DG2_TAG:
			DG2File dg2 = (DG2File)dg;
			List<FaceImageInfo> allFaceImageInfos = new ArrayList<FaceImageInfo>();
			List<FaceInfo> faceInfos = dg2.getFaceInfos();
			for (FaceInfo faceInfo : faceInfos) {
				allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
			}
			if (allFaceImageInfos.size() > 0) {
				//				if (progressDialog != null && progressDialog.isShowing()) {
				//					progressDialog.dismiss();
				//				}
				//				progressDialog = new ProgressDialog(this);
				//				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				//				progressDialog.setMessage("Decoding image...");
				//				progressDialog.show();
				new AsyncImageDecode().execute(allFaceImageInfos.get(0));
			} else {
				//				progressDialog.dismiss();
			}
			break;
		default:
			break;
		}
	}

	class AsyncImageDecode extends AsyncTask<FaceImageInfo, String, Bitmap> {

		@Override
		protected Bitmap doInBackground(FaceImageInfo... params) {
			try {
				FaceImageInfo faceImageInfo = params[0];
				InputStream faceImageInputStream = faceImageInfo.getImageInputStream();
				String mimeType = faceImageInfo.getMimeType();
				return ImageUtil.read(faceImageInputStream, mimeType);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return null;
			} catch (Exception e) {
				System.err.println("DEBUG: EXCEPTION: " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			imageView.setImageBitmap(result);
			//			progressDialog.dismiss();
			progressBar.setProgress(progressBar.getMax());
		}
	}

	class PassportProgress {

		public static final int STARTED = 0, FINISHED = 1;

		private int type;
		private int tag;
		private LDSFile file;

		public PassportProgress(int type, int tag) {
			this.type = type;
			this.tag = tag;
			this.file = null;
		}

		public PassportProgress(int type, LDSFile file) {
			this.type = type;
			this.tag = -1;
			this.file = file;
		}

		public int getType() {
			return type;
		}

		public LDSFile getFile() {
			return file;
		}

		public int getTag() {
			return tag;
		}
	}
}
