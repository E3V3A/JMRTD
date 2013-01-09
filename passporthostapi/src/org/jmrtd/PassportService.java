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
 * $Id:PassportService.java 352 2008-05-19 06:55:21Z martijno $
 */

package org.jmrtd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.tlv.TLVOutputStream;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.MRZInfo;

/**
 * Card service for reading files (such as data groups) and using the BAC and AA
 * protocols on the passport. Defines secure messaging. Defines active
 * authentication.
 * 
 * Based on ICAO-TR-PKI and ICAO-TR-LDS.
 * 
 * Usage:
 * 
 * <pre>
 *        open() ==&gt;&lt;br /&gt;
 *        doBAC(...) ==&gt;&lt;br /&gt;
 *        doAA() ==&gt;&lt;br /&gt;
 *        getInputStream(...)&lt;sup&gt;*&lt;/sup&gt; ==&gt;&lt;br /&gt;
 *        close()
 * </pre>
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision:352 $
 */
public class PassportService extends PassportApduService implements Serializable {

	private static final long serialVersionUID = 1751933705552226972L;

	/** Data group 1 contains the MRZ. */
	public static final short EF_DG1 = 0x0101;

	/** Data group 2 contains face image data. */
	public static final short EF_DG2 = 0x0102;

	/** Data group 3 contains finger print data. */
	public static final short EF_DG3 = 0x0103;

	/** Data group 4 contains iris data. */
	public static final short EF_DG4 = 0x0104;

	/** Data group 5 contains displayed portrait. */
	public static final short EF_DG5 = 0x0105;

	/** Data group 6 is RFU. */
	public static final short EF_DG6 = 0x0106;

	/** Data group 7 contains displayed signature. */
	public static final short EF_DG7 = 0x0107;

	/** Data group 8 contains data features. */
	public static final short EF_DG8 = 0x0108;

	/** Data group 9 contains structure features. */
	public static final short EF_DG9 = 0x0109;

	/** Data group 10 contains substance features. */
	public static final short EF_DG10 = 0x010A;

	/** Data group 11 contains additional personal details. */
	public static final short EF_DG11 = 0x010B;

	/** Data group 12 contains additional document details. */
	public static final short EF_DG12 = 0x010C;

	/** Data group 13 contains optional details. */
	public static final short EF_DG13 = 0x010D;

	/** Data group 14 is RFU. */
	public static final short EF_DG14 = 0x010E;

	/** Data group 15 contains the public key used for Active Authentication. */
	public static final short EF_DG15 = 0x010F;

	/** Data group 16 contains person(s) to notify. */
	public static final short EF_DG16 = 0x0110;

	/** The security document. */
	public static final short EF_SOD = 0x011D;

	/** The data group presence list. */
	public static final short EF_COM = 0x011E;

	/**
	 * File with the EAC CVCA references. Note: this can be overridden by a file
	 * identifier in the DG14 file (TerminalAuthenticationInfo). So check that
	 * one first. Also, this file does not have a header tag, like the others.
	 */
	public static final short EF_CVCA = 0x011C;

	/** Short file identifiers for the DGs */
	public static final byte
	SF_DG1 = 0x01,
	SF_DG2 = 0x02,
	SF_DG3 = 0x03,
	SF_DG4 = 0x04,
	SF_DG5 = 0x05,
	SF_DG6 = 0x06,
	SF_DG7 = 0x07,
	SF_DG8 = 0x08,
	SF_DG9 = 0x09,
	SF_DG10 = 0x0A,
	SF_DG11 = 0x0B,
	SF_DG12 = 0x0C,
	SF_DG13 = 0x0D,
	SF_DG14 = 0x0E,
	SF_DG15 = 0x0F,
	SF_DG16 = 0x10,
	SF_COM = 0x1E,
	SF_SOD = 0x1D,
	SF_CVCA = 0x1C;

