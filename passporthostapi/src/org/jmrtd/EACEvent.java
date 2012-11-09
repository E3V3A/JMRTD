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

package org.jmrtd;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;

/**
 * Event to indicate EAC protocol was executed.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * 
 * @version $Revision: $
 */
public class EACEvent extends EventObject {

	private static final long serialVersionUID = 6992383777555486463L;

	private PassportService service;
	private BigInteger keyId;
	private PublicKey cardKey;
	private KeyPair keyPair;
	private CVCPrincipal caReference;
	private List<CardVerifiableCertificate> terminalCertificates = new ArrayList<CardVerifiableCertificate>();
	private PrivateKey terminalKey;
	private boolean success;
	private String documentNumber;
	private byte[] cardChallenge;

	/**
	 * Constructs a new event.
	 * 
	 * @param service
	 *            event source
	 * @param keyPair
	 *            the ECDH key pair used for authenticating the chip
	 * @param success
	 *            status of protocol
	 */
	public EACEvent(PassportService service, BigInteger keyId, PublicKey cardKey,
			KeyPair keyPair, CVCPrincipal caReference,
			List<CardVerifiableCertificate> terminalCertificates, PrivateKey terminalKey,
			String documentNumber, byte[] cardChallenge, boolean success) {
		super(service);
		this.service = service;
		this.keyId = keyId;
		this.cardKey = cardKey;
		this.keyPair = keyPair;
		this.success = success;
		this.caReference = caReference;
		for (CardVerifiableCertificate c : terminalCertificates) {
			this.terminalCertificates.add(c);
		}
		this.terminalKey = terminalKey;
		this.documentNumber = documentNumber;
		this.cardChallenge = cardChallenge;
	}

	/**
	 * Gets the resulting wrapper.
	 * 
	 * @return the resulting wrapper
	 */
	public SecureMessagingWrapper getWrapper() {
		return service.getWrapper();
	}

	/**
	 * Gets the status of the executed EAC protocol run.
	 * 
	 * @return status of the EAC protocol run.
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Gets the host key pair used for EAC chip authentication.
	 * 
	 * @return the host key pair used for EAC chip authentication
	 */
	public KeyPair getKeyPair() {
		return keyPair;
	}

	/**
	 * Gets CA certificate's reference used during EAC.
	 * 
	 * @return CA certificate's reference
	 */
	public CVCPrincipal getCAReference() {
		return caReference;
	}

	/**
	 * Gets the chain of CVCertificates used to authenticate the terminal to
	 * the card.
	 * 
	 * @return the chain of CVCertificates used to authenticate the terminal to
	 *         the card
	 */
	public List<CardVerifiableCertificate> getCVCertificates() {
		return terminalCertificates;
	}

	/**
	 * Gets the terminal private key used during EAC.
	 * 
	 * @return the terminal private key
	 */
	public PrivateKey getTerminalKey() {
		return terminalKey;
	}

	/**
	 * Gets the id of the card used during EAC.
	 * 
	 * @return the id of the card
	 */
	public String getDocumentNumber() {
		return documentNumber;
	}

	/**
	 * Gets the card's challenge generated during EAC.
	 * 
	 * @return the card's challenge
	 */
	public byte[] getCardChallenge() {
		return cardChallenge;
	}

	/**
	 * Gets the card's public key used during EAC.
	 * 
	 * @return the card's public key
	 */
	public PublicKey getCardPublicKey() {
		return cardKey;
	}

	/**
	 * Gets the card's public key ID used during EAC.
	 * 
	 * @return the card's public key ID
	 */
	public BigInteger getCardPublicKeyId() {
		return keyId;
	}

	/**
	 * Gets the service.
	 * 
	 * @return a service
	 */
	public PassportService getService() {
		return service;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("EACEvent [keyID = " + keyId + ", ");
		//    	result.append("cardKey = " + cardKey + ", ");
		//    	result.append("keyPair = " + keyPair + ", ");
		result.append("caReference = " + caReference + ", ");
		for (CardVerifiableCertificate cert: terminalCertificates) {
			try {
				CVCPrincipal reference = cert.getHolderReference();
				if (!caReference.equals(reference)) {
					result.append("holderReference = " + reference + ", ");
				}
			} catch (CertificateException ce) {
				result.append("holderReference = ???, ");
				ce.printStackTrace();
			}

		}
		//        result.append("terminalCertificates = " + terminalCertificates + ", ");
		//        result.append("terminalKey = " + terminalKey + ", ");
		//        result.append("documentNumber = " + documentNumber + ", ");
		//        result.append("cardChallenge = " + cardChallenge + ", ");
		result.append("success = " + success + "]");
		return result.toString();
	}
}
