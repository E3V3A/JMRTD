/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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

package sos.mrtd.sample.newgui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;

import sos.data.Country;
import sos.data.Gender;
import sos.mrtd.COMFile;
import sos.mrtd.DG15File;
import sos.mrtd.DG1File;
import sos.mrtd.DG2File;
import sos.mrtd.FaceInfo;
import sos.mrtd.MRZInfo;
import sos.mrtd.PassportFile;
import sos.mrtd.PassportPersoService;
import sos.mrtd.PassportService;
import sos.mrtd.SODFile;
import sos.smartcards.CardFileInputStream;
import sos.smartcards.CardManager;
import sos.smartcards.CardServiceException;
import sos.smartcards.TerminalCardService;
import sos.util.Files;
import sos.util.Hex;
import sos.util.Icons;

/**
 * Frame for displaying a passport while (and after) it is being read.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 308 $
 */
public class PassportFrame extends JFrame
{
	private static final String PASSPORT_FRAME_TITLE = "JMRTD - Passport";
	private static final Dimension PREFERRED_SIZE = new Dimension(520, 420);

	private static final Icon CERTIFICATE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("script_key"));
	private static final Icon CERTIFICATE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("script_key"));
	private static final Icon KEY_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("key"));
	private static final Icon KEY_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("key"));
	private static final Icon MAGNIFIER_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("magnifier"));
	private static final Icon MAGNIFIER_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("magnifier"));
	private static final Icon SAVE_AS_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon SAVE_AS_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon CLOSE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon LOAD_IMAGE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_image"));
	private static final Icon LOAD_IMAGE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_image"));
	private static final Icon DELETE_IMAGE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("image_delete"));
	private static final Icon LOAD_CERT_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_page_white"));
	private static final Icon LOAD_CERT_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_page_white"));
	private static final Icon LOAD_KEY_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_key"));
	private static final Icon LOAD_KEY_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_key"));
	private static final Icon DELETE_IMAGE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("image_delete"));
	private static final Icon UPLOAD_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_burn"));
	private static final Icon UPLOAD_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_burn"));

	private FacePreviewPanel facePreviewPanel;

	private JPanel panel, centerPanel;

	private Map<Short, CardFileInputStream> fileStreams;
	private Map<Short, InputStream> bufferedStreams;
	private int totalLength;

	private DG1File dg1;
	private DG2File dg2;
	private DG15File dg15;
	private SODFile sod;
	private COMFile com;

	private X509Certificate docSigningCert, countrySigningCert;

	private VerificationIndicator verificationPanel;
	private boolean isBACVerified;
	private boolean isAAVerified;
	private boolean isDSVerified;
	private Country issuingState;

	public PassportFrame() {
		super(PASSPORT_FRAME_TITLE);
		verificationPanel = new VerificationIndicator();
		panel = new JPanel(new BorderLayout());
		centerPanel = new JPanel(new BorderLayout());
		panel.add(centerPanel, BorderLayout.CENTER);
		panel.add(verificationPanel, BorderLayout.SOUTH);
		facePreviewPanel = new FacePreviewPanel(160, 200);
		centerPanel.add(facePreviewPanel, BorderLayout.WEST);
		bufferedStreams = new HashMap<Short, InputStream>();
		fileStreams = new HashMap<Short, CardFileInputStream>();
		getContentPane().add(panel);
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createToolsMenu());
		setIconImage(Icons.getImage("jmrtd_icon"));
		pack();
		setVisible(true);
	}

	/**
	 * Fills the passportFiles inputstreams with passport inputstreams.
	 * 
	 * @param service the service
	 * 
	 * @return a passport frame.
	 */
	public void readFromService(PassportService service, boolean isBACVerified) throws CardServiceException {
		long t0 = System.currentTimeMillis();
		long t = t0;
		System.out.println("DEBUG: start reading from service t = 0");
		final PassportService s = service;
//		s.addAPDUListener(new APDUListener() {
//		int i;
//		public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
//		System.out.println("-> (" + (i++) + ") " + Hex.bytesToHexString(capdu.getBytes()));
//		System.out.println("<- " + Hex.bytesToHexString(rapdu.getBytes()));
//		}
//		});
		try {
			this.isBACVerified = isBACVerified;
			verificationPanel.setBACState(isBACVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_UNKNOWN);
			putFile(PassportService.EF_COM, service);
			putFile(PassportService.EF_SOD, service);
			InputStream comIn = getFile(PassportService.EF_COM);
			COMFile comFile = new COMFile(comIn);
			int[] tags = comFile.getTagList();
			for (int i = 0; i < tags.length; i++) {
				putFile(PassportFile.lookupFIDByTag(tags[i]), service);
			}
		} catch (CardServiceException cse) {
			cse.printStackTrace();
			dispose();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		t = System.currentTimeMillis();
		System.out.println("DEBUG: inputstreams buffered t = " + ((double)(t - t0) / 1000) + "s");

		(new Thread(new Runnable() {
			public void run() {
				try {
					ProgressMonitor m = new ProgressMonitor(getContentPane(), "Reading ", "[" + 0 + "/" + (totalLength / 1024) + " kB]", 0, totalLength);
					while (estimateBytesRead() >  0) {
						Thread.sleep(200);
						int bytesRead = estimateBytesRead();
						m.setProgress(bytesRead);
						m.setNote("[" + (bytesRead / 1024) + "/" + (totalLength /1024) + " kB]");
					}
				} catch (InterruptedException ie) {
				} catch (Exception e) {
				}
			}
		})).start();

		displayInputStreams();
		t = System.currentTimeMillis();
		System.out.println("DEBUG: finished displaying t = " + ((double)(t - t0) / 1000) + "s");

		verifySecurity(s);
		t = System.currentTimeMillis();
		System.out.println("DEBUG: finished verifying t = " + ((double)(t - t0) / 1000) + "s");
	}

	private int estimateBytesRead() {
		int bytesRead = 0;
		for (short tag: fileStreams.keySet()) {
			CardFileInputStream in = fileStreams.get(tag);
			bytesRead += in.getFilePos();
		}
		return bytesRead;
	}

	public void createEmptyPassport() {
		try {
			/* EF.COM */
			int[] tagList = { PassportFile.EF_DG1_TAG, PassportFile.EF_DG2_TAG };
			COMFile comFile = new COMFile("01", "07", "04", "00", "00", tagList);
			byte[] comBytes = comFile.getEncoded();

			/* EF.DG1 */
			Date today = Calendar.getInstance().getTime();
			String primaryIdentifier = "";
			String[] secondaryIdentifiers = { "" };
			MRZInfo mrzInfo = new MRZInfo(MRZInfo.DOC_TYPE_ID1, Country.NL, primaryIdentifier, secondaryIdentifiers, "", Country.NL, today, Gender.MALE, today, "");
			DG1File dg1File = new DG1File(mrzInfo);
			byte[] dg1Bytes = dg1File.getEncoded();

			/* EF.DG2 */
			DG2File dg2File = new DG2File(); 
			byte[] dg2Bytes = dg2File.getEncoded();

			/* EF.SOD */
			// FIXME: docSigningCert == null, generate something here...
			Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
			String digestAlgorithm = "SHA256";
			String signatureAlgorithm = "SHA256withRSA";
			MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
			hashes.put(1, digest.digest(dg1Bytes));
			hashes.put(2, digest.digest(dg2Bytes));
			byte[] encryptedDigest = new byte[128]; // Arbitrary value. Should be use a private key to generate a real signature?
			SODFile sodFile = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, encryptedDigest, docSigningCert);
			byte[] sodBytes = sodFile.getEncoded();

			putFile(PassportService.EF_COM, comBytes);
			putFile(PassportService.EF_DG1, dg1Bytes);
			putFile(PassportService.EF_DG2, dg2Bytes);
			putFile(PassportService.EF_SOD, sodBytes);

			displayInputStreams();

			verifySecurity(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readFromZipFile(File file) throws IOException {
		final ZipFile zipIn = new ZipFile(file);
		try {
			Enumeration<? extends ZipEntry> entries = zipIn.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry == null) { break; }
				String fileName = entry.getName();
				int size = (int)(entry.getSize() & 0x00000000FFFFFFFFL);
				try {
					short fid = Hex.hexStringToShort(fileName.substring(0, fileName.indexOf('.')));
					byte[] bytes = new byte[size];
					DataInputStream dataIn = new DataInputStream(zipIn.getInputStream(entry));
					dataIn.readFully(bytes);
					putFile(fid, bytes);
				} catch (NumberFormatException nfe) {
					/* NOTE: ignore this file */
				}
			}

			displayInputStreams();

			verifySecurity(null);

		} catch (IOException ioe) {
			ioe.printStackTrace();
			dispose();
		}
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
	}

	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}


	/**
	 * Reads the datagroups and adds them to the GUI.
	 * Assumes inputstreams in <code>passportFiles</code> are reset to beginning.
	 */
	private void displayInputStreams() {
		try {
			InputStream in = null;
			in = getFile(PassportService.EF_DG1);
			dg1 = new DG1File(in);
			final HolderInfoPanel holderInfoPanel = new HolderInfoPanel(dg1);
			final MRZPanel mrzPanel = new MRZPanel(dg1);
			centerPanel.add(holderInfoPanel, BorderLayout.CENTER);
			centerPanel.add(mrzPanel, BorderLayout.SOUTH);
			centerPanel.revalidate();
			centerPanel.repaint();
			holderInfoPanel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					MRZInfo info = holderInfoPanel.getMRZ();
					mrzPanel.setMRZ(info);
					dg1 = new DG1File(info);
					putFile(PassportService.EF_DG1, dg1.getEncoded());
					isBACVerified = false;
					isAAVerified = false;
					verificationPanel.setBACState(VerificationIndicator.VERIFICATION_UNKNOWN);
					verificationPanel.setAAState(VerificationIndicator.VERIFICATION_UNKNOWN);
					verificationPanel.setDSState(VerificationIndicator.VERIFICATION_UNKNOWN);
					verificationPanel.setCSState(VerificationIndicator.VERIFICATION_UNKNOWN);
				}
			});

			for (short fid: bufferedStreams.keySet()) {
				in = getFile(fid);
				in.reset();
				switch (fid) {
				case PassportService.EF_DG1:
					break;
				case PassportService.EF_DG2:
					dg2 = new DG2File(in);
					facePreviewPanel.addFaces(dg2.getFaces());
					break;
				case PassportService.EF_DG15:
					dg15 = new DG15File(in);
					break;
				case PassportService.EF_COM:
					break;
				case PassportService.EF_SOD:
					break;
				default: System.out.println("WARNING: datagroup not yet supported " + Hex.shortToHexString(fid));
				}
				in.reset();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void putFile(short fid, PassportService service) throws CardServiceException, IOException {
		if (!bufferedStreams.containsKey(fid)) {
			CardFileInputStream in = service.readFile(fid);
			int length = in.getFileLength();
			InputStream bufferedIn = new BufferedInputStream(in, length + 1);
			bufferedIn.mark(in.available() + 2);
			fileStreams.put(fid, in);
			bufferedStreams.put(fid, bufferedIn);
			totalLength += length;
		} 
	}

	private void putFile(short fid, byte[] bytes) {
		fileStreams.put(fid, null);
		if (bytes != null) {
			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
			bufferedStreams.put(fid, in);
		}
	}

	/**
	 * Gets an inputstream that is ready for reading. Makes sure it is reset.
	 * 
	 * @param fid
	 * @return
	 */
	private InputStream getFile(short fid) {
		try {
			InputStream in = bufferedStreams.get(fid);
			if (in != null) {
				in.reset(); 
			}
			return in;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalStateException("ERROR: " + ioe.toString());
		}
	}

	private byte[] getFileBytes(short fid) {
		InputStream in = getFile(fid);
		if (in == null) { return null; }
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[256];
		while (true)
			try {
				int bytesRead = in.read(buf);
				if (bytesRead < 0) { break; }
				out.write(buf, 0, bytesRead);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return out.toByteArray();
	}

	private void verifySecurity(PassportService service) {

		/* Check whether BAC was used */
		verificationPanel.setBACState(isBACVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_FAILED);

		/* Check active authentication */
		try {
			InputStream dg15In = getFile(PassportService.EF_DG15);
			if (dg15In != null && service != null) {
				dg15 = new DG15File(dg15In);
				PublicKey pubKey = dg15.getPublicKey();
				isAAVerified = service.doAA(pubKey);
			}
			verificationPanel.setAAState(isAAVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_FAILED);
			docSigningCert = null;
			countrySigningCert = null;
		} catch (IOException ioe) {
			verificationPanel.setAAState(VerificationIndicator.VERIFICATION_UNKNOWN);
		} catch (CardServiceException cse) {
			verificationPanel.setAAState(VerificationIndicator.VERIFICATION_UNKNOWN);
		}

		/* Check hashes in the SOd correspond to hashes we compute. */
		try {
			InputStream comIn = getFile(PassportService.EF_COM);
			COMFile comFile = new COMFile(comIn);
			int[] tags = comFile.getTagList();

			InputStream sodIn = getFile(PassportService.EF_SOD);
			SODFile sodFile = new SODFile(sodIn);
			Map<Integer, byte[]> hashes = sodFile.getDataGroupHashes();

			if (tags.length != hashes.size()) {
				System.err.println("WARNING: \"Jeroen van Beek sanity check\" failed!");
			}

			String algorithm = sodFile.getDigestAlgorithm();
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			isDSVerified = true;
			for (int dgNumber: hashes.keySet()) {
				int dgTag = PassportFile.lookupTagByDataGroupNumber(dgNumber);
				short dgFID = PassportFile.lookupFIDByTag(dgTag);
				byte[] storedHash = hashes.get(dgNumber);

				digest.reset();

				InputStream dgIn = bufferedStreams.get(dgFID);
				dgIn.reset();
				byte[] buf = new byte[1024];
				while (true) {
					int bytesRead = dgIn.read(buf);
					if (bytesRead < 0) { break; }
					digest.update(buf, 0, bytesRead);
				}
				byte[] computedHash = digest.digest();
				if (!Arrays.equals(storedHash, computedHash)) {
					isDSVerified = false;
					break;
				}
			}

			docSigningCert = sodFile.getDocSigningCertificate();
			isDSVerified &= sodFile.checkDocSignature(docSigningCert);
		} catch (NoSuchAlgorithmException nsae) {
			isDSVerified = false;
			nsae.printStackTrace();
		} catch (Exception ioe) {
			isDSVerified = false;
			ioe.printStackTrace();
		}
		verificationPanel.setDSState(isDSVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_FAILED);

		/* Check country signer certificate, if known. */
		try {
			if (docSigningCert == null) { throw new IllegalStateException("Cannot check CSCA if DS failed"); }
			issuingState = null;
			InputStream dg1In = getFile(PassportService.EF_DG1);
			if (dg1In != null) {
				DG1File dg1 = new DG1File(dg1In);
				MRZInfo mrzInfo = dg1.getMRZInfo();
				issuingState = mrzInfo.getIssuingState();
			}
			URL baseDir = Files.getBaseDir();
			URL cscaDir = new URL(baseDir + "/csca");
			/* TODO: also check .pem, .der formats? */
			URL cscaFile = new URL(cscaDir + "/" + issuingState.toString().toLowerCase() + ".cer");
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			InputStream cscaIn = cscaFile.openStream();
			if (cscaIn == null) {
				throw new IllegalStateException("CSCA check failed");
			}
			countrySigningCert = (X509Certificate)certFactory.generateCertificate(cscaIn);
			docSigningCert.verify(countrySigningCert.getPublicKey());
			verificationPanel.setCSState(VerificationIndicator.VERIFICATION_SUCCEEDED);

		} catch (FileNotFoundException fnfe) {
			verificationPanel.setCSState(VerificationIndicator.VERIFICATION_UNKNOWN);
		} catch (CertificateException e) {
			verificationPanel.setCSState(VerificationIndicator.VERIFICATION_FAILED);
		} catch (GeneralSecurityException gse) {
			verificationPanel.setCSState(VerificationIndicator.VERIFICATION_FAILED);
			gse.printStackTrace();
		} catch (IOException ioe) {
			verificationPanel.setCSState(VerificationIndicator.VERIFICATION_UNKNOWN);
		} catch (Exception e) {
			verificationPanel.setCSState(VerificationIndicator.VERIFICATION_FAILED);
		}

		verificationPanel.revalidate();
	}

	/* Menu stuff... */

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Save As...*/
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		fileMenu.add(saveAsItem);
		saveAsItem.setAction(new SaveAsAction());

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(new CloseAction());

		return fileMenu;
	}

	private JMenu createViewMenu() {
		JMenu menu = new JMenu("View");

		/* View portrait at full size... */
		JMenuItem viewImageAtOriginalSize = new JMenuItem();
		menu.add(viewImageAtOriginalSize);
		viewImageAtOriginalSize.setAction(new ViewPortraitAtOriginalSizeAction());

		menu.addSeparator();

		/* View DS Certificate... */
		JMenuItem viewDocumentSignerCertificate = new JMenuItem();
		menu.add(viewDocumentSignerCertificate);
		viewDocumentSignerCertificate.setAction(new ViewDocumentSignerCertificateAction());

		/* View CS Certificate... */
		JMenuItem viewCountrySignerCertificate = new JMenuItem();
		menu.add(viewCountrySignerCertificate);
		viewCountrySignerCertificate.setAction(new ViewCountrySignerCertificateAction());

		/* View AA public key */
		JMenuItem viewAAPublicKey = new JMenuItem();
		menu.add(viewAAPublicKey);
		viewAAPublicKey.setAction(new ViewAAPublicKeyAction());

		return menu;
	}

	private JMenu createToolsMenu() {
		JMenu menu = new JMenu("Tools");

		/* Load additional portrait from file... */
		JMenuItem loadPortraitFromFile = new JMenuItem();
		menu.add(loadPortraitFromFile);
		loadPortraitFromFile.setAction(new AddPortraitAction());

		/* Delete selected portrait */
		JMenuItem deletePortrait = new JMenuItem();
		menu.add(deletePortrait);
		deletePortrait.setAction(new RemovePortraitAction());

		menu.addSeparator();

		/* Replace DSC with another certificate from file... */
		JMenuItem loadDocSignCertFromFile = new JMenuItem();
		menu.add(loadDocSignCertFromFile);
		loadDocSignCertFromFile.setAction(new LoadDocSignCertAction());

		/* Replace AA key with another key from file... */
		JMenuItem loadAAKeyFromFile = new JMenuItem();
		menu.add(loadAAKeyFromFile);
		loadAAKeyFromFile.setAction(new LoadAAKeyAction());

		menu.addSeparator();

		JMenuItem upload = new JMenuItem();
		menu.add(upload);
		upload.setAction(getUploadAction());

		return menu;
	}

	private class CloseAction extends AbstractAction
	{
		public CloseAction() {
			putValue(SMALL_ICON, CLOSE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, CLOSE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Close Window");
			putValue(NAME, "Close");
		}

		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}

	private class SaveAsAction extends AbstractAction
	{
		public SaveAsAction() {
			putValue(SMALL_ICON, SAVE_AS_SMALL_ICON);
			putValue(LARGE_ICON_KEY, SAVE_AS_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Save passport to file");
			putValue(NAME, "Save As...");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith("zip") || f.getName().endsWith("ZIP"); }
				public String getDescription() { return "ZIP archives"; }				
			});
			int choice = fileChooser.showSaveDialog(getContentPane());
			switch (choice) {
			case JFileChooser.APPROVE_OPTION:
				try {
					File file = fileChooser.getSelectedFile();
					FileOutputStream fileOut = new FileOutputStream(file);
					ZipOutputStream zipOut = new ZipOutputStream(fileOut);
					for (short tag: bufferedStreams.keySet()) {
						String entryName = Hex.shortToHexString(tag) + ".bin";
						InputStream dg = getFile(tag);
						zipOut.putNextEntry(new ZipEntry(entryName));
						int bytesRead;
						byte[] dgBytes = new byte[1024];
						while((bytesRead = dg.read(dgBytes)) > 0){
							zipOut.write(dgBytes, 0, bytesRead);
						}
						zipOut.closeEntry();
					}
					zipOut.finish();
					zipOut.close();
					fileOut.flush();
					fileOut.close();						
					break;
				} catch (IOException fnfe) {
					fnfe.printStackTrace();
				}
			default: break;
			}
		}
	}

	private class ViewPortraitAtOriginalSizeAction extends AbstractAction
	{
		public ViewPortraitAtOriginalSizeAction() {
			putValue(SMALL_ICON, MAGNIFIER_SMALL_ICON);
			putValue(LARGE_ICON_KEY, MAGNIFIER_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View portrait image at original size");
			putValue(NAME, "Portrait at 100%...");
		}

		public void actionPerformed(ActionEvent e) {
			int index = facePreviewPanel.getSelectedIndex();
			if (dg2 == null) {
				InputStream dg2In = getFile(PassportService.EF_DG2);
				dg2 = new DG2File(dg2In);
			}
			FaceInfo faceInfo = dg2.getFaces().get(index);
			PortraitFrame portraitFrame = new PortraitFrame(faceInfo);
			portraitFrame.setVisible(true);
			portraitFrame.pack();
		}
	}

	private class AddPortraitAction extends AbstractAction
	{
		public AddPortraitAction() {
			putValue(SMALL_ICON, LOAD_IMAGE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, LOAD_IMAGE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Import (additional) portrait from file");
			putValue(NAME, "Import portrait...");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory()
					|| f.getName().endsWith("jpg") || f.getName().endsWith("JPG")
					|| f.getName().endsWith("png") || f.getName().endsWith("PNG")
					|| f.getName().endsWith("bmp") || f.getName().endsWith("BMP"); }
				public String getDescription() { return "Image files"; }				
			});
			int choice = fileChooser.showOpenDialog(getContentPane());
			switch (choice) {
			case JFileChooser.APPROVE_OPTION:
				try {
					File file = fileChooser.getSelectedFile();
					BufferedImage image = ImageIO.read(file);

					FaceInfo faceInfo = new FaceInfo(
							Gender.UNSPECIFIED,
							FaceInfo.EyeColor.UNSPECIFIED,
							FaceInfo.HAIR_COLOR_UNSPECIFIED,
							FaceInfo.EXPRESSION_UNSPECIFIED,
							FaceInfo.SOURCE_TYPE_UNSPECIFIED,
							image);
					if (dg2 == null) {
						InputStream dg2In = getFile(PassportService.EF_DG2);
						dg2 = new DG2File(dg2In);
					}
					dg2.addFaceInfo(faceInfo);
					putFile(PassportService.EF_DG2, dg2.getEncoded());
					facePreviewPanel.addFace(faceInfo);
				} catch (IOException ioe) {
					/* NOTE: Do nothing. */
				}
				break;
			default:
				break;
			}
		}
	}

	private class RemovePortraitAction extends AbstractAction
	{
		public RemovePortraitAction() {
			putValue(SMALL_ICON, DELETE_IMAGE_LARGE_ICON);
			putValue(LARGE_ICON_KEY, DELETE_IMAGE_SMALL_ICON);
			putValue(SHORT_DESCRIPTION, "Delete selected portrait");
			putValue(NAME, "Delete portrait");
		}

		public void actionPerformed(ActionEvent e) {
			int index = facePreviewPanel.getSelectedIndex();
			dg2.removeFaceInfo(index);
			putFile(PassportService.EF_DG2, dg2.getEncoded());
			facePreviewPanel.removeFace(index);
		}
	}

	private class LoadDocSignCertAction extends AbstractAction
	{
		public LoadDocSignCertAction() {
			putValue(SMALL_ICON, LOAD_CERT_SMALL_ICON);
			putValue(LARGE_ICON_KEY, LOAD_CERT_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Import (and replace) Document Signer Certificate from file");
			putValue(NAME, "Import Doc.Cert...");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory()
					|| f.getName().endsWith("pem") || f.getName().endsWith("PEM")
					|| f.getName().endsWith("cer") || f.getName().endsWith("CER")
					|| f.getName().endsWith("der") || f.getName().endsWith("DER")
					|| f.getName().endsWith("crt") || f.getName().endsWith("CRT")
					|| f.getName().endsWith("cert") || f.getName().endsWith("CERT"); }
				public String getDescription() { return "Certificate files"; }				
			});
			int choice = fileChooser.showOpenDialog(getContentPane());
			switch (choice) {
			case JFileChooser.APPROVE_OPTION:
				try {
					File file = fileChooser.getSelectedFile();
					throw new IOException("TODO: do something with " + file);
				} catch (IOException ioe) {
					/* NOTE: Do nothing. */
				}
				break;
			default:
				break;
			}
		}
	}

	private class LoadAAKeyAction extends AbstractAction
	{
		public LoadAAKeyAction() {
			putValue(SMALL_ICON, LOAD_KEY_SMALL_ICON);
			putValue(LARGE_ICON_KEY, LOAD_KEY_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "Import (and replace) Active Authentication public key from file");
			putValue(NAME, "Import AA Pub.Key...");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory()
					|| f.getName().endsWith("cer") || f.getName().endsWith("CER")
					|| f.getName().endsWith("der") || f.getName().endsWith("DER")
					|| f.getName().endsWith("x509") || f.getName().endsWith("X509")
					|| f.getName().endsWith("pkcs8") || f.getName().endsWith("PKCS8"); }
				public String getDescription() { return "Key files"; }								
			});
			int choice = fileChooser.showOpenDialog(getContentPane());
			switch (choice) {
			case JFileChooser.APPROVE_OPTION:
				try {
					File file = fileChooser.getSelectedFile();
					throw new IOException("TODO: do something with " + file);
				} catch (IOException ioe) {
					/* NOTE: Do nothing. */
				}
				break;
			default:
				break;
			}
		}
	}

	private class ViewDocumentSignerCertificateAction extends AbstractAction
	{
		public ViewDocumentSignerCertificateAction() {
			putValue(SMALL_ICON, CERTIFICATE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, CERTIFICATE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View Document Signer Certificate");
			putValue(NAME, "Doc. Cert...");
		}

		public void actionPerformed(ActionEvent e) {
			JFrame certificateFrame = new CertificateFrame("Document Signer Certificate", docSigningCert);
			certificateFrame.pack();
			certificateFrame.setVisible(true);
		}
	}

	private class ViewCountrySignerCertificateAction extends AbstractAction
	{
		public ViewCountrySignerCertificateAction() {
			putValue(SMALL_ICON, CERTIFICATE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, CERTIFICATE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View Country Signer Certificate");
			putValue(NAME, "CSCA Cert...");
		}

		public void actionPerformed(ActionEvent e) {
			if (countrySigningCert == null) {
				JOptionPane.showMessageDialog(getContentPane(), "CSCA for " + issuingState.getName() + " not found", "CSCA not found...", JOptionPane.ERROR_MESSAGE);
			} else {
				JFrame certificateFrame = new CertificateFrame("Country Signer Certificate (" + issuingState + ", from file)", countrySigningCert);
				certificateFrame.pack();
				certificateFrame.setVisible(true);
			}
		}
	}

	private class ViewAAPublicKeyAction extends AbstractAction
	{
		public ViewAAPublicKeyAction() {
			putValue(SMALL_ICON, KEY_SMALL_ICON);
			putValue(LARGE_ICON_KEY, KEY_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View Active Authentication Public Key");
			putValue(NAME, "AA Pub. Key...");
		}

		public void actionPerformed(ActionEvent e) {
			try {
				InputStream in = getFile(PassportService.EF_DG15);
				dg15 = new DG15File(in);
				PublicKey pubKey = dg15.getPublicKey();
				KeyFrame keyFrame = new KeyFrame("Active Authentication Public Key", pubKey);
				keyFrame.pack();
				keyFrame.setVisible(true);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private Action getUploadAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				CardManager cm = CardManager.getInstance();
				cm.stop();
				BACEntry bacEntry = null;
				if (dg1 != null) {
					MRZInfo mrzInfo = dg1.getMRZInfo();
					bacEntry = new BACEntry(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());
				}
				PublicKey aaPublicKey = null;
				if (dg15 != null) {
					aaPublicKey = dg15.getPublicKey();
				}
				UploadOptionsChooser chooser = new UploadOptionsChooser(bacEntry, aaPublicKey);
				int choice = chooser.showOptionsDialog(getContentPane());
				switch (choice) {
				case UploadOptionsChooser.APPROVE_OPTION:
					try {
						CardTerminal terminal = chooser.getSelectedTerminal();
						PassportPersoService persoService = new PassportPersoService(new TerminalCardService(terminal));
						persoService.open();
						if (chooser.isBACSelected()) {
							persoService.setBAC(bacEntry.getDocumentNumber(), bacEntry.getDateOfBirth(), bacEntry.getDateOfExpiry());
						}
						if (chooser.isAASelected()) {
							persoService.putPrivateKey(chooser.getAAPrivateKey());
						}
						for (short fid: bufferedStreams.keySet()) {
							byte[] fileBytes = getFileBytes(fid); 
							persoService.createFile(fid, (short)fileBytes.length);
							persoService.selectFile(fid);
							persoService.writeFile(fid, new ByteArrayInputStream(fileBytes));
						}
						persoService.lockApplet();
						persoService.close();
					} catch (IOException ioe) {
						/* NOTE: Do nothing. */
					} catch (CardServiceException cse) {
						cse.printStackTrace();
					} catch (GeneralSecurityException gse) {
						gse.printStackTrace();
					} finally {
						cm.start();
					}
					break;
				default:
					break;
				}
				cm.start();
			}			
		};
		action.putValue(Action.SMALL_ICON, UPLOAD_SMALL_ICON);
		action.putValue(Action.LARGE_ICON_KEY, UPLOAD_LARGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Upload this passport to a passport applet");
		action.putValue(Action.NAME, "Upload passport...");
		return action;
	}
}
