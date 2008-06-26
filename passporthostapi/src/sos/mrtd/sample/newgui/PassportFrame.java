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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

import org.bouncycastle.asn1.icao.DataGroupHash;

import sos.gui.Icons;
import sos.mrtd.COMFile;
import sos.mrtd.DG15File;
import sos.mrtd.DG1File;
import sos.mrtd.DG2File;
import sos.mrtd.FaceInfo;
import sos.mrtd.PassportFile;
import sos.mrtd.PassportService;
import sos.mrtd.SODFile;
import sos.smartcards.CardServiceException;
import sos.util.Hex;

/**
 * Displays a passport while (and after) it is being read.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 308 $
 */
public class PassportFrame extends JFrame
{
	private static final String PASSPORT_FRAME_TITLE = "JMRTD - Passport";
	private static final Dimension PREFERRED_SIZE = new Dimension(500, 420);
	
	private static final Icon CERTIFICATE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("rosette"));
	private static final Icon CERTIFICATE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("rosette"));
	private static final Icon MAGNIFIER_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("magnifier"));
	private static final Icon MAGNIFIER_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("magnifier"));
	private static final Icon SAVE_AS_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon SAVE_AS_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_SMALL_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon CLOSE_LARGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));

	private JPanel panel, centerPanel;
	private Map<Short, InputStream> passportFiles;

	private DG1File dg1;
	private DG15File dg15;
	private SODFile sod;
	private COMFile com;

	private VerificationIndicator verificationPanel;
	private boolean isBACVerified;
	private boolean isAAVerified;
	private boolean isDSVerified;

	public PassportFrame() {
		super(PASSPORT_FRAME_TITLE);
		verificationPanel = new VerificationIndicator();
		panel = new JPanel(new BorderLayout());
		centerPanel = new JPanel(new BorderLayout());
		panel.add(centerPanel, BorderLayout.CENTER);
		panel.add(verificationPanel, BorderLayout.SOUTH);
		passportFiles = new HashMap<Short, InputStream>();
		getContentPane().add(panel);
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		setIconImage(Icons.getImage("jmrtd_icon"));
	}

	/**
	 * Constructs the GUI based on a passport service.
	 *
	 * @param service the service
	 * 
	 * @return a passport frame.
	 */
	public void readFromService(PassportService service, boolean isBACVerified) throws CardServiceException {
		final PassportService s = service;
		try {
			this.isBACVerified = isBACVerified;
			bufferFile(PassportService.EF_COM, service);
			bufferFile(PassportService.EF_SOD, service);
			InputStream comIn = passportFiles.get(PassportService.EF_COM);
			COMFile com = new COMFile(comIn);
			comIn.reset();
			int[] tags = com.getTagList();
			for (int i = 0; i < tags.length; i++) {
				bufferFile(PassportFile.lookupFIDByTag(tags[i]), service);
			}



		} catch (CardServiceException cse) {
			cse.printStackTrace();
			dispose();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		displayInputStreams();

		try {
			verifySecurity(s);
		} catch (CardServiceException cse) {
			cse.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private InputStream bufferFile(short fid, PassportService service) throws CardServiceException, IOException {
		InputStream in = service.readFile(fid);
		InputStream bufferedIn = new BufferedInputStream(in, in.available() + 1);
		bufferedIn.mark(in.available() + 2);
		passportFiles.put(fid, bufferedIn);
		return bufferedIn;
	}

	public void readFromZipFile(File file) throws IOException {
		final ZipFile zipIn = new ZipFile(file);
		(new Thread(new Runnable() {
			public void run() {
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
							passportFiles.put(fid, new ByteArrayInputStream(bytes));
						} catch (NumberFormatException nfe) {
							/* NOTE: ignore this file */
						}
					}
					displayInputStreams();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					dispose();
				}
			}
		})).start();
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
			for (short id: passportFiles.keySet()) {
				InputStream in = passportFiles.get(id);
				in.reset();
				switch (id) {
				case PassportService.EF_DG1:
					dg1 = new DG1File(in);
					centerPanel.add(new HolderInfoPanel(dg1), BorderLayout.CENTER);
					centerPanel.add(new MRZPanel(dg1), BorderLayout.SOUTH);
					centerPanel.revalidate();
					break;
				case PassportService.EF_DG2:
					centerPanel.add(new FacePanel(in, 160, 200), BorderLayout.WEST);
					panel.revalidate();
					break;
				case PassportService.EF_DG15:
					dg15 = new DG15File(in);
					break;
				case PassportService.EF_COM:
					com = new COMFile(in);
					break;
				case PassportService.EF_SOD:
					sod = new SODFile(in);
					break;
				default: System.out.println("WARNING: datagroup not yet supported " + Hex.shortToHexString(id));
				}
				in.reset();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void verifySecurity(PassportService service)
	throws IOException, CardServiceException {

		/* Check whether BAC was used */
		verificationPanel.setBACState(isBACVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_FAILED);

		/* Check active authentication */
		InputStream dg15In = passportFiles.get(PassportService.EF_DG15);
		dg15In.reset();
		if (dg15In != null) {
			DG15File dg15 = new DG15File(dg15In);
			PublicKey pubKey = dg15.getPublicKey();
			dg15In.reset();
			isAAVerified = service.doAA(pubKey);
		}
		verificationPanel.setAAState(isAAVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_FAILED);

		/* Check hashes signed by document signer */
		try {
			InputStream comIn = passportFiles.get(PassportService.EF_COM);
			comIn.reset();
			COMFile com = new COMFile(comIn);
			int[] tags = com.getTagList();

			InputStream sodIn = passportFiles.get(PassportService.EF_SOD);
			sodIn.reset();
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

				InputStream dgIn = passportFiles.get(PassportFile.lookupFIDByTag(tags[i]));
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

		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		verificationPanel.setDSState(isDSVerified ? VerificationIndicator.VERIFICATION_SUCCEEDED : VerificationIndicator.VERIFICATION_FAILED);

		verificationPanel.revalidate();
	}

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
					for (short id: passportFiles.keySet()) {
						String entryName = Hex.shortToHexString(id) + ".bin";
						InputStream dg = passportFiles.get(id);
						try { dg.reset(); } catch (IOException ioe) { System.out.println("DEBUG: entryName = " + entryName); }
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
			InputStream dg2In = passportFiles.get(PassportService.EF_DG2);
			try { dg2In.reset(); } catch (IOException ioe) { ioe.printStackTrace(); }
			DG2File dg2File = new DG2File(dg2In);
			FaceInfo face = dg2File.getFaces().get(0);
			Image image = face.getImage();
			JLabel label = new JLabel(new ImageIcon(image));
			JOptionPane.showMessageDialog(getContentPane(), label, "Portrait", JOptionPane.PLAIN_MESSAGE, null);
		}
	}

	private class ViewDocumentSignerCertificateAction extends AbstractAction
	{
		public ViewDocumentSignerCertificateAction() {
			putValue(SMALL_ICON, CERTIFICATE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, CERTIFICATE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View Document Signer Certificate");
			putValue(NAME, "DS Cert...");
		}

		public void actionPerformed(ActionEvent e) {
			try {

				InputStream sodIn = passportFiles.get(PassportService.EF_SOD);
				sodIn.reset();
				SODFile sodFile = new SODFile(sodIn);
				Certificate cert = sodFile.getDocSigningCertificate();
				JTextArea textArea = new JTextArea(cert.toString(), 20, 40);
				JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(textArea), "Document Signer Certificate", JOptionPane.PLAIN_MESSAGE, null);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (CertificateException ce) {
				ce.printStackTrace();
			} catch (CardServiceException cse) {
				cse.printStackTrace();
			}

		}
	}

	private class ViewCountrySignerCertificateAction extends AbstractAction
	{
		public ViewCountrySignerCertificateAction() {
			putValue(SMALL_ICON, CERTIFICATE_SMALL_ICON);
			putValue(LARGE_ICON_KEY, CERTIFICATE_LARGE_ICON);
			putValue(SHORT_DESCRIPTION, "View Country Signer Certificate");
			putValue(NAME, "CS Cert...");
		}

		public void actionPerformed(ActionEvent e) {
		}
	}

}
