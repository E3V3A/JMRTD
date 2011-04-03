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
 * $Id: PassportFrame.java 894 2009-03-23 15:50:46Z martijno $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
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

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.swing.ImagePanel;
import net.sourceforge.scuba.util.Files;
import net.sourceforge.scuba.util.Hex;
import net.sourceforge.scuba.util.Icons;

import org.jmrtd.AuthAdapter;
import org.jmrtd.BACKeySpec;
import org.jmrtd.EACEvent;
import org.jmrtd.Passport;
import org.jmrtd.PassportService;
import org.jmrtd.app.PreferencesPanel.ReadingMode;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
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
import org.jmrtd.lds.SecurityInfo;

/**
 * Frame for displaying a passport while (and after) it is being read.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 894 $
 */
public class PassportViewFrame extends JMRTDFrame
{
	private static final long serialVersionUID = -4624658204381014128L;

	private static final String PASSPORT_FRAME_TITLE = "MRTD";
	private static final Dimension PREFERRED_SIZE = new Dimension(640, 420);

	private static final Icon CERTIFICATE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("script_key"));
	private static final Icon FINGERPRINT_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("shading"));
	private static final Icon KEY_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("key"));
	private static final Icon KEY_GO = new ImageIcon(Icons.getFamFamFamSilkIcon("key_go"));
	private static final Icon MAGNIFIER_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("magnifier"));
	private static final Icon SAVE_AS_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon OPEN_EDITOR_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("application_edit"));

	private ActionMap actionMap;

	private Logger logger = Logger.getLogger("org.jmrtd");

	private DisplayPreviewPanel displayPreviewPanel;

	private JPanel panel, westPanel, centerPanel, southPanel;
	private JProgressBar progressBar;
	private JMenu viewMenu;

	private Passport passport;

	private EACEvent eacEvent;

	private VerificationIndicator verificationIndicator;

	public PassportViewFrame(Passport passport, ReadingMode readingMode) {
		super(PASSPORT_FRAME_TITLE);
		logger.setLevel(Level.ALL);
		actionMap = new ActionMap();
		this.passport = passport;
		verificationIndicator = new VerificationIndicator();
		panel = new JPanel(new BorderLayout());
		westPanel = new JPanel();
		centerPanel = new JPanel(new BorderLayout());
		southPanel = new JPanel();
		progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
		panel.add(westPanel, BorderLayout.WEST);
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
		//		centerPanel.add(displayPreviewPanel, BorderLayout.WEST);
		westPanel.add(displayPreviewPanel);
		getContentPane().add(panel);
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createToolsMenu());
		pack();
		setVisible(true);

		try {
			passport.addAuthenticationListener(new AuthAdapter() {
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
					if (eacEvent == null || !eacEvent.isSuccess()) {
						logger.warning("Starting to read DG3, but eacEvent = " + eacEvent);
					}
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
			} catch (Exception ioe) {
				String errorMessage = "Exception reading file " + Integer.toHexString(fid) + ": \n"
					+ ioe.getClass().getSimpleName() + "\n" + ioe.getMessage() + "\n";
				JTextArea messageArea = new JTextArea(errorMessage, 5, 15);
				JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(messageArea), "Problem reading file", JOptionPane.WARNING_MESSAGE);
				continue;
			}
		}
	}

	private void updateViewMenu() {
		try {
			InputStream dg14in = passport.getInputStream(PassportService.EF_DG14);
			if (dg14in == null) { return; }
			DG14File dg14 = new DG14File(dg14in);
			PrivateKey terminalKey = null;
			List<CardVerifiableCertificate> cvCertificates = null;
			Map<Integer, PublicKey> publicKeyMap = dg14.getPublicKeys();
			int cardPublicKeyId = -1;
			if (eacEvent != null) {
				terminalKey = eacEvent.getTerminalKey();
				cvCertificates = eacEvent.getCVCertificates();
				cardPublicKeyId = eacEvent.getCardPublicKeyId();
			}
			createEACMenus(terminalKey, cvCertificates, publicKeyMap, cardPublicKeyId);
		} catch (CardServiceException cse) {
			cse.printStackTrace();
		}
	}

	private void displayHolderInfo() throws IOException {
		try {
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
		final HolderViewPanel holderInfoPanel = new HolderViewPanel(mrzInfo);
		final MRZPanel mrzPanel = new MRZPanel(mrzInfo);
		centerPanel.add(holderInfoPanel, BorderLayout.CENTER);
		centerPanel.add(mrzPanel, BorderLayout.SOUTH);
		centerPanel.revalidate();
		centerPanel.repaint();
		} catch (CardServiceException cse) {
			logger.severe("Could not read DG1 for displaying.");
			cse.printStackTrace();
		}
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
		JMenu menu = new JMenu("File");
		menu.add(getSaveAsAction());
		menu.add(getCloseAction());
		return menu;
	}

	private JMenu createViewMenu() {
		JMenu menu = new JMenu("View");
		menu.add(getViewPortraitAtOriginalSizeAction());
		menu.add(getViewFingerPrintsAction());
		menu.addSeparator();
		menu.add(getViewDocumentSignerCertificateAction());
		menu.add(getViewAAPublicKeyAction());
		menu.add(getViewEACInfoAction());
		viewMenu = menu;
		return menu;
	}

	private void createEACMenus(PrivateKey terminalKey, List<CardVerifiableCertificate> terminalCertificates,
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
		menu.add(getOpenEditorAction());
		return menu;
	}

	/* Menu item actions below... */

	private Action getOpenEditorAction() {
		Action action = actionMap.get("OpenEditor");
		if (action != null) { return action; }
		action = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				JFrame editorFrame = new PassportEditFrame(passport, ReadingMode.SAFE_MODE);
				// dispose();
			}

		};
		action.putValue(Action.SMALL_ICON, OPEN_EDITOR_ICON);
		action.putValue(Action.LARGE_ICON_KEY, OPEN_EDITOR_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Open passport in editor");
		action.putValue(Action.NAME, "Open in editor");
		actionMap.put("OpenEditor", action);
		return action;
	}

	private Action getViewTerminalCertificateAction(final JFrame frame, final List<CardVerifiableCertificate> terminalCertificates) {
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
		Action action = actionMap.get("Close");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -4351062033708816679L;

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		action.putValue(Action.SMALL_ICON, CLOSE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CLOSE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Close Window");
		action.putValue(Action.NAME, "Close");
		actionMap.put("Close", action);
		return action;
	}

	private Action getSaveAsAction() {
		Action action = actionMap.get("SaveAs");
		if (action != null) { return action; }
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		action = new AbstractAction() {

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
							try {
								InputStream dg = passport.getInputStream(fid);
								zipOut.putNextEntry(new ZipEntry(entryName));
								int bytesRead;
								byte[] dgBytes = new byte[1024];
								while((bytesRead = dg.read(dgBytes)) > 0){
									zipOut.write(dgBytes, 0, bytesRead);
								}
								zipOut.closeEntry();
							} catch (CardServiceException cse) {
								logger.warning("Skipping entry " + entryName);
							}
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
		actionMap.put("SaveAs", action);
		return action;
	}

	private Action getViewPortraitAtOriginalSizeAction() {
		Action action = actionMap.get("ViewPortraitAtOrginalSize");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -7141975907898754026L;

			public void actionPerformed(ActionEvent e) {
				viewPreviewImageAtOriginalSize();
			}
		};
		action.putValue(Action.SMALL_ICON, MAGNIFIER_ICON);
		action.putValue(Action.LARGE_ICON_KEY, MAGNIFIER_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View portrait image at original size");
		action.putValue(Action.NAME, "Portrait at 100%...");
		actionMap.put("ViewPortraitAtOrginalSize", action);
		return action;
	}

	private void viewPreviewImageAtOriginalSize() {
		DisplayedImageInfo info = displayPreviewPanel.getSelectedDisplayedImage();
		switch (info.getType()) {
		case DisplayedImageInfo.TYPE_PORTRAIT:
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
		Action action = actionMap.get("ViewFingerPrints");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -7141975907858754026L;

			public void actionPerformed(ActionEvent e) {
				try {
					InputStream dg3In = passport.getInputStream(PassportService.EF_DG3);
					DG3File dg3 = new DG3File(dg3In);
					List<FingerInfo> fingerPrints = dg3.getFingerInfos();
					FingerPrintFrame fingerPrintFrame = new FingerPrintFrame(fingerPrints);
					fingerPrintFrame.setVisible(true);
					fingerPrintFrame.pack();
				} catch (CardServiceException cse) {
					cse.printStackTrace();
				}
			}
		};
		action.putValue(Action.SMALL_ICON, FINGERPRINT_ICON);
		action.putValue(Action.LARGE_ICON_KEY, FINGERPRINT_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View fingerprint images at original size");
		action.putValue(Action.NAME, "Fingerprints...");
		actionMap.put("ViewFingerPrints", action);
		return action;
	}

	private Action getViewEACInfoAction() {
		Action action = actionMap.get("ViewEACInfo");
		if (action != null) { return action; }
		action = new AbstractAction() {
			private static final long serialVersionUID = 8408771892967788142L;

			public void actionPerformed(ActionEvent e) {
				JTextArea area = new JTextArea(15, 45);
				try {
					// FIXME: make a special frame for viewing this? (Includes certificates, etc.)
					InputStream dg14In =  passport.getInputStream(PassportService.EF_DG14);
					DG14File dg14 = new DG14File(dg14In);
					List<SecurityInfo> securityInfos = dg14.getSecurityInfos();
					for (SecurityInfo si: securityInfos) {
						area.append(si.getClass().getSimpleName() + ":\n");
						area.append("   " + si.toString() + "\n");
					}
					InputStream cvcaIn = passport.getInputStream(PassportService.EF_CVCA);
					CVCAFile cvca = new CVCAFile(cvcaIn);
					CVCPrincipal caReference = cvca.getCAReference();
					CVCPrincipal altCAReference = cvca.getAltCAReference();
					if (caReference != null) { area.append("CA reference:\n   " + caReference.toString() + "\n"); }
					if (altCAReference != null) { area.append("Alt. CA reference:\n  " + altCAReference.toString() + "\n"); }
					JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(area), "EAC information (card)", JOptionPane.PLAIN_MESSAGE, null);
				} catch (Exception ex) {
					area.append("Could not get EAC information from this card!\n");
					//					area.append("   " + ex.getMessage() + "\n");
					JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(area), "EAC information (card)", JOptionPane.PLAIN_MESSAGE, null);
				}
			}
		};
		action.putValue(Action.SMALL_ICON, CERTIFICATE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CERTIFICATE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View EAC information");
		action.putValue(Action.NAME, "EAC card info...");
		actionMap.put("ViewEACInfo", action);
		return action;
	}

	private Action getViewDocumentSignerCertificateAction() {
		Action action = actionMap.get("ViewDocumentSignerCertificate");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = 3937090454142759317L;

			public void actionPerformed(ActionEvent e) {
				try{
					List<Certificate> chain = passport.getCertificateChain();
					JFrame certificateFrame = null;
					if (chain != null && chain.size() > 0) {
						certificateFrame = new CertificateChainFrame("Certificate chain", chain, true);
					} else {
						SODFile sod = new SODFile(passport.getInputStream(PassportService.EF_SOD));
						X509Certificate docSigningCertificate = sod.getDocSigningCertificate();
						if (docSigningCertificate != null) {
							certificateFrame = new CertificateChainFrame(docSigningCertificate, false);
						}
					}
					if (certificateFrame != null) {
						certificateFrame.pack();
						certificateFrame.setVisible(true);
					}
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		action.putValue(Action.SMALL_ICON, CERTIFICATE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CERTIFICATE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Document Certificate Chain");
		action.putValue(Action.NAME, "Cert. chain...");
		actionMap.put("ViewDocumentSignerCertificate", action);
		return action;
	}

	private Action getViewAAPublicKeyAction() {
		Action action = actionMap.get("ViewAAPublicKey");
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -3064369119565468811L;

			public void actionPerformed(ActionEvent e) {
				try {
					InputStream dg15In = passport.getInputStream(PassportService.EF_DG15);
					DG15File dg15 = new DG15File(dg15In);
					PublicKey pubKey = dg15.getPublicKey();
					KeyFrame keyFrame = new KeyFrame("Active Authentication Public Key", pubKey);
					keyFrame.pack();
					keyFrame.setVisible(true);
				} catch (CardServiceException cse) {
					cse.printStackTrace();
				}
			}
		};
		action.putValue(Action.SMALL_ICON, KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View Active Authentication Public Key");
		action.putValue(Action.NAME, "AA Pub. Key...");
		actionMap.put("ViewAAPublicKey", action);
		return action;
	}
}
