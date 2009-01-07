/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.swing.JProgressBar;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import sos.data.Country;
import sos.data.Gender;
import sos.mrtd.COMFile;
import sos.mrtd.DG11File;
import sos.mrtd.DG12File;
import sos.mrtd.DG15File;
import sos.mrtd.DG1File;
import sos.mrtd.DG2File;
import sos.mrtd.DG3File;
import sos.mrtd.DG4File;
import sos.mrtd.DG5File;
import sos.mrtd.DG6File;
import sos.mrtd.DG7File;
import sos.mrtd.DataGroup;
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
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision: 308 $
 */
public class PassportFrame extends JFrame
{
	private static final long serialVersionUID = -4624658204381014128L;

	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48");
	private static final String PASSPORT_FRAME_TITLE = "JMRTD - Passport";
	private static final Dimension PREFERRED_SIZE = new Dimension(520, 420);
	private static final int BUFFER_SIZE = 243;

	private static final Icon CERTIFICATE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("script_key"));
	private static final Icon KEY_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("key"));
	private static final Icon MAGNIFIER_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("magnifier"));
	private static final Icon SAVE_AS_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("disk"));
	private static final Icon CLOSE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("bin"));
	private static final Icon LOAD_IMAGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_image"));
	private static final Icon DELETE_IMAGE_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("image_delete"));
	private static final Icon LOAD_CERT_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_page_white"));
	private static final Icon LOAD_KEY_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("folder_key"));
	private static final Icon UPLOAD_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_burn"));

	private Logger logger = Logger.getLogger(getClass().getSimpleName());

	private FacePreviewPanel facePreviewPanel;

	private JPanel panel, centerPanel, southPanel;
	private JProgressBar progressBar;

	private Map<Short, InputStream> files;
	private Map<Short, InputStream> bufferedStreams;
	private Map<Short, byte[]> fileContents;
	private int bytesRead;
	private int totalLength;

	private COMFile com;
	private DG1File dg1;
	private DG2File dg2;
	private DG3File dg3;
	private DG4File dg4;
	private DG5File dg5;
	private DG6File dg6;
	private DG7File dg7;
	private DataGroup dg8, dg9, dg10;
	private DG11File dg11;
	private DG12File dg12;
	private DataGroup dg13, dg14;
	private DG15File dg15;
	private DataGroup dg16;
	private SODFile sod;

	private X509Certificate docSigningCert, countrySigningCert;

	private VerificationIndicator verificationIndicator;
	private Country issuingState;

	private BACEntry bacEntry;

	public PassportFrame() {
		super(PASSPORT_FRAME_TITLE);
		logger.setLevel(Level.ALL);
		verificationIndicator = new VerificationIndicator();
		panel = new JPanel(new BorderLayout());
		centerPanel = new JPanel(new BorderLayout());
		southPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
		panel.add(centerPanel, BorderLayout.CENTER);
		southPanel.add(verificationIndicator);
		southPanel.add(progressBar);
		panel.add(southPanel, BorderLayout.SOUTH);
		facePreviewPanel = new FacePreviewPanel(160, 200);
		centerPanel.add(facePreviewPanel, BorderLayout.WEST);
		fileContents = new HashMap<Short, byte[]>();
		bufferedStreams = new HashMap<Short, InputStream>();
		files = new HashMap<Short, InputStream>();
		getContentPane().add(panel);
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createToolsMenu());
		setIconImage(JMRTD_ICON);
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
	public void readFromService(PassportService service, BACEntry bacEntry) throws CardServiceException {
		try {
			this.bacEntry = bacEntry;
			if (bacEntry != null) {
				verificationIndicator.setBACSucceeded();
			}
			long t = System.currentTimeMillis();
			logger.info(Integer.toString((int)(System.currentTimeMillis() - t)/1000));
			setupFilesFromServicePassportSource(service);
			displayInputStreams(service);
			verifySecurity(service);
			logger.info(Integer.toString((int)(System.currentTimeMillis() - t)/1000));
		} catch (Exception e) {
			e.printStackTrace();
			dispose();
			return;
		}
	}

	public void readFromZipFile(File file) throws IOException {
		try {
			setupFilesFromZipPassportSource(file);
			displayInputStreams();
			verifySecurity(null);
		} catch (Exception e) {
			e.printStackTrace();
			dispose();
		}
	}

	public void readFromEmptyPassport() {
		try {
			setupFilesFromEmptyPassportSource();
			displayInputStreams();
			verifySecurity(null);
		} catch (Exception e) {
			e.printStackTrace();
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
		displayInputStreams(null);	
	}

	private void displayInputStreams(PassportService service) {	
		try {
			(new Thread(new Runnable() {
				public void run() {
					try {
						progressBar.setMaximum(totalLength);
						while (bytesRead <= totalLength) {
							Thread.sleep(200);
							progressBar.setValue(bytesRead);
						}
					} catch (InterruptedException ie) {
					} catch (Exception e) {
					}
				}
			})).start();

			InputStream dg1In = getFile(PassportService.EF_DG1);
			dg1 = new DG1File(dg1In);
			MRZInfo mrzInfo = dg1.getMRZInfo();
			if (bacEntry != null &&
					!(mrzInfo.getDocumentNumber().equals(bacEntry.getDocumentNumber()) &&
							mrzInfo.getDateOfBirth().equals(bacEntry.getDateOfBirth())) &&
							mrzInfo.getDateOfExpiry().equals(bacEntry.getDateOfExpiry())) {
				System.out.println("WARNING: MRZ used in BAC differs from MRZ in DG1!");
			}
			final HolderInfoPanel holderInfoPanel = new HolderInfoPanel(mrzInfo);
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
					dg1 = new DG1File(updatedMRZInfo);
					putFile(PassportService.EF_DG1, dg1.getEncoded());
					verificationIndicator.setBACNotChecked();
					verificationIndicator.setAANotChecked();
					verificationIndicator.setDSNotChecked();
					verificationIndicator.setCSNotChecked(null);
				}
			});

			for (short fid: files.keySet()) {
				InputStream in = getFile(fid);
				switch (fid) {
				case PassportService.EF_COM:
					/* NOTE: Already processed this one above. */
					break;
				case PassportService.EF_DG1:
					/* NOTE: Already processed this one above. */
					break;
				case PassportService.EF_DG2:
					dg2 = new DG2File(in);
					facePreviewPanel.addFaces(dg2.getFaces());
					break;
				case PassportService.EF_DG3:
					dg3 = new DG3File(in);
					break;
				case PassportService.EF_DG4:
					dg4 = new DG4File(in);
					break;
				case PassportService.EF_DG5:
					dg5 = new DG5File(in);
					break;
				case PassportService.EF_DG6:
					dg6 = new DG6File(in);
					break;
				case PassportService.EF_DG7:
					dg7 = new DG7File(in);
					break;
				case PassportService.EF_DG11:
					dg11 = new DG11File(in);
					break;
				case PassportService.EF_DG12:
					dg12 = new DG12File(in);
					break;
				case PassportService.EF_DG15:
					dg15 = new DG15File(in);
					break;
				case PassportService.EF_SOD:
					/* NOTE: Already processed this one above. */
					break;
				default: System.out.println("WARNING: datagroup not yet supported " + Hex.shortToHexString(fid));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void verifySecurity(PassportService service) {

		verificationIndicator.setBACNotChecked();
		verificationIndicator.setAANotChecked();
		verificationIndicator.setDSNotChecked();
		verificationIndicator.setCSNotChecked(null);

		/* Check whether BAC was used */
		if (bacEntry != null) {
			verificationIndicator.setBACSucceeded();
		} else {
			verificationIndicator.setBACFailed("BAC not used");
		}

		/* Check active authentication */
		try {
			InputStream dg15In = getFile(PassportService.EF_DG15);
			if (dg15In != null && service != null) {
				dg15 = new DG15File(dg15In);
				PublicKey pubKey = dg15.getPublicKey();
				if (service.doAA(pubKey)) {
					verificationIndicator.setAASucceeded();
				} else {
					verificationIndicator.setAAFailed("Response to AA incorrect");
				}
			}
			docSigningCert = null;
			countrySigningCert = null;
		} catch (CardServiceException cse) {
			// cse.printStackTrace();
			verificationIndicator.setAAFailed("AA failed (" + cse.getMessage() + ")");
		}

		/* Check hashes in the SOd correspond to hashes we compute. */
		try {
			InputStream comIn = getFile(PassportService.EF_COM);
			com = new COMFile(comIn);
			List<Integer> tagList = com.getTagList();
			Collections.sort(tagList);

			InputStream sodIn = getFile(PassportService.EF_SOD);
			sod = new SODFile(sodIn);
			Map<Integer, byte[]> hashes = sod.getDataGroupHashes();

			verificationIndicator.setDSNotChecked();

			/* Jeroen van Beek sanity check */
			List<Integer> tagsOfHashes = new ArrayList<Integer>();
			tagsOfHashes.addAll(hashes.keySet());
			Collections.sort(tagsOfHashes);
			if (tagsOfHashes.equals(tagList)) {
				verificationIndicator.setDSFailed("\"Jeroen van Beek sanity check\" failed!");
				return; /* NOTE: Serious enough to not perform other checks, leave method. */
			}

			String algorithm = sod.getDigestAlgorithm();
			MessageDigest digest = MessageDigest.getInstance(algorithm);

			for (int dgNumber: hashes.keySet()) {
				int dgTag = PassportFile.lookupTagByDataGroupNumber(dgNumber);
				short dgFID = PassportFile.lookupFIDByTag(dgTag);
				byte[] storedHash = hashes.get(dgNumber);

				digest.reset();

				InputStream dgIn = getFile(dgFID);

				byte[] buf = new byte[4096];
				while (true) {
					int bytesRead = dgIn.read(buf);
					if (bytesRead < 0) { break; }
					digest.update(buf, 0, bytesRead);
				}
				byte[] computedHash = digest.digest();
				if (!Arrays.equals(storedHash, computedHash)) {
					verificationIndicator.setDSFailed("Authentication of DG" + dgNumber + " failed");
					return; /* NOTE: Serious enough to not perform other checks, leave method. */
				}
			}

			docSigningCert = sod.getDocSigningCertificate();
			if (sod.checkDocSignature(docSigningCert)) {
				verificationIndicator.setDSSucceeded();
			} else {
				verificationIndicator.setDSFailed("DS Signature incorrect");
			}
		} catch (NoSuchAlgorithmException nsae) {
			verificationIndicator.setDSFailed(nsae.getMessage());
			return; /* NOTE: Serious enough to not perform other checks, leave method. */
		} catch (Exception e) {
			e.printStackTrace();
			verificationIndicator.setDSFailed(e.getMessage());
			return; /* NOTE: Serious enough to not perform other checks, leave method. */
		}

		/* Check country signer certificate, if known. */
		if (docSigningCert == null) {
			verificationIndicator.setCSFailed("Cannot check CSCA: missing DS certificate");
			return; /* NOTE: Serious enough to not perform other checks, leave method. */
		}
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
			InputStream cscaIn = cscaFile.openStream();
			if (cscaIn == null) {
				verificationIndicator.setCSFailed("Cannot get CS certificate");
				return; /* NOTE: Serious enough to not perform other checks, leave method. */
			}
			countrySigningCert = (X509Certificate)certFactory.generateCertificate(cscaIn);
			docSigningCert.verify(countrySigningCert.getPublicKey());
			verificationIndicator.setCSSucceeded(); /* NOTE: No exception... verification succeeded! */
		} catch (FileNotFoundException fnfe) {
			verificationIndicator.setCSFailed("Could not open CSCA certificate");
			return; /* NOTE: Serious enough to not perform other checks, leave method. */
		} catch (SignatureException se) {
			verificationIndicator.setCSFailed(se.getMessage());
		} catch (CertificateException ce) {
			verificationIndicator.setCSFailed(ce.getMessage());
		} catch (GeneralSecurityException gse) {
			verificationIndicator.setCSFailed(gse.getMessage());
			gse.printStackTrace();
		} catch (IOException ioe) {
			verificationIndicator.setCSFailed("Could not open CSCA certificate");
		} catch (Exception e) {
			verificationIndicator.setCSFailed(e.getMessage());
		}
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

		menu.addSeparator();

		/* View DS Certificate... */
		JMenuItem viewDocumentSignerCertificate = new JMenuItem();
		menu.add(viewDocumentSignerCertificate);
		viewDocumentSignerCertificate.setAction(getViewDocumentSignerCertificateAction());

		/* View CS Certificate... */
		JMenuItem viewCountrySignerCertificate = new JMenuItem();
		menu.add(viewCountrySignerCertificate);
		viewCountrySignerCertificate.setAction(getViewCountrySignerCertificateAction());

		/* View AA public key */
		JMenuItem viewAAPublicKey = new JMenuItem();
		menu.add(viewAAPublicKey);
		viewAAPublicKey.setAction(getViewAAPublicKeyAction());

		return menu;
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

		/* Replace AA key with another key from file... */
		JMenuItem loadAAKeyFromFile = new JMenuItem();
		menu.add(loadAAKeyFromFile);
		loadAAKeyFromFile.setAction(getLoadAAPublicKeyAction());

		menu.addSeparator();

		JMenuItem upload = new JMenuItem();
		menu.add(upload);
		upload.setAction(getUploadAction());

		return menu;
	}

	/* Menu item actions below... */

	private Action getCloseAction() {
		Action action = new AbstractAction() {
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
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(Files.ZIP_FILE_FILTER);
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
		};
		action.putValue(Action.SMALL_ICON, SAVE_AS_ICON);
		action.putValue(Action.LARGE_ICON_KEY, SAVE_AS_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Save passport to file");
		action.putValue(Action.NAME, "Save As...");
		return action;
	}

	private Action getViewPortraitAtOriginalSizeAction() {
		Action action = new AbstractAction() {
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
		};
		action.putValue(Action.SMALL_ICON, MAGNIFIER_ICON);
		action.putValue(Action.LARGE_ICON_KEY, MAGNIFIER_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "View portrait image at original size");
		action.putValue(Action.NAME, "Portrait at 100%...");
		return action;
	}

	private Action getAddPortraitAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(Files.IMAGE_FILE_FILTER);
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
		};
		action.putValue(Action.SMALL_ICON, LOAD_IMAGE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_IMAGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (additional) portrait from file");
		action.putValue(Action.NAME, "Import portrait...");
		return action;
	}

	private Action getRemovePortraitAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int index = facePreviewPanel.getSelectedIndex();
				dg2.removeFaceInfo(index);
				putFile(PassportService.EF_DG2, dg2.getEncoded());
				facePreviewPanel.removeFace(index);
			}
		};
		action.putValue(Action.SMALL_ICON, DELETE_IMAGE_ICON);
		action.putValue(Action.LARGE_ICON_KEY, DELETE_IMAGE_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Delete selected portrait");
		action.putValue(Action.NAME, "Delete portrait");
		return action;
	}

	private Action getLoadDocSignCertAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(Files.CERTIFICATE_FILE_FILTER);
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
		};
		action.putValue(Action.SMALL_ICON, LOAD_CERT_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_CERT_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (and replace) Document Signer Certificate from file");
		action.putValue(Action.NAME, "Import Doc.Cert...");
		return action;

	}

	private Action getLoadAAPublicKeyAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(Files.KEY_FILE_FILTER);
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
		};
		action.putValue(Action.SMALL_ICON, LOAD_KEY_ICON);
		action.putValue(Action.LARGE_ICON_KEY, LOAD_KEY_ICON);
		action.putValue(Action.SHORT_DESCRIPTION, "Import (and replace) Active Authentication public key from file");
		action.putValue(Action.NAME, "Import AA Pub.Key...");
		return action;
	}

	private Action getViewDocumentSignerCertificateAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JFrame certificateFrame = new CertificateFrame("Document Signer Certificate", docSigningCert);
				certificateFrame.pack();
				certificateFrame.setVisible(true);
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
			public void actionPerformed(ActionEvent e) {
				if (countrySigningCert == null) {
					JOptionPane.showMessageDialog(getContentPane(), "CSCA for " + issuingState.getName() + " not found", "CSCA not found...", JOptionPane.ERROR_MESSAGE);
				} else {
					JFrame certificateFrame = new CertificateFrame("Country Signer Certificate (" + issuingState + ", from file)", countrySigningCert);
					certificateFrame.pack();
					certificateFrame.setVisible(true);
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
			public void actionPerformed(ActionEvent e) {
				InputStream in = getFile(PassportService.EF_DG15);
				dg15 = new DG15File(in);
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

	private Action getUploadAction() {
		Action action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				CardManager cm = CardManager.getInstance();
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
					CardTerminal terminal = chooser.getSelectedTerminal();
					boolean wasPolling = cm.isPolling(terminal);
					try {
						cm.stopPolling(terminal);
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
							System.out.println("DEBUG: createFile " + Integer.toHexString(fid));
							persoService.createFile(fid, (short)fileBytes.length);

							System.out.println("DEBUG: selectFile " + Integer.toHexString(fid));
							persoService.selectFile(fid);

							System.out.println("DEBUG: writeFile " + Integer.toHexString(fid) + " " + fileBytes.length);
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

	private void setupFilesFromServicePassportSource(PassportService service) throws IOException, CardServiceException {
		bytesRead = 0;
		totalLength = 0;
		files.clear();

		CardFileInputStream comIn = service.readFile(PassportService.EF_COM);
		int comLength = comIn.getFileLength();
		BufferedInputStream bufferedComIn = new BufferedInputStream(comIn, comLength + 1);
		totalLength += comLength;
		bufferedComIn.mark(comLength + 1);
		files.put(PassportService.EF_COM, bufferedComIn);
		COMFile com = new COMFile(bufferedComIn);
		for (int tag: com.getTagList()) {
			CardFileInputStream in = service.readDataGroup(tag);
			in.mark(in.getFileLength() + 1);
			files.put(PassportFile.lookupFIDByTag(tag), in);
			totalLength += in.getFileLength();
		}
		bufferedComIn.reset();
		CardFileInputStream sodIn = service.readFile(PassportService.EF_SOD);
		int sodLength = sodIn.getFileLength();
		BufferedInputStream bufferedSodIn = new BufferedInputStream(sodIn, sodLength + 1);
		bufferedSodIn.mark(sodLength + 1);
		totalLength += sodLength;
		files.put(PassportService.EF_SOD, bufferedSodIn);
	}

	private void setupFilesFromZipPassportSource(File file) throws IOException {
		bytesRead = 0;
		totalLength = 0;
		files.clear();

		ZipFile zipIn = new ZipFile(file);
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
				files.put(fid, new ByteArrayInputStream(bytes));
				totalLength += bytes.length;
			} catch (NumberFormatException nfe) {
				/* NOTE: ignore this file */
			}
		}
	}

	private void setupFilesFromEmptyPassportSource() throws GeneralSecurityException {
		bytesRead = 0;
		totalLength = 0;
		files.clear();

		/* EF.COM */
		List<Integer> tagList = new ArrayList<Integer>();
		tagList.add(PassportFile.EF_DG1_TAG);
		tagList.add(PassportFile.EF_DG2_TAG);
		COMFile com = new COMFile("01", "07", "04", "00", "00", tagList);
		byte[] comBytes = com.getEncoded();
		totalLength += comBytes.length;
		files.put(PassportService.EF_COM, new ByteArrayInputStream(comBytes));

		/* EF.DG1 */
		Date today = Calendar.getInstance().getTime();
		String primaryIdentifier = "";
		String[] secondaryIdentifiers = { "" };
		MRZInfo mrzInfo = new MRZInfo(MRZInfo.DOC_TYPE_ID1, Country.NL, primaryIdentifier, secondaryIdentifiers, "", Country.NL, today, Gender.MALE, today, "");
		DG1File dg1 = new DG1File(mrzInfo);
		byte[] dg1Bytes = dg1.getEncoded();
		totalLength += dg1Bytes.length;
		files.put(PassportService.EF_DG1, new ByteArrayInputStream(dg1Bytes));

		/* EF.DG2 */
		DG2File dg2 = new DG2File(); 
		byte[] dg2Bytes = dg2.getEncoded();
		totalLength += dg2Bytes.length;
		files.put(PassportService.EF_DG2, new ByteArrayInputStream(dg2Bytes));

		/* EF.SOD */
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		Date dateOfIssuing = today;
		Date dateOfExpiry = today;
		String digestAlgorithm = "SHA256";
		String signatureAlgorithm = "SHA256withRSA";
		X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
		certGenerator.setSerialNumber(new BigInteger("1"));
		certGenerator.setIssuerDN(new X509Name("C=NL, O=JMRTD, OU=CSCA, CN=jmrtd.org/emailAddress=info@jmrtd.org"));
		certGenerator.setSubjectDN(new X509Name("C=NL, O=JMRTD, OU=DSCA, CN=jmrtd.org/emailAddress=info@jmrtd.org"));
		certGenerator.setNotBefore(dateOfIssuing);
		certGenerator.setNotAfter(dateOfExpiry);
		certGenerator.setPublicKey(publicKey);
		certGenerator.setSignatureAlgorithm(signatureAlgorithm);
		docSigningCert = (X509Certificate)certGenerator.generate(privateKey, "BC");
		// FIXME: docSigningCert == null, generate something here...
		Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
		MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
		hashes.put(1, digest.digest(dg1Bytes));
		hashes.put(2, digest.digest(dg2Bytes));
		byte[] encryptedDigest = new byte[128]; // Arbitrary value. Use a private key to generate a real signature?
		SODFile sod = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, encryptedDigest, docSigningCert);
		byte[] sodBytes = sod.getEncoded();
		totalLength += sodBytes.length;
		files.put(PassportService.EF_SOD, new ByteArrayInputStream(sodBytes));
	}
	
	private void putFile(short fid, byte[] bytes) {
		if (bytes == null) { return; }
		fileContents.put(fid, bytes);
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		in.mark(bytes.length + 1);
		bufferedStreams.put(fid, in);
		totalLength += bytes.length;
	}

	/**
	 * Gets an inputstream that is ready for reading. Makes sure it is reset.
	 * 
	 * @param fid
	 * @return
	 */
	private synchronized InputStream getFile(final short fid) {
		try {
			InputStream in = null;
			byte[] file = fileContents.get(fid);
			if (file != null) {
				/* Already completely read this file. */
				in = new ByteArrayInputStream(file);
				in.mark(file.length + 1);
			} else {
				/* Maybe partially read? Use the buffered stream. */
				in = bufferedStreams.get(fid);
				if (in != null && in.markSupported()) { in.reset(); }
			}
			if (in == null) {
				/* Not read yet. Start reading it. */
				final PassportFrame frame = this;
				final InputStream unBufferedIn = files.get(fid);
				unBufferedIn.reset();
				final PipedInputStream pipedIn = new PipedInputStream(BUFFER_SIZE);
				final PipedOutputStream out = new PipedOutputStream(pipedIn);
				final ByteArrayOutputStream copyOut = new ByteArrayOutputStream();
				bufferedStreams.put(fid, pipedIn);
				(new Thread(new Runnable() {
					public void run() {
						byte[] buf = new byte[BUFFER_SIZE];
						try {
							while (true) {
								int bytesRead = unBufferedIn.read(buf);
								if (bytesRead < 0) { break; }
								out.write(buf, 0, bytesRead);
								copyOut.write(buf, 0, bytesRead);
								frame.bytesRead += bytesRead;
							}
							out.flush(); out.close();
							copyOut.flush();
							byte[] copyOutBytes = copyOut.toByteArray();
							fileContents.put(fid, copyOutBytes);
							copyOut.close();
						} catch (IOException ioe) {
							ioe.printStackTrace();
							/* FIXME: what if something goes wrong inside this thread? */
						}
					}
				})).start();
				in = new BufferedInputStream(pipedIn, BUFFER_SIZE);
				in.mark(BUFFER_SIZE + 1);
			}
			return in;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalStateException("ERROR: " + ioe.toString());
		}
	}

	private byte[] getFileBytes(short fid) {
		byte[] result = fileContents.get(fid);
		if (result != null) { return result; }
		InputStream in = getFile(fid);
		if (in == null) { return null; }
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[256];
		while (true) {
			try {
				int bytesRead = in.read(buf);
				if (bytesRead < 0) { break; }
				out.write(buf, 0, bytesRead);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return out.toByteArray();
	}
}
