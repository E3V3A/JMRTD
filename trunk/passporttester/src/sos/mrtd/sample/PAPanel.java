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
 * $Id: PAPanel.java 894 2009-03-23 15:50:46Z martijno $
 */

package sos.mrtd.sample;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.tlv.BERTLVInputStream;
import net.sourceforge.scuba.tlv.BERTLVObject;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.AAEvent;
import org.jmrtd.AuthListener;
import org.jmrtd.BACEvent;
import org.jmrtd.EACEvent;
import org.jmrtd.PassportApduService;
import org.jmrtd.PassportService;
import org.jmrtd.SecureMessagingWrapper;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.PassportFile;
import org.jmrtd.lds.SODFile;

/**
 * Convenient GUI component examining the security object.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 894 $
 */
public class PAPanel extends JPanel implements AuthListener
{
	private static final Border PANEL_BORDER =
		BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

	private JTextArea area;
	private JButton readObjectButton, readDSCertButton, loadCSCertButton, computeHashButton;

	private PassportApduService apduService;
	private PassportService passportService;

	private SecureMessagingWrapper wrapper;

	private Certificate docSigningCert;
	private Certificate countrySigningCert;

	private Map<Integer, byte[]> storedHashes;

	public PAPanel(PassportApduService service)
	throws CardServiceException {
		super(new BorderLayout());
		setService(service);
		this.wrapper = null;

		final JPanel hashesPanel = new JPanel(new FlowLayout());
		readObjectButton = new JButton("Read from SO");
		hashesPanel.add(readObjectButton);
		readObjectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				try {
					SODFile sodFile =
						new SODFile(passportService.readFile(PassportService.EF_SOD));
					area.append("Read pubkey security object\n");
					storedHashes = sodFile.getDataGroupHashes();
					for (int i: storedHashes.keySet()) {
						area.append("   stored hash of ");
						area.append("DG" + i + ": ");
						area.append(Hex.bytesToHexString(storedHashes.get(i)));
						area.append("\n");
					}
					area.append("\n");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		computeHashButton = new JButton("Compute");
		hashesPanel.add(computeHashButton);
		computeHashButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				(new Thread(new Runnable() {
					public void run() {
						try {
							String alg = "SHA256";
							// TODO: this is a hack... find out how to properly parse the sig. alg from the security object
							if (storedHashes != null && storedHashes.size() > 0
									&& (storedHashes.get(1)).length == 20) {
								alg = "SHA1";
							}
							MessageDigest digest = MessageDigest.getInstance(alg);
							COMFile comFile = new COMFile(passportService.readFile(PassportService.EF_COM));
							List<Integer> tagList = comFile.getTagList();
							for (int i = 0; i < tagList.size(); i++) {
								int dgTag = tagList.get(i);
								int dgNumber = PassportFile.lookupDataGroupNumberByTag(dgTag);
								short dgFID = PassportFile.lookupFIDByTag(dgTag);
								BERTLVInputStream tlvIn = new BERTLVInputStream(passportService.readFile(dgFID));
								int tag = tlvIn.readTag();
								tlvIn.readLength();
								byte[] valueBytes = tlvIn.readValue();
								byte[] file = (new BERTLVObject(tag, valueBytes)).getEncoded();
								byte[] computedHash = digest.digest(file);
								area.append("   computed hash of ");
								area.append("DG" + dgNumber + ": " + Hex.bytesToHexString(computedHash));
								if (storedHashes != null && storedHashes.size() > i
										&& Arrays.equals(storedHashes.get(dgNumber), computedHash)) {
									area.append(" --> OK");
								} else {
									area.append(" --> FAIL");
								}
								area.append("\n");
							}
							area.append("\n");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				})).start();
			}
		});

		JPanel certsPanel = new JPanel();
		readDSCertButton = new JButton("DS cert");
		certsPanel.add(readDSCertButton);
		readDSCertButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				try {
					SODFile sodFile =
						new SODFile(passportService.readFile(PassportService.EF_SOD));
					docSigningCert = sodFile.getDocSigningCertificate();
					area.append("docSigningCert = \n" + docSigningCert);
					area.append("\n");

					boolean succes = sodFile.checkDocSignature(docSigningCert);
					area.append(" --> Signature check: " + succes + "\n");           
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		loadCSCertButton = new JButton("CS cert");
		certsPanel.add(loadCSCertButton);
		loadCSCertButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				try {
					final JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle("Select certificate file");
					// chooser.setCurrentDirectory(currentDir);
					chooser.setFileHidingEnabled(false);
					int n = chooser.showOpenDialog(hashesPanel);
					if (n != JFileChooser.APPROVE_OPTION) {
						return;
					}

					/* DEBUG... */
//					System.out.println("WRITING...");
//					FileOutputStream out = new FileOutputStream("docsigning_cert.der");
//					out.write(docSigningCert.getEncoded());
//					out.flush();
//					out.close();
//
//					System.out.println("DEBUG: docSigningCert.getClass() == " + docSigningCert.getClass());
//
//					System.out.println(((X509Certificate)docSigningCert).getType());

					File file = chooser.getSelectedFile();
					CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
					Certificate countrySigningCert = certFactory.generateCertificate(new FileInputStream(file));
					area.append("Contents of: ");
					area.append("\"" + file.toString() + "\"\n");
					area.append(countrySigningCert.toString() + "\n");
					try {
						area.append(" --> Signature check: ");
						docSigningCert.verify(countrySigningCert.getPublicKey());
						area.append("check: true");
					} catch (Exception e) {
						area.append("check: false - " + e.toString());
						e.printStackTrace();
					} finally {
						area.append("\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		hashesPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Hashes"));
		certsPanel.setBorder(BorderFactory.createTitledBorder(PANEL_BORDER, "Certificates"));
		JPanel leftPanel = new JPanel(new GridLayout(2,1));
		leftPanel.add(hashesPanel);
		leftPanel.add(certsPanel);
		add(leftPanel, BorderLayout.WEST);
		area = new JTextArea(20, 30);
		add(new JScrollPane(area), BorderLayout.CENTER);
	}

	public void setService(PassportApduService service) throws CardServiceException {
		this.apduService = service;
		this.passportService = new PassportService(apduService);
	}

	public void performedBAC(BACEvent be) {
		this.wrapper = be.getWrapper();
		passportService.setWrapper(wrapper);
	}

    public void performedEAC(EACEvent ee) {
        this.wrapper = ee.getWrapper();
        passportService.setWrapper(wrapper);
    }

	public void performedAA(AAEvent ae) {
	}
}
