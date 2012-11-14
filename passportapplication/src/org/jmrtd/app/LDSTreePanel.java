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
 * $Id: KeyFrame.java 893 2009-03-23 15:43:42Z martijno $
 */

package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.crypto.interfaces.DHPublicKey;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import net.sourceforge.scuba.util.Hex;

import org.jmrtd.PassportService;
import org.jmrtd.app.util.ImageUtil;
import org.jmrtd.cbeff.BiometricDataBlock;
import org.jmrtd.cbeff.ISO781611;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.lds.COMFile;
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
import org.jmrtd.lds.DataGroup;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceImageInfo.FeaturePoint;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;
import org.jmrtd.lds.ImageInfo;
import org.jmrtd.lds.IrisBiometricSubtypeInfo;
import org.jmrtd.lds.IrisImageInfo;
import org.jmrtd.lds.IrisInfo;
import org.jmrtd.lds.LDS;
import org.jmrtd.lds.LDSElement;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.SODFile;

/**
 * Panel for navigating the LDS (in edit mode).
 * 
 * FIXME: Create a top level LDS struct in passporthostapi and make treemodel depend on that.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 * 
 * @since 0.4.7
 */
public class LDSTreePanel extends JPanel {

	private static final long serialVersionUID = 8507600800214680846L;

	private static final Dimension PREFERRED_SIZE = new Dimension(160, 420);

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private LDSTreeModel treeModel;

	/**
	 * Constructs a new tree panel.
	 * 
	 * @param document the document
	 */
	public LDSTreePanel(LDS lds) {
		setLayout(new BorderLayout());
		setDocument(lds);
	}

	/**
	 * Reloads the underlying tree model.
	 */
	public void reload() {
		treeModel.reload();
	}

	@Override
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	/* ONLY PRIVATE METHODS BELOW */

