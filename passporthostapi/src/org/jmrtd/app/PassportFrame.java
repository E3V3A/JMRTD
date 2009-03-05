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
 * $Id$
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
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
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.smartcardio.CardTerminal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
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
import javax.swing.SpringLayout;

import org.ejbca.cvc.CVCertificate;
import org.jmrtd.app.PreferencesPanel.ReadingMode;

import sos.data.Country;
import sos.data.Gender;
import sos.mrtd.COMFile;
import sos.mrtd.CVCAFile;
import sos.mrtd.DG11File;
import sos.mrtd.DG12File;
import sos.mrtd.DG14File;
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
import sos.mrtd.Passport;
import sos.mrtd.PassportFile;
import sos.mrtd.PassportPersoService;
import sos.mrtd.PassportService;
import sos.mrtd.SODFile;
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
 * @version $Revision$
 */
public class PassportFrame extends JFrame
{
	private static final long serialVersionUID = -4624658204381014128L;

	private static final Image JMRTD_ICON = Icons.getImage("jmrtd_logo-48x48");
	private static final String PASSPORT_FRAME_TITLE = "JMRTD - Passport";
	private static final Dimension PREFERRED_SIZE = new Dimension(540, 420);

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
    private static final Icon EAC_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("key_go"));

	private Logger logger = Logger.getLogger(getClass().getSimpleName());

	private FacePreviewPanel facePreviewPanel;

	private JPanel panel, centerPanel, southPanel;
	private JProgressBar progressBar;

    private Passport passport;

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
	private DataGroup dg13;
    private DG14File dg14;
	private DG15File dg15;
	private DataGroup dg16;
	private SODFile sod;
    private CVCAFile cvca;

	private X509Certificate countrySigningCert;
    private PrivateKey aaPrivateKey;
    
    /** EAC related fields */

    private PublicKey eacPassportPubKey = null;
    private PrivateKey terminalPrivateKey = null;
    private List<CVCertificate> terminalCertificates = new ArrayList<CVCertificate>();

    private Set<JComponent> eacComponents = new HashSet<JComponent>();
    
	private VerificationIndicator verificationIndicator;
	private Country issuingState;

	private BACEntry bacEntry;

	public PassportFrame() {
		super(PASSPORT_FRAME_TITLE);
		logger.setLevel(Level.ALL);
		verificationIndicator = new VerificationIndicator();
		panel = new JPanel(new BorderLayout());
		centerPanel = new JPanel(new BorderLayout());
		southPanel = new JPanel();
		progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
		panel.add(centerPanel, BorderLayout.CENTER);
		SpringLayout southLayout = new SpringLayout();
		southPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		//		southLayout.putConstraint(SpringLayout.NORTH, verificationIndicator, 2, SpringLayout.NORTH, southPanel);
		//		southLayout.putConstraint(SpringLayout.WEST, verificationIndicator, 2, SpringLayout.WEST, southPanel);
		//		southLayout.putConstraint(SpringLayout.NORTH, progressBar, 2, SpringLayout.NORTH, southPanel);
		//		southLayout.putConstraint(SpringLayout.EAST, progressBar, 2, SpringLayout.EAST, southPanel);
		southPanel.add(verificationIndicator);
		southPanel.add(progressBar);
		panel.add(southPanel, BorderLayout.SOUTH);
		facePreviewPanel = new FacePreviewPanel(160, 200);
		centerPanel.add(facePreviewPanel, BorderLayout.WEST);
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
	public void readFromService(PassportService service, BACEntry bacEntry, ReadingMode readingMode) throws CardServiceException {
		try {
			this.bacEntry = bacEntry;
			if (bacEntry != null) {
				verificationIndicator.setBACSucceeded();
			}
			long t = System.currentTimeMillis();
			logger.info(Integer.toString((int)(System.currentTimeMillis() - t)/1000));
            passport = new Passport(service);
			displayProgressBar();
			switch (readingMode) {
			case SAFE_MODE:
				verifySecurity(service);
				displayInputStreams();
				verifySecurity(service);
				break;
			case PROGRESSIVE_MODE:
				displayInputStreams(true);
				verifySecurity(service);
				break;
			}
			logger.info(Integer.toString((int)(System.currentTimeMillis() - t)/1000));
		} catch (Exception e) {
			e.printStackTrace();
			dispose();
			return;
		}
	}

	public void readFromZipFile(File file) throws IOException {
		try {
            passport = new Passport(file);
			displayInputStreams();
			verifySecurity(null);
		} catch (Exception e) {
			e.printStackTrace();
			dispose();
		}
	}

	public void readFromEmptyPassport() {
		try {
            passport = new Passport();
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
		displayInputStreams(false);	
	}

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
				switch (fid) {
				case PassportService.EF_COM:
					/* NOTE: Already processed this one above. */
					break;
				case PassportService.EF_DG1:
					/* NOTE: Already processed this one above. */
					break;
				case PassportService.EF_DG2:
					dg2 = new DG2File(in);
					facePreviewPanel.addFaces(dg2.getFaces(), isProgressiveMode);
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
                case PassportService.EF_DG14:
                    dg14 = new DG14File(in);
                    break;
				case PassportService.EF_DG15:
					dg15 = new DG15File(in);
					break;
				case PassportService.EF_SOD:
					/* NOTE: Already processed this one above. */
					break;
				default:
                    if(fid == passport.CVCA_FID) {
                        cvca = new CVCAFile(in);
                    }else{
                      String message = "File " + Integer.toHexString(fid) + " not supported!";
                      JOptionPane.showMessageDialog(getContentPane(), message, "File not supported", JOptionPane.WARNING_MESSAGE);
                    }
				}
			} catch (Exception e) {
				JTextArea message = new JTextArea(5, 15);
				message.append("Exception reading file " + Integer.toHexString(fid) + ": \n" + e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n");
				JOptionPane.showMessageDialog(getContentPane(), new JScrollPane(message), "Problem reading file", JOptionPane.WARNING_MESSAGE);
				continue;
			}
		}
	}

	private void displayHolderInfo() throws IOException {
		InputStream dg1In = passport.getInputStream(PassportService.EF_DG1);
		dg1 = new DG1File(dg1In);
		MRZInfo mrzInfo = dg1.getMRZInfo();
		if (bacEntry != null &&
				!(mrzInfo.getDocumentNumber().equals(bacEntry.getDocumentNumber()) &&
						mrzInfo.getDateOfBirth().equals(bacEntry.getDateOfBirth())) &&
						mrzInfo.getDateOfExpiry().equals(bacEntry.getDateOfExpiry())) {
			JOptionPane.showMessageDialog(getContentPane(), "Problem reading file", "MRZ used in BAC differs from MRZ in DG1!", JOptionPane.WARNING_MESSAGE);
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
				passport.putFile(PassportService.EF_DG1, dg1.getEncoded());
				verificationIndicator.setBACNotChecked();
				verificationIndicator.setAANotChecked();
				verificationIndicator.setDSNotChecked();
				verificationIndicator.setCSNotChecked(null);
			}
		});
	}

	private void displayProgressBar() {
		(new Thread(new Runnable() {
			public void run() {
				try {
                    int totalLength=passport.getTotalLength();
                    progressBar.setMaximum(totalLength);
					while (passport.getBytesRead() <= totalLength) {
                        Thread.sleep(200);
						progressBar.setValue(passport.getBytesRead());
					}
				} catch (InterruptedException ie) {
				} catch (Exception e) {
				}
			}
		})).start();
	}

	private void verifySecurity(PassportService service) {
		verificationIndicator.setBACNotChecked();
		verificationIndicator.setAANotChecked();
		verificationIndicator.setDSNotChecked();
		verificationIndicator.setCSNotChecked(null);
		verifyBAC(service);
		verifyAA(service);
		verifyDS(service);
		verifyCS(service);
	}

	/** Checks whether BAC was used. */
	private void verifyBAC(PassportService service) {

		if (bacEntry != null) {
			verificationIndicator.setBACSucceeded();
		} else {
			verificationIndicator.setBACFailed("BAC not used");
		}
	}

	/** Check active authentication. */
	private void verifyAA(PassportService service) {
		try {
			if (sod == null) {
				InputStream sodIn = passport.getInputStream(PassportService.EF_SOD);
				sod = new SODFile(sodIn);
			}
			if (sod.getDataGroupHashes().get(15) == null) {
				verificationIndicator.setAAFailed("AA not supported (no DG15 hash in EF.SOd)");
			}
			InputStream dg15In = passport.getInputStream(PassportService.EF_DG15);
			if (dg15In != null && service != null) {
				dg15 = new DG15File(dg15In);
				PublicKey pubKey = dg15.getPublicKey();
				if (service.doAA(pubKey)) {
					verificationIndicator.setAASucceeded();
				} else {
					verificationIndicator.setAAFailed("Response to AA incorrect");
				}
			}
		} catch (CardServiceException cse) {
			// cse.printStackTrace();
			verificationIndicator.setAAFailed("AA failed (" + cse.getMessage() + ")");
		}
	}

	/** Checks hashes in the SOd correspond to hashes we compute. */
	private void verifyDS(PassportService service) {
		countrySigningCert = null;
		try {
			InputStream comIn = passport.getInputStream(PassportService.EF_COM);
			com = new COMFile(comIn);
			List<Integer> tagList = com.getTagList();
			Collections.sort(tagList);

			InputStream sodIn = passport.getInputStream(PassportService.EF_SOD);
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

			String digestAlgorithm = sod.getDigestAlgorithm();
			MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);

			for (int dgNumber: hashes.keySet()) {
				int tag = PassportFile.lookupTagByDataGroupNumber(dgNumber);
				short fid = PassportFile.lookupFIDByTag(tag);
				byte[] storedHash = hashes.get(dgNumber);

				digest.reset();

				InputStream dgIn = passport.getInputStream(fid);

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

			X509Certificate docSigningCert = sod.getDocSigningCertificate();
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
	}

	/** Checks country signer certificate, if known. */
	private void verifyCS(PassportService service) {

		if (sod == null) {
			verificationIndicator.setCSFailed("Cannot check CSCA: missing SOD file");
			return; /* NOTE: Serious enough to not perform other checks, leave method. */
		}
		try {
			issuingState = null;
			InputStream dg1In = passport.getInputStream(PassportService.EF_DG1);
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
			sod.getDocSigningCertificate().verify(countrySigningCert.getPublicKey());
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

        menu.addSeparator();
        
        for(JMenuItem item : createEACMenus()) {
            menu.add(item);
            enableEACItems(false);
        }
		return menu;
	}

    private JMenuItem[] createEACMenus() {
        JMenuItem viewPassportKeyItem = new JMenuItem();
        eacComponents.add(viewPassportKeyItem);
        viewPassportKeyItem.setAction(getViewPassportKeyAction());
        JMenuItem viewTerminalKeyItem = new JMenuItem();
        eacComponents.add(viewTerminalKeyItem);
        viewTerminalKeyItem.setAction(getViewTerminalKeyAction());
        JMenuItem viewTerminalCertificateItem = new JMenuItem();
        eacComponents.add(viewTerminalCertificateItem);
        viewTerminalCertificateItem.setAction(getViewTerminalCertificateAction(this));
        return new JMenuItem[] { viewPassportKeyItem, viewTerminalKeyItem, viewTerminalCertificateItem};
    }
    
    private void enableEACItems(boolean enabled) {
        for(JComponent c : eacComponents) {
            c.setEnabled(enabled);
        }
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

		JMenuItem upload = new JMenuItem();
		menu.add(upload);
		upload.setAction(getUploadAction());

		return menu;
	}
    
    /* Menu item actions below... */

    private Action getViewTerminalCertificateAction(final JFrame frame) {
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

    private Action getViewPassportKeyAction() {
        Action action = new AbstractAction() {

            private static final long serialVersionUID = -4351062035608816679L;

            public void actionPerformed(ActionEvent e) {
                    KeyFrame keyFrame = new KeyFrame("EAC Passport Public Key", eacPassportPubKey);
                    keyFrame.pack();
                    keyFrame.setVisible(true);
            }
        };
        action.putValue(Action.SMALL_ICON, KEY_ICON);
        action.putValue(Action.LARGE_ICON_KEY, KEY_ICON);
        action.putValue(Action.SHORT_DESCRIPTION, "View passport public EAC key");
        action.putValue(Action.NAME, "Passport EAC key");
        return action;
    }

    private Action getViewTerminalKeyAction() {
        Action action = new AbstractAction() {

            private static final long serialVersionUID = -4351062035608816679L;

            public void actionPerformed(ActionEvent e) {
                if(terminalPrivateKey != null) {
                    KeyFrame keyFrame = new KeyFrame("Terminal Private Key", terminalPrivateKey);
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
		Action action = new AbstractAction() {

			private static final long serialVersionUID = 9113082315691234764L;

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
				int index = facePreviewPanel.getSelectedIndex();
				if (dg2 == null) {
					InputStream dg2In = passport.getInputStream(PassportService.EF_DG2);
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

			private static final long serialVersionUID = 9003244936310622991L;

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
							InputStream dg2In = passport.getInputStream(PassportService.EF_DG2);
							dg2 = new DG2File(dg2In);
						}
						dg2.addFaceInfo(faceInfo);
						passport.putFile(PassportService.EF_DG2, dg2.getEncoded());
						facePreviewPanel.addFace(faceInfo, false);
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
				int index = facePreviewPanel.getSelectedIndex();
				dg2.removeFaceInfo(index);
				passport.putFile(PassportService.EF_DG2, dg2.getEncoded());
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

			private static final long serialVersionUID = -2441362506867899044L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(Files.CERTIFICATE_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
    				File file = fileChooser.getSelectedFile();
                    X509Certificate cert = readCertFromFile(file);
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

    private Action getLoadDocSignKeyAction() {
        Action action = new AbstractAction() {

            private static final long serialVersionUID = -1001362506867899044L;

            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(Files.KEY_FILE_FILTER);
                int choice = fileChooser.showOpenDialog(getContentPane());
                switch (choice) {
                case JFileChooser.APPROVE_OPTION:
                    File file = fileChooser.getSelectedFile();
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
		Action action = new AbstractAction() {

			private static final long serialVersionUID = -8265676252065941094L;

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(Files.KEY_FILE_FILTER);
				int choice = fileChooser.showOpenDialog(getContentPane());
				switch (choice) {
				case JFileChooser.APPROVE_OPTION:
						File file = fileChooser.getSelectedFile();
						PublicKey pubKey = readPublicRSAKeyFromFile(file);
                        if(pubKey != null) {
                            dg15 = new DG15File(pubKey);
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
                
                        if(p.getPublic() != null) {
                            dg15 = new DG15File(p.getPublic());
                            passport.putFile(PassportService.EF_DG15, dg15.getEncoded());
                        }
                        aaPrivateKey = p.getPrivate();
                }catch(Exception ex) {
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
        Action action = new AbstractAction() {

            private static final long serialVersionUID = -1265676252065941094L;

            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(Files.KEY_FILE_FILTER);
                int choice = fileChooser.showOpenDialog(getContentPane());
                switch (choice) {
                case JFileChooser.APPROVE_OPTION:
                        File file = fileChooser.getSelectedFile();
                        aaPrivateKey = readPrivateRSAKeyFromFile(file);
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

			private static final long serialVersionUID = -3064369119565468811L;

			public void actionPerformed(ActionEvent e) {
				InputStream in = passport.getInputStream(PassportService.EF_DG15);
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

    private Action getViewAAPrivateKeyAction() {
        Action action = new AbstractAction() {

            private static final long serialVersionUID = -1064369119565468811L;

            public void actionPerformed(ActionEvent e) {
                if(aaPrivateKey != null) {
                KeyFrame keyFrame = new KeyFrame("Active Authentication Private Key", aaPrivateKey);
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
//                        try{ Thread.sleep(2000); }catch(Exception ex) { }
						PassportPersoService persoService = new PassportPersoService(new TerminalCardService(terminal));
						persoService.open();
						if (chooser.isBACSelected()) {
							persoService.setBAC(bacEntry.getDocumentNumber(), bacEntry.getDateOfBirth(), bacEntry.getDateOfExpiry());
						}
						if (aaPublicKey != null && aaPrivateKey != null) {
							persoService.putPrivateKey(aaPrivateKey);
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

       public static PrivateKey readPrivateRSAKeyFromFile(File file) {
            try {
                InputStream fl = fullStream(file);
                byte[] key = new byte[fl.available()];
                KeyFactory kf = KeyFactory.getInstance("RSA");
                fl.read(key, 0, fl.available());
                fl.close();
                PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(key);
                return kf.generatePrivate(keysp);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

       public static PublicKey readPublicRSAKeyFromFile(File file) {
           try {
               InputStream fl = fullStream(file);
               byte[] key = new byte[fl.available()];
               KeyFactory kf = KeyFactory.getInstance("RSA");
               fl.read(key, 0, fl.available());
               fl.close();
               X509EncodedKeySpec keysp = new X509EncodedKeySpec(key);
               return kf.generatePublic(keysp);
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           }
       }

        public static X509Certificate readCertFromFile(File file) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                InputStream certstream = fullStream(file);
                return (X509Certificate) cf.generateCertificate(certstream);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private static InputStream fullStream(File f) throws IOException {
            DataInputStream dis = new DataInputStream(new FileInputStream(f));
            byte[] bytes = new byte[dis.available()];
            dis.readFully(bytes);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            return bais;
        }

}
