/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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

package sos.mrtd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.RSAKeyGenParameterSpec;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import sos.smartcards.CardServiceException;
import sos.smartcards.ISO7816;
import sos.tlv.BERTLVObject;
import sos.util.ASN1Utils;
import sos.util.Hex;

/**
 * Service for initializing blank passport reference applets.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 */
public class PassportPersoService {
	private static final byte INS_SET_DOCNR_DOB_DOE = (byte) 0x10;
	private static final short AAPRIVKEY_FID = 0x0001;
	private static final byte INS_PUT_DATA = (byte) 0xda;;

	private static final byte PRIVMODULUS_TAG = 0x60;
	private static final byte PRIVEXPONENT_TAG = 0x61;
	private static final byte MRZ_TAG = 0x62;

	private PassportService service;
	
	public PassportPersoService(PassportService service)
			throws CardServiceException {
		this.service = service;
	}
	
	/**
	 * Generates an RSA keypair fit for Active Authentication.
	 * 
	 * @return a KeyPair
	 * @throws GeneralSecurityException
	 * @throws NoSuchAlgorithmException
	 *             when BouncyCastle provider cannot be found.
	 */
	public static KeyPair generateAAKeyPair() throws GeneralSecurityException,
			NoSuchAlgorithmException {
		String preferredProvider = "BC";
		Provider provider = Security.getProvider(preferredProvider);
		if (provider == null) {
			return null;
		}
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
				provider);
		generator.initialize(new RSAKeyGenParameterSpec(1024,
				RSAKeyGenParameterSpec.F4));
		KeyPair keyPair = generator.generateKeyPair();
		return keyPair;
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
		ResponseAPDU rapdu = service.transmit(capdu);
		if (wrapper != null) {
			rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
		}
		return rapdu.getData();
	}

	/***************************************************************************
	 * Sends a PUT_DATA command to the card to set the private keys used for
	 * Active Authentication.
	 * 
	 * @param wrapper
	 *            for secure messaging.
	 * @param key
	 *            holding the private key data.
	 * @throws IOException
	 *             on error.
	 */
	public void putPrivateKey(PrivateKey key)
			throws CardServiceException {
		try {
			byte[] encodedPriv = key.getEncoded();
			BERTLVObject encodedPrivObject = BERTLVObject
					.getInstance(new ByteArrayInputStream(encodedPriv));
			byte[] privKeyData = (byte[]) encodedPrivObject.getChildByIndex(2)
					.getValue();
			BERTLVObject privKeyDataObject = BERTLVObject
					.getInstance(new ByteArrayInputStream(privKeyData));
			byte[] privModulus = (byte[]) privKeyDataObject.getChildByIndex(1)
					.getValue();
			byte[] privExponent = (byte[]) privKeyDataObject.getChildByIndex(3)
					.getValue();

			putPrivateKey(privModulus, privExponent);
		} catch (IOException ioe) {
			throw new CardServiceException(ioe.toString());
		} catch (Exception pe) {
			throw new CardServiceException(pe.toString());
		}
	}

	private void putPrivateKey(byte[] privModulus, byte[] privExponent)
			throws CardServiceException {
		try {
			BERTLVObject privModulusObject = new BERTLVObject(PRIVMODULUS_TAG,
					new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG,
							privModulus));

			putData((byte) 0, PRIVMODULUS_TAG, privModulusObject
					.getEncoded());

			BERTLVObject privExponentObject = new BERTLVObject(
					PRIVEXPONENT_TAG, new BERTLVObject(
							BERTLVObject.OCTET_STRING_TYPE_TAG, privExponent));

			putData((byte) 0, PRIVEXPONENT_TAG, privExponentObject
					.getEncoded());
		} catch (Exception ioe) {
			throw new CardServiceException(ioe.toString());
		}
	}

	/***************************************************************************
	 * Sends a CREATE_FILE APDU to the card.
	 * 
	 * @param wrapper
	 *            for secure messaging.
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
		CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816,
				ISO7816.INS_CREATE_FILE, p1, p2, data, le);
		return apdu;
	}

	private byte[] sendCreateFile(SecureMessagingWrapper wrapper, short fid,
			short length) throws CardServiceException {
		CommandAPDU capdu = createCreateFileAPDU(fid, length);
		if (wrapper != null) {
			capdu = wrapper.wrap(capdu);
		}
		ResponseAPDU rapdu = service.transmit(capdu);
		if (wrapper != null) {
			rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
		}
		return rapdu.getData();
	}

	// FIXEDME: MO added the arraycopy here...
	// MO: Did I? Changed offset to 0...
	private CommandAPDU createUpdateBinaryAPDU(short offset, int data_len,
			byte[] data) {
		byte p1 = (byte) ((offset >>> 8) & 0xff);
		byte p2 = (byte) (offset & 0xff);
		byte[] chunk = new byte[data_len];
		// System.out.println("DEBUG: offset = " + offset + " data.length = " +
		// data.length + " chunk.length = " + chunk.length + " data_len = " +
		// data_len);
		System.arraycopy(data, 0, chunk, 0, data_len);
		CommandAPDU apdu = new CommandAPDU(ISO7816.CLA_ISO7816,
				ISO7816.INS_UPDATE_BINARY, p1, p2, chunk);
		return apdu;
	}

	private byte[] sendUpdateBinary(SecureMessagingWrapper wrapper,
			short offset, int data_len, byte[] data)
			throws CardServiceException {
		CommandAPDU capdu = createUpdateBinaryAPDU(offset, data_len, data);
		if (wrapper != null) {
			capdu = wrapper.wrap(capdu);
		}
		ResponseAPDU rapdu = service.transmit(capdu);
		if (wrapper != null) {
			rapdu = wrapper.unwrap(rapdu, rapdu.getBytes().length);
		}
		return rapdu.getData();

	}

	private int getPlainDataMaxLength(SecureMessagingWrapper wrapper) {
		int maxWithoutSM = 0xff;
		byte[] dummyData = new byte[maxWithoutSM];
		CommandAPDU dummy = new CommandAPDU((byte) 0, (byte) 0, (byte) 0,
				(byte) 0, dummyData);
		byte[] wrappedApdu = wrapper.wrap(dummy).getBytes();
		int x = wrappedApdu.length - dummy.getBytes().length;
		int lowestMod8 = ((maxWithoutSM - x) / 8) * 8;
		return lowestMod8;
	}

	/**
	 * Writes a DataGroup in the passport
	 * 
	 * @param wrapper
	 *            the secure messaging wrapper
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
	 * @param docNr
	 *            the passport document number
	 * @param dob
	 *            the date of birth of the holder
	 * @param doe
	 *            the expiry data of the passport
	 * @throws CardServiceException
	 */
	public void putMRZ(byte[] docNr, byte[] dob, byte[] doe)
			throws CardServiceException {
		try {
			BERTLVObject mrzObject = new BERTLVObject(MRZ_TAG,
					new BERTLVObject(BERTLVObject.OCTET_STRING_TYPE_TAG, docNr));
			mrzObject.addSubObject(new BERTLVObject(
					BERTLVObject.OCTET_STRING_TYPE_TAG, dob));
			mrzObject.addSubObject(new BERTLVObject(
					BERTLVObject.OCTET_STRING_TYPE_TAG, doe));

			putData((byte) 0, MRZ_TAG, mrzObject.getEncoded());
		} catch (Exception ioe) {
			throw new CardServiceException(ioe.toString());
		}
	}

	/**
	 * Returns an InputStream (formatted as DG15) given a public key
	 * 
	 * @param key the PublicKey instance
	 * @return an InputStream 
	 * @throws IOException
	 */
	public InputStream createDG15(PublicKey key) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		byte[] keyBytes = key.getEncoded();

		out.write(0x6F);
		out.write(ASN1Utils.lengthId(keyBytes.length)); // FIXME: use
		// BERTLVOutputStream
		out.write(keyBytes);

		return new ByteArrayInputStream(out.toByteArray());
	}

	/**
	 * Locks the passport applet so that no data may be written to it.
	 * 
	 * @param wrapper
	 * @throws CardServiceException
	 */
	public void lockApplet()
			throws CardServiceException {
		putData((byte) 0xde, (byte) 0xad, null);
	}

	private void selectFile(byte[] fid)
			throws CardServiceException {
		service.sendSelectFile(service.getWrapper(), fid);
	}

	/**
	 * Selects a file on the applet.
	 * 
	 * @param wrapper the secure messaging wrapper
	 * @param fid the file ID to select
	 * @throws CardServiceException
	 */
	public void selectFile(short fid)
			throws CardServiceException {
		byte[] fiddle = { (byte) ((fid >>> 8) & 0xff), (byte) (fid & 0xff) };
		selectFile(fiddle);
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
	 * @param wrapper
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
				String docNrString = mrzInfo.getDocumentNumber();
				SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
				String birthData = sdf.format(mrzInfo.getDateOfBirth());
				String expData = sdf.format(mrzInfo.getDateOfExpiry());
				byte[] docNr = docNrString.getBytes("ASCII");
				byte[] dob = birthData.getBytes("ASCII");
				byte[] doe = expData.getBytes("ASCII");
				putMRZ(docNr, dob, doe);
			}
		}
	}
	
	/**
	 * Dumps the content of a passport as a zip file
	 * 
	 * @throws IOException 
	 */
	public void dumpPassport(File f) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(f));
		
		short passportFiles[] = {PassportService.EF_DG1, PassportService.EF_DG2, PassportService.EF_DG3, PassportService.EF_DG4, PassportService.EF_DG5, PassportService.EF_DG6, PassportService.EF_DG7, PassportService.EF_DG8, PassportService.EF_DG9, PassportService.EF_DG10,
								 PassportService.EF_DG11, PassportService.EF_DG12, PassportService.EF_DG13, PassportService.EF_DG14, PassportService.EF_DG15, PassportService.EF_DG16, PassportService.EF_COM, PassportService.EF_SOD};

		for(short i : passportFiles) {
            InputStream is = null;
			try {
				is = service.readFile(i);
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
}
