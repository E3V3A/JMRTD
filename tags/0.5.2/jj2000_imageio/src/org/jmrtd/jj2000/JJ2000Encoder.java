/* $Id: $ */

package org.jmrtd.jj2000;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jj2000.j2k.codestream.writer.FileCodestreamWriter;
import jj2000.j2k.codestream.writer.HeaderEncoder;
import jj2000.j2k.codestream.writer.PktEncoder;
import jj2000.j2k.encoder.EncoderSpecs;
import jj2000.j2k.entropy.encoder.EntropyCoder;
import jj2000.j2k.entropy.encoder.PostCompRateAllocator;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.Tiler;
import jj2000.j2k.image.forwcomptransf.ForwCompTransf;
import jj2000.j2k.quantization.quantizer.Quantizer;
import jj2000.j2k.roi.encoder.ROIScaler;
import jj2000.j2k.util.ParameterList;
import jj2000.j2k.wavelet.analysis.AnWTFilter;
import jj2000.j2k.wavelet.analysis.ForwardWT;

/**
 * JPEG 2000 encoder based on jj2000 library.
 * Tested with jj2000-5.2-SNAPSHOT.jar only.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
public class JJ2000Encoder {

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private final static String[][] ENCODER_PINFO = {
		{ "debug", null, "", "off" },
		{ "disable_jp2_extension", "[on|off]", "", "off" },
		{ "file_format", "[on|off]", "", "on" },
		{ "pph_tile", "[on|off]", "", "off" },
		{ "pph_main", "[on|off]", "", "off" },
		{ "pfile", "<filename of arguments file>", "", null },
		{ "tile_parts", "", "", "0" },
		{ "tiles", "<nominal tile width> <nominal tile height>", "", "0 0" },
		{ "ref", "<x> <y>", "", "0 0" },
		{ "tref", "<x> <y>",  "", "0 0" },
		{ "rate", "<output bitrate in bpp>", "", "1.0" },
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
			LOGGER.warning("No tiles option specified");
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
			LOGGER.warning("Can't have packed packet headers in both main and tile headers");

		if (pl.getParameter("rate") == null) {
			LOGGER.warning("Target bitrate not specified");
			return;
		}
		try {
			rate = pl.getFloatParameter("rate");
			if(rate==-1) {
				rate = Float.MAX_VALUE;
			}
		} catch (NumberFormatException e) {
			LOGGER.warning("Invalid value in 'rate' option: " + pl.getParameter("rate"));
			if(pl.getParameter("debug").equals("on")) {
				e.printStackTrace();
			} else {
				LOGGER.warning("Use '-debug' option for more details");
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
			LOGGER.warning("Invalid value in 'tile_parts' option: " + pl.getParameter("tile_parts"));
			if(pl.getParameter("debug").equals("on")) {
				e.printStackTrace();
			} else {
				LOGGER.warning("Use '-debug' option for more details");
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
			e.printStackTrace();
			return;
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
		if(pktspertp > 0 || pphTile || pphMain) {
			/* FIXME: Not supported. We don't have a memory only alternative for CodeStreamManipulator. */
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
		List<String[]> pl = new ArrayList<String[]>();

		String[][] str = ENCODER_PINFO;
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = ForwCompTransf.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = AnWTFilter.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = ForwardWT.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = Quantizer.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = ROIScaler.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = EntropyCoder.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = HeaderEncoder.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = PostCompRateAllocator.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		str = PktEncoder.getParameterInfo();
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				pl.add(str[i]);
			}
		}
		
		str = new String[pl.size()][4];
		if (str != null) {
			for (int i = str.length - 1; i >= 0; i--) {
				str[i] = (String[])pl.get(i);
			}
		}
		return str;
	}	
}
