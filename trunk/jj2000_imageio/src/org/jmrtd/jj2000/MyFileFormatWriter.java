package org.jmrtd.jj2000;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jj2000.j2k.fileformat.FileFormatBoxes;

/**
 * This class writes the file format wrapper that may or may not exist
 * around a valid JPEG 2000 codestream. This class writes the simple
 * possible legal fileformat
 * 
 * Note: changed this so that it uses OutputStream instead of File.
 * 
 * @see jj2000.j2k.fileformat.reader.FileFormatReader
 * */
class MyFileFormatWriter implements FileFormatBoxes {

	/** The file from which to read the codestream and write file */
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;

	/** Image height */
	private int height;

	/** Image width */
	private int width;

	/** Number of components */
	private int nc;

	/** Bits per component */
	private int bpc[];

	/** Flag indicating whether number of bits per component varies */
	private boolean bpcVaries;

	/** Length of codestream */
	private int clength;

	/** Length of Colour Specification Box */
	private static final int CSB_LENGTH = 15;

	/** Length of File Type Box */
	private static final int FTB_LENGTH = 20;

	/** Length of Image Header Box */
	private static final int IHB_LENGTH = 22;

	/** base length of Bits Per Component box */
	private static final int BPC_LENGTH = 8;

	/**
	 * The constructor of the FileFormatWriter. It receives all the
	 * information necessary about a codestream to generate a legal JP2 file
	 * 
	 * @param filename
	 *            The name of the file that is to be made a JP2 file
	 * 
	 * @param height
	 *            The height of the image
	 * 
	 * @param width
	 *            The width of the image
	 * 
	 * @param nc
	 *            The number of components
	 * 
	 * @param bpc
	 *            The number of bits per component
	 * 
	 * @param clength
	 *            Length of codestream
	 * */
	public MyFileFormatWriter(InputStream inputStream, OutputStream outputStream, int height, int width, int nc, int[] bpc, int clength) {
		this.height = height;
		this.width = width;
		this.nc = nc;
		this.bpc = bpc;
		this.clength = clength;

		this.dataInputStream = inputStream instanceof DataInputStream ? (DataInputStream) inputStream
				: new DataInputStream(inputStream);
		this.dataOutputStream = outputStream instanceof DataOutputStream ? (DataOutputStream) outputStream
				: new DataOutputStream(outputStream);

		bpcVaries = false;
		int fixbpc = bpc[0];
		for (int i = nc - 1; i > 0; i--) {
			if (bpc[i] != fixbpc)
				bpcVaries = true;
		}

	}

