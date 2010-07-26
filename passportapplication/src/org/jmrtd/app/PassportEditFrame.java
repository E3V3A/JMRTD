/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2010  The JMRTD team
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

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.scuba.data.Gender;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;
import net.sourceforge.scuba.swing.ImagePanel;
import net.sourceforge.scuba.util.Files;
import net.sourceforge.scuba.util.Hex;
import net.sourceforge.scuba.util.Icons;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.BACKeySpec;
import org.jmrtd.EACEvent;
import org.jmrtd.Passport;
import org.jmrtd.PassportPersoService;
import org.jmrtd.PassportService;
import org.jmrtd.TrustStore;
import org.jmrtd.app.PreferencesPanel.ReadingMode;
import org.jmrtd.cvc.CVCertificate;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.DG11File;
import org.jmrtd.lds.DG12File;
import org.jmrtd.lds.DG14File;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.DG3File;
import org.jmrtd.lds.DG4File;
import org.jmrtd.lds.DG5File;
import org.jmrtd.lds.DG6File;
import org.jmrtd.lds.DG7File;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.FingerInfo;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.PassportFile;
import org.jmrtd.lds.SODFile;

/**
 * Frame for editing a passport.
 * 
 * FIXME: Under construction.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 894 $
 */
public class PassportEditFrame extends JFrame
{
	private static final long serialVersionUID = -4624658204371014128L;

	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48", PassportEditFrame.class);
	private static final String PASSPORT_FRAME_TITLE = "JMRTD - Passport";
	private static final Dimension PREFERRED_SIZE = new Dimension(540, 420);

