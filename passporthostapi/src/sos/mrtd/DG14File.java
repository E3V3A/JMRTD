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

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.interfaces.DHPublicKey;

import net.sourceforge.scuba.tlv.BERTLVInputStream;
import net.sourceforge.scuba.tlv.BERTLVObject;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962NamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;

/**
 * Data Group 14 stores a set of SecurityInfos for Extended Access Control, see
 * EAC 1.11 spec. To us the interesting bits are: the map of public (EC or DH)
 * keys, the map of protocol identifiers which should match the key's map (not
 * checked here!), and the file identifier of the efCVCA file.
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class DG14File extends DataGroup {

    /** The different security infos that make up this file */
    private List<SecurityInfo> securityInfos = new ArrayList<SecurityInfo>();

    /**
     * Constructs a new DG14 file from the data in <code>in</code>.
     * 
     * @param in
     *            the input stream to parse the data from
     */
    public DG14File(InputStream in) {
        try {
            BERTLVInputStream tlvIn = new BERTLVInputStream(in);
            tlvIn.readTag();
            tlvIn.readLength();
            byte[] value = tlvIn.readValue();
            ASN1InputStream asn1in = new ASN1InputStream(value);
            DERSet set = (DERSet) asn1in.readObject();
            for (int i = 0; i < set.size(); i++) {
                DERObject o = set.getObjectAt(i).getDERObject();
                SecurityInfo si = new SecurityInfo(o);
                if (ChipAuthenticationPublicKeyInfo.checkRequiredIdentifier(si
                        .getObjectIdentifier())) {
                    si = new ChipAuthenticationPublicKeyInfo(si);
                } else if (ChipAuthenticationInfo.checkRequiredIdentifier(si
                        .getObjectIdentifier())) {
                    si = new ChipAuthenticationInfo(si);
                } else if (TerminalAuthenticationInfo
                        .checkRequiredIdentifier(si.getObjectIdentifier())) {
                    si = new TerminalAuthenticationInfo(si);
                }
                securityInfos.add(si);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Constructs a new DG14 file from the provided data:
     * 
     * @param publicKeys
     *            the map of (EC or DH) public keys indexed by key identifiers.
     *            If only one key, the index can be -1.
     * @param chipInfoMap
     *            the map of protocol identifiers for EAC indexed by key
     *            identifiers. If only one protocol identifier, the index can be
     *            -1.
     * @param cvcaFileIdList
     *            the list of file identifiers of the efCVCA file(s)
     * @param cvcaShortFileIdMap
     *            a mapping of file identifiers (see above) to short file
     *            identifiers (can be empty)
     */
    public DG14File(Map<Integer, PublicKey> publicKeys,
            Map<Integer, DERObjectIdentifier> chipInfoMap,
            List<Integer> cvcaFileIdList,
            Map<Integer, Integer> cvcaShortFileIdMap) {
        if (publicKeys.size() == 0) {
            throw new IllegalArgumentException("Need at least one key.");
        }
        if (publicKeys.size() == 1 && publicKeys.containsKey(-1)) {
            PublicKey k = publicKeys.get(-1);
            securityInfos.add(new ChipAuthenticationPublicKeyInfo(
                    getProtocolIdentifier(k), getSubjectPublicKeyInfo(k)));
        } else {
            for (int i : publicKeys.keySet()) {
                if (i < 0) {
                    throw new IllegalArgumentException("Wrong key Id: " + i);
                }
                PublicKey k = publicKeys.get(i);
                securityInfos
                        .add(new ChipAuthenticationPublicKeyInfo(
                                getProtocolIdentifier(k),
                                getSubjectPublicKeyInfo(k), i));
            }
        }
        if (chipInfoMap != null && chipInfoMap.size() > 0) {
            if (chipInfoMap.size() == 1 && chipInfoMap.containsKey(-1)) {
                securityInfos.add(new ChipAuthenticationInfo(chipInfoMap
                        .get(-1), ChipAuthenticationInfo.VERSION_NUM));
            } else {
                for (int i : chipInfoMap.keySet()) {
                    securityInfos.add(new ChipAuthenticationInfo(chipInfoMap
                            .get(i), ChipAuthenticationInfo.VERSION_NUM, i));
                }
            }
        }
        if (cvcaFileIdList == null || cvcaFileIdList.size() == 0) {
            securityInfos.add(new TerminalAuthenticationInfo(
                    EACObjectIdentifiers.id_TA,
                    TerminalAuthenticationInfo.VERSION_NUM));
        } else {
            for (Integer i : cvcaFileIdList) {
                securityInfos.add(new TerminalAuthenticationInfo(i,
                        cvcaShortFileIdMap.containsKey(i) ? cvcaShortFileIdMap
                                .get(i) : -1));
            }
        }
    }

    public byte[] getEncoded() {
        if (isSourceConsistent) {
            return sourceObject;
        }
        try {
            ASN1EncodableVector v = new ASN1EncodableVector();
            for (SecurityInfo si : securityInfos) {
                v.add(si.getDERObject());
            }
            DERSet s = new DERSet(v);
            BERTLVObject secInfos = new BERTLVObject(PassportFile.EF_DG14_TAG,
                    s.getDEREncoded());
            sourceObject = secInfos.getEncoded();
            isSourceConsistent = true;
            return sourceObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the list of file identifier references to efCVCA files, possibly
     * empty.
     * 
     * @return the list of file identifier
     */
    public List<Integer> getCVCAFileIds() {
        List<Integer> cvcaFiles = new ArrayList<Integer>();
        for (SecurityInfo si : securityInfos) {
            if (si instanceof TerminalAuthenticationInfo) {
                int i = ((TerminalAuthenticationInfo) si).getFileID();
                if (i != -1) {
                    cvcaFiles.add(i);
                }
            }
        }
        return cvcaFiles;
    }

    /**
     * Returns a corresponding short file ID.
     * 
     * @param fileId
     *            the file ID
     * @return an SFI for the given file ID, -1 if not present
     */
    public byte getCVCAShortFileId(int fileId) {
        for (SecurityInfo si : securityInfos) {
            if (si instanceof TerminalAuthenticationInfo) {
                if (((TerminalAuthenticationInfo) si).getFileID() == fileId) {
                    return ((TerminalAuthenticationInfo) si).getShortFileID();
                }
            }
        }
        return -1;
    }

    /**
     * Returns the mapping of key identifiers to EAC protocol identifiers
     * contained in this file. The key identifier may be -1 if there is only one
     * protocol identifier.
     * 
     * @return the mapping of key identifiers to EAC protocol identifiers
     */
    public Map<Integer, DERObjectIdentifier> getChipAuthenticationInfos() {
        Map<Integer, DERObjectIdentifier> map = new TreeMap<Integer, DERObjectIdentifier>();
        for (SecurityInfo si : securityInfos) {
            if (si instanceof ChipAuthenticationInfo) {
                ChipAuthenticationInfo i = (ChipAuthenticationInfo) si;
                map.put(i.getKeyId(), i.getObjectIdentifier());
                if (i.getKeyId() == -1) {
                    return map;
                }
            }
        }
        return map;
    }

    /**
     * Returns the mapping of key identifiers to public keys. The key identifier
     * may be -1 if there is only one key.
     * 
     * @return the mapping of key identifiers to public keys
     */
    public Map<Integer, PublicKey> getPublicKeys() {
        List<ChipAuthenticationPublicKeyInfo> l = new ArrayList<ChipAuthenticationPublicKeyInfo>();
        Map<Integer, PublicKey> r = new TreeMap<Integer, PublicKey>();
        for (SecurityInfo si : securityInfos) {
            if (si instanceof ChipAuthenticationPublicKeyInfo) {
                l.add((ChipAuthenticationPublicKeyInfo) si);
            }
        }
        if (l.size() == 0) {
            throw new IllegalStateException("No keys?");
        }
        for (ChipAuthenticationPublicKeyInfo info : l) {
            r
                    .put(info.getKeyId(), getPublicKey(info
                            .getSubjectPublicKeyInfo()));
        }
        return r;
    }

    private static PublicKey getPublicKey(SubjectPublicKeyInfo info) {
        try {
            KeySpec spec = new X509EncodedKeySpec(info.getDEREncoded());
            // TODO: Why does this "DH" work for both EC & DH, and "EC" does
            // not?
            KeyFactory kf = KeyFactory.getInstance("DH");
            return kf.generatePublic(spec);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException("Could not decode key.");
        }
    }

    private static DERObjectIdentifier getProtocolIdentifier(PublicKey publicKey) {
        if (publicKey instanceof ECPublicKey) {
            return EACObjectIdentifiers.id_PK_ECDH;
        } else if (publicKey instanceof DHPublicKey) {
            return EACObjectIdentifiers.id_PK_DH;
        } else {
            throw new IllegalArgumentException("Wrong key type.");
        }
    }

    private static SubjectPublicKeyInfo getSubjectPublicKeyInfo(
            PublicKey publicKey) {
        // Here we need to some hocus-pokus, the EAC specification require for
        // all the
        // key information to include the domain parameters explicitly. This is
        // not what
        // Bouncy Castle does by default. But we first have to check if this is
        // the case.
        try {
            if (publicKey instanceof ECPublicKey) {
                SubjectPublicKeyInfo vInfo = new SubjectPublicKeyInfo(
                        (DERSequence) new ASN1InputStream(publicKey
                                .getEncoded()).readObject());
                DERObject parameters = vInfo.getAlgorithmId().getParameters()
                        .getDERObject();
                X9ECParameters params = null;
                if (parameters instanceof DERObjectIdentifier) {
                    params = X962NamedCurves
                            .getByOID((DERObjectIdentifier) parameters);
                    org.bouncycastle.math.ec.ECPoint p = params.getG();
                    p = p.getCurve().createPoint(p.getX().toBigInteger(),
                            p.getY().toBigInteger(), false);
                    params = new X9ECParameters(params.getCurve(), p, params
                            .getN(), params.getH(), params.getSeed());
                } else {
                    return vInfo;

                }

                org.bouncycastle.jce.interfaces.ECPublicKey pub = (org.bouncycastle.jce.interfaces.ECPublicKey) publicKey;
                AlgorithmIdentifier id = new AlgorithmIdentifier(vInfo
                        .getAlgorithmId().getObjectId(), params.getDERObject());
                org.bouncycastle.math.ec.ECPoint p = pub.getQ();
                // In case we would like to compress the point:
                // p = p.getCurve().createPoint(p.getX().toBigInteger(),
                // p.getY().toBigInteger(), true);
                vInfo = new SubjectPublicKeyInfo(id, p.getEncoded());
                return vInfo;

            } else if (publicKey instanceof DHPublicKey) {
                return new SubjectPublicKeyInfo(
                        (DERSequence) new ASN1InputStream(publicKey
                                .getEncoded()).readObject());
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized key type, should be DH or EC");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
