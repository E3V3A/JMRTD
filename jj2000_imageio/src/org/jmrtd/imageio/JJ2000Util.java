/* $Id: $ */

package org.jmrtd.imageio;

import icc.ICCProfiler;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import jj2000.j2k.codestream.HeaderInfo;
import jj2000.j2k.codestream.reader.BitstreamReaderAgent;
import jj2000.j2k.codestream.reader.HeaderDecoder;
import jj2000.j2k.codestream.writer.FileCodestreamWriter;
import jj2000.j2k.codestream.writer.HeaderEncoder;
import jj2000.j2k.codestream.writer.PktEncoder;
import jj2000.j2k.decoder.DecoderSpecs;
import jj2000.j2k.encoder.EncoderSpecs;
import jj2000.j2k.entropy.decoder.EntropyDecoder;
import jj2000.j2k.entropy.encoder.EntropyCoder;
import jj2000.j2k.entropy.encoder.PostCompRateAllocator;
import jj2000.j2k.fileformat.reader.FileFormatReader;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.Coord;
import jj2000.j2k.image.DataBlk;
import jj2000.j2k.image.DataBlkInt;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.Tiler;
import jj2000.j2k.image.forwcomptransf.ForwCompTransf;
import jj2000.j2k.image.input.ImgReader;
import jj2000.j2k.image.invcomptransf.InvCompTransf;
import jj2000.j2k.io.RandomAccessIO;
import jj2000.j2k.quantization.dequantizer.Dequantizer;
import jj2000.j2k.quantization.quantizer.Quantizer;
import jj2000.j2k.roi.ROIDeScaler;
import jj2000.j2k.roi.encoder.ROIScaler;
import jj2000.j2k.util.ISRandomAccessIO;
import jj2000.j2k.util.ParameterList;
import jj2000.j2k.wavelet.analysis.AnWTFilter;
import jj2000.j2k.wavelet.analysis.ForwardWT;
import jj2000.j2k.wavelet.synthesis.InverseWT;
import colorspace.ColorSpace;
import colorspace.ColorSpace.CSEnum;

