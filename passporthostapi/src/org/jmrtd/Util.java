/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECField;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

import net.sf.scuba.tlv.TLVOutputStream;
import net.sf.scuba.util.Hex;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.DHParameter;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962NamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SecurityInfo;

/**
 * Some static helper functions. Mostly dealing with low-level crypto.
 * 
 * @deprecated The visibility of this class will be changed to package.
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * @author Engelbert Hubbers (hubbers@cs.ru.nl)
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @author Ronny Wichers Schreur (ronny@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class Util {

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	/** Mode for KDF. */
	public static final int
	ENC_MODE = 1,
	MAC_MODE = 2,
	PACE_MODE = 3;

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	private Util() {
	}

	/**
	 * Derives the ENC or MAC key for BAC from the keySeed.
	 *
	 * @param keySeed the key seed.
	 * @param mode either <code>ENC_MODE</code> or <code>MAC_MODE</code>.
	 * 
	 * @return the key.
	 */
	public static SecretKey deriveKey(byte[] keySeed, int mode) throws GeneralSecurityException {
		return deriveKey(keySeed, "DESede", 128, mode);
	}

	public static SecretKey deriveKey(byte[] keySeed, String cipherAlgName, int keyLength, int mode) throws GeneralSecurityException {	
		return deriveKey(keySeed, cipherAlgName, keyLength, null, mode);
	}

	/**
	 * Derives a shared key.
	 * 
	 * @param keySeed the shared secret, as octets
	 * @param digestAlg in Java mnemonic notation (for example "SHA-1", "SHA-256")
	 * @param cipherAlg in Java mnemonic notation (for example "DESede", "AES")
	 * @param keyLength length in bits
	 * @param nonce optional nonce or <code>null</code>
	 * @param counter counter or mode
	 *
	 * @return the derived key
	 *
	 * @throws GeneralSecurityException if something went wrong
	 */
	public static SecretKey deriveKey(byte[] keySeed, String cipherAlg, int keyLength, byte[] nonce, int counter) throws GeneralSecurityException {
		String digestAlg = inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation(cipherAlg, keyLength);
		LOGGER.info("DEBUG: key derivation uses digestAlg = " + digestAlg);
		MessageDigest digest = MessageDigest.getInstance(digestAlg);
		digest.reset();
		digest.update(keySeed);
		if (nonce != null) {
			digest.update(nonce);
		}
		digest.update(new byte[] { 0x00, 0x00, 0x00, (byte)counter });
		byte[] hashResult = digest.digest();
		LOGGER.info("DEBUG: hashResult.length = " + hashResult.length);
		byte[] keyBytes = null;
		if ("DESede".equalsIgnoreCase(cipherAlg) || "3DES".equalsIgnoreCase(cipherAlg)) {
			/* TR-SAC 1.01, 4.2.1. */
			switch(keyLength) {
			case 112:
			case 128:
				keyBytes = new byte[24];
				System.arraycopy(hashResult, 0, keyBytes, 0, 8); /* E  (octets 1 to 8) */
				System.arraycopy(hashResult, 8, keyBytes, 8, 8); /* D  (octets 9 to 16) */
				System.arraycopy(hashResult, 0, keyBytes, 16, 8); /* E (again octets 1 to 8, i.e. 112-bit 3DES key) */
				break;
			default:
				throw new IllegalArgumentException("KDF can only use DESede with 128-bit key length");
			}
		} else if ("AES".equalsIgnoreCase(cipherAlg) || cipherAlg.startsWith("AES")) {
			LOGGER.info("DEBUG: key derivation with AES uses key length " + keyLength);
			/* TR-SAC 1.01, 4.2.2. */
			switch(keyLength) {
			case 128:
				keyBytes = new byte[16]; /* NOTE: 128 = 16 * 8 */
				System.arraycopy(hashResult, 0, keyBytes, 0, 16);
				break;
			case 192:
				keyBytes = new byte[24]; /* NOTE: 192 = 24 * 8 */
				System.arraycopy(hashResult, 0, keyBytes, 0, 24);
				break;
			case 256:
				keyBytes = new byte[32]; /* NOTE: 256 = 32 * 8 */
				System.arraycopy(hashResult, 0, keyBytes, 0, 32);
				break;
			default:
				throw new IllegalArgumentException("KDF can only use AES with 128-bit, 192-bit key or 256-bit length, found: " + keyLength + "-bit key length");
			}
		}
		//		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(cipherAlgName);
		//		return keyFactory.generateSecret(new SecretKeySpec(keyBytes, cipherAlgName));
		return new SecretKeySpec(keyBytes, cipherAlg);
	}

	/**
	 * Computes the static key seed, based on information from the MRZ.
	 *
	 * @param documentNumber a string containing the document number.
	 * @param dateOfBirth a string containing the date of birth (YYMMDD).
	 * @param dateOfExpiry a string containing the date of expiry (YYMMDD).
	 *
	 * @return a byte array of length 16 containing the key seed.
	 */
	/*
	 * NOTE: since 0.4.9, this method no longer checks input validity. Client is responsible now.
	 */
	public static byte[] computeKeySeed(String documentNumber, String dateOfBirth, String dateOfExpiry) throws GeneralSecurityException {

		/* Check digits... */
		byte[] cd1 = { (byte)MRZInfo.checkDigit(documentNumber) };
		byte[] cd2 = { (byte)MRZInfo.checkDigit(dateOfBirth) };
		byte[] cd3 = { (byte)MRZInfo.checkDigit(dateOfExpiry) };

		MessageDigest shaDigest = MessageDigest.getInstance("SHA1");

		shaDigest.update(getBytes(documentNumber));
		shaDigest.update(cd1);
		shaDigest.update(getBytes(dateOfBirth));
		shaDigest.update(cd2);
		shaDigest.update(getBytes(dateOfExpiry));
		shaDigest.update(cd3);

		byte[] hash = shaDigest.digest();
		byte[] keySeed = new byte[16];
		System.arraycopy(hash, 0, keySeed, 0, 16);
		return keySeed;
	}

	public static long computeSendSequenceCounter(byte[] rndICC, byte[] rndIFD) {
		if (rndICC == null || rndICC.length != 8
				|| rndIFD == null || rndIFD.length != 8) {
			throw new IllegalStateException("Wrong length input");
		}
		long ssc = 0;
		for (int i = 4; i < 8; i++) {
			ssc <<= 8;
			ssc += (long)(rndICC[i] & 0x000000FF);
		}
		for (int i = 4; i < 8; i++) {
			ssc <<= 8;
			ssc += (long)(rndIFD[i] & 0x000000FF);
		}
		return ssc;
	}

	/**
	 * Pads the input <code>in</code> according to ISO9797-1 padding method 2.
	 *
	 * @param in input
	 *
	 * @return padded output
	 */
	public static byte[] pad(/*@ non_null */ byte[] in) {
		return pad(in, 0, in.length);
	}

	/*@ requires 0 <= offset && offset < length;
	  @ requires 0 <= length && length <= in.length;
	 */
	public static byte[] pad(/*@ non_null */ byte[] in, int offset, int length) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(in, offset, length);
		out.write((byte)0x80);
		while (out.size() % 8 != 0) {
			out.write((byte)0x00);
		}
		return out.toByteArray();
	}

	public static byte[] unpad(byte[] in) {
		int i = in.length - 1;
		while (i >= 0 && in[i] == 0x00) {
			i--;
		}
		if ((in[i] & 0xFF) != 0x80) {
			throw new IllegalStateException("unpad expected constant 0x80, found 0x" + Integer.toHexString((in[i] & 0x000000FF)) + "\nDEBUG: in = " + Hex.bytesToHexString(in) + ", index = " + i);
		}
		byte[] out = new byte[i];
		System.arraycopy(in, 0, out, 0, i);
		return out;
	}

	/**
	 * Recovers the M1 part of the message sent back by the AA protocol
	 * (INTERNAL AUTHENTICATE command). The algorithm is described in
	 * ISO 9796-2:2002 9.3.
	 * 
	 * Based on code by Ronny (ronny@cs.ru.nl) who presumably ripped this
	 * from Bouncy Castle. 
	 * 
	 * @param digestLength should be 20
	 * @param plaintext response from card, already 'decrypted' (using the
	 * AA public key)
	 * 
	 * @return the m1 part of the message
	 */
	public static byte[] recoverMessage(int digestLength, byte[] plaintext) {
		if (plaintext == null || plaintext.length < 1) {
			throw new IllegalArgumentException("Plaintext too short to recover message");
		}
		if (((plaintext[0] & 0xC0) ^ 0x40) != 0) {
			// 0xC0 = 1100 0000, 0x40 = 0100 0000
			throw new NumberFormatException("Could not get M1");
		}
		if (((plaintext[plaintext.length - 1] & 0xF) ^ 0xC) != 0) {
			// 0xF = 0000 1111, 0xC = 0000 1100
			throw new NumberFormatException("Could not get M1");
		}
		int delta = 0;
		if (((plaintext[plaintext.length - 1] & 0xFF) ^ 0xBC) == 0) {
			delta = 1;
		} else {
			// 0xBC = 1011 1100
			throw new NumberFormatException("Could not get M1");
		}

		/* find out how much padding we've got */
		int paddingLength = 0;
		for (; paddingLength < plaintext.length; paddingLength++) {
			// 0x0A = 0000 1010
			if (((plaintext[paddingLength] & 0x0F) ^ 0x0A) == 0) {
				break;
			}
		}
		int messageOffset = paddingLength + 1;

		int paddedMessageLength = plaintext.length - delta - digestLength;
		int messageLength = paddedMessageLength - messageOffset;

		/* there must be at least one byte of message string */
		if (messageLength <= 0) {
			throw new NumberFormatException("Could not get M1");
		}

		/* TODO: if we contain the whole message as well, check the hash of that. */
		if ((plaintext[0] & 0x20) == 0) {
			throw new NumberFormatException("Could not get M1");
		} else {
			byte[] recoveredMessage = new byte[messageLength];
			System.arraycopy(plaintext, messageOffset, recoveredMessage, 0, messageLength);
			return recoveredMessage;
		}
	}

	/**
	 * Converts an integer to an octet string.
	 * Based on BSI TR 03111 Section 3.1.2.
	 *
	 * @param val positive integer
	 * @param length length
	 * 
	 * @return octet string
	 */
	public static byte[] i2os(BigInteger val, int length) {
		BigInteger base = BigInteger.valueOf(256);
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {	
			BigInteger remainder = val.mod(base);
			val = val.divide(base);
			result[length - 1 - i] = (byte)remainder.intValue();
		}
		return result;
	}

	/**
	 * Converts an integer to an octet string.
	 * 
	 * @param val positive integer
	 * @return octet string
	 */
	public static byte[] i2os(BigInteger val) {
		/* FIXME: Quick hack. What if val < 0? -- MO */
		/* Do something with: int sizeInBytes = val.bitLength() / Byte.SIZE; */

		int sizeInNibbles = val.toString(16).length();
		if (sizeInNibbles % 2 != 0) { sizeInNibbles++; }
		return i2os(val, sizeInNibbles / 2);
	}

	/**
	 * Converts an octet string to an integer.
	 * Based on BSI TR 03111 Section 3.1.2.
	 *
	 * @param bytes octet string
	 * 
	 * @return positive integer
	 */
	public static BigInteger os2i(byte[] bytes) {
		if (bytes == null) { throw new IllegalArgumentException(); }
		return os2i(bytes, 0, bytes.length);
	}

	/**
	 * Converts an octet string to an integer.
	 * Based on BSI TR 03111 Section 3.1.2.
	 *
	 * @param bytes octet string
	 * @param offset offset of octet string
	 * @param length length of octet string
	 * 
	 * @return positive integer
	 */
	public static BigInteger os2i(byte[] bytes, int offset, int length) {
		if (bytes == null) { throw new IllegalArgumentException(); }
		BigInteger result = BigInteger.ZERO;
		BigInteger base = BigInteger.valueOf(256);
		for (int i = offset; i < offset + length; i++) {
			result = result.multiply(base);
			result = result.add(BigInteger.valueOf(bytes[i] & 0xFF));
		}
		return result;
	}

	/**
	 * Convert an octet string to field element via OS2FE as specified in BSI TR-03111.
	 *
	 * @param bytes octet string
	 * @return positive integer
	 */
	public static BigInteger os2fe(byte[] bytes, BigInteger p) {
		return Util.os2i(bytes).mod(p);
	}

	/**
	 * Encode an EC public key point.
	 * Prefixes a <code>0x04</code> (without a length).
	 * 
	 * @param point public key point
	 * @return
	 */
	public static byte[] publicKeyECPointToOS(ECPoint point) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		BigInteger x = point.getAffineX();
		BigInteger y = point.getAffineY();
		try {
			bOut.write(0x04);
			bOut.write(i2os(x));
			bOut.write(i2os(y));
			bOut.close();
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe.getMessage());
		}
		return bOut.toByteArray();
	}

	/* Best effort. FIXME: test and improve. -- MO */
	/**
	 * Infers a digest algorithm mnemonic from a signature algorithm mnemonic.
	 * 
	 * @param signatureAlgorithm a signature algorithm
	 * @return a digest algorithm, or null if inference failed
	 */
	public static String inferDigestAlgorithmFromSignatureAlgorithm(String signatureAlgorithm) {
		if (signatureAlgorithm == null) { throw new IllegalArgumentException(); }
		String digestAlgorithm = null;
		String signatureAlgorithmToUppercase = signatureAlgorithm.toUpperCase();
		if (signatureAlgorithmToUppercase.contains("WITH")) {
			String[] components = signatureAlgorithmToUppercase.split("WITH");
			digestAlgorithm = components[0];
		}
		if ("SHA1".equalsIgnoreCase(digestAlgorithm)) { digestAlgorithm = "SHA-1"; }
		if ("SHA224".equalsIgnoreCase(digestAlgorithm)) { digestAlgorithm = "SHA-224"; }
		if ("SHA256".equalsIgnoreCase(digestAlgorithm)) { digestAlgorithm = "SHA-256"; }
		if ("SHA384".equalsIgnoreCase(digestAlgorithm)) { digestAlgorithm = "SHA-384"; }
		if ("SHA512".equalsIgnoreCase(digestAlgorithm)) { digestAlgorithm = "SHA-512"; }
		return digestAlgorithm;
	}

	public static String inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation(String cipherAlg, int keyLength) {
		if (cipherAlg == null) { throw new IllegalArgumentException(); }
		if ("DESede".equals(cipherAlg) || "AES-128".equals(cipherAlg)) { return "SHA-1"; }
		if ("AES".equals(cipherAlg) && keyLength == 128) { return "SHA-1"; }
		if ("AES-256".equals(cipherAlg) || "AES-192".equals(cipherAlg)) { return "SHA-256"; }
		if ("AES".equals(cipherAlg) && (keyLength == 192 || keyLength == 256)) { return "SHA-256"; }
		throw new IllegalArgumentException("Unsupported cipher algorithm or key length \"" + cipherAlg + "\", " + keyLength);
	}
	
	public static DHParameterSpec toExplicitDHParameterSpec(DHParameters params) {
		BigInteger p = params.getP();
		BigInteger generator = params.getG();
		int order = (int)params.getL();
		return new DHParameterSpec(p, generator, order);
	}

	/**
	 * The public key algorithm (like RSA or) with some extra information (like 1024 bits).
	 * 
	 * @param publicKey a public key
	 * 
	 * @return the algorithm
	 */
	public static String getDetailedPublicKeyAlgorithm(PublicKey publicKey) {
		String publicKeyAlgorithm = publicKey.getAlgorithm();
		if (publicKey instanceof RSAPublicKey) {
			RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
			int publicKeyBitLength = rsaPublicKey.getModulus().bitLength();
			publicKeyAlgorithm += " [" + publicKeyBitLength + " bit]";
		} else if (publicKey instanceof ECPublicKey) {
			ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
			ECParameterSpec ecParams = ecPublicKey.getParams();
			String name = getCurveName(ecParams);
			if (name != null) {
				publicKeyAlgorithm += " [" + name + "]";
			}
		}
		return publicKeyAlgorithm;
	}

	/**
	 * Gets the curve name if known (or null).
	 * 
	 * @param params an specification of the curve
	 * 
	 * @return the name
	 */
	public static String getCurveName(ECParameterSpec params) {
		org.bouncycastle.jce.spec.ECNamedCurveSpec namedECParams = toNamedCurveSpec(params);
		if (namedECParams == null) { return null; }
		return namedECParams.getName();
	}

	public static ECParameterSpec toExplicitECParameterSpec(ECNamedCurveParameterSpec parameterSpec) {
		return toExplicitECParameterSpec(toECNamedCurveSpec(parameterSpec));
	}

	/**
	 * Translates (named) curve spec to JCA compliant explicit param spec.
	 * 
	 * @param params
	 * @return another spec not name based
	 */
	public static ECParameterSpec toExplicitECParameterSpec(ECParameterSpec params) {
		try {
			ECPoint g = params.getGenerator();
			BigInteger n = params.getOrder(); // Order, order
			int h = params.getCofactor(); // co-factor
			EllipticCurve curve = params.getCurve();
			BigInteger a = curve.getA();
			BigInteger b = curve.getB();
			ECField field = curve.getField();
			if (field instanceof ECFieldFp) {
				BigInteger p = ((ECFieldFp)field).getP();
				ECField resultField = new ECFieldFp(p);
				EllipticCurve resultCurve = new EllipticCurve(resultField, a, b);
				ECParameterSpec resultParams = new ECParameterSpec(resultCurve, g, n, h);
				return resultParams;
			} else if (field instanceof ECFieldF2m) {
				int m = ((ECFieldF2m)field).getM();
				ECField resultField = new ECFieldF2m(m);
				EllipticCurve resultCurve = new EllipticCurve(resultField, a, b);
				ECParameterSpec resultParams = new ECParameterSpec(resultCurve, g, n, h);
				return resultParams;
			} else {
				System.out.println("WARNING: could not make named EC param spec explicit");
				return params;
			}
		} catch (Exception e) {
			System.out.println("WARNING: could not make named EC param spec explicit");
			return params;
		}
	}

	private static org.bouncycastle.jce.spec.ECNamedCurveSpec toNamedCurveSpec(ECParameterSpec ecParamSpec) {
		if (ecParamSpec == null) { return null; }
		if (ecParamSpec instanceof org.bouncycastle.jce.spec.ECNamedCurveSpec) { return (org.bouncycastle.jce.spec.ECNamedCurveSpec)ecParamSpec; }
		@SuppressWarnings("unchecked")
		List<String> names = (List<String>)Collections.list(ECNamedCurveTable.getNames());
		List<org.bouncycastle.jce.spec.ECNamedCurveSpec> namedSpecs = new ArrayList<org.bouncycastle.jce.spec.ECNamedCurveSpec>();
		for (String name: names) {
			org.bouncycastle.jce.spec.ECNamedCurveSpec namedSpec = toECNamedCurveSpec(ECNamedCurveTable.getParameterSpec(name));
			if (namedSpec.getCurve().equals(ecParamSpec.getCurve())
					&& namedSpec.getGenerator().equals(ecParamSpec.getGenerator())
					&& namedSpec.getOrder().equals(ecParamSpec.getOrder())
					&& namedSpec.getCofactor() == ecParamSpec.getCofactor()
					) {
				namedSpecs.add(namedSpec);
			}
		}
		if (namedSpecs.size() == 0) {
			//			throw new IllegalArgumentException("No named curve found");
			return null;
		} else if (namedSpecs.size() == 1) {
			return namedSpecs.get(0);
		} else {
			return namedSpecs.get(0);
		}
	}

	/**
	 * Translates internal BC named curve spec to BC provided JCA compliant named curve spec.
	 * 
	 * @param namedParamSpec
	 * 
	 * @return
	 */
	public static org.bouncycastle.jce.spec.ECNamedCurveSpec toECNamedCurveSpec(org.bouncycastle.jce.spec.ECNamedCurveParameterSpec namedParamSpec) {
		String name = namedParamSpec.getName();
		org.bouncycastle.math.ec.ECCurve curve = namedParamSpec.getCurve();
		org.bouncycastle.math.ec.ECPoint generator = namedParamSpec.getG();
		BigInteger order = namedParamSpec.getN();
		BigInteger coFactor = namedParamSpec.getH();
		byte[] seed = namedParamSpec.getSeed();
		return new org.bouncycastle.jce.spec.ECNamedCurveSpec(name, curve, generator, order, coFactor, seed);
	}

	/*
	 * NOTE: Woj, I moved this here from DG14File, seemed more appropriate here. -- MO
	 * FIXME: Do we still need this now that we have reconstructPublicKey? -- MO
	 * 
	 * Woj says: Here we need to some hocus-pokus, the EAC specification require for
	 * all the key information to include the domain parameters explicitly. This is
	 * not what Bouncy Castle does by default. But we first have to check if this is
	 * the case.
	 */
	public static SubjectPublicKeyInfo toSubjectPublicKeyInfo(PublicKey publicKey) {
		try {
			String algorithm = publicKey.getAlgorithm();
			if ("EC".equals(algorithm) || "ECDH".equals(algorithm) || (publicKey instanceof ECPublicKey)) {
				ASN1InputStream asn1In = new ASN1InputStream(publicKey.getEncoded());
				SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo((ASN1Sequence)asn1In.readObject());
				asn1In.close();
				AlgorithmIdentifier algorithmIdentifier = subjectPublicKeyInfo.getAlgorithm();
				String algOID = algorithmIdentifier.getAlgorithm().getId();
				if (!SecurityInfo.ID_EC_PUBLIC_KEY.equals(algOID)) {
					throw new IllegalStateException("Was expecting id-ecPublicKey (" + SecurityInfo.ID_EC_PUBLIC_KEY_TYPE + "), found " + algOID);
				}
				ASN1Primitive derEncodedParams = algorithmIdentifier.getParameters().toASN1Primitive();
				X9ECParameters params = null;
				if (derEncodedParams instanceof ASN1ObjectIdentifier) {
					ASN1ObjectIdentifier paramsOID = (ASN1ObjectIdentifier)derEncodedParams;

					/* It's a named curve from X9.62. */
					params = X962NamedCurves.getByOID(paramsOID);
					if (params == null) { throw new IllegalStateException("Could not find X9.62 named curve for OID " + paramsOID.getId()); }

					/* Reconstruct the parameters. */
					org.bouncycastle.math.ec.ECPoint generator = params.getG();
					org.bouncycastle.math.ec.ECCurve curve = generator.getCurve();
					generator = curve.createPoint(generator.getX().toBigInteger(), generator.getY().toBigInteger(), false);
					params = new X9ECParameters(params.getCurve(), generator, params.getN(), params.getH(), params.getSeed());
				} else {
					/* It's not a named curve, we can just return the decoded public key info. */
					return subjectPublicKeyInfo;
				}

				if (publicKey instanceof org.bouncycastle.jce.interfaces.ECPublicKey) {
					org.bouncycastle.jce.interfaces.ECPublicKey ecPublicKey = (org.bouncycastle.jce.interfaces.ECPublicKey)publicKey;
					AlgorithmIdentifier id = new AlgorithmIdentifier(subjectPublicKeyInfo.getAlgorithm().getAlgorithm(), params.toASN1Primitive());
					org.bouncycastle.math.ec.ECPoint q = ecPublicKey.getQ();
					/* FIXME: investigate the compressed versus uncompressed point issue. What is allowed in TR03110? -- MO */
					// In case we would like to compress the point:
					// p = p.getCurve().createPoint(p.getX().toBigInteger(), p.getY().toBigInteger(), true);
					subjectPublicKeyInfo = new SubjectPublicKeyInfo(id, q.getEncoded());
					return subjectPublicKeyInfo;
				} else {
					return subjectPublicKeyInfo;
				}
			} else if ("DH".equals(algorithm) || (publicKey instanceof DHPublicKey)) {
				DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
				DHParameterSpec dhSpec = dhPublicKey.getParams();
				return new SubjectPublicKeyInfo(
						new AlgorithmIdentifier(EACObjectIdentifiers.id_PK_DH,
								new DHParameter(dhSpec.getP(), dhSpec.getG(), dhSpec.getL()).toASN1Primitive()),
								new ASN1Integer(dhPublicKey.getY()));
			} else {
				throw new IllegalArgumentException("Unrecognized key type, found " + publicKey.getAlgorithm() + ", should be DH or ECDH");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static PublicKey toPublicKey(SubjectPublicKeyInfo subjectPublicKeyInfo) {
		try {
			byte[] encodedPublicKeyInfoBytes = subjectPublicKeyInfo.getEncoded(ASN1Encoding.DER);
			KeySpec keySpec = new X509EncodedKeySpec(encodedPublicKeyInfoBytes);
			try {
				KeyFactory factory = KeyFactory.getInstance("DH");
				return factory.generatePublic(keySpec);
			} catch (GeneralSecurityException gse) {
				KeyFactory factory = KeyFactory.getInstance("EC", BC_PROVIDER);
				return factory.generatePublic(keySpec);
			}
		} catch (GeneralSecurityException gse2) {
			gse2.printStackTrace();
			return null;
		} catch (Exception ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	/**
	 * Reconstructs the public key to use explicit domain params for EC public keys
	 * 
	 * @param publicKey the public key
	 * 
	 * @return the same public key (if not EC or error), or a reconstructed one (if EC)
	 */
	public static PublicKey reconstructPublicKey(PublicKey publicKey) {
		if (!(publicKey instanceof ECPublicKey)) { return publicKey; }
		try {
			ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
			ECPoint w = ecPublicKey.getW();
			ECParameterSpec params = ecPublicKey.getParams();
			params = toExplicitECParameterSpec(params);
			if (params == null) { return publicKey; }
			ECPublicKeySpec explicitPublicKeySpec = new ECPublicKeySpec(w, params);
			return KeyFactory.getInstance("EC", BC_PROVIDER).generatePublic(explicitPublicKeySpec);
		} catch (Exception e) {
			System.out.println("WARNING: could not make public key param spec explicit");
			return publicKey;
		}
	}

	//	/**
	//	 * Reconstructs the public key to use explicit domain params for EC public keys
	//	 * 
	//	 * @param publicKey the public key
	//	 * 
	//	 * @return the same public key (if not EC), or a reconstructed one (if EC)
	//	 */
	//	public static PublicKey reconstructPublicKey(PublicKey publicKey) {
	//		if (!(publicKey instanceof ECPublicKey)) { return publicKey; }
	//		ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
	//		ECPoint w = ecPublicKey.getW();
	//		ECParameterSpec params = ecPublicKey.getParams();
	//		ECPoint g = params.getGenerator();
	//		BigInteger n = params.getOrder(); // Order, order
	//		int h = params.getCofactor(); // co-factor
	//		EllipticCurve curve = params.getCurve();
	//		BigInteger a = curve.getA();
	//		BigInteger b = curve.getB();
	//		ECField field = curve.getField();
	//		if (field instanceof ECFieldFp) {
	//			BigInteger p = ((ECFieldFp)field).getP();
	//			ECField resultField = new ECFieldFp(p);
	//			EllipticCurve resultCurve = new EllipticCurve(resultField, a, b);
	//			ECParameterSpec resultParams = new ECParameterSpec(resultCurve, g, n, h);
	//			ECPublicKeySpec resultPublicKeySpec = new ECPublicKeySpec(w, resultParams);
	//			try {
	//				return KeyFactory.getInstance("EC", BC_PROVIDER).generatePublic(resultPublicKeySpec);
	//			} catch (GeneralSecurityException gse) {
	//				gse.printStackTrace();
	//				return publicKey;
	//			}
	//		} else if (field instanceof ECFieldF2m) {
	//			int m = ((ECFieldF2m)field).getM();
	//			ECField resultField = new ECFieldF2m(m);
	//			EllipticCurve resultCurve = new EllipticCurve(resultField, a, b);
	//			ECParameterSpec resultParams = new ECParameterSpec(resultCurve, g, n, h);
	//			ECPublicKeySpec resultPublicKeySpec = new ECPublicKeySpec(w, resultParams);
	//			try {
	//				return KeyFactory.getInstance("EC", BC_PROVIDER).generatePublic(resultPublicKeySpec);
	//			} catch (GeneralSecurityException gse) {
	//				gse.printStackTrace();
	//				return publicKey;
	//			}
	//		} else {
	//			return publicKey;
	//		}
	//	}

	public static byte[] encodePublicKeyDataObject(String oid, PublicKey publicKey) {
		return encodePublicKeyDataObject(oid, publicKey, true);
	}

	/**
	 * Based on TR-SAC 1.01 4.5.1 and 4.5.2.
	 * 
	 * NOTE: For signing authentication token, not for sending to smart card.
	 * 
	 * @param oid
	 * @param publicKey
	 * @param isContextKnown whether context of public key is known to receiver (we will not include domain parameters in that case).
	 * 
	 */
	public static byte[] encodePublicKeyDataObject(String oid, PublicKey publicKey, boolean isContextKnown) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(bOut);
		try {
			tlvOut.writeTag(0x7F49); // FIXME: constant for 7F49 */
			if (publicKey instanceof DHPublicKey) {
				DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
				DHParameterSpec params = dhPublicKey.getParams();
				BigInteger p = params.getP();
				int l = params.getL();
				BigInteger generator = params.getG();
				BigInteger y = dhPublicKey.getY();
				tlvOut.write(new ASN1ObjectIdentifier(oid).getEncoded()); /* Object Identifier, NOTE: encoding already contains 0x06 tag  */
				if (!isContextKnown) {
					tlvOut.writeTag(0x81); tlvOut.writeValue(i2os(p)); /* p: Prime modulus */
					tlvOut.writeTag(0x82); tlvOut.writeValue(i2os(BigInteger.valueOf(l))); /* q: Order of the subgroup */
					tlvOut.writeTag(0x83); tlvOut.writeValue(i2os(generator)); /* Generator */
				}
				tlvOut.writeTag(0x84); tlvOut.writeValue(i2os(y)); /* y: Public value */
			} else if (publicKey instanceof ECPublicKey) {				
				ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
				ECParameterSpec params = ecPublicKey.getParams();
				BigInteger p = getPrime(params);
				EllipticCurve curve = params.getCurve();
				BigInteger a = curve.getA(); 
				BigInteger b = curve.getB();
				ECPoint generator = params.getGenerator();
				BigInteger order = params.getOrder();
				int coFactor = params.getCofactor();
				ECPoint publicPoint = ecPublicKey.getW();
				tlvOut.write(new ASN1ObjectIdentifier(oid).getEncoded()); /* Object Identifier, NOTE: encoding already contains 0x06 tag */
				if (!isContextKnown) {
					tlvOut.writeTag(0x81); tlvOut.writeValue(i2os(p)); /* Prime modulus */
					tlvOut.writeTag(0x82); tlvOut.writeValue(i2os(a)); /* First coefficient */
					tlvOut.writeTag(0x83); tlvOut.writeValue(i2os(b)); /* Second coefficient */
					tlvOut.writeTag(0x84); tlvOut.write(i2os(generator.getAffineX())); tlvOut.write(i2os(generator.getAffineY())); tlvOut.writeValueEnd(); /* Base point, FIXME: correct encoding? */
					tlvOut.writeTag(0x85); tlvOut.writeValue(i2os(order)); /* Order of the base point */
				}
				tlvOut.writeTag(0x86); tlvOut.writeValue(publicKeyECPointToOS(publicPoint)); /* Public point */				
				if (!isContextKnown) {
					tlvOut.writeTag(0x87); tlvOut.writeValue(i2os(BigInteger.valueOf(coFactor))); /* Cofactor */			
				}
			}
			tlvOut.writeValueEnd(); /* 7F49 */
			tlvOut.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalStateException("Error in encoding public key");
		}
		return bOut.toByteArray();
	}

	/**
	 * Write uncompressed coordinates (for EC) or public value (DH).
	 * 
	 * FIXME: how can we be sure coords are uncompressed?
	 * 
	 * @param publicKey
	 * 
	 * @return encoding for smart card
	 */
	public static byte[] encodePublicKeyForSmartCard(PublicKey publicKey) {
		if (publicKey == null) {
			throw new IllegalArgumentException("Cannot encode null public key");
		}
		if (publicKey instanceof ECPublicKey) {
			ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
			try {
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				bOut.write(Util.publicKeyECPointToOS(ecPublicKey.getW()));
				byte[] encodedPublicKey = bOut.toByteArray();
				bOut.close();
				return encodedPublicKey;
			} catch (IOException ioe) {
				/* NOTE: Should never happen. */
				throw new IllegalStateException("Internal error writing to memory: " + ioe.getMessage());
			}
		} else if (publicKey instanceof DHPublicKey) {
			DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
			return Util.i2os(dhPublicKey.getY());
		} else {
			throw new IllegalArgumentException("Unsupported public key: " + publicKey.getClass().getCanonicalName());
		}
	}

	public static PublicKey decodePublicKeyFromSmartCard(byte[] encodedPublicKey, AlgorithmParameterSpec params) {
		if (params == null) { throw new IllegalArgumentException("Params cannot be null"); }
		try {
			DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(encodedPublicKey));
			if (params instanceof ECParameterSpec) {
				int b = dataIn.read();
				if (b != 0x04) { throw new IllegalArgumentException("Expected encoded public key to start with 0x04"); }
				int length = (encodedPublicKey.length - 1) / 2;
				byte[] xCoordBytes = new byte[length];
				byte[] yCoordBytes = new byte[length];
				dataIn.readFully(xCoordBytes);
				dataIn.readFully(yCoordBytes);
				dataIn.close();

				BigInteger p = getPrime(params);
				BigInteger x = Util.os2fe(xCoordBytes, p);
				BigInteger y = Util.os2fe(yCoordBytes, p);
				ECPoint w = new ECPoint(x, y);

				ECParameterSpec ecParams = (ECParameterSpec)params;
				KeyFactory kf = KeyFactory.getInstance("EC");
				return kf.generatePublic(new ECPublicKeySpec(w, ecParams));
			} else if (params instanceof DHParameterSpec) {
				int b = dataIn.read();
				if (b != 0x04) { throw new IllegalArgumentException("Expected encoded public key to start with 0x04"); }
				int length = encodedPublicKey.length - 1;
				byte[] publicValue = new byte[length];
				dataIn.readFully(publicValue);
				dataIn.close();

				BigInteger y = Util.os2i(publicValue);

				KeyFactory kf = KeyFactory.getInstance("DH");
				DHParameterSpec dhParams = (DHParameterSpec)params;
				return kf.generatePublic(new DHPublicKeySpec(y, dhParams.getP(), dhParams.getG()));
			}
			throw new IllegalArgumentException("Expected ECParameterSpec or DHParameterSpec, found " + params.getClass().getCanonicalName());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalArgumentException(ioe.getMessage());
		} catch (GeneralSecurityException gse) {
			gse.printStackTrace();
			throw new IllegalArgumentException(gse.getMessage());
		}
	}

	public static String inferMacAlgorithmFromCipherAlgorithm(String cipherAlg) {
		if (cipherAlg == null) { throw new IllegalArgumentException("Cannot infer MAC algorithm from cipher algoruthm null"); }
		/*
		 * NOTE: AESCMAC will generate 128bit results, not 64bit,
		 * both authentication token generation and secure messaging,
		 * where the Mac is applied, will copy only the first 8 bytes. -- MO
		 */
		if (cipherAlg.startsWith("DESede")) {
			/* FIXME: Is macAlg = "ISO9797Alg3Mac" equivalent to macAlg = "DESedeMac"??? - MO */
			return "ISO9797Alg3Mac";
		} else if (cipherAlg.startsWith("AES")) {
			return "AESCMAC";
		} else {
			throw new IllegalArgumentException("Cannot infer MAC algorithm from cipher algorithm \"" + cipherAlg + "\"");
		}
	}

	public static byte[] generateAuthenticationToken(String oid, SecretKey macKey, PublicKey publicKey) throws GeneralSecurityException {
		String cipherAlg = PACEInfo.toCipherAlgorithm(oid);
		Mac mac = Mac.getInstance(inferMacAlgorithmFromCipherAlgorithm(cipherAlg), BC_PROVIDER);
		byte[] encodedPCDPublicKeyDataObject = encodePublicKeyDataObject(oid, publicKey);
		mac.init(macKey);
		byte[] maccedPublicKeyDataObject = mac.doFinal(encodedPCDPublicKeyDataObject);
		LOGGER.info("DEBUG: maccedPublicKeyDataObject.length = " + maccedPublicKeyDataObject.length);
		
		/* Output length needs to be 64 bits. */
		byte[] authenticationToken = new byte[8];
		System.arraycopy(maccedPublicKeyDataObject, 0, authenticationToken, 0, authenticationToken.length);
		return authenticationToken;
	}

	public static String inferProtocolIdentifier(PublicKey publicKey) {
		String algorithm = publicKey.getAlgorithm();
		if ("EC".equals(algorithm) || "ECDH".equals(algorithm)) {
			return SecurityInfo.ID_PK_ECDH_OID;
		} else if ("DH".equals(algorithm)) {
			return SecurityInfo.ID_PK_DH_OID;
		} else {
			throw new IllegalArgumentException("Wrong key type. Was expecting ECDH or DH public key.");
		}
	}

	public static AlgorithmParameterSpec mapNonceGM(byte[] nonceS, byte[] sharedSecretH, AlgorithmParameterSpec params) {
		if (params == null) { throw new IllegalArgumentException("Unsupported parameters for mapping nonce"); }
		if (params instanceof ECParameterSpec) {
			ECParameterSpec ecParams = (ECParameterSpec)params;
			BigInteger affineX = os2i(sharedSecretH);
			BigInteger affineY = computeAffineY(affineX, ecParams);
			ECPoint sharedSecretPointH = new ECPoint(affineX, affineY);
			return mapNonceGMWithECDH(os2i(nonceS), sharedSecretPointH, ecParams);
		} else if (params instanceof DHParameterSpec) {
			DHParameterSpec dhParams = (DHParameterSpec)params;
			return mapNonceGMWithDH(os2i(nonceS), os2i(sharedSecretH), dhParams);
		} else {
			throw new IllegalArgumentException("Unsupported parameters for mapping nonce, expected ECParameterSpec or DHParameterSpec, found " + params.getClass().getCanonicalName());
		}
	}

	public static AlgorithmParameterSpec mapNonceIM(byte[] nonceS, byte[] nonceT, byte[] sharedSecretH, AlgorithmParameterSpec params) {
		/* FIXME: work in progress. */
		return null;
	}

	private static ECParameterSpec mapNonceGMWithECDH(BigInteger nonceS, ECPoint sharedSecretPointH, ECParameterSpec params) {
		/*
		 * D~ = (p, a, b, G~, n, h) where G~ = [s]G + H
		 */
		ECPoint generator = params.getGenerator();
		EllipticCurve curve = params.getCurve();
		BigInteger a = curve.getA();
		BigInteger b = curve.getB();
		ECFieldFp field = (ECFieldFp)curve.getField();
		BigInteger p = field.getP();
		BigInteger order = params.getOrder();
		int cofactor = params.getCofactor();
		ECPoint ephemeralGenerator = add(multiply(nonceS, generator, params), sharedSecretPointH, params);
		if (!toBouncyCastleECPoint(ephemeralGenerator, params).isValid()) {
			LOGGER.info("ephemeralGenerator is not a valid point");
		}
		return new ECParameterSpec(new EllipticCurve(new ECFieldFp(p), a, b), ephemeralGenerator, order, cofactor);
	}

	private static DHParameterSpec mapNonceGMWithDH(BigInteger nonceS, BigInteger sharedSecretH, DHParameterSpec params) {
		// g~ = g^s * h
		BigInteger p = params.getP();
		BigInteger generator = params.getG();
		BigInteger ephemeralGenerator = generator.modPow(nonceS, p).multiply(sharedSecretH).mod(p);
		return new DHParameterSpec(p, ephemeralGenerator, params.getL());
	}

	private static ECPoint add(ECPoint x, ECPoint y, ECParameterSpec params) {
		org.bouncycastle.math.ec.ECPoint bcX = toBouncyCastleECPoint(x, params);
		org.bouncycastle.math.ec.ECPoint bcY = toBouncyCastleECPoint(y, params);
		org.bouncycastle.math.ec.ECPoint bcSum = bcX.add(bcY);
		return fromBouncyCastleECPoint(bcSum);
	}

	public static ECPoint multiply(BigInteger s, ECPoint point, ECParameterSpec params) {
		org.bouncycastle.math.ec.ECPoint bcPoint = toBouncyCastleECPoint(point, params);
		org.bouncycastle.math.ec.ECPoint bcProd = bcPoint.multiply(s);
		return fromBouncyCastleECPoint(bcProd);
	}

	private static byte[] getBytes(String str) {
		byte[] bytes = str.getBytes();
		try {
			bytes = str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException use) {
			/* NOTE: unlikely. */
			use.printStackTrace();
		}
		return bytes;
	}

	public static BigInteger getPrime(AlgorithmParameterSpec params) {
		if (params == null) { throw new IllegalArgumentException("Parameters null"); }
		if (params instanceof DHParameterSpec) {
			return ((DHParameterSpec)params).getP();
		} else if (params instanceof ECParameterSpec) {
			EllipticCurve curve = ((ECParameterSpec)params).getCurve();
			ECField field = curve.getField();
			if (!(field instanceof ECFieldFp)) { throw new IllegalStateException("Was expecting prime field of type ECFieldFp, found " + field.getClass().getCanonicalName()); }
			return ((ECFieldFp)field).getP();
		} else {
			throw new IllegalArgumentException("Unsupported agreement algorithm, was expecting DHParameterSpec or ECParameterSpec, found " + params.getClass().getCanonicalName());
		}
	}

	public static byte[] wrapDO(byte tag, byte[] data) {
		if (data == null) { throw new IllegalArgumentException("Data to wrap is null"); }
		byte[] result = new byte[data.length + 2];
		result[0] = tag;
		result[1] = (byte)data.length;
		System.arraycopy(data, 0, result, 2, data.length);
		return result;
	}

	public static byte[] unwrapDO(byte expectedTag, byte[] wrappedData) {
		if (wrappedData == null || wrappedData.length < 2)  { throw new IllegalArgumentException("Wrapped data is null or length < 2"); }
		byte actualTag = wrappedData[0];
		if (actualTag != expectedTag) { throw new IllegalArgumentException("Expected tag " + Integer.toHexString(expectedTag) + ", found tag " + Integer.toHexString(actualTag)); }
		byte[] result = new byte[wrappedData.length - 2];
		System.arraycopy(wrappedData, 2, result, 0, result.length);
		return result;
	}

	public static String inferKeyAgreementAlgorithm(PublicKey publicKey) {
		if (publicKey instanceof ECPublicKey) {
			return "ECDH";
		} else if (publicKey instanceof DHPublicKey) {
			return "DH";
		} else {
			throw new IllegalArgumentException("Unsupported public key: " + publicKey);
		}
	}

	/**
	 * This just solves the curve equation for y.
	 * 
	 * @param affineX the x coord of a point on the curve
	 * @param params EC parameters for curve over Fp
	 * @return the corresponding y coord
	 */
	private static BigInteger computeAffineY(BigInteger affineX, ECParameterSpec params) {
		ECCurve bcCurve = toBouncyCastleECCurve(params);
		ECFieldElement a = bcCurve.getA();
		ECFieldElement b = bcCurve.getB();
		ECFieldElement x = bcCurve.fromBigInteger(affineX);		
		ECFieldElement y = x.multiply(x).add(a).multiply(x).add(b).sqrt();
		return y.toBigInteger();
	}

	private static org.bouncycastle.math.ec.ECPoint toBouncyCastleECPoint(ECPoint point, ECParameterSpec params) {
		org.bouncycastle.math.ec.ECCurve bcCurve = toBouncyCastleECCurve(params);
		return bcCurve.createPoint(point.getAffineX(), point.getAffineY(), false);
		// return new org.bouncycastle.math.ec.ECPoint.Fp(bcCurve, bcCurve.fromBigInteger(point.getAffineX()), bcCurve.fromBigInteger(point.getAffineY()));
	}

	private static ECPoint fromBouncyCastleECPoint(org.bouncycastle.math.ec.ECPoint point) {
		point = point.normalize();
		if (!point.isValid()) { LOGGER.warning("point not valid"); }
		return new ECPoint(point.getAffineXCoord().toBigInteger(), point.getAffineYCoord().toBigInteger());
	}

	private static ECCurve toBouncyCastleECCurve(ECParameterSpec params) {
		EllipticCurve curve = params.getCurve();
		ECField field = curve.getField();
		if (!(field instanceof ECFieldFp)) { throw new IllegalArgumentException("Only prime field supported (for now), found " + field.getClass().getCanonicalName()); }
		int coFactor = params.getCofactor();
		BigInteger order = params.getOrder();
		BigInteger a = curve.getA();
		BigInteger b = curve.getB();
		BigInteger p = getPrime(params);
		return new ECCurve.Fp(p, a, b, order, BigInteger.valueOf(coFactor));
	}
}
