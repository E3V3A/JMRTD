package org.jmrtd.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import javax.crypto.interfaces.DHPublicKey;
import javax.swing.ActionMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.Passport;
import org.jmrtd.cbeff.BiometricDataBlock;
import org.jmrtd.cbeff.ISO781611;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.DG14File;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.DG3File;
import org.jmrtd.lds.DG4File;
import org.jmrtd.lds.DataGroup;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceImageInfo.FeaturePoint;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.FingerImageInfo;
import org.jmrtd.lds.FingerInfo;
import org.jmrtd.lds.ImageInfo;
import org.jmrtd.lds.IrisBiometricSubtypeInfo;
import org.jmrtd.lds.IrisImageInfo;
import org.jmrtd.lds.IrisInfo;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.SODFile;

public class LDSTreePanel extends JPanel {

	private static final long serialVersionUID = 8507600800214680846L;

	private static final Dimension PREFERRED_SIZE = new Dimension(160, 420);

	private Passport passport;
	private ActionMap actionMap;

	public LDSTreePanel(Passport passport) {
		try {
			setLayout(new BorderLayout());
			this.passport = passport;
			TreeNode root = buildTree(passport);
			add(new JScrollPane(new JTree(root)));
			revalidate();
			setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MutableTreeNode buildTree(Passport passport) throws IOException {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("MRTD");

		List<Short> fileList = passport.getFileList();

		for (short fid: fileList) {
			try {
				InputStream inputStream = passport.getInputStream(fid);
				LDSFile file = LDSFile.getInstance(inputStream);
				rootNode.add(buildTree(file));
			} catch (CardServiceException cse) {
				cse.printStackTrace();
			}
		}
		return rootNode;
	}

	private MutableTreeNode buildTree(LDSFile passportFile) {
		if (passportFile instanceof COMFile) {
			return buildTreeFromCOMFile((COMFile)passportFile);
		}
		if (passportFile instanceof SODFile) {
			return buildTreeFromSODFile((SODFile)passportFile);
		}
		if (passportFile instanceof DataGroup) {
			return buildTreeFromDataGroup((DataGroup)passportFile);
		}
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(passportFile);
		return node;
	}

	private MutableTreeNode buildTreeFromCOMFile(COMFile com) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("COM");
		node.add(new DefaultMutableTreeNode("LDS Version: " + com.getLDSVersion()));
		node.add(new DefaultMutableTreeNode("Unicode version: " + com.getUnicodeVersion()));
		DefaultMutableTreeNode tagsNode = new DefaultMutableTreeNode("Datagroups");
		node.add(tagsNode);
		int[] tagList = com.getTagList();
		for (int tag: tagList) {
			int dgNumber = DataGroup.lookupDataGroupNumberByTag(tag);
			tagsNode.add(new DefaultMutableTreeNode("DG" + dgNumber + " (" + Integer.toHexString(tag) + ")"));
		}
		return node;
	}

	private MutableTreeNode buildTreeFromSODFile(SODFile sod) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("SOd");
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

	private MutableTreeNode buildTreeFromDataGroup(DataGroup dataGroup) {
		switch(dataGroup.getTag()) {
		case LDSFile.EF_DG1_TAG:
			return buildTreeFromDG1((DG1File)dataGroup);
		case LDSFile.EF_DG2_TAG:
			return buildTreeFromDG2((DG2File)dataGroup);
		case LDSFile.EF_DG3_TAG:
			return buildTreeFromDG3((DG3File)dataGroup);
		case LDSFile.EF_DG4_TAG:
			return buildTreeFromDG4((DG4File)dataGroup);
		case LDSFile.EF_DG14_TAG:
			return buildTreeFromDG14((DG14File)dataGroup);
		case LDSFile.EF_DG15_TAG:
			return buildTreeFromDG15((DG15File)dataGroup);
		}
		return new DefaultMutableTreeNode(dataGroup);
	}

	private MutableTreeNode buildTreeFromDG1(DG1File dg1) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("DG1");
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

	private MutableTreeNode buildTreeFromDG2(DG2File dg2) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("DG2");
		List<FaceInfo> faceInfos = dg2.getFaceInfos();
		for (FaceInfo faceInfo: faceInfos) {
			node.add(buildTreeFromFaceInfo(faceInfo));
		}
		return node;
	}

