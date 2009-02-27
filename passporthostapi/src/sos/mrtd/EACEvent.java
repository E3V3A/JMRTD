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
 */

package sos.mrtd;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.ejbca.cvc.CVCertificate;

/**
 * Event to indicate EAC protocol was executed.
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class EACEvent extends EventObject {

    private PassportService service;

    private int keyId;

    private PublicKey cardKey;

    private KeyPair keyPair;

    private String caReference;

    private List<CVCertificate> terminalCertificates = new ArrayList<CVCertificate>();

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
    public EACEvent(PassportService service, int keyId, PublicKey cardKey,
            KeyPair keyPair, String caReference,
            List<CVCertificate> terminalCertificates, PrivateKey terminalKey,
            String documentNumber, byte[] cardChallenge, boolean success) {
        super(service);
        this.service = service;
        this.keyId = keyId;
        this.cardKey = cardKey;
        this.keyPair = keyPair;
        this.success = success;
        this.caReference = caReference;
        for (CVCertificate c : terminalCertificates) {
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
     * Returns the host key pair used for EAC chip authentication.
     * 
     * @return the host key pair used for EAC chip authentication
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * Returns CA certificate's reference used during EAC.
     * 
     * @return CA certificate's reference
     */
    public String getCAReference() {
        return caReference;
    }

    /**
     * Returns the chain of CVCertificates used to authenticate the terminal to
     * the card.
     * 
     * @return the chain of CVCertificates used to authenticate the terminal to
     *         the card
     */
    public List<CVCertificate> getCVCertificates() {
        return terminalCertificates;
    }

    /**
     * Returns the terminal private key used during EAC.
     * 
     * @return the terminal private key
     */
    public PrivateKey getTerminalKey() {
        return terminalKey;
    }

    /**
     * Returns the id of the card used during EAC.
     * 
     * @return the id of the card
     */
    public String getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Return the card's challenge generated during EAC.
     * 
     * @return the card's challenge
     */
    public byte[] getCardChallenge() {
        return cardChallenge;
    }

    /**
     * Return the card's public key used during EAC.
     * 
     * @return the card's public key
     */
    public PublicKey getCardPublicKey() {
        return cardKey;
    }

    /**
     * Return the card's public key ID used during EAC.
     * 
     * @return the card's public key ID
     */

    public int getCardPublicKeyId() {
        return keyId;
    }

    public PassportService getService() {
        return service;
    }

}
