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
 * $Id$
 */

package org.jmrtd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.ECFieldF2m;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.CommandAPDU;
import net.sourceforge.scuba.smartcards.ISO7816;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import net.sourceforge.scuba.tlv.ASN1Constants;
import net.sourceforge.scuba.tlv.TLVOutputStream;
import net.sourceforge.scuba.util.Hex;

import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.MRZInfo;

/**
 * Service for initializing blank passport reference applets.
 *
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class PassportPersoService extends CardService {

	private static final long serialVersionUID = 4975606132249105202L;

	private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

	private static final byte INS_SET_DOCNR_DOB_DOE = (byte) 0x10;
	private static final short AAPRIVKEY_FID = 0x0001;
	private static final byte INS_PUT_DATA = (byte) 0xda;;

	private static final byte PRIVMODULUS_TAG = 0x60;
	private static final byte PRIVEXPONENT_TAG = 0x61;
	private static final byte MRZ_TAG = 0x62;
	private static final byte ECPRIVATE_TAG = 0x63;
	private static final byte CVCERTIFICATE_TAG = 0x64;

	private PassportService service;

	public PassportPersoService(CardService service)
	throws CardServiceException {
		this.service = (service instanceof PassportService) ? (PassportService)service : new PassportService(service);
	}

	private CommandAPDU createPutDataApdu(byte p1, byte p2, byte[] data) {
		byte cla = 0;
		byte ins = INS_PUT_DATA;
		return new CommandAPDU(cla, ins, p1, p2, data);
	}

	private byte[] putData(byte p1, byte p2,
			byte[] data) throws CardServiceException {
		CommandAPDU capdu = createPutDataApdu(p1, p2, data);
		SecureMessagingWrapper wrapper = service.getWrapper();

		if (wrapper != null) {
			capdu = wrapper.wrap(capdu);
		}

		ResponseAPDU rapdu = transmit(capdu);
		if (wrapper != null) {
			rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
		}
		return rapdu.getData();
	}

	/**
	 * Sends a PUT_DATA command to the card to set the private keys used for
	 * Active Authentication.
	 * 
	 * @param privateKey holding the private key data.
	 * @throws IOException on error.
	 */
	public void putPrivateKey(PrivateKey privateKey) throws CardServiceException {
		if (!"RSA".equals(privateKey.getAlgorithm())) {
			throw new CardServiceException("Was expecting RSA private key");
		}
		try {
			byte[] privModulus = ((RSAPrivateKey)privateKey).getModulus().toByteArray();
			byte[] privExponent = ((RSAPrivateKey)privateKey).getPrivateExponent().toByteArray();
			
			/* Construct objects for modulus and exponent and send them to applet. */
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TLVOutputStream tlvOut = new TLVOutputStream(out);
			tlvOut.writeTag(PRIVMODULUS_TAG);
			tlvOut.writeTag(ASN1Constants.OCTET_STRING_TYPE_TAG);
			tlvOut.writeValue(privModulus);
			tlvOut.writeValueEnd(); /* PRIVMODULUS_TAG */
			out.flush();
			out.close();
			putData((byte)0, PRIVMODULUS_TAG, out.toByteArray());

			out.reset();
			
			tlvOut = new TLVOutputStream(out);
			tlvOut.writeTag(PRIVEXPONENT_TAG);
			tlvOut.writeTag(ASN1Constants.OCTET_STRING_TYPE_TAG);
			tlvOut.writeValue(privExponent);
			tlvOut.writeValueEnd(); /* PRIVEXPONENT_TAG */
			out.flush();
			out.close();
			putData((byte)0, PRIVEXPONENT_TAG, out.toByteArray());
			
		} catch (IOException ioe) {
			throw new CardServiceException(ioe.toString());
		} catch (Exception pe) {
			throw new CardServiceException(pe.toString());
		}
	}

	/**
	 * Sends a PUT_DATA command to the card to set the private key used for
	 * Extended Access Control.
	 * 
	 * @param privKey
	 *            holding the private key data.
	 * @throws CardServiceException
	 *             on error.
	 */
	public void putPrivateEACKey(PrivateKey privKey)
	throws CardServiceException {

		ECPrivateKey privateKey = (ECPrivateKey)privKey;
		byte[] aArray = privateKey.getParams().getCurve().getA().toByteArray();
		byte[] bArray = privateKey.getParams().getCurve().getB().toByteArray();

		byte[] rArray = privateKey.getParams().getOrder().toByteArray();
		short k = (short) privateKey.getParams().getCofactor();

		byte[] kArray = new byte[2];
		kArray[0] = (byte) ((k & 0xFF00) >> 8);
		kArray[1] = (byte) (k & 0xFF);

		ECFieldF2m fm = (ECFieldF2m) privateKey.getParams().getCurve()
		.getField();
		byte[] pArray = null;
		if (fm.getMidTermsOfReductionPolynomial() == null) {
			int m = fm.getM();
			pArray = new byte[2];
			pArray[0] = (byte) ((m & 0xFF00) >> 8);
			pArray[1] = (byte) (m & 0xFF);
		} else {
			int[] ms = fm.getMidTermsOfReductionPolynomial();
			int off = 0;
			pArray = new byte[ms.length * 2];
			for (int i = 0; i < ms.length; i++) {
				int m = ms[i];
				pArray[off + 0] = (byte) ((m & 0xFF00) >> 8);
				pArray[off + 1] = (byte) (m & 0xFF);
				off += 2;
			}
		}

		org.bouncycastle.jce.interfaces.ECPrivateKey ecPrivateKey = (org.bouncycastle.jce.interfaces.ECPrivateKey)privateKey;
		org.bouncycastle.math.ec.ECPoint point = ecPrivateKey.getParameters().getG();
		byte[] gArray = point.getEncoded();
		byte[] sArray = privateKey.getS().toByteArray();
		pArray = tagData((byte) 0x81, pArray);
		aArray = tagData((byte) 0x82, aArray);
		bArray = tagData((byte) 0x83, bArray);
		gArray = tagData((byte) 0x84, gArray);
		rArray = tagData((byte) 0x85, rArray);
		sArray = tagData((byte) 0x86, sArray);
		kArray = tagData((byte) 0x87, kArray);

		int offset = 0;
		byte[] all = new byte[pArray.length + aArray.length + bArray.length
		                      + gArray.length + rArray.length + sArray.length + kArray.length];
		System.arraycopy(pArray, 0, all, offset, pArray.length);
		offset += pArray.length;
		System.arraycopy(aArray, 0, all, offset, aArray.length);
		offset += aArray.length;
		System.arraycopy(bArray, 0, all, offset, bArray.length);
		offset += bArray.length;
		System.arraycopy(gArray, 0, all, offset, gArray.length);
		offset += gArray.length;
		System.arraycopy(rArray, 0, all, offset, rArray.length);
		offset += rArray.length;
		System.arraycopy(sArray, 0, all, offset, sArray.length);
		offset += sArray.length;
		System.arraycopy(kArray, 0, all, offset, kArray.length);
		offset += kArray.length;

		putData((byte) 0, ECPRIVATE_TAG, all);
	}

	// For quick and dirty tagging of data
	// FIXME: Woj, you know we have TLVOutputStream for quicker and way less dirty tagging ;) -- MO
	private static byte[] tagData(byte tag, byte[] data) {
		byte[] result = new byte[data.length + 2];
		System.arraycopy(data, 0, result, 2, data.length);
		result[0] = tag;
		result[1] = (byte) data.length;
		return result;
	}

	/**
	 * Sends a PUT_DATA command to the card to set the root cv certificate for
	 * Extended Access Control.
	 * 
	 * @param certificate
	 *            card verifiable certificate
	 * @throws CardServiceException
	 *             on error.
	 */
	public void putCVCertificate(CardVerifiableCertificate certificate)
	throws CardServiceException {
		try {
			putData((byte) 1, CVCERTIFICATE_TAG, certificate.getCertBodyData());
		} catch (Exception e) {
			throw new CardServiceException(e.toString());
		}
	}

	/***************************************************************************
	 * Sends a CREATE_FILE APDU to the card.
	 * 
	 * @param fid
	 *            (file identifier) of the new file.
	 * @param length
	 *            of the new file.
	 */
	public void createFile(short fid, short length) throws CardServiceException {
		sendCreateFile(service.getWrapper(), fid, length);
	}

	private CommandAPDU createCreateFileAPDU(short fid, short length) {
		byte p1 = (byte) 0x00;
		byte p2 = (byte) 0x00;
		int le = 0;
		byte[] data = { 0x63, 4, (byte) ((length >>> 8) & 0xff),
				(byte) (length & 0xff), (byte) ((fid >>> 8) & 0xff),
				(byte) (fid & 0xff) };
		CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816,	ISO7816.INS_CREATE_FILE, p1, p2, data, le);
		return apdu;
	}

	private byte[] sendCreateFile(SecureMessagingWrapper wrapper, short fid, short length) throws CardServiceException {
		CommandAPDU capdu = createCreateFileAPDU(fid, length);
		if (wrapper != null) {
			capdu = wrapper.wrap(capdu);
		}
		ResponseAPDU rapdu = transmit(capdu);
		if (wrapper != null) {
			rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
		}
		return rapdu.getData();
	}

	private CommandAPDU createUpdateBinaryAPDU(short offset, int data_len, byte[] data) {
		byte p1 = (byte) ((offset >>> 8) & 0xff);
		byte p2 = (byte) (offset & 0xff);
		byte[] chunk = new byte[data_len];
		System.arraycopy(data, 0, chunk, 0, data_len);
		CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816,	ISO7816.INS_UPDATE_BINARY, p1, p2, chunk);
		return apdu;
	}

	private byte[] sendUpdateBinary(SecureMessagingWrapper wrapper,
			short offset, int data_len, byte[] data)
	throws CardServiceException {
		CommandAPDU capdu = createUpdateBinaryAPDU(offset, data_len, data);
		if (wrapper != null) {
			capdu = wrapper.wrap(capdu);
		}
		ResponseAPDU rapdu = transmit(capdu);

		if (wrapper != null) {
			rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
		}
		return rapdu.getData();

	}

	private int getPlainDataMaxLength(SecureMessagingWrapper wrapper) {
		int maxWithoutSM = 0xff;
		byte[] dummyData = new byte[maxWithoutSM];
		
		CommandAPDU dummy = new CommandAPDU((byte) 0, (byte) 0, (byte) 0, (byte) 0, dummyData);
		CommandAPDU cWrapped = wrapper.wrap(dummy);

		byte[] wrappedApdu = cWrapped.getBytes();
		int x = wrappedApdu.length - dummy.getBytes().length;
		int lowestMod8 = ((maxWithoutSM - x) / 8) * 8;
		return lowestMod8;
	}

	/**
	 * Writes a DataGroup in the passport
	 * 
	 * @param fid
	 *            the fid of the file to write
	 * @param i
	 *            the inputstream of the file to write
	 * @throws CardServiceException
	 */
	public void writeFile(short fid, InputStream i) throws CardServiceException {
		SecureMessagingWrapper wrapper = service.getWrapper();
		try {
			int length = 0xff;
			if (wrapper != null) {
				length -= 32;
			}
			byte[] data = new byte[length];

			int r = 0;
			short offset = 0;
			while (true) {
				r = i.read(data, (short) 0, data.length);
				if (r == -1)
					break;
				sendUpdateBinary(wrapper, offset, r, data);
				offset += r;
			}
		} catch (IOException ioe) {
			throw new CardServiceException(ioe.toString());
		}
	}

	/**
	 * Initiates the passport with MRZ data
	 * 
	 * @param documentNumber
	 *            the passport document number
	 * @param dateOfBirth
	 *            the date of birth of the holder
	 * @param dateOfExpiry
	 *            the date of expiry of the passport
	 * @throws CardServiceException
	 */
	public void setBAC(String documentNumber, String dateOfBirth, String dateOfExpiry)
	throws CardServiceException {
		try {
			byte[] docNr = documentNumber.trim().toUpperCase().getBytes("UTF-8");
			byte[] dob = dateOfBirth.getBytes("UTF-8");
			byte[] doe = dateOfExpiry.getBytes("UTF-8");
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TLVOutputStream tlvOut = new TLVOutputStream(out);
			tlvOut.writeTag(MRZ_TAG);
			tlvOut.writeTag(ASN1Constants.OCTET_STRING_TYPE_TAG);
			tlvOut.writeValue(docNr);
			tlvOut.writeTag(ASN1Constants.OCTET_STRING_TYPE_TAG);
			tlvOut.writeValue(dob);
			tlvOut.writeTag(ASN1Constants.OCTET_STRING_TYPE_TAG);
			tlvOut.writeValue(doe);
			tlvOut.writeValueEnd(); /* MRZ_TAG */
			tlvOut.flush();
			tlvOut.close();
			putData((byte) 0, MRZ_TAG, out.toByteArray());
		} catch (Exception ioe) {
			throw new CardServiceException(ioe.toString());
		}
	}

	/**
	 * Locks the passport applet so that no data may be written to it.
	 * 
	 * @throws CardServiceException
	 */
	public void lockApplet()
	throws CardServiceException {
		putData((byte) 0xde, (byte) 0xad, null);
	}

	/**
	 * Selects a file on the applet.
	 * 
	 * @param fid the file ID to select
	 * @throws CardServiceException
	 */
	public void selectFile(short fid)
	throws CardServiceException {
		service.sendSelectFile(service.getWrapper(), fid);
	}

	private short getFidFromFilename(String fileName) {
		if (!fileName.endsWith("bin"))
			return -1;

		short fid;
		fid = Short.decode("0x" + fileName.substring(0, 4));
		return fid;
	}

	/**
	 * Burns a passport
	 * 
	 * @param passportData
	 * @throws CardServiceException
	 * @throws IOException
	 */
	public void burnPassport(ZipFile passportData) throws CardServiceException, IOException {
		Enumeration<? extends ZipEntry> dgs = passportData.entries();

		while (dgs.hasMoreElements()) {
			ZipEntry dgZip = dgs.nextElement();
			short fid = getFidFromFilename(dgZip.getName());
			short length = (short) (dgZip.getSize() & 0xffff);

			if (fid != -1) {
				createFile(fid, length);
				selectFile(fid);
				writeFile(fid, passportData.getInputStream(dgZip));
			}

			if (fid == 0x0101) {
				DG1File dg1 = new DG1File(passportData.getInputStream(dgZip));
				MRZInfo mrzInfo = dg1.getMRZInfo();
				setBAC(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());
			}
		}
	}

	/**
	 * Dumps the content of a passport as a zip file
	 * 
	 * @throws IOException
	 * @deprecated Client code should take care of this
	 */
	public void dumpPassport(File f) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(f));

		short passportFiles[] = {PassportService.EF_DG1, PassportService.EF_DG2, PassportService.EF_DG3, PassportService.EF_DG4, PassportService.EF_DG5, PassportService.EF_DG6, PassportService.EF_DG7, PassportService.EF_DG8, PassportService.EF_DG9, PassportService.EF_DG10,
				PassportService.EF_DG11, PassportService.EF_DG12, PassportService.EF_DG13, PassportService.EF_DG14, PassportService.EF_DG15, PassportService.EF_DG16, PassportService.EF_COM, PassportService.EF_SOD};

		for(short i : passportFiles) {
			InputStream is = null;
			try {
				is = service.getInputStream(i);
			} catch (CardServiceException e) {
				continue;
			}		
			ZipEntry entry = new ZipEntry(Hex.shortToHexString(i) + ".bin");
			zip.putNextEntry(entry);
			byte[] buf = new byte[0xff];
			while(is.available() > 0) {
				int read = is.read(buf);
				zip.write(buf, 0, read);
			}
			zip.closeEntry();			
		}
		zip.close();
	}

	public void close() {
		service.close();
	}

	public boolean isOpen() {
		return service.isOpen();	}

	public void open() throws CardServiceException {
		service.open();
	}

	public ResponseAPDU transmit(CommandAPDU capdu) throws CardServiceException {
		ResponseAPDU rapdu =  service.transmit(capdu);
		notifyExchangedAPDU(0, capdu, rapdu);
		return rapdu;
	}
	
	public byte[] getATR() throws CardServiceException {
		return service.getATR();
	}
}
