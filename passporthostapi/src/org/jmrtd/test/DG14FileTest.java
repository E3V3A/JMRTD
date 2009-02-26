package org.jmrtd.test;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.util.Map;
import java.util.TreeMap;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sos.mrtd.DG14File;
import junit.framework.TestCase;

public class DG14FileTest extends TestCase {

    public void testReflexive1() throws NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance("EC");
        keyGen1.initialize(192);
        KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance("DH");

        Map<Integer,PublicKey> keys = new TreeMap<Integer,PublicKey>();
        keys.put(1, keyGen1.generateKeyPair().getPublic());
        keys.put(2, keyGen2.generateKeyPair().getPublic());
        Map<Integer,DERObjectIdentifier> algs = new TreeMap<Integer, DERObjectIdentifier>();
        algs.put(1, EACObjectIdentifiers.id_CA_DH_3DES_CBC_CBC);
        algs.put(2, EACObjectIdentifiers.id_CA_ECDH_3DES_CBC_CBC);
        DG14File f = new DG14File(keys, algs, null, null);
        assertEquals(keys, f.getPublicKeys());
        assertEquals(algs, f.getChipAuthenticationInfos());
    }
}
