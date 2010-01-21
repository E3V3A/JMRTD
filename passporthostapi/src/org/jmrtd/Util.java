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
 * $Id$
 */

package org.jmrtd;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import net.sourceforge.scuba.util.Hex;

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
 * @version $Revision$
 */
public class Util
{
	public static final int ENC_MODE = 1;
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
	public static SecretKey deriveKey(byte[] keySeed, int mode)
	throws GeneralSecurityException {
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
	 * @param docNrStr a string containing the document number.
	 * @param dateOfBirthStr a string containing the date of birth (YYMMDD).
	 * @param dateOfExpiryStr a string containing the date of expiry (YYMMDD).
	 *
	 * @return a byte array of length 16 containing the key seed.
	 */
	public static byte[] computeKeySeed(String docNrStr,
			String dateOfBirthStr,
			String dateOfExpiryStr)
	throws UnsupportedEncodingException, GeneralSecurityException {
		if (docNrStr.length() != 9
				|| dateOfBirthStr.length() != 6
				|| dateOfExpiryStr.length() != 6) {
			throw new UnsupportedEncodingException("Wrong length MRZ input");
		}

		/* Check digits... */
		byte[] cd1 = { (byte)MRZInfo.checkDigit(docNrStr) };
		byte[] cd2 = { (byte)MRZInfo.checkDigit(dateOfBirthStr) };
		byte[] cd3 = { (byte)MRZInfo.checkDigit(dateOfExpiryStr) };

		MessageDigest shaDigest = MessageDigest.getInstance("SHA1");
		shaDigest.update(docNrStr.getBytes("UTF-8"));
		shaDigest.update(cd1);
		shaDigest.update(dateOfBirthStr.getBytes("UTF-8"));
		shaDigest.update(cd2);
		shaDigest.update(dateOfExpiryStr.getBytes("UTF-8"));
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

//	public static String printDERObject(byte[] derBytes) throws IOException {
//		ASN1InputStream asn1 = new ASN1InputStream(derBytes);
//		return printDERObject(asn1.readObject());
//	}
//
//	public static String printDERObject(DERObject derObj) {
//		if(derObj instanceof DERSequence) return printDERObject((DERSequence)derObj);
//		if(derObj instanceof DERSet) return printDERObject((DERSet)derObj);
//		if(derObj instanceof DERTaggedObject) return printDERObject((DERTaggedObject)derObj);
//		if(derObj instanceof DERNull) return printDERObject((DERNull)derObj);
//		if(derObj instanceof DERUnknownTag) return printDERObject((DERUnknownTag)derObj);
//		if(derObj instanceof DERObjectIdentifier) return printDERObject((DERObjectIdentifier)derObj);
//		if(derObj instanceof DERString) return printDERObject((DERString)derObj);
//		if(derObj instanceof DEROctetString) return printDERObject((DEROctetString)derObj);
//		if(derObj instanceof DERUTCTime) return printDERObject((DERUTCTime)derObj);
//		if(derObj instanceof DERGeneralizedTime) return printDERObject((DERGeneralizedTime)derObj);
//		if(derObj instanceof DEREnumerated) return printDERObject((DEREnumerated)derObj);
//		if(derObj instanceof DERInteger) return printDERObject((DERInteger)derObj);
//		if(derObj instanceof DERBoolean) return printDERObject((DERBoolean)derObj);
//		if(derObj instanceof DERApplicationSpecific) return printDERObject((DERApplicationSpecific)derObj);
//		return derObj.getClass().getSimpleName()+"?";        
//	}
//
//	public static String printDERObject(DERSequence derSeq) {
//		String r = "DERSequence:";
//		for (Enumeration<DERObject> e = derSeq.getObjects() ; e.hasMoreElements() ;) {
//			r = r + "\n  " + printDERObject(e.nextElement()).replaceAll("\n", "\n  ");
//		}
//		return r;
//	}
//
//	public static String printDERObject(DERSet derSet) {
//		String r = "DERSet:";
//		for (Enumeration<DERObject> e = derSet.getObjects() ; e.hasMoreElements() ;) {
//			r = r + "\n  " + printDERObject(e.nextElement()).replaceAll("\n", "\n  ");
//		}
//		return r;
//	}
//
//	public static String printDERObject(DERTaggedObject derTaggedObject) {
//		String r = "DERTaggedObject:";
//		r = r + "\n  TagNum: " + derTaggedObject.getTagNo();
//		r = r + "\n  Object: " + printDERObject(derTaggedObject.getObject()).replaceAll("\n", "\n  ");
//		return r;
//	}
//
//
//	public static String printDERObject(DERNull derNull) {
//		return "DERNull";
//	}
//
//	public static String printDERObject(DERUnknownTag derUnknownTag) {
//		return "DERUnknownTag: "+derUnknownTag.getTag();
//	}
//
//	public static String printDERObject(DERObjectIdentifier derObjectIdentifier) {
//		return "DERObjectIdentifier: "+derObjectIdentifier.getId();
//	}
//
//	public static String printDERObject(DEROctetString derOctetString) {
//		return "DEROctetString: "+Hex.bytesToHexString(derOctetString.getOctets());
//	}
//
//	public static String printDERObject(DERString derString) {
//		return derString.getClass().getSimpleName()+": "+derString.getString();
//	}
//
//	public static String printDERObject(DERUTCTime derUTCTime) {
//		return "DERUTCTime: "+derUTCTime.getAdjustedTime();
//	}
//
//	public static String printDERObject(DERGeneralizedTime derGeneralizedTime) {
//		return "DERGeneralizedTime: "+derGeneralizedTime.getTime();
//	}
//
//	public static String printDERObject(DEREnumerated derEnumerated) {
//		return "DEREnumerated: "+derEnumerated.getValue();
//	}
//
//	public static String printDERObject(DERBoolean derBoolean) {
//		return "DERBoolean: "+derBoolean.isTrue();
//	}
//
//	public static String printDERObject(DERInteger derInteger) {
//		return "DERInteger: "+derInteger.getValue();
//	}
//
//	public static String printDERObject(DERApplicationSpecific derApplicationSpecific) {
//		String r = "DERApplicationSpecific:";
//		r = r + "\n  Application Tag: " + derApplicationSpecific.getApplicationTag();
//		r = r + "\n  Contents:        " + Hex.bytesToHexString(derApplicationSpecific.getContents());
//		return r;
//	}
}

