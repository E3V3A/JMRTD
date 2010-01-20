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

package org.jmrtd.lds;

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;

/**
 * A specialised SecurityInfo structure that stores chip authentication info,
 * see EAC 1.11 specification.
 * 
 * This data structure provides detailed information on an implementation of
 * Chip Authentication.
 * <ul>
 * <li>The object identifier <code>protocol</code> SHALL identify the
 *     algorithms to be used (i.e. key agreement, symmetric cipher and MAC).</li>
 * <li>The integer <code>version</code> SHALL identify the version of the protocol.
 *     Currently, versions 1 and 2 are supported.</li>
 * <li>The integer <code>keyId</code> MAY be used to indicate the local key identifier.
 *     It MUST be used if the MRTD chip provides multiple public keys for Chip
 *     Authentication.</li>
 * </ul>
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 * FIXME: dependency on BC?
 */
public class ChipAuthenticationInfo extends SecurityInfo {

    public static final int VERSION_NUM = 1;

    public ChipAuthenticationInfo(SecurityInfo si) {
        super(si);
    }

    public ChipAuthenticationInfo(InputStream in) throws IOException {
        super(in);
    }

    /**
     * Constructs a new object
     * 
     * @param identifier
     *            a proper EAC identifier
     * @param version
     *            has to be one
     * @param keyId
     *            the key identifier
     */
    public ChipAuthenticationInfo(DERObjectIdentifier identifier,
            Integer version, Integer keyId) {
        super(identifier, new DERInteger(version),
                keyId != null ? new DERInteger(keyId) : null);
    }

    /**
     * Constructs a new object
     * 
     * @param identifier
     *            a proper EAC identifier
     * @param version
     *            has to be one
     */
    public ChipAuthenticationInfo(DERObjectIdentifier identifier,
            Integer version) {
        this(identifier, version, null);
    }

    /**
     * Returns a key identifier stored in this ChipAuthenticationInfo structure, null
     * if not present
     * 
     * @return key identifier stored in this ChipAuthenticationInfo structure
     */
    public Integer getKeyId() {
        if (optionalData == null) {
            return -1;
        }
        return ((DERInteger) optionalData).getValue().intValue();
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
            if (optionalData != null && !(optionalData instanceof DERInteger)) {
                throw new IllegalArgumentException("Key ID not an integer: "
                        + optionalData.getClass().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(
                    "Malformed ChipAuthenticationInfo.");
        }
    }

    /**
     * Checks whether the given object identifier identfies a
     * ChipAuthenticationInfo structure.
     * 
     * @param id
     *            object identifier
     * @return true if the match is positive
     */
    public static boolean checkRequiredIdentifier(DERObjectIdentifier id) {
        return id.equals(EACObjectIdentifiers.id_CA_DH_3DES_CBC_CBC)
                || id.equals(EACObjectIdentifiers.id_CA_ECDH_3DES_CBC_CBC);
    }
}
