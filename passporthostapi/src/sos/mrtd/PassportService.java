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
 * $Id:PassportService.java 352 2008-05-19 06:55:21Z martijno $
 */

package sos.mrtd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;

import org.ejbca.cvc.AlgorithmUtil;
import org.ejbca.cvc.CVCertificate;

import sos.smartcards.CardFileInputStream;
import sos.smartcards.CardService;
import sos.smartcards.CardServiceException;
import sos.smartcards.FileSystemStructured;
import sos.tlv.BERTLVInputStream;
import sos.util.Hex;

/**
 * Card service for reading files (such as data groups) and
 * using the BAC and AA protocols on the passport.
 * Defines secure messaging.
 * Defines active authentication.
 * 
 * Based on ICAO-TR-PKI and ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       open() ==&gt;<br />
 *       doBAC(...) ==&gt;<br />
 *       doAA() ==&gt;<br />
 *       readFile(...)<sup>*</sup> ==&gt;<br />
 *       close()
 *    </pre> 
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @version $Revision:352 $
 */
public class PassportService extends PassportApduService
{
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
	/** File indicating which data groups are present. */
	public static final short EF_COM = 0x011E;
    /** File with the EAC CVCA references. Note: this can be overridden
     *  by a file identifier in the DG14 file (TerminalAuthenticationInfo).
     *  So check that one first. Also, this file does not have a header tag, like the others.
     */
    public static final short EF_CVCA = 0x011C;

    /** Short file identifiers for the DGs */
    
    public static final byte SF_DG1 = 0x01;
    public static final byte SF_DG2 = 0x02;
    public static final byte SF_DG3 = 0x03;
    public static final byte SF_DG4 = 0x04;
    public static final byte SF_DG5 = 0x05;
    public static final byte SF_DG6 = 0x06;
    public static final byte SF_DG7 = 0x07;
    public static final byte SF_DG8 = 0x08;
    public static final byte SF_DG9 = 0x09;
    public static final byte SF_DG10 = 0x0a;
    public static final byte SF_DG11 = 0x0b;
    public static final byte SF_DG12 = 0x0c;
    public static final byte SF_DG13 = 0x0d;
    public static final byte SF_DG14 = 0x0e;
    public static final byte SF_DG15 = 0x0f;
    public static final byte SF_DG16 = 0x10;
    public static final byte SF_COM = 0x1E;
    public static final byte SF_SOD = 0x1D;
    public static final byte SF_CVCA = 0x1C;
    
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	/**
	 * The file read block size, some passports cannot handle large values
	 * @deprecated hack
	 */
	public static int maxBlockSize = 255;

    private static final int SESSION_STOPPED_STATE = 0;
	private static final int SESSION_STARTED_STATE = 1;
    // TODO: this should be bit masks, e.g. AA and EAC should
    // be settable both at the same time 
	private static final int BAC_AUTHENTICATED_STATE = 2;
	private static final int AA_AUTHENTICATED_STATE = 3;
    private static final int EAC_AUTHENTICATED_STATE = 4;

	private int state;

	private Collection<AuthListener> authListeners;

	/**
	 * @deprecated visibility will be set to private
	 */
	protected SecureMessagingWrapper wrapper;
	private Signature aaSignature;
	private MessageDigest aaDigest;
	private Cipher aaCipher;
	private Random random;

	private PassportFileSystem fs;

	/**
	 * Creates a new passport service for accessing the passport.
	 * 
	 * @param service another service which will deal with sending
	 *        the apdus to the card.
	 *
	 * @throws GeneralSecurityException when the available JCE providers
	 *         cannot provide the necessary cryptographic primitives.
	 */
	public PassportService(CardService service) throws CardServiceException {
		super(service);
		try {
			aaSignature = Signature.getInstance("SHA1WithRSA/ISO9796-2"); /* FIXME: SHA1WithRSA also works? */
			aaDigest = MessageDigest.getInstance("SHA1");
			aaCipher = Cipher.getInstance("RSA/NONE/NoPadding");
			random = new SecureRandom();
			authListeners = new ArrayList<AuthListener>();
			fs = new PassportFileSystem();
		} catch (GeneralSecurityException gse) {
			throw new CardServiceException(gse.toString());
		}
		state = SESSION_STOPPED_STATE;
	}