	/**
	 * This method reads the codestream and writes the file format wrapper
	 * and the codestream to the same file
	 * 
	 * @return The number of bytes increases because of the file format
	 * 
	 * @exception java.io.IOException
	 *                If an I/O error ocurred.
	 * */
	public int writeFileFormat() throws IOException {
		byte[] codestream = new byte[clength];
		dataInputStream.readFully(codestream, 0, clength);
		try {

			// Write the JP2_SINATURE_BOX
			// fi.seek(0);
			dataOutputStream.writeInt(0x0000000c);
			dataOutputStream.writeInt(JP2_SIGNATURE_BOX);
			dataOutputStream.writeInt(0x0d0a870a);

			// Write File Type box
			writeFileTypeBox();

			// Write JP2 Header box
			writeJP2HeaderBox();

			// Write the Codestream box
			writeContiguousCodeStreamBox(codestream);

			dataOutputStream.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Error while writing JP2 file format "
					+ e.getMessage());
		}
		if (bpcVaries) {
			return 12 + FTB_LENGTH + 8 + IHB_LENGTH + CSB_LENGTH + BPC_LENGTH + nc + 8;
		} else {
			return 12 + FTB_LENGTH + 8 + IHB_LENGTH + CSB_LENGTH + 8;
		}
	}

	/**
	 * This method writes the File Type box
	 * 
	 * @exception java.io.IOException
	 *                If an I/O error ocurred.
	 * */
	public void writeFileTypeBox() throws IOException {
		// Write box length (LBox)
		// LBox(4) + TBox (4) + BR(4) + MinV(4) + CL(4) = 20
		dataOutputStream.writeInt(FTB_LENGTH);

		// Write File Type box (TBox)
		dataOutputStream.writeInt(FILE_TYPE_BOX);

		// Write File Type data (DBox)
		// Write Brand box (BR)
		dataOutputStream.writeInt(FT_BR);

		// Write Minor Version
		dataOutputStream.writeInt(0);

		// Write Compatibility list
		dataOutputStream.writeInt(FT_BR);
	}

	/**
	 * This method writes the JP2Header box
	 * 
	 * @exception java.io.IOException
	 *                If an I/O error ocurred.
	 * */
	public void writeJP2HeaderBox() throws IOException {

		// Write box length (LBox)
		// if the number of bits per components varies, a bpcc box is
		// written
		if (bpcVaries) {
			dataOutputStream.writeInt(8 + IHB_LENGTH + CSB_LENGTH + BPC_LENGTH + nc);
		} else {
			dataOutputStream.writeInt(8 + IHB_LENGTH + CSB_LENGTH);
		}

		// Write a JP2Header (TBox)
		dataOutputStream.writeInt(JP2_HEADER_BOX);

		// Write image header box
		writeImageHeaderBox();

		// Write Colour Bpecification Box
		writeColourSpecificationBox();

		// if the number of bits per components varies write bpcc box
		if (bpcVaries) {
			writeBitsPerComponentBox();
		}
	}

	/**
	 * This method writes the Bits Per Component box
	 * 
	 * @exception java.io.IOException
	 *                If an I/O error ocurred.
	 * 
	 */
	public void writeBitsPerComponentBox() throws IOException {

		// Write box length (LBox)
		dataOutputStream.writeInt(BPC_LENGTH + nc);

		// Write a Bits Per Component box (TBox)
		dataOutputStream.writeInt(BITS_PER_COMPONENT_BOX);

		// Write bpc fields
		for (int i = 0; i < nc; i++) {
			dataOutputStream.writeByte(bpc[i] - 1);
		}
	}

	/**
	 * This method writes the Colour Specification box
	 * 
	 * @exception java.io.IOException
	 *                If an I/O error ocurred.
	 * 
	 */
	public void writeColourSpecificationBox() throws IOException {

		// Write box length (LBox)
		dataOutputStream.writeInt(CSB_LENGTH);

		// Write a Bits Per Component box (TBox)
		dataOutputStream.writeInt(COLOUR_SPECIFICATION_BOX);

		// Write METH field
		dataOutputStream.writeByte(CSB_METH);

		// Write PREC field
		dataOutputStream.writeByte(CSB_PREC);

		// Write APPROX field
		dataOutputStream.writeByte(CSB_APPROX);

		// Write EnumCS field
		if (nc > 1) {
			dataOutputStream.writeInt(CSB_ENUM_SRGB);
		} else {
			dataOutputStream.writeInt(CSB_ENUM_GREY);
		}
	}

	/**
	 * This method writes the Image Header box
	 * 
	 * @exception java.io.IOException
	 *                If an I/O error ocurred.
	 * */
	public void writeImageHeaderBox() throws IOException {

		// Write box length
		dataOutputStream.writeInt(IHB_LENGTH);

		// Write ihdr box name
		dataOutputStream.writeInt(IMAGE_HEADER_BOX);

		// Write HEIGHT field
		dataOutputStream.writeInt(height);

		// Write WIDTH field
		dataOutputStream.writeInt(width);

		// Write NC field
		dataOutputStream.writeShort(nc);

		// Write BPC field
		// if the number of bits per component varies write 0xff else write
		// number of bits per components
		if (bpcVaries)
			dataOutputStream.writeByte(0xff);
		else
			dataOutputStream.writeByte(bpc[0] - 1);

		// Write C field
		dataOutputStream.writeByte(IMB_C);

		// Write UnkC field
		dataOutputStream.writeByte(IMB_UnkC);

		// Write IPR field
		dataOutputStream.writeByte(IMB_IPR);
	}

	/**
	 * This method writes the Contiguous codestream box
	 * 
	 * @param cs
	 *            The contiguous codestream
	 * 
	 * @exception java.io.IOException
	 *                If an I/O error ocurred.
	 * */
	public void writeContiguousCodeStreamBox(byte[] cs) throws IOException {

		// Write box length (LBox)
		// This value is set to 0 since in this implementation, this box is
		// always last
		dataOutputStream.writeInt(clength + 8);

		// Write contiguous codestream box name (TBox)
		dataOutputStream.writeInt(CONTIGUOUS_CODESTREAM_BOX);

		// Write codestream
		for (int i = 0; i < clength; i++) {
			dataOutputStream.writeByte(cs[i]);
		}
	}
}
