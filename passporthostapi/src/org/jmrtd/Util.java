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
 * $Id: Util.java 1386 2012-03-22 11:16:16Z martijno $
 */

package org.jmrtd;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECField;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import net.sourceforge.scuba.util.Hex;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.jmrtd.lds.MRZInfo;

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
 * @version $Revision: 1386 $
 */
public class Util {

	/** Encrypt mode. */
	public static final int ENC_MODE = 1;
	
	/** MAC mode. */
	public static final int MAC_MODE = 2;

	private Util() {
	}

	/**
	 * Derives the ENC or MAC key from the keySeed.
	 *
	 * @param keySeed the key seed.
	 * @param mode either <code>ENC_MODE</code> or <code>MAC_MODE</code>.
	 * 
	 * @return the key.
	 */
	public static SecretKey deriveKey(byte[] keySeed, int mode) throws GeneralSecurityException {
		MessageDigest shaDigest = MessageDigest.getInstance("SHA1");
		shaDigest.update(keySeed);
		byte[] c = { 0x00, 0x00, 0x00, (byte)mode };
		shaDigest.update(c);
		byte[] hash = shaDigest.digest();
		byte[] key = new byte[24];
		System.arraycopy(hash, 0, key, 0, 8);
		System.arraycopy(hash, 8, key, 8, 8);
		System.arraycopy(hash, 0, key, 16, 8);
		SecretKeyFactory desKeyFactory = SecretKeyFactory.getInstance("DESede");
		return desKeyFactory.generateSecret(new DESedeKeySpec(key));
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
	public static byte[] pad(/*@ non_null */ byte[] in,
			int offset, int length) {
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
			return KeyFactory.getInstance("EC", "BC").generatePublic(explicitPublicKeySpec);
		} catch (Exception e) {
			System.out.println("WARNING: could not make public key param spec explicit");
			return publicKey;
		}
	}

	private static org.bouncycastle.jce.spec.ECNamedCurveSpec toNamedCurveSpec(ECParameterSpec ecParamSpec) {
		if (ecParamSpec == null) { return null; }
		if (ecParamSpec instanceof org.bouncycastle.jce.spec.ECNamedCurveSpec) { return (org.bouncycastle.jce.spec.ECNamedCurveSpec)ecParamSpec; }
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
}

