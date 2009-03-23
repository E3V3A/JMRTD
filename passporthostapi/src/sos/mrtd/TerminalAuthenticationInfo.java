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

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;

/**
 * A specialised SecurityInfo structure that stores terminal authentication
 * info, see EAC 1.11 specification.
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class TerminalAuthenticationInfo extends SecurityInfo {

    public static final int VERSION_NUM = 1;

    public TerminalAuthenticationInfo(SecurityInfo si) {
        super(si);
    }

    public TerminalAuthenticationInfo(InputStream in) throws IOException {
        super(in);
    }

    /**
     * Constructs a new object.
     * 
     * @param identifier
     *            the id_TA identifier
     * @param version
     *            has to be 1
     * @param efCVCA
     *            the file ID information of the efCVCA file
     */
    public TerminalAuthenticationInfo(DERObjectIdentifier identifier,
            Integer version, DERSequence efCVCA) {
        super(identifier, new DERInteger(version), efCVCA);
    }

    /**
     * Constructs a new object.
     * 
     * @param identifier
     *            the id_TA identifier
     * @param version
     *            has to be 1
     */
    public TerminalAuthenticationInfo(DERObjectIdentifier identifier,
            Integer version) {
        this(identifier, version, null);
    }

    /**
     * Constructs a new object with the required object identifier and version
     * number and:
     * 
     * @param fileId
     *            a file identifier reference to the efCVCA file
     * @param shortFileId
     *            short file id for the above file, -1 if none
     */
    public TerminalAuthenticationInfo(Integer fileId, Integer shortFileId) {
        this(
                EACObjectIdentifiers.id_TA,
                VERSION_NUM,
                shortFileId.byteValue() != -1 ? new DERSequence(
                        new ASN1Encodable[] {
                                new DEROctetString(Hex.hexStringToBytes(Hex
                                        .shortToHexString(fileId.shortValue()))),
                                new DEROctetString(Hex.hexStringToBytes(Hex
                                        .byteToHexString(shortFileId
                                                .byteValue()))) })
                        : new DERSequence(
                                new ASN1Encodable[] { new DEROctetString(Hex
                                        .hexStringToBytes(Hex
                                                .shortToHexString(fileId
                                                        .shortValue()))) }));
    }

    /**
     * Checks the correctness of the data for this instance of SecurityInfo
     */
    protected void checkFields() {
        try {
            if (!checkRequiredIdentifier(identifier)) {
                throw new IllegalArgumentException("Wrong identifier: "
                        + identifier.getId());
            }
            if (!(requiredData instanceof DERInteger)
                    || ((DERInteger) requiredData).getValue().intValue() != VERSION_NUM) {
                throw new IllegalArgumentException("Wrong version");
            }
            if (optionalData != null) {
                DERSequence s = (DERSequence) optionalData;
                DEROctetString fid = (DEROctetString) s.getObjectAt(0);
                if (fid.getOctets().length != 2) {
                    throw new IllegalArgumentException("Malformed FID.");
                }
                if (s.size() == 2) {
                    DEROctetString sfi = (DEROctetString) s.getObjectAt(1);
                    if (sfi.getOctets().length != 1) {
                        throw new IllegalArgumentException("Malformed SFI.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(
                    "Malformed TerminalAuthenticationInfo.");
        }
    }

    /**
     * Checks whether the given object identifier identfies a
     * TerminalAuthenticationInfo structure.
     * 
     * @param id
     *            object identifier
     * @return true if the match is positive
     */
    public static boolean checkRequiredIdentifier(DERObjectIdentifier id) {
        return id.equals(EACObjectIdentifiers.id_TA);
    }

    /**
     * Returns the efCVCA file identifier stored in this file, -1 if none
     * 
     * @return the efCVCA file identifier stored in this file
     */
    public int getFileID() {
        if (optionalData == null) {
            return -1;
        }
        DERSequence s = (DERSequence) optionalData;
        DEROctetString fid = (DEROctetString) s.getObjectAt(0);
        byte[] fidBytes = fid.getOctets();
        return Hex.hexStringToInt(Hex.bytesToHexString(fidBytes));
    }

    /**
     * Returns the efCVCA short file identifier stored in this file, -1 if none
     * or not present
     * 
     * @return the efCVCA short file identifier stored in this file
     */
    public byte getShortFileID() {
        if (optionalData == null) {
            return -1;
        }
        DERSequence s = (DERSequence) optionalData;
        if (s.size() != 2) {
            return -1;
        }
        return ((DEROctetString) s.getObjectAt(1)).getOctets()[0];
    }

}
