/* $Id: $ */

package org.jmrtd.jj2000;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import jj2000.j2k.codestream.writer.FileCodestreamWriter;
import jj2000.j2k.codestream.writer.HeaderEncoder;
import jj2000.j2k.codestream.writer.PktEncoder;
import jj2000.j2k.encoder.EncoderSpecs;
import jj2000.j2k.entropy.encoder.EntropyCoder;
import jj2000.j2k.entropy.encoder.PostCompRateAllocator;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.DataBlk;
import jj2000.j2k.image.DataBlkInt;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.Tiler;
import jj2000.j2k.image.forwcomptransf.ForwCompTransf;
import jj2000.j2k.image.input.ImgReader;
import jj2000.j2k.quantization.quantizer.Quantizer;
import jj2000.j2k.roi.encoder.ROIScaler;
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
			if (pinfo[i][3] != null)
				defpl.put(pinfo[i][0], pinfo[i][3]);
		}
		ParameterList pl = new ParameterList(defpl);
		pl.put("rate", Double.toString(bitRate));
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
				compData[i] = JJ2000Util.unsignedARGBToSignedComponent(pixels[i], c, nc, nomRangeBits);
			}
			blk.setData(compData);
			return blk;
		}
		
	}	
}