/**
 * Utility class for access to jj2000 library.
 * Tested with jj2000-5.2-SNAPSHOT.jar only.
 * FIXME: Only decoding for now.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
class JJ2000Util {

	/**
	 * Prevent unwanted instantiation.
	 */
	private JJ2000Util() {
	}

	private final static String[][] DECODER_PINFO = {
		{ "u", "[on|off]", "", "off" },
		{ "v", "[on|off]", "", "off" },
		{ "verbose", "[on|off]", "", "off" },
		{ "pfile", "", "", null },
		{ "res", "", "", null },
		{ "i", "", "", null },
		{ "o", "", "", null },
		{ "rate", "", "", "3" },
		{ "nbytes", "", "", "-1" },
		{ "parsing", null, "", "on" },
		{ "ncb_quit", "", "", "-1" },
		{ "l_quit", "", "", "-1" },
		{ "m_quit", "", "", "-1" },
		{ "poc_quit", null, "", "off" },
		{ "one_tp", null, "", "off" },
		{ "comp_transf", null, "", "on" },
		{ "debug", null, "", "off" },
		{ "cdstr_info", null, "", "off" },
		{ "nocolorspace", null, "", "off" },
		{ "colorspace_debug", null, "", "off" } };

	private final static String[][] ENCODER_PINFO = {
		{ "debug", null, "", "off" },
		{ "disable_jp2_extension", "[on|off]", "", "off" },
		{ "file_format", "[on|off]", "", "off" },
		{ "pph_tile", "[on|off]", "", "off" },
		{ "pph_main", "[on|off]", "", "off" },
		{ "pfile", "<filename of arguments file>", "", null },
		{ "tile_parts", "", "0" },
		{ "tiles", "<nominal tile width> <nominal tile height>", "", "0 0" },
		{ "ref", "<x> <y>", "", "0 0" },
		{ "tref", "<x> <y>",  "", "0 0" },
		{ "rate", "<output bitrate in bpp>", "", "3" },
		{ "lossless", "[on|off]", "", "off" },
		{ "i", "<image file> [,<image file> [,<image file> ... ]]",  "", null },
		{ "o", "<file name>",  "", null },
		{ "verbose", null,  "", "off" },
		{ "v", "[on|off]", "", "off" },
		{ "u", "[on|off]", "", "off" } };

	public static Bitmap read(InputStream in) throws IOException {
		String[][] pinfo = getAllDecoderParameters();
		ParameterList defpl = new ParameterList();
		for (int i = pinfo.length - 1; i >= 0; i--) {
			if (pinfo[i][3] != null)
				defpl.put(pinfo[i][0], pinfo[i][3]);
		}
		ParameterList pl = new ParameterList(defpl);

		return decode(new ISRandomAccessIO(in), pl);
	}

	public static void write(Bitmap bitmap, OutputStream outputStream) throws IOException {

		BlkImgDataSrc imgsrc = new BitmapDataSrc(bitmap);

		String[][] pinfo = getAllEncoderParameters();
		ParameterList defpl = new ParameterList();
		for (int i = pinfo.length - 1; i >= 0; i--) {
			if (pinfo[i][3] != null)
				defpl.put(pinfo[i][0], pinfo[i][3]);
		}
		ParameterList pl = new ParameterList(defpl);

		encode(imgsrc, outputStream, pl);
	}

	/* ONLY PRIVATE METHODS BELOW. */

	private static void encode(BlkImgDataSrc imgsrc, OutputStream outputStream, ParameterList pl) throws IOException {

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
				encoderSpecs.cblks, // encoderSpecs.getCodeBlockSize(),
				encoderSpecs.pss, // encoderSpecs.getPrecinctPartition(),
				encoderSpecs.bms, // encoderSpecs.getBypass(),
				encoderSpecs.mqrs, // encoderSpecs.getResetMQ(),
				encoderSpecs.rts, // encoderSpecs.getTerminateOnByte(),
				encoderSpecs.css, // encoderSpecs.getCausalCXInfo(),
				encoderSpecs.sss, // encoderSpecs.getCodeSegSymbol(),
				encoderSpecs.lcs, // encoderSpecs.getMethodForMQLengthCalc(),
				encoderSpecs.tts // encoderSpecs.getMethodForMQTermination()
				);

		// Rely on rate allocator to limit amount of data
		//		File tmpFile = File.createTempFile("jiio-", ".tmp");
		//		tmpFile.deleteOnExit();

		// Creates CodestreamWriter
		FileCodestreamWriter bwriter = new FileCodestreamWriter(outputStream, Integer.MAX_VALUE);

		// Creates the rate allocator
		float rate = (float)(pl.getFloatParameter("rate")); // encoderSpecs.getEncodingRate();
		PostCompRateAllocator ralloc = PostCompRateAllocator.createInstance(ecoder, pl, 
				rate,
				bwriter,
				encoderSpecs);		

		// MO - ? get this from imgsrc somehow? Our bitmaps are unsigned, but we transform to signed.
		boolean[] imsigned = new boolean[componentCount];
		for (int i = 0; i < componentCount; i++) {
			imsigned[i] = true;
		}

		// Instantiates the HeaderEncoder
		HeaderEncoder headenc = new HeaderEncoder(imgsrc, imsigned, dwt, imgtiler,
				encoderSpecs, rois, ralloc, pl);

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

		// Tile-parts and packed packet headers
		//		int pktspertp = encoderSpecs.getPacketPerTilePart();
		//		int ntiles = imgtiler.getNumTiles();
		//		if (pktspertp>0 || pphTile || pphMain){
		//			CodestreamManipulator cm =
		//					new CodestreamManipulator(tmpFile, ntiles, pktspertp,
		//							pphMain, pphTile, tempSop,
		//							tempEph);
		//			fileLength += cm.doCodestreamManipulation();
		//		}

		// File Format
		int nc = imgsrc.getNumComps() ;
		int[] bpc = new int[nc];
		for(int comp = 0; comp < nc; comp++) {
			bpc[comp] = imgsrc.getNomRangeBits(comp);
		}
	}

	private static Bitmap decode(RandomAccessIO in, ParameterList pl) throws IOException {

		// The codestream should be wrapped in the jp2 fileformat, Read the
		// file format wrapper
		FileFormatReader fileFormatReader = new FileFormatReader(in);
		fileFormatReader.readFileFormat();
		if (!fileFormatReader.JP2FFUsed) {
			throw new IOException("Was expecting JP2 file format");
		}
		in.seek(fileFormatReader.getFirstCodeStreamPos());

		// Instantiate header decoder and read main header
		HeaderInfo headerInfo = new HeaderInfo();
		HeaderDecoder headerDecoder = null;
		try {
			headerDecoder = new HeaderDecoder(in, pl, headerInfo);
		} catch (EOFException e) {
			throw new IOException("Codestream too short or bad header, unable to decode");
		}

		int originalComponentCount = headerDecoder.getNumComps();
		/* int nTiles = */ headerInfo.siz.getNumTiles();
		DecoderSpecs decoderSpecs = headerDecoder.getDecoderSpecs();

		// Get demixed bitdepths
		int[] originalBitDepths = new int[originalComponentCount];
		for (int i = 0; i < originalComponentCount; i++) {
			originalBitDepths[i] = headerDecoder.getOriginalBitDepth(i);
		}

		BitstreamReaderAgent bitStreamReader = BitstreamReaderAgent.createInstance(in, headerDecoder, pl, decoderSpecs, pl.getBooleanParameter("cdstr_info"), headerInfo);
		EntropyDecoder entropyDecoder = headerDecoder.createEntropyDecoder(bitStreamReader, pl);

		ROIDeScaler roiDeScaler = headerDecoder.createROIDeScaler(entropyDecoder, pl, decoderSpecs);

		Dequantizer dequantizer = headerDecoder.createDequantizer(roiDeScaler, originalBitDepths, decoderSpecs);

		// full page inverse wavelet transform
		InverseWT inverseWT = InverseWT.createInstance(dequantizer, decoderSpecs);

		// resolution level to reconstruct
		int imgRes = bitStreamReader.getImgRes();
		inverseWT.setImgResLevel(imgRes);

		ImgDataConverter imgDataConverter = new ImgDataConverter(inverseWT, 0);

		InvCompTransf invCompTransf = new InvCompTransf(imgDataConverter, decoderSpecs, originalBitDepths, pl);

		// **** Color space mapping ****
		ColorSpace colorSpace = null;
		BlkImgDataSrc color = null;
		try {
			colorSpace = new ColorSpace(in, headerDecoder, pl);
			BlkImgDataSrc channels = headerDecoder.createChannelDefinitionMapper(invCompTransf, colorSpace);
			BlkImgDataSrc resampled = headerDecoder.createResampler(channels, colorSpace);
			BlkImgDataSrc palettized = headerDecoder.createPalettizedColorSpaceMapper(resampled, colorSpace);
			color = headerDecoder.createColorSpaceMapper(palettized, colorSpace);
		} catch (Exception e) {
			throw new IOException("Error processing jp2 colorspace information: " + e.getMessage());
		}

		// This is the last image in the decoding chain and should be
		// assigned by the last transformation:
		BlkImgDataSrc decodedImage = color;
		if (color == null) {
			decodedImage = invCompTransf;
		}
		int imgComponentCount = decodedImage.getNumComps();

		// **** Create image writers/image display ****

		DataBlkInt[] blk = new DataBlkInt[imgComponentCount];
		int[] imgBitDepths = new int[imgComponentCount];

		int imgWidth = decodedImage.getImgWidth();
		int imgHeight = decodedImage.getImgHeight();

		// Find the list of tile to decode.
		Coord nT = decodedImage.getNumTiles(null);

		// Loop on vertical tiles
		for (int y = 0; y < nT.y; y++) {
			// Loop on horizontal tiles
			for (int x = 0; x < nT.x; x++) {
				decodedImage.setTile(x, y);

				int width = decodedImage.getImgWidth();
				int height = decodedImage.getImgHeight();
				int ulx = decodedImage.getImgULX();
				int uly = decodedImage.getImgULY();

				for (int i = 0; i < imgComponentCount; i++) {
					blk[i] = new DataBlkInt(ulx, uly, width, height);
					blk[i].data = null;
					blk[i] = (DataBlkInt)decodedImage.getInternCompData(blk[i], i);
					imgBitDepths[i] = decodedImage.getNomRangeBits(i);
				}
			}
		}

		CSEnum colorSpaceType = colorSpace.getColorSpace();
		if (colorSpaceType.equals(ColorSpace.sRGB)) {
			return decodeSignedRGB(blk, imgWidth, imgHeight, imgBitDepths);
			/*
			 * For Android use:
			 *   return Bitmap.createBitmap(colors, 0, imgWidth, imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
			 * 
			 * For J2SE use:
			 *   BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
			 *   image.setRGB(0, 0, imgWidth, imgHeight, colors, 0, imgWidth);
			 *   return image;
			 */			
		} else if (colorSpaceType.equals(ColorSpace.GreyScale)) {
			/* NOTE: Untested */
			return decodeGrayScale(blk, imgWidth, imgHeight, imgBitDepths);

			/* For Android use:
			 *   return Bitmap.createBitmap(colors, 0, imgWidth, imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
			 *   
			 * For J2SE use:
			 *   BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
			 *   image.setRGB(0, 0, imgWidth, imgHeight, colors, 0, imgWidth);
			 *   return image;
			 */
		}
		throw new IOException("Unsupported color space type.");
	}

	/**
	 * Decodes n-bit signed to 8-bit unsigned.
	 * 
	 * @param blk
	 * @param depths
	 * @return
	 */
	private static Bitmap decodeSignedRGB(DataBlkInt[] blk, int width, int height, int[] depths) {
		if (blk == null || blk.length != 3) {
			throw new IllegalArgumentException("Was expecting 3 bands");
		}
		if (depths == null || depths.length != 3) {
			throw new IllegalArgumentException("Was expecting 3 bands");
		}
		if (depths[0] != depths[1] || depths[1] != depths[2] || depths[2] != depths[0]) {
			throw new IllegalArgumentException("Different depths for bands");
		}

		int depth = depths[0];

		int[] rData = blk[0].getDataInt();
		int[] gData = blk[1].getDataInt();
		int[] bData = blk[2].getDataInt();

		if (rData.length != gData.length || gData.length != bData.length || bData.length != rData.length) {
			throw new IllegalArgumentException("Different dimensions for bands");
		}

		int[] pixels = new int[rData.length];
		//		int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE;
		//		int minG = Integer.MAX_VALUE, maxG = Integer.MIN_VALUE;
		//		int minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;

		for (int j = 0; j < rData.length; j++) {

			/* Signed values, should be in [-128 .. 127] (for depth = 8). */
			int r = rData[j];
			int g = gData[j];
			int b = bData[j];

			/* Determine min and max per band. For debugging. Turns out values outside [-128 .. 127] are possible in samples!?! */
			//			if (r < minR) { minR = r; } if (r > maxR) { maxR = r; }
			//			if (g < minG) { minG = g; } if (g > maxG) { maxG = g; }
			//			if (b < minB) { minB = b; } if (b > maxB) { maxB = b; }

			pixels[j] = signedComponentsToUnsignedARGB(r, g, b, depth);		}
		Bitmap bitmap = new Bitmap(pixels, width, height, 24, -1, true, 3);
		return bitmap;
	}

	/**
	 * Decodes 8-bit gray scale to 8-bit unsigned RGB.
	 * 
	 * @param blk
	 * @param depths
	 * @return
	 */
	private static Bitmap decodeGrayScale(DataBlkInt[] blk, int width, int height, int[] depths) {
		if (blk.length != 1) {
			throw new IllegalArgumentException("Was expecting 1 band");
		}
		if (depths.length != 1) {
			throw new IllegalArgumentException("Was expecting 1 band");
		}
		int[] data = blk[0].getDataInt();
		int[] pixels = new int[data.length];
		for (int j = 0; j < data.length; j++) {
			pixels[j] = 0xFF000000 | ((data[j] & 0xFF) << 16) | ((data[j] & 0xFF) << 8) | (data[j] & 0xFF);
		}
		Bitmap bitmap = new Bitmap(pixels, width, height, 24, -1, true, 3);
		return bitmap;
	}

	private static String[][] getAllDecoderParameters() {
		Vector<String[]> vec = new Vector<String[]>();
		int i;

		String[][] str = BitstreamReaderAgent.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = EntropyDecoder.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = ROIDeScaler.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = Dequantizer.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = InvCompTransf.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = HeaderDecoder.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = ICCProfiler.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = DECODER_PINFO;
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = new String[vec.size()][4];
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				str[i] = (String[]) vec.elementAt(i);
			}
		}

		return str;
	}

	private static String[][] getAllEncoderParameters() {
		Vector<String[]> vec = new Vector<String[]>();

		String[][] str = ENCODER_PINFO;
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = ForwCompTransf.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = AnWTFilter.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = ForwardWT.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = Quantizer.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = ROIScaler.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = EntropyCoder.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = HeaderEncoder.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = PostCompRateAllocator.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = PktEncoder.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = new String[vec.size()][4];
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				str[i] = (String[])vec.elementAt(i);

		return str;
	}

	static class BitmapDataSrc extends ImgReader {

		private Bitmap bitmap;

		public BitmapDataSrc(Bitmap bitmap) {
			this.bitmap = bitmap;
			this.w = bitmap.getWidth();
			this.h = bitmap.getHeight();
			this.nc = 3;
		}

		@Override
		public int getFixedPoint(int c) {
			if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
			return 0;
		}

		@Override
		public int getNomRangeBits(int c) {
			if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
			return (1 << nc);
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
			int[] compData = new int[pixels.length];
			if (blk == null) { blk = new DataBlkInt(0, 0, w, h); }
			int nomRangeBits = getNomRangeBits(c);
			for (int i = 0; i < pixels.length; i++) {
				compData[i] = unsignedARGBToSignedComponent(pixels[i], c, nc, nomRangeBits);
			}
			blk.setData(compData);
			return blk;
		}
		
	}
	
	/**
	 * 
	 * @param unsignedPixel
	 * @param c component index
	 * @param nc number of components, should be 3
	 * @param nomRangeBits, should be 8
	 * @return
	 */
	private static int unsignedARGBToSignedComponent(int unsignedPixel, int c, int nc, int nomRangeBits) {
		if (c < 0 || c >= nc) { throw new IllegalArgumentException(); }
		int byteIndex = nc - c - 1;
		int unsignedCompValue = (unsignedPixel & (0xFF << (byteIndex * 8))) >> (byteIndex * 8);
		int signedCompValue = unsignedCompValue - (1 << (nomRangeBits - 1));
		return signedCompValue;
	}
	
	private static int signedComponentsToUnsignedARGB(int r, int g, int b, int nomRangeBits) {
		if (r < -(1 << (nomRangeBits - 1))) { r = 0x00; } else if (r > ((1 << (nomRangeBits - 1)) - 1)) { r = (1 << nomRangeBits) - 1; } else { r += (1 << (nomRangeBits -1)); }
		if (g < -(1 << (nomRangeBits - 1))) { g = 0x00; } else if (g > ((1 << (nomRangeBits - 1)) - 1)) { g = (1 << nomRangeBits) - 1; } else { g += (1 << (nomRangeBits -1)); }
		if (b < -(1 << (nomRangeBits - 1))) { b = 0x00; } else if (b > ((1 << (nomRangeBits - 1)) - 1)) { b = (1 << nomRangeBits) - 1; } else { b += (1 << (nomRangeBits -1)); }

		return 0xFF000000 | ((r & 0xFF) << (2 * nomRangeBits)) | ((g & 0xFF) << nomRangeBits) | (b & 0xFF);
	}
}
