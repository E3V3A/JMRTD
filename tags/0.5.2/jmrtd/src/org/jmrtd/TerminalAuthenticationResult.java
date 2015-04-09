/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2015  The JMRTD team
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

package org.jmrtd;

import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;

/**
 * Result of EAC protocols.
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision$
 */
public class TerminalAuthenticationResult  {

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");
	
	private ChipAuthenticationResult chipAuthenticationResult;
	private CVCPrincipal caReference;
	private List<CardVerifiableCertificate> terminalCertificates = new ArrayList<CardVerifiableCertificate>();
	private PrivateKey terminalKey;
	private String documentNumber;
	private byte[] cardChallenge;

	/**
	 * Constructs a new event.
	 * 
	 * @param chipAuthenticationResult the chip authentication result
	 * @param caReference the CA
	 * @param terminalCertificates terminal certificates
	 * @param terminalKey the terminal's private key
	 * @param documentNumber the documentNumber
	 * @param cardChallenge the challenge
	 */
	public TerminalAuthenticationResult(ChipAuthenticationResult chipAuthenticationResult, CVCPrincipal caReference,
			List<CardVerifiableCertificate> terminalCertificates, PrivateKey terminalKey,
			String documentNumber, byte[] cardChallenge) {
		this.chipAuthenticationResult = chipAuthenticationResult;
		this.caReference = caReference;
		for (CardVerifiableCertificate c : terminalCertificates) {
			this.terminalCertificates.add(c);
		}
		this.terminalKey = terminalKey;
		this.documentNumber = documentNumber;
		this.cardChallenge = cardChallenge;
	}

	/**
	 * Gets the chip authentication result;
	 * 
	 * @return the chip authenticaiton result
	 */
	public ChipAuthenticationResult getChipAuthenticationResult() {
		return chipAuthenticationResult;
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

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("EACEvent [chipAuthenticationResult = " + chipAuthenticationResult + ", ");
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
				LOGGER.severe("Exception: " + ce.getMessage());
			}

		}
		//        result.append("terminalCertificates = " + terminalCertificates + ", ");
		//        result.append("terminalKey = " + terminalKey + ", ");
		//        result.append("documentNumber = " + documentNumber + ", ");
		//        result.append("cardChallenge = " + cardChallenge + ", ");
		result.append("]");
		return result.toString();
	}
}