	public static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private final int TAG_CVCERTIFICATE_SIGNATURE = 0x5F37;

	/**
	 * The file read block size, some passports cannot handle large values
	 * 
	 * @deprecated hack
	 */
	public static int maxBlockSize = 223;

	private static final int SESSION_STOPPED_STATE = 0;

	private static final int SESSION_STARTED_STATE = 1;

	// TODO: this should be bit masks, e.g. AA and EAC should
	// be settable both at the same time
	private static final int BAC_AUTHENTICATED_STATE = 2;

	private static final int AA_AUTHENTICATED_STATE = 3;

	private static final int CA_AUTHENTICATED_STATE = 4;

	private static final int TA_AUTHENTICATED_STATE = 5;

	private static final int EAC_AUTHENTICATED_STATE = TA_AUTHENTICATED_STATE;

	private int state;

	private byte[] eacKeyHash = new byte[0]; // So that doing doTA without
	// doCA does not give NPE

	private Collection<AuthListener> authListeners;

	/**
	 * @deprecated visibility will be set to private
	 */
	protected SecureMessagingWrapper wrapper;

	private transient Signature aaSignature;
	private transient MessageDigest aaDigest;
	private transient Cipher aaCipher;
	protected Random random;
	private MRTDFileSystem fs;

	/**
	 * Creates a new passport service for accessing the passport.
	 * 
	 * @param service
	 *            another service which will deal with sending the apdus to the
	 *            card.
	 * 
	 * @throws GeneralSecurityException
	 *             when the available JCE providers cannot provide the necessary
	 *             cryptographic primitives.
	 */
	public PassportService(CardService service) throws CardServiceException {
		super(service);
		try {
			aaSignature = Signature.getInstance("SHA1WithRSA/ISO9796-2", JMRTDSecurityProvider.getBouncyCastleProvider());
			aaDigest = MessageDigest.getInstance("SHA1");
			aaCipher = Cipher.getInstance("RSA/NONE/NoPadding");
			random = new SecureRandom();
			authListeners = new ArrayList<AuthListener>();
			fs = new MRTDFileSystem(this);
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		}
		state = SESSION_STOPPED_STATE;
	}

	/**
	 * Opens a session. This is done by connecting to the card, selecting the
	 * passport application.
	 * 
	 * @throws CardServiceException on error
	 */
	public void open() throws CardServiceException {
		if (isOpen()) {
			return;
		}
		synchronized(this) {
			super.open();
			state = SESSION_STARTED_STATE;
		}
	}

	/**
	 * Whether this service is open.
	 * 
	 * @return a boolean
	 */
	public boolean isOpen() {
		return (state != SESSION_STOPPED_STATE);
	}