	/**
	 * Opens a session. This is done by connecting to the card, selecting the
	 * passport application.
	 */
	public void open() throws CardServiceException {
		if (isOpen()) {
			return;
		}
		super.open();
		state = SESSION_STARTED_STATE;
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
	 * @param documentNumber the document number
	 * @param dateOfBirth card holder's birth date
	 * @param dateOfExpiry document's expiry date
	 * 
	 * @throws CardServiceException if authentication failed
	 */
	public synchronized void doBAC(String documentNumber, Date dateOfBirth, Date dateOfExpiry)
	throws CardServiceException {
		try {
			byte[] keySeed = Util.computeKeySeed(documentNumber, SDF.format(dateOfBirth), SDF.format(dateOfExpiry));
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

    
    /**
     * Performs the EAC protocol with the passport. For details see
     * TR-03110 ver. 1.11. In short: a. authenticate the chip with (EC)DH
     * key aggrement rotocol (new secure messaging keys are created then),
     * b. feed the sequence of terminal certificates to the card for verification.
     * c. get a challenge from the passport, sign it with terminal private key,
     * send back to the card for verification.
     * 
     * @param keyId
     *            passport's public key id (stored in DG14), -1 if none.
     * @param key
     *            passport's public key (stored in DG14).
     * @param caReference the CA certificate key reference, this can
     *                     be read from the CVCA file
     * @param terminalCertificates
     *            the list/chain of terminal certificates
     * @param terminalKey
     *            terminal private key
     * @param documentNumber
     *            the passport number
     * @return whether EAC was successful
     * @throws CardServiceException
     *             on error
     */
    public synchronized boolean doEAC(int keyId, PublicKey key,
            String caReference,
            List<CVCertificate> terminalCertificates, PrivateKey terminalKey,
            String documentNumber) throws CardServiceException {
        try {

            String algName = (key instanceof ECPublicKey)? "ECDH" : "DH";
            KeyPairGenerator genKey = KeyPairGenerator.getInstance(algName);
            AlgorithmParameterSpec spec = null;
            if("DH".equals(algName)) {
                DHPublicKey k = (DHPublicKey)key;
                spec = k.getParams();          
            }else{
                ECPublicKey k = (ECPublicKey)key;
                spec = k.getParams();                
            }
            genKey.initialize(spec);
            
            KeyPair keyPair = genKey.generateKeyPair();

            KeyAgreement agreement = KeyAgreement.getInstance(algName);
            agreement.init(keyPair.getPrivate());
            agreement.doPhase(key, true);

            // TODO: this SHA1ing may have to be removed?
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] secret = md.digest(agreement.generateSecret());

            byte[] keyData = null;
            byte[] idData = null;
            byte[] keyHash = null;
            if("DH".equals(algName)) {
                DHPublicKey k = (DHPublicKey)keyPair.getPublic();
                keyData = k.getY().toByteArray();
                md = MessageDigest.getInstance("SHA1");
                keyHash = md.digest(keyData);                
            }else{
                org.bouncycastle.jce.interfaces.ECPublicKey k = (org.bouncycastle.jce.interfaces.ECPublicKey) keyPair
                .getPublic();
                keyData = k.getQ().getEncoded();
                keyHash = k.getQ().getX().toBigInteger().toByteArray();
            }
            keyData = wrapDO((byte) 0x91, keyData);
            if(keyId != -1) {
                // TODO: what this key id format should exactly be? 
                String kId = Hex.intToHexString(keyId);
                while(kId.startsWith("00")) {
                    kId = kId.substring(2);
                }
                idData = wrapDO((byte)0x84,Hex.hexStringToBytes(kId));
            }
            try {
                sendMSEKAT(wrapper, keyData, idData);
            } catch (CardServiceException cse) {
                cse.printStackTrace();
                return false;
            }
           
            // Use only the first 16 bytes?
            // byte[] s = new byte[16];
            // System.arraycopy(secret, 0, s, 0, 16);
            // secret = s;
            
            // Replace the secure messaging keys with the new generated ones:
            SecretKey ksEnc = Util.deriveKey(secret, Util.ENC_MODE);
            SecretKey ksMac = Util.deriveKey(secret, Util.MAC_MODE);
            long ssc = 0;
            wrapper = new SecureMessagingWrapper(ksEnc, ksMac, ssc);

            String sigAlg = "SHA1withRSA";

            // Now send the certificates for verification
            byte[] certRef = wrapDO((byte)0x83,caReference.getBytes());
            
            for (CVCertificate cert : terminalCertificates) {
                try {
                    sendMSEDST(wrapper, certRef);
                    byte[] body = getCertBodyData(cert);
                    byte[] sig = getCertSignatureData(cert);
                    // true means do not do chaining, send all in one APDU
                    // the actual passport may require chaining (when the certificate
                    // is too big to fit into one APDU).
                    sendPSO(wrapper, body, sig);
                    sigAlg = AlgorithmUtil.getAlgorithmName(cert.getCertificateBody().getPublicKey().getObjectIdentifier());
                    certRef = wrapDO((byte)0x83,cert.getCertificateBody().getHolderReference().getConcatenated().getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Now send get challenge + mutual authentication

            byte[] rpicc = sendGetChallenge(wrapper);
            byte[] idpic = new byte[documentNumber.length() + 1]; 
            System.arraycopy(documentNumber.getBytes(), 0, idpic, 0, documentNumber.length());
            idpic[idpic.length - 1] = (byte)MRZInfo.checkDigit(documentNumber);
            
            byte[] dtbs = new byte[idpic.length + rpicc.length + keyHash.length];
            System.arraycopy(idpic, 0, dtbs, 0, idpic.length);
            System.arraycopy(rpicc, 0, dtbs, idpic.length, rpicc.length);
            System.arraycopy(keyHash, 0, dtbs, idpic.length + rpicc.length, keyHash.length);


            Signature sig = Signature.getInstance(sigAlg);
            sig.initSign(terminalKey);
            sig.update(dtbs);
            byte[] signature = sig.sign();

            sendMSEAT(wrapper, certRef);
            sendMutualAuthenticate(wrapper, signature);

            EACEvent event = new EACEvent(this, keyId, key, keyPair, caReference,
                    terminalCertificates, terminalKey, documentNumber, rpicc, true);
            notifyEACPerformed(event);
            state = EAC_AUTHENTICATED_STATE;
            return true;
        } catch (GeneralSecurityException gse) {
            throw new CardServiceException(gse.toString());
        }
    }

    private byte[] wrapDO(byte tag, byte[] data) {
        byte[] result = new byte[data.length + 2];
        result[0] = tag;
        result[1] = (byte)data.length;
        System.arraycopy(data, 0, result, 2, data.length);
        return result;
    }
    
    private byte[] getCertBodyData(CVCertificate cert) throws IOException, NoSuchFieldException {
        return cert.getCertificateBody().getDEREncoded();
        
    }
    
    // TODO: this is a nasty hack, but unfortunately the cv-cert lib does not provide a proper API
    // call for this
    private byte[] getCertSignatureData(CVCertificate cert) throws IOException, NoSuchFieldException {
        byte[] data = cert.getDEREncoded();
        byte[] body = cert.getCertificateBody().getDEREncoded();
        int index = 0;
        byte b1 = body[0];
        byte b2 = body[1];
        while(index < data.length) {
            if(data[index] == b1 && data[index + 1] == b2) {
                break;
            }
            index ++;
        }
        index += body.length;
        if(index < data.length) {
            byte[] result = new byte[data.length - index];
            System.arraycopy(data, index, result, 0, result.length);
            // Sanity check:
            assert result[0] == (byte)0x5F && result[1] == (byte)0x37;
            return result;
        }
        return null;
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
	 * Notifies listeners about BAC events.
	 * 
	 * @param event BAC event
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
	 * @param publicKey the public key to use (usually read from the card)
	 * 
	 * @return a boolean indicating whether the card was authenticated
	 * 
	 * @throws GeneralSecurityException if something goes wrong
	 */
	public boolean doAA(PublicKey publicKey) throws CardServiceException {
		try {
			byte[] m2 = new byte[8];
			random.nextBytes(m2);
			byte[] response = sendAA(publicKey, m2);
			aaCipher.init(Cipher.DECRYPT_MODE, publicKey);
			aaSignature.initVerify(publicKey);
			int digestLength = aaDigest.getDigestLength(); /* should always be 20 */
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
	 * Performs the <i>Active Authentication</i> protocol. This method
	 * just gives the response from the card without checking. Use {@link #doAA(PublicKey)}
	 * instead.
	 * 
	 * @param publicKey the public key to use (usually read from the card)
	 * @param challenge the random challenge of exactly 8 bytes
	 * 
	 * @return response from the card
	 */
	public byte[] sendAA(PublicKey publicKey, byte[] challenge) throws CardServiceException {
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
	 * @param event AA event.
	 */
	protected void notifyAAPerformed(AAEvent event) {
		for (AuthListener l: authListeners) {
			l.performedAA(event);
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
	 * Gets the wrapper. Returns <code>null</code> until
	 * BAC has been performed.
	 * 
	 * @return the wrapper
	 */
	public SecureMessagingWrapper getWrapper() {
		return wrapper;
	}

	/**
	 * @deprecated hack
	 * @param wrapper wrapper
	 */
	public void setWrapper(SecureMessagingWrapper wrapper) {
		this.wrapper = wrapper;
		BACEvent event = new BACEvent(this, null, null, null, null, true);
		notifyBACPerformed(event);
	}

	/**
	 * Gets the file indicated by a file identifier.
	 * 
	 * @param fid ICAO file identifier
	 * 
	 * @return the file
	 * 
	 * @throws IOException if the file cannot be read
	 */
	public CardFileInputStream readFile(short fid) throws CardServiceException {
		return new CardFileInputStream(fid, maxBlockSize, fs);
	}

	private class PassportFileSystem implements FileSystemStructured
	{
		private short selectedFID;

		public synchronized short getSelectedFID() {
			return selectedFID;
		}

		public synchronized byte[] readBinary(int offset, int length) throws CardServiceException {
			return sendReadBinary(wrapper, (short)offset, length);
		}

		public synchronized void selectFile(short fid) throws CardServiceException {
			sendSelectFile(wrapper, fid);
			selectedFID = fid;
		}

		public synchronized int getFileLength() throws CardServiceException {
			try {
				/* Each passport file consists of a TLV structure. */
                /* Woj: no, not each, CVCA does not and has a fixed length */
				byte[] prefix = readBinary(0, 8);
				ByteArrayInputStream baIn = new ByteArrayInputStream(prefix);
				BERTLVInputStream tlvIn = new BERTLVInputStream(baIn);
				int tag = tlvIn.readTag();
                if(tag == CVCAFile.CAR_TAG) {
                    return CVCAFile.LENGTH;
                }
				int vLength = tlvIn.readLength();
				int tlLength = prefix.length - baIn.available();
				return tlLength + vLength;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new CardServiceException(ioe.toString());
			}
		}
	}
}
