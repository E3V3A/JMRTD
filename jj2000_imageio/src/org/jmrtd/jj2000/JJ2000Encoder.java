/* $Id: $ */

package org.jmrtd.jj2000;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import jj2000.j2k.codestream.writer.FileCodestreamWriter;
import jj2000.j2k.codestream.writer.HeaderEncoder;
import jj2000.j2k.codestream.writer.PktEncoder;
import jj2000.j2k.encoder.EncoderSpecs;
import jj2000.j2k.entropy.encoder.EntropyCoder;
import jj2000.j2k.entropy.encoder.PostCompRateAllocator;
import jj2000.j2k.fileformat.FileFormatBoxes;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.DataBlk;
import jj2000.j2k.image.DataBlkInt;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.Tiler;
import jj2000.j2k.image.forwcomptransf.ForwCompTransf;
import jj2000.j2k.image.input.ImgReader;
import jj2000.j2k.quantization.quantizer.Quantizer;
import jj2000.j2k.roi.encoder.ROIScaler;
import jj2000.j2k.util.CodestreamManipulator;
import jj2000.j2k.util.FacilityManager;
import jj2000.j2k.util.ParameterList;
import jj2000.j2k.wavelet.analysis.AnWTFilter;
import jj2000.j2k.wavelet.analysis.ForwardWT;