	private void setDocument(LDS lds) {
		try {
			System.out.println("DEBUG: setDocument(" + lds + ")");
			if (getComponentCount() > 0) { removeAll(); }
			treeModel = new LDSTreeModel(lds);
			add(new JScrollPane(new JTree(treeModel)));
			revalidate();
			repaint();
			setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private LDSTreeNode buildTree(short fid, InputStream inputStream) {
		switch (fid) {
		case PassportService.EF_CVCA:
			try {
				return buildTreeFromCVCAFile(new CVCAFile(inputStream));
			} catch (Exception e) {
				e.printStackTrace();
				return new LDSTreeNode("File " + Integer.toHexString(fid) + " throws " + e.getMessage());
			}
		case PassportService.EF_COM:
			try {
				return buildTreeFromCOMFile(new COMFile(inputStream));
			} catch (Exception e) {
				e.printStackTrace();
				return new LDSTreeNode("File " + Integer.toHexString(fid) + " throws " + e.getMessage());
			}
		case PassportService.EF_SOD:
			try {
				return buildTreeFromSODFile(new SODFile(inputStream));
			} catch (Exception e) {
				e.printStackTrace();
				return new LDSTreeNode("File " + Integer.toHexString(fid) + " throws " + e.getMessage());
			}
		case PassportService.EF_DG1:
		case PassportService.EF_DG2:
		case PassportService.EF_DG3:
		case PassportService.EF_DG4:
		case PassportService.EF_DG5:
		case PassportService.EF_DG6:
		case PassportService.EF_DG7:
		case PassportService.EF_DG8:
		case PassportService.EF_DG9:
		case PassportService.EF_DG10:
		case PassportService.EF_DG11:
		case PassportService.EF_DG12:
		case PassportService.EF_DG13:
		case PassportService.EF_DG14:
		case PassportService.EF_DG15:
		case PassportService.EF_DG16:
			try {
				return buildTreeFromDataGroup((DataGroup)LDSFileUtil.getLDSFile(fid, inputStream));
			} catch (Exception e) {
				e.printStackTrace();
				return new LDSTreeNode("File " + Integer.toHexString(fid) + " throws " + e.getMessage());
			}
		default: return new LDSTreeNode("File " + Integer.toHexString(fid));
		}
	}

	private LDSTreeNode buildTreeFromCOMFile(COMFile com) {
		LDSTreeNode node = new LDSTreeNode("COM", com);
		node.add(new DefaultMutableTreeNode("LDS Version: " + com.getLDSVersion()));
		node.add(new DefaultMutableTreeNode("Unicode version: " + com.getUnicodeVersion()));
		DefaultMutableTreeNode tagsNode = new DefaultMutableTreeNode("Datagroups");
		node.add(tagsNode);
		int[] tagList = com.getTagList();
		for (int tag: tagList) {
			try {
				int dgNumber = LDSFileUtil.lookupDataGroupNumberByTag(tag);
				tagsNode.add(new DefaultMutableTreeNode("DG" + dgNumber + " (" + Integer.toHexString(tag) + ")"));
			} catch (NumberFormatException nfe) {
				LOGGER.warning("Did not recognize tag in EF_COM tag list: 0x" + Integer.toHexString(tag));
			}
		}
		return node;
	}

	private LDSTreeNode buildTreeFromSODFile(SODFile sod) {
		LDSTreeNode node = new LDSTreeNode("SOd", sod);
		node.add(new DefaultMutableTreeNode("LDS version: " + sod.getLDSVersion()));
		node.add(new DefaultMutableTreeNode("Unicode version: " + sod.getUnicodeVersion()));
		DefaultMutableTreeNode hashesNode = new DefaultMutableTreeNode("Hashes");
		node.add(hashesNode);
		Map<Integer, byte[]> dataGroupHashes = sod.getDataGroupHashes();
		for (Map.Entry<Integer, byte[]> entry: dataGroupHashes.entrySet()) {
			int dgNumber = entry.getKey();
			hashesNode.add(new DefaultMutableTreeNode("DG" + dgNumber + ": " + Hex.bytesToHexString(entry.getValue())));
		}
		node.add(new DefaultMutableTreeNode("Issuer: " + sod.getIssuerX500Principal()));
		node.add(new DefaultMutableTreeNode("Serial number: " + sod.getSerialNumber()));
		node.add(new DefaultMutableTreeNode("Digest algorithm: " + sod.getDigestAlgorithm()));
		node.add(new DefaultMutableTreeNode("Digest encryption algorithm: " + sod.getDigestEncryptionAlgorithm()));
		try {
			Certificate dsCert = sod.getDocSigningCertificate();
			node.add(buildTreeFromCertificate(dsCert, "Document signing certificate"));
		} catch (Exception ce) {
			ce.printStackTrace();
		}
		return node;
	}

	private LDSTreeNode buildTreeFromCVCAFile(CVCAFile cvcaFile) {
		LDSTreeNode node = new LDSTreeNode("CVCA", cvcaFile);
		CVCPrincipal caRef = cvcaFile.getCAReference();
		CVCPrincipal altCARef = cvcaFile.getAltCAReference();
		node.add(new DefaultMutableTreeNode("CA reference: " + (caRef == null ? "" : "\"" + caRef.toString() + "\"")));
		node.add(new DefaultMutableTreeNode("Alt. CA reference: " + (altCARef == null ? "" : "\"" + altCARef.toString() + "\"")));
		return node;
	}

	private LDSTreeNode buildTreeFromDataGroup(DataGroup dataGroup) {
		switch(dataGroup.getTag()) {
		case LDSFile.EF_DG1_TAG:
			return buildTreeFromDG1((DG1File)dataGroup);
		case LDSFile.EF_DG2_TAG:
			return buildTreeFromDG2((DG2File)dataGroup);
		case LDSFile.EF_DG3_TAG:
			return buildTreeFromDG3((DG3File)dataGroup);
		case LDSFile.EF_DG4_TAG:
			return buildTreeFromDG4((DG4File)dataGroup);
		case LDSFile.EF_DG5_TAG:
			return buildTreeFromDG5((DG5File)dataGroup);
		case LDSFile.EF_DG6_TAG:
			return buildTreeFromDG6((DG6File)dataGroup);
		case LDSFile.EF_DG7_TAG:
			return buildTreeFromDG7((DG7File)dataGroup);
		case LDSFile.EF_DG11_TAG:
			return buildTreeFromDG11((DG11File)dataGroup);
		case LDSFile.EF_DG12_TAG:
			return buildTreeFromDG12((DG12File)dataGroup);
		case LDSFile.EF_DG14_TAG:
			return buildTreeFromDG14((DG14File)dataGroup);
		case LDSFile.EF_DG15_TAG:
			return buildTreeFromDG15((DG15File)dataGroup);
		}
		return new LDSTreeNode(dataGroup);
	}

	private LDSTreeNode buildTreeFromDG1(DG1File dg1) {
		LDSTreeNode node = new LDSTreeNode("DG1", dg1);
		MRZInfo mrzInfo = dg1.getMRZInfo();
		node.add(new DefaultMutableTreeNode("Document code: " + mrzInfo.getDocumentCode()));		
		node.add(new DefaultMutableTreeNode("Document number: " + mrzInfo.getDocumentNumber()));
		node.add(new DefaultMutableTreeNode("Last name: " + mrzInfo.getPrimaryIdentifier()));
		DefaultMutableTreeNode firstNamesNode = new DefaultMutableTreeNode("First names");
		node.add(firstNamesNode);
		for (String firstName: mrzInfo.getSecondaryIdentifierComponents()) {
			firstNamesNode.add(new DefaultMutableTreeNode(firstName));
		}
		node.add(new DefaultMutableTreeNode("Issuing state: " + mrzInfo.getIssuingState()));
		node.add(new DefaultMutableTreeNode("Nationality: " + mrzInfo.getNationality()));		
		node.add(new DefaultMutableTreeNode("Date of birth: " + mrzInfo.getDateOfBirth()));
		node.add(new DefaultMutableTreeNode("Date of expiry: " + mrzInfo.getDateOfExpiry()));
		node.add(new DefaultMutableTreeNode("Gender: " + mrzInfo.getGender()));
		node.add(new DefaultMutableTreeNode("Optional data / personal number: \"" + mrzInfo.getPersonalNumber() + "\""));
		return node;
	}

	private LDSTreeNode buildTreeFromDG2(DG2File dg2) {
		LDSTreeNode node = new LDSTreeNode("DG2", dg2);
		List<FaceInfo> faceInfos = dg2.getFaceInfos();
		for (FaceInfo faceInfo: faceInfos) {
			node.add(buildTreeFromFaceInfo(faceInfo));
		}
		return node;
	}

	private LDSTreeNode buildTreeFromDG3(DG3File dg3) {
		LDSTreeNode node = new LDSTreeNode("DG3", dg3);
		List<FingerInfo> fingerInfos = dg3.getFingerInfos();
		for (FingerInfo fingerInfo: fingerInfos) {
			node.add(buildTreeFromFingerInfo(fingerInfo));
		}
		return node;
	}

	private LDSTreeNode buildTreeFromDG4(DG4File dg4) {
		LDSTreeNode node = new LDSTreeNode("DG4", dg4);
		List<IrisInfo> irisInfos = dg4.getIrisInfos();
		for (IrisInfo irisInfo: irisInfos) {
			node.add(buildTreeFromIrisInfo(irisInfo));
		}
		return node;
	}


	private LDSTreeNode buildTreeFromDG5(DG5File dg5) {
		LDSTreeNode node = new LDSTreeNode("DG5", dg5);
		List<DisplayedImageInfo> imageInfos = dg5.getImages();
		DefaultMutableTreeNode imagesNode = new DefaultMutableTreeNode("Images (" + imageInfos.size() + ")");
		node.add(imagesNode);
		for (DisplayedImageInfo imageInfo: imageInfos) {
			MutableTreeNode imageInfoNode =	buildTreeFromImageInfo(imageInfo);
			imagesNode.add(imageInfoNode);
		}
		return node;
	}

	private LDSTreeNode buildTreeFromDG6(DG6File dg6) {
		LDSTreeNode node = new LDSTreeNode("DG6", dg6);
		List<DisplayedImageInfo> imageInfos = dg6.getImages();
		DefaultMutableTreeNode imagesNode = new DefaultMutableTreeNode("Images (" + imageInfos.size() + ")");
		node.add(imagesNode);
		for (DisplayedImageInfo imageInfo: imageInfos) {
			MutableTreeNode imageInfoNode =	buildTreeFromImageInfo(imageInfo);
			imagesNode.add(imageInfoNode);
		}
		return node;
	}

	private LDSTreeNode buildTreeFromDG7(DG7File dg7) {
		LDSTreeNode node = new LDSTreeNode("DG7", dg7);
		List<DisplayedImageInfo> imageInfos = dg7.getImages();
		DefaultMutableTreeNode imagesNode = new DefaultMutableTreeNode("Images (" + imageInfos.size() + ")");
		node.add(imagesNode);
		for (DisplayedImageInfo imageInfo: imageInfos) {
			MutableTreeNode imageInfoNode =	buildTreeFromImageInfo(imageInfo);
			imagesNode.add(imageInfoNode);
		}
		return node;
	}

	private LDSTreeNode buildTreeFromDG11(DG11File dg11) {
		LDSTreeNode node = new LDSTreeNode("DG11", dg11);
		List<Integer> tagsPresent = dg11.getTagPresenceList();
		if (tagsPresent.contains(DG11File.FULL_NAME_TAG)) {
			node.add(new DefaultMutableTreeNode("Full name primary identifier: " + dg11.getFullNamePrimaryIdentifier()));
			List<String> fullNameSecondaryIdentifiers = dg11.getFullNameSecondaryIdentifiers();
			DefaultMutableTreeNode fullNameSecondaryIdentifiersNode = new DefaultMutableTreeNode("Full name secondary identifiers");
			node.add(fullNameSecondaryIdentifiersNode);
			for (String fullNameSecondaryIdentifier: fullNameSecondaryIdentifiers) {
				fullNameSecondaryIdentifiersNode.add(new DefaultMutableTreeNode(fullNameSecondaryIdentifier));
			}
		}
		if (tagsPresent.contains(DG11File.FULL_DATE_OF_BIRTH_TAG)) { node.add(new DefaultMutableTreeNode("Full date of birth: " + dg11.getFullDateOfBirth())); }
		if (tagsPresent.contains(DG11File.PLACE_OF_BIRTH_TAG)) { node.add(new DefaultMutableTreeNode("Place of birth: " + dg11.getPlaceOfBirth())); }
		if (tagsPresent.contains(DG11File.TITLE_TAG)) { node.add(new DefaultMutableTreeNode("Title: " + dg11.getTitle())); }
		if (tagsPresent.contains(DG11File.PROFESSION_TAG)) { node.add(new DefaultMutableTreeNode("Profession: " + dg11.getProfession())); }
		if (tagsPresent.contains(DG11File.TELEPHONE_TAG)) { node.add(new DefaultMutableTreeNode("Telephone: " + dg11.getTelephone())); }
		if (tagsPresent.contains(DG11File.PERSONAL_SUMMARY_TAG)) { node.add(new DefaultMutableTreeNode("Personal summary: " + dg11.getPersonalSummary())); }
		if (tagsPresent.contains(DG11File.CUSTODY_INFORMATION_TAG)) { node.add(new DefaultMutableTreeNode("Custody information: " + dg11.getCustodyInformation())); }
		if (tagsPresent.contains(DG11File.PERMANENT_ADDRESS_TAG)) { node.add(new DefaultMutableTreeNode("Permanent address:" + dg11.getPermanentAddress())); }
		if (tagsPresent.contains(DG11File.PROOF_OF_CITIZENSHIP_TAG)) { node.add(new DefaultMutableTreeNode("Proof of citizenship: byte[" + dg11.getProofOfCitizenship().length + "]")); }
		if (tagsPresent.contains(DG11File.PERSONAL_NUMBER_TAG)) { node.add(new DefaultMutableTreeNode("Personal number: " + dg11.getPersonalNumber())); }
		if (tagsPresent.contains(DG11File.OTHER_VALID_TD_NUMBERS_TAG)) { node.add(new DefaultMutableTreeNode("Other valid TD numbers: " + dg11.getOtherValidTDNumbers())); }
		return node;
	}

	private LDSTreeNode buildTreeFromDG12(DG12File dg12) {
		LDSTreeNode node = new LDSTreeNode("DG12", dg12);
		List<Integer> tagsPresent = dg12.getTagPresenceList();
		if (tagsPresent.contains(DG12File.DATE_AND_TIME_OF_PERSONALIZATION)) { node.add(new DefaultMutableTreeNode("Date and time of personalization: " + dg12.getDateAndTimeOfPersonalization())); }
		if (tagsPresent.contains(DG12File.DATE_OF_ISSUE_TAG)) { node.add(new DefaultMutableTreeNode("Date of issue: " + dg12.getDateOfIssue())); }
		if (tagsPresent.contains(DG12File.ENDORSEMENTS_AND_OBSERVATIONS_TAG)) { node.add(new DefaultMutableTreeNode("Endorsements and observations: " + dg12.getEndorsementsAndObservations())); }
		if (tagsPresent.contains(DG12File.IMAGE_OF_FRONT_TAG)) {
			DefaultMutableTreeNode imageNode = new DefaultMutableTreeNode("Image of front");
			byte[] imageBytes = dg12.getImageOfFront();
			imageNode.add(new DefaultMutableTreeNode("Encoded length: " + imageBytes.length));
			try {
				BufferedImage image = ImageUtil.read(new ByteArrayInputStream(imageBytes), imageBytes.length, "image/jpeg");
				addNodes(image, imageNode);
			} catch (IOException ioe) {
				imageNode.add(new DefaultMutableTreeNode("Error decoding image"));
			}
		}
		if (tagsPresent.contains(DG12File.IMAGE_OF_REAR_TAG)) {
			DefaultMutableTreeNode imageNode = new DefaultMutableTreeNode("Image of rear");
			byte[] imageBytes = dg12.getImageOfRear();
			imageNode.add(new DefaultMutableTreeNode("Encoded length: " + imageBytes.length));
			try {
				BufferedImage image = ImageUtil.read(new ByteArrayInputStream(imageBytes), imageBytes.length, "image/jpeg");
				addNodes(image, imageNode);
			} catch (IOException ioe) {
				imageNode.add(new DefaultMutableTreeNode("Error decoding image"));
			}
		}
		if (tagsPresent.contains(DG12File.ISSUING_AUTHORITY_TAG)) { node.add(new DefaultMutableTreeNode("Issuing authority: " + dg12.getIssuingAuthority())); }
		if (tagsPresent.contains(DG12File.NAME_OF_OTHER_PERSON_TAG)) { node.add(new DefaultMutableTreeNode("Names of other persons: " + dg12.getNamesOfOtherPersons())); }
		if (tagsPresent.contains(DG12File.PERSONALIZATION_SYSTEM_SERIAL_NUMBER_TAG)) { node.add(new DefaultMutableTreeNode("Personalization sytem serial number: " + dg12.getPersonalizationSystemSerialNumber())); }
		if (tagsPresent.contains(DG12File.TAX_OR_EXIT_REQUIREMENTS_TAG)) { node.add(new DefaultMutableTreeNode("a: " + dg12.getTaxOrExitRequirements())); }
		return node;
	}

	private LDSTreeNode buildTreeFromDG14(DG14File dg14) {
		LDSTreeNode node = new LDSTreeNode("DG14", dg14);
		DefaultMutableTreeNode cvcaFileIdsNode = new DefaultMutableTreeNode("TA");
		node.add(cvcaFileIdsNode);
		List<Short> cvcaFileIds = dg14.getCVCAFileIds();
		for (int fileId: cvcaFileIds) {
			cvcaFileIdsNode.add(new DefaultMutableTreeNode("CVCA File Id: " + fileId + ", as short file Id: " + dg14.getCVCAShortFileId(fileId)));
		}
		DefaultMutableTreeNode caNode = new DefaultMutableTreeNode("CA");
		node.add(caNode);
		Map<BigInteger, String> caInfos = dg14.getChipAuthenticationInfos();
		for (Map.Entry<BigInteger, String> entry: caInfos.entrySet()) {
			caNode.add(new DefaultMutableTreeNode(entry.getKey() + ": " + entry.getValue()));
		}
		DefaultMutableTreeNode caPubKeyNode = new DefaultMutableTreeNode("CA Public Keys");
		node.add(caPubKeyNode);
		Map<BigInteger, PublicKey> caPubKeyInfos = dg14.getChipAuthenticationPublicKeyInfos();
		for (Map.Entry<BigInteger, PublicKey> entry: caPubKeyInfos.entrySet()) {
			DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(entry.getKey());
			entryNode.add(buildTreeFromPublicKey(entry.getValue()));
			caPubKeyNode.add(entryNode);
		}
		return node;
	}

	private LDSTreeNode buildTreeFromDG15(DG15File dg15) {
		LDSTreeNode node = new LDSTreeNode("DG15", dg15);
		PublicKey publicKey = dg15.getPublicKey();
		node.add(buildTreeFromPublicKey(publicKey));
		return node;
	}

	private LDSTreeNode buildTreeFromFaceInfo(FaceInfo faceInfo) {
		LDSTreeNode node = new LDSTreeNode("FaceInfo", faceInfo);
		node.add(buildTreeFromSBH(faceInfo));
		for (FaceImageInfo faceImageInfo: faceInfo.getFaceImageInfos()) {
			DefaultMutableTreeNode headerNode = new DefaultMutableTreeNode("Image header");
			headerNode.add(new DefaultMutableTreeNode("Quality: " + faceImageInfo.getQuality()));
			headerNode.add(new DefaultMutableTreeNode("Color space: " + faceImageInfo.getColorSpace()));
			headerNode.add(new DefaultMutableTreeNode("Device type: " + faceImageInfo.getDeviceType()));
			headerNode.add(new DefaultMutableTreeNode("Expression: " + faceImageInfo.getExpression()));
			headerNode.add(new DefaultMutableTreeNode("Eye color: " + faceImageInfo.getEyeColor()));
			headerNode.add(new DefaultMutableTreeNode("Feature mask: " + faceImageInfo.getFeatureMask()));
			FeaturePoint[] featurePoints = faceImageInfo.getFeaturePoints();
			DefaultMutableTreeNode fpNode = new DefaultMutableTreeNode("Feature points (" + featurePoints.length + ")");
			for (FeaturePoint featurePoint: featurePoints) {
				fpNode.add(new DefaultMutableTreeNode(featurePoint.toString()));
			}
			headerNode.add(fpNode);
			node.add(headerNode);
			LDSTreeNode imageNode = buildTreeFromImageInfo(faceImageInfo);
			node.add(imageNode);
		}
		return node;
	}

	private LDSTreeNode buildTreeFromFingerInfo(FingerInfo fingerInfo) {
		LDSTreeNode node = new LDSTreeNode("FingerInfo", fingerInfo);
		node.add(buildTreeFromSBH(fingerInfo));
		for (FingerImageInfo fingerImageInfo: fingerInfo.getFingerImageInfos()) {
			node.add(buildTreeFromImageInfo(fingerImageInfo));
		}
		return node;
	}

	private LDSTreeNode buildTreeFromIrisInfo(IrisInfo irisInfo) {
		LDSTreeNode node = new LDSTreeNode("IrisInfo", irisInfo);
		node.add(buildTreeFromSBH(irisInfo));
		for (IrisBiometricSubtypeInfo irisBiometricSubtypeInfo: irisInfo.getIrisBiometricSubtypeInfos()) {
			node.add(buildTreeFromIrisBiometricSubtypeInfo(irisBiometricSubtypeInfo));
		}
		return node;
	}

	private LDSTreeNode buildTreeFromIrisBiometricSubtypeInfo(IrisBiometricSubtypeInfo irisBiometricSubtypeInfo) {
		LDSTreeNode node = new LDSTreeNode("IrisBiometricSubtypeInfo", irisBiometricSubtypeInfo);
		for (IrisImageInfo irisImageInfo: irisBiometricSubtypeInfo.getIrisImageInfos()) {
			node.add(buildTreeFromImageInfo(irisImageInfo));
		}
		return node;
	}	

	private MutableTreeNode buildTreeFromSBH(BiometricDataBlock bdb) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("SBH");
		Map<Integer, byte[]> sbh = bdb.getStandardBiometricHeader().getElements();
		for (Map.Entry<Integer, byte[]> entry: sbh.entrySet()) {
			int key = entry.getKey();
			byte[] value = entry.getValue();
			node.add(new DefaultMutableTreeNode(getSBHKey(key) + " (" + Integer.toHexString(key) + "): " + Hex.bytesToHexString(value)));
		}
		return node;
	}

	private String getSBHKey(int key) {
		switch(key) {
		case ISO781611.PATRON_HEADER_VERSION_TAG: return "Patron header version";
		case ISO781611.BIOMETRIC_TYPE_TAG: return "Biometric type";
		case ISO781611.BIOMETRIC_SUBTYPE_TAG: return "Biometric sub-type";
		case ISO781611.CREATION_DATE_AND_TIME_TAG: return "Creation date and time";
		case ISO781611.VALIDITY_PERIOD_TAG: return "Validity period";
		case ISO781611.CREATOR_OF_BIOMETRIC_REFERENCE_DATA: return "Creator of biometric reference data";
		case ISO781611.FORMAT_OWNER_TAG: return "Format owner";
		case ISO781611.FORMAT_TYPE_TAG: return "Format type";
		default: return "Unknown";
		}
	}

	private LDSTreeNode buildTreeFromImageInfo(ImageInfo imageInfo) {
		LDSTreeNode node = new LDSTreeNode("Image", imageInfo);
		node.add(new DefaultMutableTreeNode("Mime-type: \"" + imageInfo.getMimeType() + "\""));
		node.add(new DefaultMutableTreeNode("Encoded record length: " + imageInfo.getRecordLength()));
		node.add(new DefaultMutableTreeNode("Encoded image length: " + imageInfo.getImageLength()));
		node.add(new DefaultMutableTreeNode("Reported dimensions: " + imageInfo.getWidth() + " x " + imageInfo.getHeight()));
		try{
			BufferedImage image = ImageUtil.read(imageInfo.getImageInputStream(), imageInfo.getImageLength(), imageInfo.getMimeType());
			addNodes(image, node);
		} catch(IOException e){
			node.add(new DefaultMutableTreeNode("Actual dimensions: unable to decode image"));
		}

		return node;
	}


	private MutableTreeNode buildTreeFromCertificate(Certificate certificate, String nodeName) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeName);
		if (certificate instanceof X509Certificate) {
			node.add(new DefaultMutableTreeNode("Issuer: " + ((X509Certificate) certificate).getIssuerX500Principal()));
			node.add(new DefaultMutableTreeNode("Subject: " + ((X509Certificate) certificate).getIssuerX500Principal()));
			node.add(new DefaultMutableTreeNode("Serial number: " + ((X509Certificate) certificate).getSerialNumber()));
			node.add(new DefaultMutableTreeNode("Not before: " + ((X509Certificate) certificate).getNotBefore()));
			node.add(new DefaultMutableTreeNode("Not after: " + ((X509Certificate) certificate).getNotAfter()));
		} else {
			node.add(new DefaultMutableTreeNode(certificate));
		}
		node.add(buildTreeFromPublicKey(certificate.getPublicKey()));
		return node;
	}

	private MutableTreeNode buildTreeFromPublicKey(PublicKey publicKey) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Public key");
		node.add(new DefaultMutableTreeNode("Public Key Algorithm: \"" + publicKey.getAlgorithm() + "\""));
		if (publicKey instanceof RSAPublicKey) {
			RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
			node.add(new DefaultMutableTreeNode("Modulus: " + rsaPublicKey.getModulus()));
			node.add(new DefaultMutableTreeNode("Public exponent: " + rsaPublicKey.getPublicExponent()));
		} else if (publicKey instanceof ECPublicKey) {
			ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
			node.add(new DefaultMutableTreeNode("W: " + ecPublicKey.getW()));
			node.add(new DefaultMutableTreeNode("Params: " + ecPublicKey.getParams()));
		} else if (publicKey instanceof DHPublicKey) {
			DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
			node.add(new DefaultMutableTreeNode("Y: " + dhPublicKey.getY()));
			node.add(new DefaultMutableTreeNode("Params: " + dhPublicKey.getParams()));
		}
		return node;
	}

	private String imageTypeToString(int type) {
		switch (type) {
		case BufferedImage.TYPE_3BYTE_BGR: return "3 byte bgr";
		case BufferedImage.TYPE_4BYTE_ABGR: return "4 byte abgr";
		case BufferedImage.TYPE_4BYTE_ABGR_PRE: return "4 byte abgr pre";
		case BufferedImage.TYPE_BYTE_BINARY: return "Byte binary";
		case BufferedImage.TYPE_BYTE_GRAY: return "Byte gray";
		case BufferedImage.TYPE_BYTE_INDEXED: return "Byte indexed";
		case BufferedImage.TYPE_INT_ARGB: return "Int argb";
		case BufferedImage.TYPE_INT_ARGB_PRE: return "Int arbg pre";
		case BufferedImage.TYPE_INT_BGR: return "Int bgr";
		case BufferedImage.TYPE_INT_RGB: return "Int rgb";
		case BufferedImage.TYPE_USHORT_555_RGB: return "Ushort 555 rgb";
		case BufferedImage.TYPE_USHORT_565_RGB: return "Ushort 565 rgb";
		case BufferedImage.TYPE_USHORT_GRAY: return "Ushort gray";
		case BufferedImage.TYPE_CUSTOM: return "Custom";
		default: return "Unknown";
		}
	}

	private void addNodes(BufferedImage image, DefaultMutableTreeNode node) {
		node.add(new DefaultMutableTreeNode("Actual dimensions: " + image.getWidth() + " x " + image.getHeight()));
		node.add(new DefaultMutableTreeNode("Actual image type: " + imageTypeToString(image.getType())));
	}

	private class LDSTreeModel extends DefaultTreeModel {

		private static final long serialVersionUID = 3694551659268775436L;

		private LDS lds;

		public LDSTreeModel(LDS lds) {
			super(new DefaultMutableTreeNode("LDS"));
			this.lds = lds;
		}

		@Override
		public Object getChild(Object parent, int index) {
			if (getRoot().equals(parent)) {
				List<Short> files = lds.getFileList();
				Short fid = files.get(index);
				if (fid == null) { LOGGER.severe("Unexpected null fid in passport file list at index " + index); return null; }
				try {
					InputStream inputStream = lds.getInputStream(fid);
					return buildTree(fid, inputStream);
				} catch (Exception cse) {
					cse.printStackTrace();
					try {
						int dgNumber = LDSFileUtil.lookupDataGroupNumberByFID(fid);
						return new DefaultMutableTreeNode("DG" + dgNumber);
					} catch (NumberFormatException nfe) {
						return new DefaultMutableTreeNode("File " + Integer.toHexString(fid));	
					}					
				}
			} else {
				return super.getChild(parent, index);
			}
		}

		@Override
		public int getChildCount(Object parent) {
			if (getRoot().equals(parent)) {
				List<Short> files = lds.getFileList();
				return files.size();
			} else {
				return super.getChildCount(parent);
			}
		}

		@Override
		public boolean isLeaf(Object node) {
			if (getRoot().equals(node)) {
				return false;
			} else if (node instanceof DefaultMutableTreeNode && getRoot().equals(((DefaultMutableTreeNode)node).getParent())) {
				return false;
			} else {
				return super.isLeaf(node);
			}
		}
	}

	private class LDSTreeNode extends DefaultMutableTreeNode {

		private LDSElement ldsInfo;

		public LDSTreeNode(Object userObject) {
			super(userObject);
			if (userObject instanceof LDSElement) {
				this.ldsInfo = (LDSElement)userObject;
			}
		}

		public LDSTreeNode(Object userObject, LDSElement ldsInfo) {
			super(userObject);
			this.ldsInfo = ldsInfo;
		}

		public LDSElement getLDSInfo() {
			return ldsInfo;
		}
	}
}
