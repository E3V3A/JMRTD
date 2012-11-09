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
 * $Id: $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
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
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import net.sourceforge.scuba.data.Gender;
import net.sourceforge.scuba.smartcards.APDUListener;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.EACEvent;
import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.Passport;
import org.jmrtd.PassportPersoService;
import org.jmrtd.PassportService;
import org.jmrtd.app.PreferencesDialog.ReadingMode;
import org.jmrtd.app.swing.ImagePanel;
import org.jmrtd.app.util.FileUtil;
import org.jmrtd.app.util.IconUtil;
import org.jmrtd.app.util.ImageUtil;
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
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceImageInfo.FeaturePoint;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;
import org.jmrtd.lds.ImageInfo;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.MRZInfo;
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
public class DocumentEditFrame extends JMRTDFrame {

	private static final long serialVersionUID = -4624658204371014128L;

	private static final String PASSPORT_FRAME_TITLE = "Edit document";
	private static final Dimension PREFERRED_SIZE = new Dimension(800, 500);

	private static final Icon CHANGE_DOCUMENT_TYPE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("page_go"));
	private static final Icon CHANGE_DOCUMENT_TYPE_TO_ID_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("bullet_go"));
	private static final Icon CERTIFICATE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("script_key"));
	private static final Icon FINGERPRINT_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("shading"));
	private static final Icon KEY_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("key"));
	private static final Icon KEY_GO = new ImageIcon(IconUtil.getFamFamFamSilkIcon("key_go"));
	private static final Icon MAGNIFIER_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("magnifier"));
	private static final Icon SAVE_AS_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("bin"));
	private static final Icon LOAD_IMAGE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("folder_image"));
	private static final Icon DELETE_IMAGE_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("image_delete"));
	private static final Icon LOAD_CERT_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("folder_page_white"));
	private static final Icon LOAD_KEY_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("folder_key"));
	private static final Icon UPLOAD_ICON = new ImageIcon(IconUtil.getFamFamFamSilkIcon("drive_burn"));

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	/**
	 * The name of the EC curve for DH key pair generation (this is the only one
	 * that our passport applet supports. 
	 */
	private static final String EC_CURVE_NAME = "c2pnb163v1";

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private ImagePreviewPanel displayPreviewPanel;

	private JPanel panel, westPanel, centerPanel;
	private JMenu viewMenu;

	private DG1EditPanel dg1EditPanel;
	private LDSTreePanel treePanel;
	private MRZPanel mrzPanel;

	private Passport passport;

	private EACEvent eacEvent;

	private ActionMap actionMap;

	private APDUListener apduListener;

	public DocumentEditFrame(Passport passport, ReadingMode readingMode, APDUListener apduListener) {
		super(PASSPORT_FRAME_TITLE);
		this.apduListener = apduListener;
		LOGGER.setLevel(Level.ALL);
		this.passport = passport;
		actionMap = new ActionMap();
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		panel = new JPanel(new BorderLayout());
		westPanel = new JPanel();
		centerPanel = new JPanel(new BorderLayout());
		panel.add(westPanel, BorderLayout.WEST);
		panel.add(centerPanel, BorderLayout.CENTER);
		displayPreviewPanel = new ImagePreviewPanel(160, 200);
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

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		treePanel = new LDSTreePanel(passport);
		splitPane.add(treePanel);
		splitPane.add(panel);

		contentPane.add(splitPane, BorderLayout.CENTER);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createToolsMenu());
		pack();
		setVisible(true);

		try {
			passport.addAuthenticationListener(new AuthListener() {
				public void performedAA(AAEvent ae) { /* NOTE: do nothing */ }

				public void performedBAC(BACEvent be) { /* NOTE: do nothing */ }

				public void performedEAC(EACEvent ee) {
					eacEvent = ee;
					updateViewMenu(); // FIXME: make switch to allow creation/editing of EAC document. This is legacy from view mode.
				}
			});
			long t = System.currentTimeMillis();
			LOGGER.info("time: " + Integer.toString((int)(System.currentTimeMillis() - t) / 1000));

			displayInputStreams();
			LOGGER.info("time: " + Integer.toString((int)(System.currentTimeMillis() - t)/1000));
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
	private void displayInputStreams() {
		try {
			displayDG1();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		for (short fid: passport.getFileList()) {
			try {
				InputStream inputStream = passport.getInputStream(fid);
				if (inputStream == null) { LOGGER.warning("Got null inputstream while trying to display " + Integer.toHexString(fid & 0xFFFF)); }
				switch (fid) {
				case PassportService.EF_COM:
					/* NOTE: Already processed this one. */
					break;
				case PassportService.EF_DG1:
					/* NOTE: Already processed this one. */
					break;
				case PassportService.EF_DG2:
					try {
						DG2File dg2 = new DG2File(inputStream);
						List<FaceInfo> faceInfos = dg2.getFaceInfos();
						for (FaceInfo faceInfo: faceInfos) {
							List<FaceImageInfo> faceImageInfos = faceInfo.getFaceImageInfos();
							for (FaceImageInfo faceImageInfo: faceImageInfos) {
								displayPreviewPanel.addDisplayedImage(faceImageInfo);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						String errorMessage = "DEBUG: Exception reading file " + Integer.toHexString(fid) + ": \n"
								+ e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n";
						JTextArea messageArea = new JTextArea(errorMessage, 5, 15);
						JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(messageArea), "Problem reading file", JOptionPane.WARNING_MESSAGE);

					}
					break;
				case PassportService.EF_DG3:
					DG3File dg3 = new DG3File(inputStream);
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
					break;
				case PassportService.EF_DG4:
					DG4File dg4 = new DG4File(inputStream);
					break;
				case PassportService.EF_DG5:
					DG5File dg5 = new DG5File(inputStream);
					break;
				case PassportService.EF_DG6:
					DG6File dg6 = new DG6File(inputStream);
					break;
				case PassportService.EF_DG7:
					DG7File dg7 = new DG7File(inputStream);
					List<DisplayedImageInfo> infos = dg7.getImages();
					for (DisplayedImageInfo info: infos) { displayPreviewPanel.addDisplayedImage(info); }
					break;
				case PassportService.EF_DG11:
					DG11File dg11 = new DG11File(inputStream);
					break;
				case PassportService.EF_DG12:
					DG12File dg12 = new DG12File(inputStream);
					break;
				case PassportService.EF_DG14:
					DG14File dg14 = new DG14File(inputStream);
					updateViewMenu();
					break;
				case PassportService.EF_DG15:
					DG15File dg15 = new DG15File(inputStream);
					break;
				case PassportService.EF_SOD:
					/* NOTE: Already processed this one above. */
					break;
				case PassportService.EF_CVCA:
					CVCAFile cvca = new CVCAFile(inputStream);
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
	}

	private void updateViewMenu() {
		try {
			InputStream dg14InputStream = passport.getInputStream(PassportService.EF_DG14);
			if (dg14InputStream == null) { return; }
			DG14File dg14 = new DG14File(dg14InputStream);
			PrivateKey terminalKey = null;
			List<CardVerifiableCertificate> cvCertificates = null;
			Map<BigInteger, PublicKey> publicKeyMap = null;
			BigInteger cardPublicKeyId = BigInteger.valueOf(-1);
			if (eacEvent != null) {
				terminalKey = eacEvent.getTerminalKey();
				cvCertificates = eacEvent.getCVCertificates();
				publicKeyMap = dg14.getChipAuthenticationPublicKeyInfos();
				cardPublicKeyId = eacEvent.getCardPublicKeyId();
			} // TODO: else { default values }
			createEACMenus(terminalKey, cvCertificates, publicKeyMap, cardPublicKeyId);
		} catch (CardServiceException cse) {
			LOGGER.info("Could not read DG14. No EAC support.");
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.severe("Could not decode DG14. Unexpected exception. No EAC support. " + e.getMessage());
		}
	}

	private void displayDG1() throws IOException {
		try {
			InputStream dg1InputStream = passport.getInputStream(PassportService.EF_DG1);
			DG1File dg1 = new DG1File(dg1InputStream);
			MRZInfo mrzInfo = dg1.getMRZInfo();
			BACKeySpec bacEntry = passport.getBACKeySpec();
			if (bacEntry != null &&
					!(MRZInfo.equalsModuloFillerChars(mrzInfo.getDocumentNumber(), bacEntry.getDocumentNumber()) &&
							mrzInfo.getDateOfBirth().equals(bacEntry.getDateOfBirth())) &&
							mrzInfo.getDateOfExpiry().equals(bacEntry.getDateOfExpiry())) {
				JOptionPane.showMessageDialog(getContentPane(), "MRZ used in BAC differs from\nMRZ in DG1!", "Warning", JOptionPane.WARNING_MESSAGE);
			}
			dg1EditPanel = new DG1EditPanel(mrzInfo);
			mrzPanel = new MRZPanel(mrzInfo);
			setMRZ(mrzInfo);
			dg1EditPanel.addActionListener(new ActionListener() {
				/* User changes DG1 info in GUI. */
				public void actionPerformed(ActionEvent e) {
					MRZInfo mrzInfo = dg1EditPanel.getMRZ();
					setMRZ(mrzInfo);
				}
			});
		} catch (CardServiceException cse) {
			cse.printStackTrace();
		}
	}

	private void setMRZ(MRZInfo mrzInfo) {

		DG1File dg1 = new DG1File(mrzInfo);
		passport.putFile(PassportService.EF_DG1, dg1.getEncoded());
		treePanel.reload();

		mrzPanel.setMRZ(mrzInfo);
		dg1EditPanel.setMRZ(mrzInfo);

		//		if (centerPanel.getComponentCount() > 0) {
		//			centerPanel.removeAll();
		//		}
		if (centerPanel.getComponentCount() == 0) {
			centerPanel.add(dg1EditPanel, BorderLayout.CENTER);
			centerPanel.add(mrzPanel, BorderLayout.SOUTH);
		}
		centerPanel.revalidate();
		centerPanel.repaint();
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

		int documentType = MRZInfo.DOC_TYPE_UNSPECIFIED;
		try {
			MRZInfo mrzInfo = null;
			DG1File dg1 = new DG1File(passport.getInputStream(PassportService.EF_DG1));
			mrzInfo = dg1.getMRZInfo();
			documentType = mrzInfo.getDocumentType();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.severe("Could not determine current document type");
		}

		/* Change document type (ID1, ID2, ID3) */
		JMenu changeDocTypeMenuItem = new JMenu("Change doc type");
		menu.add(changeDocTypeMenuItem);
		changeDocTypeMenuItem.setAction(getChangeDocTypeAction());
		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButtonMenuItem id1MenuItem = new JRadioButtonMenuItem();
		buttonGroup.add(id1MenuItem); changeDocTypeMenuItem.add(id1MenuItem); id1MenuItem.setAction(getChangeDocTypeToAction(1));
		id1MenuItem.setSelected(documentType == MRZInfo.DOC_TYPE_ID1);
		JRadioButtonMenuItem id2MenuItem = new JRadioButtonMenuItem();
		id2MenuItem.setSelected(documentType == MRZInfo.DOC_TYPE_ID2);
		buttonGroup.add(id2MenuItem); changeDocTypeMenuItem.add(id2MenuItem); id2MenuItem.setAction(getChangeDocTypeToAction(2));
		JRadioButtonMenuItem id3MenuItem = new JRadioButtonMenuItem();
		buttonGroup.add(id3MenuItem); changeDocTypeMenuItem.add(id3MenuItem); id3MenuItem.setAction(getChangeDocTypeToAction(3));
		id3MenuItem.setSelected(documentType == MRZInfo.DOC_TYPE_ID3);

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

		/* Replace EAC private key with another private key from file... */
		JMenuItem loadEACPrivateKeyFromFile = new JMenuItem();
		menu.add(loadEACPrivateKeyFromFile);
		loadEACPrivateKeyFromFile.setAction(getLoadEACKeysAction());

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

	private Action getViewPassportKeyAction(BigInteger keyId, final PublicKey key, boolean eacUsed) {
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
		Action action = actionMap.get("SaveAs");
		if (action != null) { return action; }
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
								/* Skip this file. */
								LOGGER.warning("Skipping " + entryName);
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
			try {
				FaceImageInfo faceInfo = (FaceImageInfo)info;
				PortraitFrame portraitFrame = new PortraitFrame(faceInfo);
				portraitFrame.pack();
				portraitFrame.setVisible(true);
			} catch (Exception cse) {
				cse.printStackTrace();
			}
			break;
		default:
			try {
				JFrame frame = new JFrame("Image");
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
					InputStream dg3InputStream = passport.getInputStream(PassportService.EF_DG3);
					DG3File dg3 = new DG3File(dg3InputStream);
					List<FingerInfo> fingerPrints = dg3.getFingerInfos();
					FingerPrintFrame fingerPrintFrame = new FingerPrintFrame(fingerPrints);
					fingerPrintFrame.setVisible(true);
					fingerPrintFrame.pack();
				} catch (CardServiceException cse) {
					cse.printStackTrace();
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
				fileChooser.setFileFilter(FileUtil.IMAGE_FILE_FILTER);

				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					try {
						File file = fileChooser.getSelectedFile();
						preferences.put(JMRTDApp.IMAGE_FILES_DIR_KEY, file.getParent());
						BufferedImage image = ImageIO.read(file);
						ByteArrayOutputStream encodedImageOut = new ByteArrayOutputStream();
						ImageUtil.write(image, "image/jpeg", encodedImageOut);
						encodedImageOut.flush();
						byte[] imageBytes = encodedImageOut.toByteArray();
						int width = image.getWidth(), height = image.getHeight();
						FaceImageInfo faceImageInfo = new FaceImageInfo(
								Gender.UNSPECIFIED,
								FaceImageInfo.EyeColor.UNSPECIFIED,
								FaceImageInfo.HAIR_COLOR_UNSPECIFIED,
								0x0000,
								FaceImageInfo.EXPRESSION_UNSPECIFIED,
								new int[3],
								new int[3],
								0x00, // color space
								FaceImageInfo.FACE_IMAGE_TYPE_BASIC,
								FaceImageInfo.SOURCE_TYPE_UNSPECIFIED,
								0x0000,
								0x0000, // quality
								new FeaturePoint[0],
								width, height,
								new ByteArrayInputStream(imageBytes), imageBytes.length, FaceImageInfo.IMAGE_DATA_TYPE_JPEG);
						FaceInfo faceInfo = new FaceInfo(Arrays.asList(new FaceImageInfo[] { faceImageInfo }));
						InputStream dg2InputStream = passport.getInputStream(PassportService.EF_DG2);
						DG2File dg2 = new DG2File(dg2InputStream);
						dg2.addFaceInfo(faceInfo);
						passport.putFile(PassportService.EF_DG2, dg2.getEncoded());
						displayPreviewPanel.addDisplayedImage(faceImageInfo);
					} catch (IOException ioe) {
						/* NOTE: Do nothing. */
						ioe.printStackTrace();
					} catch (CardServiceException cse) {
						cse.printStackTrace();
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
				try {
					int index = displayPreviewPanel.getSelectedIndex();
					InputStream dg2InputStream = passport.getInputStream(PassportService.EF_DG2);
					DG2File dg2 = new DG2File(dg2InputStream);
					dg2.removeFaceInfo(index);
					passport.putFile(PassportService.EF_DG2, dg2.getEncoded());
					displayPreviewPanel.removeDisplayedImage(index);
				} catch (CardServiceException cse) {
					cse.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
					LOGGER.severe("Could not decode DG2. " + ex.getMessage());
				}
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
				fileChooser.setFileFilter(FileUtil.CERTIFICATE_FILE_FILTER);
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
				fileChooser.setFileFilter(FileUtil.CV_CERTIFICATE_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					CardVerifiableCertificate cert = (CardVerifiableCertificate)readCertFromFile(file, "CVC");
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

	private Action getLoadEACKeysAction() {
		Action action = actionMap.get("LoadEACKeys");
		if (action != null) { return action; }
		final Preferences preferences = Preferences.userNodeForPackage(getClass());
		action = new AbstractAction() {

			private static final long serialVersionUID = -6337563458744158650L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				String directory = preferences.get(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, null);
				if (directory != null) {
					fileChooser.setCurrentDirectory(new File(directory));
				}
				fileChooser.setFileFilter(FileUtil.KEY_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					PrivateKey privKey = readPrivateKeyFromFile(file, "EC");
					if (privKey == null) {
						LOGGER.severe("Could not load EAC keys"); // FIXME: GUI feedback
					} else {
						try {
							PublicKey pubKey = getECPublicKeyFromPrivateKey(privKey);
							passport.setEACPrivateKey(privKey);
							passport.setEACPublicKey(pubKey);
						} catch (GeneralSecurityException gse) {
							gse.printStackTrace();
							LOGGER.severe("Could not load EAC keys"); // FIXME: GUI feedback
						}
					}
					break;
				default:
					break;
				}
			}
		};
		action.putValue(Action.SMALL_ICON, LOAD_KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (and replace) Extended Access Control keys (in PKCS#8 format) from file");
		action.putValue(Action.NAME, "Import EAC keys...");
		actionMap.put("LoadEACKeys", action);
		return action;
	}

	/*
	 * FIXME: Hack based on BC classes. Untested, and should probably go in some utility class.
	 * See http://stackoverflow.com/questions/5186793/fileformat-for-ec-public-private-keys. 
	 */
	private static PublicKey getECPublicKeyFromPrivateKey(PrivateKey privateKey) throws GeneralSecurityException {
		KeyFactory keyFactory = KeyFactory.getInstance("EC", BC_PROVIDER);
		org.bouncycastle.jce.provider.JCEECPrivateKey priv = (org.bouncycastle.jce.provider.JCEECPrivateKey)privateKey;
		org.bouncycastle.jce.spec.ECParameterSpec params = priv.getParameters();
		org.bouncycastle.jce.spec.ECPublicKeySpec pubKS = new org.bouncycastle.jce.spec.ECPublicKeySpec(params.getG().multiply(priv.getD()), params);
		PublicKey publicKey = keyFactory.generatePublic(pubKS);
		return publicKey;
	}

	private Action getGenerateEACKeys() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -3333362506867899044L;

			public void actionPerformed(ActionEvent e) {
				try {
					KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", BC_PROVIDER);
					generator.initialize(new ECGenParameterSpec(EC_CURVE_NAME));
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
				fileChooser.setFileFilter(FileUtil.KEY_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					PrivateKey key = readPrivateKeyFromFile(file, "RSA");
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
				fileChooser.setFileFilter(FileUtil.KEY_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					PublicKey pubKey = readPublicKeyFromFile(file, "RSA");
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
				fileChooser.setFileFilter(FileUtil.KEY_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
					File file = fileChooser.getSelectedFile();
					preferences.put(JMRTDApp.CERT_AND_KEY_FILES_DIR_KEY, file.getParent());
					passport.setAAPrivateKey(readPrivateKeyFromFile(file, "RSA"));
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
					InputStream sodInputStream = passport.getInputStream(PassportService.EF_SOD);
					SODFile	sod = new SODFile(sodInputStream);
					JFrame certificateFrame = new CertificateChainFrame("Document Signer Certificate", sod.getDocSigningCertificate(), false);
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

	private Action getViewAAPublicKeyAction() {
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -3064369119565468811L;

			public void actionPerformed(ActionEvent e) {
				try {
					InputStream dg15InputStream = passport.getInputStream(PassportService.EF_DG15);
					DG15File dg15 = new DG15File(dg15InputStream);
					PublicKey pubKey = dg15.getPublicKey();
					KeyFrame keyFrame = new KeyFrame("Active Authentication Public Key", pubKey);
					keyFrame.pack();
					keyFrame.setVisible(true);
				} catch (CardServiceException cse) {
					cse.printStackTrace();
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
					try {
						InputStream dg1InputStream = passport.getInputStream(PassportService.EF_DG1);
						DG1File dg1 = new DG1File(dg1InputStream);
						MRZInfo mrzInfo = dg1.getMRZInfo();
						bacEntry = new BACKey(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());
					} catch (CardServiceException cse) {
						cse.printStackTrace();
						// bacEntry = null;
					} catch (Exception ex) {
						ex.printStackTrace();
						LOGGER.severe("Could not decode DG1. " + ex.getMessage());
					}
				}
				List<Short> fileList = passport.getFileList();
				PublicKey aaPublicKey = null;
				if (fileList.contains(PassportService.EF_DG15)) {
					try {
						InputStream dg15InputStream = passport.getInputStream(PassportService.EF_DG15);
						if (dg15InputStream != null) {
							DG15File dg15 = new DG15File(dg15InputStream);
							aaPublicKey = dg15.getPublicKey();
						}
					} catch (CardServiceException cse) {
						cse.printStackTrace();
						// aaPublicKey = null;
					} catch (Exception ex) {
						ex.printStackTrace();
						LOGGER.severe("Could not decode DG15. " + ex.getMessage());
					}
				}
				UploadOptionsChooser chooser = new UploadOptionsChooser(bacEntry, aaPublicKey);
				int choice = chooser.showOptionsDialog(getContentPane());
				switch (choice) {
				case UploadOptionsChooser.APPROVE_OPTION:
					CardTerminal terminal = chooser.getSelectedTerminal();
					boolean wasPolling = cm.isPolling(terminal);
					try {
						cm.stopPolling(terminal);
						// FIXME: have to wait for the poller to actually stop?
						PassportPersoService persoService = new PassportPersoService(CardService.getInstance(terminal));
						if (apduListener != null) { persoService.addAPDUListener(apduListener); }
						persoService.open();
						if (chooser.isBACSelected()) {
							persoService.setBAC(bacEntry.getDocumentNumber(), bacEntry.getDateOfBirth(), bacEntry.getDateOfExpiry());
						}
						if (aaPublicKey != null) {
							PrivateKey aaPrivateKey = passport.getAAPrivateKey();
							if(aaPrivateKey != null) {
								persoService.putPrivateKey(aaPrivateKey);
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
							if (fileBytes == null) {
								LOGGER.warning("DEBUG: skipping " + Integer.toHexString(fid) + ", file not present?");
								continue;
							}
							persoService.createFile(fid, (short)fileBytes.length);
							persoService.selectFile(fid);
							ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
							persoService.writeFile(fid, inputStream);
						}
						persoService.lockApplet();
						persoService.close();
						// TODO: to see when it is done
						// Proper progress bar should be implemented
						LOGGER.info("Passport uploaded.");
						//					} catch (IOException ioe) {
						//						/* NOTE: Do nothing. */
					} catch (CardServiceException cse) {
						// FIXME: show dialog to user, e.g. when no card present or other error.
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

	private Action getChangeDocTypeToAction(final int requestedDocumentType) {
		Action action = actionMap.get("ChangeDocTypeToID" + requestedDocumentType);
		if (action != null) { return action; }
		action = new AbstractAction() {

			private static final long serialVersionUID = -648071841610376908L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					DG1File dg1 = new DG1File(passport.getInputStream(PassportService.EF_DG1));
					MRZInfo mrzInfo = dg1.getMRZInfo();
					String documentCode = mrzInfo.getDocumentCode();
					int originalDocumentType = mrzInfo.getDocumentType();
					if (requestedDocumentType != originalDocumentType) {
						switch(requestedDocumentType) {
						case MRZInfo.DOC_TYPE_ID1: documentCode = "I"; break;
						case MRZInfo.DOC_TYPE_ID2: documentCode = "V"; break;
						case MRZInfo.DOC_TYPE_ID3: documentCode = "P"; break;
						}
					}
					// FIXME: if we go from 3 -> 1, spread personal number over opt.data 1 and opt.data 2?
					mrzInfo.setDocumentCode(documentCode);
					setMRZ(mrzInfo);
				} catch (CardServiceException cse) {
					cse.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
					LOGGER.severe("Could not decode DG1. " + ex.getMessage());
				}
			}
		};
		action.putValue(Action.SMALL_ICON, CHANGE_DOCUMENT_TYPE_TO_ID_ICON);
		action.putValue(Action.LARGE_ICON_KEY, CHANGE_DOCUMENT_TYPE_TO_ID_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Change type to ID" + requestedDocumentType);
		action.putValue(Action.NAME, "ID" + requestedDocumentType);
		actionMap.put("ChangeDocTypeToID" + requestedDocumentType, action);
		return action;
	}

	private Action getChangeDocTypeAction() {
		Action action = actionMap.get("ChangeDocType");
		if (action != null) { return action; }
		action = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			}			
		};
		action.putValue(Action.SMALL_ICON, CHANGE_DOCUMENT_TYPE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, UPLOAD_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Change document type");
		action.putValue(Action.NAME, "Change doc.type");
		actionMap.put("ChangeDocType", action);
		return action;
	}

	private static PrivateKey readPrivateKeyFromFile(File file, String algorithmName) {
		try {
			byte[] key = new byte[(int)file.length()];
			DataInputStream inputStream = new DataInputStream(new FileInputStream(file));
			inputStream.readFully(key);
			inputStream.close();
			KeyFactory kf = null;
			if ("EC".equals(algorithmName)) {
				kf = KeyFactory.getInstance(algorithmName, BC_PROVIDER);
			} else {
				/* e.g., "RSA" */
				kf = KeyFactory.getInstance(algorithmName);
			}
			PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(key);
			return kf.generatePrivate(keysp);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static PublicKey readPublicKeyFromFile(File file, String algorithmName) {
		if ("RSA".equals(algorithmName)) {
			try {
				KeyFactory kf = KeyFactory.getInstance(algorithmName);
				byte[] key = new byte[(int)file.length()];
				DataInputStream inputStream = new DataInputStream(new FileInputStream(file));
				inputStream.readFully(key);
				inputStream.close();
				X509EncodedKeySpec keysp = new X509EncodedKeySpec(key);
				return kf.generatePublic(keysp);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
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