/**
 * Utility class for encoding access to jj2000 library.
 * Tested with jj2000-5.2-SNAPSHOT.jar only.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
public class JJ2000Encoder {

	private final static String[][] ENCODER_PINFO = {
		{ "debug", null, "", "off" },
		{ "disable_jp2_extension", "[on|off]", "", "off" },
		{ "file_format", "[on|off]", "", "on" },
		{ "pph_tile", "[on|off]", "", "off" },
		{ "pph_main", "[on|off]", "", "off" },
		{ "pfile", "<filename of arguments file>", "", null },
		{ "tile_parts", "", "0", null },
		{ "tiles", "<nominal tile width> <nominal tile height>", "", "0 0" },
		{ "ref", "<x> <y>", "", "0 0" },
		{ "tref", "<x> <y>",  "", "0 0" },
		{ "rate", "<output bitrate in bpp>", "", "1.5" },
		{ "lossless", "[on|off]", "", "off" },
		{ "i", "<image file> [,<image file> [,<image file> ... ]]",  "", null },
		{ "o", "<file name>",  "", null },
		{ "verbose", null,  "", "off" },
		{ "v", "[on|off]", "", "off" },
		{ "u", "[on|off]", "", "off" } };

	/**
	 * Prevent unwanted instantiation.
	 */
	private JJ2000Encoder() {
	}

	public static void encode(OutputStream outputStream, Bitmap bitmap, double bitRate) throws IOException {

		BlkImgDataSrc imgsrc = new BitmapDataSrc(bitmap);

		String[][] pinfo = getAllEncoderParameters();
		ParameterList defpl = new ParameterList();
		for (int i = pinfo.length - 1; i >= 0; i--) {
			if (pinfo[i][3] != null) {
				defpl.put(pinfo[i][0], pinfo[i][3]);
			}
		}
		ParameterList pl = new ParameterList(defpl);
		pl.put("rate", Double.toString(bitRate));
		pl.put("tile_parts", Integer.toString(0));
		encode(imgsrc, outputStream, pl);
	}

	/* ONLY PRIVATE METHODS BELOW. */

	private static void encode(BlkImgDataSrc imgsrc, OutputStream fileOutputStream, ParameterList pl) throws IOException {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		int tilesCount = 1; // MO - ?? get this from pl?
		int componentCount = 3; // MO - ?? get this from pl?
		EncoderSpecs encoderSpecs = new EncoderSpecs(tilesCount, componentCount, imgsrc, pl);

		int refx = 0, refy = 0, trefx = 0, trefy = 0, tw = imgsrc.getImgWidth(), th = imgsrc.getImgHeight(); // MO - 1 tile?

		Tiler imgtiler = new Tiler(imgsrc, refx, refy, trefx, trefy, tw, th);

		// Creates the forward component transform
		ForwCompTransf fctransf = new ForwCompTransf(imgtiler, encoderSpecs);

		// Creates ImgDataConverter
		ImgDataConverter converter = new ImgDataConverter(fctransf);

		// Creates ForwardWT (forward wavelet transform)
		ForwardWT dwt = ForwardWT.createInstance(converter, pl, encoderSpecs);

		// Creates Quantizer
		Quantizer quant = Quantizer.createInstance(dwt,encoderSpecs);

		// Creates ROIScaler
		ROIScaler rois = ROIScaler.createInstance(quant, pl, encoderSpecs);

		// Creates EntropyCoder
		EntropyCoder ecoder = EntropyCoder.createInstance(rois, pl,
				encoderSpecs.cblks,
				encoderSpecs.pss,
				encoderSpecs.bms,
				encoderSpecs.mqrs,
				encoderSpecs.rts,
				encoderSpecs.css,
				encoderSpecs.sss,
				encoderSpecs.lcs,
				encoderSpecs.tts
				);

		// Creates CodestreamWriter
		FileCodestreamWriter bwriter = new FileCodestreamWriter(outputStream, Integer.MAX_VALUE);

		// Creates the rate allocator
		float rate = (float)(pl.getFloatParameter("rate"));
		PostCompRateAllocator ralloc = PostCompRateAllocator.createInstance(ecoder, pl, rate, bwriter, encoderSpecs);

		int pktspertp = 0;
		boolean tempSop = false;
		boolean tempEph = false;
		boolean pphTile = false;
		boolean pphMain = false;

		if (pl.getParameter("tiles") == null) {
			System.err.println("No tiles option specified");
			return;
		}

		if (pl.getParameter("pph_tile").equals("on")){
			pphTile = true;

			if(pl.getParameter("Psop").equals("off")){
				pl.put("Psop","on");
				tempSop = true;
			}
			if(pl.getParameter("Peph").equals("off")){
				pl.put("Peph","on");
				tempEph = true;
			}                  
		}

		if (pl.getParameter("pph_main").equals("on")){
			pphMain = true;

			if(pl.getParameter("Psop").equals("off")){
				pl.put("Psop","on");
				tempSop = true;
			}
			if(pl.getParameter("Peph").equals("off")){
				pl.put("Peph","on");
				tempEph = true;
			}                  
		}

		if(pphTile && pphMain)
			System.err.println("Can't have packed packet headers in both main and tile headers");

		if (pl.getParameter("rate") == null) {
			System.err.println("Target bitrate not specified");
			return;
		}
		try {
			rate = pl.getFloatParameter("rate");
			if(rate==-1) {
				rate = Float.MAX_VALUE;
			}
		} catch (NumberFormatException e) {
			System.err.println("Invalid value in 'rate' option: " + pl.getParameter("rate"));
			if(pl.getParameter("debug").equals("on")) {
				e.printStackTrace();
			} else {
				System.err.println("Use '-debug' option for more details");
			}
			return;
		}
		try {
			pktspertp = pl.getIntParameter("tile_parts");
			if(pktspertp != 0){
				if(pl.getParameter("Psop").equals("off")){
					pl.put("Psop","on");
					tempSop = true;
				}
				if(pl.getParameter("Peph").equals("off")){
					pl.put("Peph","on");
					tempEph = true;
				}   
			}               
		} catch (NumberFormatException e) {
			System.err.println("Invalid value in 'tile_parts' option: " + pl.getParameter("tile_parts"));
			if(pl.getParameter("debug").equals("on")) {
				e.printStackTrace();
			} else {
				System.err.println("Use '-debug' option for more details");
			}
			return;
		}

		// MO - ? get this from imgsrc somehow? Our bitmaps are unsigned, but we transform to signed.
		boolean[] imsigned = new boolean[componentCount];
		for (int i = 0; i < componentCount; i++) {
			imsigned[i] = true;
		}

		// Instantiates the HeaderEncoder
		HeaderEncoder headenc = new HeaderEncoder(imgsrc, imsigned, dwt, imgtiler, encoderSpecs, rois, ralloc, pl);

		ralloc.setHeaderEncoder(headenc);

		// Writes header to be able to estimate header overhead
		headenc.encodeMainHeader();

		//Initializes rate allocator, with proper header
		// overhead. This will also encode all the data
		try {
			ralloc.initialize();
		} catch (RuntimeException e) {
			//            if (WRITE_ABORTED.equals(e.getMessage())) {
			//                bwriter.close();
			//                tmpFile.delete();
			//                processWriteAborted();
			e.printStackTrace();
			return;
			//		} else throw e;
		}

		// Write header (final)
		headenc.reset();
		headenc.encodeMainHeader();

		// Insert header into the codestream
		bwriter.commitBitstreamHeader(headenc);

		// Now do the rate-allocation and write result
		ralloc.runAndWrite();

		//Done for data encoding
		bwriter.close();

		// Calculate file length
		int fileLength = bwriter.getLength();

		String outname = "tmp";
		int ntiles = 1;

		// Tile-parts and packed packet headers
		if(pktspertp > 0 || pphTile || pphMain) {
			int headInc;
			try {
				CodestreamManipulator cm = new CodestreamManipulator(outname, ntiles, pktspertp, pphMain, pphTile, tempSop, tempEph);
				fileLength += cm.doCodestreamManipulation();
				String res="";
				if(pktspertp > 0) {
					FacilityManager.
					getMsgLogger().println("Created tile-parts containing at most "+ pktspertp+ " packets per tile.", 4, 6);
				}
				if(pphTile) {
					FacilityManager.getMsgLogger().
					println("Moved packet headers to tile headers", 4, 6);
				}
				if(pphMain) {
					FacilityManager.getMsgLogger().
					println("Moved packet headers "+
							"to main header",4,6);
				}
			} catch(IOException e) {
				System.err.println("Error while creating tileparts or packed packet headers" + ((e.getMessage() != null) ? (":\n"+e.getMessage()) : ""));
				if(pl.getParameter("debug").equals("on")) {
					e.printStackTrace();
				} else {
					System.err.println("Use '-debug' option for more details");
				}
				return;
			}
		}

		// File Format
		int nc = imgsrc.getNumComps() ;
		int[] bpc = new int[nc];
		for(int comp = 0; comp < nc; comp++) {
			bpc[comp] = imgsrc.getNomRangeBits(comp);
		}

		outputStream.flush();
		InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

		MyFileFormatWriter ffw = new MyFileFormatWriter(inputStream, fileOutputStream, imgsrc.getImgHeight(), imgsrc.getImgWidth(), nc, bpc, fileLength);
		fileLength += ffw.writeFileFormat();
	}

	private static String[][] getAllEncoderParameters() {
		Vector<String[]> vec = new Vector<String[]>();

		String[][] str = ENCODER_PINFO;
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = ForwCompTransf.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = AnWTFilter.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = ForwardWT.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = Quantizer.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = ROIScaler.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = EntropyCoder.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = HeaderEncoder.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = PostCompRateAllocator.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = PktEncoder.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}
		str = new String[vec.size()][4];
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				str[i] = (String[])vec.elementAt(i);
			}
		}

		return str;
	}

	static class BitmapDataSrc extends ImgReader {

		private Bitmap bitmap;
		private int nomRangeBits;

		public BitmapDataSrc(Bitmap bitmap) {
			this.bitmap = bitmap;
			this.w = bitmap.getWidth();
			this.h = bitmap.getHeight();
			this.nc = 3;
			this.nomRangeBits = 8;
		}

		@Override
		public int getFixedPoint(int c) {
			if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
			return 0;
		}

		@Override
		public int getNomRangeBits(int c) {
			if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
			return nomRangeBits;
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public boolean isOrigSigned(int c) {
			if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
			return false;
		}

		@Override
		public DataBlk getInternCompData(DataBlk blk, int c) {
			return getCompData(blk, c);
		}

		@Override
		public DataBlk getCompData(DataBlk blk, int c) {
			if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
			int[] pixels = bitmap.getPixels();
			if (blk == null) { blk = new DataBlkInt(0, 0, w, h); System.out.println("DEBUG: created new blk"); }
			int blkDataLength = (blk.h - blk.uly) * (blk.w - blk.ulx);
			int[] compData = (int[])blk.getData();
			if (compData == null) {
				compData = new int[blkDataLength];
			} else {
				assert compData.length == blkDataLength;
			}
			int i = 0;
			for (int y = 0; y < blk.h; y++) {
				for (int x = 0; x < blk.w; x++) {
					int bitmapIndex = (blk.uly + y) * blk.w + (blk.ulx + x);
					int signedCompValue = JJ2000Util.unsignedARGBToSignedComponent(pixels[bitmapIndex], c, nc, nomRangeBits);
					compData[i] = signedCompValue;
					i++;
				}
			}
			blk.setData(compData);
			return blk;
		}
	}

	/**
	 * This class writes the file format wrapper that may or may not exist
	 * around a valid JPEG 2000 codestream. This class writes the simple
	 * possible legal fileformat
	 * 
	 * @see jj2000.j2k.fileformat.reader.FileFormatReader
	 * */
	static class MyFileFormatWriter implements FileFormatBoxes {

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
}
