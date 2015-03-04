/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2014  The JMRTD team
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
import java.io.Serializable;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import net.sf.scuba.smartcards.APDUWrapper;
import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.tlv.TLVOutputStream;
import net.sf.scuba.util.Hex;

import org.jmrtd.cert.CVCAuthorizationTemplate.Role;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.PACEInfo;

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
 *        sendSelectApplet() ==&gt;&lt;br /&gt;
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

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

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

	/** CardAccess. */
	public static final short EF_CARD_ACCESS = 0x011C;

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

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	/**
	 * The file read block size, some passports cannot handle large values
	 * 
	 * @deprecated hack
	 */
	public static int maxBlockSize = 223;

	private static final int SESSION_STOPPED_STATE = 0;

	private static final int SESSION_STARTED_STATE = 1;

	/*
	 * TODO: this should be bit masks, e.g. AA and EAC should be settable both at the same time
	 * Better yet, AA, CA should probably not be part of this state machine at all.
	 */
	private static final int BAC_AUTHENTICATED_STATE = 2;

	//	private static final int AA_AUTHENTICATED_STATE = 3;

	private static final int CA_AUTHENTICATED_STATE = 4;

	private static final int TA_AUTHENTICATED_STATE = 5;

	private int state;

	/**
	 * @deprecated visibility will be set to private
	 */
	protected SecureMessagingWrapper wrapper;

	protected Random random;
	private MRTDFileSystem fs;

	/**
	 * Creates a new passport service for accessing the passport.
	 * 
	 * @param service another service which will deal with sending the apdus to the card
	 * 
	 * @throws CardServiceException on error
	 */
	public PassportService(CardService service) throws CardServiceException {
		super(service);
		random = new SecureRandom(); /* for BAC */
		fs = new MRTDFileSystem(this);

		state = SESSION_STOPPED_STATE;
	}

	/**
	 * Opens a session to the card. As of 0.4.10 this no longer auto selects the passport application,
	 * caller is responsible to call #sendSelectApplet(boolean) now.
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
	 * Selects the MRTD card side applet. If PACE has been executed successfully previously, then the card has authenticated
	 * us and a secure messaging channel has been established. If not, then the caller should request BAC execution as a next
	 * step.
	 * 
	 * @param hasPACESucceeded indicates whether PACE has been executed successfully (in which case a secure messaging channel has been established)
	 * 
	 * @throws CardServiceException on error
	 */
	public void sendSelectApplet(boolean hasPACESucceeded) throws CardServiceException {
		if (hasPACESucceeded) {
			/* Use SM as set up by doPACE() */
			LOGGER.info("DEBUG: wrapper = " + wrapper);
			sendSelectApplet(wrapper, APPLET_AID);
		} else {
			/* Use plain messaging to select the applet, caller will have to do doBAC. */
			sendSelectApplet(null, APPLET_AID);
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
			byte[] keySeed = computeKeySeedForBAC(bacKey);
			SecretKey kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
			SecretKey kMac = Util.deriveKey(keySeed, Util.MAC_MODE);

			try {
				doBAC(kEnc, kMac);
			} catch (CardServiceException cse) {
				LOGGER.warning("BAC failed for BAC key \"" + bacKey + "\"");
				throw cse;
			}
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		}
	}

	/**
	 * Performs the <i>Basic Access Control</i> protocol.
	 * It does BAC using kEnc and kMac keys, usually calculated
	 * from the document number, the card holder's date of birth,
	 * and the card's date of expiry.
	 * 
	 * @param kEnc 3DES key required for BAC
	 * @param kMac 3DES key required for BAC
	 * 
	 * @throws CardServiceException if authentication failed
	 * @throws GeneralSecurityException on security primitives related problems
	 */
	public synchronized void doBAC(SecretKey kEnc, SecretKey kMac) throws CardServiceException, GeneralSecurityException {
		byte[] rndICC = sendGetChallenge();
		byte[] rndIFD = new byte[8];
		random.nextBytes(rndIFD);
		byte[] kIFD = new byte[16];
		random.nextBytes(kIFD);
		byte[] response = sendMutualAuth(rndIFD, rndICC, kIFD, kEnc, kMac);
		byte[] kICC = new byte[16];
		System.arraycopy(response, 16, kICC, 0, 16);
		byte[] keySeed = new byte[16];
		for (int i = 0; i < 16; i++) {
			keySeed[i] = (byte) ((kIFD[i] & 0xFF) ^ (kICC[i] & 0xFF));
		}
		SecretKey ksEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
		SecretKey ksMac = Util.deriveKey(keySeed, Util.MAC_MODE);
		long ssc = Util.computeSendSequenceCounter(rndICC, rndIFD);
		wrapper = new DESedeSecureMessagingWrapper(ksEnc, ksMac, ssc);
		state = BAC_AUTHENTICATED_STATE;
	}

	public synchronized void sendSelectFile(short fid) throws CardServiceException {
		sendSelectFile(wrapper, fid);
	}

	/**
	 * Sends a <code>READ BINARY</code> command to the passport, use wrapper when secure channel set up.
	 * 
	 * @param offset offset into the file
	 * @param le the expected length of the file to read
	 * @param longRead whether to use extended length APDUs
	 * 
	 * @return a byte array of length <code>le</code> with (the specified part of) the contents of the currently selected file
	 *         
	 * @throws CardServiceException on tranceive error
	 */
	public synchronized byte[] sendReadBinary(int offset, int le, boolean longRead) throws CardServiceException {
		return sendReadBinary(wrapper, offset, le, longRead);
	}

	/**
	 * Performs the PACE 2.0 / SAC protocol.
	 * 
	 * @param keySpec the MRZ
	 * @param oid as specified in the PACEInfo, indicates GM or IM, DH or ECDH, cipher, digest, length
	 * @param params explicit static domain parameters the domain params for DH or ECDH
	 *
	 * @throws PACEException on error
	 */
	public synchronized void doPACE(BACKeySpec keySpec, String oid,  AlgorithmParameterSpec params) throws PACEException {
		PACEInfo.MappingType mappingType = PACEInfo.toMappingType(oid); /* Either GM or IM. */
		String agreementAlg = PACEInfo.toKeyAgreementAlgorithm(oid); /* Either DH or ECDH. */
		String cipherAlg  = PACEInfo.toCipherAlgorithm(oid); /* Either DESede or AES. */
		String digestAlg = PACEInfo.toDigestAlgorithm(oid); /* Either SHA-1 or SHA-256. */
		int keyLength = PACEInfo.toKeyLength(oid); /* Of the enc cipher. Either 128, 192, or 256. */

		if (agreementAlg == null) { throw new IllegalArgumentException("Unknown agreement algorithm"); }
		if (!("ECDH".equals(agreementAlg) || "DH".equals(agreementAlg))) {
			throw new IllegalArgumentException("Unsupported agreement algorithm, expected ECDH or DH, found " + agreementAlg);	
		}
		if ("ECDH".equals(agreementAlg)) {
			if (!(params instanceof ECParameterSpec)) { throw new IllegalArgumentException("Expected ECParameterSpec for agreement algorithm " + agreementAlg); }
		} else if ("DH".equals(agreementAlg)) {
			if (!(params instanceof DHParameterSpec)) { throw new IllegalArgumentException("Expected DHParameterSpec for agreement algorithm " + agreementAlg); }
		}

		/* Derive the static key K_pi. This will be used later on, but if derivation fails we want to know in advance. */
		SecretKey staticPACEKey = null;
		Cipher staticPACECipher = null;
		try {
			byte[] keySeed = computeKeySeedForPACE(keySpec);
			staticPACEKey = Util.deriveKey(keySeed, cipherAlg, keyLength, Util.PACE_MODE);
			staticPACECipher = Cipher.getInstance(cipherAlg + "/CBC/NoPadding");
		} catch (GeneralSecurityException gse) {
			throw new PACEException("PCD side error in static PACE key derivation step");
		}

		try {
			/* FIXME: multiple domain params feature not implemented here, for now. */
			byte[] referencePrivateKeyOrForComputingSessionKey = null;

			/* Send to the PICC. */
			sendMSESetATMutualAuth(wrapper, oid, MRZ_PACE_KEY_REFERENCE, referencePrivateKeyOrForComputingSessionKey);
		} catch (CardServiceException cse) {
			throw new PACEException("PICC side error in static PACE key derivation step", cse.getSW());
		}

		/* 
		 * PCD and PICC exchange a chain of general authenticate commands.
		 * Steps 1 to 4 below correspond with steps in table in 3.3 of
		 * ICAO TR-SAC 1.01.
		 */

		/*
		 * 1. Encrypted Nonce 		- --- Absent				- 0x80 Encrypted Nonce
		 * 
		 * Receive encrypted nonce z = E(K_pi, s).
		 * (This is steps 1-3 in Table 4.4 in BSI 03111 2.0.)
		 * 
		 * Decrypt nonce s = D(K_pi, z).
		 * (This is step 4 in Table 4.4 in BSI 03111 2.0.)
		 */
		byte[] piccNonce = null;
		try {
			byte[] step1Data = new byte[] { };
			/* Command data is empty. this implies an empty dynamic authentication object. */
			byte[] step1Response = sendGeneralAuthenticate(wrapper, step1Data, false);
			byte[] step1EncryptedNonce = Util.unwrapDO((byte)0x80, step1Response);

			/* (Re)initialize the K_pi cipher for decryption. */
			staticPACECipher.init(Cipher.DECRYPT_MODE, staticPACEKey, new IvParameterSpec(new byte[16])); /* FIXME: iv length 16 is independent of keylength? */
			piccNonce = staticPACECipher.doFinal(step1EncryptedNonce);
		} catch (GeneralSecurityException gse) {
			LOGGER.severe("Exception: " + gse.getMessage());
			throw new PACEException("PCD side exception in tranceiving nonce step: " + gse.getMessage());
		} catch (CardServiceException cse) {
			throw new PACEException("PICC side exception in tranceiving nonce step", cse.getSW());
		}

		/*
		 * 2. Map Nonce 			- 0x81 Mapping Data			- 0x82 Mapping Data
		 * 
		 * (This is step 3.a) in the protocol in TR-SAC.)
		 * (This is step 5 in Table 4.4 in BSI 03111 2.0.)
		 * 
		 * Receive additional data required for map (i.e. a public key from PICC, and (conditionally) a nonce t).
		 * Compute ephemeral domain parameters D~ = Map(D_PICC, s).
		 */
		KeyAgreement mappingAgreement = null;
		PublicKey pcdMappingPublicKey = null;
		PrivateKey pcdMappingPrivateKey = null;
		AlgorithmParameterSpec ephemeralParams = null;
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(agreementAlg, BC_PROVIDER);
			keyPairGenerator.initialize(params);
			KeyPair kp = keyPairGenerator.generateKeyPair();
			pcdMappingPublicKey = kp.getPublic();
			pcdMappingPrivateKey = kp.getPrivate();
			mappingAgreement = KeyAgreement.getInstance(agreementAlg);
			mappingAgreement.init(pcdMappingPrivateKey);

			byte[] mappingSharedSecretBytes = null;
			byte[] step2Data = null;
			switch(mappingType) {
			case GM:
				/* Encode our public key. */
				byte[] pcdMappingEncodedPublicKey = Util.encodePublicKeyForSmartCard(pcdMappingPublicKey);
				step2Data = pcdMappingEncodedPublicKey;
				break;
			case IM:
				/* TODO: Generate nonce T, send it as step2Data. */
				throw new IllegalStateException("IM not yet implemented"); // FIXME
			}

			step2Data = Util.wrapDO((byte)0x81, step2Data);
			byte[] step2Response = sendGeneralAuthenticate(wrapper, step2Data, false);

			switch(mappingType) {
			case GM:
				byte[] piccMappingEncodedPublicKey = Util.unwrapDO((byte)0x82, step2Response);
				try {
					PublicKey piccMappingPublicKey = Util.decodePublicKeyFromSmartCard(piccMappingEncodedPublicKey, params);
					mappingAgreement.doPhase(piccMappingPublicKey, true);
					mappingSharedSecretBytes = mappingAgreement.generateSecret();
				} catch (GeneralSecurityException gse) {
					LOGGER.severe("Exception: " + gse.getMessage());
					throw new PACEException("Error during mapping" + gse.getMessage());
				}

				ephemeralParams = Util.mapNonceGM(piccNonce, mappingSharedSecretBytes, params);
				break;
			case IM:
				/* NOTE: The context specific data object 0x82 SHALL be empty (TR SAC 3.3.2). */
				throw new IllegalStateException("DEBUG: IM not yet implemented"); // FIXME
			}
		} catch (GeneralSecurityException gse) {
			throw new PACEException("PCD side error in mapping nonce step: " + gse.getMessage());
		} catch (CardServiceException cse) {
			throw new PACEException("PICC side exception in mapping nonce step", cse.getSW());
		}

		/*
		 * 3. Perform Key Agreement	- 0x83 Ephemeral Public Key	- 0x84 Ephemeral Public Key
		 * 
		 * Choose random ephemeral key pair (SK_PCD~, PK_PCD~, D~).
		 * Exchange PK_PCD~ and PK_PICC~ with PICC.
		 * Check that PK_PCD~ and PK_PICC~ differ.
		 * Key agreement K = KA(SK_PCD~, PK_PICC~, D~).
		 * Compute session keys K_mac = KDF_mac(K), K_enc = KDF_enc(K).
		 */
		KeyAgreement keyAgreement = null;
		PublicKey pcdPublicKey = null;
		PrivateKey pcdPrivateKey = null;
		PublicKey piccPublicKey = null;
		byte[] sharedSecretBytes = null;

		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(agreementAlg, BC_PROVIDER);
			keyPairGenerator.initialize(ephemeralParams);
			KeyPair kp = keyPairGenerator.generateKeyPair();
			pcdPublicKey = kp.getPublic();
			pcdPrivateKey = kp.getPrivate();
			keyAgreement = KeyAgreement.getInstance(agreementAlg, BC_PROVIDER);
			keyAgreement.init(pcdPrivateKey);

			byte[] pcdEncodedPublicKey = Util.encodePublicKeyForSmartCard(pcdPublicKey);
			byte[] step3Data = Util.wrapDO((byte)0x83, pcdEncodedPublicKey);
			byte[] step3Response = sendGeneralAuthenticate(wrapper, step3Data, false);
			byte[] piccEncodedPublicKey = Util.unwrapDO((byte)0x84, step3Response);
			piccPublicKey = Util.decodePublicKeyFromSmartCard(piccEncodedPublicKey, ephemeralParams);
			ECPoint piccPublicKeyECPoint = ((ECPublicKey)piccPublicKey).getW();
			BigInteger p = Util.getPrime(ephemeralParams);
			if (pcdPublicKey.equals(piccPublicKey)) { throw new PACEException("PCD's public key and PICC's public key are the same in key agreement step!"); }
			keyAgreement.doPhase(piccPublicKey, true);
			sharedSecretBytes = keyAgreement.generateSecret();			
		} catch (IllegalStateException ise) {
			throw new PACEException("PCD side exception in key agreement step: " + ise.getMessage());
		} catch (GeneralSecurityException gse) {
			throw new PACEException("PCD side exception in key agreement step: " + gse.getMessage());
		} catch (CardServiceException cse) {
			throw new PACEException("PICC side exception in key agreement step", cse.getSW());
		}

		/* Derive secure messaging keys. */
		SecretKey encKey = null;
		SecretKey macKey = null;
		try {
			encKey = Util.deriveKey(sharedSecretBytes, cipherAlg, keyLength, Util.ENC_MODE);
			macKey = Util.deriveKey(sharedSecretBytes, cipherAlg, keyLength, Util.MAC_MODE);
		} catch (GeneralSecurityException gse) {
			LOGGER.severe("Exception: " + gse.getMessage());
			throw new PACEException("Security exception during secure messaging key derivation: " + gse.getMessage());
		}

		/*
		 * 4. Mutual Authentication	- 0x85 Authentication Token	- 0x86 Authentication Token
		 * 
		 * Compute authentication token T_PCD = MAC(K_mac, PK_PICC~).
		 * Exchange authentication token T_PCD and T_PICC with PICC.
		 * Check authentication token T_PICC.
		 */
		try {
			LOGGER.info("DEBUG: macKey = (" + macKey.getEncoded().length + ") " + Hex.bytesToHexString(macKey.getEncoded()));
			byte[] pcdToken = Util.generateAuthenticationToken(oid, macKey, piccPublicKey);
			byte[] step4Data = Util.wrapDO((byte)0x85, pcdToken);
			byte[] step4Response = sendGeneralAuthenticate(wrapper, step4Data, true);
			byte[] piccToken = Util.unwrapDO((byte)0x86, step4Response);
			byte[] expectedPICCToken = Util.generateAuthenticationToken(oid, macKey, pcdPublicKey);
			if (!Arrays.equals(expectedPICCToken, piccToken)) {
				throw new GeneralSecurityException("PICC authentication token mismatch");
			}
		} catch (GeneralSecurityException gse) {
			throw new PACEException("PCD side exception in authentication token generation step: " + gse.getMessage());
		} catch (CardServiceException cse) {
			throw new PACEException("PICC side exception in authentication token generation step", cse.getSW());
		}

		/*
		 * Start secure messaging.
		 * 
		 * 4.6 of TR-SAC: If Secure Messaging is restarted, the SSC is used as follows:
		 *  - The commands used for key agreement are protected with the old session keys and old SSC.
		 *    This applies in particular for the response of the last command used for session key agreement.
		 *  - The Send Sequence Counter is set to its new start value, i.e. within this specification the SSC is set to 0.
		 *  - The new session keys and the new SSC are used to protect subsequent commands/responses.
		 */
		try {
			if (cipherAlg.startsWith("DESede")) {
				wrapper = new DESedeSecureMessagingWrapper(encKey, macKey);
			} else if (cipherAlg.startsWith("AES")) {
				long ssc = wrapper == null ? 0L : wrapper.getSendSequenceCounter();
				wrapper = new AESSecureMessagingWrapper(encKey, macKey, ssc, 0L);
			}
			LOGGER.info("DEBUG: Starting secure messaging based on PACE");
		} catch (GeneralSecurityException gse) {
			LOGGER.severe("Exception: " + gse.getMessage());
			throw new IllegalStateException("Security exception in secure messaging establishment: " + gse.getMessage());
		}
	}

	/**
	 * Perform CA (Chip Authentication) part of EAC (version 1). For details see TR-03110
	 * ver. 1.11. In short, we authenticate the chip with (EC)DH key agreement
	 * protocol and create new secure messaging keys.
	 * 
	 * @param keyId passport's public key id (stored in DG14), -1 if none
	 * @param publicKey passport's public key (stored in DG14)
	 * 
	 * @return the chip authentication result
	 * 
	 * @throws CardServiceException if CA failed or some error occurred
	 */
	public synchronized ChipAuthenticationResult doCA(BigInteger keyId, PublicKey publicKey) throws CardServiceException {
		if (publicKey == null) { throw new IllegalArgumentException("Public key is null"); }
		try {
			String agreementAlg = Util.inferKeyAgreementAlgorithm(publicKey);
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(agreementAlg);
			AlgorithmParameterSpec params = null;
			if ("DH".equals(agreementAlg)) {
				DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
				params = dhPublicKey.getParams();
			} else if ("ECDH".equals(agreementAlg)) {
				ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
				params = ecPublicKey.getParams();
			} else {
				throw new IllegalStateException("Unsupported algorithm \"" + agreementAlg + "\"");
			}
			keyPairGenerator.initialize(params);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();

			KeyAgreement agreement = KeyAgreement.getInstance(agreementAlg);
			agreement.init(keyPair.getPrivate());
			agreement.doPhase(publicKey, true);

			byte[] secret = agreement.generateSecret();

			// TODO: this SHA1ing may have to be removed?
			// TODO: this hashing is needed for our Java Card passport applet implementation
			// byte[] secret = md.digest(secret);

			byte[] keyData = null;
			byte[] idData = null;
			byte[] keyHash = new byte[0];
			if ("DH".equals(agreementAlg)) {
				DHPublicKey dhPublicKey = (DHPublicKey)keyPair.getPublic();
				keyData = dhPublicKey.getY().toByteArray();
				// TODO: this is probably wrong, what should be hashed?
				MessageDigest md = MessageDigest.getInstance("SHA1");
				md = MessageDigest.getInstance("SHA1");
				keyHash = md.digest(keyData);
			} else if ("ECDH".equals(agreementAlg)) {
				org.bouncycastle.jce.interfaces.ECPublicKey ecPublicKey = (org.bouncycastle.jce.interfaces.ECPublicKey)keyPair.getPublic();
				keyData = ecPublicKey.getQ().getEncoded();
				byte[] t = Util.i2os(ecPublicKey.getQ().getX().toBigInteger());
				keyHash = Util.alignKeyDataToSize(t, ecPublicKey.getParameters().getCurve().getFieldSize() / 8);
			}
			keyData = Util.wrapDO((byte)0x91, keyData);
			if (keyId.compareTo(BigInteger.ZERO) >= 0) {
				byte[] keyIdBytes = keyId.toByteArray();
				idData = Util.wrapDO((byte)0x84, keyIdBytes);
			}
			sendMSEKAT(wrapper, keyData, idData);

			SecretKey ksEnc = Util.deriveKey(secret, Util.ENC_MODE);
			SecretKey ksMac = Util.deriveKey(secret, Util.MAC_MODE);
			long ssc = 0;

			wrapper = new DESedeSecureMessagingWrapper(ksEnc, ksMac, ssc);
			state = CA_AUTHENTICATED_STATE;
			return new ChipAuthenticationResult(keyId, publicKey, keyHash, keyPair);
		} catch (GeneralSecurityException e) {
			throw new CardServiceException(e.toString());
		}
	}

	/* From BSI-03110 v1.1, B.2:
	 * 
	 * <pre>
	 * The following sequence of commands SHALL be used to implement Terminal Authentication:
	 * 	1. MSE:Set DST
	 * 	2. PSO:Verify Certificate
	 * 	3. MSE:Set AT
	 * 	4. Get Challenge
	 * 	5. External Authenticate
	 * Steps 1 and 2 are repeated for every CV certificate to be verified
	 * (CVCA Link Certificates, DV Certificate, IS Certificate).
	 * </pre>
	 */
	/**
	 * Perform TA (Terminal Authentication) part of EAC (version 1). For details see
	 * TR-03110 ver. 1.11. In short, we feed the sequence of terminal
	 * certificates to the card for verification, get a challenge from the
	 * card, sign it with terminal private key, and send back to the card
	 * for verification.
	 * 
	 * @param caReference reference issuer
	 * @param terminalCertificates terminal certificate chain
	 * @param terminalKey terminal private key
	 * @param taAlg algorithm
	 * @param chipAuthenticationResult the chip authentication result
	 * @param documentNumber the document number
	 * 
	 * @return the challenge from the card
	 * 
	 * @throws CardServiceException on error
	 */
	public synchronized TerminalAuthenticationResult doTA(CVCPrincipal caReference, List<CardVerifiableCertificate> terminalCertificates,
			PrivateKey terminalKey, String taAlg, ChipAuthenticationResult chipAuthenticationResult, String documentNumber) throws CardServiceException {
		try {
			if (terminalCertificates == null || terminalCertificates.size() < 1) {
				throw new IllegalArgumentException("Need at least 1 certificate to perform TA, found: " + terminalCertificates);
			}

			byte[] caKeyHash = chipAuthenticationResult.getKeyHash();
			/* The key hash that resulted from CA. */
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
					byte[] authorityRefBytes = Util.wrapDO((byte) 0x83, authorityReference.getName().getBytes("ISO-8859-1"));
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
				throw new CardServiceException("No terminal key");
			}

			/* Step 3: MSE Set AT */
			CVCPrincipal holderRef = terminalCert.getHolderReference();
			byte[] holderRefBytes = Util.wrapDO((byte) 0x83, holderRef.getName().getBytes("ISO-8859-1"));
			/* Manage Security Environment: Set for external authentication: Authentication Template */
			sendMSESetATExtAuth(wrapper, holderRefBytes);

			/* Step 4: send get challenge */
			byte[] rPICC = sendGetChallenge(wrapper);

			/* Step 5: external authenticate. */
			/* FIXME: idPICC should be public key in case of PACE. See BSI TR 03110 v2.03 4.4. */
			byte[] idPICC = new byte[documentNumber.length() + 1];
			System.arraycopy(documentNumber.getBytes("ISO-8859-1"), 0, idPICC, 0, documentNumber.length());
			idPICC[idPICC.length - 1] = (byte)MRZInfo.checkDigit(documentNumber);

			ByteArrayOutputStream dtbs = new ByteArrayOutputStream();
			dtbs.write(idPICC);
			dtbs.write(rPICC);
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
			byte[] signedData = sig.sign();
			if (sigAlg.toUpperCase().endsWith("ECDSA")) {
				int keySize = ((org.bouncycastle.jce.interfaces.ECPrivateKey)terminalKey).getParameters().getCurve().getFieldSize() / 8;
				signedData = Util.getRawECDSASignature(signedData, keySize);
			}
			sendMutualAuthenticate(wrapper, signedData);
			state = TA_AUTHENTICATED_STATE;
			return new TerminalAuthenticationResult(chipAuthenticationResult, caReference, terminalCertificates, terminalKey, documentNumber, rPICC);
		} catch (CardServiceException cse) {
			throw cse;
		} catch (Exception e) {
			throw new CardServiceException(e.toString());
		}
	}

	/**
	 * Performs the <i>Active Authentication</i> protocol.
	 * 
	 * @param publicKey the public key to use (usually read from the card)
	 * @param digestAlgorithm the digest algorithm to use, or null
	 * @param signatureAlgorithm signature algorithm
	 * @param challenge challenge
	 * 
	 * @return a boolean indicating whether the card was authenticated
	 * 
	 * @throws CardServiceException on error
	 */
	public byte[] doAA(PublicKey publicKey, String digestAlgorithm, String signatureAlgorithm, byte[] challenge) throws CardServiceException {
		try {
			if (challenge == null || challenge.length != 8) {
				throw new IllegalArgumentException("AA failed: bad challenge");
			}
			byte[] response = sendInternalAuthenticate(wrapper, challenge);			
			return response;
		} catch (IllegalArgumentException iae) {
			LOGGER.severe("Exception: " + iae.getMessage());
			throw new CardServiceException(iae.toString());
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
	public APDUWrapper getWrapper() {
		return wrapper;
	}

	/**
	 * @deprecated hack
	 * 
	 * @param wrapper wrapper
	 */
	public void setWrapper(SecureMessagingWrapper wrapper) {
		this.wrapper = wrapper;
	}

	/**
	 * Gets the file as an input stream indicated by a file identifier.
	 * The resulting input stream will send APDUs to the card.
	 * 
	 * @param fid ICAO file identifier
	 * 
	 * @return the file as an input stream
	 * 
	 * @throws CardServiceException if the file cannot be read
	 */
	public synchronized CardFileInputStream getInputStream(short fid) throws CardServiceException {
		synchronized(fs) {
			fs.selectFile(fid);
			return new CardFileInputStream(maxBlockSize, fs);
		}
	}
	
	private static byte[] computeKeySeedForBAC(BACKeySpec bacKey) throws GeneralSecurityException {
		String documentNumber = bacKey.getDocumentNumber();
		String dateOfBirth = bacKey.getDateOfBirth();
		String dateOfExpiry = bacKey.getDateOfExpiry();

		if (dateOfBirth == null || dateOfBirth.length() != 6) {
			throw new IllegalArgumentException("Wrong date format used for date of birth. Expected yyMMdd, found " + dateOfBirth);
		}
		if (dateOfExpiry == null || dateOfExpiry.length() != 6) {
			throw new IllegalArgumentException("Wrong date format used for date of expiry. Expected yyMMdd, found " + dateOfExpiry);
		}
		if (documentNumber == null) {
			throw new IllegalArgumentException("Wrong document number. Found " + documentNumber);
		}

		documentNumber = fixDocumentNumber(documentNumber);

		byte[] keySeed = Util.computeKeySeedForBAC(documentNumber, dateOfBirth, dateOfExpiry);

		return keySeed;
	}
	
	private static byte[] computeKeySeedForPACE(BACKeySpec bacKey) throws GeneralSecurityException {
		String documentNumber = bacKey.getDocumentNumber();
		String dateOfBirth = bacKey.getDateOfBirth();
		String dateOfExpiry = bacKey.getDateOfExpiry();

		if (dateOfBirth == null || dateOfBirth.length() != 6) {
			throw new IllegalArgumentException("Wrong date format used for date of birth. Expected yyMMdd, found " + dateOfBirth);
		}
		if (dateOfExpiry == null || dateOfExpiry.length() != 6) {
			throw new IllegalArgumentException("Wrong date format used for date of expiry. Expected yyMMdd, found " + dateOfExpiry);
		}
		if (documentNumber == null) {
			throw new IllegalArgumentException("Wrong document number. Found " + documentNumber);
		}

		documentNumber = fixDocumentNumber(documentNumber);

		byte[] keySeed = Util.computeKeySeedForPACE(documentNumber, dateOfBirth, dateOfExpiry);

		return keySeed;
	}

	private static String fixDocumentNumber(String documentNumber) {
		/* The document number, excluding trailing '<'. */
		String minDocumentNumber = documentNumber.replace('<', ' ').trim().replace(' ', '<');

		/* The document number, including trailing '<' until length 9. */
		String maxDocumentNumber = minDocumentNumber;
		while (maxDocumentNumber.length() < 9) {
			maxDocumentNumber += "<";
		}
		return maxDocumentNumber;
	}
}