	/**
	 * Performs the <i>Basic Access Control</i> protocol.
	 * 
	 * @param bacKey the key based on the document number,
	 *               the card holder's birth date,
	 *               and the document's expiry date
	 * 
	 * @throws CardServiceException if authentication failed
	 */
	public synchronized void doBAC(BACKeySpec bacKey) throws CardServiceException {
		try {
			String documentNumber = bacKey.getDocumentNumber();
			if (documentNumber == null || documentNumber.length() < 9) {
				throw new IllegalArgumentException("Document number must have length at least 9");
			}
			String dateOfBirth = bacKey.getDateOfBirth();
			String dateOfExpiry = bacKey.getDateOfExpiry();
			byte[] keySeed = Util.computeKeySeed(documentNumber, dateOfBirth, dateOfExpiry);
			SecretKey kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
			SecretKey kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
			byte[] rndICC = sendGetChallenge();
			byte[] rndIFD = new byte[8];
			random.nextBytes(rndIFD);
			byte[] kIFD = new byte[16];
			random.nextBytes(kIFD);
			byte[] response = sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
			byte[] kICC = new byte[16];
			System.arraycopy(response, 16, kICC, 0, 16);
			keySeed = new byte[16];
			for (int i = 0; i < 16; i++) {
				keySeed[i] = (byte) ((kIFD[i] & 0xFF) ^ (kICC[i] & 0xFF));
			}
			SecretKey ksEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
			SecretKey ksMac = Util.deriveKey(keySeed, Util.MAC_MODE);
			long ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
			wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);
			BACEvent event = new BACEvent(this, rndICC, rndIFD, kICC, kIFD, true);
			notifyBACPerformed(event);
			state = BAC_AUTHENTICATED_STATE;
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		} catch (UnsupportedEncodingException uee) {
			throw new CardServiceException(uee.toString());
		}
	}

	public synchronized void sendSelectFile(short fid) throws CardServiceException {
		sendSelectFile(wrapper, fid);
	}

	/**
	 * Sends a <code>READ BINARY</code> command to the passport, use wrapper when secure channel set up.
	 * 
	 * @param offset
	 *            offset into the file
	 * @param le
	 *            the expected length of the file to read
	 * 
	 * @return a byte array of length <code>le</code> with (the specified part
	 *         of) the contents of the currently selected file
	 */
	public synchronized byte[] sendReadBinary(int offset, int le, boolean longRead) throws CardServiceException {
		return sendReadBinary(wrapper, offset, le, longRead);
	}

	/**
	 * Performs the EAC protocol with the passport. For details see TR-03110
	 * ver. 1.11. In short: a. authenticate the chip with (EC)DH key agreement
	 * protocol (new secure messaging keys are created then), b. feed the
	 * sequence of terminal certificates to the card for verification. c. get a
	 * challenge from the passport, sign it with terminal private key, send back
	 * to the card for verification.
	 * 
	 * @param keyId passport's public key id (stored in DG14), -1 if none
	 * @param publicKey passport's public key (stored in DG14)
	 * @param caReference the CA certificate key reference, this can be read from the CVCA file
	 * @param terminalCertificates the chain of terminal certificates
	 * @param terminalKey terminal private key
	 * @param documentNumber the passport number
	 *
	 * @throws CardServiceException on error
	 */
	public synchronized void doEAC(BigInteger keyId, PublicKey publicKey,
			CVCPrincipal caReference, List<CardVerifiableCertificate> terminalCertificates,
			PrivateKey terminalKey, String documentNumber) throws CardServiceException {
		KeyPair keyPair = null;
		byte[] rpicc = null;
		try {
			keyPair = doCA(keyId, publicKey);
			assert(terminalCertificates != null && terminalCertificates.size() == 3);
			rpicc = doTA(caReference, terminalCertificates, terminalKey, null, null, documentNumber);
			state = EAC_AUTHENTICATED_STATE;
		} catch (CardServiceException cse) {
			throw cse;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			EACEvent event = new EACEvent(this, keyId, publicKey, keyPair, caReference, terminalCertificates, terminalKey, documentNumber, rpicc, state == EAC_AUTHENTICATED_STATE);
			notifyEACPerformed(event);
		}
	}

	/**
	 * Perform CA (Chip Authentication) part of EAC. For details see TR-03110
	 * ver. 1.11. In short, we authenticate the chip with (EC)DH key agreement
	 * protocol and create new secure messaging keys.
	 * 
	 * @param keyId
	 *            passport's public key id (stored in DG14), -1 if none.
	 * @param publicKey
	 *            passport's public key (stored in DG14).
	 * @throws CardServiceException
	 *             if CA failed or some error occurred
	 */
	public synchronized KeyPair doCA(BigInteger keyId, PublicKey publicKey) throws CardServiceException {
		if (publicKey == null) { throw new IllegalArgumentException("Public key is null"); }
		try {
			String algName = (publicKey instanceof ECPublicKey) ? "ECDH" : "DH";
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algName);
			AlgorithmParameterSpec spec = null;
			if ("DH".equals(algName)) {
				DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
				spec = dhPublicKey.getParams();
			} else if ("ECDH".equals(algName)) {
				ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
				spec = ecPublicKey.getParams();
			} else {
				throw new IllegalStateException("Unsupported algorithm \"" + algName + "\"");
			}
			keyPairGenerator.initialize(spec);

			KeyPair keyPair = keyPairGenerator.generateKeyPair();

			KeyAgreement agreement = KeyAgreement.getInstance(algName);
			agreement.init(keyPair.getPrivate());
			agreement.doPhase(publicKey, true);

			byte[] secret = agreement.generateSecret();

			// TODO: this SHA1ing may have to be removed?
			// TODO: this hashing is needed for our passport applet implementation
			// byte[] secret = md.digest(secret);

			byte[] keyData = null;
			byte[] idData = null;
			if ("DH".equals(algName)) {
				DHPublicKey dhPublicKey = (DHPublicKey)keyPair.getPublic();
				keyData = dhPublicKey.getY().toByteArray();
				// TODO: this is probably wrong, what should be hashed?
				MessageDigest md = MessageDigest.getInstance("SHA1");
				md = MessageDigest.getInstance("SHA1");
				eacKeyHash = md.digest(keyData);
			} else if ("ECDH".equals(algName)) {
				org.bouncycastle.jce.interfaces.ECPublicKey ecPublicKey =
						(org.bouncycastle.jce.interfaces.ECPublicKey)keyPair.getPublic();
				keyData = ecPublicKey.getQ().getEncoded();
				byte[] t = ecPublicKey.getQ().getX().toBigInteger().toByteArray();
				eacKeyHash = alignKeyDataToSize(t, ecPublicKey.getParameters().getCurve().getFieldSize() / 8);
			} else {
				throw new IllegalStateException("Unsupported algorithm \"" + algName + "\", don't know how to select hash function");
			}
			keyData = wrapDO((byte) 0x91, keyData);
			if (keyId.compareTo(BigInteger.ZERO) >= 0) {
				byte[] keyIdBytes = keyId.toByteArray();
				idData = wrapDO((byte) 0x84, keyIdBytes);
			}
			sendMSEKAT(wrapper, keyData, idData);
			SecretKey ksEnc = Util.deriveKey(secret, Util.ENC_MODE);
			SecretKey ksMac = Util.deriveKey(secret, Util.MAC_MODE);
			long ssc = 0;
			wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);
			state = CA_AUTHENTICATED_STATE;
			return keyPair;
		} catch (GeneralSecurityException e) {
			throw new CardServiceException(e.toString());
		}
	}

	/**
	 * Perform TA (Terminal Authentication) part of EAC. For details see
	 * TR-03110 ver. 1.11. In short, we feed the sequence of terminal
	 * certificates to the card for verification, get a challenge from the
	 * passport, sign it with terminal private key, and send back to the card
	 * for verification.
	 * 
	 * From BSI-03110 v1.1, B.2:
	 * 
	 * The following sequence of commands SHALL be used to implement Terminal Authentication:
	 * 	1. MSE:Set DST
	 * 	2. PSO:Verify Certificate
	 * 	3. MSE:Set AT
	 * 	4. Get Challenge
	 * 	5. External Authenticate
	 * Steps 1 and 2 are repeated for every CV certificate to be verified
	 * (CVCA Link Certificates, DV Certificate, IS Certificate).
	 */
	public synchronized byte[] doTA(CVCPrincipal caReference, List<CardVerifiableCertificate> terminalCertificates, PrivateKey terminalKey,
			String taAlg, byte[] caKeyHash, String documentNumber) throws CardServiceException {
		try {
			if (terminalCertificates == null || terminalCertificates.size() < 1) {
				throw new IllegalArgumentException("Need at least 1 certificate to perform TA, found: " + terminalCertificates);
			}

			/* The key hash that resulted from CA. FIXME: don't rely on global var for this -- MO */
			if (caKeyHash == null) {
				caKeyHash = eacKeyHash;
			}
			if (caKeyHash == null) {
				throw new IllegalArgumentException("CA key hash is null");
			}

			/* FIXME: check that terminalCertificates holds a (inverted, i.e. issuer before subject) chain. */

			/* Check if first cert is/has the expected CVCA, and remove it from chain if it is the CVCA. */
			CardVerifiableCertificate firstCert = terminalCertificates.get(0);
			Role firstCertRole = firstCert.getAuthorizationTemplate().getRole();
			if (Role.CVCA.equals(firstCertRole)) {
				CVCPrincipal firstCertHolderReference = firstCert.getHolderReference();
				if (caReference != null && !caReference.equals(firstCertHolderReference)) {
					throw new CardServiceException("First certificate holds wrong authority, found " + firstCertHolderReference.getName() + ", expected " + caReference.getName());
				}
				if (caReference == null) {
					caReference = firstCertHolderReference;
				}
				terminalCertificates.remove(0);
			}
			CVCPrincipal firstCertAuthorityReference = firstCert.getAuthorityReference();
			if (caReference != null && !caReference.equals(firstCertAuthorityReference)) {
				throw new CardServiceException("First certificate not signed by expected CA, found " + firstCertAuthorityReference.getName() + ",  expected " + caReference.getName());
			}
			if (caReference == null) {
				caReference = firstCertAuthorityReference;
			}

			/* Check if the last cert is an IS cert. */
			CardVerifiableCertificate lastCert = terminalCertificates.get(terminalCertificates.size() - 1);
			Role lastCertRole = lastCert.getAuthorizationTemplate().getRole();
			if (!Role.IS.equals(lastCertRole)) {
				throw new CardServiceException("Last certificate in chain (" + lastCert.getHolderReference().getName() + ") does not have role IS, but has role " + lastCertRole);
			}
			CardVerifiableCertificate terminalCert = lastCert;

			/* Have the MRTD check our chain. */
			for (CardVerifiableCertificate cert: terminalCertificates) {
				try {
					CVCPrincipal authorityReference = cert.getAuthorityReference();

					/* Step 1: MSE:SetDST */
					/* Manage Security Environment: Set for verification: Digital Signature Template,
					 * indicate authority of cert to check.
					 */
					byte[] authorityRefBytes = wrapDO((byte) 0x83, authorityReference.getName().getBytes("ISO-8859-1"));
					sendMSESetDST(wrapper, authorityRefBytes);

					/* Cert body is already in TLV format. */
					byte[] body = cert.getCertBodyData();

					/* Signature not yet in TLV format, prefix it with tag and length. */
					byte[] signature = cert.getSignature();
					ByteArrayOutputStream sigOut = new ByteArrayOutputStream();
					TLVOutputStream tlvSigOut = new TLVOutputStream(sigOut);
					tlvSigOut.writeTag(TAG_CVCERTIFICATE_SIGNATURE);
					tlvSigOut.writeValue(signature);
					tlvSigOut.close();
					signature = sigOut.toByteArray();

					/* Step 2: PSO:Verify Certificate */
					sendPSOExtendedLengthMode(wrapper, body, signature);					
				} catch (CardServiceException cse) {
					throw cse;
				} catch (Exception e) {
					/* FIXME: Does this mean we failed to authenticate? -- MO */
					throw new CardServiceException(e.getMessage());
				}
			}

			if (terminalKey == null) {
				return new byte[0];
			}

			/* Step 3: MSE Set AT */
			CVCPrincipal holderRef = terminalCert.getHolderReference();
			byte[] holderRefBytes = wrapDO((byte) 0x83, holderRef.getName().getBytes("ISO-8859-1"));
			/* Manage Security Environment: Set for external authentication: Authentication Template */
			sendMSESetAT(wrapper, holderRefBytes);

			/* Step 4: send get challenge */
			byte[] rpicc = sendGetChallenge(wrapper);

			/* Step 5: external authenticate */			
			byte[] idpicc = new byte[documentNumber.length() + 1];
			System.arraycopy(documentNumber.getBytes("ISO-8859-1"), 0, idpicc, 0, documentNumber.length());
			idpicc[idpicc.length - 1] = (byte)MRZInfo.checkDigit(documentNumber);			

			ByteArrayOutputStream dtbs = new ByteArrayOutputStream();
			dtbs.write(idpicc);
			dtbs.write(rpicc);
			dtbs.write(caKeyHash);
			dtbs.close();
			byte[] dtbsBytes = dtbs.toByteArray();

			String sigAlg = terminalCert.getSigAlgName();
			if (sigAlg == null) {
				throw new IllegalStateException("ERROR: Could not determine signature algorithm for terminal certificate " + terminalCert.getHolderReference().getName());
			}
			Signature sig = Signature.getInstance(sigAlg);
			sig.initSign(terminalKey);
			sig.update(dtbsBytes);
			byte[] signature = sig.sign();
			if (sigAlg.toUpperCase().endsWith("ECDSA")) {
				int keySize = ((org.bouncycastle.jce.interfaces.ECPrivateKey)terminalKey).getParameters().getCurve().getFieldSize() / 8;
				signature = getRawECDSASignature(signature, keySize);
			}
			sendMutualAuthenticate(wrapper, signature);
			state = TA_AUTHENTICATED_STATE;
			return rpicc;
		} catch (CardServiceException cse) {
			throw cse;
		} catch (Exception e) {
			throw new CardServiceException(e.toString());
		}
	}

	/**
	 * Adds an authentication event listener.
	 * 
	 * @param l listener
	 */
	public void addAuthenticationListener(AuthListener l) {
		authListeners.add(l);
	}

	/**
	 * Removes an authentication event listener.
	 * 
	 * @param l listener
	 */
	public void removeAuthenticationListener(AuthListener l) {
		authListeners.remove(l);
	}

	/**
	 * For ECDSA the EAC 1.11 specification requires the signature to be
	 * stripped down from any ASN.1 wrappers, as so.
	 *
	 * @param signature
	 * @param keySize

	 * @return signature without wrappers
	 * 
	 * @throws IOException on error
	 */
	private byte[] getRawECDSASignature(byte[] signature, int keySize) throws IOException {
		ASN1InputStream asn1In = new ASN1InputStream(signature);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ASN1Sequence obj = (ASN1Sequence)asn1In.readObject();
			Enumeration<ASN1Primitive> e = obj.getObjects();
			while (e.hasMoreElements()) {
				ASN1Integer i = (ASN1Integer)e.nextElement();
				byte[] t = i.getValue().toByteArray();
				t = alignKeyDataToSize(t, keySize);
				out.write(t);
			}
			out.flush();
			return out.toByteArray();
		} finally {
			asn1In.close();
			out.close();
		}
	}

	private byte[] alignKeyDataToSize(byte[] keyData, int size) {
		byte[] result = new byte[size];
		if(keyData.length < size) { size = keyData.length; }
		System.arraycopy(keyData, keyData.length - size, result, result.length - size, size);
		return result;
	}

	private byte[] wrapDO(byte tag, byte[] data) {
		byte[] result = new byte[data.length + 2];
		result[0] = tag;
		result[1] = (byte) data.length;
		System.arraycopy(data, 0, result, 2, data.length);
		return result;
	}

	/**
	 * Notifies listeners about BAC events.
	 * 
	 * @param event
	 *            BAC event
	 */
	protected void notifyBACPerformed(BACEvent event) {
		for (AuthListener l : authListeners) {
			l.performedBAC(event);
		}
	}

	/**
	 * Notifies listeners about EAC event.
	 * 
	 * @param event
	 *            EAC event.
	 */
	protected void notifyEACPerformed(EACEvent event) {
		for (AuthListener l : authListeners) {
			l.performedEAC(event);
		}
	}

	/**
	 * Performs the <i>Active Authentication</i> protocol.
	 * 
	 * @param publicKey
	 *            the public key to use (usually read from the card)
	 * 
	 * @return a boolean indicating whether the card was authenticated
	 * 
	 * @throws GeneralSecurityException
	 *             if something goes wrong
	 */
	public boolean doAA(PublicKey publicKey) throws CardServiceException {
		try {
			byte[] m2 = new byte[8];
			random.nextBytes(m2);
			byte[] response = sendAA(publicKey, m2);
			aaCipher.init(Cipher.DECRYPT_MODE, publicKey);
			aaSignature.initVerify(publicKey);
			int digestLength = aaDigest.getDigestLength(); /* should always be 20 */
			assert(digestLength == 20);
			byte[] plaintext = aaCipher.doFinal(response);
			byte[] m1 = Util.recoverMessage(digestLength, plaintext);
			aaSignature.update(m1);
			aaSignature.update(m2);
			boolean success = aaSignature.verify(response);
			AAEvent event = new AAEvent(this, publicKey, m1, m2, success);
			notifyAAPerformed(event);
			if (success) {
				state = AA_AUTHENTICATED_STATE;
			}
			return success;
		} catch (IllegalArgumentException iae) {
			// iae.printStackTrace();
			throw new CardServiceException(iae.toString());
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		}
	}

	/**
	 * Closes this service.
	 */
	public void close() {
		try {
			wrapper = null;
			super.close();
		} finally {
			state = SESSION_STOPPED_STATE;
		}
	}

	/**
	 * Gets the wrapper. Returns <code>null</code> until BAC has been
	 * performed.
	 * 
	 * @return the wrapper
	 */
	public SecureMessagingWrapper getWrapper() {
		return wrapper;
	}

	/**
	 * @deprecated hack
	 * 
	 * @param wrapper wrapper
	 */
	public void setWrapper(SecureMessagingWrapper wrapper) {
		this.wrapper = wrapper;
		BACEvent event = new BACEvent(this, null, null, null, null, true);
		notifyBACPerformed(event);
	}

	/**
	 * Gets the file as an input stream indicated by a file identifier.
	 * The resulting input stream will send APDUs to the card.
	 * 
	 * @param fid ICAO file identifier
	 * 
	 * @return the file as an input stream
	 * 
	 * @throws IOException if the file cannot be read
	 */
	public synchronized CardFileInputStream getInputStream(short fid) throws CardServiceException {
		synchronized(fs) {
			fs.selectFile(fid);
			return new CardFileInputStream(maxBlockSize, fs);
		}
	}

	/* ONLY PRIVATE METHODS BELOW */

	/**
	 * Performs the <i>Active Authentication</i> protocol. This method just
	 * gives the response from the card without checking. Use
	 * {@link #doAA(PublicKey)} instead.
	 * 
	 * @param publicKey
	 *            the public key to use (usually read from the card)
	 * @param challenge
	 *            the random challenge of exactly 8 bytes
	 * 
	 * @return response from the card
	 */
	private byte[] sendAA(PublicKey publicKey, byte[] challenge)	throws CardServiceException {
		if (publicKey == null) {
			throw new IllegalArgumentException("AA failed: bad key");
		}
		if (challenge == null || challenge.length != 8) {
			throw new IllegalArgumentException("AA failed: bad challenge");
		}
		byte[] response = sendInternalAuthenticate(wrapper, challenge);
		return response;
	}

	/**
	 * Notifies listeners about AA event.
	 * 
	 * @param event
	 *            AA event.
	 */
	private void notifyAAPerformed(AAEvent event) {
		for (AuthListener l : authListeners) {
			l.performedAA(event);
		}
	}
}
