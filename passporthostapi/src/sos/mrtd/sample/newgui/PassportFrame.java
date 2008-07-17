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
 * $Id: PassportGUI.java 308 2008-01-23 11:16:00Z martijno $
 */

package sos.mrtd.sample.newgui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.AbstractAction;
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

import org.bouncycastle.asn1.icao.DataGroupHash;

import sos.data.Country;
import sos.mrtd.COMFile;
import sos.mrtd.DG15File;
import sos.mrtd.DG1File;
import sos.mrtd.DG2File;
import sos.mrtd.FaceInfo;
import sos.mrtd.MRZInfo;
import sos.mrtd.PassportFile;
import sos.mrtd.PassportService;
import sos.mrtd.SODFile;
import sos.smartcards.CardFileInputStream;
import sos.smartcards.CardServiceException;
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

	private FacePreviewPanel facePreviewPanel;

	private JPanel panel, centerPanel;

	private Map<Short, CardFileInputStream> fileStreams;
	private Map<Short, InputStream> bufferedStreams;
	private int totalLength;

	private DG1File dg1;
	private DG15File dg15;
	private SODFile sod;
	private COMFile com;

	Certificate docSigningCert, countrySigningCert;

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
//			int i;
//			public void exchangedAPDU(CommandAPDU capdu, ResponseAPDU rapdu) {
//				System.out.println("-> (" + (i++) + ") " + Hex.bytesToHexString(capdu.getBytes()));
//				System.out.println("<- " + Hex.bytesToHexString(rapdu.getBytes()));
//			}
//		});
		try {
			this.isBACVerified = isBACVerified;
			verificationPanel.setBACState(isBACVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_UNKNOWN);
			putFile(PassportService.EF_COM, service);
			putFile(PassportService.EF_SOD, service);
			InputStream comIn = getFile(PassportService.EF_COM);
			COMFile com = new COMFile(comIn);
			int[] tags = com.getTagList();
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

			for (short tag: bufferedStreams.keySet()) {
				in = getFile(tag);
				in.reset();
				switch (tag) {
				case PassportService.EF_DG1:
					break;
				case PassportService.EF_DG2:
					facePreviewPanel.showFaces(in);
					break;
				case PassportService.EF_DG15:
					break;
				case PassportService.EF_COM:
					break;
				case PassportService.EF_SOD:
					break;
				default: System.out.println("WARNING: datagroup not yet supported " + Hex.shortToHexString(tag));
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
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		bufferedStreams.put(fid, in);
	}

	/**
	 * Gets an inputstream that is ready for reading. Makes sure it is reset.
	 * 
	 * @param tag
	 * @return
	 */
	private InputStream getFile(short tag) {
		try {
			InputStream in = bufferedStreams.get(tag);
			in.reset();
			return in;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalStateException("ERROR: " + ioe.toString());
		}
	}

	private void verifySecurity(PassportService service) {

		/* Check whether BAC was used */
		verificationPanel.setBACState(isBACVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_FAILED);

		/* Check active authentication */
		try {
			InputStream dg15In = getFile(PassportService.EF_DG15);
			if (dg15In != null && service != null) {
				DG15File dg15 = new DG15File(dg15In);
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
			COMFile com = new COMFile(comIn);
			int[] tags = com.getTagList();

			InputStream sodIn = getFile(PassportService.EF_SOD);
			SODFile sod = new SODFile(sodIn);
			DataGroupHash[] hashes = sod.getDataGroupHashes();
			isDSVerified = true;
			for (int i = 0; i < hashes.length; i++) {

				byte[] storedHash = hashes[i].getDataGroupHashValue().getOctets();

				/*
				 * TODO: This is a hack...
				 * Find out how to properly parse the sig. alg from the security object.
				 */
				String algorithm = "SHA256";
				if (storedHash != null && storedHash.length == 20) { algorithm = "SHA1"; }
				MessageDigest digest = MessageDigest.getInstance(algorithm);

				InputStream dgIn = bufferedStreams.get(PassportFile.lookupFIDByTag(tags[i]));
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

			docSigningCert = sod.getDocSigningCertificate();
			isDSVerified &= sod.checkDocSignature(docSigningCert);
		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		verificationPanel.setDSState(isDSVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_FAILED);

		/* Check country signer certificate, if known. */
		try {
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
			countrySigningCert = certFactory.generateCertificate(cscaFile.openStream());
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
		JMenu viewMenu = new JMenu("View");

		/* Image full size... */
		JMenuItem viewImageAtOriginalSize = new JMenuItem();
		viewMenu.add(viewImageAtOriginalSize);
		viewImageAtOriginalSize.setAction(new ViewImageAtOriginalSizeAction());

		/* DS Certificate... */
		JMenuItem viewDocumentSignerCertificate = new JMenuItem();
		viewMenu.add(viewDocumentSignerCertificate);
		viewDocumentSignerCertificate.setAction(new ViewDocumentSignerCertificateAction());

		/* CS Certificate... */
		JMenuItem viewCountrySignerCertificate = new JMenuItem();
		viewMenu.add(viewCountrySignerCertificate);
		viewCountrySignerCertificate.setAction(new ViewCountrySignerCertificateAction());

		JMenuItem viewAAPublicKey = new JMenuItem();
		viewMenu.add(viewAAPublicKey);
		viewAAPublicKey.setAction(new ViewAAPublicKeyAction());

		return viewMenu;
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

	private class ViewImageAtOriginalSizeAction extends AbstractAction
	{
		public ViewImageAtOriginalSizeAction() {
			putValue(SMALL_ICON, MAGNIFIER_SMALL_ICON);
			putValue(LARGE_ICON_KEY, MAGNIFIER_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View image at original size");
			putValue(NAME, "Image at 100%...");
		}

		public void actionPerformed(ActionEvent e) {
			int index = facePreviewPanel.getSelectedIndex();
			InputStream dg2In = getFile(PassportService.EF_DG2);
			DG2File dg2File = new DG2File(dg2In);
			FaceInfo faceInfo = dg2File.getFaces().get(index);
			FaceFrame faceFrame = new FaceFrame(faceInfo);
			faceFrame.setVisible(true);
			faceFrame.pack();
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
				DG15File file = new DG15File(in);
				PublicKey pubKey = file.getPublicKey();
				KeyFrame keyFrame = new KeyFrame("Active Authentication Public Key", pubKey);
				keyFrame.pack();
				keyFrame.setVisible(true);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