	private static final Icon CERTIFICATE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("script_key"));
	private static final Icon FINGERPRINT_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("shading"));
	private static final Icon KEY_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("key"));
	private static final Icon KEY_GO = new ImageIcon(Icons.getFamFamFamSilkIcon("key_go"));
	private static final Icon MAGNIFIER_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("magnifier"));
	private static final Icon SAVE_AS_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon LOAD_IMAGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_image"));
	private static final Icon DELETE_IMAGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("image_delete"));
	private static final Icon LOAD_CERT_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_page_white"));
	private static final Icon LOAD_KEY_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_key"));
	private static final Icon UPLOAD_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_burn"));

	private Logger logger = Logger.getLogger(getClass().getSimpleName());

	private DisplayPreviewPanel displayPreviewPanel;

	private JPanel panel, centerPanel, southPanel;
	private JProgressBar progressBar;
	private JMenu viewMenu;

	private Passport passport;

	private EACEvent eacEvent;

	private VerificationIndicator verificationIndicator;

	public PassportEditFrame(Passport passport, ReadingMode readingMode) {
		super(PASSPORT_FRAME_TITLE);
		logger.setLevel(Level.ALL);
		this.passport = passport;
		verificationIndicator = new VerificationIndicator();
		panel = new JPanel(new BorderLayout());
		centerPanel = new JPanel(new BorderLayout());
		southPanel = new JPanel();
		progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
		panel.add(centerPanel, BorderLayout.CENTER);
		southPanel.add(verificationIndicator);
		southPanel.add(progressBar);
		panel.add(southPanel, BorderLayout.SOUTH);
		displayPreviewPanel = new DisplayPreviewPanel(160, 200);
		displayPreviewPanel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() > 1) {
					viewPreviewImageAtOriginalSize();
				}
			}
		});
		centerPanel.add(displayPreviewPanel, BorderLayout.WEST);
		getContentPane().add(panel);
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createToolsMenu());
		setIconImage(JMRTD_ICON);
		pack();
		setVisible(true);

		try {
			passport.addAuthenticationListener(new AuthListener() {
				public void performedAA(AAEvent ae) { /* NOTE: do nothing */ }

				public void performedBAC(BACEvent be) { /* NOTE: do nothing */ }

				public void performedEAC(EACEvent ee) {
					eacEvent = ee;
					updateViewMenu();
				}
			});
			long t = System.currentTimeMillis();
			logger.info("time: " + Integer.toString((int)(System.currentTimeMillis() - t) / 1000));

			displayProgressBar();
			switch (readingMode) {
			case SAFE_MODE:
				passport.verifySecurity(); // blocks
				verificationIndicator.setStatus(passport.getVerificationStatus());
				displayInputStreams(false);
				break;
			case PROGRESSIVE_MODE:
				displayInputStreams(true);
				break;
			}
			passport.verifySecurity();
			verificationIndicator.setStatus(passport.getVerificationStatus());
			logger.info("time: " + Integer.toString((int)(System.currentTimeMillis() - t)/1000));
		} catch (Exception e) {
			e.printStackTrace();
			dispose();
			return;
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
	private void displayInputStreams(boolean isProgressiveMode) {
		try {
			displayHolderInfo();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		for (short fid: passport.getFileList()) {
			try {
				InputStream in = passport.getInputStream(fid);
				if (in == null) { logger.warning("Got null inputstream while trying to display " + Integer.toHexString(fid & 0xFFFF)); }
				switch (fid) {
				case PassportService.EF_COM:
					/* NOTE: Already processed this one. */
					break;
				case PassportService.EF_DG1:
					/* NOTE: Already processed this one. */
					break;
				case PassportService.EF_DG2:
					DG2File dg2 = new DG2File(in);
					List<FaceInfo> faces = dg2.getFaceInfos();
					for (FaceInfo face: faces) { displayPreviewPanel.addDisplayedImage(face, isProgressiveMode); }
					break;
				case PassportService.EF_DG3:
					DG3File dg3 = new DG3File(in);
					List<FingerInfo> fingers = dg3.getFingerInfos();
					for (FingerInfo finger: fingers) { displayPreviewPanel.addDisplayedImage(finger, isProgressiveMode); }
					break;
				case PassportService.EF_DG4:
					DG4File dg4 = new DG4File(in);
					break;
				case PassportService.EF_DG5:
					DG5File dg5 = new DG5File(in);
					break;
				case PassportService.EF_DG6:
					DG6File dg6 = new DG6File(in);
					break;
				case PassportService.EF_DG7:
					DG7File dg7 = new DG7File(in);
					List<DisplayedImageInfo> infos = dg7.getImages();
					for (DisplayedImageInfo info: infos) { displayPreviewPanel.addDisplayedImage(info, isProgressiveMode); }
					break;
				case PassportService.EF_DG11:
					DG11File dg11 = new DG11File(in);
					break;
				case PassportService.EF_DG12:
					DG12File dg12 = new DG12File(in);
					break;
				case PassportService.EF_DG14:
					DG14File dg14 = new DG14File(in);
					updateViewMenu();
					break;
				case PassportService.EF_DG15:
					DG15File dg15 = new DG15File(in);
					break;
				case PassportService.EF_SOD:
					/* NOTE: Already processed this one above. */
					break;
				case PassportService.EF_CVCA:
					CVCAFile cvca = new CVCAFile(in);
					break;
				default:
					String message = "Displaying of file " + Integer.toHexString(fid) + " not supported!";
					if ((fid & 0x010F) == fid) {
						int tag = PassportFile.lookupTagByFID(fid);
						int dgNumber = PassportFile.lookupDataGroupNumberByTag(tag);
						message = "Displaying of DG" + dgNumber + " not supported!";
					}
					JOptionPane.showMessageDialog(getContentPane(), message, "File not supported", JOptionPane.WARNING_MESSAGE);
				}
			} catch (IOException ioe) {
				String errorMessage = "Exception reading file " + Integer.toHexString(fid) + ": \n"
				+ ioe.getClass().getSimpleName() + "\n" + ioe.getMessage() + "\n";
				JTextArea messageArea = new JTextArea(errorMessage, 5, 15);
				JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(messageArea), "Problem reading file", JOptionPane.WARNING_MESSAGE);
				continue;
			}
		}
	}

	private void updateViewMenu() {
		InputStream dg14in = passport.getInputStream(PassportService.EF_DG14);
		if (dg14in == null) { return; }
		DG14File dg14 = new DG14File(dg14in);
		if (dg14 == null) { return; }
		PrivateKey terminalKey = null;
		List<CVCertificate> cvCertificates = null;
		Map<Integer, PublicKey> publicKeyMap = null;
		int cardPublicKeyId = 0;
		if (eacEvent != null) {
			terminalKey = eacEvent.getTerminalKey();
			cvCertificates = eacEvent.getCVCertificates();
			publicKeyMap = dg14.getPublicKeys();
			cardPublicKeyId = eacEvent.getCardPublicKeyId();
		} // TODO: else { default values }
		createEACMenus(terminalKey, cvCertificates, publicKeyMap, cardPublicKeyId);
	}

	private void displayHolderInfo() throws IOException {
		InputStream dg1In = passport.getInputStream(PassportService.EF_DG1);
		DG1File dg1 = new DG1File(dg1In);
		MRZInfo mrzInfo = dg1.getMRZInfo();
		BACKeySpec bacEntry = passport.getBACKeySpec();
		if (bacEntry != null &&
				!(mrzInfo.getDocumentNumber().equals(bacEntry.getDocumentNumber()) &&
						mrzInfo.getDateOfBirth().equals(bacEntry.getDateOfBirth())) &&
						mrzInfo.getDateOfExpiry().equals(bacEntry.getDateOfExpiry())) {
			JOptionPane.showMessageDialog(getContentPane(), "Problem reading file", "MRZ used in BAC differs from MRZ in DG1!", JOptionPane.WARNING_MESSAGE);
		}
		final HolderEditPanel holderInfoPanel = new HolderEditPanel(mrzInfo);
		final MRZPanel mrzPanel = new MRZPanel(mrzInfo);
		centerPanel.add(holderInfoPanel, BorderLayout.CENTER);
		centerPanel.add(mrzPanel, BorderLayout.SOUTH);
		centerPanel.revalidate();
		centerPanel.repaint();
		holderInfoPanel.addActionListener(new ActionListener() {
			/* User changes DG1 info in GUI. */
			public void actionPerformed(ActionEvent e) {
				MRZInfo updatedMRZInfo = holderInfoPanel.getMRZ();
				mrzPanel.setMRZ(updatedMRZInfo);
				DG1File dg1 = new DG1File(updatedMRZInfo);
				passport.putFile(PassportService.EF_DG1, dg1.getEncoded());
				verificationIndicator.setStatus(passport.getVerificationStatus());
			}
		});
	}

	/**
	 * Sets up the progress bar, starts up the thread, returns immediately.
	 */
	private void displayProgressBar() {
		(new Thread(new Runnable() {
			public void run() {
				try {
					int totalLength = passport.getTotalLength();
					progressBar.setMaximum(totalLength);
					while (passport.getBytesRead() <= totalLength) {
						Thread.sleep(200);
						progressBar.setValue(passport.getBytesRead());
					}
				} catch (InterruptedException ie) {
					/* NOTE: interrupted, end thread. */
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		})).start();
	}

	/* Menu stuff below... */

	private JMenu createFileMenu() {
		JMenu fileMenu = new JMenu("File");

		/* Save As...*/
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		fileMenu.add(saveAsItem);
		saveAsItem.setAction(getSaveAsAction());

		/* Close */
		JMenuItem closeItem = new JMenuItem("Close");
		fileMenu.add(closeItem);
		closeItem.setAction(getCloseAction());

		return fileMenu;
	}

	private JMenu createViewMenu() {
		JMenu menu = new JMenu("View");

		/* View portrait at full size... */
		JMenuItem viewImageAtOriginalSize = new JMenuItem();
		menu.add(viewImageAtOriginalSize);
		viewImageAtOriginalSize.setAction(getViewPortraitAtOriginalSizeAction());

		/* View fingerprints (if any) at full size... */
		JMenuItem viewFingerPrints = new JMenuItem();
		menu.add(viewFingerPrints);
		viewFingerPrints.setAction(getViewFingerPrintsAction());

		menu.addSeparator();

		/* View DS Certificate... */
		JMenuItem viewDocumentSignerCertificate = new JMenuItem();
		menu.add(viewDocumentSignerCertificate);
		viewDocumentSignerCertificate.setAction(getViewDocumentSignerCertificateAction());

		/* View DS key, if any... */
		JMenuItem viewDocumentSignerKey = new JMenuItem();
		menu.add(viewDocumentSignerKey);
		viewDocumentSignerKey.setAction(getViewDocumentSignerKeyAction());

		/* View CS Certificate... */
		JMenuItem viewCountrySignerCertificate = new JMenuItem();
		menu.add(viewCountrySignerCertificate);
		viewCountrySignerCertificate.setAction(getViewCountrySignerCertificateAction());

		/* View AA public key */
		JMenuItem viewAAPublicKey = new JMenuItem();
		menu.add(viewAAPublicKey);
		viewAAPublicKey.setAction(getViewAAPublicKeyAction());

		/* View AA private key */
		JMenuItem viewAAPrivateKey = new JMenuItem();
		menu.add(viewAAPrivateKey);
		viewAAPrivateKey.setAction(getViewAAPrivateKeyAction());

		viewMenu = menu;

		return menu;
	}

	private void createEACMenus(PrivateKey terminalKey, List<CVCertificate> terminalCertificates,
			Map<Integer, PublicKey> passportEACKeys, Integer usedId) {		
		Set<Map.Entry<Integer, PublicKey>> entries = passportEACKeys != null ? passportEACKeys.entrySet() : new HashSet<Map.Entry<Integer, PublicKey>>();
		int pubKeysCount = passportEACKeys != null ? passportEACKeys.size() : 0;

		JMenu viewPassportKeyMenu = new JMenu("Passport EAC keys");
		for (Map.Entry<Integer, PublicKey> entry: entries) {
			int id = entry.getKey();
			PublicKey publicKey = entry.getValue();
			JMenuItem item = new JMenuItem();
			item.setAction(getViewPassportKeyAction(id, publicKey, (pubKeysCount == 1) || usedId.equals(id)));
			viewPassportKeyMenu.add(item);
		}
		Component viewPassportKeyItem = pubKeysCount <= 1 ? viewPassportKeyMenu : viewPassportKeyMenu.getComponent(0);

		JMenuItem viewTerminalKeyItem = new JMenuItem();
		viewTerminalKeyItem.setAction(getViewTerminalKeyAction(terminalKey));
		JMenuItem viewTerminalCertificateItem = new JMenuItem();
		viewTerminalCertificateItem.setAction(getViewTerminalCertificateAction(this, terminalCertificates));
		viewMenu.addSeparator();
		viewMenu.add(viewPassportKeyItem);
		viewMenu.add(viewTerminalCertificateItem);
		viewMenu.add(viewTerminalKeyItem);
	}

	private JMenu createToolsMenu() {
		JMenu menu = new JMenu("Tools");

		/* Load additional portrait from file... */
		JMenuItem loadPortraitFromFile = new JMenuItem();
		menu.add(loadPortraitFromFile);
		loadPortraitFromFile.setAction(getAddPortraitAction());

		/* Delete selected portrait */
		JMenuItem deletePortrait = new JMenuItem();
		menu.add(deletePortrait);
		deletePortrait.setAction(getRemovePortraitAction());

		menu.addSeparator();

		/* Replace DSC with another certificate from file... */
		JMenuItem loadDocSignCertFromFile = new JMenuItem();
		menu.add(loadDocSignCertFromFile);
		loadDocSignCertFromFile.setAction(getLoadDocSignCertAction());

		/* Replace DS key with another key from file... */
		JMenuItem loadDocSignKeyFromFile = new JMenuItem();
		menu.add(loadDocSignKeyFromFile);
		loadDocSignKeyFromFile.setAction(getLoadDocSignKeyAction());

		menu.addSeparator();

		/* Replace AA key with another key from file... */
		JMenuItem loadAAKeyFromFile = new JMenuItem();
		menu.add(loadAAKeyFromFile);
		loadAAKeyFromFile.setAction(getLoadAAPublicKeyAction());

		/* Replace AA private key with another key from file... */
		JMenuItem loadAAPrivateKeyFromFile = new JMenuItem();
		menu.add(loadAAPrivateKeyFromFile);
		loadAAPrivateKeyFromFile.setAction(getLoadAAPrivateKeyAction());

		/* Generate new AA key pair */
		JMenuItem generateAAKeys = new JMenuItem();
		menu.add(generateAAKeys);
		generateAAKeys.setAction(getAAGenerateAction());

		menu.addSeparator();

		/* Generate new EAC key pair */
		JMenuItem generateEACKeys = new JMenuItem();
		menu.add(generateEACKeys);
		generateEACKeys.setAction(getGenerateEACKeys());

		/* Generate load CVCA Certificate */
		JMenuItem loadCVCA = new JMenuItem();
		menu.add(loadCVCA);
		loadCVCA.setAction(getLoadCVCACertificate());

		menu.addSeparator();

		JMenuItem upload = new JMenuItem();
		menu.add(upload);
		upload.setAction(getUploadAction());

		return menu;
	}

	/* Menu item actions below... */

	private Action getViewTerminalCertificateAction(final JFrame frame, final List<CVCertificate> terminalCertificates) {
		Action action = new AbstractAction() {
			private static final long serialVersionUID = -2671362506812399044L;

			public void actionPerformed(ActionEvent e) {
				new TerminalCertificatesDialog(frame, terminalCertificates, false);
			}
		};
		action.putValue(Action.SMALL_ICON, CERTIFICATE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CERTIFICATE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Terminal CV Certificates");
		action.putValue(Action.NAME, "Terminal EAC Certs.");
		return action;
	}

	private Action getViewPassportKeyAction(Integer id, final PublicKey key, boolean eacUsed) {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -4351062035608816679L;

			public void actionPerformed(ActionEvent e) {
				KeyFrame keyFrame = new KeyFrame("EAC Passport Public Key", key);
				keyFrame.pack();
				keyFrame.setVisible(true);
			}
		};
		action.putValue(Action.SMALL_ICON, eacUsed ? KEY_GO : KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, eacUsed ? KEY_GO : KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View passport public EAC key");
		action.putValue(Action.NAME, "Passport EAC key");
		return action;
	}

	private Action getViewTerminalKeyAction(final PrivateKey terminalKey) {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -4351062035608816679L;

			public void actionPerformed(ActionEvent e) {
				if(terminalKey != null) {
					KeyFrame keyFrame = new KeyFrame("Terminal Private Key", terminalKey);
					keyFrame.pack();
					keyFrame.setVisible(true);
				}else{
					// TODO: handle this somehow...
				}
			}
		};
		action.putValue(Action.SMALL_ICON, KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View terminal private key");
		action.putValue(Action.NAME, "Terminal EAC key");
		return action;
	}


	private Action getCloseAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -4351062033708816679L;

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		action.putValue(Action.SMALL_ICON, CLOSE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CLOSE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Close Window");
		action.putValue(Action.NAME, "Close");
		return action;
	}

	private Action getSaveAsAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 9113082315691234764L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(Files.ZIP_FILE_FILTER);
				int choice = fileChooser.showSaveDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, file.getParent());
						FileOutputStream fileOut = new FileOutputStream(file);
						ZipOutputStream zipOut = new ZipOutputStream(fileOut);
						for (short fid: passport.getFileList()) {
							String entryName = Hex.shortToHexString(fid) + ".bin";
							InputStream dg = passport.getInputStream(fid);
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
		};
		action.putValue(Action.SMALL_ICON, SAVE_AS_ICON);
		action.putValue(Action.LARGE_ICON_KEY, SAVE_AS_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Save passport to file");
		action.putValue(Action.NAME, "Save As...");
		return action;
	}

	private Action getViewPortraitAtOriginalSizeAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -7141975907898754026L;

			public void actionPerformed(ActionEvent e) {
				viewPreviewImageAtOriginalSize();
			}
		};
		action.putValue(Action.SMALL_ICON, MAGNIFIER_ICON);
		action.putValue(Action.LARGE_ICON_KEY, MAGNIFIER_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View portrait image at original size");
		action.putValue(Action.NAME, "Portrait at 100%...");
		return action;
	}

	private void viewPreviewImageAtOriginalSize() {
		DisplayedImageInfo info = displayPreviewPanel.getSelectedDisplayedImage();
		switch (info.getType()) {
		case DisplayedImageInfo.TYPE_PORTRAIT:
			InputStream dg2In = passport.getInputStream(PassportService.EF_DG2);
			DG2File dg2 = new DG2File(dg2In);

			FaceInfo faceInfo = (FaceInfo)info;
			PortraitFrame portraitFrame = new PortraitFrame(faceInfo);
			portraitFrame.pack();
			portraitFrame.setVisible(true);
			break;
		default:
			JFrame frame = new JFrame("Image");
			ImagePanel imagePanel = new ImagePanel();
			imagePanel.setImage(info.getImage());
			frame.getContentPane().add(imagePanel);
			frame.pack();
			frame.setVisible(true);
			break;
		}
	}

	private Action getViewFingerPrintsAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -7141975907858754026L;

			public void actionPerformed(ActionEvent e) {
				InputStream dg3In = passport.getInputStream(PassportService.EF_DG3);
				DG3File dg3 = new DG3File(dg3In);
				List<FingerInfo> fingerPrints = dg3.getFingerInfos();
				FingerPrintFrame fingerPrintFrame = new FingerPrintFrame(fingerPrints);
				fingerPrintFrame.setVisible(true);
				fingerPrintFrame.pack();
			}
		};
		action.putValue(Action.SMALL_ICON, FINGERPRINT_ICON);
		action.putValue(Action.LARGE_ICON_KEY, FINGERPRINT_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View fingerprint images at original size");
		action.putValue(Action.NAME, "Fingerprints...");
		return action;
	}

	private Action getAddPortraitAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 9003244936310622991L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.IMAGE_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(Files.IMAGE_FILE_FILTER);

				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.IMAGE_FILES_DIR_KEY, file.getParent());
						BufferedImage image = ImageIO.read(file);

						FaceInfo faceInfo = new FaceInfo(
								Gender.UNSPECIFIED,
								FaceInfo.EyeColor.UNSPECIFIED,
								FaceInfo.HAIR_COLOR_UNSPECIFIED,
								FaceInfo.EXPRESSION_UNSPECIFIED,
								FaceInfo.SOURCE_TYPE_UNSPECIFIED,
								image);
						InputStream dg2In = passport.getInputStream(PassportService.EF_DG2);
						DG2File dg2 = new DG2File(dg2In);
						dg2.addFaceInfo(faceInfo);
						passport.putFile(PassportService.EF_DG2, dg2.getEncoded());
						displayPreviewPanel.addDisplayedImage(faceInfo, false);
					} catch (IOException ioe) {
						/* NOTE: Do nothing. */
					}
					break;
				default:
					break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, LOAD_IMAGE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_IMAGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (additional) portrait from file");
		action.putValue(Action.NAME, "Import portrait...");
		return action;
	}

	private Action getRemovePortraitAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -6635439106858528541L;

			public void actionPerformed(ActionEvent e) {
				int index = displayPreviewPanel.getSelectedIndex();
				InputStream dg2In = passport.getInputStream(PassportService.EF_DG2);
				DG2File dg2 = new DG2File(dg2In);
				dg2.removeFaceInfo(index);
				passport.putFile(PassportService.EF_DG2, dg2.getEncoded());
				displayPreviewPanel.removeDisplayedImage(index);
			}
		};
		action.putValue(Action.SMALL_ICON, DELETE_IMAGE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, DELETE_IMAGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Delete selected portrait");
		action.putValue(Action.NAME, "Delete portrait");
		return action;
	}

	private Action getLoadDocSignCertAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -2441362506867899044L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(Files.CERTIFICATE_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					X509Certificate cert = (X509Certificate)readCertFromFile(file, "X509");
					if(cert != null) {
						passport.updateCOMSODFile(cert);
					}
					break;
				default:
					break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, LOAD_CERT_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_CERT_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (and replace) Document Signer Certificate from file");
		action.putValue(Action.NAME, "Import Doc.Cert...");
		return action;

	}

	private Action getLoadCVCACertificate() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -1231362506867899044L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(Files.CV_CERTIFICATE_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					CVCertificate cert = (CVCertificate)readCertFromFile(file, "CVC");
					if(cert != null) {
						passport.setCVCertificate(cert);
					}
					break;
				default:
					break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, LOAD_CERT_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_CERT_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (and replace) EAC Terminal Root Certificate (CVCA) from file");
		action.putValue(Action.NAME, "Import CVCA Cert...");
		return action;
	}

	private Action getGenerateEACKeys() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -3333362506867899044L;

			public void actionPerformed(ActionEvent e) {
				try {
					String preferredProvider = "BC";
					Provider provider = Security.getProvider(preferredProvider);
					KeyPairGenerator generator = KeyPairGenerator.getInstance(
							"ECDH", provider);
					generator.initialize(new ECGenParameterSpec(
							PassportPersoService.EC_CURVE_NAME));
					KeyPair keyPair = generator.generateKeyPair();

					passport.setEACPrivateKey(keyPair.getPrivate());
					passport.setEACPublicKey(keyPair.getPublic());
				} catch (GeneralSecurityException ex) {
					ex.printStackTrace(); /* NOTE: not silent. -- MO */
				}
			}
		};
		action.putValue(Action.SMALL_ICON, KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Generate and set a pair of EAC keys");
		action.putValue(Action.NAME, "Generate EAC keys");
		return action;
	}

	private Action getLoadDocSignKeyAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -1001362506867899044L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(Files.KEY_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					PrivateKey key = readPrivateRSAKeyFromFile(file);
					passport.setDocSigningPrivateKey(key);
					break;
				default:
					break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, LOAD_KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (and replace) Document Signer private key from file");
		action.putValue(Action.NAME, "Import Doc.Key...");
		return action;
	}

	private Action getLoadAAPublicKeyAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -8265676252065941094L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(Files.KEY_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					PublicKey pubKey = readPublicRSAKeyFromFile(file);
					if(pubKey != null) {
						DG15File dg15 = new DG15File(pubKey);
						passport.putFile(PassportService.EF_DG15, dg15.getEncoded());
					}
					break;
				default:
					break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, LOAD_KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (and replace) Active Authentication public key from file");
		action.putValue(Action.NAME, "Import AA Pub.Key...");
		return action;
	}

	private Action getAAGenerateAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -2065676252065941094L;

			public void actionPerformed(ActionEvent e) {
				try {
					KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
					KeyPair p = gen.generateKeyPair();
					passport.setAAPrivateKey(p.getPrivate());
					passport.setAAPublicKey(p.getPublic());
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		action.putValue(Action.SMALL_ICON, KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Generate a new pair of Active Authentication keys");
		action.putValue(Action.NAME, "Generate AA keys");
		return action;
	}

	private Action getLoadAAPrivateKeyAction() {
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -1265676252065941094L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(Files.KEY_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					passport.setAAPrivateKey(readPrivateRSAKeyFromFile(file));
					break;
				default:
					break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, LOAD_KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (and replace) Active Authentication private key from file");
		action.putValue(Action.NAME, "Import AA Priv.Key...");
		return action;
	}

	private Action getViewDocumentSignerCertificateAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 3937090454142759317L;

			public void actionPerformed(ActionEvent e) {
				try{
					InputStream sodIn = passport.getInputStream(PassportService.EF_SOD);
					SODFile	sod = new SODFile(sodIn);
					JFrame certificateFrame = new CertificateFrame("Document Signer Certificate", sod.getDocSigningCertificate());
					certificateFrame.pack();
					certificateFrame.setVisible(true);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		action.putValue(Action.SMALL_ICON, CERTIFICATE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CERTIFICATE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Document Signer Certificate");
		action.putValue(Action.NAME, "Doc. Cert...");
		return action;
	}

	private Action getViewCountrySignerCertificateAction() {
		Action action = new AbstractAction() {	

			private static final long serialVersionUID = -7115158536366060439L;

			public void actionPerformed(ActionEvent e) {
				try {
					InputStream sodIn = passport.getInputStream(PassportService.EF_SOD);
					SODFile	sod = new SODFile(sodIn);
					X509Certificate docSigningCertificate = sod.getDocSigningCertificate();
					X509Certificate countrySigningCert = null;
					List<TrustStore> cscaStores = passport.getCSCAStores();
					for (TrustStore cscaStore: cscaStores) {
						List<Certificate> chain = cscaStore.getCertificateChain(docSigningCertificate);
						if (chain.size() > 1) {
							countrySigningCert = (X509Certificate)chain.get(1);
						}
					}

					if (countrySigningCert == null) {
						JOptionPane.showMessageDialog(getContentPane(), "CSCA certificate not found", "CSCA not found...", JOptionPane.ERROR_MESSAGE);
					} else {
						JFrame certificateFrame = new CertificateFrame("Country Signer Certificate (from store)", countrySigningCert);
						certificateFrame.pack();
						certificateFrame.setVisible(true);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}
		};
		action.putValue(Action.SMALL_ICON, CERTIFICATE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CERTIFICATE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Country Signer Certificate");
		action.putValue(Action.NAME, "CSCA Cert...");
		return action;
	}

	private Action getViewAAPublicKeyAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -3064369119565468811L;

			public void actionPerformed(ActionEvent e) {
				InputStream dg15In = passport.getInputStream(PassportService.EF_DG15);
				DG15File dg15 = new DG15File(dg15In);
				PublicKey pubKey = dg15.getPublicKey();
				KeyFrame keyFrame = new KeyFrame("Active Authentication Public Key", pubKey);
				keyFrame.pack();
				keyFrame.setVisible(true);
			}
		};
		action.putValue(Action.SMALL_ICON, KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Active Authentication Public Key");
		action.putValue(Action.NAME, "AA Pub. Key...");
		return action;
	}

	private Action getViewAAPrivateKeyAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -1064369119565468811L;

			public void actionPerformed(ActionEvent e) {
				PrivateKey key = passport.getAAPrivateKey();
				if(key != null) {
					KeyFrame keyFrame = new KeyFrame("Active Authentication Private Key", key);
					keyFrame.pack();
					keyFrame.setVisible(true);
				}
			}
		};
		action.putValue(Action.SMALL_ICON, KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Active Authentication Private Key");
		action.putValue(Action.NAME, "AA Priv. Key...");
		return action;
	}

	private Action getViewDocumentSignerKeyAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -2064369119565468811L;

			public void actionPerformed(ActionEvent e) {
				PrivateKey key = passport.getDocSigningPrivateKey(); 
				if(key != null) {
					KeyFrame keyFrame = new KeyFrame("Doc Signing Private Key", key);
					keyFrame.pack();
					keyFrame.setVisible(true);
				}
			}
		};
		action.putValue(Action.SMALL_ICON, KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Doc Signing Private Key");
		action.putValue(Action.NAME, "Doc Key...");
		return action;
	}

	private Action getUploadAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -1281934051651404839L;

			public void actionPerformed(ActionEvent e) {
				CardManager cm = CardManager.getInstance();
				BACKeySpec bacEntry = passport.getBACKeySpec();
				if (bacEntry == null) {
					InputStream dg1In = passport.getInputStream(PassportService.EF_DG1);
					DG1File dg1 = new DG1File(dg1In);
					MRZInfo mrzInfo = dg1.getMRZInfo();
					bacEntry = new BACKeySpec(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());
				}
				PublicKey aaPublicKey = null;
				InputStream dg15In = passport.getInputStream(PassportService.EF_DG15);
				if (dg15In != null) {
					DG15File dg15 = new DG15File(dg15In);
					aaPublicKey = dg15.getPublicKey();
				}
				UploadOptionsChooser chooser = new UploadOptionsChooser(bacEntry, aaPublicKey);
				int choice = chooser.showOptionsDialog(getContentPane());
				switch (choice) {
				case UploadOptionsChooser.APPROVE_OPTION:
					CardTerminal terminal = chooser.getSelectedTerminal();
					boolean wasPolling = cm.isPolling(terminal);
					try {
						cm.stopPolling(terminal);
						// FIXME: have to wait for the poller?
						PassportPersoService persoService = new PassportPersoService(new TerminalCardService(terminal));
						persoService.open();
						if (chooser.isBACSelected()) {
							persoService.setBAC(bacEntry.getDocumentNumber(), bacEntry.getDateOfBirth(), bacEntry.getDateOfExpiry());
						}
						if (aaPublicKey != null) {
							PrivateKey k = passport.getAAPrivateKey();
							if(k != null) {
								persoService.putPrivateKey(k);
							}
						}
						if(passport.getCVCertificate() != null) {
							persoService.putCVCertificate(passport.getCVCertificate());
						}
						if(passport.getEACPrivateKey() != null) {
							persoService.putPrivateEACKey(passport.getEACPrivateKey());
						}
						for (short fid: passport.getFileList()) {
							byte[] fileBytes = passport.getFileBytes(fid);
							persoService.createFile(fid, (short)fileBytes.length);
							persoService.selectFile(fid);
							ByteArrayInputStream in = new ByteArrayInputStream(fileBytes);
							persoService.writeFile(fid, in);
						}
						persoService.lockApplet();
						persoService.close();
						// TODO: to see when it is done
						// Proper progress bar should be implemented
						System.out.println("Passport uploaded.");
						//					} catch (IOException ioe) {
						//						/* NOTE: Do nothing. */
					} catch (CardServiceException cse) {
						cse.printStackTrace();
						//					} catch (GeneralSecurityException gse) {
						//						gse.printStackTrace();
					} finally {
						if (wasPolling) { cm.startPolling(terminal); }
					}
					break;
				default:
					break;
				}
			}			
		};
		action.putValue(Action.SMALL_ICON, UPLOAD_ICON);
		action.putValue(Action.LARGE_ICON_KEY, UPLOAD_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Upload this passport to a passport applet");
		action.putValue(Action.NAME, "Upload passport...");
		return action;
	}

	private static PrivateKey readPrivateRSAKeyFromFile(File file) {
		try {
			byte[] key = new byte[(int)file.length()];
			DataInputStream in = new DataInputStream(new FileInputStream(file));
			in.readFully(key);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(key);
			return kf.generatePrivate(keysp);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static PublicKey readPublicRSAKeyFromFile(File file) {
		try {
			KeyFactory kf = KeyFactory.getInstance("RSA");
			byte[] key = new byte[(int)file.length()];
			DataInputStream in = new DataInputStream(new FileInputStream(file));
			in.readFully(key);
			X509EncodedKeySpec keysp = new X509EncodedKeySpec(key);
			return kf.generatePublic(keysp);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Certificate readCertFromFile(File file, String algorithmName) {
		try {
			CertificateFactory cf = CertificateFactory.getInstance(algorithmName);
			return cf.generateCertificate(new FileInputStream(file));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
