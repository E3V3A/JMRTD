/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.swing.SwingUtilities;

import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.AuthAdapter;
import org.jmrtd.BACKeySpec;
import org.jmrtd.EACEvent;
import org.jmrtd.Passport;
import org.jmrtd.PassportService;
import org.jmrtd.app.PreferencesDialog.ReadingMode;
import org.jmrtd.app.swing.ImagePanel;
import org.jmrtd.app.util.FileUtil;
import org.jmrtd.app.util.IconUtil;
import org.jmrtd.app.util.ImageUtil;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.DG14File;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.DG3File;
import org.jmrtd.lds.DG4File;
import org.jmrtd.lds.DG7File;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;
import org.jmrtd.lds.ImageInfo;
import org.jmrtd.lds.IrisBiometricSubtypeInfo;
import org.jmrtd.lds.IrisImageInfo;
import org.jmrtd.lds.IrisInfo;
import org.jmrtd.lds.LDS;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.SecurityInfo;

/**
 * Frame for displaying a passport while (and after) it is being read.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 894 $
 */
public class DocumentViewFrame extends JMRTDFrame {

	private static final long serialVersionUID = -4624658204381014128L;

	private static final String PASSPORT_FRAME_TITLE = "View document";
	private static final Dimension PREFERRED_SIZE = new Dimension(540, 420);

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private static final Icon CERTIFICATE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("script_key"));
	private static final Icon FINGERPRINT_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("shading"));
	private static final Icon KEY_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("key"));
	private static final Icon KEY_GO = new ImageIcon(IconUtil.getFamFamFamSilkIcon("key_go"));
	private static final Icon MAGNIFIER_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("magnifier"));
	private static final Icon SAVE_AS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("bin"));
	private static final Icon OPEN_EDITOR_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("application_edit"));

	private ActionMap actionMap;

	private ImagePreviewPanel displayPreviewPanel;

	private JPanel panel, centerPanel, southPanel;
	private JProgressBar progressBar;
	private JMenu viewMenu;

	private Passport passport;

	private EACEvent eacEvent;
	
	private boolean isDisplaying;

	private VerificationIndicator verificationIndicator;

	private APDUListener apduListener;

	public DocumentViewFrame(Passport passport, ReadingMode readingMode, APDUListener apduListener) {
		super(PASSPORT_FRAME_TITLE);
		actionMap = new ActionMap();
		this.passport = passport;
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		verificationIndicator = new VerificationIndicator();
		panel = new JPanel(new BorderLayout());
		centerPanel = new JPanel(new BorderLayout());
		southPanel = new JPanel();
		progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
		panel.add(centerPanel, BorderLayout.CENTER);
		southPanel.add(verificationIndicator);
		southPanel.add(progressBar);
		panel.add(southPanel, BorderLayout.SOUTH);
		displayPreviewPanel = new ImagePreviewPanel(160, 200);
		displayPreviewPanel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() > 1) {
					viewPreviewImageAtOriginalSize();
				}
			}
		});
		centerPanel.add(displayPreviewPanel, BorderLayout.WEST);

		contentPane.add(panel, BorderLayout.CENTER);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createToolsMenu());		

		/* FIXME: have caller call pack(), setVisible(), do the other stuff in a new thread */
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
			LOGGER.info("time: " + Integer.toString((int)(System.currentTimeMillis() - t) / 1000));

			isDisplaying = true;
			displayProgressBar();
			switch (readingMode) {
			case SAFE_MODE:
				passport.verifySecurity(); // blocks
				verificationIndicator.setStatus(passport.getVerificationStatus());
				displayInputStreams();
				break;
			case PROGRESSIVE_MODE:
				displayInputStreams();
				break;
			}
			passport.verifySecurity();
			verificationIndicator.setStatus(passport.getVerificationStatus());
			LOGGER.info("time: " + Integer.toString((int)(System.currentTimeMillis() - t)/1000));
		} catch (Exception e) {
			e.printStackTrace();
			
			/* START DEBUG */
			System.out.println("DEBUG: this exception was previously (< 0.4.8) uncaught -- MO 21");
			e.printStackTrace();
			String errorMessage = "DEBUG: Uncaught exception 21: "
					+ e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n";
			JTextArea messageArea = new JTextArea(errorMessage, 5, 15);
			JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(messageArea), "Problem displaying / verifying", JOptionPane.WARNING_MESSAGE);
			/* END DEBUG */	
			
			dispose();
			return;
		} finally {
			isDisplaying = false;
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
			displayDG1Info();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		try { /* DEBUG DEBUG */
			LDS lds = passport.getLDS();
			List<Short> fileList = lds.getFileList();
			for (short fid: fileList) {
				try {
					switch (fid) {
					case PassportService.EF_COM:
					case PassportService.EF_SOD:
					case PassportService.EF_CVCA:
					case PassportService.EF_DG1:
						/* NOTE: Already displayed these, or not relevant here. */
						break;
					case PassportService.EF_DG2:
						try {
							DG2File dg2 = lds.getDG2File();
							if (dg2 == null) { LOGGER.warning("DEBUG: EF.DG2 is in file list, but cannot get file from LDS"); continue; }
							List<FaceInfo> faceInfos = dg2.getFaceInfos();
							for (FaceInfo faceInfo: faceInfos) {
								List<FaceImageInfo> faceImageInfos = faceInfo.getFaceImageInfos();
								for (FaceImageInfo faceImageInfo: faceImageInfos) {
									displayPreviewPanel.addDisplayedImage(faceImageInfo);
								}
							}
						} catch (Exception e) {
							/* START DEBUG */
							System.out.println("DEBUG: this exception was previously (< 0.4.8) uncaught -- MO 2");
							e.printStackTrace();
							String errorMessage = "DEBUG: Exception reading file " + Integer.toHexString(fid) + ": \n"
									+ e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n";
							JTextArea messageArea = new JTextArea(errorMessage, 5, 15);
							JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(messageArea), "Problem reading file", JOptionPane.WARNING_MESSAGE);
							/* END DEBUG */
						}
						break;
					case PassportService.EF_DG3:
						try {
							DG3File dg3 = lds.getDG3File();
							if (dg3 == null) { LOGGER.warning("DEBUG: EF.DG3 is in file list, but cannot get file from LDS"); continue; }
							if (eacEvent == null || !eacEvent.isSuccess()) {
								LOGGER.warning("Starting to read DG3, but eacEvent = " + eacEvent);
							}
							List<FingerInfo> fingerInfos = dg3.getFingerInfos();
							for (FingerInfo fingerInfo: fingerInfos) {
								List<FingerImageInfo> fingerImageInfos = fingerInfo.getFingerImageInfos();
								for (FingerImageInfo fingerImageInfo: fingerImageInfos) {
									displayPreviewPanel.addDisplayedImage(fingerImageInfo);
								}
							}
						} catch (Exception e) {
							/* START DEBUG */
							System.out.println("DEBUG: this exception was previously (< 0.4.8) uncaught -- MO 3");
							e.printStackTrace();
							String errorMessage = "DEBUG: Exception reading file " + Integer.toHexString(fid) + ": \n"
									+ e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n";
							JTextArea messageArea = new JTextArea(errorMessage, 5, 15);
							JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(messageArea), "Problem reading file", JOptionPane.WARNING_MESSAGE);
							/* END DEBUG */
						}
						break;
					case PassportService.EF_DG4:
						DG4File dg4 = lds.getDG4File();
						if (dg4 == null) { LOGGER.warning("DEBUG: EF.DG4 is in file list, but cannot get file from LDS"); continue; }
						if (eacEvent == null || !eacEvent.isSuccess()) {
							LOGGER.warning("Starting to read DG4, but eacEvent = " + eacEvent);
						}
						List<IrisInfo> irisInfos = dg4.getIrisInfos();
						for (IrisInfo irisInfo: irisInfos) {
							List<IrisBiometricSubtypeInfo> irisBiometricSubtypeInfos = irisInfo.getIrisBiometricSubtypeInfos();
							for (IrisBiometricSubtypeInfo irisBiometricSubtypeInfo: irisBiometricSubtypeInfos) {
								List<IrisImageInfo> irisImageInfos = irisBiometricSubtypeInfo.getIrisImageInfos();
								for (IrisImageInfo irisImageInfo: irisImageInfos) {
									displayPreviewPanel.addDisplayedImage(irisImageInfo);
								}
							}
						}
						break;
					case PassportService.EF_DG7:
						DG7File dg7 = lds.getDG7File();
						List<DisplayedImageInfo> infos = dg7.getImages();
						for (DisplayedImageInfo info: infos) { displayPreviewPanel.addDisplayedImage(info); }
						break;
					case PassportService.EF_DG14:
						try {
							/* DG14File dg14 = */ lds.getDG14File();
							lds.getCVCAFile();
							updateViewMenu();
						} catch (Exception e) {
							/* START DEBUG */
							System.out.println("DEBUG: this exception was previously (< 0.4.8) uncaught -- MO 14");
							e.printStackTrace();
							String errorMessage = "DEBUG: Exception reading DG14 file " + Integer.toHexString(fid) + ": \n"
									+ e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n";
							JTextArea messageArea = new JTextArea(errorMessage, 5, 15);
							JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(messageArea), "Problem reading file", JOptionPane.WARNING_MESSAGE);
							/* END DEBUG */	
						}
						break;
					case PassportService.EF_DG5:
					case PassportService.EF_DG6:
					case PassportService.EF_DG11:
					case PassportService.EF_DG12:
					case PassportService.EF_DG15:
						lds.getFile(fid);
						break;
					default:
						String message = "Displaying of file " + Integer.toHexString(fid) + " not supported!";
						if ((fid & 0x010F) == fid) {
							int tag = LDSFileUtil.lookupTagByFID(fid);
							int dgNumber = LDSFileUtil.lookupDataGroupNumberByTag(tag);
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
		} catch (Throwable e) {
			/* START DEBUG */
			System.out.println("DEBUG: this exception was previously (< 0.4.8) uncaught -- MO method exit");
			e.printStackTrace();
			String errorMessage = "DEBUG: Uncaught exception: \n"
					+ e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n";
			JTextArea messageArea = new JTextArea(errorMessage, 5, 15);
			JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(messageArea), "Problem reading file", JOptionPane.WARNING_MESSAGE);
			/* END DEBUG */	
		}
	}

	private void updateViewMenu() {
		try {
			LDS lds = passport.getLDS();
			DG14File dg14 = lds.getDG14File();
			if (dg14 == null) { return; }
			PrivateKey terminalKey = null;
			List<CardVerifiableCertificate> cvCertificates = null;
			Map<BigInteger, PublicKey> publicKeyMap = dg14.getChipAuthenticationPublicKeyInfos();
			BigInteger cardPublicKeyId = BigInteger.valueOf(-1);
			if (eacEvent != null) {
				terminalKey = eacEvent.getTerminalKey();
				cvCertificates = eacEvent.getCVCertificates();
				cardPublicKeyId = eacEvent.getCardPublicKeyId();
			}
			createEACMenus(terminalKey, cvCertificates, publicKeyMap, cardPublicKeyId);
		} catch (Exception ex) {
			ex.printStackTrace();
			LOGGER.severe("Could not decode DG14. " + ex.getMessage());
		}
	}

	private void displayDG1Info() throws IOException {
		try {
			LDS lds = passport.getLDS();
			DG1File dg1 = lds.getDG1File();
			MRZInfo mrzInfo = dg1.getMRZInfo();
			BACKeySpec bacEntry = passport.getBACKeySpec();
			if (bacEntry != null &&
					!(MRZInfo.equalsModuloFillerChars(mrzInfo.getDocumentNumber(), bacEntry.getDocumentNumber()) &&
							mrzInfo.getDateOfBirth().equals(bacEntry.getDateOfBirth())) &&
							mrzInfo.getDateOfExpiry().equals(bacEntry.getDateOfExpiry())) {
				JOptionPane.showMessageDialog(getContentPane(), "MRZ used in BAC differs from\nMRZ in DG1!", "Warning", JOptionPane.WARNING_MESSAGE);
			}
			final HolderViewPanel holderInfoPanel = new HolderViewPanel(mrzInfo);
			final MRZPanel mrzPanel = new MRZPanel(mrzInfo);
			centerPanel.add(holderInfoPanel, BorderLayout.CENTER);
			centerPanel.add(mrzPanel, BorderLayout.SOUTH);
			centerPanel.revalidate();
			centerPanel.repaint();
		} catch (Exception cse) {
			LOGGER.severe("Could not read DG1 for displaying.");
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
					while (isDisplaying) {
						int pos = passport.getLDS().getPosition();
						int length = passport.getLDS().getLength();
//						System.out.println("DEBUG: pos = " + pos + "/" + length);
						progressBar.setMaximum(length);
						progressBar.setValue(pos);
						Thread.sleep(200);
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
			Map<BigInteger, PublicKey> passportEACKeys, BigInteger usedId) {		
		Set<Map.Entry<BigInteger, PublicKey>> entries = passportEACKeys != null ? passportEACKeys.entrySet() : new HashSet<Map.Entry<BigInteger, PublicKey>>();
		int pubKeysCount = passportEACKeys != null ? passportEACKeys.size() : 0;

		JMenu viewPassportKeyMenu = new JMenu("Passport EAC keys");
		for (Map.Entry<BigInteger, PublicKey> entry: entries) {
			BigInteger keyId = entry.getKey();
			PublicKey publicKey = entry.getValue();
			JMenuItem item = new JMenuItem();
			item.setAction(getViewPassportKeyAction(keyId, publicKey, (pubKeysCount == 1) || usedId.equals(keyId)));
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

			private static final long serialVersionUID = 6290353637971392593L;

			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						/* JFrame editorFrame = */ new DocumentEditFrame(passport, ReadingMode.SAFE_MODE, apduListener);
					}
				});
				dispose();
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

	private Action getViewPassportKeyAction(BigInteger id, final PublicKey key, boolean eacUsed) {
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
				fileChooser.setFileFilter(FileUtil.ZIP_FILE_FILTER);
				int choice = fileChooser.showSaveDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.PASSPORT_ZIP_FILES_DIR_KEY, file.getParent());
						FileOutputStream fileOut = new FileOutputStream(file);
						ZipOutputStream zipOut = new ZipOutputStream(fileOut);
						for (short fid: passport.getLDS().getFileList()) {
							String entryName = Hex.shortToHexString(fid) + ".bin";
							try {
								InputStream dg = passport.getLDS().getInputStream(fid);
								zipOut.putNextEntry(new ZipEntry(entryName));
								int bytesRead;
								byte[] dgBytes = new byte[1024];
								while((bytesRead = dg.read(dgBytes)) > 0){
									zipOut.write(dgBytes, 0, bytesRead);
								}
								zipOut.closeEntry();
							} catch (Exception cse) {
								LOGGER.warning("Skipping entry " + entryName);
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
		ImageInfo info = displayPreviewPanel.getSelectedDisplayedImage();
		switch (info.getType()) {
		case DisplayedImageInfo.TYPE_PORTRAIT:
			FaceImageInfo faceImageInfo = (FaceImageInfo)info;
			PortraitFrame portraitFrame = new PortraitFrame(faceImageInfo);
			portraitFrame.pack();
			portraitFrame.setVisible(true);
			break;
		case DisplayedImageInfo.TYPE_FINGER:
			FingerImageInfo fingerImageInfo = (FingerImageInfo)info;

			FingerInfo fingerInfo = new FingerInfo(0,
					30,
					FingerInfo.SCALE_UNITS_PPI,
					500,
					500, /* FIXME: get some of these from fingerImageInfo? */
					500,
					500,
					8, /* FIXME: get depth from fingerImageInfo? */
					fingerImageInfo.getCompressionAlgorithm(),
					Arrays.asList(new FingerImageInfo[] { fingerImageInfo }));
			FingerPrintFrame fingerFrame = new FingerPrintFrame(Arrays.asList(new FingerInfo[] { fingerInfo }));
			fingerFrame.pack();
			fingerFrame.setVisible(true);
			break;
		default:
			try {
				JFrame frame = new JMRTDFrame("Image");
				Image image = ImageUtil.read(info.getImageInputStream(), info.getImageLength(), info.getMimeType());
				ImagePanel imagePanel = new ImagePanel();
				imagePanel.setImage(image);
				frame.getContentPane().add(imagePanel);
				frame.pack();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
					InputStream dg3InputStream = passport.getLDS().getInputStream(PassportService.EF_DG3);
					DG3File dg3 = new DG3File(dg3InputStream);
					List<FingerInfo> fingerInfos = dg3.getFingerInfos();
					FingerPrintFrame fingerPrintFrame = new FingerPrintFrame(fingerInfos);
					fingerPrintFrame.setVisible(true);
					fingerPrintFrame.pack();
				} catch (Exception ex) {
					ex.printStackTrace();
					LOGGER.severe("Could not decode DG3. " + ex.getMessage());
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
					DG14File dg14 = passport.getLDS().getDG14File();
					Collection<SecurityInfo> securityInfos = dg14.getSecurityInfos();
					for (SecurityInfo si: securityInfos) {
						area.append(si.getClass().getSimpleName() + ":\n");
						area.append("   " + si.toString() + "\n");
					}
					CVCAFile cvca = passport.getLDS().getCVCAFile();
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
						SODFile sod = passport.getLDS().getSODFile();
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
					DG15File dg15 = passport.getLDS().getDG15File();
					PublicKey pubKey = dg15.getPublicKey();
					KeyFrame keyFrame = new KeyFrame("Active Authentication Public Key", pubKey);
					keyFrame.pack();
					keyFrame.setVisible(true);
				} catch (Exception ex) {
					ex.printStackTrace();
					LOGGER.severe("Could not decode DG15. " + ex.getMessage());
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