	private MutableTreeNode buildTreeFromDG3(DG3File dg3) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("DG3");
		List<FingerInfo> fingerInfos = dg3.getFingerInfos();
		for (FingerInfo fingerInfo: fingerInfos) {
			node.add(buildTreeFromFingerInfo(fingerInfo));
		}
		return node;
	}

	private MutableTreeNode buildTreeFromDG4(DG4File dg3) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("DG4");
		List<IrisInfo> irisInfos = dg3.getIrisInfos();
		for (IrisInfo irisInfo: irisInfos) {
			node.add(buildTreeFromIrisInfo(irisInfo));
		}
		return node;
	}

	private MutableTreeNode buildTreeFromDG14(DG14File dg14) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("DG14");

		DefaultMutableTreeNode cvcaFileIdsNode = new DefaultMutableTreeNode("TA");
		node.add(cvcaFileIdsNode);
		List<Integer> cvcaFileIds = dg14.getCVCAFileIds();
		for (int fileId: cvcaFileIds) {
			cvcaFileIdsNode.add(new DefaultMutableTreeNode("CVCA File Id: " + fileId + ", as short file Id: " + dg14.getCVCAShortFileId(fileId)));
		}

		DefaultMutableTreeNode caNode = new DefaultMutableTreeNode("CA");
		node.add(caNode);
		Map<Integer, String> caInfos = dg14.getChipAuthenticationInfos();
		for (Map.Entry<Integer, String> entry: caInfos.entrySet()) {
			caNode.add(new DefaultMutableTreeNode(entry.getKey() + " -> " + entry.getValue()));
		}

		DefaultMutableTreeNode caPubKeyNode = new DefaultMutableTreeNode("CA Public Keys");
		node.add(caPubKeyNode);
		Map<Integer, PublicKey> caPubKeyInfos = dg14.getChipAuthenticationPublicKeyInfos();
		for (Map.Entry<Integer, PublicKey> entry: caPubKeyInfos.entrySet()) {
			DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(entry.getKey());
			entryNode.add(buildTreeFromPublicKey(entry.getValue()));
			caPubKeyNode.add(entryNode);
		}

		return node;
	}

	private MutableTreeNode buildTreeFromDG15(DG15File dg15) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("DG15");
		PublicKey publicKey = dg15.getPublicKey();
		node.add(buildTreeFromPublicKey(publicKey));
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

	private MutableTreeNode buildTreeFromFaceInfo(FaceInfo faceInfo) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("FaceInfo");
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
			MutableTreeNode imageNode = buildTreeFromImageInfo(faceImageInfo);
			node.add(imageNode);
		}
		return node;
	}

	private MutableTreeNode buildTreeFromFingerInfo(FingerInfo fingerInfo) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("FingerInfo");
		node.add(buildTreeFromSBH(fingerInfo));
		for (FingerImageInfo fingerImageInfo: fingerInfo.getFingerImageInfos()) {
			node.add(buildTreeFromImageInfo(fingerImageInfo));
		}
		return node;
	}

	private MutableTreeNode buildTreeFromIrisInfo(IrisInfo irisInfo) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("IrisInfo");
		node.add(buildTreeFromSBH(irisInfo));
		for (IrisBiometricSubtypeInfo irisBiometricSubtypeInfo: irisInfo.getIrisBiometricSubtypeInfos()) {
			node.add(buildTreeFromIrisBiometricSubtypeInfo(irisBiometricSubtypeInfo));
		}
		return node;
	}

	private MutableTreeNode buildTreeFromIrisBiometricSubtypeInfo(IrisBiometricSubtypeInfo irisBiometricSubtypeInfo) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("IrisBiometricSubtypeInfo");
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

	private MutableTreeNode buildTreeFromImageInfo(ImageInfo imageInfo) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Image");
		node.add(new DefaultMutableTreeNode("Dimensions: " + imageInfo.getWidth() + " x " + imageInfo.getHeight()));
		node.add(new DefaultMutableTreeNode("Mime-type: \"" + imageInfo.getMimeType() + "\""));
		node.add(new DefaultMutableTreeNode("Encoded record length: " + imageInfo.getRecordLength()));
		node.add(new DefaultMutableTreeNode("Encoded image length: " + imageInfo.getImageLength()));
		return node;
	}

	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}
}
